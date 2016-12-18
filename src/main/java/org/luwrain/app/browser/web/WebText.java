
package org.luwrain.app.browser.web;

import java.awt.Rectangle;
import java.util.*;

import org.luwrain.browser.Browser;
import org.luwrain.browser.ElementIterator;

import org.luwrain.core.*;

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

    // internal structure of element
    protected final Vector<WebElement> children = new Vector<WebElement>();
    protected final WebElement parent;
    protected LinkedHashMap<String,String> attributes=new LinkedHashMap<String,String>();

    //The iterator pointing at the corresponding node in DOM structure
    protected final ElementIterator nodeIt;

    // WebView related attributes
    protected boolean needToBeExpanded=true;
    protected boolean needToBeHidden=false;
    protected boolean needBeginLine =false;
    protected boolean needEndLine=false;
    protected boolean needToBeComplex=false;

    private boolean toRemove=false;
    private long weight=0;

    // some other options

    WebText(WebElement parent,ElementIterator nodeIt)
    {
	//Breaks page loading, subject to debug: NullCheck.notNull(parent, "parent");
	NullCheck.notNull(nodeIt, "nodeIt");
	this.parent = parent;
	this.nodeIt = nodeIt;
    }

    @Override public Type getType()
    {
	return Type.Text;
    }

    @Override public void init()
    {
    }

    @Override public Vector<Vector<WebElement>> getComplexMatrix()
    {
	return null;
    }

    void setNeedToBeExpanded(boolean needToBeExpanded)
    {
	this.needToBeExpanded=needToBeExpanded;
    }

    void setNeedToBeHidden(boolean needToBeHidden)
    {
	this.needToBeHidden=needToBeHidden;
    }

    void setNeedBeginLine(boolean needBeginLine)
    {
	this.needBeginLine=needBeginLine;
    }

    void setNeedEndLine(boolean needEndLine)
    {
	this.needEndLine=needEndLine;
    }

    @Override public String getTextSay()
    {
	// FIXME: make link sayble in Navigation area but not in WebElement
	String res=nodeIt.getText();
	if(attributes.containsKey("href"))
	    res="Ссылка "+nodeIt.getText();
	return res;
    }

    @Override public String getTextView()
    {
	String res=nodeIt.getText();
	if(attributes.containsKey("href"))
	    res="[Link: "+nodeIt.getText()+"]";
	return res;
    }

    @Override public String getTextShort()
    {
	return nodeIt.getType();
    }

    @Override public ElementIterator getNode()
    {
	return nodeIt;
    }

    @Override public boolean hasChildren()
    {
	return !children.isEmpty();
    }

    @Override public Vector<WebElement> getChildren()
    {
	return children;
    }

    @Override public boolean isComplex()
    {
	return needToBeComplex;
    }

    @Override public boolean needToBeExpanded()
    {
	return needToBeExpanded;
    }

    @Override public boolean needToBeHidden()
    {
	return needToBeHidden;
    }

    @Override public boolean alwaysFromNewLine()
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
	return nodeIt.isVisible();
    }

    @Override public WebElement getParent()
    {
	return parent;
    }

    @Override public void setAttribute(String name,String value)
    {
	attributes.put(name,value);
    }

    @Override public void mixAttributes(WebElement element_)
    {
	// FIXME: work attributes more abstract
	final WebText element=(WebText)element_;
	element.attributes.putAll(element.attributes);
    }

    @Override public LinkedHashMap<String,String> getAttributes()
    {
	return attributes;
    }

    @Override public void print(int lvl,boolean printChildren)
    {
	
	System.out.print(new String(new char[lvl]).replace("\0", "."));
	System.out.print("v:"+nodeIt.isVisible()+" t:"+nodeIt.forTEXT()+
			 " w:"+this.getWeight()+" "+
			 nodeIt.getType()+":"+nodeIt.getText().replace('\n',' ')+
			 //" css:"+nodeIt.getComputedStyleProperty("font-weight")+
			 (attributes.containsKey("href")?", href:"+attributes.get("href"):"")+
			 " rect:"+nodeIt.getRect().x+"x"+nodeIt.getRect().y+"-"+(nodeIt.getRect().width+nodeIt.getRect().x)+"x"+(nodeIt.getRect().height+nodeIt.getRect().y)+
			 "");
	System.out.println();
	if(printChildren)
		for(WebElement e:children)
			e.print(lvl+1,true);
	
    }

	@Override public long getWeight()
	{
		return weight;
	}

	@Override public void incWeight(long value)
	{
		this.weight += value;
	}

    @Override public String getDescr()
    {
	NullCheck.notNull(nodeIt, "nodeIt");
	return nodeIt.getType() + " " + nodeIt.getText();
    }
}
