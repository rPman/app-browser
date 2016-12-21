/*
   Copyright 2015-2016 Roman Volovodov <gr.rPman@gmail.com>
   Copyright 2012-2016 Michael Pozhidaev <michael.pozhidaev@gmail.com>

   This file is part of the LUWRAIN.

   LUWRAIN is free software; you can redistribute it and/or
   modify it under the terms of the GNU General Public
   License as published by the Free Software Foundation; either
   version 3 of the License, or (at your option) any later version.

   LUWRAIN is distributed in the hope that it will be useful,
   but WITHOUT ANY WARRANTY; without even the implied warranty of
   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
   General Public License for more details.
*/

package org.luwrain.app.browser.selector;

import java.util.Vector;

import org.luwrain.browser.Browser;
import org.luwrain.browser.ElementIterator;
import org.luwrain.browser.NodeInfo;

// select filter for any text container element's, text filtered by text filter as substring

// null string threat as any values
// TODO: make RegEx support in filter
public class SelectorChildrenImpl extends SelectorAllImpl implements SelectorChildren
{
	Integer[] childs=new Integer[0];
	int idx=0;
	
	/** make selector for root children of Browser */
	public SelectorChildrenImpl(Browser browser,boolean visible)
    {
	super(visible);
	// make cache for children
	Vector<Integer> childsList=new Vector<Integer>();
	SelectorAllImpl all=new SelectorAllImpl(visible);
	ElementIterator list=browser.iterator();
	all.moveFirst(list);
	while(true)
	{
		NodeInfo info=browser.getDOMList().get(list.getPos()); 
		if(info.getParent()==null)
			childsList.add(browser.getDOMmap().get(info.getNode()));
		if(!all.moveNext(list)) break;
	}
	this.childs=childsList.toArray(new Integer[childsList.size()]);
    }

	/** make selector for children of cur element */
	public SelectorChildrenImpl(ElementIterator cur,boolean visible)
    {
	super(visible);
	Browser browser=cur.getBrowser();
	// make cache for children
	Vector<Integer> childsList=new Vector<Integer>();
	SelectorAllImpl all=new SelectorAllImpl(visible);
	ElementIterator list=browser.iterator();
	all.moveFirst(list);
	while(true)
	{
		NodeInfo info=browser.getDOMList().get(list.getPos()); 
		if(info.getParent()==cur.getPos())
			childsList.add(browser.getDOMmap().get(info.getNode()));
		if(!all.moveNext(list)) break;
	}
	this.childs=childsList.toArray(new Integer[childsList.size()]);
    }

    @Override public boolean suits(ElementIterator wel_)
    { // ... we never call this method, i think
    	return false;
    }
    public boolean moveFirst(ElementIterator it)
    {
    	if(childs.length==0) return false;
    	idx=0;
    	it.setPos(childs[0]);
    	return true;
    }
    public boolean moveNext(ElementIterator it)
    {
    	if(idx+1>=childs.length) return false;
    	idx++;
    	it.setPos(childs[idx]);
    	return true;
    }
    public boolean movePrev(ElementIterator it)
    {
    	if(idx<=0) return false;
    	idx--;
    	it.setPos(childs[idx]);
    	return true;
    }
    public boolean moveToPos(ElementIterator it, int pos)
    {
    	for(int i:childs)
    	{
    		if(childs[i]==pos)
   			{
    			idx=i;
    			it.setPos(childs[idx]);
    			return true;
   			}
    	}
    	return false;
    }

	@Override public int getChildrenCount()
	{
		return childs.length;
	}
}
