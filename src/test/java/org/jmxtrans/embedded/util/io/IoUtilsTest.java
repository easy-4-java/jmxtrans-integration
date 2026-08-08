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

import java.io.ByteArrayInputStream;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.io.Writer;
import java.net.URLConnection;

import org.junit.Test;

import static org.junit.Assert.assertTrue;

/**
 * Unit tests for {@link IoUtils}.
 *
 * @since 3.0.0
 */
public class IoUtilsTest {

    @Test
    public void shouldCloseNullCloseableWithoutThrowing() {
        IoUtils.closeQuietly((Closeable) null);
    }

    @Test
    public void shouldCloseValidCloseableQuietly() {
        final boolean[] closed = {false};
        Closeable closeable = new Closeable() {
            @Override
            public void close() throws IOException {
                closed[0] = true;
            }
        };
        IoUtils.closeQuietly(closeable);
        assertTrue("Closeable should have been closed", closed[0]);
    }

    @Test
    public void shouldSwallowExceptionFromCloseable() {
        Closeable throwing = new Closeable() {
            @Override
            public void close() throws IOException {
                throw new IOException("simulated");
            }
        };
        IoUtils.closeQuietly(throwing);
        // no exception should propagate
    }

    @Test
    public void shouldCloseNullWriterWithoutThrowing() {
        IoUtils.closeQuietly((Writer) null);
    }

    @Test
    public void shouldCloseValidWriterQuietly() {
        final boolean[] closed = {false};
        Writer writer = new StringWriter() {
            @Override
            public void close() throws IOException {
                closed[0] = true;
                super.close();
            }
        };
        IoUtils.closeQuietly(writer);
        assertTrue("Writer should have been closed", closed[0]);
    }

    @Test
    public void shouldSwallowExceptionFromWriter() {
        Writer throwing = new StringWriter() {
            @Override
            public void close() throws IOException {
                throw new IOException("simulated");
            }
        };
        IoUtils.closeQuietly(throwing);
        // no exception should propagate
    }

    @Test
    public void shouldCloseNullInputStreamWithoutThrowing() {
        IoUtils.closeQuietly((InputStream) null);
    }

    @Test
    public void shouldCloseValidInputStreamQuietly() {
        InputStream is = new ByteArrayInputStream(new byte[]{1, 2, 3});
        IoUtils.closeQuietly(is);
        // no exception should propagate
    }

    @Test
    public void shouldSwallowExceptionFromInputStream() {
        InputStream throwing = new InputStream() {
            @Override
            public int read() throws IOException {
                throw new IOException("simulated");
            }

            @Override
            public void close() throws IOException {
                throw new IOException("close failed");
            }
        };
        IoUtils.closeQuietly(throwing);
        // no exception should propagate
    }

    @Test
    public void shouldCloseNullURLConnectionWithoutThrowing() {
        IoUtils.closeQuietly((URLConnection) null);
    }
}
