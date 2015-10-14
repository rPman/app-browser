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

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Date;
import java.util.Objects;
import java.util.Vector;



import javafx.concurrent.Worker.State;

import org.luwrain.browser.Browser;
import org.luwrain.browser.BrowserEvents;
import org.luwrain.core.*;
import org.luwrain.core.events.*;
import org.luwrain.controls.*;
import org.luwrain.popups.*;
//import org.luwrain.interaction.browser.ElementListImpl;
import org.luwrain.interaction.browser.WebPage;
import org.luwrain.browser.ElementList;
import org.luwrain.browser.ElementList.*;

/* Simple representation of web browser
* Screen modes:
* 1. TEXT - screen contains info about current web page element in current web element list 
* 2. PAGE - screen contains current page info, state, progress, title, sizes and so on
* 3. ELEMENT - similar TEXT but show html info about current element except text 
* 
* Any time user can use keyboard shortcuts to...
* 1. <Shift>+F1/F2/F3	change screen mode to PAGE/TEXT/DOWNLOAD
* 2. <ESC>	stop page loading
* 3. <F6>		open new web address (also stop previous page loading) - open PAGE mode 
* 4. make new element list selector (each type has own keyboard shortcuts) with current position on page
*    a. <F2>	ALL - open all selector
*    b. <F3>/<Shift>+F3	TEXT - open text selector, with <Shift> show edit input for filter and make new
*    c. <F4>/<Shift>+F4	TAG - open tag selector, with <Shift> show edit input for tag name and filter (single line tagname=value or simple tagname)
*    d. <F5>/<Shift>+F5	CSS - open css selector, with <Shift> show edit input for css selector name and filter (single line stylename=value or simple stylename)
* 5. <F1>		change visibility flag for selector
* 6. <F8>		delete current selector
* 8. <Shift>+LEFT/RIGHT	navigate using current selector without filter
* 8. TAB/<Ctrl>+TAB	navigate using current selector with current filter (if exist or without)
* 9. <Shift>+UP/DOWN/HOME/END	change element position on page with current selector
* 10. <ENTER>
*     in PAGE mode change mode to TEXT 
*     in TEXT/ELEMENT screen modes simulate mouse click,
*     but for input and text edit elements open text editor (browser PROMPT event)
*     and for chckbox and radiobutton - change value (off/on round robin)
*     ?? how to work with tristate checkboxes ??
* 11. <F9>		show/hide browser window (for visible browsing by mouse and keyboard)
* any time browser message can be happend - it is show modal window and block using browser, ESC - cancel, ENTER - accept, text can be entered, if it need
* 
* Screen modes window format:
* 1. PAGE, each line contains own info, first word - type, all text after - value, value can be multiline
* url: current url link of page (multiline)
* title: multiline title, if page loaded or empty
* state: name of enum javafx.concurrent.Worker.State (READE, RUNNING,...)
* progress: current progress integer value as %
* size: current text/html page size (in bytes, in future use human readable format)
* ?? when state changed, message about it can be happend ??
* 
* 2. TEXT:
* first line - element type (text, form element (text area/input, select, checkbox and button), link,..) 
* all other line - text content (or readable element value)
* ?? todo, make methods to show ALT tips text ??
* 
* 3. DOWNLOAD, each line:
* file name
* progress
* multiline - link of download
*/
class BrowserArea extends NavigateArea implements Constants
{
    enum ScreenMode {
	PAGE,
	TEXT,
	DOWNLOAD
    };

    private final ScreenPage screenPage=new ScreenPage();
    private final ScreenText screenText=new ScreenText();

    private Luwrain luwrain;
    private ControlEnvironment environment;
    private Actions actions;
    private WebPage page;
    private BrowserEvents browserEvents;
    private ScreenMode screenMode=ScreenMode.PAGE;
    private ElementList elements=null;

    private SelectorTEXT textSelectorEmpty=null;
    private SelectorTEXT textSelectorFiltered=null;
    private SelectorTAG tagSelectorEmpty=null;
    private SelectorTAG tagSelectorFiltered=null;
    private SelectorCSS cssSelectorEmpty=null;
    private SelectorCSS cssSelectorFiltered=null;
    private Selector currentSelectorEmpty=null;
    private Selector currentSelectorFiltered=null;
    
	String nextPromptresult=null;

    private final FileDownloadThread fileDownloadThread=new FileDownloadThread();
    private final ScreenDownload screenDownload=new ScreenDownload(fileDownloadThread);

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

	    //String[] res=WebElementList.splitTextForScreen(10,"test\ntest");
    }

    private void fillCurrentElementInfo()
    {
    	if(currentSelectorEmpty==null)
    	{
	    screenText.setType("");
	    screenText.setText("");
	    screenText.setLink("");
    	} else
    	{
	    String type=elements.getType();
	    String text=elements.getText();
	    String link=elements.getLink();
	    screenText.setType(type);
	    screenText.setText(text);
	    if(link!=null) screenText.setLink(link);
	    environment.say(type+". "+text);
    	}
    }

    @Override public int getLineCount()
    {
    	switch(screenMode)
    	{
	case PAGE: 
	    return screenPage.getLinesCount();
	case TEXT:
		return elements.getSplittedCount();
	    //return screenText.getLinesCount();
	case 
		DOWNLOAD: return screenDownload.getLinesCount();
	default:
	    return 0;
    	}
    }

    @Override public String getLine(int index)
    {
    	switch(screenMode)
    	{
	case PAGE:
	    return screenPage.getStringByLine(index);
	case TEXT:
	{
		SplittedLine split=elements.getSplittedLineByIndex(index);
		return split.type+" "+split.text;
	    //return screenText.getStringByLine(index);
	}
	case DOWNLOAD:
	    return screenDownload.getStringByLine(index);
	default:
	    return "";
    	}
    }

    @Override public String getAreaName()
    {
    	String translatedModeName;
    	switch(screenMode)
    	{
	case PAGE:
	    translatedModeName=PAGE_ANY_SCREENMODE_PAGE;break;
	case TEXT:
	    translatedModeName=PAGE_ANY_SCREENMODE_TEXT;break;
	case DOWNLOAD:
	    translatedModeName=PAGE_ANY_SCREENMODE_DOWNLOAD;break;
	default:
	    translatedModeName=screenMode.name();break;
    	}
    	return page.getBrowserTitle()+" "+translatedModeName;
    }

    @Override public boolean onKeyboardEvent(KeyboardEvent event)
    {
    	NullCheck.notNull(event, "event");
    	if (event.isCommand())
	    switch (event.getCommand())
	    {
		    case KeyboardEvent.ESCAPE:
			onBreakCommand();
			return true;
		    case KeyboardEvent.F5:
			onChangeTagFilters();
			return true;
		    case KeyboardEvent.F6: 
			onChangeCurrentPageLink();
			return true;
		    case KeyboardEvent.F7: 
			onChangeScreenModeToText();
			return true;
		    case KeyboardEvent.F8: 
			onChangeScreenModeToPage();
			return true;
		    case KeyboardEvent.F9: 
	    	onChangeDefaultEventResult();
			return true;
		    case KeyboardEvent.F11:
			onChangeWebViewVisibility();
			return true;
	    		// navigation
		    case KeyboardEvent.ARROW_LEFT:
		    case KeyboardEvent.ALTERNATIVE_ARROW_LEFT:
			if(event.withShiftOnly()) 
			{
			    onElementNavigateLeft();
			    return true;
			}
			break;
		    case KeyboardEvent.ARROW_RIGHT:
		    case KeyboardEvent.ALTERNATIVE_ARROW_RIGHT:
			if(event.withShiftOnly()) 
			{
			    onElementNavigateRight();
			    return true;
			}
	    		break;
	    		// filtered navigation
		    case KeyboardEvent.TAB:
			if(event.withShiftOnly())
			{
			    onSearchResultNavigationLeft();
			    return true;
			} else 
			    if(!event.withAlt()&&!event.withControl()&&!event.withShift()) 
			    {
				onSearchResultNavigationRight();
				return true;
			    }
			// actions
		    case KeyboardEvent.ENTER:
			onDefaultAction();
			return true;
		    default:
			return super.onKeyboardEvent(event);
	    }

    	switch(event.getCharacter())
    	{
	    // Ctrl+F or '/' call text search
	case 'f':
	    if(!event.withControlOnly()) 
		return true; 
	case '/':
	    onChangeTextFilter();
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
    	switch(screenMode)
    	{
		case DOWNLOAD:  
		case PAGE:
			page.stop();
		break;  
		case TEXT:
			screenDownload.breakExecution();
	    break;  
    	}
    }

    private void onChangeScreenModeToText()
    {
	screenMode=ScreenMode.TEXT;
	fillCurrentElementInfo();
	environment.onAreaNewContent(this);
    }

    void onChangeScreenModeToPage()
    {
	screenMode=ScreenMode.PAGE;
	environment.onAreaNewContent(this);
    }

    private void onChangeTextFilter()
    {
		environment.say(PAGE_ANY_PROMPT_TEXT_FILTER);
		//FIXME: Ask for new filter;
		String filter = Popups.simple(luwrain, POPUP_TITLE_CHANGE_TEXT_FILTER, PAGE_ANY_PROMPT_TEXT_FILTER, "");
		if(filter==null)
		    return;
		if(filter.isEmpty())
		    filter=null;
		// make new selector
		textSelectorFiltered=page.selectorTEXT(true,filter);
		currentSelectorFiltered=textSelectorFiltered;
		currentSelectorEmpty=textSelectorEmpty; // current empty selector also seto to text
		if(!textSelectorFiltered.first(elements))
		{ // not found
		    environment.say(PAGE_SCREEN_ANY_HAVENO_ELEMENT);
		} else
		{ // element found
		    // change screen mode to TEXT
		    screenMode=ScreenMode.TEXT;
		    moveHotPointYToElement();
		    fillCurrentElementInfo();
		    environment.onAreaNewContent(this);
		}
    }

    private void onChangeTagFilters()
    {
	environment.say(PAGE_ANY_PROMPT_TAGFILTER_NAME);
	//FIXME:Ask for new filter;
	String filter = Popups.simple(luwrain, POPUP_TITLE_CHANGE_TAG_FILTER, PAGE_ANY_PROMPT_TAGFILTER_NAME, "");
	if(filter==null)
	    return;
	if(filter.isEmpty())
	    filter=null;
	environment.say(PAGE_ANY_PROMPT_TAGFILTER_ATTR);
	//FIXME:Ask for new attr name;
	String attrName = Popups.simple(luwrain, POPUP_TITLE_CHANGE_TAG_FILTER, PAGE_ANY_PROMPT_TAGFILTER_ATTR, "");
	//FIXME:Ask for new attr;		*/
	if(attrName==null)
	    return;
	if(attrName.isEmpty()) 
	    attrName=null;
	environment.say(PAGE_ANY_PROMPT_TAGFILTER_VALUE);
	//FIXME:Ask for new attr value;
	String attrValue = Popups.simple(luwrain, POPUP_TITLE_CHANGE_TAG_FILTER, PAGE_ANY_PROMPT_TAGFILTER_VALUE, "");
	if(attrValue==null)
	    return;
	if(attrValue.isEmpty())
	    attrValue=null;
	// make new selector
	tagSelectorFiltered=page.selectorTAG(true,filter,attrName,attrValue);
	currentSelectorFiltered=tagSelectorFiltered;
	if(!textSelectorFiltered.first(elements))
	{ // not found
	    environment.say(PAGE_SCREEN_ANY_HAVENO_ELEMENT);
	} else
	{ // element found
	    // change screen mode to TEXT
	    screenMode=ScreenMode.TEXT;
	    fillCurrentElementInfo();
	    environment.onAreaNewContent(this);
	}
    }

    private void onChangeScreenModeToDownload()
    { // control pressed
	screenMode=ScreenMode.DOWNLOAD;
	screenDownload.refreshInfo();
    }
    
    private void onChangeDefaultEventResult()
    {
    	nextPromptresult = Popups.simple(luwrain, "События вебсценариев", "Укажите ответ на следующее событие вебсценариев prompt/confirm", "");
    }

    private void onChangeCurrentPageLink()
    {
	String link = Popups.simple(luwrain, "Открыть страницу", PAGE_ANY_PROMPT_ADDRESS, "ya.ru");
	if(link==null) 
	    return;
	if(!link.matches("^(http|https|ftp)://.*$"))
	    link="http://"+link;
	page.load(link);
	screenPage.changedUrl(link);
	screenMode=ScreenMode.PAGE;
	environment.onAreaNewContent(this);
    }

    private void onChangeWebViewVisibility()
    {
	page.setVisibility(!page.getVisibility());
    }

    private void onElementNavigateLeft()
    { // prev
	if(currentSelectorEmpty==null)
	    return; // dev bug, if it happend
	if(!currentSelectorEmpty.prev(elements))
	{
	    environment.say(PAGE_SCREEN_ANY_FIRST_ELEMENT);
	    return;
	}
	fillCurrentElementInfo();
	environment.onAreaNewContent(this);
    }

    private void onElementNavigateRight()
    { // next
		if(currentSelectorEmpty==null) 
		    return; // dev bug, if it happend
		if(!currentSelectorEmpty.next(elements))
		{
			environment.say(PAGE_SCREEN_ANY_END_ELEMENT);
		    return;
		}
		fillCurrentElementInfo();
		environment.onAreaNewContent(this);
    }

    private void onSearchResultNavigationLeft()
    { // prev
		if(currentSelectorFiltered==null) 
		    return; // dev bug, if it happend
		if(!currentSelectorFiltered.prev(elements))
		{
		    environment.say(PAGE_SCREEN_ANY_FIRST_ELEMENT);
		    //return;
		}
		moveHotPointYToElement();
		fillCurrentElementInfo();
		environment.onAreaNewContent(this);
		return;
    }

    private void onSearchResultNavigationRight()
    { // next
	if(currentSelectorFiltered==null)
	    return; // dev bug, if it happend
	if(!currentSelectorFiltered.next(elements))
	{
	    environment.say(PAGE_SCREEN_ANY_END_ELEMENT);
	    //return;
	}
	moveHotPointYToElement();
	fillCurrentElementInfo();
	environment.onAreaNewContent(this);
    }
    
    private void moveHotPointYToElement()
    {
    	int pos=elements.getPos();
    	SplittedLine[] sls=elements.getSplittedLineByPos(pos);
    	int snum=sls[0].index;
    	setHotPointY(snum);
    	setHotPointX(0);
    }

    private void onDefaultAction()
    {
    	if(textSelectorEmpty==null) return;
    	SplittedLine sl=elements.getSplittedLineByIndex(getHotPointY());
    	boolean res=textSelectorEmpty.to(elements,sl.pos);
   		Log.debug("web","to:"+res+", hot:"+getHotPointY()+", pos:"+sl.pos);

   		if(elements.isEditable())
		{ // edit content
		    final String oldValue=Constants.defaultIfNull(elements.getText(),"");
		    final String newValue = Popups.simple(luwrain, POPUP_TITLE_CHANGE_ELEMENT_EDIT, PAGE_ANY_PROMPT_NEW_TEXT, oldValue);
		    if (newValue == null)
			return;
		    elements.setText(newValue);
		    fillCurrentElementInfo();
		    environment.onAreaNewContent(this);
		    return;
		}
		elements.clickEmulate();
    }

    private void onPageChangeState(State state)
    {
	Log.debug("browser", "PageChangeStateEvent received");
	screenPage.changedUrl("test");
	screenPage.changedTitle("title");
	screenPage.changedState(state.name());
	if(state==State.SUCCEEDED)
	{
		screenPage.changedTitle(page.getTitle());
	    screenPage.changedUrl(page.getUrl());
	    Date d1=new Date(); // debug rescan speed 
	    page.RescanDOM();
	    Date d2=new Date();
	    textSelectorEmpty=page.selectorTEXT(true,null);
	    elements.splitAllElementsTextToLines(TEXT_SCREEN_WIDTH,textSelectorEmpty);
	    Date d3=new Date();
	    Log.debug("web","Rescan "+(d2.getTime()-d1.getTime())+"ms, page element count:"+elements.getSplittedLines().length+", text splits:"+elements.getSplittedCount()+" "+(d3.getTime()-d2.getTime())+"ms");
	    
	    if(!textSelectorEmpty.first(elements))
	    {
	    	environment.say(PAGE_SCREEN_ANY_HAVENO_ELEMENT);
	    } else
	    {
	    	// empty page, have no eny text
	    }
	    currentSelectorEmpty=textSelectorEmpty;
	    screenMode=ScreenMode.PAGE;
	    environment.onAreaNewContent(this);
	    environment.say(PAGE_ANY_STATE_LOADED);
	} else
	{
	    environment.say(PAGE_ANY_STATE_CANCELED);
	    screenPage.changedTitle("");
	}
	environment.onAreaNewContent(this);
    }

    private void onProgress(Number progress)
    {
	screenPage.changedProgress((double)progress);
	environment.onAreaNewContent(this);
    }

    private void onAlert(final String message)
    {
		if (message == null || message.trim().isEmpty()) return;
		luwrain.message(PAGE_SCREEN_ALERT_MESSAGE+message, Luwrain.MESSAGE_OK);
    }

    private String onPrompt(final String message,final String value)
    {
		//YesNoPopup popup = new YesNoPopup(luwrain, POPUP_TITLE_WEB_MESSAGE,PAGE_SCREEN_ALERT_MESSAGE+message, false);
		//luwrain.popup(popup);
    	//return Popups.simple(luwrain, POPUP_TITLE_WEB_MESSAGE, PAGE_SCREEN_PROMPT_MESSAGE+message, value);
		if (message == null || message.trim().isEmpty()) return null;
		luwrain.message(PAGE_SCREEN_PROMPT_MESSAGE+message, Luwrain.MESSAGE_OK);
		String result=nextPromptresult;
    	nextPromptresult=null;
    	return result;
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
		//YesNoPopup popup = new YesNoPopup(luwrain, POPUP_TITLE_WEB_MESSAGE,PAGE_SCREEN_CONFIRM_MESSAGE+message, false);
		//luwrain.popup(popup);
		//if (popup.closing.cancelled()) return null;
		//return popup.result();
   		luwrain.message (PAGE_SCREEN_CONFIRM_MESSAGE+message, Luwrain.MESSAGE_OK);
		boolean result=nextPromptresult!=null&&(nextPromptresult.equals("y")||nextPromptresult.equals("Y"));
    	nextPromptresult=null;
    	return result;
    }
}
