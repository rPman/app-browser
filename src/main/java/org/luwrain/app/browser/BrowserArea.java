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
import org.luwrain.controls.*;

class BrowserArea extends NavigateArea
{
    private ControlEnvironment environment;
    private Browser browser;

    public BrowserArea(ControlEnvironment environment, Browser browser)
    {
	super(environment);
	this.environment = environment;
	this.browser = browser;
	if (environment == null)
	    throw new NullPointerException("environment may not be null");
	if (browser == null)
	    throw new NullPointerException("browser may not be null");
    }

    @Override public int getLineCount()
    {
	return 3;
    }

    @Override public String getLine(int index)
    {
	switch(index)
	{
	case 0:
	    return "Line 0";
	case 1:
	    return "line 1";
	default:
	    return "";
	}
    }

    @Override public String getAreaName()
    {
	return browser.getBrowserTitle();
    }
}
