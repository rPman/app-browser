package org.luwrain.app.browser.web;

import java.util.LinkedHashMap;
import java.util.Vector;

import org.luwrain.browser.ElementIterator;

public interface WebElement
{
	enum Type {Text,Edit,Button,Checkbox,Radio,Select,List,ListElement,Table,TableRow,TableCell};
	/** called after all node init after cleanup TODO: conduct research - after or before cleanup */
	void init();
	/** @return String simple name of element's base type */
	Type getType();
	/** @return elementIterator for root node element, must be used only for read or clone */
	ElementIterator getNode();
	boolean haveChildren();
	Vector<WebElement> getChildren();
	WebElement getParent();
	ElementIterator getElement();
	/** @return	true, if element can have navigation structure like Table or List, and text on screen short representation of it */
	boolean needToBeComplex();
	/** @return true, if this element must be used only new begin on WebView line */
	boolean needBeginLine();
	/** @return true, if this element must have not any elements after on WebView line */
	boolean needEndLine();
	/** @return true, if element must be expanded in WebView automatically with children */
	boolean needToBeExpanded();
	/** @return true, if text on WebView must be hidden and used only short text represent */
	boolean needToBeHidden();
	/** @return splitter text, used after this web element in single line in WebView to split next, usual it is a single space or empty string */
	String getSplitter();
	/** @return element text representation */
	String getTextSay();
	String getTextView();
	String getTextShort();
	/** mark element to remove */
	void toDelete();
	boolean isDeleted();
	/** visible root element */
	boolean isVisible();
	/** set element attribute name, for example href  */
	void setAttribute(String name,String value);
	/** set element attributes equal attributes from element */
	void mixAttributes(WebElement element);
	/** return list of attributes */
	LinkedHashMap<String,String> getAttributes();
	
	/** return matrix for complex mode view, preparied for each complex WebElement on init and used for WebView.refillComplex  */
	Vector<Vector<WebElement>> getComplexMatrix();

	void print(int lvl);
}
