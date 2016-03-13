package org.luwrain.app.browser.web;

import org.luwrain.browser.ElementIterator;

public class WebTableRow extends WebText
{
	/** usuali it is list number element in multilevel or simle lists, changed by WebList in constructor */
	protected String name=null;
	
	public WebTableRow(WebElement parent,ElementIterator element)
	{
		super(parent,element);
		super.needBeginLine=true;
		super.needEndLine=true;
	}
	@Override public Type getType()
	{
		return Type.TableRow;
	}
	@Override public String getTextSay()
	{
		return rootElement.getText();
	}
	@Override public String getTextView()
	{
		return rootElement.getText();
	}
	@Override public String getTextShort()
	{
		return name;
	}

}