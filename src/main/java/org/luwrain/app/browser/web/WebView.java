
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

    /** The list of lines, each item is a list of WebElement parts*/
    private final Vector<Vector<WebElementPart>> lines;

    /** The text cache, representing web elements view, must be of the same size as a number of lines*/ 
    private final Vector<String> cache;

    public WebView()
    {
	lines = new Vector<Vector<WebElementPart>>();
cache = new Vector<String>();
    }

    WebView(Vector<Vector<WebElementPart>> lines, Vector<String> cache)
    {
	NullCheck.notNull(lines, "lines");
	NullCheck.notNull(cache, "cache");
	this.lines = lines;
	this.cache = cache;
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
    public String getLineByIndex(int y)
    {
	if(y<0||y>=cache.size())
	    throw new IllegalArgumentException("y = " + y);
	return cache.get(y);
    }

    /**
     * return element and it's position in line for coordinates
     * @param x is a character position in line
     * @param y is a line number
     * @return WebElementPart in this position or null 
     */
    public WebElementPart getElementByPos(int x,int y)
    {
	if(y<0||y>=lines.size())
	    throw new IllegalArgumentException("y = " + y);
	for(WebElementPart wp:lines.get(y))
	{
	    if(x>=wp.pos&&x<wp.pos+wp.textLength)
		return wp;
	}
	// we need return first element on line
	if(lines.get(y).size()==0) 
	    return null;
	return lines.get(y).get(0);
    }

    /**
     * return list of parts in line by position
     * @param y is a line number
     * @return list of WebElementPart
     */
    public Vector<WebElementPart> getPartsByLineIndex(int y)
    {
	if(y<0||y>=lines.size())
	    throw new IllegalArgumentException("y = " + y);
	return lines.get(y);
    }

    public void print()
    {
	for(int i=0;i<lines.size();i++)
	{
	    System.out.print("parts: ");
	    for(WebElementPart part:lines.get(i))
		System.out.print(part.element.getTextShort()+"["+part.element.hashCode()+"]{"+part.text.replace('\n',' ')+"} ");
	    System.out.println();//" cache:"+cache.get(i));
	}
    }
}
