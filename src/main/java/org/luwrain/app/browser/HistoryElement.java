
package org.luwrain.app.browser;

import org.luwrain.app.browser.web.*;
import org.luwrain.core.*;

class HistoryElement
{
    final WebElement element;
    final boolean mode;

    HistoryElement(WebElement element,boolean mode)
    {
	NullCheck.notNull(element, "element");
	this.element = element;
	this.mode=mode;
    }
}
