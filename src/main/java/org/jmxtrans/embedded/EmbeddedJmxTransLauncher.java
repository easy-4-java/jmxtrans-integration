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

import javax.annotation.Resource;

/**
 * Spring-friendly launcher bean that holds an {@link EmbeddedJmxTrans} instance
 * and exposes it through the standard JavaBean accessors so that dependency
 * injection containers can wire it in at start-up.
 *
 * <p>The launcher is intentionally lightweight: configuration, lifecycle and
 * worker scheduling are delegated to the embedded {@code EmbeddedJmxTrans}
 * runtime, leaving only the trivial "&nbsp;hold + accessor&nbsp;" responsibility
 * to this class. Typically declared as a {@code @Bean} in a Spring
 * {@code @Configuration} class together with a
 * {@code javax.annotation.Resource}-injected field so that the embedded
 * exporter starts as part of the surrounding application context.</p>
 *
 * @author [@Loong Wan](https://github.com/loong10k)
 * @since 3.0.0
 * @see EmbeddedJmxTrans
 * @see javax.annotation.Resource
 */
public class EmbeddedJmxTransLauncher {

	/**
	 * The embedded exporter instance that this launcher owns and exposes.
	 * Injected by the surrounding container via {@link Resource}.
	 */
	@Resource
	protected EmbeddedJmxTrans jmxtrans;

	/**
	 * Returns the underlying {@link EmbeddedJmxTrans} instance so that callers
	 * can interact with the running exporter (start, stop, collect metrics).
	 *
	 * @return the embedded exporter, never {@code null} once
	 *         {@link #setJmxtrans(EmbeddedJmxTrans)} has been called by the
	 *         surrounding container.
	 */
	public EmbeddedJmxTrans getJmxtrans() {
		return jmxtrans;
	}

	/**
	 * Replaces the currently held {@link EmbeddedJmxTrans} instance. Most
	 * callers will not need this method because the embedded exporter is
	 * usually injected by the surrounding container at construction time.
	 *
	 * @param jmxtrans the new embedded exporter; must not be {@code null}.
	 */
	public void setJmxtrans(EmbeddedJmxTrans jmxtrans) {
		this.jmxtrans = jmxtrans;
	}

}
