
package org.luwrain.app.browser;

import org.luwrain.core.*;
import org.luwrain.core.events.*;

class PromptEvent extends ThreadSyncEvent
{
    private String message;
    private String value;
    private String answer;

    PromptEvent(Area area,
		String message, String value)
    {
	super(area);
	this.message = message;
	this.value = value;
    }

    String message()
    {
	return message;
    }

    String value()
    {
	return value;
    }

    void setAnswer(String answer)
    {
	this.answer = answer;
    }

    String answer()
    {
	return answer;
    }
}
