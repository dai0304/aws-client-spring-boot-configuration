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
public class AwsBeanDefinitionRegistrar implements ImportBeanDefinitionRegistrar {
	
	@Override
	public void registerBeanDefinitions(AnnotationMetadata importingClassMetadata, BeanDefinitionRegistry registry) {
		log.trace("registerBeanDefinitions: {}", registry);
		
//		boolean syncEnabled = Boolean.valueOf(environment.getProperty("aws.sync-enabled", "true"));
//		boolean asyncEnabled = Boolean.valueOf(environment.getProperty("aws.async-enabled", "false"));
//		
//		if (syncEnabled == false) {
//			log.debug("AWS sync client is disabled.");
//		}
//		if (asyncEnabled == false) {
//			log.debug("AWS async client is disabled.");
//		}
		
		AnnotationAttributes attributes = AnnotationAttributes.fromMap(
				importingClassMetadata.getAnnotationAttributes(EnableAwsClient.class.getName(), false));
		Class<?>[] clientClasses = attributes.getClassArray("value");
		
		Arrays.stream(clientClasses)
			.forEach(c -> registerAwsClient(registry, c));
	}
	
	private void registerAwsClient(BeanDefinitionRegistry registry, Class<?> clientClass) {
		try {
			log.trace("Attempt to configure AWS client: {}", clientClass);
			if (AwsClientFactoryBean.isConfigurable(registry, clientClass) == false) {
				return;
			}
			
			Class<?> builderClass = Class.forName(clientClass.getName() + "ClientBuilder");
			if (registry.containsBeanDefinition(clientClass.getName())) {
				log.debug("Skip {} -- already configured", clientClass.getName());
				return;
			}
			
			RootBeanDefinition clientBeanDef = createAwsClientBeanDefinition(builderClass, clientClass);
			BeanDefinitionHolder clientBDHolder = new BeanDefinitionHolder(clientBeanDef, clientClass.getName());
			BeanDefinitionReaderUtils.registerBeanDefinition(clientBDHolder, registry);
			
			log.trace("AWS client {} is configured", clientClass.getName());
		} catch (ClassNotFoundException e) {
			log.trace("Skip.  Builder class for {} is not found in classpath", clientClass);
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
