
package org.luwrain.app.browser;

import org.luwrain.core.*;
import org.luwrain.core.events.*;

class CheckChangesEvent extends ThreadSyncEvent
{
    CheckChangesEvent(Area area)
    {
    	super(area);
    }
}
