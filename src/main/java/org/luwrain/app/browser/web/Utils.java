package org.luwrain.app.browser.web;

import java.util.Vector;

public class Utils
{
    static public String[] splitTextForScreen(int width,String string)
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
}
