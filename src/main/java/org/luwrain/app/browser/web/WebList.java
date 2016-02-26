package org.luwrain.app.browser.web;

import java.util.Vector;

import org.luwrain.browser.ElementIterator;

public class WebList extends WebEdit
{
	private int liCount;
	private boolean isMultilevel;
	
	Vector<Vector<WebElement>> matrix;
	
	public WebList(WebElement parent,ElementIterator element)
	{
		super(parent,element);
		matrix=new Vector<Vector<WebElement>>();
	}
	@Override public void init()
	{
		// try to compute size
		String ctext=rootElement.getComputedText();
		//if(ctext.length()>LIST_COMPLEX_HIDE_LENGTH_LIMIT)
			needToBeComplex=false;
		// calculate numbers for elements and detect multilevel
		liCount=0;
		isMultilevel=false;
		liCalc(this,"");
	}
	@Override public Vector<Vector<WebElement>> getComplexMatrix()
	{
		return matrix;
	}
	/** method to calculate list elements recursive */
	private void liCalc(WebElement element,String parentName)
	{
		String liDot=(parentName.isEmpty()?"":".");
		int num=0;
		for(WebElement child:element.getChilds())
		{
			// multilevel, if one of li contains other list
			if(child instanceof WebList)
				isMultilevel=true;
			// calc li elements
			if(child instanceof WebListElement)
			{
				liCount++;
				num++;
				WebListElement li=((WebListElement)child);
				if(li.name==null)
					li.name=parentName+liDot+num;
				matrix.add(new Vector<WebElement>(){{ add(child);}});
			}
			liCalc(child,parentName+(num==0?"":liDot+num));
		}
	}
	
	@Override public String getType()
	{
		return "List";
	}
	@Override public String getTextSay()
	{
		return getType()+" "+rootElement.getText();
	}
	@Override public String getTextView()
	{
		return "["+getType()+" "+rootElement.getText()+"]";
	}
	@Override public String getTextShort()
	{
		String ordered;
		switch(rootElement.getType())
		{
			case "ol":
				ordered="ordered";
				break;
			case "ul":
			default:
				ordered="unordered";
				break;
		}
		return ordered+(isMultilevel?" multilevel":"")+" list with "+liCount+" element";
	}
}
