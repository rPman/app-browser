package org.luwrain.app.browser.web;

import org.luwrain.browser.ElementIterator;

public class WebEdit extends WebText
{
	public WebEdit(WebElement parent,ElementIterator element)
	{
		super(parent,element);
		super.needBeginLine=false;
		super.needEndLine=true;
	}
	@Override public Type getType()
	{
		return Type.Edit;
	}
	@Override public String getTextSay()
	{
		return getType().name()+" "+rootElement.getText();
	}
	@Override public String getTextView()
	{
		return "["+getType().name()+" "+rootElement.getText()+"]";
	}

}
