package org.luwrain.app.browser.web;

import java.awt.Rectangle;
import java.util.Vector;

public class WebBuilderNormal implements WebViewBuilder
{
	/** root web element used to fill lines */
	private WebElement root=null;
	/** list of lines and each contains linst of WebElement parts */
	private Vector<Vector<WebElementPart>> lines=new Vector<Vector<WebElementPart>>();
	/** cache of string lines for web elements view, must have size equal lines */ 
	private Vector<String> cache=new Vector<String>();

	/** width limit for current refill */
	private int widthLimit;
	/** last element, added to lines */
	private WebElement last;
	/** current width of last string line */
	private int lastWidth;
	/** last line num */
	private int lastPos;

	@Override public void refill(WebView wView,WebElement root,int width)
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
		// move result to view
		wView.setLines(lines);
		wView.setCache(cache);
	}

	/** recursive method, add element to end of lines */
	private void refill(WebElement element)
	{
		if((!element.needToBeComplex()||element==root)&&element.needToBeExpanded()&&element.haveChildren())
		{ // we must expand this web element
			for(WebElement child:element.getChildren())
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
			String[] splited=Utils.splitTextForScreen(widthLimit,text);
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

}
