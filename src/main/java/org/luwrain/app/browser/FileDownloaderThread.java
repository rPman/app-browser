
package org.luwrain.app.browser;

import javax.swing.SwingUtilities;
import java.net.*;
import java.io.*;
import java.util.*;

class FileDownloadThread extends Thread implements Constants
{
        String downloadLink=null;

        public void run() 
        {
	    SwingUtilities.invokeLater(new Runnable() { @Override public void run()
		    {
			//        		screenDownload.setLink(downloadLink);
			//        		screenDownload.refreshInfo();
		    }});

	    HttpURLConnection httpConn=null;

	    try
	    {
		if(downloadLink==null) return; // fixme: make dev error handling
		URL url = new URL(downloadLink);
		httpConn = (HttpURLConnection) url.openConnection();
		int responseCode = httpConn.getResponseCode();

		// always check HTTP response code first
		if (responseCode == HttpURLConnection.HTTP_OK)
		{
		    String fileName = "";
		    String disposition = httpConn.getHeaderField("Content-Disposition");
		    String contentType = httpConn.getContentType();
		    int contentLength = httpConn.getContentLength();

		    if (disposition != null)
		    {
			// extracts file name from header field
			int index = disposition.indexOf("filename=");
			if (index > 0)
			{
			    fileName = disposition.substring(index + 10,disposition.length() - 1);
			}
		    } else {
			// extracts file name from URL
			// FIXME: make better filename extraction from url
			fileName = downloadLink.substring(downloadLink.lastIndexOf("/") + 1,downloadLink.length());
		    }

		    //System.out.println("Content-Type = " + contentType);
		    //System.out.println("Content-Disposition = " + disposition);
		    //System.out.println("Content-Length = " + contentLength);
		    //System.out.println("fileName = " + fileName);

		    final String fileType_=contentType;
		    final String fileName_=fileName;
		    final int fileSize_=contentLength;
		    SwingUtilities.invokeLater(new Runnable() { @Override public void run()
			    {
				//				screenDownload.filename=fileName_;
				//				screenDownload.filesize=fileSize_;
				//				screenDownload.filetype=fileType_;
				//				screenDownload.refreshInfo();
			    }});
	
		    // opens input stream from the HTTP connection
		    InputStream inputStream;
		    inputStream=httpConn.getInputStream();
		    String saveFilePath = DEFAULT_DOWNLOAD_DIR+File.separator + fileName;

		    // opens an output stream to save into file
		    FileOutputStream outputStream = new FileOutputStream(saveFilePath);

		    int bytesRead = -1;
		    int bytesAll=0;
		            byte[] buffer = new byte[BUFFER_SIZE];
		            Date prev=new Date();
		            while ((bytesRead = inputStream.read(buffer)) != -1)
		            {
		                bytesAll+=bytesRead;
		                outputStream.write(buffer, 0, bytesRead);
		                // show progress, not more often then 1 times per second
		                Date now=new Date();
		                if(now.getTime()-prev.getTime()>=1000)
		                { // show progress
				    final int downloadedSize=bytesAll;
				    SwingUtilities.invokeLater(new Runnable() { @Override public void run()
					    {
						//		                		screenDownload.progress=100*downloadedSize/screenDownload.filesize;
						//		                		screenDownload.refreshInfo();
					    }});
		                }
		            }

		            outputStream.close();
		            inputStream.close();
		            // download finished
			    SwingUtilities.invokeLater(new Runnable() { @Override public void run()
				    {
					//					screenDownload.progress=100;
					//					screenDownload.refreshInfo();
					//			        	environment.say(PAGE_DOWNLOAD_FINISHED);
				    }});
			    //System.out.println("File downloaded");
		} else
		{
		    //System.out.println("No file to download. Server replied HTTP code: " + responseCode);
		    throw new Exception();
		}
	    } catch(Exception e)
	    {
		// download failed
		SwingUtilities.invokeLater(new Runnable() { @Override public void run()
		        {
			    //			    environment.say(PAGE_DOWNLOAD_FAILED);
		        }});
		e.printStackTrace();
	    }
	    finally
	    {
		if(httpConn!=null) httpConn.disconnect();
	    }
        }
    }
