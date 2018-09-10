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

import java.lang.reflect.UndeclaredThrowableException;
import java.util.Map;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanDefinitionRegistryPostProcessor;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;


/**
 * TODO miyamoto.daisuke.
 *
 * @author miyamoto.daisuke
 * @since #version#
 */
@Slf4j
@RequiredArgsConstructor
public class AwsClientBeanRegistrar
		implements BeanDefinitionRegistryPostProcessor, Ordered, InitializingBean {
	
	private final ConfigurableEnvironment environment;
	
	private boolean syncEnabled = true;
	
	private boolean asyncEnabled;
	
	private final AwsClientBuilderConfigurer awsClientBuilderConfigurer;
	
	
	@Override
	public int getOrder() {
		return Ordered.LOWEST_PRECEDENCE;
	}
	
	@Override
	public void afterPropertiesSet() throws Exception {
		syncEnabled = Boolean.valueOf(environment.getProperty("aws.sync-enabled", "true"));
		asyncEnabled = Boolean.valueOf(environment.getProperty("aws.async-enabled", "false"));
	}
	
	@Override
	public void postProcessBeanDefinitionRegistry(BeanDefinitionRegistry registry) throws BeansException {
		log.trace("postProcessBeanDefinitionRegistry");
	}
	
	@Override
	public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
		log.trace("postProcessBeanFactory");
		
		if (syncEnabled == false) {
			log.debug("AWS sync client is disabled.");
		}
		if (asyncEnabled == false) {
			log.debug("AWS async client is disabled.");
		}
		AwsClientBuilderLoader.loadBuilderNames().forEach(n -> registerAwsClient(beanFactory, n));
	}
	
	private void registerAwsClient(ConfigurableListableBeanFactory beanFactory, String builderClassName) {
		try {
			log.trace("Attempt to configure AWS client: {}", builderClassName);
			if (syncEnabled == false && builderClassName.endsWith("AsyncClientBuilder") == false) {
				log.trace("Skip {} -- sync client is disabled.", builderClassName);
				return;
			}
			if (asyncEnabled == false && builderClassName.endsWith("AsyncClientBuilder")) {
				log.trace("Skip {} -- async client is disabled", builderClassName);
				return;
			}
			if (awsClientBuilderConfigurer.isConfigurable(builderClassName) == false) {
				return;
			}
			
			Class<?> builderClass = Class.forName(builderClassName);
			Class<?> clientClass = AwsClientUtil.getClientClass(builderClass);
			
			if (beanFactory.containsBeanDefinition(clientClass.getName())) {
				log.debug("Skip {} -- already configured", clientClass.getName());
				return;
			}
			
			Object builder = AwsClientUtil.createBuilder(builderClass);
			awsClientBuilderConfigurer.configureBuilder(builderClassName, clientClass, builder);
			Object client = AwsClientUtil.buildClient(builder);
			
			beanFactory.registerSingleton(clientClass.getName(), client);
			log.trace("AWS client {} is configured", clientClass.getName());
		} catch (ClassNotFoundException e) {
			log.trace("Skip.  Builder class is not found in classpath: {}", builderClassName);
			// ignore
		} catch (ClientDisabledException e) {
			log.debug("Skip.  Client auto-configuration is disabled: {}", builderClassName);
			// ignore
		} catch (ClientClassNotDeterminedException | IllegalStateException | UndeclaredThrowableException e) {
			log.error("Illegal builder: {}", builderClassName, e);
			throw e;
		}
	}
}
