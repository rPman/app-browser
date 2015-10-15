
package org.luwrain.app.browser;

import java.util.*;
import org.luwrain.browser.*;

class SplittedLineProc
{
    static class SplittedLine
    {
		String type;
		String text;
		int pos; // element position in domidx
		int index; // line index in global line count
		int elIdx; // element index in current selector
	
		SplittedLine(String type,String text,int pos,int index,int elIdx)
		{
		    this.type=type;
		    this.text=text;
		    this.pos=pos;
		    this.index=index;
		    this.elIdx=elIdx;
		}
    };
    private SplittedLine[][] splittedLines=new SplittedLine[0][];
    private int splittedCount=0;

    SplittedLine[][] getSplittedLines()
    {
	return splittedLines;
    }

    int getSplittedCount()
    {
	return splittedCount;
    }

    SplittedLine getSplittedLineByIndex(int index)
    {
	int i=0;
	for(SplittedLine[] split:splittedLines)
	{
	    if(i+split.length > index)
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
    void splitAllElementsTextToLines(int width,ElementList.Selector selector, ElementList el)
    {
	final LinkedList<SplittedLine[]> result=new LinkedList<SplittedLine[]>();
	splittedCount=0;
	int index=0;
	int elIdx=0;
	if(selector.first(el))
	{
	    do {
		final String type = el.getType();
		final String text = el.getText();
		final String[] lines = SplittedLineProc.splitTextForScreen(width,text);
		final LinkedList<SplittedLine> splitted=new LinkedList<SplittedLine>();
				for(String line:lines) 
				    splitted.add(new SplittedLine(type,line, el.getPos(),index++,elIdx));
				result.add(splitted.toArray(new SplittedLine[splitted.size()]));
				elIdx++;
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
		SplittedLine[] sl=getSplittedLineByPos(el.getPos());
		if(sl==null||sl.length==0) return; // FIXME: make better error handling, out of bound, cache size invalid
		int slIdx=sl[0].index;
		int index=slIdx;
		int elIdx=sl[0].elIdx;
		final Vector<SplittedLine> splitted=new Vector<SplittedLine>();
		for(String line:lines)
		    splitted.add(new SplittedLine(type,line,el.getPos(),index++,elIdx));
		splittedCount-=splittedLines[elIdx].length;
		splittedLines[elIdx]=splitted.toArray(new SplittedLine[splitted.size()]);
		splittedCount += splitted.size();
	}

    static private String[] splitTextForScreen(int width,String string)
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
