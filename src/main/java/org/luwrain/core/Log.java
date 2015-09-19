/*
   Copyright 2012-2015 Michael Pozhidaev <msp@altlinux.org>

   This file is part of the Luwrain.

   Luwrain is free software; you can redistribute it and/or
   modify it under the terms of the GNU General Public
   License as published by the Free Software Foundation; either
   version 3 of the License, or (at your option) any later version.

   Luwrain is distributed in the hope that it will be useful,
   but WITHOUT ANY WARRANTY; without even the implied warranty of
   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
   General Public License for more details.
*/

package org.luwrain.core;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

//TODO:Thread safety;

public class Log
{
    public static void debug(String component, String message)
    {
	if (component == null || message == null)
	    return;
	if (component.isEmpty())
	    System.out.println(timestamp() + message); else
	    System.out.println(timestamp() + component + ":" + message);
    }

    public static void info(String component, String message)
    {
	if (component == null || message == null)
	    return;
	if (component.isEmpty())
	    System.out.println(timestamp() + message); else
	    System.out.println(timestamp() + component + ":" + message);
    }

    public static void warning(String component, String message)
    {
	if (component == null || message == null)
	    return;
	if (component.isEmpty())
	    System.out.println("WARNING:" + timestamp() + message); else
	    System.out.println("WARNING:" + timestamp() + component + ":" + message);
    }

    public static void error(String component, String message)
    {
	if (component == null || message == null)
	    return;
	if (component.isEmpty())
	    System.out.println("ERROR:" + timestamp() + message); else
	    System.out.println("ERROR:" + timestamp() + component + ":" + message);
    }

    public static void fatal(String component, String message)
    {
	if (component == null || message == null)
	    return;
	if (component.isEmpty())
	    System.out.println("FATAL:" + timestamp() + message); else
	    System.out.println("FATAL:" + timestamp() + component + ":" + message);
    }

    private static String timestamp()
    {
    	final java.util.Date now=new java.util.Date();
    	SimpleDateFormat fmt=new SimpleDateFormat("d-M-Y H:m:s.SSS ");
    	return fmt.format(now);
    }

}