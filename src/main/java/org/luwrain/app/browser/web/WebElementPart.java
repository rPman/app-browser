
package org.luwrain.app.browser.web;

import org.luwrain.core.*;

/** structure to store WebElement link and position in line or text part inside */
public class WebElementPart
{
    public final WebElement element;

    /** cached part of element text (full element or part, if element contains on multiple lines) */
    public final String text;

    /** text length */
    public final int textLength; 

    /** text part position in WebElement text */
    public final int from,to;

    /** text position on line (for example 0 if element begin in line) */
    public final int pos;

    WebElementPart(WebElement el, String text, int textLen,
		   int pos, int from, int to)
    {
	NullCheck.notNull(el, "el");
	NullCheck.notNull(text, "text");
	this.element = el;
	this.text = text;
	this.textLength = textLen;
	this.pos = pos;
	this.from = from;
	this.to = to;
    }
}
