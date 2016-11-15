package org.luwrain.app.browser.web;

import java.util.Vector;

import org.luwrain.core.Luwrain;
import org.luwrain.popups.Popups;

public class BigSearcherTest
{
	public static void main(WebDocument doc,Luwrain luwrain)
	{
		double BIG_WEIGHT_LIMIT=0.13;
		double BIG_WEIGHT_FAIR_TOTAL = 0.4;
		double BIG_WEIGHT_FAIR_CHILD = 0.4;
		int BIG_WEIGHT_FAIR_COUNT = 3;
		int BIG_MAX_COUNT = 5;

		
		String wmin = Popups.simple(luwrain, "BIG_WEIGHT_LIMIT", "min: ", "0.05");
		String wmax = Popups.simple(luwrain, "BIG_WEIGHT_LIMIT", "max: ", "0.5");
		String wstp = Popups.simple(luwrain, "BIG_WEIGHT_LIMIT", "max: ", "0.5");
		double wn=Double.parseDouble(wmin);
		double wx=Double.parseDouble(wmax);
		double ws=Double.parseDouble(wstp);
		//for(BIG_WEIGHT_FAIR_COUNT=2;BIG_WEIGHT_FAIR_COUNT<4;BIG_WEIGHT_FAIR_COUNT++)
		//{
			for(BIG_WEIGHT_LIMIT=wn;BIG_WEIGHT_LIMIT<wx;BIG_WEIGHT_LIMIT+=ws)
			{
				System.out.println(
						//"\nBIG_WEIGHT_FAIR_COUNT:="+BIG_WEIGHT_FAIR_COUNT+
						", BIG_WEIGHT_LIMIT:="+String.format("%1.4f",BIG_WEIGHT_LIMIT));
				//
				for(BIG_WEIGHT_FAIR_TOTAL=0.05;BIG_WEIGHT_FAIR_TOTAL<=0.4;BIG_WEIGHT_FAIR_TOTAL+=0.025)
					System.out.print("\t\t"+String.format("%1.4f",BIG_WEIGHT_FAIR_TOTAL));
				System.out.println();		
				
				for(BIG_WEIGHT_FAIR_CHILD=0.05;BIG_WEIGHT_FAIR_CHILD<=0.55;BIG_WEIGHT_FAIR_CHILD+=0.05)
				{
					Vector<Double> w1=new Vector<Double>(); 
					Vector<Double> w2=new Vector<Double>(); 
					System.out.print(String.format("%1.4f",BIG_WEIGHT_FAIR_CHILD));
					for(BIG_WEIGHT_FAIR_TOTAL=0.05;BIG_WEIGHT_FAIR_TOTAL<=0.4;BIG_WEIGHT_FAIR_TOTAL+=0.025)
					{
						// calculate weight and mark BIG elements in sorted set
						doc.elementInit(doc.getRoot(),new Weight.ByTextLen());
						WeightSortedSet result=new WeightSortedSet();
						new ByFairDistrib(BIG_WEIGHT_LIMIT, BIG_WEIGHT_FAIR_TOTAL, BIG_WEIGHT_FAIR_CHILD, BIG_WEIGHT_FAIR_COUNT, BIG_MAX_COUNT).search(doc.getRoot(),result);
						// debug
						//root.print(1,true);
			
						System.out.print("\t"+result.size());
						// count sum of BIG weight
						long sum=0;
						for(WebElement e:result)
							sum+=e.getWeight();
						System.out.print("\t"+String.format("%1.4f",(double)sum/doc.getRoot().getWeight()));
						w1.add(((double)sum/doc.getRoot().getWeight())/result.size());
						w2.add(((double)sum/doc.getRoot().getWeight())*result.size());
						//System.out.println("BIG result for BIG_WEIGHT_FAIR_TOTAL="+BIG_WEIGHT_FAIR_TOTAL);
						//for(WebElement e:result)
						//	e.print(0,false);
					}
					System.out.print("\t\t\t");
					for(Double w:w1)
						System.out.print("\t"+String.format("%1.4f",w));
					System.out.print("\t\t\t");
					for(Double w:w2)
						System.out.print("\t"+String.format("%1.4f",w));
					System.out.println();		
				}
				
				doc.elementInit(doc.getRoot(),new Weight.ByTextLen());
				WeightSortedSet result=new WeightSortedSet();
				new ByFairDistrib(BIG_WEIGHT_LIMIT, 0.2, 0.125, BIG_WEIGHT_FAIR_COUNT, BIG_MAX_COUNT).search(doc.getRoot(),result);
				System.out.println("BIG result:");
				for(WebElement e:result)
					e.print(0,false);
				System.out.println("BIG result END");

			}
		//}
	}
}
