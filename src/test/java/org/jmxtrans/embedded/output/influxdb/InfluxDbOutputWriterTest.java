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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

/**
 * Unit tests for {@link InfluxDbOutputWriter}.
 *
 * @since 3.0.0
 */
public class InfluxDbOutputWriterTest {

    private final InfluxDbOutputWriter writer = new InfluxDbOutputWriter();

    @Test
    public void shouldReturnDefaultUrlWhenNull() {
        assertEquals("http://127.0.0.1:8086", writer.getUrl(null));
    }

    @Test
    public void shouldReturnConfiguredUrlWhenNonNull() {
        assertEquals("http://custom:9090", writer.getUrl("http://custom:9090"));
    }

    @Test
    public void shouldReturnDefaultDatabaseWhenNull() {
        assertEquals("Metrics_127.0.0.1", writer.getDatabase(null));
    }

    @Test
    public void shouldReturnConfiguredDatabaseWhenNonNull() {
        assertEquals("mydb", writer.getDatabase("mydb"));
    }

    @Test
    public void shouldPassThroughUser() {
        assertNull(writer.getUser(null));
        assertEquals("admin", writer.getUser("admin"));
    }

    @Test
    public void shouldPassThroughPassword() {
        assertNull(writer.getPassword(null));
        assertEquals("secret", writer.getPassword("secret"));
    }

    @Test
    public void shouldHaveDefaultConstructor() {
        InfluxDbOutputWriter w = new InfluxDbOutputWriter();
        assertNotNull(w);
    }

    @Test
    public void shouldExposeSettingEnabledConstant() {
        assertEquals("enabled", InfluxDbOutputWriter.SETTING_ENABLED);
    }
}
