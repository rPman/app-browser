
package org.luwrain.app.browser;

import org.luwrain.core.*;
import org.luwrain.core.events.*;

class DownloadEvent extends ThreadSyncEvent
{
    private String url;

    DownloadEvent(Area area, String url)
    {
	super(area);
	this.url = url;
    }

    String url()
    {
	return url;
    }
}
