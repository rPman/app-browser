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
	@Override public String getTextSay()
	{
		return getType()+" "+rootElement.getText();
	}
	@Override public String getTextView()
	{
		return "["+getType()+" "+rootElement.getText()+"]";
	}
}
