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

import java.util.*;

import org.luwrain.core.*;
import org.luwrain.core.events.*;
import org.luwrain.controls.*;
import org.luwrain.popups.*;
import org.luwrain.browser.*;
import org.luwrain.browser.Events.WebState;

class BrowserArea extends NavigateArea
{
	static final int PAGE_SCANER_INTERVAL=1000; 
	static final int PAGE_SCANER_AROUND_ELEMENTS_COUNT=10; 
	
	
	static private final int MIN_WIDTH = 10;

	private Luwrain luwrain;
	private ControlEnvironment environment;
	private Actions actions;
	private org.luwrain.browser.Browser page;
	private org.luwrain.browser.Events browserEvents;
	private ElementList elements=null;
	private SplittedLineProc splittedLineProc = null;

	private SelectorText textSelectorEmpty=null;
	private SelectorText textSelectorInvisible=null;
	private Selector currentSelector = null;

	private int progress=0;
	private WebState state=WebState.READY;

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
	
	WebState pageState=WebState.CANCELLED;
	
	BrowserArea(Luwrain luwrain, Actions actions,
		Browser browser)
	{
		super(new DefaultControlEnvironment(luwrain));
		this.luwrain = luwrain;
		this.actions = actions;
		this.environment = new DefaultControlEnvironment(luwrain);
		this.page = browser;
		NullCheck.notNull(luwrain, "luwrain");
		NullCheck.notNull(actions, "actions");
		NullCheck.notNull(browser, "browser");
		browserEvents = new Events(luwrain, this);
		this.page.init(browserEvents);
		elements=page.iterator();
		elementsForScan=page.iterator();
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
		return split.text;
	}

	@Override public String getAreaName()
	{
		return page.getTitle()+" "+state.name()+" "+progress;
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
			refresh();
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
		case KeyboardEvent.F10:
			onChangeWebViewVisibility();
		return true;
		}
		if(event.getCharacter()==' ')
		{
			onInfoAction();
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
	if(event instanceof CheckChangesEvent)
	{
		if(pageState!=WebState.SUCCEEDED) return true;
		if(lastHotPointY!=getHotPointY()||scanPos==-1)
		{
			if(page.isBusy()) return true;
			final SplittedLineProc.SplittedLine sl = splittedLineProc.getSplittedLineByIndex(getHotPointY());
			if(sl==null) return true;
			scanPos=sl.elements[0].pos;
		}
		
		if(elementsForScan.isChangedAround(textSelectorInvisible,scanPos,PAGE_SCANER_AROUND_ELEMENTS_COUNT))
		{ // detected changes, add event to rescan page dom
			onRescanPageDom();
		}
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
	if(pageState!=WebState.SUCCEEDED)
	    return;
	if(page.isBusy())
	{
	    Log.debug("web","webpage is busy");
	    return;
	}
	page.RescanDOM();
	textSelectorEmpty=page.selectorText(true,null);
	textSelectorInvisible=page.selectorText(false,null);
	final int width = luwrain.getAreaVisibleWidth(this);
	splittedLineProc.splitAllElementsTextToLines(width > MIN_WIDTH?width:width, textSelectorEmpty, elements);
	if(!textSelectorEmpty.moveFirst(elements))
	    environment.hint(Hints.NO_CONTENT); 
	currentSelector = textSelectorEmpty;
	environment.onAreaNewContent(this);
	luwrain.onAreaNewContent(this);
    }

    private void ready()
    {
	onRescanPageDom();

	final ElementList it = page.iterator();
	final SelectorAll sel = page.selectorAll(false);
	System.out.println("Begin enumerating");
	if (!sel.moveFirst(it))
	{
	    System.out.println("no first");
	}
	while(sel.moveNext(it))
	    System.out.println(it.getText());
	System.out.println("Finished!");

    }

    private void refresh()
    {
	onRescanPageDom();
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
		if(textSelectorEmpty==null)
			return false;
		SplittedLineProc.SplitedElementPos sPos=splittedLineProc.getSplitedByXY(getHotPointX(),getHotPointY());
		if(sPos==null) return true;
		if(sPos.element==0)
		{ // reached last element in line
			environment.playSound(Sounds.NO_ITEMS_BELOW);
			return true;
		}
		// select next element in text selector
		SplittedLineProc.InlineElement next=splittedLineProc.getSplittedLines()[sPos.splited][sPos.line].elements[sPos.element-1];
		boolean res=textSelectorEmpty.moveToPos(elements,next.pos);
		this.setHotPointX(next.start);

  		onNewSelectedElement();
  		environment.onAreaNewContent(this);
  		return true;
	}

	private boolean onElementNavigateRight()
	{ // next
		if(textSelectorEmpty==null)
			return false;
		SplittedLineProc.SplitedElementPos sPos=splittedLineProc.getSplitedByXY(getHotPointX(),getHotPointY());
		if(sPos==null) return true;
		if(splittedLineProc.getSplittedLines()[sPos.splited][sPos.line].elements.length-1==sPos.element)
		{ // reached last element in line
			environment.playSound(Sounds.NO_ITEMS_BELOW);
			return true;
		}
		// select next element in text selector
		SplittedLineProc.InlineElement next=splittedLineProc.getSplittedLines()[sPos.splited][sPos.line].elements[sPos.element+1];
		boolean res=textSelectorEmpty.moveToPos(elements,next.pos);
		this.setHotPointX(next.start);

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
		luwrain.enqueueEvent(new CheckChangesEvent(this));
	}
	
	private boolean onDefaultAction()
	{
		if(textSelectorEmpty==null)
			return false;
		SplittedLineProc.SplitedElementPos sPos=splittedLineProc.getSplitedByXY(getHotPointX(),getHotPointY());
		if(sPos==null) return true;
		// select next element in text selector
		SplittedLineProc.InlineElement element=splittedLineProc.getSplittedLines()[sPos.splited][sPos.line].elements[sPos.element];
		boolean res=textSelectorEmpty.moveToPos(elements,element.pos);
		if(!res) return true; // FIXME: make error handling for res==false
		if(elements.isEditable())
		{
	   		// FIXME: make method to detect multitext
			if(elements.getType().equals("select"))
	   		{ // need multiselect popup
	   			onMultiTextEditElement();
	   		} else
	   		{
	   			// edit element contents
	   			onEditElement();
	   		}
			return true;
		} else
		{
			// emulate click
			elements.clickEmulate();
			return true;
		}
	}
	
	private void onEditElement()
	{
		String oldValue = elements.getText();
		if (oldValue == null) oldValue = "";
		String newValue = Popups.simple(luwrain, "Редактирование формы", "Введите новое значение текстового поля формы:", oldValue);
		if (newValue == null) return;
		elements.setText(newValue);
		final int width = luwrain.getAreaVisibleWidth(this);
		splittedLineProc.updateSplitForElementText(width,elements);
		onNewSelectedElement();
		environment.onAreaNewContent(this);
	}
	
	private void onMultiTextEditElement()
	{
		String[] listValues = elements.getMultipleText();
		if (listValues.length==0) return; // FIXME:
		EditListPopup popup=new EditListPopup(luwrain,
				new FixedEditListPopupModel(listValues),
				"Редактирование формы","Выберите значение из списка",elements.getText());
		luwrain.popup(popup);
		if(popup.closing.cancelled()) return;
		elements.setText(popup.text());
		final int width = luwrain.getAreaVisibleWidth(this);
		splittedLineProc.updateSplitForElementText(width,elements);
		onNewSelectedElement();
		environment.onAreaNewContent(this);
	}

	private void onInfoAction()
	{
		if(textSelectorEmpty==null) return;
		SplittedLineProc.SplitedElementPos sPos=splittedLineProc.getSplitedByXY(getHotPointX(),getHotPointY());
		if(sPos==null) return;
		// select next element in text selector
		SplittedLineProc.InlineElement element=splittedLineProc.getSplittedLines()[sPos.splited][sPos.line].elements[sPos.element];
		boolean res=textSelectorEmpty.moveToPos(elements,element.pos);
		if(!res) return; // FIXME: make error handling for res==false

		final String type=elements.getType();
		final String text=elements.getText();
		final String link=elements.getLink();
		if(link!=null&&!link.isEmpty())
		{
			luwrain.say(elementTypeTranslation(type) + " "+link);
		} else
		{
			luwrain.say(elementTypeTranslation(type));
		}
	}
	
	private void onPageChangeState(WebState state)
	{
		pageState=state;
		switch(state)
		{
			case SCHEDULED:
				return;
			case RUNNING:
				luwrain.message("Загрузка страницы");
				return;
			case SUCCEEDED:
ready();
				luwrain.message("Страница загружена", Luwrain.MESSAGE_DONE);
				return;
			case READY:
				// luwrain.message("Страница загружена", Luwrain.MESSAGE_DONE);
				return;
			case FAILED:
				luwrain.message("Страница не может быть загружена", Luwrain.MESSAGE_ERROR);
				return;
			case CANCELLED:
				return;
		}
		System.out.println("browser:unhandled page state changing:" + state);
	}

	private void onProgress(Number progress)
	{
		this.progress=(int)(progress==null?0:Math.floor(progress.doubleValue()*100));
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
