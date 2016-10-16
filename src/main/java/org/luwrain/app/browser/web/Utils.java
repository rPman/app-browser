package org.luwrain.app.browser.web;

import java.util.Vector;

class Utils
{
    static String[] splitTextForScreen(int width, String string)
    {
    	final Vector<String> text = new Vector<String>();
    	if(string==null||string.isEmpty())
	    return new String[]{""};
    	int i = 0;
    	while(i < string.length())
    	{
	    String line;
	    if(i+width>=string.length()) // last part of string fit to the screen
		line=string.substring(i); else
	    { // too long part
		line=string.substring(i,i+width-1);
		// walk to first stopword char at end of line FIXME: use more stopword characters than space, for example ',','.' and so on
		final int sw = line.lastIndexOf(' ');
		if(sw!=-1)
		    line = line.substring(0,sw);
	    }
	    text.add(line);
	    i += line.length();
    	}
    	return text.toArray(new String[(text.size())]);
    }
}
