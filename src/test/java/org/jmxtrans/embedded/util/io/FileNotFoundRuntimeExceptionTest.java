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
package org.jmxtrans.embedded.util.io;

import java.io.FileNotFoundException;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

/**
 * Unit tests for {@link FileNotFoundRuntimeException}.
 *
 * @since 3.0.0
 */
public class FileNotFoundRuntimeExceptionTest {

    @Test
    public void shouldCreateWithDefaultConstructor() {
        FileNotFoundRuntimeException ex = new FileNotFoundRuntimeException();
        assertNull(ex.getMessage());
        assertNull(ex.getCause());
    }

    @Test
    public void shouldCreateWithMessage() {
        FileNotFoundRuntimeException ex = new FileNotFoundRuntimeException("file missing");
        assertEquals("file missing", ex.getMessage());
    }

    @Test
    public void shouldCreateWithMessageAndCause() {
        FileNotFoundException cause = new FileNotFoundException("x");
        FileNotFoundRuntimeException ex = new FileNotFoundRuntimeException("msg", cause);
        assertEquals("msg", ex.getMessage());
        assertSame(cause, ex.getCause());
    }

    @Test
    public void shouldCreateWithCauseOnly() {
        FileNotFoundException cause = new FileNotFoundException("x");
        FileNotFoundRuntimeException ex = new FileNotFoundRuntimeException(cause);
        assertSame(cause, ex.getCause());
    }

    @Test
    public void shouldCreateWithFullConstructor() {
        FileNotFoundException cause = new FileNotFoundException("x");
        FileNotFoundRuntimeException ex = new FileNotFoundRuntimeException(
                "msg", cause, true, true);
        assertEquals("msg", ex.getMessage());
        assertSame(cause, ex.getCause());
    }

    @Test
    public void shouldBeIoRuntimeException() {
        assertTrue(new FileNotFoundRuntimeException() instanceof IoRuntimeException);
    }

    @Test
    public void shouldBeRuntimeException() {
        assertTrue(new FileNotFoundRuntimeException() instanceof RuntimeException);
    }
}
