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
package org.jmxtrans.embedded;

import org.junit.Test;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;

/**
 * Unit tests for {@link EmbeddedJmxTransLauncher}.
 *
 * @since 3.0.0
 */
public class EmbeddedJmxTransLauncherTest {

    /**
     * A freshly constructed launcher must allow its backing
     * {@link EmbeddedJmxTrans} field to be read and written through the
     * JavaBean accessors without losing reference identity.
     */
    @Test
    public void shouldStoreAndRetrieveInjectedEmbeddedJmxTrans() {
        EmbeddedJmxTransLauncher launcher = new EmbeddedJmxTransLauncher();
        EmbeddedJmxTrans instance = new EmbeddedJmxTrans();

        launcher.setJmxtrans(instance);

        assertSame(instance, launcher.getJmxtrans());
    }

    /**
     * The default {@code EmbeddedJmxTrans} field must remain {@code null}
     * until {@link EmbeddedJmxTransLauncher#setJmxtrans(EmbeddedJmxTrans)}
     * is called by the surrounding container &mdash; only the injection
     * layer is allowed to populate the field.
     */
    @Test
    public void shouldExposeNullEmbeddedJmxTransBeforeInjection() {
        EmbeddedJmxTransLauncher launcher = new EmbeddedJmxTransLauncher();
        assertNull(launcher.getJmxtrans());
    }

    /**
     * The launcher constructor must never throw.
     */
    @Test
    public void shouldBeInstantiable() {
        EmbeddedJmxTransLauncher launcher = new EmbeddedJmxTransLauncher();
        assertNotNull(launcher);
    }
}
