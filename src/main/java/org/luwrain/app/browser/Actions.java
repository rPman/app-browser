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

import java.net.*;

import org.luwrain.core.*;
import org.luwrain.core.events.*;
import org.luwrain.popups.*;

class Actions
{
    private final Luwrain luwrain;

    Actions(Luwrain luwrain)
    {
	NullCheck.notNull(luwrain, "luwrain");
	this.luwrain = luwrain;
    }

    Action[] getBrowserActions()
    {
	return new Action[]{
	    new Action("open-url", "Открыть URL", new KeyboardEvent(KeyboardEvent.Special.F6)),
	};
    }

boolean onOpenUrl(BrowserArea area)
    {
	NullCheck.notNull(area, "area");
	if (area.isBusy())
	    return false;
		String url = Popups.simple(luwrain, "Открыть страницу", "Введите адрес страницы:", "http://");
		if(url == null || url.trim().isEmpty())
		    return true;
		if(!url.matches("^(http|https|ftp)://.*$"))
		    url = "http://" + url;
		try {
		area.open(new URL(url));
		}
		catch (MalformedURLException e)
		{
		    luwrain.message("Неверно оформленная ссылка:" + url, Luwrain.MESSAGE_ERROR);
		    return true;
		}
		return true;
    }


}
