
package org.luwrain.app.browser;

import javafx.concurrent.Worker.State;

import org.luwrain.core.*;
import org.luwrain.browser.*;

class Events implements BrowserEvents
{
    private Luwrain luwrain;
    private Area area;

    Events(Luwrain luwrain, Area area)
    {
	NullCheck.notNull(luwrain, "luwrain");
	NullCheck.notNull(area, "area");
	this.luwrain = luwrain;
	this.area = area;
    }

    @Override public void onChangeState(State state)
    {
		luwrain.enqueueEvent(new PageChangeStateEvent(area, state));
    }

    @Override public void onProgress(Number progress)
    {
    	luwrain.enqueueEvent(new ProgressEvent(area, progress));
    }

    @Override public void onAlert(String message)
    {
    	AlertEvent event=new AlertEvent(area, message);
    	luwrain.enqueueEvent(event);
    	//event.waitForBeProcessed();
    }

    @Override public String onPrompt(String message, String value)
    {
    	final PromptEvent event = new PromptEvent(area, message, value);
    	luwrain.enqueueEvent(event);
    	//Log.debug("browser", "onPrompt sent, awaiting...");
    	//event.waitForBeProcessed();
    	//Log.debug("browser", "onPrompt receive answer");
    	return event.answer();
    }

    @Override public void onError(String message)
    {
    	luwrain.enqueueEvent(new ErrorEvent(area, message));
    }

    @Override public boolean onDownloadStart(String url)
    {
    	luwrain.enqueueEvent(new DownloadEvent(area, url));
    	return true;
    }

    @Override public Boolean onConfirm(String message)
    {
		final ConfirmEvent event = new ConfirmEvent(area, message);
		luwrain.enqueueEvent(event);
		//event.waitForBeProcessed();
		return event.answer();
    }
};
