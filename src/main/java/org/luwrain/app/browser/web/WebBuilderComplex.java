package org.luwrain.app.browser.web;

import java.util.Vector;

public class WebBuilderComplex implements WebViewBuilder
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
		// move result to view
		wView.setLines(lines);
		wView.setCache(cache);
	}
}
