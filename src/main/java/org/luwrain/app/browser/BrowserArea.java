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

class BrowserArea extends NavigateArea
{
	static final String PAGE_ANY_STATE_LOADED="Страница загружена";
	static final String PAGE_ANY_STATE_CANCELED="Загрузка страницы отменена";
	static final String PAGE_ANY_SCREENMODE_PAGE="информация о странице";
	static final String PAGE_ANY_SCREENMODE_DOWNLOAD="загрузка файла ";
	static final String PAGE_ANY_SCREENMODE_TEXT="просмотр текста ";
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
	static final String PAGE_ANY_PROMPT_ACCEPT_DOWNLOAD="Запрос на загрузку файла";
	static final String PAGE_DOWNLOAD_START="Загрузка файла начата";
	static final String PAGE_DOWNLOAD_FINISHED="Загрузка файла завершена";
	static final String PAGE_DOWNLOAD_FAILED="Загрузка файла прервана";
	static final String PAGE_DOWNLOAD_FIELD_FILESIZE="Размер файла ";
	static final String PAGE_DOWNLOAD_FIELD_FILETYPE="Тип ";
	static final String PAGE_DOWNLOAD_FIELD_PROGRESS="Состояние ";
	static final String PAGE_DOWNLOAD_FIELD_PROGRESS_FINISHED="загружено";
	static final String PAGE_ANY_PROMPT_TAGFILTER_NAME="Введите имя тега для поиска";
	static final String PAGE_ANY_PROMPT_TAGFILTER_VALUE="имя атрибута";
	static final String PAGE_ANY_PROMPT_TAGFILTER_ATTR="значение атрибута";
	
	// downloader settings
	static final String DEFAULT_DOWNLOAD_DIR=".";
	static final int BUFFER_SIZE=1024*1024;

	// FIXME: get current screen text table width from environment and do it any time but not from constant
	static final int TEXT_SCREEN_WIDTH=100;
	
	private ControlEnvironment environment;
    private WebPage page;
    private BrowserEvents browserEvents;
    
    enum ScreenMode {PAGE,TEXT,DOWNLOAD};
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
    
    class ScreenDownload
    { // contains first line - file name, file size, progress and last multiline - download link
    	String filename="";
    	String filetype=null;
    	Integer filesize=null;
    	Integer progress=null;
    	public String[] link=new String[1];
    	public void setLink(String string)
    	{
    		link=splitTextForScreen(string);
    	}
    	public int getLinesCount()
    	{
    		return 4+link.length;
    	}
    	public String getStringByLine(int line)
    	{
    		if(line==0) return filename;
    		if(line==1) return filetype==null?null:PAGE_DOWNLOAD_FIELD_FILETYPE+filetype;
    		if(line==2) return filesize==null?null:PAGE_DOWNLOAD_FIELD_FILESIZE+filesize;
    		if(line==3) return progress==null?null:PAGE_DOWNLOAD_FIELD_PROGRESS+(progress==100?PAGE_DOWNLOAD_FIELD_PROGRESS_FINISHED:progress+"%");
    		if(line>=4&&line<4+link.length)
    		{
    			int subline=line-4;
    			return link[subline];
    		}
			return null;
    	}
    	public void breakExecution()
    	{
    		if(fileDownloadThread.isAlive())
   			{
    			fileDownloadThread.interrupt();
   			}
    	}
        void refreshInfo()
        {
        	if(screenMode==ScreenMode.DOWNLOAD)
        		environment.onAreaNewContent(that);
        }
    }
    ScreenDownload screenDownload=new ScreenDownload();
    
    public class FileDownloadThread extends Thread
    {
        String downloadLink=null;
        public void run() 
        {
        	SwingUtilities.invokeLater(new Runnable() { @Override public void run()
        	{
        		screenDownload.setLink(downloadLink);
        		screenDownload.refreshInfo();
        	}});

        	HttpURLConnection httpConn=null;
			try
			{
	        	if(downloadLink==null) return; // fixme: make dev error handling
	        	
		        URL url = new URL(downloadLink);
		        httpConn = (HttpURLConnection) url.openConnection();
		        int responseCode = httpConn.getResponseCode();
		 
		        // always check HTTP response code first
		        if (responseCode == HttpURLConnection.HTTP_OK)
		        {
		            String fileName = "";
		            String disposition = httpConn.getHeaderField("Content-Disposition");
		            String contentType = httpConn.getContentType();
		            int contentLength = httpConn.getContentLength();
		 
		            if (disposition != null)
		            {
		                // extracts file name from header field
		                int index = disposition.indexOf("filename=");
		                if (index > 0)
		                {
		                    fileName = disposition.substring(index + 10,disposition.length() - 1);
		                }
		            } else {
		                // extracts file name from URL
		            	// FIXME: make better filename extraction from url
		                fileName = downloadLink.substring(downloadLink.lastIndexOf("/") + 1,downloadLink.length());
		            }
		 
		            //System.out.println("Content-Type = " + contentType);
		            //System.out.println("Content-Disposition = " + disposition);
		            //System.out.println("Content-Length = " + contentLength);
		            //System.out.println("fileName = " + fileName);
		 
			        final String fileType_=contentType;
			        final String fileName_=fileName;
			        final int fileSize_=contentLength;
		            SwingUtilities.invokeLater(new Runnable() { @Override public void run()
			        {
			        	screenDownload.filename=fileName_;
			        	screenDownload.filesize=fileSize_;
			        	screenDownload.filetype=fileType_;
			        	screenDownload.refreshInfo();
			        }});
	
			        // opens input stream from the HTTP connection
		            InputStream inputStream;
					inputStream=httpConn.getInputStream();
		            String saveFilePath = DEFAULT_DOWNLOAD_DIR+File.separator + fileName;
		             
		            // opens an output stream to save into file
		            FileOutputStream outputStream = new FileOutputStream(saveFilePath);
		 
		            int bytesRead = -1;
		            int bytesAll=0;
		            byte[] buffer = new byte[BUFFER_SIZE];
		            Date prev=new Date();
		            while ((bytesRead = inputStream.read(buffer)) != -1)
		            {
		                bytesAll+=bytesRead;
		                outputStream.write(buffer, 0, bytesRead);
		                // show progress, not more often then 1 times per second
		                Date now=new Date();
		                if(now.getTime()-prev.getTime()>=1000)
		                { // show progress
		                	final int downloadedSize=bytesAll;
		                	SwingUtilities.invokeLater(new Runnable() { @Override public void run()
		                	{
		                		screenDownload.progress=100*downloadedSize/screenDownload.filesize;
		                		screenDownload.refreshInfo();
		                	}});
		                }
		            }
		 
		            outputStream.close();
		            inputStream.close();
		            // download finished
			        SwingUtilities.invokeLater(new Runnable() { @Override public void run()
			        {
                		screenDownload.progress=100;
                		screenDownload.refreshInfo();
			        	environment.say(PAGE_DOWNLOAD_FINISHED);
			        }});
			        //System.out.println("File downloaded");
		        } else
		        {
		            //System.out.println("No file to download. Server replied HTTP code: " + responseCode);
		        	throw new Exception();
		        }
			} catch(Exception e)
			{
				// download failed
		        SwingUtilities.invokeLater(new Runnable() { @Override public void run()
		        {
		        	environment.say(PAGE_DOWNLOAD_FAILED);
		        }});
				e.printStackTrace();
			}
			finally
			{
		        if(httpConn!=null) httpConn.disconnect();
			}
        }
    }
    
    FileDownloadThread fileDownloadThread=new FileDownloadThread();
    
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

	    			environment.say(PAGE_ANY_STATE_LOADED);
				} else
				{
	    			environment.say(PAGE_ANY_STATE_CANCELED);
					screenPage.changedTitle("");
				}
    			environment.onAreaNewContent(that);
			}
			@Override public void onProgress(Number progress)
			{
				screenPage.changedProgress((double)progress);
    			environment.onAreaNewContent(that);
			}
			@Override public void onAlert(final String message)
			{
				environment.say(PAGE_SCREEN_ALERT_MESSAGE+message);
    			MessagesControl.Alert alert=new MessagesControl.Alert(PAGE_SCREEN_ALERT_MESSAGE,message);
    			msgControl.messages.add(alert);
    			//try{ synchronized(alert){alert.wait();} } catch(InterruptedException e) {e.printStackTrace();}
    			synchronized(alert){msgControl.doit();}
    			alert.remove();
			}
			@Override public String onPrompt(final String message,final String value)
			{
				environment.say(PAGE_SCREEN_PROMPT_MESSAGE+message);
				
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
			@Override public boolean onDownloadStart(String url)
			{
				Log.warning("browser","DOWNLOAD: "+url);
				environment.say(PAGE_ANY_PROMPT_ACCEPT_DOWNLOAD);
    			MessagesControl.Prompt prompt=new MessagesControl.Prompt(PAGE_SCREEN_PROMPT_MESSAGE,"");
    			msgControl.messages.add(prompt);
    			//try{ synchronized(prompt){prompt.wait();} } catch(InterruptedException e) {e.printStackTrace();}
    			synchronized(prompt){msgControl.doit();}
    			String result=prompt.result;
    			prompt.remove();
    			
    			if(result!=null)
    			{ // cancel previous downloading and start new
    				if(fileDownloadThread.isAlive()) fileDownloadThread.interrupt();
    				fileDownloadThread.downloadLink=url;
    				fileDownloadThread.start();
    				environment.say(PAGE_DOWNLOAD_START);
    			}
    			
				return result!=null;
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
    		case DOWNLOAD: return screenDownload.getLinesCount();
    		default: return 0;
    	}
    }

    @Override public String getLine(int index)
    {
    	switch(screenMode)
    	{
    		case PAGE: return screenPage.getStringByLine(index);
    		case TEXT: return screenText.getStringByLine(index);
    		case DOWNLOAD: return screenDownload.getStringByLine(index);
    		default: return "";
    	}
    }

    @Override public String getAreaName()
    {
    	String translatedModeName;
    	switch(screenMode)
    	{
    		case PAGE:translatedModeName=PAGE_ANY_SCREENMODE_PAGE;break;
    		case TEXT:translatedModeName=PAGE_ANY_SCREENMODE_TEXT;break;
    		case DOWNLOAD:translatedModeName=PAGE_ANY_SCREENMODE_DOWNLOAD;break;
    		default: translatedModeName=screenMode.name();break;
    	}
    	return page.getBrowserTitle()+" "+translatedModeName;
    }
    
    public boolean onKeyboardEvent(KeyboardEvent event)
    {
		//Log.debug("webbrowser","alt:"+event.withAlt()+", ctrl"+event.withControl()+", shift:"+event.withShift());
    	switch(event.getCharacter())
    	{
    		// Ctrl+F or '/' call text search
    		case 'f':if(!event.withControlOnly()) return true; 
    		case '/':
    			{onChangeTextFilter();return true;}
    		default: break;
    	}
    	switch (event.getCommand())
    	{
    		case KeyboardEvent.ESCAPE: {onBreakCommand();return true;}
    		case KeyboardEvent.F5: {onChangeTagFilters();return true;}
    		case KeyboardEvent.F6: {onChangeCurrentPageLink();return true;}
    		case KeyboardEvent.F7: {onChangeScreenModeToText();return true;}
    		case KeyboardEvent.F8: {onChangeScreenModeToPage();return true;}
    		case KeyboardEvent.F9: {onChangeScreenModeToDownload();return true;}
    		case KeyboardEvent.F11:{onChangeWebViewVisibility();return true;}

    		// navigation
    		case KeyboardEvent.ARROW_LEFT:
    		case KeyboardEvent.ALTERNATIVE_ARROW_LEFT:
    			if(event.withShiftOnly()) {onElementNavigateLeft();return true;}
   			break;
    		case KeyboardEvent.ARROW_RIGHT:
    		case KeyboardEvent.ALTERNATIVE_ARROW_RIGHT:
    			if(event.withShiftOnly()) {onElementNavigateRight();return true;}
    		break;
    		// filtered navigation
    		case KeyboardEvent.TAB:
   			if(event.withShiftOnly()){onSearchResultNavigationLeft();return true;}
   			else if(!event.withAlt()&&!event.withControl()&&!event.withShift()) {onSearchResultNavigationRight();return true;}

   			// actions
    		case KeyboardEvent.ENTER:{onDefaultAction();return true;}
    		default: break;
    	}
    	return super.onKeyboardEvent(event);
    }

    //
    void onBreakCommand()
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
    void onChangeScreenModeToText()
    {
		screenMode=ScreenMode.TEXT;
		fillCurrentElementInfo();
		environment.onAreaNewContent(that);
    }
    void onChangeScreenModeToPage()
	{
		screenMode=ScreenMode.PAGE;
		environment.onAreaNewContent(that);
	}
    void onChangeTextFilter()
	{
		environment.say(PAGE_ANY_PROMPT_TEXT_FILTER);

		MessagesControl.Prompt prompt=new MessagesControl.Prompt(PAGE_ANY_PROMPT_TEXT_FILTER,"");
		msgControl.messages.add(prompt);
		//try{ synchronized(prompt){prompt.wait();} } catch(InterruptedException e) {e.printStackTrace();}
		synchronized(prompt){msgControl.doit();}
		String filter=prompt.result;
		prompt.remove();
		
		if(filter==null) return;
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
	}
    void onChangeTagFilters()
    {
		environment.say(PAGE_ANY_PROMPT_TAGFILTER_NAME);
		MessagesControl.Prompt prompt;
		prompt=new MessagesControl.Prompt(PAGE_ANY_PROMPT_TAGFILTER_NAME,"");
		msgControl.messages.add(prompt);
		//try{ synchronized(prompt){prompt.wait();} } catch(InterruptedException e) {e.printStackTrace();}
		synchronized(prompt){msgControl.doit();}
		String filter=prompt.result;
		prompt.remove();
		
		if(filter==null) return;
		if(filter.isEmpty()) filter=null;

		environment.say(PAGE_ANY_PROMPT_TAGFILTER_ATTR);
		prompt=new MessagesControl.Prompt(PAGE_ANY_PROMPT_TAGFILTER_ATTR,"");
		msgControl.messages.add(prompt);
		//try{ synchronized(prompt){prompt.wait();} } catch(InterruptedException e) {e.printStackTrace();}
		synchronized(prompt){msgControl.doit();}
		String attrName=prompt.result;
		prompt.remove();
		
		if(attrName==null) return;
		if(attrName.isEmpty()) attrName=null;

		environment.say(PAGE_ANY_PROMPT_TAGFILTER_VALUE);
		prompt=new MessagesControl.Prompt(PAGE_ANY_PROMPT_TAGFILTER_VALUE,"");
		msgControl.messages.add(prompt);
		//try{ synchronized(prompt){prompt.wait();} } catch(InterruptedException e) {e.printStackTrace();}
		synchronized(prompt){msgControl.doit();}
		String attrValue=prompt.result;
		prompt.remove();
		
		if(attrValue==null) return;
		if(attrValue.isEmpty()) attrValue=null;

		
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
			environment.onAreaNewContent(that);
		}
    }
    void onChangeScreenModeToDownload()
    { // control pressed
		screenMode=ScreenMode.DOWNLOAD;
		screenDownload.refreshInfo();
	}
    void onChangeCurrentPageLink()
	{
		//String link="http://rpserver/a.html";
		//String link="http://ya.ru";
		environment.say(PAGE_ANY_PROMPT_ADDRESS);

		MessagesControl.Prompt prompt=new MessagesControl.Prompt(PAGE_ANY_PROMPT_ADDRESS,"");
		msgControl.messages.add(prompt);
		//try{ synchronized(prompt){prompt.wait();} } catch(InterruptedException e) {e.printStackTrace();}
		synchronized(prompt){msgControl.doit();}
		String link=prompt.result;
		prompt.remove();
		
		if(link==null) return;
		if(!link.matches("^(http|https|ftp)://.*$")) link="http://"+link;
		
		page.load(link);
		screenPage.changedUrl(link);
		
		screenMode=ScreenMode.PAGE;
		
		environment.onAreaNewContent(that);
	}
    void onChangeWebViewVisibility()
	{
		page.setVisibility(!page.getVisibility());
	}

    void onElementNavigateLeft()
    { // prev
		if(currentSelectorEmpty==null) return; // dev bug, if it happend
		if(!currentSelectorEmpty.prev(elements))
		{
			environment.say(PAGE_SCREEN_ANY_FIRST_ELEMENT);
			return;
		}
		fillCurrentElementInfo();
		environment.onAreaNewContent(that);
	}
    void onElementNavigateRight()
    { // next
		if(currentSelectorEmpty==null) return; // dev bug, if it happend
		if(!currentSelectorEmpty.next(elements))
		{
			environment.say(PAGE_SCREEN_ANY_END_ELEMENT);
			return;
		}
		fillCurrentElementInfo();
		environment.onAreaNewContent(that);
	}
    void onSearchResultNavigationLeft()
    { // prev
		if(currentSelectorFiltered==null) return; // dev bug, if it happend
		if(!currentSelectorFiltered.prev(elements))
		{
			environment.say(PAGE_SCREEN_ANY_FIRST_ELEMENT);
			return;
		}
		fillCurrentElementInfo();
		environment.onAreaNewContent(that);
		return;
	}
    void onSearchResultNavigationRight()
    { // next
		if(currentSelectorFiltered==null) return; // dev bug, if it happend
		if(!currentSelectorFiltered.next(elements))
		{
			environment.say(PAGE_SCREEN_ANY_END_ELEMENT);
			return;
		}
		fillCurrentElementInfo();
		environment.onAreaNewContent(that);
	}
    void onDefaultAction()
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
		return;
	}
}
