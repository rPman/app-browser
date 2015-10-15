
package org.luwrain.app.browser;

import org.luwrain.core.*;
import org.luwrain.core.events.*;

class ConfirmEvent extends ThreadSyncEvent
{
    private String message;
    private boolean answer;

    ConfirmEvent(Area area, String message)
    {
	super(area);
	this.message = message;
    }

    String message()
    {
	return message;
    }

    void setAnswer(boolean answer)
    {
	this.answer = answer;
    }

    boolean answer()
    {
	return answer;
    }
}
