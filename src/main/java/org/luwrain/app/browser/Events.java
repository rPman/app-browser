
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
	if (state == null)
	    return;
	luwrain.runInMainThread(()->area.onPageChangeState(state));
    }

    @Override public void onProgress(Number progress)
    {
	if (progress == null)
	    return;
	luwrain.runInMainThread(()->area.onProgress(progress));
    }

    @Override public void onAlert(String message)
    {
	if (message == null)
	    return;
	luwrain.runInMainThread(()->area.onAlert(message));
    }

    @Override public String onPrompt(String message, String value)
    {
	if (message == null || value == null)
	    return "";
    	final PromptEvent event = new PromptEvent(area, message, value);
    	luwrain.enqueueEvent(event);
    	//Log.debug("browser", "onPrompt sent, awaiting...");
    	//event.waitForBeProcessed();
    	//Log.debug("browser", "onPrompt receive answer");
    	return event.answer();
    }

    @Override public void onError(String message)
    {
	if (message == null)
	    return;
	NullCheck.notNull(message, "message");
	luwrain.runInMainThread(()->area.onError(message));
    }

    @Override public boolean onDownloadStart(String url)
    {
	if (url == null)
	    return true;
	luwrain.runInMainThread(()->area.onDownloadStart(url));
    	return true;
    }

    @Override public Boolean onConfirm(String message)
    {
	if (message == null)
	    return false;
		final ConfirmEvent event = new ConfirmEvent(area, message);
		luwrain.enqueueEvent(event);
		//event.waitForBeProcessed();
		return event.answer();
    }

	@Override public void onPageChanged()
	{
		// FIXME: make new method in area
		area.onContentChanged();
	}
};
