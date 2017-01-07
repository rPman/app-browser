
package org.luwrain.app.browser;

import org.luwrain.browser.ElementIterator;

/** The selector for iteration over all elements on the page*/
class AllNodesSelector extends Selector
{
    /** Consider only visible elements of the page*/
    protected boolean visible;

    public AllNodesSelector(boolean visible)
    {
	this.visible=visible;
    }

public boolean isVisible()
    {
	return visible;	
    }

public void setVisible(boolean visible)	
    {
	this.visible=visible;
    }

    // return true if current element is visible
public boolean checkVisible(ElementIterator it)
    {
	return it.isVisible();
    }

    /** return true if current element suits the condition of this selector.*/
    @Override public boolean suits(ElementIterator it)
    {
	if(visible&&!checkVisible(it))
	    return false;
	return true;
    }
}
