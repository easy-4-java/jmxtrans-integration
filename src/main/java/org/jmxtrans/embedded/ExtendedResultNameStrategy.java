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

import java.util.Map;
import java.util.concurrent.Callable;

import org.jmxtrans.embedded.util.network.MacAddressUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Specialised {@link ResultNameStrategy} that registers two extra
 * expression evaluators &mdash; {@code #mac_address#} and
 * {@code #escaped_mac_address#} &mdash; on top of the default
 * {@code hostname}/{@code hostaddress} family.
 *
 * <p>The MAC address is resolved at construction time via
 * {@link MacAddressUtils#getMacAddress()}; if the lookup fails (for instance
 * because the JVM is running in an environment where the network subsystem is
 * sandboxed) the failure is logged but the strategy remains usable with the
 * default evaluators inherited from the parent class.</p>
 *
 * @author [@Loong Wan](https://github.com/loong10k)
 * @since 3.0.0
 * @see ResultNameStrategy
 * @see MacAddressUtils#getMacAddress()
 */
public class ExtendedResultNameStrategy extends ResultNameStrategy {

    /**
     * SLF4J logger used to surface failures while the MAC address is probed
     * during construction.
     */
    protected final Logger logger = LoggerFactory.getLogger(getClass());

    /**
     * Cache of expression evaluators inherited from the base class. Held as a
     * field so that subclasses (and tests) can inspect the registered
     * evaluators without re-allocating them.
     */
    protected Map<String, Callable<String>> _expressionEvaluators = null;

    /**
     * Builds the strategy and registers the {@code mac_address} and
     * {@code escaped_mac_address} expression evaluators. Any failure raised
     * by {@link MacAddressUtils#getMacAddress()} is caught and reported via
     * the SLF4J logger; the strategy remains usable in either case.
     */
	public ExtendedResultNameStrategy() {
		super();
		try {

			String macAddress = MacAddressUtils.getMacAddress();;
            registerExpressionEvaluator("mac_address", macAddress);
            registerExpressionEvaluator("escaped_mac_address", macAddress.replaceAll("\\:", "_"));

        } catch (Exception e) {
            logger.error("Exception resolving localhost, expressions like #hostname#, #canonical_hostname# or #hostaddress# will not be available", e);
        }
	}

}
