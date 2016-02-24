package org.luwrain.app.browser.web;

import java.util.Iterator;

import org.luwrain.browser.Browser;
import org.luwrain.browser.ElementIterator;
import org.luwrain.browser.Selector;
import org.luwrain.browser.SelectorChilds;

public class WebDocument
{
	// make WebDocument structure for web page, more simple than html document, i.e.  only visible elements and without element with single child
	// only visible elements
	public WebElement root=null;
	
	/**
	 * replace WebElement structure for given web page
	 * @param page - web page
	 */
	public void make(Browser page)
	{
		// get all childs without parent (it must have only one)
		SelectorChilds rootChilds=page.rootChilds(false);
		ElementIterator list=page.iterator();
		rootChilds.moveFirst(list);
		root=new WebText(null,list.clone());
		// check for all childs
		make_(page,root,list.getChilds(false));
		// * repair structure
		// remove nodes without childs and invisible
		cleanup(root);
	}
	
	private void make_(Browser page,WebElement parent,Selector childs)
	{
		ElementIterator list=page.iterator();
		// have no childs
		if(!childs.moveFirst(list)) return;
		//System.out.println("* PARENT "+(list.getParent()==null?"null":list.getParent().getType()));
		// enumerate childs
		while(true)
		{
			//System.out.println("*   "+list.getType()+": "+list.getText().replace("\n"," "));
			WebElement element;
			if(list.isEditable())
			{
				element=new WebEdit(parent,list.clone());
			} else
			switch(list.getType())
			{
				case "button":
					element=new WebButton(parent,list.clone());
				break;
				case "select":
				case "table":
				case "ul":
				default:
					element=new WebText(parent,list.clone());
				break;
			}
			if(list.forTEXT())
			{ // it can have text but no recurse
				
			} else
			{ // it can have computed text but have recurse
				//System.out.println("*   recurse "+list.getType());
				make_(page,element,list.getChilds(false));
			}
			parent.getChilds().add(element);
			if(!childs.moveNext(list)) break;
		}
		//System.out.println("* ]");
	}
	private void cleanup(WebElement element)
	{
		// clean childs
		int cnt=0;
		for(WebElement child:element.getChilds())
		{
			cleanup(child);
			if(!child.isDeleted())
				cnt++;
		}
		// remove this if have no child and invisible
		if(cnt==0&&!element.isVisible())
			element.toDelete();
		// remove marked
		Iterator<WebElement> i=element.getChilds().iterator();
		while (i.hasNext())
		{
			WebElement child=i.next();
			if(child.isDeleted())
				i.remove();
		}
		// replace single child with its parent
		//System.out.println("replace: "+element.getType()+" "+element.getText());
		if(element.getParent()!=null&&element.getChilds().size()==1)
		{
			if(element.getParent().getText().equals("link"))
			{ // keep href attribute value
				element.setAttribute("href",element.getParent().getNode().getAttributeProperty("href"));
			}
			int idx=element.getParent().getChilds().indexOf(element);
			if(idx!=-1)
			{ // idx can't be -1 but we check
				// replace element in parent childs to first child of element (loose element at all)
				element.getParent().getChilds().set(idx,element.getChilds().get(0));
			}
		}
	}
}
