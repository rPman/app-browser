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

import org.luwrain.core.*;
import org.luwrain.core.events.*;
import org.luwrain.controls.*;
import org.luwrain.popups.*;

import java.util.Timer;
import java.util.TimerTask;
import java.util.Vector;

import org.luwrain.app.browser.Events;
import org.luwrain.app.browser.web.*;
import org.luwrain.browser.*;
import org.luwrain.browser.Events.WebState;

class BrowserArea extends NavigateArea
{
    static private final int PAGE_SCANNER_INTERVAL=1000;
    static private final int PAGE_SCANNER_INTERVAL_FAST=100;
    static private final int PAGE_SCANNER_AROUND_ELEMENTS_COUNT=10; 

    private Luwrain luwrain;
    private ControlEnvironment environment;
    private Actions actions;
    private Browser page;
    private Events browserEvents;

    private WebState state = WebState.READY;
    private int progress = 0;

    WebDocument wDoc = new WebDocument();
    WebView wView = new WebView();
    /** current element in view */
    WebElement element;

    static class HistoryElement
    {
	HistoryElement(WebElement element,boolean mode)
	{
	    this.element=element;
	    this.mode=mode;
	}

	WebElement element;
	boolean mode;
    }

    final Vector<HistoryElement> elementHistory = new Vector<HistoryElement>();
    private boolean complexMode=false;

    static private class AutoPageElementScanner extends TimerTask
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

	void schedule()
	{
	    pageTimer=new Timer();
	    pageTimer.scheduleAtFixedRate(this,PAGE_SCANNER_INTERVAL,PAGE_SCANNER_INTERVAL);
	}
		public void fast()
		{
			//pageTimer.cancel();
			pageTimer.scheduleAtFixedRate(this,PAGE_SCANNER_INTERVAL_FAST,PAGE_SCANNER_INTERVAL);
		}
	}
	
	private AutoPageElementScanner pageScaner;
	ElementIterator elementsForScan=null;
	private SelectorText textSelectorInvisible=null;

	int scanPos=-1;
	public void onTimerElementScan()
	{
		if(state!=WebState.SUCCEEDED) return;
		luwrain.enqueueEvent(new CheckChangesEvent(this));
	}

	BrowserArea(Luwrain luwrain, Actions actions, Browser browser)
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

		pageScaner=new AutoPageElementScanner(this);
		pageScaner.schedule();
	}

	@Override public String getAreaName()
	{
		return page.getTitle()+" "+state.name()+" "+progress;
	}

	@Override public int getLineCount()
	{
		return wView.getLinesCount();
	}

	@Override public String getLine(int index)
	{
		return wView.getLineByPos(index);
	}

	@Override public boolean onKeyboardEvent(KeyboardEvent event)
	{
		NullCheck.notNull(event, "event");
		if (event.isSpecial() && !event.isModified())
		switch (event.getSpecial())
		{
		case TAB:
			return onInfoAction();
		case ESCAPE:
			onBreakCommand();
			return true;
		case F5: 
			onRescanPageDom();
			return true;
		case F6: 
			onChangeCurrentPageLink();
			return true;
		case ALTERNATIVE_ARROW_LEFT:
			return onElementNavigateLeft();
		case ALTERNATIVE_ARROW_RIGHT:
			return onElementNavigateRight();
		case ENTER:
			return onDefaultAction();
		case BACKSPACE:
			return onHistoryBack();
		case F10:
			onChangeWebViewVisibility();
			return true;
		default:
			break;
		}
		if(event.getChar()==' ')
		{
			WebElementPart part=wView.getElementByPos(getHotPointX(),getHotPointY());
			if(part!=null)
				environment.say(part.text);
		}
		return super.onKeyboardEvent(event);
	}
	
	@Override public boolean onEnvironmentEvent(EnvironmentEvent event)
	{
		NullCheck.notNull(event, "event");
		switch(event.getCode())
		{
		case CLOSE:
			actions.closeApp();
			return true;
		case THREAD_SYNC:
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
			if(state!=WebState.SUCCEEDED) return true;
			
			if(page.isBusy()) return true;
			//if(wView.getLinesCount()<=getHotPointY()) return true;
			Vector<WebElementPart> line=wView.getPartsByPos(getHotPointY());
			if(line==null||line.size()==0) return true;
			scanPos=line.get(0).element.getElement().getPos();
			
			if(elementsForScan.isChangedAround(textSelectorInvisible,scanPos,PAGE_SCANNER_AROUND_ELEMENTS_COUNT))
			{ // detected changes, add event to rescan page dom
				onRescanPageDom();
			}
			return true;
		}
		return false;
	}
	private void onPageChangeState(WebState state)
	{
		this.state=state;
		switch(state)
		{
			case SCHEDULED:
				return;
			case RUNNING:
				luwrain.message("Загрузка страницы");
				return;
			case SUCCEEDED:
				onRescanPageDom();
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

	private void onBreakCommand()
	{
		page.stop();
	}

    private void onRescanPageDom()
    {
    	System.out.println("rescan start");
		if(state!=WebState.SUCCEEDED)
		    return;
		if(page.isBusy())
		{
		    Log.debug("web","webpage is busy");
		    return;
		}
		page.RescanDOM();
		elementsForScan=page.iterator();
		textSelectorInvisible=page.selectorText(false,null);
	
		wDoc=new WebDocument();
		wDoc.make(page);
		wDoc.getRoot().print(1);
		
		element=wDoc.getRoot();
		complexMode=false;
		refill();
    	System.out.print("rescan end");
    }
    private void repairHotPoint()
    {
		int x=getHotPointX(),y=getHotPointY();
		WebElementPart part=null;
		while(true)
		{ // loop try to select eny element (under cursor, last, first)
			part=wView.getElementByPos(x,y);
			if(part==null)
			{
				// last try?
				if(y==0&&x==0) break;
				// we try to select first element in the last line
				if(y==wView.getLinesCount()-1)
				{
					if(x==0)
					{ // try to get first line
						y=0;
						continue;
					} else
					{ // try to get first element in line
						x=0;
						continue;
					}
				} else
				if(y>=wView.getLinesCount())
				{
					y=wView.getLinesCount()-1;
					continue;
				} else
				{
					x=0;
					break;
				}
			} else
				break;
		}
		setHotPoint(x,y);
    }

	private void onChangeCurrentPageLink()
	{
		String link = Popups.simple(luwrain, "Открыть страницу", "Введите адрес страницы:", "http://");
		if(link==null||link=="") 
			return;
		if(!link.matches("^(http|https|ftp)://.*$"))
			link="http://"+link;
		page.load(link);
		environment.onAreaNewContent(this);
	}

	private boolean onElementNavigateLeft()
	{ // prev
		WebElementPart part=wView.getElementByPos(getHotPointX(),getHotPointY());
		Vector<WebElementPart> line=wView.getPartsByPos(getHotPointY());
		if(part==null||line==null) return false;
		int idx=line.indexOf(part);
		if(idx==0)
		{ // move previous line
			if(getHotPointY()==0) return false;
			line=wView.getPartsByPos(getHotPointY()-1);
			setHotPoint(line.lastElement().pos,getHotPointY()-1);
		} else
		{ // move inside line
			setHotPoint(line.get(idx-1).pos,getHotPointY());
		}
  		onNewSelectedElement();
  		environment.onAreaNewContent(this);
  		return true;
	}
	private boolean onElementNavigateRight()
	{ // next
		WebElementPart part=wView.getElementByPos(getHotPointX(),getHotPointY());
		Vector<WebElementPart> line=wView.getPartsByPos(getHotPointY());
		if(part==null||line==null) return false;
		int idx=line.indexOf(part);
		if(idx==line.size()-1)
		{ // move next line
			if(getHotPointY()+1==wView.getLinesCount()) return false;
			setHotPoint(0,getHotPointY()+1);
		} else
		{ // move inside line
			setHotPoint(line.get(idx+1).pos,getHotPointY());
		}
  		onNewSelectedElement();
  		environment.onAreaNewContent(this);
  		return true;
	}
	private void  onNewSelectedElement()
	{
		WebElementPart part=wView.getElementByPos(getHotPointX(),getHotPointY());
		final String type=part.element.getTextShort();
		final String text=part.element.getTextSay();
		//final String link=part.element.getLink();
		luwrain.say(elementTypeTranslation(type) + " "+text);
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
	private boolean onDefaultAction()
	{
		WebElementPart part=wView.getElementByPos(getHotPointX(),getHotPointY());
		if(part==null) return false;
		if(part.element.needToBeComplex()||complexMode)
		{ // select complex element as base for view in navigation area
			// store prev element to history
			elementHistory.add(new HistoryElement(element,complexMode));
			//
			complexMode=!complexMode;
			element=part.element;
			// refill
			wView=new WebView();
			WebViewBuilder builder;
			if(complexMode)
				builder=new WebBuilderComplex();
			else
				builder=new WebBuilderNormal();
			builder.refill(wView,element,luwrain.getAreaVisibleWidth(this));
			/**/wView.print();
			
			repairHotPoint();
			environment.onAreaNewContent(this);
			luwrain.onAreaNewContent(this);
			return true;
		}
		if(part.element.getElement().isEditable())
		{ // editable element, edit it
			if(part.element instanceof WebSelect)
				onMultiTextEditElement(part);
			else if(part.element instanceof WebRadio||part.element instanceof WebCheckbox)
				onClickElement(part);
			else
				onEditElement(part);
		} else
		{ // any other element - click it
			onClickElement(part);
		}
		return true;
	}
	private boolean onClickElement(WebElementPart part)
	{
		part.element.getElement().clickEmulate();
		onTimerElementScan();
		//pageScaner.fast();
		return true;
	}
	private boolean onHistoryBack()
	{
		if(elementHistory.isEmpty()) return false;
		HistoryElement h=elementHistory.lastElement();
		elementHistory.remove(elementHistory.size()-1);
		complexMode=h.mode;
		element=h.element;
		refill();
		return true;
	}
	
	private void refill()
	{
		wView=new WebView();
		WebViewBuilder builder;
		if(complexMode)
			builder=new WebBuilderComplex();
		else
			builder=new WebBuilderNormal();
		builder.refill(wView,element,luwrain.getAreaVisibleWidth(this));
		/**/wView.print();
		repairHotPoint();
		//
		WebElementPart part=wView.getElementByPos(getHotPointX(),getHotPointY());
		if(part!=null)
			environment.say(part.text);
		environment.onAreaNewContent(this);
		luwrain.onAreaNewContent(this);
	}
	
	private void onEditElement(WebElementPart part)
	{
		ElementIterator e=part.element.getElement();
		String oldValue = e.getText();
		if (oldValue == null) oldValue = "";
		String newValue = Popups.simple(luwrain, "Редактирование формы", "Введите новое значение текстового поля формы:", oldValue);
		if (newValue == null) return;
		e.setText(newValue);
		refill();
	}
	private void onMultiTextEditElement(WebElementPart part)
	{
		ElementIterator e=part.element.getElement();
		String[] listValues = e.getMultipleText();
		if (listValues.length==0) return; // FIXME:
		EditListPopup popup=new EditListPopup(luwrain,
				new FixedEditListPopupModel(listValues),
				"Редактирование формы","Выберите значение из списка",e.getText());
		luwrain.popup(popup);
		if(popup.closing.cancelled()) return;
		e.setText(popup.text());
		refill();
	}

	private boolean onInfoAction()
	{
		WebElementPart part=wView.getElementByPos(getHotPointX(),getHotPointY());
		if(part==null) return false;
		// first info - is short text
		String info=part.element.getTextShort()+" ";
		// second info - nearest parent  of complrx view item
		WebElement e=part.element;
		if(e instanceof WebText)
		{
			// FIXME:
			String cssfont=e.getElement().getComputedStyleProperty("font-weight");
			if(cssfont!=null&&!cssfont.equals("normal"))
			{
				info+=" font "+cssfont;
			}
			if(e.getAttributes().containsKey("href"))
			{
				info+=" link "+e.getAttributes().get("href");
			}
		}
		// scan for parent complex
		while(e!=null)
		{
			if(e instanceof WebListElement)
			{
				info+=" list item "+e.getTextShort();
				break;
			}
			e=e.getParent();
		}
		environment.say(info);
		return true;
	}
	private void onChangeWebViewVisibility()
	{
		page.setVisibility(!page.getVisibility());
	}
}
