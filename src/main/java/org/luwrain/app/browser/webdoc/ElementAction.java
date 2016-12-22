package org.luwrain.app.browser.webdoc;

import org.luwrain.browser.ElementIterator;

public class ElementAction
{
	public enum Type {CLICK, EDIT, SELECT};
	public Type type;
	public ElementIterator element;
	public ElementAction(Type type,ElementIterator element)
	{
		this.type=type;
		this.element=element;
	}
}
