
package org.luwrain.app.browser;

import java.net.*;

class ScreenPage implements Constants
{
    String[] url=new String[1];
    int urlLine=0;
    String[] title=new String[1];
    int titleLine=1;
    String state="";
    int stateLine=2;
    String progress;
    int progressLine=3;
    String size="";
    int sizeLine=4;

    void changedUrl(String string)
    {
	url=Constants.splitTextForScreen(PAGE_SCREEN_TEXT_URL+string);
	titleLine=   0+url.length;
	stateLine=   0+url.length+title.length;
	progressLine=1+url.length+title.length;
	sizeLine=    2+url.length+title.length;
    }

    void changedTitle(String string)
    {
	title=Constants.splitTextForScreen(PAGE_SCREEN_TEXT_TITLE+string);
	stateLine=   0+url.length+title.length;
	progressLine=1+url.length+title.length;
	sizeLine=    2+url.length+title.length;
    }

    void changedState(String string)
    {
	state=PAGE_SCREEN_TEXT_STATE+string;
    }

    void changedProgress(double num)
    {
	progress=PAGE_SCREEN_TEXT__PROGRESS+Integer.toString((int)(num*100))+"%";
    }

    void changedSize(int num)
    {
	size=PAGE_SCREEN_TEXT_SIZE+Integer.toString(num);
    }

    int getLinesCount()
    {
	return sizeLine+1;
    }

    String getStringByLine(int line)
    {
	if(line>=urlLine&&line<urlLine+url.length)
	{
	    int subline=line-urlLine;
	    return url[subline];
	} else
	    if(line>=titleLine&&line<titleLine+title.length)
	    {
		int subline=line-titleLine;
		return title[subline];
	    } else
		if(line==stateLine) return state;else
		    if(line==progressLine) return progress;else
			if(line==sizeLine) return size;
	return "";
    }
}
