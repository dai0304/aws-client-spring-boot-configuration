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
package jp.xet.springconfig.aws.v2;

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
class AwsClientV2BeanDefinitionRegistrar implements ImportBeanDefinitionRegistrar {
	
	@Override
	public void registerBeanDefinitions(AnnotationMetadata importingClassMetadata, BeanDefinitionRegistry registry) {
		log.trace("registerBeanDefinitions: {}", registry);
		
		AnnotationAttributes attributes = AnnotationAttributes.fromMap(
				importingClassMetadata.getAnnotationAttributes(EnableAwsClientV2.class.getName(), false));
		if (attributes == null) {
			log.warn("Attributes of EnableAwsClientV2 is null.");
			return;
		}
		Class<?>[] clientClasses = attributes.getClassArray("value");
		
		Arrays.stream(clientClasses)
			.forEach(c -> registerAwsClient(registry, c));
	}
	
	private void registerAwsClient(BeanDefinitionRegistry registry, Class<?> clientClass) {
		try {
			log.trace("Attempt to configure AWS client: {}", clientClass);
			if (registry.containsBeanDefinition(clientClass.getName())) {
				log.debug("Skip.  Bean {} is already configured", clientClass.getName());
				return;
			}
			
			RootBeanDefinition clientBeanDef = createAwsClientBeanDefinition(clientClass);
			BeanDefinitionHolder clientBDHolder = new BeanDefinitionHolder(clientBeanDef, clientClass.getName());
			BeanDefinitionReaderUtils.registerBeanDefinition(clientBDHolder, registry);
			
			log.trace("AWS client {} is configured", clientClass.getName());
		} catch (IllegalStateException | UndeclaredThrowableException e) {
			log.error("Illegal builder for the client {}", clientClass, e);
			throw e;
		}
	}
	
	private RootBeanDefinition createAwsClientBeanDefinition(Class<?> clientClass) {
		ConstructorArgumentValues ctorArgs = new ConstructorArgumentValues();
		ctorArgs.addIndexedArgumentValue(0, clientClass);
		ctorArgs.addIndexedArgumentValue(1, new RuntimeBeanReference("awsClientV2PropertiesMap"));
		ctorArgs.addIndexedArgumentValue(2, new RuntimeBeanReference("awsS3ClientV2Properties"));
		
		RootBeanDefinition clientBeanDef = new RootBeanDefinition(AwsClientV2FactoryBean.class);
		clientBeanDef.setTargetType(clientClass);
		clientBeanDef.setConstructorArgumentValues(ctorArgs);
		return clientBeanDef;
	}
}
