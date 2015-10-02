
package org.luwrain.app.browser;

import javafx.concurrent.Worker.State;
import org.luwrain.core.*;
import org.luwrain.core.events.*;

class PageChangeStateEvent extends ThreadSyncEvent
{
    private State state;

    PageChangeStateEvent(Area area, State state)
    {
	super(area);
	this.state = state;
    }

    State state()
    {
	return state;
    }
}
