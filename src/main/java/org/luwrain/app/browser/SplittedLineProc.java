
package org.luwrain.app.browser;

import java.util.*;

import org.luwrain.browser.*;

class SplittedLineProc
{
	static public class SplittedLine
	{
		public String type;
		public String text;
		public int pos; // element position in domidx
		public int index; // line index in global line count

		public SplittedLine(String type,String text,int pos,int index)
		{
			this.type=type;
			this.text=text;
			this.pos=pos;
			this.index=index;
		}
	};


	private SplittedLine[][] splittedLines=new SplittedLine[0][];
	private int splittedCount=0;

    public SplittedLine[][] getSplittedLines()
    {
	return splittedLines;
    }

    public int getSplittedCount()
    {
	return splittedCount;
    }

    public SplittedLine getSplittedLineByIndex(int index)
    {
	int i=0;
	for(SplittedLine[] split:splittedLines)
	{
	    if(i+split.length>index)
		return split[index-i]; 
	    i+=split.length;
	}
	return null;
    }

    public SplittedLine[] getSplittedLineByPos(int pos)
    {
	for(SplittedLine[] split: splittedLines)
	{
	    if(split[0].pos==pos) return split; 
	}
	return null;
    }

    /* scan all elements via selector and call getText for each of them and split into lines, store in cache, accessed via getCachedText
     * it change current position to end
     */
    public void splitAllElementsTextToLines(int width,ElementList.Selector selector, ElementList el)
    {
	final Vector<SplittedLine[]> result=new Vector<SplittedLine[]>();
	splittedCount=0;
	int index=0;
	if(selector.first(el))
	{
	    do {
		String type = el.getType();
		String text = el.getText();
		String[] lines = SplittedLineProc.splitTextForScreen(width,text);
		final Vector<SplittedLine> splitted=new Vector<SplittedLine>();
				for(String line:lines) 
				    splitted.add(new SplittedLine(type,line, el.getPos(),index++));
				result.add(splitted.toArray(new SplittedLine[splitted.size()]));
				splittedCount+=splitted.size();
	    } while(selector.next(el));
	}
	splittedLines=result.toArray(new SplittedLine[result.size()][]);
    }

    /* update split for current element text, used to update info in split text cache */
    void updateSplitForElementText(int width, ElementList el)
    {
	String type= el.getType();
		String text = el.getText();
		String[] lines = SplittedLineProc.splitTextForScreen(width,text);
		if(splittedLines.length < el.getPos()) return; // FIXME: make better error handling, out of bound, cache size invalid
		final Vector<SplittedLine> splitted=new Vector<SplittedLine>();
		int index=0;
		for(String line:lines)
		{
		    splitted.add(new SplittedLine(type,line,el.getPos(),index));
			index++;
		}
		splittedCount-=splittedLines[el.getPos()].length;
		splittedLines[el.getPos()]=splitted.toArray(new SplittedLine[splitted.size()]);
		splittedCount += splitted.size();
	}

    public static String[] splitTextForScreen(int width,String string)
    {
    	final LinkedList<String> text=new LinkedList<String>();
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
		line=string.substring(i,i+width-1);
		// walk to first stopword char at end of line
		int sw=line.lastIndexOf(' ');
		if(sw!=-1)
		{ // have stop char, cut line to it (but include)
		    line=line.substring(0,sw);
		}
	    }
	    // check for new line char
	    final int nl=line.indexOf('\n');
			if(nl!=-1)
			{ // have new line char, cut line to it
			    line=line.substring(0,nl);
			    i++; // skip new line
			}
			text.add(line);
			i+=line.length();
    	}
    	return text.toArray(new String[(text.size())]);
    }
}
