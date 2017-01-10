
package org.luwrain.app.browser;

import java.net.*;
import java.util.*;

import org.luwrain.core.*;
import org.luwrain.core.events.*;
import org.luwrain.core.events.KeyboardEvent.Special;
import org.luwrain.controls.*;
import org.luwrain.controls.doctree.*;
import org.luwrain.doctree.*;

import org.luwrain.browser.*;
import org.luwrain.browser.Events.WebState;
//import org.luwrain.app.browser.web.*;
//import org.luwrain.app.browser.DocumentBuilder.RunInfo;

class BrowserArea extends DoctreeArea
{
    protected final Luwrain luwrain;
    protected final Callback callback;
    protected final ControlEnvironment environment;

    protected final Browser page;
    protected Events events;

    //    final DocumentBuilder builder = new DocumentBuilder();
    Document doc=null;

    protected WebState state = WebState.READY;
    protected int progress = 0;

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
	long t1=new Date().getTime();
	page.RescanDOM();
	//doc = new WebDocument();
	//doc.make(page);
	complexMode = false;
	updateView();
	Log.debug("browser", "DOM refreshed successfully "+(t1-new Date().getTime()+"ms"));
	return true;
    }

    protected void updateView()
    {
   	// store current position
   	int x=getHotPointX(),y=getHotPointY();
   	// regenerate full document from 
	final DocumentBuilder builder = new DocumentBuilder(page);
	doc = builder.build();
   	page.setWatchNodes(builder.watch);
	doc.commit();
	setDocument(doc, luwrain.getAreaVisibleWidth(this));
	this.onMoveHotPoint(new MoveHotPointEvent(x,y,false));
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

    @Override protected boolean onSpace(KeyboardEvent event)
    {
    	onClick();
    	return super.onSpace(event);
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
    
    protected boolean onPressBackspace()
	{
    	page.executeScript("history.go(-1);");
		return true;
	}

    /*
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
    */

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
    	if (isEmpty() || isBusy())
    	    return false;
    	Run run=this.getCurrentRun();
    	if(run instanceof TextRun)
    	{
    		Object t=((TextRun)run).getAssociatedObject();
    		if(t==null) return false;
    		if(t instanceof ElementAction)
    		{
    			ElementAction action=(ElementAction)t;
    			Boolean result=null;
    			switch(action.type)
    			{
    				case UNKNOWN:
					case CLICK:
						result = emulateClick(action.element);
						break;
					case EDIT:
						result = onFormEditText(action.element);
						break;
					case SELECT:
						result = onFormSelectFromList(action.element);
						break;
					case IFRAME:
						result = onIFrameOpen(action.element);
						break;
					default:
						Log.error("web-browser","unknown action type: "+action.type.name());
						return false;
    			}
				((Actions)callback).skipNextContentChangedNotify();
				page.doFastUpdate();
    		} else
    		{
    			Log.error("web-browser","current TextRun have unknown associated object, type "+t.getClass());
    		}
    	}
    	//System.out.println(run.getClass());
    	return false;
    }

    protected boolean onPressEnter()
	{
    	if (isEmpty() || isBusy())
    	    return false;
    	Run run=this.getCurrentRun();
    	if(run instanceof TextRun)
    	{
    		Object t=((TextRun)run).getAssociatedObject();
    		if(t==null) return false;
    		if(t instanceof ElementAction)
    		{
				ElementAction action=(ElementAction)t;
				return emulateSubmit(action.element);
    		}
    	}
		return false;
	}

    
    /**Asks the browser core to emulate the action which looks like the user
     * clicks on the given element. This operation may be performed only if
     * the browser is free (meaning, not empty and not busy).
     *
     * @param part The element to emulate click on
     * @return true if the operation has been done, false otherwise (usually the browser is busy or doesn't have a loaded page)
     */
    protected boolean emulateClick(ElementIterator el)
    {
	NullCheck.notNull(el, "el");
	if (isEmpty() || isBusy())
	    return false;
	el.clickEmulate();
	return true;
    }
    protected boolean emulateSubmit(ElementIterator el)
    {
	NullCheck.notNull(el, "el");
	if (isEmpty() || isBusy())
	    return false;
	el.submitEmulate();
	return true;
    }

    protected boolean onFormEditText(ElementIterator el)
    {
	NullCheck.notNull(el, "el");
	if (isEmpty() || isBusy())
	    return false;
	final String oldValue = el.getText();
	final String newValue = callback.askFormTextValue(oldValue != null?oldValue:"");
	if (newValue == null) 
	    return true;
	el.setText(newValue);
	updateView();
	return true;
    }

    protected boolean onFormSelectFromList(ElementIterator el)
    {
	NullCheck.notNull(el, "el");
	if (isEmpty() || isBusy())
	    return false;
	final String[] items = el.getMultipleText();
	if (items == null || items.length==0) 
	    return true; // FIXME:
	final String res = callback.askFormListValue(items, true);
	if (res == null)
	    return true;
	el.setText(res);
	updateView();
	return true;
    }
    
    protected boolean onIFrameOpen(ElementIterator el)
    {
    	NullCheck.notNull(el, "el");
    	if (isEmpty() || isBusy())
    	    return false;
    	String src=el.getAttributeProperty("src");
    	if(src!=null&&!src.isEmpty())
    	{
    		page.load(src);
    		return true;
    	}
    	return false;
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
	    goToStartPage();
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

    private void goToStartPage()
	{
    	this.onMoveHotPoint(new MoveHotPointEvent(0,0,false));
	}

	protected void onContentChanged()
    {
    	refresh();
		callback.onBrowserContentChanged(page.getLastTimeChanged());
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
	void onBrowserContentChanged(long lastTimeChanged);
	String askFormTextValue(String currentValue);
	String askFormListValue(String[] items, boolean fromListOnly);
    }

	public boolean onChangeBrowserVisibility()
	{
		page.setVisibility(!page.getVisibility());
		return true;
	}
}
