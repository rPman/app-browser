
package org.luwrain.app.browser;

class ScreenDownload implements Constants
{ // contains first line - file name, file size, progress and last multiline - download link

    FileDownloadThread fileDownloadThread;

    String filename="";
    String filetype=null;
    Integer filesize=null;
    Integer progress=null;

    String[] link=new String[1];

    ScreenDownload(FileDownloadThread thread)
    {
	fileDownloadThread = thread;
    }

	void setLink(String string)
    	{
	    link=Constants.splitTextForScreen(string);
    	}

	int getLinesCount()
    	{
	    return 4+link.length;
    	}

	String getStringByLine(int line)
    	{
	    if(line==0) return filename;
	    if(line==1) return filetype==null?null:PAGE_DOWNLOAD_FIELD_FILETYPE+filetype;
	    if(line==2) return filesize==null?null:PAGE_DOWNLOAD_FIELD_FILESIZE+filesize;
	    if(line==3) return progress==null?null:PAGE_DOWNLOAD_FIELD_PROGRESS+(progress==100?PAGE_DOWNLOAD_FIELD_PROGRESS_FINISHED:progress+"%");
	    if(line>=4&&line<4+link.length)
	    {
		int subline=line-4;
		return link[subline];
	    }
	    return null;
    	}

	void breakExecution()
    	{
	    if(fileDownloadThread.isAlive())
	    {
		fileDownloadThread.interrupt();
	    }
    	}
        void refreshInfo()
        {
	    /*
	    if(screenMode==ScreenMode.DOWNLOAD)
		environment.onAreaNewContent(that);
	    */
        }
    }
