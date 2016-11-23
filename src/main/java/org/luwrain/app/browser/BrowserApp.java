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

import org.luwrain.core.*;
import org.luwrain.core.events.*;

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
	this.luwrain = luwrain;
	actions = new Actions(luwrain);
	createArea();
	return true;
    }

    @Override public String getAppName()
    {
	return "Веб-браузер";
    }

    private void createArea()
    {
    	area = new BrowserArea(luwrain, luwrain.createBrowser()){

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
    }

    private boolean onBrowserAction(EnvironmentEvent event)
    {
	NullCheck.notNull(event, "event");
	if (ActionEvent.isAction(event, "open-url"))
	    return actions.onOpenUrl(area);
	return false;
    }

    @Override public AreaLayout getAreasToShow()
    {
	return new AreaLayout(area);
    }

private void closeApp()
    {
	luwrain.closeApp();
    }
}
