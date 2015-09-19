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

import java.util.Vector;

import javax.swing.SwingUtilities;

import javafx.concurrent.Worker.State;

import org.luwrain.browser.Browser;
import org.luwrain.browser.BrowserEvents;
import org.luwrain.core.*;
import org.luwrain.core.events.*;
import org.luwrain.controls.*;
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
* 1. <Shift>+F1/F2/F3	change screen mode to PAGE/TEXT/ELEMENT
* 2. <ESC>	stop page loading
* 3. <F6>		open new web address (also stop previous page loading) - open PAGE mode 
* 4. make new element list selector (each type has own keyboard shortcuts) with current position on page
*    a. <F2>	ALL - open all selector
*    b. <F3>/<Shift>+F3	TEXT - open text selector, with <Shift> show edit input for filter and make new
*    c. <F4>/<Shift>+F3	TAG - open tag selector, with <Shift> show edit input for tag name and filter (single line tagname=value or simple tagname)
*    d. <F5>/<Shift>+F3	CSS - open css selector, with <Shift> show edit input for css selector name and filter (single line stylename=value or simple stylename)
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
*/

class BrowserArea extends NavigateArea
{
	static final String PAGE_SCREEN_TEXT_URL="ссылка ";
	static final String PAGE_SCREEN_TEXT_TITLE="заголовок ";
	static final String PAGE_SCREEN_TEXT_STATE="состояние ";
	static final String PAGE_SCREEN_TEXT__PROGRESS="процент загрузки ";
	static final String PAGE_SCREEN_TEXT_SIZE="размер ";
	static final String PAGE_SCREEN_PROMPT_MESSAGE="Запрос на ввод текста от вебстраницы";
	static final String PAGE_SCREEN_ALERT_MESSAGE="Сообщение от вебстраницы ";
	static final String PAGE_ANY_PROMPT_TEXT_FILTER="Введите строку для поиска текста";
	static final String PAGE_ANY_PROMPT_ADDRESS="Введите новый интернет адрес";
	static final String PAGE_ANY_PROMPT_NEW_TEXT="Введите новое значение для элемента";
	static final String PAGE_SCREEN_ANY_FIRST_ELEMENT="Начало списка элементов";
	static final String PAGE_SCREEN_ANY_END_ELEMENT="Конец списка элементов";
	static final String PAGE_SCREEN_ANY_HAVENO_ELEMENT="Элементы не найдены";

	static final int TEXT_SCREEN_WIDTH=100;
	
	private ControlEnvironment environment;
    private WebPage page;
    private BrowserEvents browserEvents;
    
    enum ScreenMode {PAGE,TEXT,HTML};
    private ScreenMode screenMode=ScreenMode.PAGE;
    
    // selectors
    SelectorTEXT textSelectorEmpty=null;
    SelectorTEXT textSelectorFiltered=null;
    SelectorTAG tagSelectorEmpty=null;
    SelectorTAG tagSelectorFiltered=null;
    SelectorCSS cssSelectorEmpty=null;
    SelectorCSS cssSelectorFiltered=null;
    
    Selector currentSelectorEmpty=null;
    Selector currentSelectorFiltered=null;
    
    ElementList elements=null;
    
    MessagesControl msgControl=new MessagesControl();
    
    class ScreenPage
    {
    	public String[] url=new String[1];
    	public int urlLine=0;
    	public String[] title=new String[1];
    	public int titleLine=1;
    	public String state="";
    	public int stateLine=2;
    	public String progress;
    	public int progressLine=3;
    	public String size="";
    	public int sizeLine=4;
    	public void changedUrl(String string)
    	{
    		url=splitTextForScreen(PAGE_SCREEN_TEXT_URL+string);
    		titleLine=   0+url.length;
    		stateLine=   0+url.length+title.length;
    		progressLine=1+url.length+title.length;
    		sizeLine=    2+url.length+title.length;
    	}
    	public void changedTitle(String string)
    	{
    		title=splitTextForScreen(PAGE_SCREEN_TEXT_TITLE+string);
    		stateLine=   0+url.length+title.length;
    		progressLine=1+url.length+title.length;
    		sizeLine=    2+url.length+title.length;
   		}
    	public void changedState(String string)
    	{
    		state=PAGE_SCREEN_TEXT_STATE+string;
   		}
    	public void changedProgress(double num)
    	{
    		progress=PAGE_SCREEN_TEXT__PROGRESS+Integer.toString((int)(num*100))+"%";
   		}
    	public void changedSize(int num)
    	{
    		size=PAGE_SCREEN_TEXT_SIZE+Integer.toString(num);
   		}
    	public int getLinesCount()
    	{
    		return sizeLine+1;
    	}
    	public String getStringByLine(int line)
    	{
    		if(line>=urlLine&&line<urlLine+url.length)
   			{
    			int subline=line-urlLine;
    			return url[subline];
   			} else
    		if(line>=titleLine&&line<titleLine+title.length)
    		{
    			int subline=line-titleLine;
    			return title[subline];
    		} else
    		if(line==stateLine) return state;else
    		if(line==progressLine) return progress;else
    		if(line==sizeLine) return size;
    		return "";
    	}
    }
    ScreenPage screenPage=new ScreenPage();
    
    
    String[] splitTextForScreen(String string)
    {
    	Vector<String> text=new Vector<String>();
		if(string==null||string.isEmpty()) return text.toArray(new String[(text.size())]);
		int i=0;
		while(i<string.length())
		{
			String line;
			if(i+TEXT_SCREEN_WIDTH>=string.length())
			{ // last part of string fit to the screen
				line=string.substring(i);
			} else
			{ // too long part
				line=string.substring(i,i+TEXT_SCREEN_WIDTH-1);
				// check for new line char
				int nl=line.indexOf('\n');
				if(nl!=-1)
				{ // have new line char, cut line to it
					line=line.substring(0,nl);
					i++; // skip new line
				} else
				{ // walk to first stopword char at end of line
					int sw=line.lastIndexOf(' ');
					if(sw!=-1)
					{ // have stop char, cut line to it (but include)
						line=line.substring(0,sw);
					}
				}
			}
			text.add(line);
			i+=line.length();
		}
		return text.toArray(new String[(text.size())]);
    }
    
    class ScreenText
    { // contains one first line with type of element, text multiline and multiline link
    	public String type="";
    	public String[] text=new String[1];
    	public int linkLine=2;
    	public String[] link=new String[0]; // it for anchor and images link
    	public void setType(String string)
    	{
    		type=string;
    	}
    	public void setLink(String string)
    	{
    		link=splitTextForScreen(string);
    	}
    	public void setText(String string)
    	{
    		text=splitTextForScreen(string);
    		linkLine=1+text.length;
    	}
    	public int getLinesCount()
    	{
    		return 1+text.length+link.length;
    	}
    	public String getStringByLine(int line)
    	{
    		// type
    		if(line==0) return type;
    		// text
    		if(line>=1&&line<1+text.length)
    		{
    			int subline=line-1;
    			return text[subline];
    		}
    		// link
			int subline=line-1-text.length;
			return link[subline];
    	}
    }
    ScreenText screenText=new ScreenText();
    
    void fillCurrentElementInfo()
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

    BrowserArea that;
    public BrowserArea(final ControlEnvironment environment, Browser browser)
	{
		super(environment);
		that=this;
		this.environment = environment;
		this.page = (WebPage)browser;
		if (environment == null)
			throw new NullPointerException("environment may not be null");
		if (browser == null)
			throw new NullPointerException("browser may not be null");
		
		browserEvents=new BrowserEvents()
    	{
			@Override public void onChangeState(State state)
			{
    			//screenPage.changedUrl("test");
    			//screenPage.changedTitle("title");
				screenPage.changedState(state.name());
				if(state==State.SUCCEEDED)
				{
					screenPage.changedTitle(page.getTitle());
	    			screenPage.changedUrl(page.getUrl());
					
					page.RescanDOM();
	    			
					textSelectorEmpty=page.selectorTEXT(true,null);
	    			if(!textSelectorEmpty.first(elements))
	    			{
	    				environment.say(PAGE_SCREEN_ANY_HAVENO_ELEMENT);
	    			}
	    			currentSelectorEmpty=textSelectorEmpty;

	    			screenMode=ScreenMode.PAGE;
	    			environment.onAreaNewContent(that);

	    			environment.say("Loaded");
				} else screenPage.changedTitle("");
    			environment.onAreaNewContent(that);
			}
			@Override public void onProgress(Number progress)
			{
				screenPage.changedProgress((double)progress);
    			environment.onAreaNewContent(that);
			}
			@Override public void onAlert(final String message)
			{
				SwingUtilities.invokeLater(new Runnable() { @Override public void run() {environment.say(PAGE_SCREEN_ALERT_MESSAGE+message);}});
    			MessagesControl.Alert alert=new MessagesControl.Alert(PAGE_SCREEN_ALERT_MESSAGE,message);
    			msgControl.messages.add(alert);
    			//try{ synchronized(alert){alert.wait();} } catch(InterruptedException e) {e.printStackTrace();}
    			synchronized(alert){msgControl.doit();}
    			alert.remove();
			}
			@Override public String onPrompt(final String message,final String value)
			{
				SwingUtilities.invokeLater(new Runnable() { @Override public void run() {environment.say(PAGE_SCREEN_PROMPT_MESSAGE+message);}});
    			MessagesControl.Prompt prompt=new MessagesControl.Prompt(PAGE_SCREEN_PROMPT_MESSAGE,"ya.ru");
    			msgControl.messages.add(prompt);
    			//try{ synchronized(prompt){prompt.wait();} } catch(InterruptedException e) {e.printStackTrace();}
    			synchronized(prompt){msgControl.doit();}
    			String result=prompt.result;
    			prompt.remove();
    			return result;
			}
			@Override public void onError(String message)
			{
				Log.warning("browser",message);
			}
		};
		this.page.init(browserEvents);
    	elements=page.elementList();
	}

    @Override public int getLineCount()
    {
    	switch(screenMode)
    	{
    		case PAGE: return screenPage.getLinesCount();
    		case TEXT: return screenText.getLinesCount();
    		case HTML: return 0;
    		default: return 0;
    	}
    }

    @Override public String getLine(int index)
    {
    	switch(screenMode)
    	{
    		case PAGE: return screenPage.getStringByLine(index);
    		case TEXT: return screenText.getStringByLine(index);
    		case HTML: return "";
    		default: return "";
    	}
    }

    @Override public String getAreaName()
    {
    	return page.getBrowserTitle()+" mode "+screenMode.name();
    }
    
    public boolean onKeyboardEvent(KeyboardEvent event)
    {
		Log.debug("webbrowser","alt:"+event.withAlt()+", ctrl"+event.withControl()+", shift:"+event.withShift());
    	switch (event.getCommand())
    	{
    		case KeyboardEvent.ESCAPE:
    		{
    			page.stop();
    			return true;
    		}
    		case KeyboardEvent.F3:
    		if(event.withControlOnly())
    		{ // control pressed
    			screenMode=ScreenMode.TEXT;
    			fillCurrentElementInfo();
    			environment.onAreaNewContent(that);
    			return true;
    		} else
    		{
    			return true;
    		}
    		case KeyboardEvent.F2:
       		if(event.withControlOnly())
       		{ // control pressed
       			screenMode=ScreenMode.PAGE;
       			environment.onAreaNewContent(that);
       			return true;
       		} else
    		if(event.withShiftOnly())
    		{
    			environment.say(PAGE_ANY_PROMPT_TEXT_FILTER);

    			MessagesControl.Prompt prompt=new MessagesControl.Prompt(PAGE_ANY_PROMPT_TEXT_FILTER,"");
    			msgControl.messages.add(prompt);
    			//try{ synchronized(prompt){prompt.wait();} } catch(InterruptedException e) {e.printStackTrace();}
    			synchronized(prompt){msgControl.doit();}
    			String filter=prompt.result;
    			prompt.remove();
    			
    			if(filter==null) return true;
    			if(filter.isEmpty()) filter=null;
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
    				fillCurrentElementInfo();
    				environment.onAreaNewContent(that);
    			}
    			return true;	
    		} else if(!event.withAlt()&&!event.withControl()&&!event.withShift())
    		{
    			currentSelectorEmpty=textSelectorEmpty;
    			// change screen mode to TEXT
    			screenMode=ScreenMode.TEXT;
    			fillCurrentElementInfo();
    			environment.onAreaNewContent(that);
    			return true;
    		}
    		case KeyboardEvent.F4:
        		if(event.withControlOnly())
        		{ // control pressed
        			screenMode=ScreenMode.HTML;
        			environment.onAreaNewContent(that);
        			return true;
        		} else
        		{
        			break;
        		}
    		case KeyboardEvent.F6:
    		{
    			//String link="http://rpserver/a.html";
    			//String link="http://ya.ru";
    			environment.say(PAGE_ANY_PROMPT_ADDRESS);

    			MessagesControl.Prompt prompt=new MessagesControl.Prompt(PAGE_ANY_PROMPT_ADDRESS,"rpserver");
    			msgControl.messages.add(prompt);
    			//try{ synchronized(prompt){prompt.wait();} } catch(InterruptedException e) {e.printStackTrace();}
    			synchronized(prompt){msgControl.doit();}
    			String link=prompt.result;
    			prompt.remove();
    			
    			if(link==null) return true;
    			if(!link.matches("^(http|https|ftp)://.*$")) link="http://"+link;
    			
    			page.load(link);
    			screenPage.changedUrl(link);
    			
    			screenMode=ScreenMode.PAGE;
    			
    			environment.onAreaNewContent(that);
    			return true;
    		}
    		case KeyboardEvent.F9:
    		{
    			page.setVisibility(!page.getVisibility());
    			return true;
    		}
    		// navigation
    		case KeyboardEvent.ARROW_LEFT:
    		case KeyboardEvent.ALTERNATIVE_ARROW_LEFT:
   			if(event.withShiftOnly())
    		{ // prev
   				if(currentSelectorEmpty==null) return true; // dev bug, if it happend
    			if(!currentSelectorEmpty.prev(elements))
   				{
    				environment.say(PAGE_SCREEN_ANY_FIRST_ELEMENT);
    				return true;
   				}
    			fillCurrentElementInfo();
    			environment.onAreaNewContent(that);
    			return true;
    		} else
    		{
    			break;
    		}
    		case KeyboardEvent.ARROW_RIGHT:
    		case KeyboardEvent.ALTERNATIVE_ARROW_RIGHT:
    		if(event.withShiftOnly())
    		{ // next
   				if(currentSelectorEmpty==null) return true; // dev bug, if it happend
    			if(!currentSelectorEmpty.next(elements))
   				{
    				environment.say(PAGE_SCREEN_ANY_END_ELEMENT);
    				return true;
   				}
    			fillCurrentElementInfo();
    			environment.onAreaNewContent(that);
    			return true;
    		} else
    		{
    			break;
    		}
    		// filtered navigation
    		case KeyboardEvent.TAB:
   			if(event.withShiftOnly())
   			{ // prev
   				if(currentSelectorFiltered==null) return true; // dev bug, if it happend
    			if(!currentSelectorFiltered.prev(elements))
   				{
    				environment.say(PAGE_SCREEN_ANY_FIRST_ELEMENT);
    				return true;
   				}
    			fillCurrentElementInfo();
    			environment.onAreaNewContent(that);
   				return true;
   			} else if(!event.withAlt()&&!event.withControl()&&!event.withShift())
   			{ // next
   				if(currentSelectorFiltered==null) return true; // dev bug, if it happend
    			if(!currentSelectorFiltered.next(elements))
   				{
    				environment.say(PAGE_SCREEN_ANY_END_ELEMENT);
    				return true;
   				}
    			fillCurrentElementInfo();
    			environment.onAreaNewContent(that);
   				return true;
   			}
    		// actions
    		case KeyboardEvent.ENTER:
    		{
    			if(elements.isEditable())
    			{ // edit content
    				String oldvalue=elements.getText();
    				environment.say(PAGE_ANY_PROMPT_NEW_TEXT);
    				// prompt new value
        			MessagesControl.Prompt prompt=new MessagesControl.Prompt(PAGE_ANY_PROMPT_NEW_TEXT,oldvalue);
        			msgControl.messages.add(prompt);
        			//try{ synchronized(prompt){prompt.wait();} } catch(InterruptedException e) {e.printStackTrace();}
        			synchronized(prompt){msgControl.doit();}
        			String newvalue=prompt.result;
        			prompt.remove();
        			// change to new value
        			elements.setText(newvalue);
    				// refresh screen info
        			fillCurrentElementInfo();
        			environment.onAreaNewContent(that);
    			} else
    			{ // emulate click
    				elements.clickEmulate();
    			}
    			return true;
    		}
    		default: break;
    	}
    	return super.onKeyboardEvent(event);
    }
}
