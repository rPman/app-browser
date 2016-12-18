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
	@Override public Type getType()
	{
		return Type.TableCell;
	}

	@Override public String getText()
	{
		return nodeIt.getText();
	}

	@Override public String getTextShort()
	{
		return name;
	}

}
