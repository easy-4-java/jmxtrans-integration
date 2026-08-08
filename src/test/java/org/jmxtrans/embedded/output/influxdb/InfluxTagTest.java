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

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Unit tests for {@link InfluxTag}.
 *
 * @since 3.0.0
 */
public class InfluxTagTest {

    /**
     * Plain-name/value constructor stores every argument unchanged.
     */
    @Test
    public void shouldExposeConstructorArguments() {
        InfluxTag tag = new InfluxTag("region", "eu-west-1");
        assertEquals("region", tag.getName());
        assertEquals("eu-west-1", tag.getValue());
    }

    /**
     * {@link InfluxTag#toInfluxFormat()} concatenates {@code name=value}.
     */
    @Test
    public void shouldRenderInfluxFormat() {
        InfluxTag tag = new InfluxTag("host", "server-01");
        assertEquals("host=server-01", tag.toInfluxFormat());
    }

    /**
     * {@link InfluxTag#toString()} shares the textual format with
     * {@link InfluxTag#toInfluxFormat()}.
     */
    @Test
    public void shouldRenderToStringSameAsInfluxFormat() {
        InfluxTag tag = new InfluxTag("env", "prod");
        assertEquals(tag.toInfluxFormat(), tag.toString());
    }

    /**
     * The {@link InfluxTag#equals(Object)} contract is reflexive,
     * symmetric and respects both fields.
     */
    @Test
    public void shouldBeEqualAndConsistentOnHashCode() {
        InfluxTag a = new InfluxTag("k", "v");
        InfluxTag b = new InfluxTag("k", "v");
        InfluxTag c = new InfluxTag("k", "v");
        assertEquals(a, b);
        assertEquals(b, c);
        assertEquals(a.hashCode(), b.hashCode());
        assertEquals(a, a);
    }

    /**
     * Different names or values must break equality.
     */
    @Test
    public void shouldNotBeEqualWhenNameOrValueDiffers() {
        InfluxTag base = new InfluxTag("k", "v");
        assertNotEquals(base, new InfluxTag("K", "v"));
        assertNotEquals(base, new InfluxTag("k", "V"));
    }

    /**
     * Equality must hold across the FQN/type boundary only when the other
     * object is also an {@link InfluxTag}.
     */
    @Test
    public void shouldNotBeEqualToNullOrOtherType() {
        InfluxTag tag = new InfluxTag("k", "v");
        assertNotEquals(tag, null);
        assertNotEquals(tag, "k=v");
    }

    /**
     * Constructors must reject {@code null} arguments because the
     * surrounding code relies on a non-null {@code name} / {@code value}.
     */
    @Test
    public void shouldRejectNullNameAndValue() {
        try {
            new InfluxTag(null, "v");
            fail("expected NullPointerException on null name");
        } catch (NullPointerException ignored) {
            // expected
        }
        try {
            new InfluxTag("k", null);
            fail("expected NullPointerException on null value");
        } catch (NullPointerException ignored) {
            // expected
        }
    }

    /**
     * Any object handed to {@link InfluxTag#equals(Object)} that is not an
     * {@code InfluxTag} instance should be reported as unequal.
     */
    @Test
    public void shouldAcceptAnyTypeInEqualsSafely() {
        InfluxTag tag = new InfluxTag("k", "v");
        Object differentClass = new Object() {
            @Override
            public boolean equals(Object obj) {
                return false;
            }
        };
        assertNotEquals(tag, differentClass);
        assertNotNull(tag);
        assertTrue(tag.equals(new InfluxTag("k", "v")));
    }
}
