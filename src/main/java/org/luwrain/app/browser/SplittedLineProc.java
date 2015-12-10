
package org.luwrain.app.browser;

import java.awt.Rectangle;
import java.util.*;
import org.luwrain.browser.*;
import org.luwrain.core.Log;

class SplittedLineProc
{
    static class InlineElement
    {
		String type;
		int pos; // element position in domidx
		int elIdx; // element index in current selector
		int start, end; // start and end character position for element in splitedline
		InlineElement(String type,int elIdx,int pos,int start,int end)
		{
		    this.type=type;
		    this.pos=pos;
		    this.elIdx=elIdx;
		    this.start=start;
		    this.end=end;
		}
    }
    static class SplittedLine
    {
		String text; // text representation of part of element or concatinated texts of many elements
		int index; // line index in global line count
		
		InlineElement[] elements=new InlineElement[0]; // list info about elements inside current line (one element can be inside many lines)
	
		SplittedLine(String text,int index,InlineElement[] elements)
		{
		    this.text=text;
		    this.index=index;
		    this.elements=elements;
		}
    };
    
    // used for declare element position in splittedLines as [splited][element]
    static class SplitedElementPos
    {
    	int splited; // splited line index - splittedLines
    	int line;	// line index in multiline element - splittedLines[splited]
    	int element; // element posinion in splited line - splittedLines[splited][line].elements[element] 
    	SplitedElementPos(int splited,int line,int element)
    	{
    		this.splited=splited;
    		this.line=line;
    		this.element=element;
    	}
    }
    
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

    // return splited line object by navigation area Y hotpoint position
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

    // return splited position for dom element by pos
    public SplitedElementPos getSplitedByElementDomPosition(int pos)
    {
		int splitedPos=0;
    	for(SplittedLine[] splits: splittedLines)
		{
    		int splitedLine=0;
			for(SplittedLine split:splits)
			{
				int elementPos=0;
				for(InlineElement se:split.elements)
				{
					if(se.pos==pos)
						return new SplitedElementPos(splitedPos,splitedLine,elementPos);
					elementPos++;
				}
				splitedLine++;
			}
			splitedPos++;
		}
		return null;
    }
    // return splited position info with y line index and x character position     
    public SplitedElementPos getSplitedByXY(int x,int y)
    {
		int splitedPos=0;
    	int i=0;
		for(SplittedLine[] splits:splittedLines)
		{
		    if(i+splits.length > y)
		    {
		    	if(splits.length<=y-i||y-i<0) 
		    		return null;
				int elementPos=0;
		    	for(InlineElement se:splits[y-i].elements)
		    	{
		    		if(se.start<=x&&(x<se.end||se.end==se.start))
		    			return new SplitedElementPos(splitedPos,y-i,elementPos);
					elementPos++;
		    	}
		    } 
		    i+=splits.length;
		    splitedPos++;
		}
		return null;
    }
    
    /* scan all elements via selector and call getText for each of them and split into lines, store in cache, accessed via getCachedText
     * it change current element's position to end
     * small elements (text len less than maximum width) which Y position in dom..info.rect the ?same? concatenated in one single splitedline
     */
    void splitAllElementsTextToLines(int width, Selector selector, ElementList el)
    {
		final LinkedList<SplittedLine[]> result=new LinkedList<SplittedLine[]>();
		splittedCount=0;
		int index=0;
		int elIdx=0;
		if(selector.moveFirst(el))
		{
	    	SplitedElementPos prevSePos=new SplitedElementPos(-1,-1,-1);
	    	Rectangle prevRect=null; // previous rectangle
	    	boolean prevIsEditable=false;
	    	int freeWidth=0; // current width free space in previous string line
		    do
		    {
				final boolean isEditable=el.isEditable();
				final String type = el.getType();
				final String text = isEditable?"["+el.getText().replace('\n',' ')+"]":el.getText().replace('\n',' ');
				final Rectangle rect=el.getRect();
				// only non editable element can be splited
				// TODO: make decision to split editable elements for example input (radio or checkbox) or select
				if(!isEditable&&!prevIsEditable&&prevSePos.element!=-1)
				{ // we have previous splited element
					if(text.length()<=freeWidth)
					{ // we have space for this element in previous line
						// check element position on webpage, compare Y and height in info.rect
						if(!(prevRect.y+prevRect.height<rect.y||prevRect.y>rect.y+rect.height))
						{ // the same Y position or near
							// insert element into previous splitedline
							// 0 splited line position, because we not mix multiline element and multielement in one line
							// append string to previous splited line
							if(result.size()<=prevSePos.splited)
							{
								Log.debug("web","'"+text+"', "+prevSePos.splited);
							}
							result.get(prevSePos.splited)[0].text+=" "+text;
							// add element to end of elements list (replace array)
							InlineElement[] els=result.get(prevSePos.splited)[0].elements;
							InlineElement[] nels=new InlineElement[els.length+1];
							System.arraycopy(els,0,nels,0,els.length);
							nels[els.length]=new InlineElement(type,prevSePos.splited,el.getPos(),els[els.length-1].end,els[els.length-1].end+1+text.length());
							result.get(prevSePos.splited)[prevSePos.line].elements=nels;
							// save SplitedElementPos for comparison for next element
							//prevSePos.splited=elIdx-1;
							//prevSePos.line=0;
							prevSePos.element=els.length; // pos of new element is a length previous element list
							prevRect=rect;
							prevIsEditable=isEditable;
							freeWidth-=text.length()+1;
							//
							continue;
						}
					}
				}
				// split single element to many lines
				final String[] lines = SplittedLineProc.splitTextForScreen(width,text);
				final LinkedList<SplittedLine> splitted=new LinkedList<SplittedLine>();
				int lastStringLength=0; // to get access string len outside loop
				for(String line:lines)
				{
					InlineElement se=new InlineElement(type,elIdx,el.getPos(),0,line.length());
					SplittedLine sl=new SplittedLine(line,index,new InlineElement[]{se});
				    splitted.add(sl);
				    index++;
				    //
					lastStringLength=line.length();
				}
			    // save SplitedElementPos for comparsion for next element
				prevSePos.splited=elIdx;
				prevSePos.element=0; // we have only one element
				prevSePos.line=splitted.size()-1;
				prevRect=rect;
				prevIsEditable=isEditable;
				if(lines.length==1)
				{ // single line can join with other single line elements
					freeWidth=width-1-lastStringLength;
				} else
				{ // multiline can not be joined with any other elements
					freeWidth=0;
				}
				//
				result.add(splitted.toArray(new SplittedLine[splitted.size()]));
				elIdx++;
				splittedCount+=splitted.size();
		    } while(selector.moveNext(el));
		}
		splittedLines=result.toArray(new SplittedLine[result.size()][]);
    }

    /* update split for current element text, used to update info in split text cache */
    // TODO: make decision to split editable elements for example input (radio or checkbox) or select
    void updateSplitForElementText(int width, ElementList el)
    {
		final boolean isEditable=el.isEditable();
		final String type= el.getType();
		final String text = isEditable?"["+el.getText().replace('\n',' ')+"]":el.getText().replace('\n',' ');
		String[] lines = SplittedLineProc.splitTextForScreen(width,text);
		SplitedElementPos sePos=getSplitedByElementDomPosition(el.getPos());
		if(sePos==null) return; // FIXME: make better error handling, out of bound, cache size invalid
		if(sePos.splited<0||splittedLines.length>=sePos.splited) return; // FIXME:
		if(splittedLines[sePos.splited].length==0) return; // FIXME:
		if(splittedLines[sePos.splited][0].elements.length!=1) return; // FIXME:

		int index=splittedLines[sePos.splited][0].index;
		int elIdx=splittedLines[sePos.splited][0].elements[0].elIdx;
		final Vector<SplittedLine> splitted=new Vector<SplittedLine>();
		for(String line:lines)
		{
			InlineElement se=new InlineElement(type,elIdx,el.getPos(),0,line.length());
			SplittedLine sl=new SplittedLine(line,index,new InlineElement[]{se});
		    splitted.add(sl);
		    index++;
		}
		
		splittedCount-=splittedLines[elIdx].length;
		splittedLines[elIdx]=splitted.toArray(new SplittedLine[splitted.size()]);
		splittedCount+=splitted.size();
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
			Log.debug("web","SPLIT: i="+i+", width="+width+", string="+string.length());
		line=string.substring(i,i+width-1);
		// walk to first stopword char at end of line
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
}
