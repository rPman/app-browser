package org.luwrain.app.browser.web;

import java.util.Vector;

import org.luwrain.browser.ElementIterator;

public class WebTable extends WebEdit
{
	Vector<Vector<WebElement>> matrix;
	int row,col;
	
	public WebTable(WebElement parent,ElementIterator element)
	{
		super(parent,element);
		matrix=new Vector<Vector<WebElement>>();
	}
	@Override public void init()
	{
		// try to compute size
		final String ctext = nodeIt.getComputedText();
		// calculate numbers for elements
		row=0;
		col=0;
		tableScan(this);
		// FIXME: make condition to complex
		super.needBeginLine=true;
		super.needEndLine=true;
		needToBeComplex=false;
	}
	private void tableScan(WebElement e)
	{
		for(WebElement child:e.getChildren())
		{
			String t=child.getNode().getType();
			switch(t)
			{
				case "th":
				case "td":
					while(matrix.size()<=row-1)
						matrix.add(new Vector<WebElement>());
					matrix.get(row-1).add(child);
					col++;
					WebTableCell tr=(WebTableCell)child;
					// FIXME: make using colspan and rowspan 
					tr.name="row "+row+" column "+col;
					break;
				case "tr":
					row++;
					col=0;
					tableScan(child);
					break;
					// all first level excess tags use to recurse
				case "tbody":
				case "foot":
				case "thead":
				default:
					tableScan(child);
					break;
			}
		}
	}
	
	@Override public Vector<Vector<WebElement>> getComplexMatrix()
	{
		return matrix;
	}
	
	@Override public Type getType()
	{
		return Type.Table;
	}

	@Override public String getText()
	{
		return "<" + nodeIt.getText() + ">";
	}
	@Override public String getTextShort()
	{
		return "Table "+matrix.size()+"rows";
	}
}
