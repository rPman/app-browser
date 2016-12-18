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

	@Override public String getText()
	{
		return "(" + nodeIt.getText() + ")";
	}
}
