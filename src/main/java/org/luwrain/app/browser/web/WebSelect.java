package org.luwrain.app.browser.web;

import org.luwrain.browser.ElementIterator;

public class WebSelect extends WebEdit
{
	public WebSelect(WebElement parent,ElementIterator element)
	{
		super(parent,element);
		super.needBeginLine=false;
		super.needEndLine=true;
	}
	@Override public Type getType()
	{
		return Type.Select;
	}
	@Override public String getTextSay()
	{
		return getType().name()+" " + nodeIt.getText();
	}
	@Override public String getTextView()
	{
		return "["+getType().name()+" " + nodeIt.getText()+"]";
	}
}
