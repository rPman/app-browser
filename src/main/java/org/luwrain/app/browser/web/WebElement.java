package org.luwrain.app.browser.web;

import java.util.Vector;

import org.luwrain.browser.ElementIterator;

public interface WebElement
{
	/** @return String simple name of element's base type */
	String getType();
	/** @return elementIterator for root node element, must be used only for read or clone */
	ElementIterator getNode();
	boolean haveChilds();
	Vector<WebElement> getChilds();
	WebElement getParent();
	ElementIterator getElement();
	/** @return	true, if element can have navigation structure like Table or List, but getText can return simplified node text */
	boolean isComplex();
	/** @return true, if this element must be used only new begin on WebView line */
	boolean needBeginLine();
	/** @return true, if this element must have not any elements after on WebView line */
	boolean needEndLine();
	/** @return true, if element must be expanded in WebView automatically with childs */
	boolean needToBeExpanded();
	/** @return true, if text on WebView must be hidden and used only short text represent */
	boolean needToBeHidden();
	/** @return splitter text, used after this web element in single line in WebView to split next, usual it is a single space or empty string */
	String getSplitter();
	/** @return element text representation */
	String getText();
	/** mark element to remove */
	void toDelete();
	boolean isDeleted();
	/** visible root element */
	boolean isVisible();
	/** set element attribute name, for example href  */
	void setAttribute(String name,String value);

	void print(int lvl);
}
