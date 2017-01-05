
package org.luwrain.app.browser;

import java.awt.Rectangle;
import java.util.*;

import org.luwrain.core.*;
import org.luwrain.doctree.*;
import org.luwrain.browser.*;

import org.luwrain.app.browser.selector.SelectorAll;
import org.luwrain.app.browser.selector.SelectorAllImpl;

/** this class represent main method to create doctree Document from Browser
 * - any empty or invisible nodes cleaned up
 * - some groups of nodes, grouped to addition tables row by row by onscreen position */
class DocumentBuilder
{
	/** maximum number of cols in table to awoid too small cols FIXME: make fix in DocumentArea to work with cell content does not fit in */
	static final int TABLE_MAXIMUM_COLUMN_COUNT=4;

    private final Browser browser;

    private final LinkedList<Run> curParaRuns = new LinkedList<Run>();

	/** reverse index for accessing NodeInfo by its ElementIterator's pos */
    private HashMap<Integer,NodeInfo> index;
private final NodeInfo tempRoot;

	/** list node position to watch it's content in Browser */
    LinkedList<Integer> watch = new LinkedList<Integer>();

    DocumentBuilder(Browser browser)
    {
	NullCheck.notNull(browser, "browser");
	this.browser = browser;
	this.tempRoot = new NodeInfo();
    }

	/** create new doctree Document from current state of Browser */
    Document build()
	{
		curParaRuns.clear();
		index = new HashMap<Integer,NodeInfo>();
		//		tempRoot = new NodeInfo();
		watch.clear();
		//		this.browser = browser;
		// fill temporary tree of Browser's nodes information
		fillTemporaryTree();
		// clean up temporary tree
		while(cleanup(tempRoot) != 0){};
		splitSingleChildrenNodes(tempRoot);
		// now we have compact NodeInfo's tree
		//**/tempRoot.debug(1,true);
return makeDocument();
	}

    /** Returns the first visible parent element*/
private ElementIterator checkVisibleParent(ElementIterator element)
	{
	    NullCheck.notNull(element, "element");
	    ElementIterator el = element;
		while(el != null)
		{
			if(el.isVisible())
				return el;
el = el.getParent();
		}
		return null;
	}

	/** make doctree Document
	 * any leaf elements added as Run but
	 * -  different screen positon by Y make each node to new paragraph
	 * - same parent + same Y screen position - threat as the same paragraph
	 * - TODO: same parent + same X screen position - thread as block and added as new table row
	 *   block detection algorithm called before creating Nodes
	 *   //
	 * - table, list and form - start new section (or table/list node)
	 *  */
	private Document makeDocument()
	{
		// scan full tree for block detection, tables, lists and so on
		// make planar list of Run's
		final Node root=NodeFactory.newNode(Node.Type.ROOT);
		final LinkedList<Node> subnodes = makeNodes(tempRoot);
		root.setSubnodes(subnodes.toArray(new Node[subnodes.size()]));
return new Document(root);
	}
	
	/** make doctree Node list for node and children */
private LinkedList<Node> makeNodes(NodeInfo base)
	{
	    NullCheck.notNull(base, "base");
//**/System.out.print("makeNodes: ");base.debug(0,false);		
		final LinkedList<Node> subnodes = new LinkedList<Node>();
		final Vector<RunInfo> runList = makeRuns(base);
		
		// TODO: search elements with same X screen (equals for x pos) and different Y - same group
		// TODO: search elements with different Y and same X (intersect intervals! not equal like for X)

Paragraph node=NodeFactory.newPara();
		
		final LinkedList<Run> subruns=new LinkedList<Run>();
		Rectangle rect = null;
		for(RunInfo r: runList)
		{
			if(r.isNode())
			{
				node.runs=subruns.toArray(new Run[subruns.size()]);
				subruns.clear();
				subnodes.add(node);
				node=NodeFactory.newPara();
				subnodes.add(r.node);
				continue;
			}
			// first rect compare with itself
			if(rect == null)
rect=r.info.element.getRect();
			// check, if next r in the same Y interval like previous
			final Rectangle curRect=r.info.element.getRect();
			if((curRect.y>=rect.y&&curRect.y<rect.y+rect.height)
			 ||(rect.y>=curRect.y&&rect.y<curRect.y+curRect.height))
			{ // prev and current run in the same line on screen
				// keep this runs on line
			} else
			{ // next line = new paragraph (list and tables detected in other place)
				node.runs=subruns.toArray(new Run[subruns.size()]);
				subruns.clear();
				subnodes.add(node);
				node=NodeFactory.newPara();
			}
			subruns.add(r.run);
			rect=curRect;
		}
		node.runs = subruns.toArray(new Run[subruns.size()]);
		subruns.clear();
		subnodes.add(node);
		node = NodeFactory.newPara();
				return subnodes;
	}

	/** recursive method to collect Run's for each leaf element in NodeInfo tree */
private Vector<RunInfo> makeRuns(NodeInfo node)
	{
	    NullCheck.notNull(node, "node");
//**/System.out.print("makeRuns: ");node.debug(0,false);
		ElementAction action=null;
		final Vector<RunInfo> runList=new Vector<RunInfo>();
		final ElementIterator n=node.element;
		final String tagName = n.getHtmlTagName().toLowerCase();;
		if(node.children.isEmpty())
		{
			String txt = "";
			switch(tagName)
			{
				case "img":
					txt="Image: "+n.getText();
				break;
				case "input":
					String type=n.getAttributeProperty("type");
					if(type==null) type="";
					switch(type)
					{
						case "image":
						case "button":
						case "submit":
							txt="Button: "+n.getText();
							action=new ElementAction(ElementAction.Type.CLICK,node.element);
						break;
						case "radio":
							txt="Radio: "+n.getText();
							action=new ElementAction(ElementAction.Type.CLICK,node.element);
						break;
						case "checkbox":
							txt="Checkbox: "+n.getText();
							action=new ElementAction(ElementAction.Type.CLICK,node.element);
						break;
						case "text":
						default:
							txt="Edit: "+n.getText();
							action=new ElementAction(ElementAction.Type.EDIT,node.element);
						break;
					}
				break;
				case "button":
					txt="Button: "+n.getText();
					action=new ElementAction(ElementAction.Type.CLICK,node.element);
				break;
				case "select":
					txt="Select: "+n.getText();
					action=new ElementAction(ElementAction.Type.SELECT,node.element);
				break;
				default:
					txt=n.getText();
				break;
			}
			if(!node.mixed.isEmpty())
			{ // check for A tag inside mixed
				for(ElementIterator e:node.getMixedinfo())
				{
					String etag = e.getHtmlTagName().toLowerCase();;
					if(etag.equals("a"))
					{
						// FIXME: here we have href attribute
						txt="Link: "+txt;
						action=new ElementAction(ElementAction.Type.CLICK,node.element);
						break;
					} else
					if(etag.equals("button"))
					{
						txt="Button: "+txt;
						action=new ElementAction(ElementAction.Type.CLICK,node.element);
						break;
					}
				}
			}
			// any non edit elements was UNKNOWN
			if(action==null)
			{
				action=new ElementAction(ElementAction.Type.UNKNOWN,node.element);
			}
			txt=cleanupText(txt);
			txt+=" "+cleanupText(n.getAltText());
			final TextRun run = new TextRun(txt.trim()+" ");
			if(action!=null) run.setAssociatedObject(action);
			runList.add(new RunInfo(run,node));
			watch.add(node.element.getPos());

		} else
		{
			switch(tagName)
			{
				// list
				case "ol":
				case "ul": // li element can be mixed with contents, but each child of node is a li
					//System.out.println("Found list: "+node.children.size());
					Node listNode=NodeFactory.newNode(tagName.equals("ol")?Node.Type.ORDERED_LIST:Node.Type.UNORDERED_LIST);
					LinkedList<Node> listItems=new LinkedList<Node>();
					for(NodeInfo child:node.children)
					{
						Node listItem=NodeFactory.newNode(Node.Type.LIST_ITEM);
						LinkedList<Node> listItemNodes = makeNodes(child);
						listItem.setSubnodes(listItemNodes.toArray(new Node[listItemNodes.size()]));
						listItems.add(listItem);
					}
					listNode.setSubnodes(listItems.toArray(new Node[listItems.size()]));
					runList.add(new RunInfo(listNode,node));
					//System.out.println("List end");
				break;
				// table
				case "table": // table can be mixed with any other element, for example parent form
				case "tbody": // but if tbody not exist, table would exist as single, because tr/td/th can't be mixed
					//System.out.println("Found table: "+node.children.size()+" rows");
					LinkedList<LinkedList<Node>> table=new LinkedList<LinkedList<Node>>();
					// any child is a rows! we can do not check it
					for(NodeInfo child:node.children)
					{ // each rows contains a table cell or header cell, but also we can see tbody, tfoor, thead, we must enter into
						LinkedList<Node> row=new LinkedList<Node>();
						// detect thead, tbody, tfoot
						NodeInfo child_=child;
						String tagName3=child.element.getHtmlTagName().toLowerCase();
						switch(tagName3)
						{
							case "thead":
							case "tfoot":
							case "tbody":
								// check child exist, if not, skip this row at all
								if(!child.children.isEmpty())
									child_=child.children.firstElement();
								// we must go out here but we can pass this alone child next without errors 
							break;
						}
						// list of cells
						for(NodeInfo cellChild:child_.children)
						{
							// detect collspan
							String tagName2=cellChild.element.getHtmlTagName().toLowerCase();
							String collSpanStr=null;
							switch(tagName2)
							{
								case "td":
								case "th":
									collSpanStr=cellChild.element.getAttributeProperty("colspan");
								//break; // no we can skip this check becouse we don't known what to do if we detect errors here
								default:
									Integer collSpan=null;
									if(collSpanStr!=null) try
									{
										collSpan=Integer.parseInt(collSpanStr);
									} catch(Exception e)
									{} // we can skip this error
									// add node
									Node cellNode=NodeFactory.newNode(Node.Type.TABLE_CELL);
									// mak recursive call makeNodes
									LinkedList<Node> cellNodes = makeNodes(cellChild);
									row.addAll(cellNodes);
									// emulate collspan FIXME: make Document table element usable with it
									if(collSpan!=null)
									{ // we have colspan, add empty colls to table row
										for(;collSpan>0;collSpan--)
										{
											Node emptyCellNode=NodeFactory.newNode(Node.Type.TABLE_CELL);
											Paragraph emptyPar=NodeFactory.newPara("");
											emptyCellNode.setSubnodes(new Node[]{emptyPar});
											row.add(emptyCellNode);
										}
									}
								break;	
							}
						}
						table.add(row);

					}
					// add empty cells to make table balanced by equal numbers of colls each row
					// move multiple colls to section inside of it
					// [col1,col2,col3,col4,col5,col6] -> [{col1,col2},{col3,col4},{col5,col6}]
					//
					// TODO:
					// call setSubnodes each one row
					Node tableNode=NodeFactory.newNode(Node.Type.TABLE);
					LinkedList<Node> tableRowNodes=new LinkedList<Node>();
					for(LinkedList<Node> rows:table)
					{
						Node tableRowNode=NodeFactory.newNode(Node.Type.TABLE_ROW);
						tableRowNode.setSubnodes(rows.toArray(new Node[rows.size()]));
						tableRowNodes.add(tableRowNode);
					}
					tableNode.setSubnodes(tableRowNodes.toArray(new Node[tableRowNodes.size()]));
					//
					runList.add(new RunInfo(tableNode,node));
				break;
				default:
					// unknown group mixed to run list, it would be splited to paragraphs later
					for(NodeInfo child:node.children)
						runList.addAll(makeRuns(child));
				break;
			}
		}
		return runList;
	}

	private String cleanupText(String txt)
	{
		return txt.replace("/\\s+/g"," ").trim();
	}

	void fillTemporaryTree()
	{
		// make new temporary node tree
		index = new HashMap<Integer, NodeInfo>();
		// fill temporary tree from current Browser structure as fast as possible
		// selector all for any visible nodes, include non text images and so on
		SelectorAll allVisibleNodes = new SelectorAllImpl(true);
		ElementIterator e = browser.iterator();
		// node count without root node (it not exist in SelectorAll enumerator)
		int count = 0;
		// we will scan allVisibleNodes many times, while count != index.size(), except for first scan, while count calculated 
		// first scan (calculate element count and root children)
		if(allVisibleNodes.moveFirst(e))
		do
		{ // check each Browser node for parent in index
			if(e.getParent()==null)
			{ // e - root child
			    new NodeInfo(tempRoot,e, index);
			}
			count++;
		} while(allVisibleNodes.moveNext(e));
		// check for Browser bug, if we found nodes not connected to root (multiple tree root's- it impossible but we must check it)
		int lastCount = 0;
		// infinite loop, while found all nodes
		while(count!=index.size())
		{
			if(allVisibleNodes.moveFirst(e))
			do
			{ // check each Browser node for parent in index
				ElementIterator parent = checkVisibleParent(e.getParent());
				if(parent!=null&&!index.containsKey(e.getPos())&&index.containsKey(parent.getPos()))
				{ // we have parent node already in index, add this node e as child
				    new NodeInfo(index.get(parent.getPos()),e, index);
				}
			} while(allVisibleNodes.moveNext(e));
			else
			{ // we have no any node in Browser, exit
				break;
			}
			// check for multiple root bug
			if(lastCount==index.size())
			{
				Log.error("web-browser","Browser contains nodes not connected to root, it is a bug of Browser.RescanDOM implementation. count="+count+", index.size="+index.size());
				if(allVisibleNodes.moveFirst(e))
				do
				{
					if(!index.containsKey(e.getPos()))
					{
					System.out.print("# "+e.getPos()+": ");
					System.out.print("p:"+(e.getParent()==null?"null":e.getParent().getPos())+" ");
					System.out.print(e.getHtmlTagName()+" ");
					String str=e.getText().replace('\n',' ');
					System.out.print(e.getType()+": '"+str.substring(0,Math.min(160,str.length()))+"'");
					System.out.println();
					}
				} while(allVisibleNodes.moveNext(e));
				break;
			}
			lastCount = index.size();
		}
	}

	/** recursive split single child container to one for node and his children*/
	private void splitSingleChildrenNodes(NodeInfo node)
	{
		if(node.children.size()==0) return;
		if(node.children.size()==1)
		{ // we found node with single child, replace parent for this child but not for all
			NodeInfo child=node.children.firstElement();
			String tagName = child.element.getHtmlTagName().toLowerCase();
			switch(tagName)
			{
				// skip this nodes
				case "option": // html SELECT option's nodes
				case "td": // table cells
				case "tr": // table rows
				case "th": // table cells
				case "li": // element list
					splitSingleChildrenNodes(child);
				return;
				default:
					// move this element to parent and mark for deletion
					node.mixed.add(0,node.element);
					node.element=child.element;
					node.children=child.children;
					//child.toDelete=true;
					//
					splitSingleChildrenNodes(node);
				return;
			}
		} else
		for(NodeInfo child:node.children)
		{
			splitSingleChildrenNodes(child);
		}
	}

	/** remove empty elements FIXME: make it more wisely, do not remove interactive elements with onclick and soon */
	private int cleanup(NodeInfo node)
	{
		int count=0;
		if(node.children.isEmpty())
		{
			String text=node.element.getText();
			if(text.isEmpty())
			{ // remove empty text element without child
				String tagName = node.element.getHtmlTagName().toLowerCase();
				switch(tagName)
				{
				/*
				// do not remove
				case "img":
				case "option": // html SELECT option's nodes
				case "td": // table cells
				case "tr": // table rows
				case "th": // table cells
				case "li": // element list
				case "a":
				case "input":
				case "button":
				return 0;
				 */
				// remove
				case "span":
				case "div":
				case "li":
					// remove it from parent
					if(node.parent!=null)
					{
						node.toDelete=true;
						count++;
					}
				break;
				}
			}
		} else
		for(NodeInfo child:node.children)
			count+=cleanup(child);
		
		Iterator<NodeInfo> i=node.children.iterator();
		while (i.hasNext())
		{
			NodeInfo child=i.next();
			if(child.toDelete)
				i.remove();
		}
		return count;
	}

	LinkedList<Node> makeRecursive(NodeInfo node)
	{
		final LinkedList<Node> subnodes = new LinkedList<Node>();
						return subnodes;
	}
	
	LinkedList<Node> make_(NodeInfo node)
	{
		final LinkedList<Node> subnodes = new LinkedList<Node>();

		NullCheck.notNull(node, "node");
		Log.debug("browser", "transforming " + node.element.getType() + " (" + node.element.getClass().getName() + ")");
		Log.debug("browser", "element has " + node.children.size() + " children");
    	for(NodeInfo child: node.children)
    	{
			Log.debug("browser", "child of type " + child.element.getType() + " with " + child.children.size() + " children");
			final Node blockNode = createBlockNode(child);
			if (blockNode != null)
			{
				if(!curParaRuns.isEmpty())
				{
				    final Paragraph para = NodeFactory.newPara();
				    para.runs=curParaRuns.toArray(new Run[curParaRuns.size()]);
				    subnodes.add(para);
				    curParaRuns.clear();
				}
				subnodes.add(blockNode);
				final LinkedList<Node> newNodes = make_(child);
				blockNode.setSubnodes(newNodes.toArray(new Node[newNodes.size()]));
				continue;
			}
			// is text
			if(child.children.isEmpty())
			{
				final ElementIterator n=child.element;
				String txt = "";
				String tagName = n.getHtmlTagName().toLowerCase();;
				switch(tagName)
				{
					case "input":
						String type=n.getAttributeProperty("type");
						switch(type)
						{
							case "image":
							case "button":
								txt="Button: "+n.getText();
							break;
							case "radio":
								txt="Radio: "+n.getText();
							break;
							case "checkbox":
								txt="Checkbox: "+n.getText();
							break;
							case "text":
							default:
								txt="Edit: "+n.getText();
							break;
						}
					break;
					case "button":
						txt="Button: "+n.getText();
					break;
					case "select":
						txt="Select: "+n.getText();
					break;
					default:
						txt=n.getText();
					break;
				}
				if(!child.mixed.isEmpty())
				{ // check for A tag inside mixed
					for(ElementIterator e:child.getMixedinfo())
					{
						String etag = e.getHtmlTagName().toLowerCase();;
						if(etag.equals("a"))
						{
							// FIXME: here we have href attribute
							txt="Link: "+txt;
							break;
						} else
						if(etag.equals("button"))
						{
							txt="Button: "+txt;
							break;
						}
					}
				}
				txt=cleanupText(txt);
				txt+=" "+cleanupText(n.getAltText());
				final Run run = new TextRun(txt.trim()+" ");
				curParaRuns.add(run);
			} else
			//if (!child.children.isEmpty())
			{
				final LinkedList<Node> newNodes = new LinkedList<Node>();
				for(NodeInfo cc: child.children)
					newNodes.addAll(make_(cc));
				if (!newNodes.isEmpty())
				{
				if(!curParaRuns.isEmpty())
				{
				    final Paragraph para = NodeFactory.newPara();
				    para.runs=curParaRuns.toArray(new Run[curParaRuns.size()]);
				    subnodes.add(para);
				    curParaRuns.clear();
				}
				    subnodes.addAll(newNodes);
				} //has newNodes
				continue;
			}
//			} //WebText
		    final String txt;
			if(!curParaRuns.isEmpty())
			{
			    final Paragraph para = NodeFactory.newPara();
			    para.runs=curParaRuns.toArray(new Run[curParaRuns.size()]);
			    subnodes.add(para);
			    curParaRuns.clear();
			}
			Log.debug("browser", "prepared " + subnodes.size() + " subnodes");
	    }
		return subnodes;
	}

	private Node createBlockNode(NodeInfo node)
	{
	NullCheck.notNull(node, "node");
	/*
	if(el instanceof WebTable)
	    return NodeFactory.newNode(Node.Type.TABLE);
	if(el instanceof WebTableRow)
	    return NodeFactory.newNode(Node.Type.TABLE_ROW);
	if(el instanceof WebTableCell)
	    return NodeFactory.newNode(Node.Type.TABLE_CELL);
	if(el instanceof WebList)
	    return NodeFactory.newNode(el.getNode().getType().equals("ol")?Node.Type.ORDERED_LIST:Node.Type.UNORDERED_LIST);
	return null;
	*/
	
	final String tagName = node.element.getHtmlTagName().toLowerCase();
	switch(tagName)
	{
		case "table":
		case "tr":	
		case "td":	
		case "th":	
		case "tbody":	
		case "li":	
		case "ol":	
		case "ul":	
			return NodeFactory.newSection(1);
	}
	return null;
	}

}
