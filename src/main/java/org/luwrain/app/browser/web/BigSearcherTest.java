package org.luwrain.app.browser.web;

public class BigSearcherTest
{
	public static void main(WebDocument doc)
	{
		double BIG_WEIGHT_LIMIT=0.6;
		double BIG_WEIGHT_FAIR_TOTAL = 0.4;
		double BIG_WEIGHT_FAIR_CHILD = 0.4;
		int BIG_WEIGHT_FAIR_COUNT = 3;
		int BIG_MAX_COUNT = 5;

		
		for(BIG_WEIGHT_FAIR_TOTAL=0.1;BIG_WEIGHT_FAIR_TOTAL<=0.9;BIG_WEIGHT_FAIR_TOTAL+=0.05)
		{
			// calculate weight and mark BIG elements in sorted set
			doc.elementInit(doc.getRoot(),new Weight.ByTextLen());
			WeightSortedSet result=new WeightSortedSet();
			new ByFairDistrib(BIG_WEIGHT_LIMIT, BIG_WEIGHT_FAIR_TOTAL, BIG_WEIGHT_FAIR_CHILD, BIG_WEIGHT_FAIR_COUNT, BIG_MAX_COUNT).search(doc.getRoot(),result);
			// debug
			//root.print(1,true);
			System.out.println("BIG result for BIG_WEIGHT_FAIR_TOTAL="+BIG_WEIGHT_FAIR_TOTAL);
			for(WebElement e:result)
				e.print(0,false);
		}
	}
}
