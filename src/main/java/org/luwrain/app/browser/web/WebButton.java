package org.luwrain.app.browser.web;

import org.luwrain.browser.ElementIterator;

public class WebButton extends WebText
{

	public WebButton(WebElement parent,ElementIterator element)
	{
		super(parent,element);
	}
	@Override public Type getType()
	{
		return Type.Button;
	}
	@Override public String getTextSay()
	{
		return rootElement.getText();
	}
	@Override public String getTextView()
	{
		return "["+getType()+" "+rootElement.getText()+"]";
	}
}
