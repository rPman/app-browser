
package org.luwrain.app.browser;

import java.net.*;
import java.util.Date;

import org.luwrain.core.*;
import org.luwrain.core.events.*;
import org.luwrain.popups.*;

class Actions implements BrowserArea.Callback
{
    private final Luwrain luwrain;
    
    /** time in milliseconds betwiin page updates to notify user */
    static final int UPDATE_NOTIFY_MIN_INTERVAL=1000;
    private long lastTimeChanged=0;

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

    @Override public String askFormTextValue(String oldValue)
    {
	return Popups.simple(luwrain, "Редактирование формы", "Новое значение:", oldValue);
    }

    @Override public String askFormListValue(String[] items, boolean fromListOnly)
    {
	return (String)Popups.fixedList(luwrain, "Выберите значение из списка:", items);
    }

	@Override public void onBrowserContentChanged(long lastTimeChanged)
	{
		// TODO: make message about content changed
		if(lastTimeChanged-this.lastTimeChanged>UPDATE_NOTIFY_MIN_INTERVAL)
		{
			luwrain.message("Страница обновлена", Luwrain.MESSAGE_REGULAR);
		} else
		{
			luwrain.playSound(Sounds.INTRO_REGULAR);
		}
		this.lastTimeChanged=lastTimeChanged;
	}
	public void skipNextContentChangedNotify()
	{
		this.lastTimeChanged=new Date().getTime();
	}

    /*
	private boolean onInfoAction()
	{
		WebElementPart part = view.getPartByPos(getHotPointX(),getHotPointY());
		if(part==null) return false;
		// first info - is short text
		String info=part.element.getTextShort()+" ";
		// second info - nearest parent  of complrx view item
		WebElement e=part.element;
		if(e instanceof WebText)
		{
			// FIXME:
			String cssfont=e.getElement().getComputedStyleProperty("font-weight");
			if(cssfont!=null&&!cssfont.equals("normal"))
			{
				info+=" font "+cssfont;
			}
			if(e.getAttributes().containsKey("href"))
			{
				info+=" link "+e.getAttributes().get("href");
			}
		}
		// scan for parent complex
		while(e!=null)
		{
			if(e instanceof WebListItem)
			{
				info+=" list item "+e.getTextShort();
				break;
			}
			e=e.getParent();
		}
		environment.say(info);
		return true;
	}
    */

    /*
	private void onChangeWebViewVisibility()
	{
		page.setVisibility(!page.getVisibility());
	}
    */

}
