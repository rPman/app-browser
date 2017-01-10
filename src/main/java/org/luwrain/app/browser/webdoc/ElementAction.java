package org.luwrain.app.browser.webdoc;

import org.luwrain.browser.ElementIterator;

public class ElementAction
{
	/** UNKNOWN type is for any other than edit and link elements */
	public enum Type {CLICK, EDIT, SELECT, UNKNOWN, IFRAME};
	public Type type;
	public ElementIterator element;
	public ElementAction(Type type,ElementIterator element)
	{
		this.type=type;
		this.element=element;
	}
}
