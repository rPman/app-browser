package org.luwrain.app.browser.web;

import org.luwrain.browser.ElementIterator;

public class WebButton extends WebText
{

	public WebButton(WebElement parent,ElementIterator element)
	{
		super(parent,element);
		
	}
	@Override public String getType()
	{
		return "Button";
	}

}
