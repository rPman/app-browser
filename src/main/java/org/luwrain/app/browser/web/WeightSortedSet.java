package org.luwrain.app.browser.web;

import java.util.TreeSet;

/** SortedSet of WebElements ordered by weight with reversed order */
public class WeightSortedSet extends TreeSet<WebElement>
{
	private static final long serialVersionUID=1L;
	public WeightSortedSet()
	{
		super((o1, o2)->
		{
			if(o1 == o2 || o1.getWeight() == o2.getWeight()) return 0;
			// reversed order
			return o1.getWeight() < o2.getWeight()?1:-1;
	    });
	}
	
}
