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
package org.jmxtrans.embedded.util.io;

import java.io.Closeable;
import java.io.InputStream;
import java.io.Writer;
import java.net.HttpURLConnection;
import java.net.URLConnection;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Quiet close helpers for the various I/O resources used by JmxTrans.
 *
 * <p>The {@link #closeQuietly(Closeable)} family of methods swallows any
 * exception thrown by {@link Closeable#close()} so that they can be safely
 * chained in {@code finally} blocks where the surrounding code has no good
 * strategy for handling close failures. Use them sparingly &mdash; the
 * underlying streams may still leak if the JVM holds onto the resource
 * beyond the close attempt.</p>
 *
 * @author <a href="https://github.com/loong10k">Loong Wan</a>
 * @since 3.0.0
 */
public class IoUtils {

    /** SLF4J logger used by the various close helpers. */
    protected final static Logger logger = LoggerFactory.getLogger(IoUtils.class.getName());

    /**
     * Quietly closes a {@link URLConnection}, falling back to
     * {@link HttpURLConnection#disconnect()} when the connection is an HTTP
     * connection. Null connections are accepted as no-ops.
     *
     * @param cnn the connection to close / disconnect; may be {@code null}.
     */
    public static void closeQuietly (URLConnection cnn) {
        if (cnn == null) {
            return;
        } else if (cnn instanceof HttpURLConnection) {
            ((HttpURLConnection) cnn).disconnect();
        } else {
            // do nothing
        }
    }

    /**
     * Quietly closes a {@link Closeable}, swallowing every exception thrown
     * by {@link Closeable#close()}. Null arguments are accepted as no-ops.
     *
     * @param closeable the closeable to close; may be {@code null}.
     */
    public static void closeQuietly(Closeable closeable) {
        if (closeable == null)
            return;
        try {
            closeable.close();
        } catch (Exception e) {
            // ignore silently
        }
    }

    /**
     * Quietly closes a {@link Writer}, swallowing every exception. Null
     * arguments are accepted as no-ops.
     *
     * @param writer the writer to close; may be {@code null}.
     */
    public static void closeQuietly(Writer writer) {
        if (writer == null)
            return;
        try {
            writer.close();
        } catch (Exception e) {
            // ignore silently
        }
    }

    /**
     * Quietly closes an {@link InputStream}, swallowing every exception.
     * Needed for old JVMs where {@link InputStream} does not implement
     * {@link Closeable}. Null arguments are accepted as no-ops.
     *
     * @param inputStream the input stream to close; may be {@code null}.
     */
    public static void closeQuietly(InputStream inputStream) {
        if (inputStream == null)
            return;
        try {
            inputStream.close();
        } catch (Exception e) {
            // ignore silently
        }
    }

}
