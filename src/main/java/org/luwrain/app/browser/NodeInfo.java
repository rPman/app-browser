
package org.luwrain.app.browser;

import java.util.*;

import org.luwrain.core.*;
import org.luwrain.browser.*;

class NodeInfo
    {
	final NodeInfo parent;
ElementIterator element;
	Vector<NodeInfo> children = new Vector<NodeInfo>();
	/** list of nodes, mixed with this node for cleanup */
	final Vector<ElementIterator> mixed = new Vector<ElementIterator>();
	boolean toDelete = false;

	/** Creates the root node*/
	NodeInfo()
	{
	    this.parent = null;
	    this.element = null;
	}

	NodeInfo(NodeInfo parent, ElementIterator element, HashMap<Integer,NodeInfo> index)
	{
	    NullCheck.notNull(parent, "parent");
	    NullCheck.notNull(element, "element");
	    this.parent = parent;
	    this.element = element.clone();
	    parent.children.add(this);
	    index.put(element.getPos(),this);
	}

	/** return element and reversed mixed in list */
	Vector<ElementIterator> getMixedinfo()
	{
	    final Vector<ElementIterator> res = new Vector<ElementIterator>();
	    res.add(element);
	    // we already add mixed in reversed mode
	    if(!mixed.isEmpty())
		res.addAll(mixed);
	    return res;
	}

	void debug(int lvl,boolean printChildren)
	{
	    System.out.print(new String(new char[lvl]).replace("\0", "."));
	    if(element==null)
	    {
		System.out.println("ROOT");
	    } else
	    {
		System.out.print(element.getPos()+": ");
		//if(toDelete) System.out.print("DELETE ");
		if(!mixed.isEmpty())
		{
		    System.out.print("[");
		    for(ElementIterator e:mixed)
		    {
			System.out.print((e==null?"ROOT":e.getHtmlTagName())+" ");
		    }
		    System.out.print("] ");
		}
		System.out.print(element.getHtmlTagName()+" ");
		String str=element.getText().replace('\n',' ');
		System.out.print(element.getType()+": '"+str.substring(0,Math.min(160,str.length()))+"'");
		System.out.println();
	    }
	    if(printChildren)
		for(NodeInfo e:children)
		    e.debug(lvl+1,true);
	};
    }
