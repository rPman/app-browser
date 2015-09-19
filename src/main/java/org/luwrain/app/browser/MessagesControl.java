package org.luwrain.app.browser;

import java.util.Iterator;
import java.util.Vector;

import javax.swing.JOptionPane;

import org.luwrain.core.Log;

public class MessagesControl
{
	static abstract class Message
	{
		public boolean todel=false;
		public void remove()
		{
			todel=true;
		}
		abstract public void doit();
	}
	
	static public class Alert extends Message
	{
		public String title,message;
		public Alert(String title,String message)
		{
			this.title=title;
			this.message=message;
		}
		@Override public void doit()
		{
			JOptionPane.showMessageDialog(null,message,"Alert",JOptionPane.INFORMATION_MESSAGE);
		}
	}
	static public class Prompt extends Message
	{
		public String message,value,result=null;
		public Prompt(String message,String value)
		{
			this.message=message;
			this.value=value;
		}
		@Override public void doit()
		{
			result=JOptionPane.showInputDialog(null,message,value);
		}
	}
	static public class Confirm extends Message
	{
		public String message;
		public boolean result;
		public Confirm(String message)
		{
			this.message=message;
		}
		@Override public void doit()
		{
			result=JOptionPane.showConfirmDialog(null,message,"Choose yes or no",JOptionPane.YES_NO_OPTION)==1;
		}
	}
	
	Vector<Message> messages=new Vector<Message>();
	
	public void doit()
	{
		Iterator<Message> iterator=messages.iterator();
		while(iterator.hasNext())
		{
			Message message=iterator.next();
			if(message.todel)
			{ // remove any message, market as todel (from last using)
				Log.debug("msgctl","remove "+message);
				iterator.remove();
			} else
			{
				message.doit();
				message.notify();
			}
		}
	}
	
}
