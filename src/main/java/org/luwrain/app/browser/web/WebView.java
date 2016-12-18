
package org.luwrain.app.browser.web;

import java.util.*;

import org.luwrain.core.*;

public class WebView
{
    // have multiline view of selected WebElement
    // contains list of text lines - text representation of ordered web elements
    // each line have width character limit, can contains one or multiple web elements (but single element can fill multiple lines)
    // WebView have two navigation modes and we can navigate them:
    // * list mode - usual mode with navigation Back and Forward (we can skip elements by move line by line), each element have own number but can use parent List numbers for short text representation
    // * table mode - this mode with navigation Up, Down, Left, Right and used for Table and List web elements, each element have text with number or short representation (by example table column header text)
    // elements themselves decide, how to fill WebView lines
    // each element with haveChildren() true can be used in WebView as root (operation by Enter - it refill current WebView)
    // each line and character on line can be linked to own web element and vice versa
    // tips:
    // Multiline text web element always start at new line

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

    /**
     * @return lines count for current view
     */
    public int getLineCount()
    {
	return lines.size();
    }

    /** return text line from cache
     * @param y line number, counted from 0
     * @return full text of line by number or null, if line number out of bounds */
    public String getLine(int y)
    {
	if(y < 0 || y >= lines.size())
	    return "";
	return lines.get(y).getText();
    }

    /**
     * return element and it's position in line for coordinates
     * @param x is a character position in line
     * @param y is a line number
     * @return WebElementPart in this position or null 
     */
    public WebElementPart getPartByPos(int x, int y)
    {
	if(y < 0 || y >= lines.size())
	    throw new IllegalArgumentException("y = " + y);
	return lines.get(y).getPartAtPos(x);
    }

    /**
     * return list of parts in line by position
     * @param y is a line number
     * @return list of WebElementPart
     */
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
