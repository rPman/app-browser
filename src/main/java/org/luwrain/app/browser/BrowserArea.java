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

import java.net.URL;
import java.util.Vector;

import org.luwrain.core.*;
import org.luwrain.core.events.*;
import org.luwrain.controls.*;
import org.luwrain.popups.*;

//import org.luwrain.app.browser.Events;
import org.luwrain.browser.*;
import org.luwrain.app.browser.web.*;

import org.luwrain.browser.Events.WebState;

class BrowserArea implements Area
{
    static private final int PAGE_SCANNER_AROUND_ELEMENTS_COUNT=10; 

    private final Luwrain luwrain;
    private final Callback callback;
    private final ControlEnvironment environment;
    //    private final Actions actions;

    private final Browser page;
    private Events events;

    private WebState state = WebState.READY;
    private int progress = 0;

    private WebDocument doc = new WebDocument();
    private WebView view = new WebView();
    private WebElement current;

    private final Vector<HistoryElement> elementHistory = new Vector<HistoryElement>();
    private boolean complexMode = false;

	private final AutoPageElementScanner pageScanner;
    private ElementIterator elementsForScan = null;
    private SelectorText textSelectorInvisible = null;

	private int scanPos = -1;

    BrowserArea(Luwrain luwrain, Callback callback, Browser browser)
    {
	NullCheck.notNull(luwrain, "luwrain");
	NullCheck.notNull(callback, "callback");
	NullCheck.notNull(browser, "browser");
	this.luwrain = luwrain;
	this.callback = callback;
	this.environment = new DefaultControlEnvironment(luwrain);
	this.page = browser;
	events = new Events(luwrain, this);
	this.page.init(events);
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
	if(isEmpty() || isBusy())
	    return false;
	page.RescanDOM();
	elementsForScan=page.iterator();
	textSelectorInvisible=page.selectorText(false,null);
	doc = new WebDocument();
	doc.make(page);
	current = doc.getRoot();
	complexMode = false;
	refill();
	Log.debug("browser", "DOM refreshed successfully");
	return true;
    }

    /**Checks if the browser has valid loaded page
     *
     * @return true if there is any successfully loaded page, false otherwise
     */ 
    boolean isEmpty()
    {
	return state != WebState.SUCCEEDED;
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

    boolean open(URL url)
    {
	NullCheck.notNull(url, "url");
	if (page.isBusy())
	    return false;
	Log.debug("browser", "opening URL " + url.toString());
	    page.load(url.toString());
	environment.onAreaNewContent(this);
	return true;
    }

    @Override public boolean onAreaQuery(AreaQuery query)
    {
	return false;
    }

    @Override public String getAreaName()
    {
	return page.getTitle()+" "+state.name()+" "+progress;
    }

    @Override public int getHotPointX()
    {
	return view.getPosX();
    }

    @Override public int getHotPointY()
    {
	return view.getPosY();
    }

    @Override public int getLineCount()
    {
	return view.getLineCount();
    }

    @Override public String getLine(int index)
    {
	return view.getLineByIndex(index);
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
		return onEscape();
	    case ARROW_LEFT:
		return onArrowLeft(event);
	    case ARROW_RIGHT:
		return onArrowRight(event);
	    case ARROW_DOWN:
		return onArrowDown(event);
	    case ARROW_UP:
		return onArrowUp(event);
	    case ALTERNATIVE_ARROW_LEFT:
		return onAlternateLeft(event);
	    case ALTERNATIVE_ARROW_RIGHT:
		return onAlternateRight(event);
	    case ENTER:
		return onClick();
	    case BACKSPACE:
		return onBackspace();
	    case F10:
		onChangeWebViewVisibility();
		return true;
	    case F9:
		BigSearcherTest.main(doc,luwrain);
		return true;
	    }
	if(!event.isSpecial() && event.getChar()==' ')
	{
	    WebElementPart part = view.getElementByPos(getHotPointX(),getHotPointY());
	    if(part!=null)
		environment.say(part.toString());
	}
	return false;
    }

	@Override public boolean onEnvironmentEvent(EnvironmentEvent event)
	{
		NullCheck.notNull(event, "event");
		switch(event.getCode())
		{
		case REFRESH:
		    refresh();
		    return true;
		case THREAD_SYNC:
			if (onThreadSyncEvent(event))
				return true;
			return false;
		default:
		    return false;
		}
	}

    @Override public Action[]getAreaActions()
    {
	return new Action[0];
    }

    void onPageChangeState(WebState state)
    {
	NullCheck.notNull(state, "state");
	Log.debug("browser", "new page state:" + state);
	this.state = state;
	switch(state)
	{
	case RUNNING:
	    callback.onBrowserRunning();
	    return;
	case SUCCEEDED:
	    refresh();
	    callback.onBrowserSuccess(page.getTitle());
	    return;
	case FAILED:
	    callback.onBrowserFailed();
	    return;
	case CANCELLED:
	case READY:
	case SCHEDULED:
	    return;
	default:
	    Log.warning("browser", "unexpected new page state:" + state);
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
	    if(view.getLineCount()==0) return true;
	    //if(wView.getLinesCount()<=getHotPointY()) return true;
	    final WebElementPart[] line=view.getPartsOnLine(getHotPointY());
	    if(line == null || line.length == 0) 
	    	return true;
	    scanPos=line[0].element.getElement().getPos();
	    if(elementsForScan.isChangedAround(textSelectorInvisible,scanPos,PAGE_SCANNER_AROUND_ELEMENTS_COUNT))
	    { // detected changes, add event to rescan page dom
		refresh();
	    }
	    return true;
	}
	return false;
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

	protected boolean onEscape()
    {
	if (!isBusy())
	    return false;
	Log.debug("browser", "trying to cancel loading");
	page.stop();
	return true;
    }

    protected boolean onArrowLeft(KeyboardEvent event)
    {
	if (noContent())
	    return true;
	if(view.moveLeftByChar())
		return true;
	luwrain.onAreaNewHotPoint(this);
	// say current line full
	char letter = getLine(view.getPosY()).charAt(view.getPosX());
	luwrain.sayLetter(letter);
	return false;
    }

    protected boolean onArrowRight(KeyboardEvent event)
    {
	if (noContent())
	    return true;
	if(view.moveRightByChar())
		return true;
	luwrain.onAreaNewHotPoint(this);
	// say current line full
	char letter = getLine(view.getPosY()).charAt(view.getPosX());
	luwrain.sayLetter(letter);
	return false;
    }

    private boolean onAlternateRight(KeyboardEvent event)
	{
   	if (noContent())
   	    return true;
	if(view.moveToNextPart())
		return true;
	luwrain.onAreaNewHotPoint(this);
	// say current line full
	WebElementPart part=view.getElementByPos(view.getPosX(),view.getPosY());
	String text=part.toString();
	if(text.isEmpty())
		environment.hint(Hints.EMPTY_LINE);
	else
		luwrain.say(text);
	return false;
	}

	private boolean onAlternateLeft(KeyboardEvent event)
	{
	if (noContent())
	    return true;
	if(view.moveToPrevPart())
		return true;
	luwrain.onAreaNewHotPoint(this);
	// say current line full
	WebElementPart part=view.getElementByPos(view.getPosX(),view.getPosY());
	String text=part.toString();
	if(text.isEmpty())
		environment.hint(Hints.EMPTY_LINE);
	else
		luwrain.say(text);
	return false;
	}

    protected boolean onArrowUp(KeyboardEvent event)
    {
	if (noContent())
	    return true;
	if(view.movePrevLine())
		return true;
	luwrain.onAreaNewHotPoint(this);
	// say current line full
	String text=getLine(view.getPosY());
	if(text.isEmpty())
		environment.hint(Hints.EMPTY_LINE);
	else
		luwrain.say(text);
	return false;
    }

    protected boolean onArrowDown(KeyboardEvent event)
    {
	if (noContent())
	    return true;
	if(view.moveNextLine())
		return true;
	luwrain.onAreaNewHotPoint(this);
	// say current line full
	String text=getLine(view.getPosY());
	if(text.isEmpty())
		environment.hint(Hints.EMPTY_LINE);
	else
		luwrain.say(text);
	return false;
    }

    private void fixHotPoint()
    {
		int x=getHotPointX(),y=getHotPointY();
		WebElementPart part=null;
		while(true)
		{ // loop try to select eny element (under cursor, last, first)
			part = view.getElementByPos(x,y);
			if(part==null)
			{
				// last try?
				if(y==0&&x==0) break;
				// we try to select first element in the last line
				if(y==view.getLineCount()-1)
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
				if(y >= view.getLineCount())
				{
					y = view.getLineCount()-1;
					continue;
				} else
				{
					x=0;
					break;
				}
			} else
				break;
		}
		//		setHotPoint(x,y);
    }

    /*
	private boolean onElementNavigateLeft()
	{ // prev
		final WebElementPart part = view.getElementByPos(getHotPointX(),getHotPointY());
Vector<WebElementPart> line = view.getPartsByLineIndex(getHotPointY());
		if(part==null||line==null) 
return false;
		int idx = line.indexOf(part);
		if(idx==0)
		{ // move previous line
			if(getHotPointY()==0) 
return false;
			line = view.getPartsByLineIndex(getHotPointY()-1);
			//			setHotPoint(line.lastElement().pos,getHotPointY()-1);
		} else
		{ // move inside line
		    //			setHotPoint(line.get(idx-1).pos,getHotPointY());
		}
  		onNewSelectedElement();
  		environment.onAreaNewContent(this);
  		return true;
	}
    */

    /*
	private boolean onElementNavigateRight()
	{ // next
		WebElementPart part = view.getElementByPos(getHotPointX(),getHotPointY());
		final Vector<WebElementPart> line = view.getPartsByLineIndex(getHotPointY());
		if(part==null||line==null) return false;
		int idx=line.indexOf(part);
		if(idx==line.size()-1)
		{ // move next line
			if(getHotPointY() + 1 == view.getLineCount()) return false;
			//			setHotPoint(0,getHotPointY()+1);
		} else
		{ // move inside line
		    //			setHotPoint(line.get(idx+1).pos,getHotPointY());
		}
  		onNewSelectedElement();
  		environment.onAreaNewContent(this);
  		return true;
	}
    */

	private void  onNewSelectedElement()
	{
		WebElementPart part = view.getElementByPos(getHotPointX(),getHotPointY());
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
	final WebElementPart part = view.getElementByPos(getHotPointX(),getHotPointY());
	if(part==null)
	    return false;
	if(part.element.needToBeComplex()||complexMode)
	{ // select complex element as base for view in navigation area
	    // store prev element to history
	    elementHistory.add(new HistoryElement(current,complexMode));
	    complexMode = !complexMode;
	    current = part.element;
	    final WebViewBuilder builder = WebViewBuilder.newBuilder(complexMode?WebViewBuilder.Type.COMPLEX:WebViewBuilder.Type.NORMAL, current,luwrain.getAreaVisibleWidth(this));
	    view = builder.build();
	    fixHotPoint();
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

    protected boolean onBackspace()
    {
	if(elementHistory.isEmpty()) return false;
	HistoryElement h=elementHistory.lastElement();
	elementHistory.remove(elementHistory.size()-1);
	complexMode=h.mode;
	current = h.element;
	refill();
	return true;
    }

	private void refill()
	{
	    final WebViewBuilder builder = WebViewBuilder.newBuilder(complexMode?WebViewBuilder.Type.COMPLEX:WebViewBuilder.Type.NORMAL, current, luwrain.getAreaVisibleWidth(this));
	    view = builder.build();
		fixHotPoint();
		WebElementPart part = view.getElementByPos(getHotPointX(),getHotPointY());
		if(part!=null)
		    environment.say(part.toString());
		environment.onAreaNewContent(this);
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
		WebElementPart part = view.getElementByPos(getHotPointX(),getHotPointY());
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

    protected void noContentMsg()
    {
	environment.hint(Hints.NO_CONTENT);
    }

    protected boolean noContent()
    {
	if (isEmpty())
	{
	    noContentMsg();
	    return true;
	}
	return false;
    }

interface Callback
{
    void onBrowserRunning();
    void onBrowserSuccess(String title);
    void onBrowserFailed();
}
}
