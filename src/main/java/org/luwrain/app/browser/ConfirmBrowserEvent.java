package org.luwrain.app.browser;

import org.luwrain.core.Event;

/* Confirm event, it show message and wait Accept or Cancel actions from user */
public class ConfirmBrowserEvent extends Event
{
	String message;
	boolean reply;

	/* get user action, yes - true, no - false */
	public boolean isAccepted()
	{
		return reply;
	}

	/* set user reply, yes - true, no - false */
	public void setAccepted(boolean reply)
	{
		this.reply=reply;
	}

	/* return confirm message */
	public String getMessage(){return message;}

	/* @param message - set event message */
	public ConfirmBrowserEvent(String message)
	{
		super(Event.UI_EVENT);
	}

}
