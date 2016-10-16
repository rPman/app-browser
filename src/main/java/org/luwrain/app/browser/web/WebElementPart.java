
package org.luwrain.app.browser.web;

import org.luwrain.core.*;

/**
 * A fragment of a text representation of WebElement, purposed to be able
 * to split in case the line doesn't have a necessary length.
 *
 * @see WebElement WebView WebViewBuilder
 */
public class WebElementPart
{
    /** The source element this part is a fragment of*/
    public final WebElement element;

    /**A copy of the text this part represents (the exact substring of a source text)*/
    final String text;

    /**The length of the text*/
    final int textLength; 

    /**The starting point of this part in source web element (meaning, its text representation)*/
    private final int from;

    /**The ending point of this part in source web element (meaning, its text representation)*/
    private final int to;

    /**The offset from the line beginning this part starts at*/
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

    @Override public String toString()
    {
	return text;
    }
}
