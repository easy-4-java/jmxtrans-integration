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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.jmxtrans.embedded.ResultNameStrategy;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Unit tests for {@link InfluxMetricConverter}.
 *
 * @since 3.0.0
 */
public class InfluxMetricConverterTest {

    private final ResultNameStrategy strategy = new ResultNameStrategy();

    @Test
    public void shouldConvertSimpleMetricNameWithoutTags() {
        InfluxMetric metric = InfluxMetricConverter.convertToInfluxMetric(
                strategy, "jvm.gc.count", 100,
                Collections.<InfluxTag>emptyList(), 1700000000000L);

        assertEquals("jvm.gc.count", metric.getMeasurement());
        assertTrue(metric.getTags().isEmpty());
        assertEquals(1700000000000L, metric.getTimestampMillis());
    }

    @Test
    public void shouldParseEmbeddedTagsFromMetricName() {
        InfluxMetric metric = InfluxMetricConverter.convertToInfluxMetric(
                strategy, "cpu.usage,host=s1,region=us", 50,
                Collections.<InfluxTag>emptyList(), 1000L);

        assertEquals("cpu.usage", metric.getMeasurement());
        assertEquals(2, metric.getTags().size());
        assertEquals("host", metric.getTags().get(0).getName());
        assertEquals("s1", metric.getTags().get(0).getValue());
        assertEquals("region", metric.getTags().get(1).getName());
        assertEquals("us", metric.getTags().get(1).getValue());
    }

    @Test
    public void shouldMergeAdditionalTagsWithParsedTags() {
        List<InfluxTag> additional = new ArrayList<InfluxTag>();
        additional.add(new InfluxTag("env", "prod"));

        InfluxMetric metric = InfluxMetricConverter.convertToInfluxMetric(
                strategy, "mem.used,host=s1", 2048,
                additional, 1000L);

        assertEquals(2, metric.getTags().size());
        assertEquals("env", metric.getTags().get(0).getName());
        assertEquals("host", metric.getTags().get(1).getName());
    }

    @Test
    public void shouldParseCommaSeparatedTagsFromString() {
        List<InfluxTag> tags = InfluxMetricConverter.tagsFromCommaSeparatedString(
                strategy, "region=eu,zone=a");
        assertEquals(2, tags.size());
        assertEquals("region", tags.get(0).getName());
        assertEquals("eu", tags.get(0).getValue());
        assertEquals("zone", tags.get(1).getName());
        assertEquals("a", tags.get(1).getValue());
    }

    @Test
    public void shouldReturnEmptyListForBlankString() {
        List<InfluxTag> tags = InfluxMetricConverter.tagsFromCommaSeparatedString(
                strategy, "   ");
        assertNotNull(tags);
        assertTrue(tags.isEmpty());
    }

    @Test
    public void shouldReturnEmptyListForEmptyString() {
        List<InfluxTag> tags = InfluxMetricConverter.tagsFromCommaSeparatedString(
                strategy, "");
        assertNotNull(tags);
        assertTrue(tags.isEmpty());
    }

    @Test(expected = InfluxMetricConverter.FailedToConvertToInfluxMetricException.class)
    public void shouldThrowOnMalformedTagSegment() {
        InfluxMetricConverter.tagsFromCommaSeparatedString(strategy, "badformat");
    }

    @Test(expected = InfluxMetricConverter.FailedToConvertToInfluxMetricException.class)
    public void shouldThrowOnTagWithMultipleEquals() {
        InfluxMetricConverter.tagsFromCommaSeparatedString(strategy, "a=b=c");
    }

    @Test
    public void shouldHandleMetricNameWithWhitespace() {
        InfluxMetric metric = InfluxMetricConverter.convertToInfluxMetric(
                strategy, " disk.io ,host= s2 ", 512,
                Collections.<InfluxTag>emptyList(), 1000L);

        assertEquals("disk.io", metric.getMeasurement());
    }

    @Test
    public void shouldParseSingleTagFromMetricName() {
        InfluxMetric metric = InfluxMetricConverter.convertToInfluxMetric(
                strategy, "net.rx,iface=eth0", 1024,
                Collections.<InfluxTag>emptyList(), 1000L);

        assertEquals("net.rx", metric.getMeasurement());
        assertEquals(1, metric.getTags().size());
        assertEquals("iface", metric.getTags().get(0).getName());
    }

    @Test
    public void shouldConstructFailedToConvertExceptionWithMessage() {
        InfluxMetricConverter.FailedToConvertToInfluxMetricException ex =
                new InfluxMetricConverter.FailedToConvertToInfluxMetricException("bad tag");
        assertEquals("bad tag", ex.getMessage());
    }

    @Test
    public void shouldPreserveTagOrder() {
        List<InfluxTag> tags = InfluxMetricConverter.tagsFromCommaSeparatedString(
                strategy, "z=last,a=first,m=middle");
        assertEquals("z", tags.get(0).getName());
        assertEquals("a", tags.get(1).getName());
        assertEquals("m", tags.get(2).getName());
    }
}
