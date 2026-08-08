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
import java.io.IOException;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

/**
 * Unit tests for {@link IoRuntimeException}.
 *
 * @since 3.0.0
 */
public class IoRuntimeExceptionTest {

    @Test
    public void shouldCreateWithDefaultConstructor() {
        IoRuntimeException ex = new IoRuntimeException();
        assertNull(ex.getMessage());
        assertNull(ex.getCause());
    }

    @Test
    public void shouldCreateWithMessage() {
        IoRuntimeException ex = new IoRuntimeException("test error");
        assertEquals("test error", ex.getMessage());
    }

    @Test
    public void shouldCreateWithMessageAndCause() {
        IOException cause = new IOException("root");
        IoRuntimeException ex = new IoRuntimeException("wrapper", cause);
        assertEquals("wrapper", ex.getMessage());
        assertSame(cause, ex.getCause());
    }

    @Test
    public void shouldCreateWithCauseOnly() {
        IOException cause = new IOException("root");
        IoRuntimeException ex = new IoRuntimeException(cause);
        assertSame(cause, ex.getCause());
    }

    @Test
    public void shouldBeRuntimeException() {
        assertTrue(new IoRuntimeException() instanceof RuntimeException);
    }

    @Test
    public void shouldPropagateGenericIOException() {
        IOException ioEx = new IOException("generic");
        IoRuntimeException propagated = IoRuntimeException.propagate(ioEx);
        assertTrue(propagated instanceof IoRuntimeException);
        assertSame(ioEx, propagated.getCause());
    }

    @Test
    public void shouldPropagateFileNotFoundExceptionAsFileNotFoundRuntimeException() {
        FileNotFoundException fnfEx = new FileNotFoundException("missing");
        IoRuntimeException propagated = IoRuntimeException.propagate(fnfEx);
        assertTrue("Should be FileNotFoundRuntimeException",
                propagated instanceof FileNotFoundRuntimeException);
        assertSame(fnfEx, propagated.getCause());
    }
}
