
package org.luwrain.app.browser.weight;

import java.awt.Rectangle;

import org.luwrain.browser.ElementIterator;

public class Weight
{
	public interface Calculator
	{
		long calcWeightFor(WebElement element);
	}
	
	/** calculate weight by count, each leaf have weight 1 */
	public static class ByCount implements Calculator
	{
		@Override public long calcWeightFor(WebElement element)
		{
			return 1;
		}
	}
	
	/** calculate weight by rectangle square */
	public static class BySquare implements Calculator
	{
		@Override public long calcWeightFor(WebElement element)
		{
			final Rectangle r = element.getNode().getRect();
			return r.width * r.height;
		}
	}

	/** calculate weight by rectangle web text length */
	public static class ByTextLen implements Calculator
	{
		@Override public long calcWeightFor(WebElement element)
		{
			ElementIterator e=element.getNode();
			long len=e.getText().length();
			// if it link, add href length too
			String href=e.getAttributeProperty("href");
			if(href!=null)
				len+=href.length();
			/*
			switch(element.getType())
			{
				case Button:
				case Checkbox:
				case Edit:
				case List:
				case ListElement:
				case Radio:
				case Select:
				case Table:
				case TableCell:
				case TableRow:
				case Text:
				default:
					break;
			}
			*/
			return len;
		}
	}

}
