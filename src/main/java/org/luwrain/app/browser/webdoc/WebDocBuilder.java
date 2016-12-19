package org.luwrain.app.browser.webdoc;

import java.util.Iterator;
import java.util.LinkedList;

import org.luwrain.app.browser.web.ByFairDistrib;
import org.luwrain.app.browser.web.WebElement;
import org.luwrain.app.browser.web.WebText;
import org.luwrain.app.browser.web.Weight;
import org.luwrain.app.browser.web.WeightSortedSet;
import org.luwrain.browser.Browser;
import org.luwrain.browser.ElementIterator;
import org.luwrain.browser.Selector;
import org.luwrain.browser.SelectorChildren;
import org.luwrain.core.Log;
import org.luwrain.core.NullCheck;
import org.luwrain.doctree.Document;
import org.luwrain.doctree.Node;
import org.luwrain.doctree.NodeFactory;

/** make doctree Document from current state of browser  */
public class WebDocBuilder
{
    private Node root = null;

    public Node getRoot()
    {
	return root;
    }

    public Document make(Browser page)
	{
		NullCheck.notNull(page, "page");
		// get all children without parent, there should be only one like this
		final SelectorChildren selector = page.rootChildren(false);
		final ElementIterator it = page.iterator();
		selector.moveFirst(it);
	    root = NodeFactory.newNode(Node.Type.ROOT);
	    final LinkedList<Node> subnodes = new LinkedList<Node>();


	    //Enumerating all children
		do {
		    make_(page, root, it.getChildren(false));
		} while(selector.moveNext(it));
		cleanup(root);
		// calculate weight and mark BIG elements in sorted set
		elementInit(root,new Weight.ByTextLen());
		WeightSortedSet result=new WeightSortedSet();
		
		root.setSubnodes(subnodes.toArray(new Node[subnodes.size()]));
		return new Document(root);
	}

	private void make_(Browser page, Node parent, Selector selector)
    {
		final ElementIterator nodeIt = page.iterator();
		if(!selector.moveFirst(nodeIt))
		    return;
		//Enumerating all children
		do {
		    final Node element;
		    Log.debug("browser", "new element:" + nodeIt.getType() + ":" + nodeIt.getText());
		    
		} while(selector.moveNext(nodeIt));
    }
	
    /** clean Node element from single children and empty non text elements */
	private void cleanup(Node element)
    {
	// clean childs
		int cnt = 0;
		for(Node child:element.getSubnodes())
		{
			child.
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
			switch(element.getNode().getType())
			{
				// ignore important tags for this optimization
				case "li":
				case "tr":
				case "td":
				case "th":
					break;
				default:
					ElementIterator e=element.getParent().getNode();
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

}
