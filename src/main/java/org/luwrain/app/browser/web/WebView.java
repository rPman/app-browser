package org.luwrain.app.browser.web;

import java.awt.Rectangle;
import java.util.Vector;

public class WebView
{
	// have multiline view of selected WebElement
	// contains list of text lines - text representation of ordered web elements
	// each line have width character limit, can contains one or multiple web elements (but single element can fill multiple lines)
	// WebView have two navigation modes and we can navigate them:
	// * list mode - usual mode with navigation Back and Forward (we can skip elements by move line by line), each element have own number but can use parent List numbers for short text representation
	// * table mode - this mode with navigation Up, Down, Left, Right and used for Table and List web elements, each element have text with number or short representation (by example table column header text)
	// elements themselves decide, how to fill WebView lines
	// each element with haveChilds() true can be used in WebView as root (operation by Enter - it refill current WebView)
	// each line and character on line can be linked to own web element and vice versa
	// tips:
	// Multiline text web element always start at new line
	
	/** structure to store WebElement link and position in line or text part inside */
	public class WebElementPart
	{
		public WebElement element;
		/** cached part of element text (full element or part, if element contains on multiple lines) */
		public String text;
		/** text length */
		public int textLength; 
		/** text part position in WebElement text */
		public int from,to;
		/** text position on line (for example 0 if element begin in line) */
		public int pos;
	}

	/** root web element used to fill lines */
	private WebElement root=null;
	/** list of lines and each contains linst of WebElement parts */
	private Vector<Vector<WebElementPart>> lines=new Vector<Vector<WebElementPart>>();
	/** cache of string lines for web elements view, must have size equal lines */ 
	private Vector<String> cache=new Vector<String>();
	
	/**
	 * @return lines count for current view
	 */
	public int getLinesCount()
	{
		return lines.size();
	}

	/** return text line from cache
	  * @param y line number, counted from 0
	  * @return full text of line by number or null, if line number out of bounds */
	public String getLineByPos(int y)
	{
		if(y<0||y>=cache.size())
			return null;
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
			return null;
		for(WebElementPart wp:lines.get(y))
		{
			if(x>=wp.pos&&x<wp.pos+wp.textLength)
				return wp;
		}
		return null;
	}
	
	/**
	 * return list of parts in line by position
	 * @param y is a line number
	 * @return list of WebElementPart
	 */
	public Vector<WebElementPart> getPartsByPos(int y)
	{
		if(y<0||y>=lines.size())
			return null;
		return lines.get(y);
	}
	
	/** refill lines and cache by new contents from element
	  * @param root element to fill lines
	  * @param width maximum number characters in line */
	public void refill(WebElement root,int width)
	{
		this.root=root;
		// cleanup
		lines=new Vector<Vector<WebElementPart>>();
		cache=new Vector<String>();
		// prepare
		widthLimit=width;
		last=null;
		lastWidth=0;
		lastPos=0;
		// refill
		refill(root);
	}
	public void refillComplex(WebElement root,int width)
	{
		this.root=root;
		// cleanup
		lines=new Vector<Vector<WebElementPart>>();
		cache=new Vector<String>();
		// prepare
		widthLimit=width;
		last=null;
		lastWidth=0;
		lastPos=0;
		// refill
		for(Vector<WebElement> row:root.getComplexMatrix())
		{
			for(WebElement element:row)
			{
				String text=element.getTextShort();
				int textLength=text.length();
				//
				boolean newline=true;
				int newWidth=lastWidth;
				if(last!=null) newWidth+=last.getSplitter().length();
				newWidth+=textLength;
				if(newWidth>widthLimit)
				{
					newline=true;
					break;
				}
				int from=0;
				int partLength=text.length();
				String splitter="";
				if(newline)
				{
					//
					lines.add(new Vector<WebElementPart>());
					cache.add("");
					lastPos=lines.size()-1; // we can use lastPos++ but size-1 more reliable
					lastWidth=0;
				} else
				{ // we known last is not null
					// get last splitter to append element
					splitter=last.getSplitter();
				}
				WebElementPart webpart=new WebElementPart();
				webpart.element=element;
				webpart.from=from;
				webpart.text=text;
				webpart.textLength=partLength;
				webpart.to=from+partLength;
				webpart.pos=lastWidth;
				lines.get(lastPos).add(webpart);
				cache.set(lastPos,cache.get(lastPos)+splitter+text); // not optimal but simple
				lastWidth+=splitter.length()+partLength;
				// next line for multiline text always new
				newline=true;
				from+=partLength;
				// 
				last=element;
			}
		}
	}
	
	/** width limit for current refill */
	private int widthLimit;
	/** last element, added to lines */
	private WebElement last;
	/** current width of last string line */
	private int lastWidth;
	/** last line num */
	private int lastPos;
	
	/** recursive method, add element to end of lines */
	private void refill(WebElement element)
	{
		if((!element.needToBeComplex()||element==root)&&element.needToBeExpanded()&&element.haveChilds())
		{ // we must expand this web element
			for(WebElement child:element.getChilds())
			{
				refill(child);
			}
		} else
		{ // we must use root element if it must not expanded or have no childs
			// get element text before (optimization - we need known text line length before using it)
			String text;
			if(element.needToBeComplex())
			{
				text=element.getTextShort();
			} else
			{
				text=element.getTextSay();
			}
			int textLength=text.length();
			String[] splited=splitTextForScreen(widthLimit,text);
			// new line or not?
			boolean newline=false;
			do // single step loop only for break statement
			{
				// first element
				if(last==null)
				{
					newline=true;
					break;
				}
				// check for last element can placed before
				if(last.needEndLine())
				{
					newline=true;
					break;
				}
				// check this element must not first at line
				if(element.needBeginLine())
				{
					newline=true;
					break;
				}
				// check this element designed on html have Y pos not like last element
				Rectangle re=element.getElement().getRect();
				Rectangle rl=last.getElement().getRect();
				if(rl.y<re.y+re.height&&re.y<=rl.y+rl.height)
				{
					newline=true;
					break;
				}
				// check for width limit
				int newWidth=lastWidth;
				if(last!=null) newWidth+=last.getSplitter().length();
				newWidth+=textLength;
				if(newWidth>widthLimit)
				{
					newline=true;
					break;
				}
			} while(false);
			// from - current position of element part in text
			int from=0;
			for(String part:splited)
			{
				int partLength=part.length();
				String splitter="";
				if(newline)
				{
					//
					lines.add(new Vector<WebElementPart>());
					cache.add("");
					lastPos=lines.size()-1; // we can use lastPos++ but size-1 more reliable
					lastWidth=0;
				} else
				{ // we known last is not null
					// get last splitter to append element
					splitter=last.getSplitter();
				}
				WebElementPart webpart=new WebElementPart();
				webpart.element=element;
				webpart.from=from;
				webpart.text=part;
				webpart.textLength=partLength;
				webpart.to=from+partLength;
				webpart.pos=lastWidth;
				lines.get(lastPos).add(webpart);
				cache.set(lastPos,cache.get(lastPos)+splitter+part); // not optimal but simple
				lastWidth+=splitter.length()+partLength;
				// next line for multiline text always new
				newline=true;
				from+=partLength;
				// 
				last=element;
			}
		}
	}
	
    static private String[] splitTextForScreen(int width,String string)
    {
    	final Vector<String> text=new Vector<String>();
    	if(string==null||string.isEmpty())
    	{
	    text.add("");
	    return text.toArray(new String[(text.size())]);
    	}
    	int i=0;
    	while(i<string.length())
    	{
	    String line;
	    if(i+width>=string.length()) // last part of string fit to the screen
		line=string.substring(i); else
	    { // too long part
		//	Log.debug("web","SPLIT: i="+i+", width="+width+", string="+string.length());
		line=string.substring(i,i+width-1);
		// walk to first stopword char at end of line FIXME: use more stopword characters than space, for example ',','.' and so on
		int sw=line.lastIndexOf(' ');
		if(sw!=-1)
		{ // have stop char, cut line to it (but include)
		    line=line.substring(0,sw);
		}
	    }
	    text.add(line);
	    i+=line.length();
    	}
    	return text.toArray(new String[(text.size())]);
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
