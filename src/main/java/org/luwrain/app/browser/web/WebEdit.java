
package org.luwrain.app.browser.web;

import org.luwrain.browser.ElementIterator;

class WebEdit extends WebText
{
    WebEdit(WebElement parent, ElementIterator element)
    {
	super(parent,element);
	super.needBeginLine = false;
	super.needEndLine = true;
    }

    @Override public Type getType()
    {
	return Type.Edit;
    }

    @Override public String getTextSay()
    {
	return "Поле для ввода " + nodeIt.getText();
    }

    @Override public String getTextView()
    {
	return "["+getType().name()+" " + nodeIt.getText()+"]";
    }
}
