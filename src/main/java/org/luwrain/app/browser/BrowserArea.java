
package org.luwrain.app.browser;

import java.net.*;
import java.util.*;

import org.luwrain.core.*;
import org.luwrain.core.events.*;
import org.luwrain.controls.*;
import org.luwrain.controls.doctree.*;
import org.luwrain.doctree.*;
import org.luwrain.browser.*;
import org.luwrain.browser.Events.WebState;
import org.luwrain.app.browser.web.*;
import org.luwrain.app.browser.webdoc.BrowserToDocumentConverter;
import org.luwrain.app.browser.webdoc.WebDocBuilder;

class BrowserArea extends DoctreeArea
{
    protected final Luwrain luwrain;
    protected final Callback callback;
    protected final ControlEnvironment environment;

    protected final Browser page;
    protected Events events;

    protected WebState state = WebState.READY;
    protected int progress = 0;

    protected WebDocument doc = new WebDocument();
    //    protected WebView view = null;
    //    protected WebIterator it = null;
    //    protected int hotPointX = 0;

    protected final Vector<HistoryElement> elementHistory = new Vector<HistoryElement>();
    protected boolean complexMode = false;

    BrowserArea(Luwrain luwrain, Callback callback, Browser browser, Announcement announcement)
    {
	super(new DefaultControlEnvironment(luwrain), announcement);
	NullCheck.notNull(luwrain, "luwrain");
	NullCheck.notNull(callback, "callback");
	NullCheck.notNull(browser, "browser");
	this.luwrain = luwrain;
	this.callback = callback;
	this.environment = new DefaultControlEnvironment(luwrain);
	this.page = browser;
	events = new Events(luwrain, this);
	this.page.init(events);
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
	if(isBusy())
	    return false;
	page.RescanDOM();
	doc = new WebDocument();
	doc.make(page);
	complexMode = false;
	updateView();
	Log.debug("browser", "DOM refreshed successfully");
	return true;
    }

    protected void updateView()
    {
	//final WebDocBuilder builder = new WebDocBuilder();
	//final Document doc = builder.make(page);
	//doc.commit();
	//setDocument(doc, luwrain.getAreaVisibleWidth(this));
   	final BrowserToDocumentConverter builder = new BrowserToDocumentConverter();
   	setDocument(builder.go(page), luwrain.getAreaVisibleWidth(this));
    }

    /**Checks if the browser has valid loaded page
     *
     * @return true if there is any successfully loaded page, false otherwise
     */ 
    boolean noWebContent()
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

    boolean stop()
    {
	if (isEmpty() || !isBusy())
	    return false;
	Log.debug("browser", "trying to cancel loading");
	page.stop();
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
	    return super.onEnvironmentEvent(event);
	}
    }

    protected boolean onBackspace()
    {
	if(elementHistory.isEmpty()) return false;
	HistoryElement h=elementHistory.lastElement();
	elementHistory.remove(elementHistory.size()-1);
	complexMode=h.mode;
	//current = h.element;
	updateView();
	return true;
    }

    /**Performs some default action relevant to the element under the hot
     * point. What the exact action will be done is dependent on the type of the
     * element under the hot point. If there is a form input edit, the user
     * will get an offer to enter some text. If there is a form list, the
     * user will get a popup to choose some item from the list and so
     * on. This operation may be performed only if the browser is free
     * (meaning, not empty and not busy).
     *
     * @return true if the operation has been done, false otherwise (usually the browser is busy or doesn't have a loaded page)
     */
    protected boolean onClick()
    {
	/*
	if (isEmpty() || isBusy())
	    return false;
	final WebElement el = it.getElementAtPos(hotPointX);
	if(el == null)
	    return false;
	if(el.isComplex() || complexMode)
	    return switchComplexMode(el);
	if(el.getNode().isEditable())
	{ // editable element, edit it
	    if(el instanceof WebRadio || el instanceof WebCheckbox)
		return emulateClick(el);
	    if(el instanceof WebSelect)
		return onFormSelectFromList(el);
	    return onFormEditText(el);
	}
	return emulateClick(el);
	*/
	return false;
    }

    /**Asks the browser core to emulate the action which looks like the user
     * clicks on the given element. This operation may be performed only if
     * the browser is free (meaning, not empty and not busy).
     *
     * @param part The element to emulate click on
     * @return true if the operation has been done, false otherwise (usually the browser is busy or doesn't have a loaded page)
     */
    protected boolean emulateClick(WebElement el)
    {
	NullCheck.notNull(el, "el");
	if (isEmpty() || isBusy())
	    return false;
	el.getNode().clickEmulate();
	return true;
    }

    protected boolean onFormEditText(WebElement el)
    {
	NullCheck.notNull(el, "el");
	if (isEmpty() || isBusy())
	    return false;
	final ElementIterator e = el.getNode();
	final String oldValue = e.getText();
	final String newValue = callback.askFormTextValue(oldValue != null?oldValue:"");
	if (newValue == null) 
	    return true;
	e.setText(newValue);
	updateView();
	return true;
    }

    protected boolean onFormSelectFromList(WebElement el)
    {
	NullCheck.notNull(el, "el");
	if (isEmpty() || isBusy())
	    return false;
	final ElementIterator e = el.getNode();
	final String[] items = e.getMultipleText();
	if (items == null || items.length==0) 
	    return true; // FIXME:
	final String res = callback.askFormListValue(items, true);
	if (res == null)
	    return true;
	e.setText(res);
	updateView();
	return true;
    }

    protected void onPageChangeState(WebState state)
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

    protected boolean onThreadSyncEvent(EnvironmentEvent event)
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
	return false;
    }

    protected void onProgress(Number progress)
    {
	NullCheck.notNull(progress, "progress");
	this.progress = (int)(progress==null?0:Math.floor(progress.doubleValue()*100));
    }

    protected void onAlert(final String message)
    {
	NullCheck.notNull(message, "message");
	if (message.trim().isEmpty())
	    return;
	luwrain.message("Внимание!" + message, Luwrain.MESSAGE_OK);
    }

    protected String onPrompt(String message, String value)
    {
	if (message.trim().isEmpty())
	    return null;
	luwrain.message("Выбор: " +message, Luwrain.MESSAGE_OK);
	return "";//result;
    }

    protected void onError(String message)
    {
	NullCheck.notNull(message, "message");
	if (message.trim().isEmpty())
	    return;
	luwrain.message (message, Luwrain.MESSAGE_ERROR);
    }

    protected void onDownloadStart(String url)
    {
	//FIXME:
    }

    protected Boolean onConfirm(String message)
    {
	luwrain.message ("Подтверждение: " +message, Luwrain.MESSAGE_OK);
	return false;
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
	String askFormTextValue(String currentValue);
	String askFormListValue(String[] items, boolean fromListOnly);
    }
}
