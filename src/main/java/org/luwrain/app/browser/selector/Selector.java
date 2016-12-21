/*
   Copyright 2012-2016 Michael Pozhidaev <michael.pozhidaev@gmail.com>
   Copyright 2015-2016 Roman Volovodov <gr.rPman@gmail.com>

   This file is part of the LUWRAIN.

   LUWRAIN is free software; you can redistribute it and/or
   modify it under the terms of the GNU General Public
   License as published by the Free Software Foundation; either
   version 3 of the License, or (at your option) any later version.

   LUWRAIN is distributed in the hope that it will be useful,
   but WITHOUT ANY WARRANTY; without even the implied warranty of
   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
   General Public License for more details.
*/

package org.luwrain.app.browser.selector;

import org.luwrain.browser.ElementIterator;

/** General navigation operations based on the behaviour of {@code suits()} method*/
public interface Selector
{
    boolean suits(ElementIterator list);

    /** Moves the iterator to the first element, approved by the {@code suits()} 
     * method. If there is no such element, the method restored 
     * the original state of the iterator.
     *
     * @param list The iterator to move
     * @return True if the iterator gets necessary position, false otherwise
     */
    boolean moveFirst(ElementIterator list);

    /** Moves the iterator to the next element, approved by the {@code suits()} 
     * method. If there is no such element, the method restored 
     * the original state of the iterator.
     *
     * @param list The iterator to move
     * @return True if the iterator gets necessary position, false otherwise
     */
    boolean moveNext(ElementIterator list);

    /** Moves the iterator to the previous element, approved by the {@code suits()} 
     * method. If there is no such element, the method restored 
     * the original state of the iterator.
     *
     * @param list The iterator to move
     * @return True if the iterator gets necessary position, false otherwise
     */
    boolean movePrev(ElementIterator list);

    boolean moveToPos(ElementIterator list, int pos);
}
