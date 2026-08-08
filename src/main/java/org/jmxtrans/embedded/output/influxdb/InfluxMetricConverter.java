/*
 * Copyright (c) 2010-2016 the original author or authors
 *
 * Permission is hereby granted, free of charge, to any person obtaining
 * a copy of this software and associated documentation files (the
 * "Software"), to deal in the Software without restriction, including
 * without limitation the rights to use, copy, modify, merge, publish,
 * distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so, subject to
 * the following conditions:
 *
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE
 * LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION
 * OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION
 * WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 *
 */
package org.jmxtrans.embedded.output.influxdb;

import java.util.ArrayList;
import java.util.List;

import org.jmxtrans.embedded.ResultNameStrategy;
import org.jmxtrans.embedded.util.tag.TagUtil;

/**
 * Conversion utilities for transforming JmxTrans results into the
 * {@link InfluxMetric} form expected by the InfluxDB line protocol.
 *
 * <p>The conversion follows two simple rules:</p>
 * <ol>
 *   <li>The measurement name is the portion of the metric name before the
 *       first comma.</li>
 *   <li>Any {@code name=value} pair after the first comma is added as an
 *       {@link InfluxTag} to the produced metric.</li>
 * </ol>
 *
 * <p>Tag <em>values</em> are resolved first against JVM system properties and
 * OS environment variables via {@link TagUtil#getTagValFromEnv(String)}; if
 * that lookup yields {@code null}, the supplied {@link ResultNameStrategy} is
 * used to evaluate embedded expressions (e.g. {@code #hostname#}).</p>
 *
 * @author Kristoffer Erlandsson
 * @since 3.0.0
 * @see InfluxMetric
 * @see InfluxTag
 */
public class InfluxMetricConverter {

    /**
     * Converts a JmxTrans metric triple into an {@link InfluxMetric}.
     *
     * <p>The metric name may be of the form
     * {@code "measurement,tag1=val1,tag2=val2"}. Tags embedded in the name are
     * appended to {@code additionalTags} (with the configured tags coming
     * first so they can be overridden by the per-metric tags).</p>
     *
     * @param strategy        the strategy used to resolve {@code #expression#}
     *                        placeholders inside tag values; must not be
     *                        {@code null}.
     * @param metricName      either a bare measurement name such as
     *                        {@code "jvm.gc.count"} or a comma-separated
     *                        measurement + tag specification.
     * @param value           the raw metric value (numeric or string-like).
     * @param additionalTags  pre-configured tags that should be added to every
     *                        exported metric; may be empty but never
     *                        {@code null}.
     * @param timestamp       the metric timestamp expressed in milliseconds
     *                        since the Unix epoch.
     * @return a fully populated {@link InfluxMetric} ready to be serialised
     *         into InfluxDB line protocol.
     */
    public static InfluxMetric convertToInfluxMetric(ResultNameStrategy strategy, String metricName, Object value, List<InfluxTag> additionalTags, long timestamp) {
        List<InfluxTag> tagsFromMetricName = parseTags(strategy, metricName);
        List<InfluxTag> allTags = new ArrayList<InfluxTag>(additionalTags);
        allTags.addAll(tagsFromMetricName);
        return new InfluxMetric(parseMeasurement(metricName), allTags, value, timestamp);
    }

    /**
     * Extracts the measurement name from a {@code measurement[,tags]} string
     * by taking the substring up to the first comma and trimming whitespace.
     *
     * @param metricName the full metric name to inspect.
     * @return the bare measurement name, never {@code null}.
     */
    private static String parseMeasurement(String metricName) {
        return metricName.split(",")[0].trim();
    }

    /**
     * Parses the {@code tag=value} pairs that follow the first comma in
     * {@code metricName}. If no comma is present the resulting list is empty.
     *
     * @param strategy   the result-name strategy used for expression
     *                   resolution.
     * @param metricName the full metric name.
     * @return a possibly empty mutable list of parsed tags; never
     *         {@code null}.
     */
    private static List<InfluxTag> parseTags(ResultNameStrategy strategy, String metricName) {
        int startOfTags = metricName.indexOf(',');
        if (startOfTags < 0) {
            return new ArrayList<InfluxTag>();
        }
        return tagsFromCommaSeparatedString(strategy, metricName.substring(startOfTags + 1));
    }

    /**
     * Splits a comma-separated {@code tag=value} string into an ordered list
     * of {@link InfluxTag}s.
     *
     * <p>The string may contain any number of segments; an empty or
     * whitespace-only input produces an empty list. Each segment must be on
     * the {@code <name>=<value>} form; any malformed segment triggers a
     * {@link FailedToConvertToInfluxMetricException}.</p>
     *
     * @param strategy the result-name strategy used to resolve embedded
     *                 expressions inside tag values.
     * @param s        the comma-separated tag specification, must not be
     *                 {@code null}.
     * @return the parsed tags in declaration order; never {@code null}.
     * @throws FailedToConvertToInfluxMetricException when a segment is not on
     *                 the {@code <name>=<value>} form.
     */
    public static List<InfluxTag> tagsFromCommaSeparatedString(ResultNameStrategy strategy, String s) {
        List<InfluxTag> tags = new ArrayList<InfluxTag>();
        if (s.trim().isEmpty()) {
            return tags;
        }
        String[] parts = s.split(",");
        for (String tagPart : parts) {
            tags.add(parseOneTag(strategy, tagPart));
        }
        return tags;
    }

    /**
     * Parses a single {@code name=value} segment into an {@link InfluxTag},
     * resolving any {@code #expression#} placeholders against
     * {@link ResultNameStrategy#resolveExpression(String)} and falling back to
     * JVM system properties / OS environment variables via
     * {@link TagUtil#getTagValFromEnv(String)}.
     *
     * @param strategy the result-name strategy.
     * @param part     a single comma-separated segment; must not be
     *                 {@code null}.
     * @return the parsed {@link InfluxTag}; never {@code null}.
     * @throws FailedToConvertToInfluxMetricException if {@code part} is not on
     *                 the {@code <name>=<value>} form.
     */
    private static InfluxTag parseOneTag(ResultNameStrategy strategy, String part) {
        String[] nameAndValue = part.trim().split("=");
        if (nameAndValue.length != 2) {
            throw new FailedToConvertToInfluxMetricException(
                    "Error when parsing influx tags from substring " + part + ", must be on format <name>=<value>,...");
        }
        String tagName = nameAndValue[0].trim();
        String tagVal = nameAndValue[1].trim();
        //1、环境变量取值
    	String tagVal_ = TagUtil.getTagValFromEnv(tagVal);

    	//2、使用结果名策略对象获取值
    	if(tagVal_ == null){
    		tagVal_ = strategy.resolveExpression(tagVal);
    	}

        return new InfluxTag(tagName, tagVal_);
    }

    /**
     * Unchecked exception raised when a tag specification string cannot be
     * parsed as {@code <name>=<value>,<name>=<value>}.
     *
     * @since 3.0.0
     */
    @SuppressWarnings("serial")
    public static class FailedToConvertToInfluxMetricException extends RuntimeException {

        /**
         * Builds a new exception with the given message.
         *
         * @param msg human-readable description of the parsing failure; must
         *           not be {@code null}.
         */
        public FailedToConvertToInfluxMetricException(String msg) {
            super(msg);
        }

    }
}
