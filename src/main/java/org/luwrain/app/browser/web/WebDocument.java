
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
	/**  minimal weight limit ratio for element to root document element, to allow select this element as BIG */
	final double BIG_WEIGHT_LIMIT=0.07;

	/** minimal rate limit for total weight for some element children with fair distribution of weight */
	final double BIG_WEIGHT_FAIR_TOTAL = 0.4;

	/** minimal difference between near children (sorted by weight) with fair fair distribution of weight */
	final double BIG_WEIGHT_FAIR_CHILD = 0.4;

	/** number of this children in element with fair distribution of weight */
	final int BIG_WEIGHT_FAIR_COUNT = 3;

	/** max number of BIG element */
	final int BIG_MAX_COUNT = 5;
	
    // make WebDocument structure for web page, more simple than html document, i.e.  only visible elements and without element with single child
    // only visible elements
    private WebElement root = null;
    
    private int currentBigCount = 0;

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
	elementInit(root);
	searchBigElements();
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
	private void elementInit(WebElement element)
	{
		element.init();
		// calculate weight
		if(!element.hasChildren())
		{
			element.incWeight(element.calcWeight());
		} else
		{
			for(WebElement child:element.getChildren())
			{
				// and init elements
				elementInit(child);
				element.incWeight(child.getWeight());
			}
		}
	}

	/* recursive search of elements to detect important BIG elements to remove or hide in UI */
	private void searchBigElements()
	{
		currentBigCount = 0;
		searchBigElementsImpl(1, this.getRoot());
	}

	private void searchBigElementsImpl(int lvl, WebElement element)
	{
	    NullCheck.notNull(element, "element");
	    Log.debug("proba", "processing " + element.getDescr());
		System.out.print("search: ");element.print(lvl,false);
		// check BIG element count
		if(currentBigCount >= BIG_MAX_COUNT)
		{
		    Log.debug("search", "stopping, big element count exceeded: " + currentBigCount);
			return;
		}
		// check element weight 
		final double weightRateRoot = (double)element.getWeight() / getRoot().getWeight();
		if(weightRateRoot < BIG_WEIGHT_FAIR_TOTAL)
		{
		    Log.debug("search", "too small element comparing to weight of the root (" + weightRateRoot + ")");
			return;
		}
		// work with item weight
		final Vector<WebElement> sortedchildren=new Vector<WebElement>();
		// clone children list
		for(WebElement child:element.getChildren())
			sortedchildren.add(child);
		// sort by weight reversed
		//				sortedchildren.sort(new Comparator<WebElement>() {
		//			@Override public int compare(WebElement o1,WebElement o2)
		sortedchildren.sort((o1, o2)->{
				if(o1 == o2 || o1.getWeight() == o2.getWeight()) return 0;
				// reversed order
				return o1.getWeight() < o2.getWeight()?1:-1;
		    });
		// debug info
		//		System.out.print("sorted: ");
		//		for(WebElement e:sortedchildren)
		//			System.out.print(e.getWeight()+" ");
		//		System.out.println();

		// get first BIG_WEIGHT_FAIR_COUNT children and calculate total weight of them
		long totalWeight = 0;
		long fairCount = 0;
		WebElement prevChild = null;
		for(WebElement child: sortedchildren)
		{
			totalWeight += child.getWeight();
			if(prevChild != null)
			{
				if((double)Long.max(prevChild.getWeight(),child.getWeight())/Long.min(prevChild.getWeight(),child.getWeight())<BIG_WEIGHT_FAIR_CHILD)
				{
					// child not in fair distribution, break
					System.out.println("break: child not in fair");
					break;
				}
			}
			fairCount++;
			prevChild=child;
		}
		if(fairCount>=BIG_WEIGHT_FAIR_COUNT&&(double)totalWeight/element.getWeight()>=BIG_WEIGHT_FAIR_TOTAL)
		{
			// this element is a BIG
			System.out.println("BIG");
			element.setBIG(true);
			// disable big status for parent (small fix of algorithm)
			WebElement p = element.getParent();
			while(p!=null)
			{
				if(p.isBIG())
				{
					p.setBIG(false);
					currentBigCount--;
				}
				p=p.getParent();
			}
			// count this BIG
			if(currentBigCount++ >= BIG_MAX_COUNT)
return;
		} else
		{
			// compare element weight with max child weight with 
			if((double)sortedchildren.get(0).getWeight()/getRoot().getWeight()<=BIG_WEIGHT_FAIR_TOTAL)
			{
				System.out.println("BIG");
				element.setBIG(true);
				// disable big status for parent (small fix of algorithm)
				WebElement p=element.getParent();
				while(p!=null)
				{
					if(p.isBIG())
					{
						p.setBIG(false);
						currentBigCount--;
					}
					p=p.getParent();
				}
				// count this BIG
				if(currentBigCount++ >= BIG_MAX_COUNT) 
return;
			}
		}
		// recurse for all child
		for(WebElement child: sortedchildren)
			searchBigElementsImpl(lvl + 1, child);
	}

}
