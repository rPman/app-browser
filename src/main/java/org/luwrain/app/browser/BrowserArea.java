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

//import java.util.Timer;
//import java.util.TimerTask;
import java.util.Vector;

import org.luwrain.app.browser.Events;
import org.luwrain.app.browser.web.*;
import org.luwrain.browser.*;
import org.luwrain.browser.Events.WebState;

class BrowserArea extends NavigationArea
{
    //static private final int PAGE_SCANNER_INTERVAL=1000;
    //    static private final int PAGE_SCANNER_INTERVAL_FAST=100;
    static private final int PAGE_SCANNER_AROUND_ELEMENTS_COUNT=10; 

    private final Luwrain luwrain;
    private final ControlEnvironment environment;
    private final Actions actions;
    private final Browser page;
    private Events browserEvents;

    private WebState state = WebState.READY;
    private int progress = 0;

    private WebDocument wDoc = new WebDocument();
    private WebView wView = new WebView();
    /** current element in view */
    private WebElement element;

    private final Vector<HistoryElement> elementHistory = new Vector<HistoryElement>();
    private boolean complexMode = false;

	private final AutoPageElementScanner pageScanner;
    private ElementIterator elementsForScan = null;
    private SelectorText textSelectorInvisible = null;

	private int scanPos = -1;

    BrowserArea(Luwrain luwrain, Actions actions, Browser browser)
    {
	super(new DefaultControlEnvironment(luwrain));
	NullCheck.notNull(luwrain, "luwrain");
	NullCheck.notNull(actions, "actions");
	NullCheck.notNull(browser, "browser");
	this.luwrain = luwrain;
	this.actions = actions;
	this.environment = new DefaultControlEnvironment(luwrain);
	this.page = browser;
	browserEvents = new Events(luwrain, this);
	this.page.init(browserEvents);
	pageScanner = new AutoPageElementScanner(this);
	pageScanner.schedule();
    }

    void onTimerElementScan()
    {
	if(state!=WebState.SUCCEEDED) return;
	luwrain.enqueueEvent(new CheckChangesEvent(this));
    }

    /**
     * Performs DOM scanning with updating the auxiliary structures used for
     * user navigation. This method may be called only if the page is
     * successfully loaded and the browser isn't busy with background work.
     *
     * @return true if the browser is free and able to do the refreshing, false otherwise
     */
    boolean refresh()
    {
	Log.debug("browser", "starting DOM refreshing");
	if(state != WebState.SUCCEEDED)
	    return false;
	if(page.isBusy())
	{
	    Log.warning("browser", "trying to refresh DOM with busy browser");
	    return false;
	}
	page.RescanDOM();
	elementsForScan=page.iterator();
	textSelectorInvisible=page.selectorText(false,null);
	wDoc = new WebDocument();
	wDoc.make(page);
	//wDoc.getRoot().print(1,true);
	element = wDoc.getRoot();
	complexMode=false;
	refill();
	Log.debug("browser", "DOM refreshed");
	return true;
    }

    /**Checks if the browser has valid loaded page
     *
     * @return true if there is any successfully loaded page, false otherwise
     */ 
    boolean isEmpty()
    {
	return false;
    }

    /**Checks if the browser is doing any background work (usually fetching
     * and loading the pages). In this state the browser has very limited
     * functionality, but allows user navigation over the previously loaded
     * page.
     *
     * @return true if the browser is busy with background tasks, false otherwise
     */
    boolean isBusy()
    {
	return page.isBusy();
    }


    @Override public String getAreaName()
    {
	return page.getTitle()+" "+state.name()+" "+progress;
    }

    @Override public int getLineCount()
    {
	return wView.getLineCount();
    }

    @Override public String getLine(int index)
    {
	return wView.getLineByIndex(index);
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
		    return refresh();
		case F6: 
		    return onOpenUrl();
		case ALTERNATIVE_ARROW_LEFT:
			return onElementNavigateLeft();
		case ALTERNATIVE_ARROW_RIGHT:
			return onElementNavigateRight();
		case ENTER:
			return onClick();
		case BACKSPACE:
			return onHistoryBack();
		case F10:
			onChangeWebViewVisibility();
			return true;
		case F9:
			BigSearcherTest.main(wDoc,luwrain);
			return true;
		default:
			break;
		}
		if(event.getChar()==' ')
		{
			WebElementPart part=wView.getElementByPos(getHotPointX(),getHotPointY());
			if(part!=null)
			    environment.say(part.toString());
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
	NullCheck.notNull(event, "event");
	if (event instanceof PromptEvent)
	{
	    Log.debug("web","t:"+Thread.currentThread().getId()+" PROMPT event inside area");
	    final PromptEvent prompt = (PromptEvent)event;
	    final String answer = onPrompt(prompt.message(), prompt.value());
	    prompt.setAnswer (answer);
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
	    if(wView.getLineCount()==0) return true;
	    //if(wView.getLinesCount()<=getHotPointY()) return true;
	    final Vector<WebElementPart> line=wView.getPartsByLineIndex(getHotPointY());
	    if(line==null||line.size()==0) return true;
	    scanPos=line.get(0).element.getElement().getPos();
	    if(elementsForScan.isChangedAround(textSelectorInvisible,scanPos,PAGE_SCANNER_AROUND_ELEMENTS_COUNT))
	    { // detected changes, add event to rescan page dom
		refresh();
	    }
	    return true;
	}
	return false;
    }

    void onPageChangeState(WebState state)
    {
	NullCheck.notNull(state, "state");
	this.state=state;
	switch(state)
	{
	case SCHEDULED:
	    return;
	case RUNNING:
	    luwrain.message("Загрузка страницы");
	    return;
	case SUCCEEDED:
	    refresh();
	    //	    luwrain.message("Страница загружена", Luwrain.MESSAGE_DONE);
	    luwrain.message(page.getTitle(), Luwrain.MESSAGE_DONE);
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

    void onProgress(Number progress)
    {
	NullCheck.notNull(progress, "progress");
	this.progress = (int)(progress==null?0:Math.floor(progress.doubleValue()*100));
    }

    void onAlert(final String message)
    {
	NullCheck.notNull(message, "message");
	if (message.trim().isEmpty())
	    return;
	luwrain.message("Внимание!" + message, Luwrain.MESSAGE_OK);
	}

	private String onPrompt(String message, String value)
	{
		if (message.trim().isEmpty())
return null;
		luwrain.message("Выбор: " +message, Luwrain.MESSAGE_OK);
		return "";//result;
	}

    void onError(String message)
	{
	    NullCheck.notNull(message, "message");
	    if (message.trim().isEmpty())
return;
   		luwrain.message (message, Luwrain.MESSAGE_ERROR);
	}

    void onDownloadStart(String url)
    {
	//FIXME:
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
				if(y==wView.getLineCount()-1)
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
				if(y>=wView.getLineCount())
				{
					y=wView.getLineCount()-1;
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

    private boolean onOpenUrl()
    {
	if (page.isBusy())
	    return false;
		String link = Popups.simple(luwrain, "Открыть страницу", "Введите адрес страницы:", "http://");
		if(link==null || link.trim().isEmpty())
		    return true;
		if(!link.matches("^(http|https|ftp)://.*$"))
		    link="http://"+link;
		Log.debug("browser", "loading URL " + link);
		page.load(link);
		environment.onAreaNewContent(this);
		return true;
    }

	private boolean onElementNavigateLeft()
	{ // prev
		WebElementPart part=wView.getElementByPos(getHotPointX(),getHotPointY());
		Vector<WebElementPart> line=wView.getPartsByLineIndex(getHotPointY());
		if(part==null||line==null) return false;
		int idx=line.indexOf(part);
		if(idx==0)
		{ // move previous line
			if(getHotPointY()==0) return false;
			line=wView.getPartsByLineIndex(getHotPointY()-1);
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
		final Vector<WebElementPart> line=wView.getPartsByLineIndex(getHotPointY());
		if(part==null||line==null) return false;
		int idx=line.indexOf(part);
		if(idx==line.size()-1)
		{ // move next line
			if(getHotPointY()+1==wView.getLineCount()) return false;
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

    /**Performs some default action relevant to the element under the hot
     * point. What exact action will be done is dependent on the type of the
     * element under the hot point. If there is a form input edit, the user
     * will get an offer to enter some text. If there is a form list, the
     * user will get a popup to choose some item from the list and so
     * on. This operation may be performed only if the browser is free
     * (meaning, not empty and not busy).
     *
     * @return true if the operation has been done, false otherwise (usually the browser is busy or doesn't have a loaded page)
     */
    private boolean onClick()
    {
	if (isEmpty() || isBusy())
	    return false;
	final WebElementPart part=wView.getElementByPos(getHotPointX(),getHotPointY());
	if(part==null)
	    return false;
	if(part.element.needToBeComplex()||complexMode)
	{ // select complex element as base for view in navigation area
	    // store prev element to history
	    elementHistory.add(new HistoryElement(element,complexMode));
	    complexMode = !complexMode;
	    element = part.element;
	    final WebViewBuilder builder = WebViewBuilder.newBuilder(complexMode?WebViewBuilder.Type.COMPLEX:WebViewBuilder.Type.NORMAL, element,luwrain.getAreaVisibleWidth(this));
	    wView = builder.build();
	    repairHotPoint();
	    environment.onAreaNewContent(this);
	    return true;
	}
	if(part.element.getElement().isEditable())
	{ // editable element, edit it
	    if(part.element instanceof WebRadio||part.element instanceof WebCheckbox)
		return emulateClick(part);
	    if(part.element instanceof WebSelect)
		return onFormSelectFromList(part);
	    return onFormEditText(part);
	}
	return emulateClick(part);
    }

    /**Asks the browser core to emulate the action which looks like the user
     * clicks on the given element. This operation may be performed only if
     * the browser is free (meaning, not empty and not busy).
     *
     * @param part The element to emulate click on
     * @return true if the operation has been done, false otherwise (usually the browser is busy or doesn't have a loaded page)
     */
    private boolean emulateClick(WebElementPart part)
    {
	NullCheck.notNull(part, "part");
	if (isEmpty() || isBusy())
	    return false;
	part.element.getElement().clickEmulate();
	onTimerElementScan();
	//pageScanner.fast();
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
		final WebViewBuilder builder = WebViewBuilder.newBuilder(complexMode?WebViewBuilder.Type.COMPLEX:WebViewBuilder.Type.NORMAL, element, luwrain.getAreaVisibleWidth(this));
		wView = builder.build();
		//**/wView.print();
		repairHotPoint();
		//
		WebElementPart part=wView.getElementByPos(getHotPointX(),getHotPointY());
		if(part!=null)
		    environment.say(part.toString());
		environment.onAreaNewContent(this);
		luwrain.onAreaNewContent(this);
	}

    private boolean onFormEditText(WebElementPart part)
    {
	NullCheck.notNull(part, "part");
	if (isEmpty() || isBusy())
	    return false;
	final ElementIterator e = part.element.getElement();
	String oldValue = e.getText();
	if (oldValue == null)
	    oldValue = "";
	String newValue = Popups.simple(luwrain, "Редактирование формы", "Введите новое значение текстового поля формы:", oldValue);
	if (newValue == null) 
	    return true;
	e.setText(newValue);
	refill();
	return true;
    }

    private boolean onFormSelectFromList(WebElementPart part)
    {
	NullCheck.notNull(part, "part");
	if (isEmpty() || isBusy())
	    return false;
	final ElementIterator e=part.element.getElement();
	final String[] listValues = e.getMultipleText();
	if (listValues.length==0) 
	    return true; // FIXME:
	EditListPopup popup=new EditListPopup(luwrain,
					      new EditListPopupUtils.FixedModel(listValues),
					      "Редактирование формы","Выберите значение из списка",e.getText(), Popups.DEFAULT_POPUP_FLAGS);
	luwrain.popup(popup);
	if(popup.closing.cancelled()) 
	    return true;
	e.setText(popup.text());
	refill();
	return true;
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
			if(e instanceof WebListItem)
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
