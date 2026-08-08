/*
 * Copyright (c) 2018-present, easy-4-java (https://github.com/easy-4-java).
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jmxtrans.embedded.util.time;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Unit tests for {@link SystemClock}.
 *
 * @since 3.0.0
 */
public class SystemClockTest {

    @Test
    public void shouldReturnNonNegativeTimestamp() {
        long now = SystemClock.now();
        assertTrue("Clock should return a positive timestamp", now > 0);
    }

    @Test
    public void shouldReturnTimestampCloseToSystemCurrentTimeMillis() {
        long clock = SystemClock.now();
        long system = System.currentTimeMillis();
        long diff = Math.abs(clock - system);
        assertTrue("Clock should be within 100ms of System.currentTimeMillis(), diff=" + diff,
                diff < 100);
    }

    @Test
    public void shouldReturnMonotonicallyNonDecreasingValues() throws Exception {
        long first = SystemClock.now();
        Thread.sleep(10);
        long second = SystemClock.now();
        assertTrue("Second call should be >= first", second >= first);
    }

    @Test
    public void shouldReturnNonNullDateString() {
        String date = SystemClock.nowDate();
        assertNotNull(date);
        assertTrue("Date string should not be empty", date.length() > 0);
    }

    @Test
    public void shouldReturnDateStringContainingCurrentYear() {
        String date = SystemClock.nowDate();
        int currentYear = java.util.Calendar.getInstance().get(java.util.Calendar.YEAR);
        assertTrue("Date should contain current year: " + date,
                date.contains(String.valueOf(currentYear)));
    }

    @Test
    public void shouldReturnConsistentTimestampAcrossCalls() {
        long t1 = SystemClock.now();
        long t2 = SystemClock.now();
        assertEquals("Rapid successive calls should return same cached value", t1, t2);
    }
}
