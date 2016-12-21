
package org.luwrain.app.browser;

import java.util.*;

import org.luwrain.core.*;

public class Extension extends org.luwrain.core.extensions.EmptyExtension
{
    @Override public Command[] getCommands(Luwrain luwrain)
    {
	return new Command[]{
	    new Command(){
		@Override public String getName()
		{
		    return "b"; // FIXME: browser
		}
		@Override public void onCommand(Luwrain luwrain)
		{
		    luwrain.launchApp("browser");
		}
	    }};
    }

    @Override public Shortcut[] getShortcuts(Luwrain luwrain)
    {
	return new Shortcut[]{
	    new Shortcut() {
		@Override public String getName()
		{
		    return "browser";
		}
		@Override public Application[] prepareApp(String[] args)
		{
		    if (args == null || args.length < 1)
			return new Application[]{new BrowserApp()};
		    LinkedList<Application> v = new LinkedList<Application>();
		    for(String s: args)
			if (s != null)
			    v.add(new BrowserApp(s));
		    if (v.isEmpty())
			return new Application[]{new BrowserApp()};
		    return v.toArray(new Application[v.size()]);
		}
	    }};
    }

    @Override public void i18nExtension(Luwrain luwrain, I18nExtension i18nExt)
    {
	i18nExt.addCommandTitle("en", "browser", "Web-browser");
	i18nExt.addCommandTitle("ru", "browser", "Веб-браузер");
    }
}
