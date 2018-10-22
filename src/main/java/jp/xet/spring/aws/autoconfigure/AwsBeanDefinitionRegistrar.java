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
import java.util.Arrays;

import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.config.BeanDefinitionHolder;
import org.springframework.beans.factory.config.ConstructorArgumentValues;
import org.springframework.beans.factory.config.RuntimeBeanReference;
import org.springframework.beans.factory.support.BeanDefinitionReaderUtils;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar;
import org.springframework.core.annotation.AnnotationAttributes;
import org.springframework.core.type.AnnotationMetadata;

@Slf4j
class AwsBeanDefinitionRegistrar implements ImportBeanDefinitionRegistrar {
	
	private static final String ENCRYPTION_CLIENT = "com.amazonaws.services.s3.AmazonS3Encryption";
	
	
	@Override
	public void registerBeanDefinitions(AnnotationMetadata importingClassMetadata, BeanDefinitionRegistry registry) {
		log.trace("registerBeanDefinitions: {}", registry);
		
		AnnotationAttributes attributes = AnnotationAttributes.fromMap(
				importingClassMetadata.getAnnotationAttributes(EnableAwsClient.class.getName(), false));
		if (attributes == null) {
			return;
		}
		Class<?>[] clientClasses = attributes.getClassArray("value");
		
		Arrays.stream(clientClasses)
			.forEach(c -> registerAwsClient(registry, c));
	}
	
	private void registerAwsClient(BeanDefinitionRegistry registry, Class<?> clientClass) {
		try {
			log.trace("Attempt to configure AWS client: {}", clientClass);
			if (clientClass.getName().equals(ENCRYPTION_CLIENT)
					&& registry.containsBeanDefinition(AwsClientFactoryBean.ENCRYPTION_MATERIALS_PROVIDER) == false) {
				log.debug("Skip.  " + AwsClientFactoryBean.ENCRYPTION_MATERIALS_PROVIDER
						+ " for " + ENCRYPTION_CLIENT + " is not configured");
				return;
			}
			
			Class<?> builderClass = Class.forName(clientClass.getName() + "ClientBuilder");
			if (registry.containsBeanDefinition(clientClass.getName())) {
				log.debug("Skip.  Bean {} is already configured", clientClass.getName());
				return;
			}
			
			RootBeanDefinition clientBeanDef = createAwsClientBeanDefinition(builderClass, clientClass);
			BeanDefinitionHolder clientBDHolder = new BeanDefinitionHolder(clientBeanDef, clientClass.getName());
			BeanDefinitionReaderUtils.registerBeanDefinition(clientBDHolder, registry);
			
			log.trace("AWS client {} is configured", clientClass.getName());
		} catch (ClassNotFoundException e) {
			log.warn("Skip.  Builder class for {} is not found in classpath", clientClass);
			// ignore
		} catch (IllegalStateException | UndeclaredThrowableException e) {
			log.error("Illegal builder for the client {}", clientClass, e);
			throw e;
		}
	}
	
	private RootBeanDefinition createAwsClientBeanDefinition(Class<?> builderClass, Class<?> clientClass) {
		ConstructorArgumentValues ctorArgs = new ConstructorArgumentValues();
		ctorArgs.addIndexedArgumentValue(0, builderClass);
		ctorArgs.addIndexedArgumentValue(1, clientClass);
		ctorArgs.addIndexedArgumentValue(2, new RuntimeBeanReference("awsClientPropertiesMap"));
		ctorArgs.addIndexedArgumentValue(3, new RuntimeBeanReference("awsS3ClientProperties"));
		
		RootBeanDefinition clientBeanDef = new RootBeanDefinition(AwsClientFactoryBean.class);
		clientBeanDef.setTargetType(clientClass);
		clientBeanDef.setConstructorArgumentValues(ctorArgs);
		return clientBeanDef;
	}
}
