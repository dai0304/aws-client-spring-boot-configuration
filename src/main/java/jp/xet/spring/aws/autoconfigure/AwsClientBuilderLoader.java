/*
 * Copyright 2018 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package jp.xet.spring.aws.autoconfigure;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import org.springframework.util.ConcurrentReferenceHashMap;

@Slf4j
class AwsClientBuilderLoader {
	
	private static final Map<ClassLoader, Set<String>> CACHE = new ConcurrentReferenceHashMap<>();
	
	/**
	 * The location to look for builders.
	 * 
	 * <p>Can be present in multiple JAR files.</p>
	 */
	static final String DEFAULT_LOCATION = "META-INF/aws.builders";
	
	@Setter
	private static String location = DEFAULT_LOCATION;
	
	
	/**
	 * Load the fully qualified class names of builder implementations of the
	 * given type from {@value #location}, using the given class loader.
	 *
	 * @throws UncheckedIOException if an error occurs while loading builder names
	 */
	static Set<String> loadBuilderNames() {
		return loadBuilderNames(null);
	}
	
	/**
	 * Load the fully qualified class names of builder implementations of the
	 * given type from {@value #location}, using the given class loader.
	 *
	 * @param classLoader the ClassLoader to use for loading resources; can be {@code null} to use the default
	 * @throws UncheckedIOException if an error occurs while loading builder names
	 */
	static Set<String> loadBuilderNames(ClassLoader classLoader) {
		Set<String> result = CACHE.get(classLoader);
		if (result != null) {
			return result;
		}
		
		try {
			Enumeration<URL> urls = classLoader != null
					? classLoader.getResources(location)
					: ClassLoader.getSystemResources(location);
			result = new HashSet<>();
			while (urls.hasMoreElements()) {
				URL url = urls.nextElement();
				try (BufferedReader br = new BufferedReader(new InputStreamReader(
						url.openStream(), StandardCharsets.UTF_8))) {
					br.lines()
						.map(String::trim)
						.filter(n -> n.charAt(0) != '#')
						.forEach(result::add);
				}
			}
			CACHE.put(classLoader, result);
			return result;
		} catch (IOException e) {
			throw new UncheckedIOException("Unable to load builders from location [" + location + "]", e);
		}
	}
}
