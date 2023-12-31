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

package org.eclipse.jetty.toolchain.test.matchers;

import java.util.ArrayList;
import java.util.List;

import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.hamcrest.SelfDescribing;

/**
 * @param <T> the type
 */
public class IsOrderedCollectionContaining<T> extends BaseMatcher<List<? super T>>
{
    private class MismatchDescription implements SelfDescribing
    {
        private final String id;
        private final List<T> entries;

        public MismatchDescription(String id, List<T> entries)
        {
            this.id = id;
            this.entries = entries;
        }

        @Override
        public void describeTo(Description description)
        {
            description.appendText(String.format("%s Entries (size: %d)", id, entries.size()));
            for (int i = 0; i < entries.size(); i++)
            {
                Object actualObj = entries.get(i);
                char indicator = badEntries.contains(i) ? '>' : ' ';
                description.appendText(String.format("%n%s[%d] %s", indicator, i, actualObj == null ? "<null>" : actualObj.toString()));
            }
        }
    }

    private final List<T> expectedList;
    private String failureReason;
    private List<Integer> badEntries = new ArrayList<>();
    private IsOrderedCollectionContaining<T>.MismatchDescription actualFailureDescription;
    private IsOrderedCollectionContaining<T>.MismatchDescription expectedFailureDescription;

    /**
     * @param expectedList the expected list
     */
    public IsOrderedCollectionContaining(List<T> expectedList)
    {
        this.expectedList = expectedList;
    }

    @Override
    public boolean matches(Object collection)
    {
        if (collection == null)
        {
            failureReason = "is <null>";
            return false;
        }

        if (!(collection instanceof List))
        {
            failureReason = "is not an instance of " + List.class.getName();
            return false;
        }

        @SuppressWarnings("unchecked")
        List<T> actualList = (List<T>)collection;

        // same size?
        boolean sizeMismatch = expectedList.size() != actualList.size();

        // test content
        this.badEntries = new ArrayList<>();
        int min = Math.min(expectedList.size(), actualList.size());
        int max = Math.max(expectedList.size(), actualList.size());
        for (int i = 0; i < min; i++)
        {
            if (!expectedList.get(i).equals(actualList.get(i)))
            {
                badEntries.add(i);
            }
        }
        for (int i = min; i < max; i++)
        {
            badEntries.add(i);
        }

        if (sizeMismatch || badEntries.size() > 0)
        {
            // build up detailed error message

            // The core failure reason
            if (sizeMismatch)
            {
                this.failureReason = String.format("size mismatch: expected <%d> entries, but got <%d> entries instead", expectedList.size(), actualList.size());
            }
            else if (badEntries.size() > 0)
            {
                this.failureReason = String.format("<%d> entr%s not matched", badEntries.size(), badEntries.size() > 1 ? "ies" : "y");
                this.actualFailureDescription = new MismatchDescription("Actual", actualList);
                this.expectedFailureDescription = new MismatchDescription("Expected", expectedList);
            }

            return false;
        }

        return true;
    }

    // Describe Expectation (reason or entries)
    @Override
    public void describeTo(Description description)
    {
        if (this.expectedFailureDescription != null)
        {
            description.appendDescriptionOf(this.expectedFailureDescription);
        }
    }

    // Describe Actual (entries)
    @Override
    public void describeMismatch(Object item, Description description)
    {
        description.appendText(this.failureReason);
        if (this.actualFailureDescription != null)
        {
            description.appendText(System.lineSeparator());
            description.appendDescriptionOf(this.actualFailureDescription);
        }
    }
}
