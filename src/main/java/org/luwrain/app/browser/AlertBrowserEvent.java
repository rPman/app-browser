package org.luwrain.app.browser;

import org.luwrain.core.Event;

/* Alert event, it show message and wait to any user interaction */
public class AlertBrowserEvent extends Event
{
	String message;

	/* return alert message */
	public String getMessage(){return message;}

	/* @param message - set event message */
	public AlertBrowserEvent(String message)
	{
		super(Event.UI_EVENT);
		this.message=message;
	}
}
