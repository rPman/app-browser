
package org.luwrain.app.browser.web;

import java.awt.Rectangle;
import java.util.Vector;

import org.luwrain.core.*;

class WebBuilderNormal implements WebViewBuilder
{
	private final WebElement root;
	private final Vector<Vector<WebElementPart>> lines = new Vector<Vector<WebElementPart>>();;
	private final Vector<String> cache = new Vector<String>();

    /**The maximum line length we may use in our process*/
    private final int maxWidth;

	/** last element, added to lines */
	private WebElement last;

	/** current width of last string line */
	private int lastWidth;

	/** last line num */
	private int lastNum;

    WebBuilderNormal(WebElement root, int maxWidth)
    {
	NullCheck.notNull(root, "root");
	this.root = root;
	this.maxWidth = maxWidth;
    }

    @Override public WebView build()
    {
		last = null;
		lastWidth = 0;
		lastNum = 0;
		build(root);
		return new WebView(lines, cache);
    }

	private void build(WebElement element)
	{
	    NullCheck.notNull(element, "element");
		if((!element.isComplex() || element == root) &&
			 element.needToBeExpanded() && element.hasChildren())
		{ // we must expand this web element
			for(WebElement child: element.getChildren())
			    build(child);
		} else
		{ // we must use root element if it must not expanded or have no childs
			// get element text before (optimization - we need known text line length before using it)
			final String text;
			if(element.isComplex())
				text = element.getTextShort(); else
				text = element.getTextSay();
			final int textLength = text.length();
			final String[] splitted = Utils.splitTextForScreen(maxWidth, text);
			// new line or not?
			boolean newline=false;
			do {// single step loop only for break statement
				// first element
				if(last == null)
				{
					newline = true;
					break;
				}
				// check for last element can placed before
				if(last.needEndLine())
				{
					newline = true;
					break;
				}
				// check this element must not first at line
				if(element.alwaysFromNewLine())
				{
					newline=true;
					break;
				}
				// check this element designed on html have Y pos not like last element
				Rectangle re=element.getNode().getRect();
				final Rectangle rl=last.getNode().getRect();
				if(rl.y>=re.y+re.height||re.y>=rl.y+rl.height)
				{
					newline=true;
					break;
				}
				// check for width limit
				int newWidth = lastWidth;
				if(last!=null) 
					newWidth+=last.getSplitter().length();
				newWidth+=textLength;
				if(newWidth > maxWidth)
				{
					newline=true;
					break;
				}
			} while(false);
			// from - current position of element part in text
			int from=0;
			for(String part: splitted)
			{
				final int partLength=part.length();
				String splitter="";
				if(newline)
				{
					lines.add(new Vector<WebElementPart>());
					cache.add("");
					lastNum = lines.size() - 1;//More reliable than ++lastNum
					lastWidth = 0;
				} else
				{ //We sure last is not null
					// get last splitter to append element
					splitter = last.getSplitter();
				}
				//				final WebElementPart webpart=new WebElementPart(element, part, partLength, lastWidth, from, from + partLength);
				lines.get(lastNum).add(new WebElementPart(element, part, partLength, lastWidth, from, from + partLength));
				cache.set(lastNum,cache.get(lastNum)+splitter+part); // not optimal but simple
				lastWidth += splitter.length()+partLength;
				// next line for multiline text always new
				newline = true;
				from += partLength;
				last = element;
			}
		}
	}
}
