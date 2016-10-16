
package org.luwrain.app.browser;

import org.luwrain.core.*;
import org.luwrain.browser.*;

class Events implements org.luwrain.browser.Events
{
    private final Luwrain luwrain;
    private final BrowserArea area;

    Events(Luwrain luwrain, BrowserArea area)
    {
	NullCheck.notNull(luwrain, "luwrain");
	NullCheck.notNull(area, "area");
	this.luwrain = luwrain;
	this.area = area;
    }

    @Override public void onChangeState(WebState state)
    {
	NullCheck.notNull(state, "state");
	luwrain.runInMainThread(()->area.			onPageChangeState(state));
    }

    @Override public void onProgress(Number progress)
    {
	NullCheck.notNull(progress, "progress");
	luwrain.runInMainThread(()->area.onProgress(progress));
    }

    @Override public void onAlert(String message)
    {
	NullCheck.notNull(message, "message");
	luwrain.runInMainThread(()->area.onAlert(message));
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
	NullCheck.notNull(message, "message");
	luwrain.runInMainThread(()->area.onError(message));
    }

    @Override public boolean onDownloadStart(String url)
    {
	NullCheck.notNull(url, "url");
	luwrain.runInMainThread(()->area.onDownloadStart(url));
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
