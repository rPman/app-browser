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
		// calculate numbers for elements and detect multilevel
		liCount=0;
		isMultilevel=false;
		liCalc(this,"");
		// FIXME: make condition to complex
		needToBeComplex=false;
		super.needBeginLine=true;
		super.needEndLine=true;
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
		for(WebElement child:element.getChildren())
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
	
	@Override public Type getType()
	{
		return Type.List;
	}
	@Override public String getTextSay()
	{
		return rootElement.getText();
	}
	@Override public String getTextView()
	{
		return "["+getType().name()+" "+rootElement.getText()+"]";
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
