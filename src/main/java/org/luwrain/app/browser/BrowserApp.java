
package org.luwrain.app.browser;

import java.net.MalformedURLException;
import java.net.URL;
import java.io.*;

import org.luwrain.core.*;
import org.luwrain.core.events.*;
import org.luwrain.controls.*;
import org.luwrain.controls.doctree.*;
import org.luwrain.doctree.loading.*;

class BrowserApp implements Application
{
	static public final String STRINGS_NAME = "luwrain.notepad";

    private Luwrain luwrain;
    private Actions actions = null;
    private BrowserArea area;

    private final String arg;

    public BrowserApp()
    {
	arg = null;
    }

    public BrowserApp(String arg)
    {
	NullCheck.notNull(arg, "arg");
	this.arg = arg;
    }

    @Override public boolean onLaunch(Luwrain luwrain)
    {
	NullCheck.notNull(luwrain, "luwrain");
	/*
	final Object o = luwrain.i18n().getStrings(STRINGS_NAME);
	if (o == null || !(o instanceof Strings))
	    return false;
	strings = (Strings)o;
	*/
	URL url=null;
	if(arg!=null)
	{
		try
		{
			url=new URL(arg);
		} catch(MalformedURLException e)
		{
			Log.error("browser","Can't init URL from argument: "+e.getMessage());
			return false;
		}
	}
	this.luwrain = luwrain;
	actions = new Actions(luwrain);
	createArea();
	/*
	if(arg!=null)
	{
		final URL urlfinal=url; 
		luwrain.runInMainThread(()->area.open(urlfinal));
	}
	*/
	try {
	area.open(new URL("http://google.com"));
	}
	catch(MalformedURLException e)
	{
	    e.printStackTrace();
	}
	return true;
    }

    private void createArea()
    {
	final org.luwrain.controls.doctree.Strings announcementStrings = (org.luwrain.controls.doctree.Strings)luwrain.i18n().getStrings("luwrain.doctree");
	final Announcement announcement = new Announcement(new DefaultControlEnvironment(luwrain), announcementStrings);

    	area = new BrowserArea(luwrain, actions, luwrain.createBrowser(), announcement){

		@Override public boolean onKeyboardEvent(KeyboardEvent event)
		{
		    NullCheck.notNull(event, "event");
		    return super.onKeyboardEvent(event);
		}

		@Override public boolean onEnvironmentEvent(EnvironmentEvent event)
		{
		    NullCheck.notNull(event, "event");
		    if (event.getType() != EnvironmentEvent.Type.REGULAR)
			return super.onEnvironmentEvent(event);
		    switch(event.getCode())
		    {
		    case ACTION:
			return onBrowserAction(event);
		    case CLOSE:
			closeApp();
			return true;
		    default:
			return super.onEnvironmentEvent(event);
		    }
		}

		    @Override public Action[] getAreaActions()
		    {
			return actions.getBrowserActions();
		    }
	    };

	try {
	final UrlLoader loader = new UrlLoader(new URL("http://bash.org.ru"));
	final UrlLoader.Result res = loader.load();
	res.doc.commit();
	area.setDocument(res.doc, 80);
	}
	catch(IOException e)
	{
	    e.printStackTrace();
	}
    }

    private boolean onBrowserAction(EnvironmentEvent event)
    {
	NullCheck.notNull(event, "event");
	if (ActionEvent.isAction(event, "open-url"))
	    return actions.onOpenUrl(area);
	return false;
    }


private void closeApp()
    {
	luwrain.closeApp();
    }

    @Override public String getAppName()
    {
	return "Веб-браузер";
    }

    @Override public AreaLayout getAreasToShow()
    {
	return new AreaLayout(area);
    }
}
