
package org.luwrain.app.browser.web;

import java.util.*;

import org.luwrain.core.*;

public class Row
{
    private final Vector<WebElementPart> parts;

    Row(Vector<WebElementPart> parts)
    {
	this.parts = parts;
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
}
