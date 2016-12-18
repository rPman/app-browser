package org.luwrain.app.browser.web;

import java.util.*;

import org.luwrain.core.*;

public class WebIterator
{
    private final WebView view;
    private int index = 0;

    WebIterator(WebView view)
    {
	NullCheck.notNull(view, "view");
	this.view = view;
    }

    public int getPosX()
    {
	return 0;
    }

    public int getPosY()
    {
	return index;
    }

    public String getText()
    {
	return view.getLine(index);
    }

    public boolean moveNext()
    {
	if(index >= view.getLineCount())
	    return false;
	++index;
	return true;
    }

    public boolean movePrev()
    {
	if(index <= 0)
	    return false;
	--index;
	return true;
    }

    public WebElement getElementAtPos(int pos)
    {
	final WebElementPart part = view.getWebLine(index).getPartAtPos(pos);
	return part != null?part.element:null;
    }
}
