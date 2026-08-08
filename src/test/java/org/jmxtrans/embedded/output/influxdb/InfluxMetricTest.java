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
package org.jmxtrans.embedded.output.influxdb;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Unit tests for {@link InfluxMetric}.
 *
 * @since 3.0.0
 */
public class InfluxMetricTest {

    @Test
    public void shouldExposeConstructorArguments() {
        List<InfluxTag> tags = Arrays.asList(new InfluxTag("host", "s1"));
        InfluxMetric metric = new InfluxMetric("cpu", tags, 42, 1700000000000L);

        assertEquals("cpu", metric.getMeasurement());
        assertEquals(tags, metric.getTags());
        assertEquals(1700000000000L, metric.getTimestampMillis());
    }

    @Test(expected = NullPointerException.class)
    public void shouldRejectNullMeasurement() {
        new InfluxMetric(null, new ArrayList<>(), 1, 0L);
    }

    @Test(expected = NullPointerException.class)
    public void shouldRejectNullTags() {
        new InfluxMetric("m", null, 1, 0L);
    }

    @Test(expected = NullPointerException.class)
    public void shouldRejectNullValue() {
        new InfluxMetric("m", new ArrayList<>(), null, 0L);
    }

    @Test
    public void shouldRenderIntegerWithSuffixI() {
        InfluxMetric metric = new InfluxMetric("cpu",
                Collections.<InfluxTag>emptyList(), 42, 1000L);
        String line = metric.toInfluxFormat();
        assertTrue("Integer value should end with 'i': " + line,
                line.contains("value=42i"));
    }

    @Test
    public void shouldRenderLongWithSuffixI() {
        InfluxMetric metric = new InfluxMetric("mem",
                Collections.<InfluxTag>emptyList(), 123456789L, 1000L);
        String line = metric.toInfluxFormat();
        assertTrue("Long value should end with 'i': " + line,
                line.contains("value=123456789i"));
    }

    @Test
    public void shouldRenderDoubleWithoutSuffixI() {
        InfluxMetric metric = new InfluxMetric("temp",
                Collections.<InfluxTag>emptyList(), 3.14, 1000L);
        String line = metric.toInfluxFormat();
        assertTrue("Double value must not end with 'i': " + line,
                line.contains("value=3.14"));
        assertTrue(line.contains("temp "));
    }

    @Test
    public void shouldRenderFloatWithoutSuffixI() {
        InfluxMetric metric = new InfluxMetric("temp",
                Collections.<InfluxTag>emptyList(), 2.5f, 1000L);
        String line = metric.toInfluxFormat();
        assertTrue("Float value must not end with 'i': " + line,
                line.contains("value=2.5"));
    }

    @Test
    public void shouldRenderBigDecimal() {
        InfluxMetric metric = new InfluxMetric("price",
                Collections.<InfluxTag>emptyList(), new BigDecimal("99.99"), 1000L);
        String line = metric.toInfluxFormat();
        assertTrue("BigDecimal value should be rendered: " + line,
                line.contains("value=99.99"));
    }

    @Test
    public void shouldRenderStringValues() {
        InfluxMetric metric = new InfluxMetric("status",
                Collections.<InfluxTag>emptyList(), "ok", 1000L);
        String line = metric.toInfluxFormat();
        assertTrue("String value should be rendered: " + line,
                line.contains("value=ok"));
    }

    @Test
    public void shouldIncludeTagsInInfluxFormat() {
        List<InfluxTag> tags = Arrays.asList(
                new InfluxTag("host", "s1"),
                new InfluxTag("region", "us")
        );
        InfluxMetric metric = new InfluxMetric("cpu", tags, 10, 1000L);
        String line = metric.toInfluxFormat();
        assertTrue("Should contain tags: " + line, line.contains("host=s1"));
        assertTrue("Should contain region tag: " + line, line.contains("region=us"));
    }

    @Test
    public void shouldIncludeTimestampAtEnd() {
        InfluxMetric metric = new InfluxMetric("cpu",
                Collections.<InfluxTag>emptyList(), 1, 1700000000000L);
        String line = metric.toInfluxFormat();
        assertTrue("Should end with timestamp: " + line,
                line.endsWith("1700000000000"));
    }

    @Test
    public void shouldRenderEmptyTagsWithoutComma() {
        InfluxMetric metric = new InfluxMetric("cpu",
                Collections.<InfluxTag>emptyList(), 1, 1000L);
        String line = metric.toInfluxFormat();
        assertEquals(-1, line.indexOf(','));
    }

    @Test
    public void shouldBeEqualForSameData() {
        List<InfluxTag> tags = Arrays.asList(new InfluxTag("k", "v"));
        InfluxMetric a = new InfluxMetric("m", tags, 42, 100L);
        InfluxMetric b = new InfluxMetric("m", tags, 42, 100L);
        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
    }

    @Test
    public void shouldNotBeEqualForDifferentMeasurement() {
        List<InfluxTag> tags = Arrays.asList(new InfluxTag("k", "v"));
        InfluxMetric a = new InfluxMetric("m1", tags, 42, 100L);
        InfluxMetric b = new InfluxMetric("m2", tags, 42, 100L);
        assertNotEquals(a, b);
    }

    @Test
    public void shouldNotBeEqualForDifferentTimestamp() {
        List<InfluxTag> tags = Arrays.asList(new InfluxTag("k", "v"));
        InfluxMetric a = new InfluxMetric("m", tags, 42, 100L);
        InfluxMetric b = new InfluxMetric("m", tags, 42, 200L);
        assertNotEquals(a, b);
    }

    @Test
    public void shouldNotBeEqualToNullOrOtherType() {
        InfluxMetric m = new InfluxMetric("m",
                Collections.<InfluxTag>emptyList(), 1, 0L);
        assertNotEquals(m, null);
        assertNotEquals(m, "not a metric");
    }

    @Test
    public void shouldBeReflexive() {
        InfluxMetric m = new InfluxMetric("m",
                Collections.<InfluxTag>emptyList(), 1, 0L);
        assertEquals(m, m);
    }

    @Test
    public void shouldProduceMeaningfulToString() {
        InfluxMetric m = new InfluxMetric("cpu",
                Collections.<InfluxTag>emptyList(), 99, 500L);
        String s = m.toString();
        assertTrue(s.contains("InfluxMetric"));
        assertTrue(s.contains("cpu"));
        assertTrue(s.contains("500"));
    }

    @Test
    public void shouldGetValueAsStringForInteger() {
        InfluxMetric m = new InfluxMetric("m",
                Collections.<InfluxTag>emptyList(), 10, 0L);
        assertEquals("10i", m.getValue());
    }

    @Test
    public void shouldGetValueAsStringForString() {
        InfluxMetric m = new InfluxMetric("m",
                Collections.<InfluxTag>emptyList(), "hello", 0L);
        assertEquals("hello", m.getValue());
    }
}
