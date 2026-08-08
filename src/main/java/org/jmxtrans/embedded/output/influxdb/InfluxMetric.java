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

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

import org.jmxtrans.embedded.util.StringUtils2;

/**
 * Immutable representation of a single measurement that is ready to be
 * serialised into InfluxDB line protocol.
 *
 * <p>A metric is composed of:</p>
 * <ul>
 *   <li>a measurement name &mdash; the portion before the first comma in
 *       the metric name,</li>
 *   <li>an ordered list of tags,</li>
 *   <li>an opaque value (numeric, {@link BigDecimal}, or {@link String}),</li>
 *   <li>and a millisecond-precision timestamp.</li>
 * </ul>
 *
 * <p>The class is thread-safe: all fields are {@code final}. The
 * {@link #toInfluxFormat()} method performs no mutation either, so it may be
 * invoked concurrently from the InfluxDB export worker.</p>
 *
 * @author Kristoffer Erlandsson
 * @since 3.0.0
 * @see InfluxTag
 * @see <a href="https://docs.influxdata.com/influxdb/v1.8/write_protocols/line_protocol_tutorial/">InfluxDB line protocol</a>
 */
public class InfluxMetric {

    /**
     * InfluxDB field name used for the value component of the line protocol
     * representation produced by this class.
     */
    private static final String FIELD_NAME = "value";

    /*
     * Number format shared by all InfluxMetric instances. The settings come
     * from influxdb-java's Point implementation to ensure identical textual
     * output.
     *
     * See https://github.com/influxdata/influxdb-java/blob/influxdb-java-2.5/src/main/java/org/influxdb/dto/Point.java#L321
     */
    protected final static NumberFormat NUMBER_FORMAT;
    static {
        NUMBER_FORMAT = NumberFormat.getInstance(Locale.ENGLISH);
        NUMBER_FORMAT.setMaximumFractionDigits(340);
        NUMBER_FORMAT.setGroupingUsed(false);
        NUMBER_FORMAT.setMinimumFractionDigits(1);
    }

    /** Measurement timestamp in milliseconds since the Unix epoch. */
    private final long timestampMillis;

    /** Tag list applied to the produced metric, never {@code null}. */
    private final List<InfluxTag> tags;

    /** Measurement name (the "table" name in InfluxDB terms). */
    private final String measurement;

    /** Raw metric value, rendered through {@link #valueAsStr()} at output time. */
    private final Object value;

    /**
     * Creates a new {@link InfluxMetric}.
     *
     * @param measurement    the measurement name; must not be {@code null}.
     * @param tags           the tags to attach; must not be {@code null}.
     * @param value          the metric value; must not be {@code null}.
     *                       Either a numeric type, a {@link BigDecimal} or a
     *                       value whose {@code toString()} is acceptable as
     *                       the InfluxDB field value.
     * @param timestampMillis the measurement timestamp in milliseconds since
     *                       the Unix epoch.
     * @throws NullPointerException if any of {@code measurement}, {@code tags}
     *                       or {@code value} is {@code null}.
     */
    public InfluxMetric(String measurement, List<InfluxTag> tags, Object value, long timestampMillis) {
        this.measurement = Objects.requireNonNull(measurement);
        this.tags = Objects.requireNonNull(tags);
        this.value = Objects.requireNonNull(value);
        this.timestampMillis = timestampMillis;
    }

    /**
     * Returns the measurement timestamp in milliseconds since the Unix
     * epoch.
     *
     * @return the timestamp in milliseconds.
     */
    public long getTimestampMillis() {
        return timestampMillis;
    }

    /**
     * Returns the tags attached to this metric, in declaration order.
     *
     * @return an immutable view of the tag list.
     */
    public List<InfluxTag> getTags() {
        return tags;
    }

    /**
     * Returns the measurement name ("table name" in InfluxDB terms).
     *
     * @return the measurement name, never {@code null}.
     */
    public String getMeasurement() {
        return measurement;
    }

    /**
     * Returns the metric value as it should appear in InfluxDB line protocol.
     *
     * <p>{@link Integer} and {@link Long} values are suffixed with the InfluxDB
     * {@code i} integer qualifier; {@link Float}, {@link Double} and
     * {@link BigDecimal} values go through {@link #NUMBER_FORMAT}; everything
     * else is rendered via {@link Object#toString()}.</p>
     *
     * @return the value rendered as a {@link String}.
     */
    public Object getValue() {
        return valueAsStr();
    }

    /**
     * Serialises the metric into InfluxDB line protocol, for example:
     * <pre>
     * measurement,tag1=v1,tag2=v2 value=42 1700000000000
     * </pre>
     *
     * @return the textual line-protocol representation; never {@code null}.
     */
    public String toInfluxFormat() {
        StringBuilder sb = new StringBuilder();
        sb.append(measurement);
        if (!tags.isEmpty()) {
            sb.append(",");
        }
        sb.append(StringUtils2.join(convertTagsToStrings(), ","))
                .append(" ")
                .append(FIELD_NAME)
                .append("=")
                .append(valueAsStr())
                .append(" ")
                .append(timestampMillis);
        return sb.toString();
    }

    /**
     * Renders {@link #value} as a textual field value suitable for the Influx
     * line protocol.
     *
     * @return the textual representation; never {@code null}.
     */
    private String valueAsStr() {
        if (value instanceof Integer || value instanceof Long) {
            return value.toString() + "i";
        }
        if (value instanceof Float || value instanceof Double || value instanceof BigDecimal) {
            synchronized (NUMBER_FORMAT) {
                return NUMBER_FORMAT.format(value);
            }
        }
        return value.toString();
    }

    /**
     * Renders every tag through {@link InfluxTag#toInfluxFormat()}.
     *
     * @return a mutable list of tag strings in the same order as
     *         {@link #tags}; never {@code null}.
     */
    private List<String> convertTagsToStrings() {
        List<String> l = new ArrayList<String>(tags.size());
        for (InfluxTag influxTag : tags) {
            l.add(influxTag.toInfluxFormat());
        }
        return l;
    }

    @Override
    public int hashCode() {
        return Objects.hash(timestampMillis, tags, measurement, valueAsStr());
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        InfluxMetric other = (InfluxMetric) obj;
        return Objects.equals(timestampMillis, other.timestampMillis)
                && Objects.equals(tags, other.tags)
                && Objects.equals(measurement, other.measurement)
                && Objects.equals(valueAsStr(), other.valueAsStr());
    }

    @Override
    public String toString() {
        return "InfluxMetric [timestampMillis=" + timestampMillis + ", tags=" + tags + ", measurement=" + measurement
                + ", value=" + value + "]";
    }

}
