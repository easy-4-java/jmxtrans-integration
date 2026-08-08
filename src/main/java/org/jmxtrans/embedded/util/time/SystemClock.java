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
package org.jmxtrans.embedded.util.time;

import java.sql.Timestamp;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * High-throughput, monotonically-refreshed current-time millisecond clock.
 *
 * <p>A typical JVM scales poorly when millions of
 * {@link System#currentTimeMillis()} calls per second are made &mdash; every
 * call asks the operating system for the wall-clock time. To side-step that
 * overhead, this class caches the wall-clock value in an {@link AtomicLong}
 * that a single daemon thread updates once per millisecond. Callers obtain
 * the cached value through {@link #now()} without paying the syscall cost.</p>
 *
 * <p>The clock is a singleton: constructor is private, and the only instance
 * is held in a lazy {@code InstanceHolder} static field.</p>
 *
 * @author lry
 * @since 3.0.0
 * @see <a href="http://git.oschina.net/yu120/sequence">oschina sequence</a>
 */
public class SystemClock {

    /** Update period (in milliseconds) of the cached clock. */
    private final long period;

    /** Latest cached wall-clock value, updated by the daemon thread. */
    private final AtomicLong now;

    /**
     * Creates a new clock that refreshes every {@code period} milliseconds.
     *
     * @param period refresh period in milliseconds.
     */
    private SystemClock(long period) {
        this.period = period;
        this.now = new AtomicLong(System.currentTimeMillis());
        scheduleClockUpdating();
    }

    /**
     * Holder class that lazily initialises the singleton when first
     * referenced.
     */
    private static class InstanceHolder {
        public static final SystemClock INSTANCE = new SystemClock(1);
    }

    /**
     * Returns the singleton instance.
     *
     * @return the singleton {@link SystemClock}; never {@code null}.
     */
    private static SystemClock instance() {
        return InstanceHolder.INSTANCE;
    }

    /**
     * Schedules the cached wall-clock refresh at the configured period
     * using a single daemon thread; the executor is intentionally not
     * shut down so that the daemon thread is reclaimed only when the JVM
     * exits.
     */
    private void scheduleClockUpdating() {
        ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(new ThreadFactory() {
            public Thread newThread(Runnable runnable) {
                Thread thread = new Thread(runnable, "System Clock");
                thread.setDaemon(true);
                return thread;
            }
        });
        scheduler.scheduleAtFixedRate(new Runnable() {
            public void run() {
                now.set(System.currentTimeMillis());
            }
        }, period, period, TimeUnit.MILLISECONDS);
    }

    /**
     * Reads the cached wall-clock value.
     *
     * @return the cached value in milliseconds since the Unix epoch.
     */
    private long currentTimeMillis() {
        return now.get();
    }

    /**
     * Returns the current cached wall-clock time in milliseconds since the
     * Unix epoch.
     *
     * @return milliseconds since the Unix epoch.
     */
    public static long now() {
        return instance().currentTimeMillis();
    }

    /**
     * Returns the current cached wall-clock time formatted as a SQL
     * {@link Timestamp#toString()} value.
     *
     * @return textual representation of the current cached time.
     */
	public static String nowDate() {
		return new Timestamp(instance().currentTimeMillis()).toString();
	}

}
