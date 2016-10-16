
package org.luwrain.app.browser.web;

/** structure to store WebElement link and position in line or text part inside */
public class WebElementPart
{
    public WebElement element;

    /** cached part of element text (full element or part, if element contains on multiple lines) */
    public String text;

    /** text length */
    public int textLength; 

    /** text part position in WebElement text */
    public int from,to;

    /** text position on line (for example 0 if element begin in line) */
    public int pos;
}
