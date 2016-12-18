
package org.luwrain.app.browser;

import java.net.*;

import org.luwrain.core.*;
import org.luwrain.core.events.*;
import org.luwrain.popups.*;

class Actions implements BrowserArea.Callback
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

    @Override public void onBrowserRunning()
    {
	luwrain.message("Идёт загрузка страницы. Пожалуйста, подождите...");
    }

    @Override public void onBrowserSuccess(String title)
    {
	NullCheck.notNull(title, "title");
	luwrain.message(title, Luwrain.MESSAGE_DONE);
    }

    @Override public void onBrowserFailed()
    {
	luwrain.message("Страница не может быть загружена", Luwrain.MESSAGE_ERROR);
    }
}
