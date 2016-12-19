
package org.luwrain.app.browser.web;

import org.luwrain.browser.ElementIterator;

public class WebEdit extends WebText
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

    @Override public String getText()
    {
	return "[" + nodeIt.getText() + "]";
    }
}
