
package org.luwrain.app.browser;

import java.util.Timer;
import java.util.TimerTask;

class AutoPageElementScanner extends TimerTask
{
    static private final int PAGE_SCANNER_INTERVAL=1000;
    static private final int PAGE_SCANNER_INTERVAL_FAST=100;

    private BrowserArea browser;
    Timer pageTimer = null;

    AutoPageElementScanner(BrowserArea browser)
    {
	this.browser = browser;
    }

    @Override public void run()
    {
	browser.onTimerElementScan();
    }

    void schedule()
    {
	pageTimer=new Timer();
	pageTimer.scheduleAtFixedRate(this,PAGE_SCANNER_INTERVAL,PAGE_SCANNER_INTERVAL);
    }

    void fast()
    {
	//pageTimer.cancel();
	pageTimer.scheduleAtFixedRate(this,PAGE_SCANNER_INTERVAL_FAST,PAGE_SCANNER_INTERVAL);
    }
}
