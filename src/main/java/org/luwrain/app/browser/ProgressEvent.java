
package org.luwrain.app.browser;

import org.luwrain.core.*;
import org.luwrain.core.events.*;

class ProgressEvent extends ThreadSyncEvent
{
    private Number value;

    ProgressEvent(Area area, Number value)
    {
	super(area);
	this.value = value;
    }

    Number value()
    {
	return value;
    }
}
