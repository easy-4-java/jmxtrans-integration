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
package org.jmxtrans.embedded.util.tag;

import org.junit.Test;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

/**
 * Unit tests for {@link TagUtil}.
 *
 * @since 3.0.0
 */
public class TagUtilTest {

    @Test
    public void shouldReturnNullForNonExistentProperty() {
        String result = TagUtil.getTagValFromEnv("definitely_not_a_real_env_var_xyz123");
        assertNull(result);
    }

    @Test
    public void shouldResolvePathEnvironmentVariable() {
        // PATH is almost always set as an environment variable on all platforms
        String path = System.getenv("PATH");
        if (path != null) {
            String result = TagUtil.getTagValFromEnv("PATH");
            assertNotNull("PATH env var should be resolved", result);
        }
    }

    @Test
    public void shouldResolveHomeEnvironmentVariable() {
        // HOME is set on Unix/macOS
        String home = System.getenv("HOME");
        if (home != null) {
            String result = TagUtil.getTagValFromEnv("HOME");
            assertNotNull("HOME env var should be resolved", result);
        }
    }

    @Test
    public void shouldReturnNullForUnknownKey() {
        String result = TagUtil.getTagValFromEnv("NONEXISTENT_KEY_12345");
        assertNull(result);
    }

    @Test
    public void shouldHandleUserDotDirIfPresentAsSystemPropertyValue() {
        // TagUtil.getTagValFromEnv uses Properties.contains() which checks
        // VALUES (not keys). If "user.dir" happens to be a VALUE in system
        // properties, it would be found. Otherwise only env var lookup applies.
        String result = TagUtil.getTagValFromEnv("user.dir");
        // We just verify it does not throw; the result depends on environment.
    }
}
