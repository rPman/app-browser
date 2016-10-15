package org.luwrain.app.browser.web;

import org.luwrain.core.*;

public interface WebViewBuilder
{
    public enum Type{
	COMPLEX, NORMAL
    };

    WebView build();

    static public WebViewBuilder newBuilder(Type type, WebElement root,int width)
    {
	NullCheck.notNull(type, "type");
	NullCheck.notNull(root, "root");
	if (width < 0)
	    throw new IllegalArgumentException("width may not be negative (witdth = " + width + ")");
	switch(type)
	{
	case COMPLEX:
	    return new WebBuilderComplex(root, width);
	case NORMAL:
	    return new WebBuilderNormal(root, width);
	default:
	    throw new IllegalArgumentException("Unknown builder type: " + type.toString());
	}
    }
}
