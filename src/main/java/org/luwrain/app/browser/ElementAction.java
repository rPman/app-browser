
package org.luwrain.app.browser;

import org.luwrain.browser.ElementIterator;

public class ElementAction
{
    /** UNKNOWN type is for any other than edit and link elements */
    public enum Type {CLICK, EDIT, SELECT, UNKNOWN, IFRAME};

    public final Type type;
    public final ElementIterator element;

    public ElementAction(Type type, ElementIterator element)
    {
	this.type = type;
	this.element = element;
    }
}
