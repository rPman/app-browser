
package org.luwrain.app.browser.web;

import java.util.*;

import org.luwrain.core.*;

public class WebLine
{
    private final Vector<WebElementPart> parts;
    private final String text;

    WebLine(Vector<WebElementPart> parts, String text)
    {
	NullCheck.notNull(parts, "parts");
	NullCheck.notNull(text, "text");
	this.parts = parts;
	this.text = text;
    }

    WebElementPart getPartAtPos(int pos)
    {
	for(WebElementPart p: parts)
	    if(pos >= p.pos && pos < p.pos + p.textLength)
		return p;
	//Returning the first part, if there is any (why???)
	if (parts.isEmpty())
	    return null;
	return parts.get(0);
    }

    WebElementPart[] getParts()
    {
	return parts.toArray(new WebElementPart[parts.size()]);
    }

    String getText()
    {
	return text;
    }

    @Override public String toString()
    {
	return text;
    }
}
