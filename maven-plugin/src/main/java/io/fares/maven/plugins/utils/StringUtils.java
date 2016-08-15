/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package io.fares.maven.plugins.utils;

import java.util.ArrayList;
import java.util.List;

public class StringUtils {


    /**
     * The empty String {@code ""}.
     */
    public static final String EMPTY = "";

    private static String[] EMPTY_STRING_ARRAY = new String[0];

    /**
     * Checks if a (trimmed) String is <code>null</code> or empty.
     *
     * @param string the String to check
     * @return <code>true</code> if the string is <code>null</code>, or length
     * zero once trimmed.
     */
    public static boolean isEmpty(String string) {
        return (string == null || string.trim().length() == 0);
    }

    public static String escapeSpace(String url) {
        // URLEncoder didn't work.
        StringBuffer buf = new StringBuffer();
        for (int i = 0; i < url.length(); i++) {
            // TODO: not sure if this is the only character that needs to be
            // escaped.
            if (url.charAt(i) == ' ')
                buf.append("%20");
            else
                buf.append(url.charAt(i));
        }
        return buf.toString();
    }

    public static String[] split(String str, char separatorChar,
                                 boolean preserveAllTokens) {
        // Performance tuned for 2.0 (JDK1.4)

        if (str == null) {
            return null;
        }
        int len = str.length();
        if (len == 0) {
            return EMPTY_STRING_ARRAY;
        }
        List<String> list = new ArrayList<String>();
        int i = 0, start = 0;
        boolean match = false;
        boolean lastMatch = false;
        while (i < len) {
            if (str.charAt(i) == separatorChar) {
                if (match || preserveAllTokens) {
                    list.add(str.substring(start, i));
                    match = false;
                    lastMatch = true;
                }
                start = ++i;
                continue;
            }
            lastMatch = false;
            match = true;
            i++;
        }
        if (match || (preserveAllTokens && lastMatch)) {
            list.add(str.substring(start, i));
        }
        return (String[]) list.toArray(new String[list.size()]);
    }

    public static String join(final List<String> list, final char separator) {
        if (list == null) {
            return null;
        }

        if (list.size() <= 0) {
            return EMPTY;
        }

        final StringBuilder buf = new StringBuilder(list.size() * 16);

        for (int i = 0; i < list.size(); i++) {
            if (i > 0) {
                buf.append(separator);
            }
            buf.append(list.get(i));
        }
        return buf.toString();
    }


}
