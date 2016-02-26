package org.luwrain.app.browser.web;

import org.luwrain.browser.ElementIterator;

public class WebTableCell extends WebText
{
	/** usuali it is list number element in multilevel or simle lists, changed by WebList in constructor */
	protected String name="*";
	
	public WebTableCell(WebElement parent,ElementIterator element)
	{
		super(parent,element);
	}
	@Override public String getType()
	{
		return "Table cell";
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
