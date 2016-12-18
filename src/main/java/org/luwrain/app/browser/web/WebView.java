
package org.luwrain.app.browser.web;

import java.util.*;

import org.luwrain.core.*;

public class WebView
{
    private final Vector<WebLine> lines;

    WebView(Vector<Vector<WebElementPart>> parts, Vector<String> lines)
    {
	NullCheck.notNull(parts, "parts");
	NullCheck.notNull(lines, "lines");
	this.lines = new Vector<WebLine>();
	this.lines.setSize(parts.size());
	for(int i = 0;i < parts.size();++i)
	    this.lines.set(i, new WebLine(parts.get(i), lines.get(i)));
    }

    public int getLineCount()
    {
	return lines.size();
    }

    public String getLine(int y)
    {
	if(y < 0 || y >= lines.size())
	    return "";
	return lines.get(y).getText();
    }

    public WebElementPart getPartByPos(int x, int y)
    {
	if(y < 0 || y >= lines.size())
	    throw new IllegalArgumentException("y = " + y);
	return lines.get(y).getPartAtPos(x);
    }

    public WebIterator createIterator()
    {
	return new WebIterator(this);
    }

    public WebElementPart[] getPartsOnLine(int index)
    {
	if(index < 0 || index >= lines.size())
	    throw new IllegalArgumentException("index = " + index);
	return lines.get(index).getParts();
    }

    String[] getLines()
    {
	final String[] res = new String[lines.size()];
	for(int i = 0;i < lines.size();++i)
	    res[i] = lines.get(i).getText();
	return res;
    }

    WebLine[] getWebLines()
    {
	return lines.toArray(new WebLine[lines.size()]);
    }
}
