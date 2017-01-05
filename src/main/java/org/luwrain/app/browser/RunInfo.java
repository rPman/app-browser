
package org.luwrain.app.browser;

import org.luwrain.core.*;
import org.luwrain.browser.*;
import org.luwrain.doctree.*;

class RunInfo
	{
public Run run=null;
		public Node node=null;
		public NodeInfo info;

RunInfo(Run run, NodeInfo info)
		{
			this.run=run;
			this.info=info;
		}

RunInfo(Node node, NodeInfo info)
		{
			this.node=node;
			this.info=info;
		}

		boolean isRun()
		{
			return run!=null;
		}

		boolean isNode()
		{
			return node!=null;
		}
	}

