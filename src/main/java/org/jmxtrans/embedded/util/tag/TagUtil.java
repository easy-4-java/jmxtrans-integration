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
package org.jmxtrans.embedded.util.tag;

import java.util.Properties;

/**
 * Lookup helpers that resolve a tag value from the surrounding JVM's
 * environment.
 *
 * <p>{@link #getTagValFromEnv(String)} consults two sources, in order:</p>
 * <ol>
 *   <li>JVM system properties &mdash; read via
 *       {@link System#getProperty(String)}.</li>
 *   <li>Process environment variables &mdash; read via
 *       {@link System#getenv(String)}.</li>
 * </ol>
 *
 * <p>Environment variables override system properties when both sources are
 * populated. The lookup is purely advisory &mdash; callers can fall back to
 * a custom {@link org.jmxtrans.embedded.ResultNameStrategy} when this method
 * returns {@code null}.</p>
 *
 * @author [@Loong Wan](https://github.com/loong10k)
 * @since 3.0.0
 */
public class TagUtil {

    /**
     * Returns the value associated with {@code tagEnvName} in the JVM system
     * properties or the OS environment.
     *
     * <p>The method first checks the system properties returned by
     * {@link System#getProperties()}; if the key is absent, the
     * environment-variables table is consulted. The returned value is the
     * more specific environment-variable value when present, otherwise the
     * system-property value, otherwise {@code null}.</p>
     *
     * @param tagEnvName the property / environment-variable name; must not
     *                   be {@code null}.
     * @return the resolved value, or {@code null} if the name is not present
     *         in either source.
     */
    public static String getTagValFromEnv(String tagEnvName) {
		assert tagEnvName != null;
		String tagVal = null;
        Properties properties = System.getProperties();
        if(properties.contains(tagEnvName)){
        	tagVal = System.getProperty(tagEnvName);
        }

 		String result = System.getenv(tagEnvName);
 		if (result != null) {
 			tagVal =  result;
 		}
 		return tagVal;
    }

}
