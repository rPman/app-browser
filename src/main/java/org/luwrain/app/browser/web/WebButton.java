package org.luwrain.app.browser.web;

import org.luwrain.browser.ElementIterator;

class WebButton extends WebText
{
    WebButton(WebElement parent,ElementIterator element)
    {
	super(parent, element);
    }

    @Override public Type getType()
    {
	return Type.Button;
    }

    @Override public String getText()
    {
	return "{" + nodeIt.getText() + "}";
    }
}
