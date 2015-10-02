
package org.luwrain.app.browser;

import org.luwrain.core.*;
import org.luwrain.core.events.*;

class ErrorEvent extends ThreadSyncEvent
{
    private String message;

    ErrorEvent(Area area, String message)
    {
	super(area);
	this.message = message;
    }

    String message()
    {
	return message;
    }
}
