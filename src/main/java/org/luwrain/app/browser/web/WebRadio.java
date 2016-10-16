package org.luwrain.app.browser.web;

import org.luwrain.browser.ElementIterator;

public class WebRadio extends WebEdit
{
	public WebRadio(WebElement parent,ElementIterator element)
	{
		super(parent,element);
	}
	@Override public Type getType()
	{
		return Type.Radio;
	}
	@Override public String getTextSay()
	{
		return nodeIt.getText();
	}
	@Override public String getTextView()
	{
		return "["+getType().name()+" " + nodeIt.getText() + "]";
	}
}
