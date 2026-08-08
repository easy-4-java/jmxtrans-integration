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

import java.io.FileNotFoundException;
import java.io.IOException;

/**
 * Unchecked wrapper around an {@link IOException}. Modelled after
 * {@code com.google.common.base.Throwables#propagate(Throwable)} so that the
 * legacy JmxTrans code base can be written entirely with checked-exception
 * boundaries while still propagating failures up to generic runtimes such as
 * scheduled executors.
 *
 * <p>{@link #propagate(IOException)} additionally down-casts
 * {@link FileNotFoundException} to the more specific
 * {@link FileNotFoundRuntimeException} so that callers can react accordingly
 * without re-inspecting the original cause.</p>
 *
 * <pre>
 *     try {
 *         ...
 *     } catch (IOException e) {
 *         throw IoRuntimeException.propagate(e);
 *     }
 * </pre>
 *
 * @author Cyrille Le Clerc
 * @since 3.0.0
 */
public class IoRuntimeException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    /**
     * Wraps the given {@link IOException} in either an
     * {@link IoRuntimeException} or, when the cause is a
     * {@link FileNotFoundException}, a {@link FileNotFoundRuntimeException}.
     *
     * <p>Inspired by {@code com.google.common.base.Throwables#propagate(java.lang.Throwable)}.</p>
     *
     * @param e the checked I/O exception to wrap; must not be {@code null}.
     * @return a runtime exception that carries {@code e} as its cause.
     */
    public static IoRuntimeException propagate(IOException e) {
        if (e instanceof FileNotFoundException) {
            return new FileNotFoundRuntimeException(e);
        } else {
            return new IoRuntimeException(e);
        }
    }

    /**
     * Default no-args constructor.
     */
    public IoRuntimeException() {
        super();
    }

    /**
     * Creates an exception with the given detail message.
     *
     * @param message the detail message.
     */
    public IoRuntimeException(String message) {
        super(message);
    }

    /**
     * Creates an exception with the given detail message and cause.
     *
     * @param message the detail message.
     * @param cause   the underlying cause.
     */
    public IoRuntimeException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * Creates an exception with full control over the standard
     * {@link Throwable} flags.
     *
     * @param message            the detail message.
     * @param cause              the underlying cause.
     * @param enableSuppression  whether suppression is enabled.
     * @param writableStackTrace whether the stack trace should be writable.
     */
    protected IoRuntimeException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }

    /**
     * Creates an exception that wraps the given cause.
     *
     * @param cause the underlying cause.
     */
    public IoRuntimeException(Throwable cause) {
        super(cause);
    }
}
