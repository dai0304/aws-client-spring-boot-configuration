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

import java.util.Locale;

import lombok.extern.slf4j.Slf4j;

import org.springframework.util.Assert;
import org.springframework.util.MethodInvoker;
import org.springframework.util.ObjectUtils;
import org.springframework.util.ReflectionUtils;

@Slf4j
class InternalReflectionUtil {
	
	@SuppressWarnings("unchecked")
	static <T> T invokeMethod(Object target, String name, Object... args) {
		Assert.notNull(target, "Target object must not be null");
		Assert.hasText(name, "Method name must not be empty");
		
		try {
			MethodInvoker methodInvoker = new MethodInvoker();
			methodInvoker.setTargetObject(target);
			methodInvoker.setTargetMethod(name);
			methodInvoker.setArguments(args);
			methodInvoker.prepare();
			
			if (log.isTraceEnabled()) {
				log.trace(String.format(Locale.ENGLISH, "Invoking method '%s' on %s with arguments %s",
						name, safeToString(target), ObjectUtils.nullSafeToString(args)));
			}
			
			return (T) methodInvoker.invoke();
		} catch (Exception e) { // NOPMD catching generic exceptions
			ReflectionUtils.handleReflectionException(e);
			throw new AssertionError("Should never get here", e);
		}
	}
	
	@SuppressWarnings("unchecked")
	static <T> T invokeStaticMethod(Class<?> target, String name, Object... args) {
		Assert.notNull(target, "Target object must not be null");
		Assert.hasText(name, "Method name must not be empty");
		
		try {
			MethodInvoker methodInvoker = new MethodInvoker();
			methodInvoker.setTargetClass(target);
			methodInvoker.setTargetMethod(name);
			methodInvoker.setArguments(args);
			methodInvoker.prepare();
			
			if (log.isTraceEnabled()) {
				log.trace(String.format(Locale.ENGLISH, "Invoking static method '%s' on %s with arguments %s",
						name, safeToString(target), ObjectUtils.nullSafeToString(args)));
			}
			
			return (T) methodInvoker.invoke();
		} catch (Exception e) { // NOPMD catching generic exceptions
			ReflectionUtils.handleReflectionException(e);
			throw new AssertionError("Should never get here", e);
		}
	}
	
	private static String safeToString(Object target) {
		try {
			return String.format(Locale.ENGLISH, "target object [%s]", target);
		} catch (Exception e) { // NOPMD catching generic exceptions
			String targetClassName = target != null ? target.getClass().getName() : "unknown";
			return String.format(Locale.ENGLISH, "target of type [%s] whose toString() method threw [%s]",
					targetClassName, e);
		}
	}
}
