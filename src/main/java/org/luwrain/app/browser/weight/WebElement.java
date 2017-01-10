
package org.luwrain.app.browser.weight;

import java.util.*;

import org.luwrain.browser.ElementIterator;

public interface WebElement
{
    enum Type {Text, Edit, Button, Checkbox, Radio, Select, List, ListElement, Table, TableRow, TableCell};

    void init();
    Type getType();
    ElementIterator getNode();
    WebElement getParent();
    boolean hasChildren();
    Vector<WebElement> getChildren();
    boolean isComplex();
    boolean alwaysFromNewLine();
    boolean needEndLine();
    boolean needToBeExpanded();
    boolean needToBeHidden();
    String getSplitter();
    String getText();
    String getTextShort();
    void toDelete();
    boolean isDeleted();
    boolean isVisible();
    long getWeight();
    void incWeight(long weight);
    void setAttribute(String name,String value);
    void mixAttributes(WebElement element);
    LinkedHashMap<String,String> getAttributes();
    Vector<Vector<WebElement>> getComplexMatrix();

    /** print this element debug info
     * @param lvl current level in recursive call, for root must be 0
     * @param printChildren if true, children will printed recursive */
    void print(int lvl,boolean printChildren);
    String getDescr();
}
