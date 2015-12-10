
package org.luwrain.app.browser;

import org.luwrain.browser.Events.WebState;
import org.luwrain.core.*;
import org.luwrain.core.events.*;

class PageChangeStateEvent extends ThreadSyncEvent
{
    private WebState state;

    PageChangeStateEvent(Area area, WebState state)
    {
	super(area);
	this.state = state;
    }

    WebState state()
    {
	return state;
    }
}
