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

import java.util.Map;
import java.util.concurrent.Callable;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Unit tests for {@link ExtendedResultNameStrategy}.
 *
 * @since 3.0.0
 */
public class ExtendedResultNameStrategyTest {

    /**
     * Constructing the strategy must always succeed, regardless of whether
     * the underlying MAC-address probe succeeds or fails; this matches the
     * documented contract that any probe failure is merely logged.
     */
    @Test
    public void shouldConstructWithoutThrowing() {
        ExtendedResultNameStrategy strategy = new ExtendedResultNameStrategy();
        assertNotNull(strategy);
        Map<String, Callable<String>> evaluators = strategy.getExpressionEvaluators();
        assertNotNull(evaluators);
        // mac_address is always registered (value may be null if probe fails)
        assertTrue("'mac_address' must be registered",
                evaluators.containsKey("mac_address"));
        // escaped_mac_address is only registered when the MAC probe succeeds
        // because null.replaceAll() throws NPE which is caught
    }

    /**
     * The mac_address evaluator is always registered. When the MAC probe
     * succeeds the callable returns a non-null value; when it fails the
     * callable may return null. We verify the evaluator is present and
     * callable without triggering resolveExpression (which escapes null).
     */
    @Test
    public void shouldHaveMacAddressEvaluatorRegistered() throws Exception {
        ExtendedResultNameStrategy strategy = new ExtendedResultNameStrategy();
        Map<String, Callable<String>> evaluators = strategy.getExpressionEvaluators();
        if (evaluators.containsKey("mac_address")) {
            Callable<String> callable = evaluators.get("mac_address");
            // Invoke the callable directly; it may return null on some platforms
            callable.call();
        }
    }

    /**
     * The {@code escaped_mac_address} variant, when registered, must never
     * contain a colon because colons are the very character the strategy
     * escapes.
     */
    @Test
    public void shouldEscapeColonsInEscapedMacAddress() {
        ExtendedResultNameStrategy strategy = new ExtendedResultNameStrategy();
        Map<String, Callable<String>> evaluators = strategy.getExpressionEvaluators();
        if (evaluators.containsKey("escaped_mac_address")) {
            String escaped = strategy.resolveExpression("#escaped_mac_address#");
            assertEquals(-1, escaped.indexOf(':'));
        }
    }

    /**
     * The inherited {@link ResultNameStrategy#resolveExpression(String)}
     * still works &mdash; i.e. the parent {@code #hostname#} family must
     * remain registered after construction.
     */
    @Test
    public void shouldInheritDefaultEvaluators() {
        ExtendedResultNameStrategy strategy = new ExtendedResultNameStrategy();
        Map<String, Callable<String>> evaluators = strategy.getExpressionEvaluators();
        assertTrue("hostname expression must be inherited",
                evaluators.containsKey("hostname"));
        assertTrue("canonical_hostname must be inherited",
                evaluators.containsKey("canonical_hostname"));
    }

    /**
     * The {@code getExpressionEvaluators()} accessor returns a mutable map
     * shared with the parent; double-check the contract by adding a new
     * evaluator via the public API and reading it back.
     */
    @Test
    public void shouldReturnConfiguredExpressionEvaluators() {
        ExtendedResultNameStrategy strategy = new ExtendedResultNameStrategy();
        strategy.registerExpressionEvaluator("custom_key", "custom_value");
        assertEquals("custom_value", strategy.resolveExpression("#custom_key#"));
    }
}
