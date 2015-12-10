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
import org.luwrain.windows.Windows;

import java.io.File;

import org.luwrain.controls.*;

class BrowserApp implements Application, Actions
{
	static public final String STRINGS_NAME = "luwrain.notepad";


    private Luwrain luwrain;
    private BrowserArea area;

    private String arg = null;

    public BrowserApp()
    {
	arg = null;
    }

    public BrowserApp(String arg)
    {
	this.arg = arg;
	if (arg == null)
	    throw new NullPointerException("fileName may not be null"); 
    }

    @Override public boolean onLaunch(Luwrain luwrain)
    {
	/*
	final Object o = luwrain.i18n().getStrings(STRINGS_NAME);
	if (o == null || !(o instanceof Strings))
	    return false;
	strings = (Strings)o;
	*/
	this.luwrain = luwrain;
	createArea();
	return true;
    }

    @Override public String getAppName()
    {
	return "Веб-браузер";
    }

    private void createArea()
    {
    	area = new BrowserArea(luwrain, this, luwrain.createBrowser());
    }

    @Override public AreaLayout getAreasToShow()
    {
	return new AreaLayout(area);
    }

    @Override public void closeApp()
    {
	luwrain.closeApp();
    }
}
