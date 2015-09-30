
package org.luwrain.app.browser;

import javafx.concurrent.Worker.State;

import org.luwrain.core.*;
import org.luwrain.controls.*;
import org.luwrain.browser.*;
import org.luwrain.interaction.browser.WebPage;

class Events implements BrowserEvents
{
    ScreenPage screenPage;
WebPage page;
    ControlEnvironment environment;

    @Override public void onChangeState(State state)
    {
	//screenPage.changedUrl("test");
	//screenPage.changedTitle("title");
	/*
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
	*/
    }

    @Override public void onProgress(Number progress)
    {
	screenPage.changedProgress((double)progress);
	//	environment.onAreaNewContent(that);
    }
    @Override public void onAlert(final String message)
    {
	//	environment.say(PAGE_SCREEN_ALERT_MESSAGE+message);
	AlertBrowserEvent event=new AlertBrowserEvent(message);
	try {event.waitForBeProcessed();} // FIXME: make better error handling
	catch(InterruptedException e){e.printStackTrace();}
	/*
	  MessagesControl.Alert alert=new MessagesControl.Alert(PAGE_SCREEN_ALERT_MESSAGE,message);
	  msgControl.messages.add(alert);
	  //try{ synchronized(alert){alert.wait();} } catch(InterruptedException e) {e.printStackTrace();}
	  synchronized(alert){msgControl.doit();}
	  alert.remove();
	*/
    }

    @Override public String onPrompt(final String message,final String value)
    {
	//	environment.say(PAGE_SCREEN_PROMPT_MESSAGE+message);
	PromptBrowserEvent event=new PromptBrowserEvent(message,value);
	try {event.waitForBeProcessed();} // FIXME: make better error handling
	catch(InterruptedException e){e.printStackTrace();}
	/*
	  MessagesControl.Prompt prompt=new MessagesControl.Prompt(PAGE_SCREEN_PROMPT_MESSAGE,"ya.ru");
	  msgControl.messages.add(prompt);
	  //try{ synchronized(prompt){prompt.wait();} } catch(InterruptedException e) {e.printStackTrace();}
	  synchronized(prompt){msgControl.doit();}
	  String result=prompt.result;
	  prompt.remove();
	*/
	return event.getPrompt();
    }

    @Override public void onError(String message)
    {
	// FIXME: make browser error handling or hide it
	Log.warning("browser",message);
    }

    @Override public boolean onDownloadStart(String url)
    {
/*
	Log.warning("browser","DOWNLOAD: "+url);
	//	environment.say(PAGE_ANY_PROMPT_ACCEPT_DOWNLOAD);
	PromptBrowserEvent event=new PromptBrowserEvent(PAGE_SCREEN_PROMPT_MESSAGE,"");
	try {event.waitForBeProcessed();} // FIXME: make better error handling
	catch(InterruptedException e){e.printStackTrace();}
	/*
	  MessagesControl.Prompt prompt=new MessagesControl.Prompt(PAGE_SCREEN_PROMPT_MESSAGE,"");
	  msgControl.messages.add(prompt);
	  //try{ synchronized(prompt){prompt.wait();} } catch(InterruptedException e) {e.printStackTrace();}
	  synchronized(prompt){msgControl.doit();}
	  String result=prompt.result;
	  prompt.remove();
	//
	if(event.getPrompt()!=null&&!event.getPrompt().isEmpty())
	{ // cancel previous downloading and start new
	    if(fileDownloadThread.isAlive()) fileDownloadThread.interrupt();
	    fileDownloadThread.downloadLink=url;
	    fileDownloadThread.start();
	    environment.say(PAGE_DOWNLOAD_START);
	    return true;
	}
*/
	return false;

    }

    @Override public Boolean onConfirm(String message)
    {
	//	environment.say(PAGE_SCREEN_CONFIRM_MESSAGE+message);
	ConfirmBrowserEvent event=new ConfirmBrowserEvent(message);
	try {event.waitForBeProcessed();} // FIXME: make better error handling
	catch(InterruptedException e){e.printStackTrace();}
	return event.isAccepted();
    }
};
