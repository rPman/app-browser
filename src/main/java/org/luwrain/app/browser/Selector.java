
package org.luwrain.app.browser;

import org.luwrain.core.*;
import org.luwrain.browser.*;

abstract public class Selector
{
abstract public boolean suits(ElementIterator list);

    /** Moves the iterator to the first element, approved by the {@code suits()} 
     * method. If there is no such element, the method restored 
     * the original state of the iterator.
     *
     * @param list The iterator to move
     * @return True if the iterator gets necessary position, false otherwise
     */
    //    boolean moveFirst(ElementIterator list);

    /** Moves the iterator to the next element, approved by the {@code suits()} 
     * method. If there is no such element, the method restored 
     * the original state of the iterator.
     *
     * @param list The iterator to move
     * @return True if the iterator gets necessary position, false otherwise
     */
    //    boolean moveNext(ElementIterator list);

    /** Moves the iterator to the previous element, approved by the {@code suits()} 
     * method. If there is no such element, the method restored 
     * the original state of the iterator.
     *
     * @param list The iterator to move
     * @return True if the iterator gets necessary position, false otherwise
     */
    //    boolean movePrev(ElementIterator list);

    //    boolean moveToPos(ElementIterator list, int pos);



    /** Moves the iterator to the first element, approved by the {@code suits()} 
     * method. If there is no such element, the method restored 
     * the original state of the iterator.
     *
     * @param it The iterator to move
     * @return True if the iterator gets necessary position, false otherwise
     */
public boolean moveFirst(ElementIterator it)
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
public boolean moveNext(ElementIterator it)
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
public boolean movePrev(ElementIterator it)
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

public boolean moveToPos(ElementIterator it, int pos)
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
