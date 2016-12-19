package org.luwrain.app.browser.webdoc;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Vector;

import org.luwrain.app.browser.web.WebCheckbox;
import org.luwrain.app.browser.web.WebDocument;
import org.luwrain.app.browser.web.WebEdit;
import org.luwrain.app.browser.web.WebElement;
import org.luwrain.app.browser.web.WebList;
import org.luwrain.app.browser.web.WebListItem;
import org.luwrain.app.browser.web.WebRadio;
import org.luwrain.app.browser.web.WebSelect;
import org.luwrain.app.browser.web.WebTable;
import org.luwrain.app.browser.web.WebTableCell;
import org.luwrain.app.browser.web.WebTableRow;
import org.luwrain.app.browser.web.WebText;
import org.luwrain.browser.Browser;
import org.luwrain.doctree.Document;
import org.luwrain.doctree.Node;
import org.luwrain.doctree.NodeFactory;
import org.luwrain.doctree.Paragraph;
import org.luwrain.doctree.Run;
import org.luwrain.doctree.TextRun;

public class WebDocBuilder
{
    private Node root = null;
    //private Paragraph curParagraph = null;
    private Vector<Run> curParaRuns = null;

    public Document make(Browser page)
	{
    	// make WebDocument structure
    	WebDocument doc = new WebDocument();
    	doc.make(page);
    	// make doctree Document from WebDocument
	    root = NodeFactory.newNode(Node.Type.ROOT);
	    final LinkedList<Node> subnodes = new LinkedList<Node>();
	    make_(subnodes,doc.getRoot());
		root.setSubnodes(subnodes.toArray(new Node[subnodes.size()]));
		return new Document(root);
    	
	}
    
    /** convert WebElement children to doctree subnodes */
    void make_(LinkedList<Node> subnodes,WebElement element)
    {
    	for(WebElement child:element.getChildren())
    	{
    		Node node = null;
    		if(child instanceof WebTable)
    		{
    			node = NodeFactory.newNode(Node.Type.TABLE);
    		} else
    		if(child instanceof WebTableRow)
    		{
    			node = NodeFactory.newNode(Node.Type.TABLE_ROW);
    		} else
    		if(child instanceof WebTableCell)
    		{
    			node = NodeFactory.newNode(Node.Type.TABLE_CELL);
    		} else
    		if(child instanceof WebList)
    		{
    			node = NodeFactory.newNode(child.getNode().getType().equals("ol")?Node.Type.ORDERED_LIST:Node.Type.UNORDERED_LIST);
    		} else
    		if(child instanceof WebEdit)
    		{ // WebEdit, WebCheckbox, WebRadio, WebSelect
    			// add this element to current paragraph
    			if(curParaRuns==null)
    			{ // make new current paragraph 
    				curParaRuns = new Vector<Run>();
    			}
    			// make text line to curParagraph FIXME: make many other runs for WebEdit variants
    			String txt = "Unknown " + child.getText();
    			if(child instanceof WebEdit)
    			{
    				txt = "Edit " + child.getText();
    			} else
    			if(child instanceof WebCheckbox)
    			{
    				txt = "Checkbox " + child.getText();
    			} else
    			if(child instanceof WebRadio)
    			{
    				txt = "Radiobutton " + child.getText();
    			} else
    			if(child instanceof WebSelect)
    			{
    				txt = "Select " + child.getText();
    			}
    				
    			Run run = new TextRun(txt);
    			curParaRuns.add(run);
    			// we must break to avoid to come to code, only for section elements
    			break;
    		} else
   			if(child instanceof WebText&&!child.hasChildren())
    		{ // has children, it is section
    			String txt = child.getText();
    			if(curParaRuns==null)
    			{ // make new current paragraph 
    				curParaRuns = new Vector<Run>();
    			}
    			Run run = new TextRun(txt);
    			curParaRuns.add(run);
    		}
    		// add current non empty paragraph and make it null
    		if(curParaRuns!=null)
    		{
    			Paragraph para=NodeFactory.newPara();
    			para.runs=curParaRuns.toArray(new Run[curParaRuns.size()]);
    			subnodes.add(para);
    			curParaRuns=null;
    		}
    		// recursive add subchildren to node (table/list/section) 	
			subnodes.add(node);
    		final LinkedList<Node> subchildren = new LinkedList<Node>();
    		make_(subchildren,child);
    		node.setSubnodes(subchildren.toArray(new Node[subchildren.size()]));
    	}
    }
	
}
