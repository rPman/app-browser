package org.luwrain.app.browser.web;

import java.util.Vector;

import org.luwrain.core.Log;
import org.luwrain.core.NullCheck;

public class ByFairDistrib implements BigSearcher
{
	/**  minimal weight limit ratio for element to root document element, to allow select this element as BIG */
	double BIG_WEIGHT_LIMIT=0.01;

	/** minimal rate limit for total weight for some element children with fair distribution of weight */
	double BIG_WEIGHT_FAIR_TOTAL = 0.4;

	/** minimal difference between near children (sorted by weight) with fair fair distribution of weight */
	double BIG_WEIGHT_FAIR_CHILD = 0.4;

	/** number of this children in element with fair distribution of weight */
	int BIG_WEIGHT_FAIR_COUNT = 3;

	/** max number of BIG element */
	int BIG_MAX_COUNT = 5;

	private int currentBigCount = 0;
	private WebElement root;
	private WeightSortedSet result;
	
	public ByFairDistrib()
	{
		
	}
	/** make BiGSearcher implementation with fair distribution algorithm with changed constants 
	 * @param BIG_WEIGHT_LIMIT minimal weight limit ratio for element to root document element, to allow select this element as BIG
	 * @param BIG_WEIGHT_FAIR_TOTAL minimal rate limit for total weight for some element children with fair distribution of weight
	 * @param BIG_WEIGHT_FAIR_CHILD minimal difference between near children (sorted by weight) with fair fair distribution of weight
	 * @param BIG_WEIGHT_FAIR_COUNT number of this children in element with fair distribution of weight
	 * @param BIG_MAX_COUNT max number of BIG element
	 */
	public ByFairDistrib(double BIG_WEIGHT_LIMIT,double BIG_WEIGHT_FAIR_TOTAL,double BIG_WEIGHT_FAIR_CHILD,int BIG_WEIGHT_FAIR_COUNT,int BIG_MAX_COUNT)
	{
		this.BIG_WEIGHT_LIMIT=BIG_WEIGHT_LIMIT;
		this.BIG_WEIGHT_FAIR_TOTAL=BIG_WEIGHT_FAIR_TOTAL;
		this.BIG_WEIGHT_FAIR_CHILD=BIG_WEIGHT_FAIR_CHILD;
		this.BIG_WEIGHT_FAIR_COUNT=BIG_WEIGHT_FAIR_COUNT;
		this.BIG_MAX_COUNT=BIG_MAX_COUNT;
	}

	/** weight ratio of element against root */
	private double weightRateRoot(WebElement element)
	{
		return (double)element.getWeight() / root.getWeight();
	}
	
	/** weight ration between two elements, compare max and min of them */
	private double weightComparsionRate(WebElement prevChild,WebElement child)
	{
		return (double)Long.max(prevChild.getWeight(),child.getWeight())/Long.min(prevChild.getWeight(),child.getWeight());
	}


	@Override public void search(WebElement root, WeightSortedSet result)
	{
		this.root=root;
		this.result=result;
		currentBigCount = 0;
		searchBigElementsImpl(1, root);
	}

	private void searchBigElementsImpl(int lvl, WebElement element)
	{
	    NullCheck.notNull(element, "element");
	    //Log.debug("proba", "processing " + element.getDescr());
//		System.out.print("search: ");
//		element.print(lvl,false);
		// check BIG element count
		if(currentBigCount >= BIG_MAX_COUNT)
		{
//		    Log.debug("search", "stopping, big element count exceeded: " + currentBigCount);
			return;
		}
		// check element weight 
		final double weightRateRoot = weightRateRoot(element);
		if(weightRateRoot < BIG_WEIGHT_FAIR_TOTAL)
		{
//		    Log.debug("search", "too small element comparing to weight of the root (" + weightRateRoot + ")");
			return;
		}
		// work with item weight
		final Vector<WebElement> sortedchildren=new Vector<WebElement>();
		// clone children list
		for(WebElement child:element.getChildren())
			sortedchildren.add(child);
		// sort by weight reversed
		//				sortedchildren.sort(new Comparator<WebElement>() {
		//			@Override public int compare(WebElement o1,WebElement o2)
		sortedchildren.sort((o1, o2)->{
				if(o1 == o2 || o1.getWeight() == o2.getWeight()) return 0;
				// reversed order
				return o1.getWeight() < o2.getWeight()?1:-1;
		    });
		// debug info
		//		System.out.print("sorted: ");
		//		for(WebElement e:sortedchildren)
		//			System.out.print(e.getWeight()+" ");
		//		System.out.println();

		// get first BIG_WEIGHT_FAIR_COUNT children and calculate total weight of them
		long totalWeight = 0;
		long fairCount = 0;
		WebElement prevChild = null;
		for(WebElement child: sortedchildren)
		{
			totalWeight += child.getWeight();
			if(prevChild != null)
			{
				double weightComparsionRate=weightComparsionRate(prevChild,child);
				//(double)Long.max(prevChild.getWeight(),child.getWeight())/Long.min(prevChild.getWeight(),child.getWeight())
				if(weightComparsionRate<BIG_WEIGHT_FAIR_CHILD)
				{
					// child not in fair distribution, break
//					System.out.println("break: child not in fair");
					break;
				}
			}
			fairCount++;
			prevChild=child;
		}
		if(fairCount>=BIG_WEIGHT_FAIR_COUNT)
		{
			// this element is a BIG
//			System.out.print("BIG: ");element.print(0,false);
			result.add(element);
			// disable big status for parent (small fix of algorithm)
			WebElement p = element.getParent();
			while(p!=null)
			{
				if(result.contains(p))
				{
					result.remove(p);
					currentBigCount--;
				}
				p=p.getParent();
			}
			// count this BIG
			if(currentBigCount++ >= BIG_MAX_COUNT)
				return;
		} else
		{
			// compare element weight with max child weight with 
			if(weightRateRoot(sortedchildren.get(0))<=BIG_WEIGHT_FAIR_TOTAL)
			{
//				System.out.print("BIG: ");element.print(0,false);
				result.add(element);
				// disable big status for parent (small fix of algorithm)
				WebElement p=element.getParent();
				while(p!=null)
				{
					if(result.contains(p))
					{
						result.remove(p);
						currentBigCount--;
					}
					p=p.getParent();
				}
				// count this BIG
				if(currentBigCount++ >= BIG_MAX_COUNT) 
					return;
			}
		}
		// recurse for all child
		for(WebElement child: sortedchildren)
			searchBigElementsImpl(lvl + 1, child);
	}

}
