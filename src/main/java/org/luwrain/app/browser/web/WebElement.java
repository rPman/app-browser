
package org.luwrain.app.browser.web;

import java.util.*;

import org.luwrain.browser.ElementIterator;

public interface WebElement
{
    enum Type {Text, Edit, Button, Checkbox, Radio, Select, List, ListElement, Table, TableRow, TableCell};

	/** called after all node init after cleanup TODO: conduct research - after or before cleanup */
	void init();

	Type getType();
	ElementIterator getNode();
	boolean hasChildren();
	Vector<WebElement> getChildren();
	WebElement getParent();
	/** @return	true, if element can have navigation structure like Table or List, and text on screen short representation of it */
	boolean isComplex();
	/** @return true, if this element must be used only new begin on WebView line */
	boolean alwaysFromNewLine();

	/** @return true, if this element must have not any elements after on WebView line */
	boolean needEndLine();

	/** @return true, if element must be expanded in WebView automatically with children */
	boolean needToBeExpanded();

	/** @return true, if text on WebView must be hidden and used only short text represent */
	boolean needToBeHidden();

	/** @return splitter text, used after this web element in single line in WebView to split next, usual it is a single space or empty string */
	String getSplitter();

	String getTextSay();
	String getTextView();

	String getTextShort();

	/** mark element to remove */
	void toDelete();
	boolean isDeleted();

	/** visible root element */
	boolean isVisible();

	long getWeight();
	void incWeight(long weight);

	/** set element attribute name, for example href  */
	void setAttribute(String name,String value);

	/** set element attributes equal attributes from element */
	void mixAttributes(WebElement element);

	/** return list of attributes */
	LinkedHashMap<String,String> getAttributes();

	/** return matrix for complex mode view, preparied for each complex WebElement on init and used for WebView.refillComplex  */
	Vector<Vector<WebElement>> getComplexMatrix();

	/** print this element debug info
	 * @param lvl current level in recursive call, for root must be 0
	 * @param printChildren if true, children will printed recursive */
	void print(int lvl,boolean printChildren);

    /**
     * Returns some element description, mostly for debug purposes.
     *
     * @return The text description of the element
     */
    String getDescr();
}
