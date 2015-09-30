
package org.luwrain.app.browser;
    class ScreenText implements Constants
    { // contains one first line with type of element, text multiline and multiline link
	String type="";
	String[] text=new String[1];
	int linkLine=2;
	String[] link=new String[0]; // it for anchor and images link

	void setType(String string)
    	{
	    type=string;
    	}

	void setLink(String string)
    	{
	    link = Constants.splitTextForScreen(string);
    	}

	void setText(String string)
    	{
	    text = Constants.splitTextForScreen(string);
	    linkLine=1+text.length;
    	}

	int getLinesCount()
    	{
	    return 1+text.length+link.length;
    	}

	String getStringByLine(int line)
    	{
	    // type
	    if(line==0) return type;
	    // text
	    if(line>=1&&line<1+text.length)
	    {
		int subline=line-1;
		return text[subline];
	    }
	    // link
	    int subline=line-1-text.length;
	    return link[subline];
    	}
    }
