package org.luwrain.app.browser.web;

import org.luwrain.browser.ElementIterator;

public class WebEdit extends WebText
{
	public WebEdit(WebElement parent,ElementIterator element)
	{
		super(parent,element);
		super.needEndLine=true;
	}
	@Override public String getType()
	{
		return "Edit";
	}

}
