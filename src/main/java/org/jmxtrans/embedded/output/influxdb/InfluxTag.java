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

import java.util.Objects;

/**
 * Immutable {@code name=value} pair used to populate the tag set of an
 * {@link InfluxMetric}.
 *
 * <p>Tags are how InfluxDB indexes series; once a series has been written its
 * tag set is fixed for the lifetime of the database. Consequently this type
 * is also immutable &mdash; both the name and the value are stored in
 * {@code final} fields so the instance can be safely shared across export
 * threads.</p>
 *
 * @author Kristoffer Erlandsson
 * @since 3.0.0
 * @see InfluxMetric
 */
public class InfluxTag {

    /** Tag name (the property name in InfluxDB). */
    private final String name;

    /** Tag value (the property value in InfluxDB). */
    private final String value;

    /**
     * Creates a new tag with the given name and value.
     *
     * @param name  the tag name; must not be {@code null}.
     * @param value the tag value; must not be {@code null}.
     * @throws NullPointerException if either {@code name} or {@code value} is
     *                              {@code null}.
     */
    public InfluxTag(String name, String value) {
        this.name = Objects.requireNonNull(name);
        this.value = Objects.requireNonNull(value);
    }

    /**
     * Returns the tag name.
     *
     * @return the tag name, never {@code null}.
     */
    public String getName() {
        return name;
    }

    /**
     * Returns the tag value.
     *
     * @return the tag value, never {@code null}.
     */
    public String getValue() {
        return value;
    }

    /**
     * Returns the InfluxDB line-protocol representation of this tag.
     *
     * @return the {@code name=value} string; never {@code null}.
     */
    public String toInfluxFormat() {
        return name + "=" + value;
    }

    @Override
    public String toString() {
        return name + "=" + value;
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, value);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        InfluxTag other = (InfluxTag) obj;
        return Objects.equals(name, other.name)
                && Objects.equals(value, other.value);
    }

}
