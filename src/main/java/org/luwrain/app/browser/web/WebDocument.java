
package org.luwrain.app.browser.web;

import java.util.Comparator;
import java.util.Iterator;
import java.util.Vector;

import org.luwrain.browser.Browser;
import org.luwrain.browser.ElementIterator;
import org.luwrain.browser.Selector;
import org.luwrain.browser.SelectorChildren;

import org.luwrain.core.*;

public class WebDocument
{
    // make WebDocument structure for web page, more simple than html document, i.e.  only visible elements and without element with single child
    // only visible elements
    private WebElement root = null;
    
    public WebElement getRoot()
    {
	return root;
    }

    /**
     * replace WebElement structure for given web page
     * @param page - web page
     */
    public void make(Browser page)
    {
	NullCheck.notNull(page, "page");
	// get all childs without parent (it must have only one)
	final SelectorChildren selector = page.rootChildren(false);
	final ElementIterator it = page.iterator();
	selector.moveFirst(it);
	root = new WebText(null, it.clone());
	//Enumerating all children
	do {
	    make_(page, root, it.getChildren(false));
	} while(selector.moveNext(it));
	cleanup(root);
	// calculate weight and mark BIG elements in sorted set
	elementInit(root,new Weight.ByTextLen());
	WeightSortedSet result=new WeightSortedSet();
	new ByFairDistrib().search(this.getRoot(),result);
	// debug
	//root.print(1,true);
	System.out.println("BIG result:");
	for(WebElement e:result)
		e.print(0,false);
	System.out.println("BIG result END");
	BigSearcherTest.main(this);
    }

	private void make_(Browser page,WebElement parent,Selector selector)
    {
	NullCheck.notNull(page, "page");
	NullCheck.notNull(parent, "parent");
	NullCheck.notNull(selector, "selector");
	final ElementIterator nodeIt = page.iterator();
	if(!selector.moveFirst(nodeIt))
	    return;
	//Enumerating all children
	do {
	    final WebElement element;
	    if(nodeIt.isEditable())
	    {
		switch(nodeIt.getType())
		{
		case "input checkbox":
		    element = new WebCheckbox(parent,nodeIt.clone());
		    break;
		case "input radio":
		    element = new WebRadio(parent,nodeIt.clone());
		    break;
		case "input button":
		    element = new WebButton(parent,nodeIt.clone());
		    break;
		case "select":
		    element = new WebSelect(parent,nodeIt.clone());
		    break;
		default:
		    element=new WebEdit(parent,nodeIt.clone());
		}
	    } else
		switch(nodeIt.getType())
		{
		case "link":
		    element=new WebText(parent,nodeIt.clone());
		    element.setAttribute("href",nodeIt.getLink());
		    break;
		case "button":
		    element = new WebButton(parent,nodeIt.clone());
		    break;
		case "list":
		    element = new WebList(parent,nodeIt.clone());
		    break;
		case "li":
		    element = new WebListItem(parent,nodeIt.clone());
		    break;
		case "table":
		    element = new WebTable(parent,nodeIt.clone());
		    break;
		case "tr":
		    element = new WebTableRow(parent,nodeIt.clone());
		    break;
		case "td":
		case "th":
		    element = new WebTableCell(parent,nodeIt.clone());
		break;
		default:
		    element=new WebText(parent,nodeIt.clone());
		    break;
		}
	    if(!nodeIt.forTEXT())
		make_(page,element,nodeIt.getChildren(false));
	    parent.getChildren().add(element);
	} while(selector.moveNext(nodeIt));
    }

    private void cleanup(WebElement element)
    {
	// clean childs
		int cnt = 0;
		for(WebElement child:element.getChildren())
		{
		    cleanup(child);
		    if(!child.isDeleted())
			cnt++;
		}
		// remove this if have no child and invisible
		if(cnt==0&&!element.isVisible())
		    element.toDelete();
		// remove marked
		Iterator<WebElement> i=element.getChildren().iterator();
		while (i.hasNext())
		{
			WebElement child=i.next();
			if(child.isDeleted())
				i.remove();
		}
		// replace single child with its parent
		//System.out.println("replace: "+element.getType()+" "+element.getText());
		if(element.getParent()!=null&&element.getChildren().size()==1)
		{
			switch(element.getElement().getType())
			{
				// ignore important tags for this optimization
				case "li":
				case "tr":
				case "td":
				case "th":
					break;
				default:
					ElementIterator e=element.getParent().getElement();
					//System.out.println("REPLACE: "+e.getType()+" "+e.getText());
					// keep attributes from removed parent in element
					element.mixAttributes(element.getParent());
					// replace by idx
					int idx=element.getParent().getChildren().indexOf(element);
					if(idx!=-1)
					{ // idx can't be -1 but we check
						// replace element in parent childs to first child of element (loose element at all)
						element.getParent().getChildren().set(idx,element.getChildren().get(0));
					}
				break;
			}
		}
	}
	public void elementInit(WebElement element, Weight.Calculator calculator)
	{
		element.init();
		// calculate weight
		if(!element.hasChildren())
		{
			element.incWeight(calculator.calcWeightFor(element));
		} else
		{
			for(WebElement child:element.getChildren())
			{
				// and init elements
				elementInit(child, calculator);
				element.incWeight(child.getWeight());
			}
		}
	}

}
