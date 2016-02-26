package org.luwrain.app.browser.web;

import org.luwrain.browser.ElementIterator;

public class WebCheckbox extends WebEdit
{
	public WebCheckbox(WebElement parent,ElementIterator element)
	{
		super(parent,element);
	}
	@Override public String getType()
	{
		return "Checkbox";
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
