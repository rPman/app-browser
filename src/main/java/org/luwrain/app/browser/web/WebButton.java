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

    @Override public String getTextSay()
    {
	return "Кнопка " + nodeIt.getText();
    }

    @Override public String getTextView()
    {
	return "["+getType()+" " + nodeIt.getText() + "]";
    }
}
