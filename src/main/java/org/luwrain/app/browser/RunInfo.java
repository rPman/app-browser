
package org.luwrain.app.browser;

import org.luwrain.core.*;
import org.luwrain.browser.*;
import org.luwrain.doctree.*;

class RunInfo
{
final Run run;
final Node node;
final NodeInfo info;

    RunInfo(Run run, NodeInfo info)
    {
	this.run = run;
	this.info = info;
	this.node = null;
    }

    RunInfo(Node node, NodeInfo info)
    {
	this.node = node;
	this.info = info;
	this.run = null;
    }

    boolean isRun()
    {
	return run != null;
    }

    boolean isNode()
    {
	return node != null;
    }
}
