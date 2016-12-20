
package org.luwrain.app.browser.webdoc;

import java.util.*;

import org.luwrain.core.*;
import org.luwrain.app.browser.web.*;
import org.luwrain.browser.Browser;
import org.luwrain.doctree.*;

public class WebDocBuilder
{
    private Node root = null;
    //private Paragraph curParagraph = null;
    private final LinkedList<Run> curParaRuns = new LinkedList<Run>();

    public Document make(Browser page)
    {
	NullCheck.notNull(page, "page");
    	// make WebDocument structure
    	final WebDocument doc = new WebDocument();
    	doc.make(page);
    	// make doctree Document from WebDocument
	root = NodeFactory.newNode(Node.Type.ROOT);
	final LinkedList<Node> subnodes = new LinkedList<Node>();
	make_(subnodes,doc.getRoot());
	root.setSubnodes(subnodes.toArray(new Node[subnodes.size()]));
	return new Document(root);
    }

    /** convert WebElement children to doctree subnodes */
    void make_(LinkedList<Node> subnodes, WebElement element)
    {
	NullCheck.notNull(subnodes, "subnodes");
	NullCheck.notNull(element, "element");
	Log.debug("browser", "transforming " + element.getType() + " (" + element.getClass().getName() + ")");
	Log.debug("browser", "element has " + element.getChildren().size() + " children");
    	for(WebElement child: element.getChildren())
    	{
	    Log.debug("browser", "child of type " + child.getType() + " with " + child.getChildren().size() + " children");
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
		final LinkedList<Node> newNodes = new LinkedList<Node>();
		make_(newNodes, child);
		blockNode.setSubnodes(newNodes.toArray(new Node[newNodes.size()]));
		continue;
	    }
	    if(child instanceof WebText)
	    {
		final String txt = child.getText();
		final Run run = new TextRun(txt);
		curParaRuns.add(run);
		if (child.hasChildren())
		{
		final LinkedList<Node> newNodes = new LinkedList<Node>();
		for(WebElement cc: child.getChildren())
		make_(newNodes, cc);
		if (!newNodes.isEmpty())
		{
		if(!curParaRuns.isEmpty())
		{
		    final Paragraph para = NodeFactory.newPara();
		    para.runs=curParaRuns.toArray(new Run[curParaRuns.size()]);
		    subnodes.add(para);
		    curParaRuns.clear();
		}
		for(Node n: newNodes)
		    subnodes.add(n);
		} //has newNodes
		continue;
		}
	    } //WebText
	    final String txt;
	    if(child instanceof WebEdit)
		txt = "Edit " + child.getText(); else
		if(child instanceof WebCheckbox)
		    txt = "Checkbox " + child.getText(); else
		    if(child instanceof WebRadio)
			txt = "Radiobutton " + child.getText(); else
			if(child instanceof WebSelect)
			    txt = "Select " + child.getText(); else
			    continue;
	    final Run run = new TextRun(txt);
	    curParaRuns.add(run);
	}
	if(!curParaRuns.isEmpty())
	{
	    final Paragraph para = NodeFactory.newPara();
	    para.runs=curParaRuns.toArray(new Run[curParaRuns.size()]);
	    subnodes.add(para);
	    curParaRuns.clear();
	}
	Log.debug("browser", "prepared " + subnodes.size() + " subnodes");
    }

    private Node createBlockNode(WebElement el)
    {
	NullCheck.notNull(el, "el");
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

	if(el instanceof WebTable)
	    return NodeFactory.newSection(1);
	if(el instanceof WebTableRow)
	    return NodeFactory.newSection(1);
	if(el instanceof WebTableCell)
	    return NodeFactory.newSection(1);
	if(el instanceof WebList)
	    return NodeFactory.newSection(1);
	return null;


    }
}
