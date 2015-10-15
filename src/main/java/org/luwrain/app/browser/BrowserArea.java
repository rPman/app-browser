/*
   Copyright 2012-2014 Michael Pozhidaev <msp@altlinux.org>

   This file is part of the Luwrain.

   Luwrain is free software; you can redistribute it and/or
   modify it under the terms of the GNU General Public
   License as published by the Free Software Foundation; either
   version 3 of the License, or (at your option) any later version.

   Luwrain is distributed in the hope that it will be useful,
   but WITHOUT ANY WARRANTY; without even the implied warranty of
   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
   General Public License for more details.
*/

package org.luwrain.app.browser;

import java.io.*;
//import java.io.FileOutputStream;
//import java.io.IOException;
//import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.*;
//import java.util.Objects;
//import java.util.Vector;

import javafx.concurrent.Worker.State;//FIXME:


import org.luwrain.core.*;
import org.luwrain.core.events.*;
import org.luwrain.controls.*;
import org.luwrain.popups.*;
import org.luwrain.interaction.browser.WebPage;
import org.luwrain.browser.*;
//import org.luwrain.browser.ElementList;
import org.luwrain.browser.ElementList.*;

class BrowserArea extends NavigateArea
{
    static private final int MIN_WIDTH = 10;

    private Luwrain luwrain;
    private ControlEnvironment environment;
    private Actions actions;
    private WebPage page;
    private BrowserEvents browserEvents;
    private ElementList elements=null;
    private SplittedLineProc splittedLineProc = null;

    private SelectorTEXT textSelectorEmpty=null;
    private Selector currentSelector = null;

    BrowserArea(Luwrain luwrain, Actions actions,
		Browser browser)
    {
	super(new DefaultControlEnvironment(luwrain));
	this.luwrain = luwrain;
	this.actions = actions;
	this.environment = new DefaultControlEnvironment(luwrain);
	this.page = (WebPage)browser;
	NullCheck.notNull(luwrain, "luwrain");
	NullCheck.notNull(actions, "actions");
	NullCheck.notNull(browser, "browser");
	browserEvents = new Events(luwrain, this);
	this.page.init(browserEvents);
	    	elements=page.elementList();
		splittedLineProc = new SplittedLineProc();
    }

    private void  onNewSelectedElement()
    {
	    final String type=elements.getType();
	    final String text=elements.getText();
	    final String link=elements.getLink();
	    luwrain.say(elementTypeTranslation(type) + " "+text);
    }

    @Override public int getLineCount()
    {
final int res = splittedLineProc.getSplittedCount();
return res >= 1?res:1;
    }

    @Override public String getLine(int index)
    {
		final SplittedLineProc.SplittedLine split = splittedLineProc.getSplittedLineByIndex(index);
		if (split == null)
		    return "";
		//		System.out.println("splitted");
		return split.text;
    }

    @Override public String getAreaName()
    {
    	return page.getBrowserTitle();
    }

    @Override public boolean onKeyboardEvent(KeyboardEvent event)
    {
    	NullCheck.notNull(event, "event");
    	if (event.isCommand() && !event.isModified())
	    switch (event.getCommand())
	    {
	    case KeyboardEvent.ESCAPE:
		onBreakCommand();
		return true;
	    case KeyboardEvent.F6: 
		onChangeCurrentPageLink();
		return true;
	    case KeyboardEvent.ALTERNATIVE_ARROW_LEFT:
		return onElementNavigateLeft();
	    case KeyboardEvent.ALTERNATIVE_ARROW_RIGHT:
		return onElementNavigateRight();
	    case KeyboardEvent.ENTER:
		onDefaultAction();
		return true;
	    }
    	return super.onKeyboardEvent(event);
    }
    
    @Override public boolean onEnvironmentEvent(EnvironmentEvent event)
    {
    	NullCheck.notNull(event, "event");
	switch(event.getCode())
	{
	case EnvironmentEvent.CLOSE:
	    actions.closeApp();
	    return true;
	case EnvironmentEvent.THREAD_SYNC:
	    if (onThreadSyncEvent(event))
		return true;
	    return super.onEnvironmentEvent(event);
	default:
	    return super.onEnvironmentEvent(event);
	}
    }
    
    private boolean onThreadSyncEvent(EnvironmentEvent event)
    {
	if (event instanceof PageChangeStateEvent)
	{
	    final PageChangeStateEvent changeState = (PageChangeStateEvent)event;
	    onPageChangeState(changeState.state());
	    return true;
	}
	if (event instanceof ProgressEvent)
	{
	    final ProgressEvent progress = (ProgressEvent)event;
	    onProgress(progress.value());
	    return true;
	}
	if (event instanceof AlertEvent)
	{
	    Log.debug("web","t:"+Thread.currentThread().getId()+" ALERT event inside area");
	    final AlertEvent alert = (AlertEvent)event;
	    onAlert(alert.message());
	    return true;
	}
	if (event instanceof PromptEvent)
	{
	    Log.debug("web","t:"+Thread.currentThread().getId()+" PROMPT event inside area");
	    final PromptEvent prompt = (PromptEvent)event;
	    final String answer = onPrompt(prompt.message(), prompt.value());
	    prompt.setAnswer (answer);
	    return true;
	}
	if (event instanceof ErrorEvent)
	{
	    final ErrorEvent error = (ErrorEvent)event;
	    onError(error.message());
	    return true;
	}
	if (event instanceof DownloadEvent)
	{
	    final DownloadEvent download = (DownloadEvent)event;
	    onDownloadStart(download.url());
	    return true;
	}
	if (event instanceof ConfirmEvent)
	{
	    final ConfirmEvent confirm = (ConfirmEvent)event;
	    final boolean answer = onConfirm(confirm.message());
	    confirm.setAnswer(answer);
	    return true;
	}
	return false;
    }

    private void onBreakCommand()
    {
	page.stop();
    }

    private void onChangeCurrentPageLink()
    {
	String link = Popups.simple(luwrain, "Открыть страницу", "Введите адрес страницы:", "http://");
	if(link==null) 
	    return;
	if(!link.matches("^(http|https|ftp)://.*$"))
	    link="http://"+link;
	page.load(link);
	environment.onAreaNewContent(this);
    }

    private void onChangeWebViewVisibility()
    {
	page.setVisibility(!page.getVisibility());
    }

    private boolean onElementNavigateLeft()
    { // prev
	if(currentSelector == null)
	    return false;
	if(!currentSelector.prev(elements))
	{
	    luwrain.hint(Hints.NO_ITEMS_ABOVE);
	    return true;
	}
	onNewSelectedElement();
	environment.onAreaNewContent(this);
	return true;
    }

    private boolean onElementNavigateRight()
    { // next
	if(currentSelector == null) 
	    return false;
	if(!currentSelector.next(elements))
	{
	    luwrain.hint(Hints.NO_ITEMS_BELOW);
	    return true;
	}
	onNewSelectedElement();
	environment.onAreaNewContent(this);
	return true;
    }

    /*
    private void moveHotPointToElement()
    {
    	int pos=elements.getPos();
    	SplittedLineProc.SplittedLine[] sls = splittedLineProc.getSplittedLineByPos(pos);
    	int snum=sls[0].index;
    	setHotPointY(snum);
    	setHotPointX(0);
    }
    */

    private boolean onDefaultAction()
    {
    	if(textSelectorEmpty==null)
	    return false;
    	final SplittedLineProc.SplittedLine sl = splittedLineProc.getSplittedLineByIndex(getHotPointY());
    	boolean res=textSelectorEmpty.to(elements,sl.pos);
   		if(elements.isEditable())
		{
		    String oldValue = elements.getText();
		    if (oldValue == null)
			oldValue = "";
		    final String newValue = Popups.simple(luwrain, "Редактирование формы", "Введите новое значение текстового поля формы:", oldValue);
		    if (newValue == null)
			return true;
		    elements.setText(newValue);
		    onNewSelectedElement();
		    environment.onAreaNewContent(this);
		    return true;
		}
		elements.clickEmulate();
		return true;
    }

    private void onPageChangeState(State state)
    {
	if (state == State.SCHEDULED)
	    return;
	if (state == State.RUNNING)
	{
	    luwrain.message("Загрузка страницы");
	    return;
	}
	if(state == State.SUCCEEDED)
	{
	    final int width = luwrain.getAreaVisibleWidth(this);
	    page.RescanDOM();
	    textSelectorEmpty=page.selectorTEXT(true,null);
	    splittedLineProc.splitAllElementsTextToLines(width > MIN_WIDTH?width:width, textSelectorEmpty, elements);
	    if(!textSelectorEmpty.first(elements))
	    	environment.hint(Hints.NO_CONTENT); 
	    currentSelector = textSelectorEmpty;
	    environment.onAreaNewContent(this);
	    luwrain.onAreaNewContent(this);
	    luwrain.message("Страница загружена", Luwrain.MESSAGE_DONE);
	    return;
	}
	if (state == State.READY)
	{
	    //	    luwrain.message("Страница загружена", Luwrain.MESSAGE_DONE);
	    return;
	}
	if (state == State.FAILED)
	{
	    luwrain.message("Страница не может быть загружена", Luwrain.MESSAGE_ERROR);
	    return;
	}
	System.out.println("browser:unhandled page state changing:" + state);
    }

    private void onProgress(Number progress)
    {
	//FIXME:
    }

    private void onAlert(final String message)
    {
		if (message == null || message.trim().isEmpty()) return;
		luwrain.message("Внимание!" + message, Luwrain.MESSAGE_ERROR);
    }

    private String onPrompt(final String message,final String value)
    {
		if (message == null || message.trim().isEmpty()) return null;
		luwrain.message("Подтверждение: " +message, Luwrain.MESSAGE_OK);
		return "";//result;
    }

    private void onError(String message)
    {
		if (message == null || message.trim().isEmpty()) return;
   		luwrain.message (message, Luwrain.MESSAGE_ERROR);
    }

    private boolean onDownloadStart(String url)
    {
	return true;
    }

    private  Boolean onConfirm(String message)
    {
   		luwrain.message ("Подтверждение: " +message, Luwrain.MESSAGE_OK);
		return false;
    }

    private String elementTypeTranslation(String type)
    {
	switch(type.toLowerCase().trim())
	{
	case "link":
	    return "Ссылка";
	default:
	    return type;
	}
    }
}
