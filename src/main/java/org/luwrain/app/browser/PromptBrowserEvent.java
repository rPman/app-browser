package org.luwrain.app.browser;

import org.luwrain.core.Event;

/* Prompt event, it show message and prompt to input new string value and allow set default value for it */
public class PromptBrowserEvent extends Event
{
	String message;
	String prompt;
	
	/* @return default return value for prompt, null on user cancel action */
	public String getPrompt()
	{
		return prompt;
	}

	/* set default return value for prompt, null for user cancel action */
	public void setPrompt(String prompt)
	{
		this.prompt=prompt;
	}

	/* return prompt message */
	public String getMessage()
	{
		return message;
	}

	/* @param message - set event message
	 * @param prompt - set default return value for prompt, null was replaced for empty string
	 */
	public PromptBrowserEvent(String message,String prompt)
	{
		super(Event.UI_EVENT);
		this.message=message;
		this.prompt=(prompt==null?"":prompt);
	}

}
