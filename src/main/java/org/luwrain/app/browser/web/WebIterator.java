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

    /** move position to begin of current part in current line
     * @return true if nowhere move */
    boolean moveToCurrentPart()
    {
	/*
	// determine current part and all parts in lines
	WebElementPart part = view.getPartByPos(posX,posY);
	if(part==null) return true;
	posX=part.pos;
	*/
	return false;
    }

    /** move position to previous WebPart (on this line or previous
     * @return true if nowhere move */
    boolean moveToPrevPart()
    {
	/*
	// determine current part and all parts in lines
	WebElementPart part = view.getPartByPos(posX,posY);
	if(part==null) return true;
	final WebElementPart[] line = view.getPartsOnLine(posY);
	if(line==null) return true;
	// line.indexOf(part)
	for(int i=0;i<line.length;i++)
	    if(line[i]==part)
	    { // part found
		// FIXME: we have bug in WebViewBuilder, line can contains zero length parts, now we fix it here but need fix in builder
		if(i<=0||(posX==line[i-1].pos&&posX==0))
		{ // first part in line, move up and end of line and call moveToCurrentPart()
		    // can't move up?
		    if(posY==0) return true;
		    // move to and end of previous line
		    posY--;
		    posX = view.getLine(posY).length();
		    return moveToCurrentPart();
		} else
		{ // prev part on this line
		    posX=line[i-1].pos;
		    return false;
		}
	    }
	// part not found, this can happened only in dev error
	Log.error("browser","moveToPrevPart can't found own element part in line");
	*/
	return true;
    }

    /** move position to next WebPart (on this line or next)
     * @return true if nowhere move */
    boolean moveToNextPart()
    {
	/*
	// determine current part and all parts in lines
	WebElementPart part = view.getPartByPos(posX,posY);
	if(part==null) return true;
	final WebElementPart[] line = view.getPartsOnLine(posY);
	if(line==null) return true;
	// line.indexOf(part)
	for(int i=0;i<line.length;i++)
	    if(line[i]==part)
	    { // part found
		if(i>=line.length-1)
		{ // last part in line, move down and begin of line
		    // can't move up?
		    if(posY >= view.getLineCount())
			return true;
		    // move to begin of next line
		    posY++;
		    posX=0;
		    return false;
		} else
		{ // bext part on this line
		    posX=line[i+1].pos;
		    return false;
		}
	    }
	// part not found, this can happened only in dev error
	Log.error("browser","moveToPrevPart can't found own element part in line");
	*/
	return true;
    }
}
