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

/**
 * Specialised {@link IoRuntimeException} raised when an {@link java.io.FileNotFoundException}
 * is propagated via {@link IoRuntimeException#propagate(java.io.IOException)}.
 *
 * <p>Modelling the &quot;file not found&quot; case as its own subclass lets
 * callers react differently &mdash; by retrying, by surfacing a 404 or by
 * generating a more user-friendly message &mdash; without losing the
 * unchecked nature of the underlying exception.</p>
 *
 * @author Cyrille Le Clerc
 * @since 3.0.0
 * @see IoRuntimeException#propagate(java.io.IOException)
 */
public class FileNotFoundRuntimeException extends IoRuntimeException {

    private static final long serialVersionUID = 1L;

    /**
     * Creates a new exception with no detail message and no cause.
     */
    public FileNotFoundRuntimeException() {
    }

    /**
     * Creates a new exception with the given detail message.
     *
     * @param message the detail message.
     */
    public FileNotFoundRuntimeException(String message) {
        super(message);
    }

    /**
     * Creates a new exception with the given detail message and cause.
     *
     * @param message the detail message.
     * @param cause   the underlying cause.
     */
    public FileNotFoundRuntimeException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * Creates a new exception with the given detail message, cause,
     * suppression flag and writable-stack-trace flag.
     *
     * @param message            the detail message.
     * @param cause              the underlying cause.
     * @param enableSuppression  whether suppression is enabled.
     * @param writableStackTrace whether the stack trace should be writable.
     */
    public FileNotFoundRuntimeException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }

    /**
     * Creates a new exception that wraps the given cause.
     *
     * @param cause the underlying cause.
     */
    public FileNotFoundRuntimeException(Throwable cause) {
        super(cause);
    }
}
