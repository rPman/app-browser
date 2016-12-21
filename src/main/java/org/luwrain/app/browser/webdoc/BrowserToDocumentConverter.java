package org.luwrain.app.browser.webdoc;

import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Vector;

import org.luwrain.app.browser.selector.SelectorAll;
import org.luwrain.app.browser.selector.SelectorAllImpl;
import org.luwrain.app.browser.web.WebCheckbox;
import org.luwrain.app.browser.web.WebEdit;
import org.luwrain.app.browser.web.WebElement;
import org.luwrain.app.browser.web.WebList;
import org.luwrain.app.browser.web.WebRadio;
import org.luwrain.app.browser.web.WebSelect;
import org.luwrain.app.browser.web.WebTable;
import org.luwrain.app.browser.web.WebTableCell;
import org.luwrain.app.browser.web.WebTableRow;
import org.luwrain.app.browser.web.WebText;
import org.luwrain.browser.Browser;
import org.luwrain.browser.ElementIterator;
import org.luwrain.core.Log;
import org.luwrain.core.NullCheck;
import org.luwrain.doctree.Document;
import org.luwrain.doctree.Node;
import org.luwrain.doctree.NodeFactory;
import org.luwrain.doctree.Paragraph;
import org.luwrain.doctree.Run;
import org.luwrain.doctree.TextRun;

/** this class represent main method to create doctree Document from Browser
 * - any empty or invisible nodes cleaned up
 * - some groups of nodes, grouped to addition tables row by row by onscreen position */
public class BrowserToDocumentConverter
{
	private Browser browser;

	private final LinkedList<Run> curParaRuns = new LinkedList<Run>();

	/** private temporary structure to story node tree for cleaning up before make document */
	class NodeInfo
	{
		/** create root node */
		public NodeInfo()
		{
			this.parent=null;
			this.element=null;
			// root node does not exist in index
		}
		public NodeInfo(NodeInfo parent,ElementIterator element)
		{
			this.parent=parent;
			this.element=element.clone();
			parent.children.add(this);
			//
			index.put(element.getPos(),this);
		}
		public NodeInfo parent;
		public ElementIterator element;
		public Vector<NodeInfo> children = new Vector<NodeInfo>();
		/** list of nodes, mixed with this node for cleanup */
		public Vector<ElementIterator> mixed = new Vector<ElementIterator>();
		public boolean toDelete=false;
		
		/** return element and reversed mixed in list */
		public Vector<ElementIterator> getMixedinfo()
		{
			Vector<ElementIterator> res=new Vector<ElementIterator>();
			res.add(element);
			// we already add mixed in reversed mode
			if(!mixed.isEmpty())
				res.addAll(mixed);
			return res;
		}
		
		// debug
		public void debug(int lvl,boolean printChildren)
		{
			System.out.print(new String(new char[lvl]).replace("\0", "."));
			if(element==null)
			{
				System.out.println("ROOT");
			} else
			{
				System.out.print(element.getPos()+": ");
				//if(toDelete) System.out.print("DELETE ");
				if(!mixed.isEmpty())
				{
					System.out.print("[");
					for(ElementIterator e:mixed)
					{
						System.out.print((e==null?"ROOT":e.getHtmlTagName())+" ");
					}
					System.out.print("] ");
				}
				System.out.print(element.getHtmlTagName()+" ");
				String str=element.getText().replace('\n',' ');
				System.out.print(element.getType()+": '"+str.substring(0,Math.min(160,str.length()))+"'");
				System.out.println();
			}
			if(printChildren)
				for(NodeInfo e:children)
					e.debug(lvl+1,true);
		};
	}
	/** reverse index for accessing NodeInfo by its ElementIterator's pos */
	HashMap<Integer,NodeInfo> index;
	final NodeInfo tempRoot = new NodeInfo();
	
	/** create new doctree Document from current state of Browser */
	public Document go(Browser browser)
	{
		this.browser = browser;
		// fill temporary tree of Browser's nodes information
		fillTemporaryTree();
		// clean up temporary tree
		while(cleanup(tempRoot)!=0){};
		splitSingleChildrenNodes(tempRoot);
		// now we have compact NodeInfo's tree
		tempRoot.debug(1,true);
		// make document
		Document doc = makeDocument();
		return doc;
	}
	
	/** return first visible parent of element or null if root child */
	ElementIterator checkVisibleParent(ElementIterator element)
	{
		while(element!=null)
		{
			if(element.isVisible())
				return element;
			element=element.getParent();
		}
		return null;
	}

	private Document makeDocument()
	{
		// make root subnodes list to make doctree Document
		Node root=NodeFactory.newNode(Node.Type.ROOT);
		// fill subnodes recursive
		final LinkedList<Node> subnodes=make_(tempRoot);
		root.setSubnodes(subnodes.toArray(new Node[subnodes.size()]));
		//
		Document doc = new Document(root);
		doc.commit();
		return doc;
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
				new NodeInfo(tempRoot,e);
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
					new NodeInfo(index.get(parent.getPos()),e);
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
				final Run run = new TextRun(txt);
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
	
	String tagName = node.element.getHtmlTagName().toLowerCase();
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
