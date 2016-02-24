package org.luwrain.app.browser.web;

import java.util.LinkedHashMap;
import java.util.Vector;

import org.luwrain.browser.Browser;
import org.luwrain.browser.ElementIterator;

/* WebText is a parent of all web elements */
public class WebText implements WebElement
{
	// Text - all element contains readable text, each of them can have own structure and have addition attributes, for example font styles, anchor links and so on
	// all other elements was child of Text but have some more functions and attributes
	// List, Table - more complex text structures, can be represented as list of elements with own navigation or 
	// Edit, Button, Checkbox, Groupbox, Select - form elements with own activity
	// * Edit - simple, can be edited, have attribute isMultiline (for textarea html element)
	// * Button - simple, can be pressed
	// * Checkbox - can be pressed and can have 2 or 3 states (on,off,unchecked)
	// * Groupbox - similar checkbox can have two state but have group name, linked with other groupbox element on page (each changes need to rescan all page but it can be optimized)
	// Each element can be represented as simplified text (request to WebPage node), each element can have ordered child, for example, complex text paragraph with font styled words or links
	// Each element have root html node,which can be accessed to read html style, attributes and so on
	// Elements can have short text representation and base type name text
	// Visualisation:
	// Elements can be expanded by default in navigation area or not and represented as short text string
	// tips:
	// * empty invisible (without childs) elements must be removed
	// * some elements can be replaced with own childs (for example simple html not table and list)
 
	// enum Type {Text,Edit,Button,Checkbox,Groupbox,List,Table};
	
	// internal structure of element
	protected Vector<WebElement> childs=new Vector<WebElement>();
	protected WebElement parent=null;
	protected LinkedHashMap<String,String> attributes=new LinkedHashMap<String,String>();
	// link to web page node
	protected ElementIterator rootElement;
	// WebView related attributes
	protected boolean needToBeExpanded=true;
	protected boolean needToBeHidden=false;
	protected boolean needBeginLine=false;
	protected boolean needEndLine=false;
	//
	private boolean toRemove=false;

	@Override public void print(int lvl)
	{
		System.out.print(new String(new char[lvl]).replace("\0", "."));
		System.out.print("v:"+rootElement.isVisible()+" t:"+rootElement.forTEXT()+" "+rootElement.getType()+":"+rootElement.getText().replace('\n',' ')+" css:"+rootElement.getComputedStyleProperty("font-weight"));
		System.out.println();
		for(WebElement e:childs)
			e.print(lvl+1);
	}

	public WebText(WebElement parent,ElementIterator element)
	{
		this.parent=parent;
		this.rootElement=element;
	}
	public void setNeedToBeExpanded(boolean needToBeExpanded)
	{
		this.needToBeExpanded=needToBeExpanded;
	}

	public void setNeedToBeHidden(boolean needToBeHidden)
	{
		this.needToBeHidden=needToBeHidden;
	}

	public void setNeedBeginLine(boolean needBeginLine)
	{
		this.needBeginLine=needBeginLine;
	}

	public void setNeedEndLine(boolean needEndLine)
	{
		this.needEndLine=needEndLine;
	}
	
	@Override public String getType()
	{
		return "Text";
	}
	@Override public String getText()
	{
		return rootElement.getText();
	}
	
	@Override public ElementIterator getNode()
	{
		return rootElement;
	}
	
	@Override public boolean haveChilds()
	{
		return !childs.isEmpty();
	}
	
	@Override public Vector<WebElement> getChilds()
	{
		return childs;
	}

	@Override public boolean isComplex()
	{
		return false;
	}
	
	@Override public boolean needToBeExpanded()
	{
		return needToBeExpanded;
	}
	
	@Override public boolean needToBeHidden()
	{
		return needToBeHidden;
	}
	
	@Override public boolean needBeginLine()
	{
		return needBeginLine;
	}

	@Override public boolean needEndLine()
	{
		return needEndLine;
	}

	@Override public String getSplitter()
	{
		return " ";
	}

	@Override public void toDelete()
	{
		toRemove=true;
	}
	@Override public boolean isDeleted()
	{
		return toRemove;
	}
	@Override public boolean isVisible()
	{
		return rootElement.isVisible();
	}
	@Override public WebElement getParent()
	{
		return parent;
	}
	@Override public void setAttribute(String name,String value)
	{
		attributes.put(name,value);
	}
	@Override public ElementIterator getElement()
	{
		return rootElement;
	}
}