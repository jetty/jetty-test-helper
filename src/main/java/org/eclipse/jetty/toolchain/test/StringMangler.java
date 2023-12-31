//
// ========================================================================
// Copyright (c) 1995 Mort Bay Consulting Pty Ltd and others.
//
// This program and the accompanying materials are made available under the
// terms of the Eclipse Public License v. 2.0 which is available at
// https://www.eclipse.org/legal/epl-2.0, or the Apache License, Version 2.0
// which is available at https://www.apache.org/licenses/LICENSE-2.0.
//
// SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
// ========================================================================
//

package org.eclipse.jetty.toolchain.test;

/**
 * Collection of utility methods for manipulating Strings for zen purposes.
 */
public final class StringMangler
{
    /**
     * Condenses a classname by stripping down the package name to just the first character of each package name
     * segment.
     *
     * <pre>
     * Examples:
     * "org.eclipse.jetty.test.FooTest"           = "oejt.FooTest"
     * "org.eclipse.jetty.server.logging.LogTest" = "oejsl.LogTest"
     * </pre>
     *
     * @param classname the fully qualified class name
     * @return the condensed name
     */
    public static String condensePackageString(String classname)
    {
        String parts[] = classname.split("\\.");
        StringBuilder dense = new StringBuilder();
        for (int i = 0; i < (parts.length - 1); i++)
        {
            dense.append(parts[i].charAt(0));
        }
        dense.append('.').append(parts[parts.length - 1]);
        return dense.toString();
    }

    /**
     * Smash a long string to fit within the max string length, by taking the middle section of the string and replacing them with an ellipsis "..."
     *
     * <pre>
     * Examples:
     * .maxStringLength( 9, "Eatagramovabits") == "Eat...its"
     * .maxStringLength(10, "Eatagramovabits") == "Eat...bits"
     * .maxStringLength(11, "Eatagramovabits") == "Eata...bits"
     * </pre>
     *
     * @param max the maximum size of the string (minimum size supported is 9)
     * @param raw the raw string to smash
     * @return the ellipsis'd version of the string.
     */
    public static String maxStringLength(int max, String raw)
    {
        int length = raw.length();
        if (length <= max)
        {
            // already short enough
            return raw;
        }

        if (max < 9)
        {
            // minimum supported
            return raw.substring(0, max);
        }

        StringBuilder ret = new StringBuilder();
        int startLen = (int)Math.round((double)max / (double)3);
        ret.append(raw.substring(0, startLen));
        ret.append("...");
        ret.append(raw.substring(length - (max - startLen - 3)));

        return ret.toString();
    }

    /**
     * Clean the input string of control characters that can
     * impact the output to the logs in harmful ways.
     *
     * @param string input string
     * @return clean form of input string
     */
    public static CharSequence escapeJava(String string)
    {
        StringBuilder ret = new StringBuilder(string.length());
        for (char c : string.toCharArray())
        {
            if ((c <= 0x1F) || (c == 0x7F))
            {
                switch (c)
                {
                    case '\r':
                        ret.append("\\r");
                        break;
                    case '\n':
                        ret.append("\\n");
                        break;
                    case '\t':
                        ret.append("\\t");
                        break;
                    default:
                        ret.append("\\u00").append(String.format("%02x", (byte)c));
                        break;
                }
            }
            else
            {
                ret.append(c);
            }
        }
        return ret;
    }
}
