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

import org.luwrain.browser.ElementIterator;

/** General navigation operations based on the behaviour of {@code: suits()} method*/
abstract class SelectorImpl implements Selector
{
    /** Moves the iterator to the first element, approved by the {@code suits()} 
     * method. If there is no such element, the method restored 
     * the original state of the iterator.
     *
     * @param it The iterator to move
     * @return True if the iterator gets necessary position, false otherwise
     */
    @Override public boolean moveFirst(ElementIterator it)
    {
	final int origState = it.getPos();
	final int count = it.getBrowser().numElements();
	it.setPos(0);
	while(it.getPos() < count && !suits(it)) 
		it.setPos(it.getPos()+1);
	if(it.getPos() >= count)
	{
		it.setPos(origState);
	    return false;
	}
	return true;
    }

    /** Moves the iterator to the next element, approved by the {@code suits()} 
     * method. If there is no such element, the method restored 
     * the original state of the iterator.
     *
     * @param it The iterator to move
     * @return True if the iterator gets necessary position, false otherwise
     */
    @Override public boolean moveNext(ElementIterator it)
    {
	final int origState = it.getPos();
	final int count = it.getBrowser().numElements();
	it.setPos(it.getPos()+1);
	while(it.getPos() < count && !suits(it)) 
		it.setPos(it.getPos()+1);
	if(it.getPos() >= count)
	{
		it.setPos(origState);
	    return false;
	}
	return true;
    }

    /** Moves the iterator to the previous element, approved by the {@code suits()} 
     * method. If there is no such element, the method restored 
     * the original state of the iterator.
     *
     * @param it The iterator to move
     * @return True if the iterator gets necessary position, false otherwise
     */
    @Override public boolean movePrev(ElementIterator it)
    {
	final int origState = it.getPos();
	it.setPos(it.getPos()-1);
	while(it.getPos() >= 0 && !suits(it)) 
		it.setPos(it.getPos()-1);
	if(it.getPos()<0)
	{
		it.setPos(origState);
	    return false;
	}
	return true;
    }

    @Override public boolean moveToPos(ElementIterator it, int pos)
    {
    	if(it.getPos() == pos)
    		return true;
    	else if(it.getPos() < pos)
	    {
		while(moveNext(it)) 
		    if(pos == it.getPos()) 
			return true;
		return false;
	    } else
	    {
		while(movePrev(it)) 
		    if(pos == it.getPos()) 
			return true;
		return false;
	    }
    }
}
