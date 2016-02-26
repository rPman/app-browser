package org.luwrain.app.browser.web;

import org.luwrain.browser.ElementIterator;

public class WebSelect extends WebEdit
{
	public WebSelect(WebElement parent,ElementIterator element)
	{
		super(parent,element);
		super.needEndLine=true;
	}
	@Override public String getType()
	{
		return "Select";
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
