package org.luwrain.app.browser.web;

public interface BigSearcher
{
	/** recursive search of elements to detect important BIG elements to remove or hide in UI
	 * @param root is a root element and start of the search */
	void search(WebElement root, WeightSortedSet result);
}
