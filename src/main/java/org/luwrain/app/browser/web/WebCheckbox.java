package org.luwrain.app.browser.web;

import org.luwrain.browser.ElementIterator;

public class WebCheckbox extends WebEdit
{
	public WebCheckbox(WebElement parent,ElementIterator element)
	{
		super(parent,element);
	}
	@Override public Type getType()
	{
		return Type.Checkbox;
	}
	@Override public String getTextSay()
	{
		return nodeIt.getText();
	}
	@Override public String getTextView()
	{
		return "["+getType()+" " + nodeIt.getText()+"]";
	}
}
