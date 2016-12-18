package org.luwrain.app.browser.web;

import java.util.*;

import org.luwrain.core.*;

public class WebIterator
{
    private final WebView view;
    //	private Vector<Vector<WebElementPart>> lines;
    //    private Vector<String> cache;

    /** current position */
	private int posX=0;
    private int posY=0;

public WebIterator(WebView view)
    {
	this.view = view;
    }
    
    public int getPosX()
	{
	return posX;
	}

	public int getPosY()
	{
	return posY;
	}

	public void setPosX(int posX)
	{
	// limit X position to current line string size +1 (we can move cursor out of element part)
	    if(posX > view.getLines().get(posY).length()) 
		posX = view.getLines().get(posY).length();
	this.posX=posX;
	}

	public void setPosY(int posY)
	{
	// limit Y position to lines count
	    if(posY >= view.getRows().size()) 
		posY = view.getRows().size();
	this.posY=posY;
	}
	
	/** move position to next line
	 * @return true if nowhere move */
	public boolean moveNextLine()
	{
	    if(posY >= view.getRows().size()) 
return true;
	posY++;
	posX=0;
	return false;
	}

	/** move position to previous line 
	 * @return true if nowhere move */
	public boolean movePrevLine()
	{
	if(posY<=0) return true;
	posY--;
	posX=0;
	return false;
	}

	/** move position left for one char
	 *  @return true if nowhere move */
	public boolean moveLeftByChar()
	{
	if(posX<=0) return true;
	posX--;
	return false;
	}

	/** move position right for one char
	 * @return true if nowhere move */
	public boolean moveRightByChar()
	{
	    if(posX > view.getLines().get(posY).length()) 
return true;
	posX++;
	return false;
	}
	
	/** move position to begin of current part in current line
	 * @return true if nowhere move */
	public boolean moveToCurrentPart()
	{
	// determine current part and all parts in lines
	WebElementPart part = view.getPartByPos(posX,posY);
	if(part==null) return true;
	posX=part.pos;
	return false;
	}
	
	/** move position to previous WebPart (on this line or previous
	 * @return true if nowhere move */
	public boolean moveToPrevPart()
	{
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
			posX = view.getLines().get(posY).length();
			return moveToCurrentPart();
		} else
		{ // prev part on this line
			posX=line[i-1].pos;
			return false;
		}
	}
	// part not found, this can happened only in dev error
	Log.error("browser","moveToPrevPart can't found own element part in line");
	return true;
	}
	
	/** move position to next WebPart (on this line or next)
	 * @return true if nowhere move */
	public boolean moveToNextPart()
	{
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
		    if(posY >= view.getRows().size()) 
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
	return true;
	}
	
}
