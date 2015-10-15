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
    static final int PAGE_SCANER_INTERVAL=1000; 
    static final int PAGE_SCANER_AROUND_ELEMENTS_COUNT=10; 
    
    
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

    class AutoPageElementScanner extends TimerTask
    {
    	BrowserArea browser;
        Timer pageTimer=null;
    	AutoPageElementScanner(BrowserArea browser)
    	{
    		this.browser=browser;
    	}
		@Override public void run()
		{
			browser.onTimerElementScan();
		}
		public void schedule()
		{
    		pageTimer=new Timer();
			pageTimer.scheduleAtFixedRate(this,PAGE_SCANER_INTERVAL,PAGE_SCANER_INTERVAL);
		}
    }
    
    private AutoPageElementScanner pageScaner;

    ElementList elementsForScan=null;
    
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
		elementsForScan=page.elementList();
		splittedLineProc = new SplittedLineProc();

		pageScaner=new AutoPageElementScanner(this);
		pageScaner.schedule();
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
		return split.type+" "+split.text;
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
	    case KeyboardEvent.F5: 
		onRescanPageDom();
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

    private void onRescanPageDom()
    {
	    page.RescanDOM();
	    textSelectorEmpty=page.selectorTEXT(true,null);
	    final int width = luwrain.getAreaVisibleWidth(this);
	    splittedLineProc.splitAllElementsTextToLines(width > MIN_WIDTH?width:width, textSelectorEmpty, elements);
	    if(!textSelectorEmpty.first(elements))
	    	environment.hint(Hints.NO_CONTENT); 
	    currentSelector = textSelectorEmpty;
	    environment.onAreaNewContent(this);
	    luwrain.onAreaNewContent(this);
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

    int lastHotPointY=-1;
    int scanPos=-1;
    public void onTimerElementScan()
    {
    	if(textSelectorEmpty==null) return;
    	if(splittedLineProc==null) return;
    	if(lastHotPointY!=getHotPointY()||scanPos==-1)
    	{
    		final SplittedLineProc.SplittedLine sl = splittedLineProc.getSplittedLineByIndex(getHotPointY());
    		if(sl==null) return;
    		scanPos=sl.pos;
    	}
    	
    	if(elementsForScan.isChangedAround(textSelectorEmpty,scanPos,PAGE_SCANER_AROUND_ELEMENTS_COUNT))
    	{ // detected changes
    		onRescanPageDom();
    		return;
    	}
    }
    
    private boolean onDefaultAction()
    {
    	if(textSelectorEmpty==null)
	    return false;
    	final SplittedLineProc.SplittedLine sl = splittedLineProc.getSplittedLineByIndex(getHotPointY());
    	boolean res=textSelectorEmpty.to(elements,sl.pos);
    	// FIXME: make error handling for res==false
   		if(elements.isEditable())
		{
		    String oldValue = elements.getText();
		    if (oldValue == null) oldValue = "";
		    String newValue = Popups.simple(luwrain, "Редактирование формы", "Введите новое значение текстового поля формы:", oldValue);
		    if (newValue == null) return true;
		    elements.setText(newValue);
		    final int width = luwrain.getAreaVisibleWidth(this);
		    splittedLineProc.updateSplitForElementText(width,elements);
		    onNewSelectedElement();
		    environment.onAreaNewContent(this);
		    return true;
		}
   		// emulate click
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
		onRescanPageDom();
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
		luwrain.message("Внимание!" + message, Luwrain.MESSAGE_OK);
    }

    private String onPrompt(final String message,final String value)
    {
		if (message == null || message.trim().isEmpty()) return null;
		luwrain.message("Выбор: " +message, Luwrain.MESSAGE_OK);
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
