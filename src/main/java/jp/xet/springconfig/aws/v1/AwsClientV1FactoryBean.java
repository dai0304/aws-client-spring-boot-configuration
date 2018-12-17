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
package jp.xet.springconfig.aws.v1;

import static jp.xet.springconfig.aws.InternalReflectionUtil.invokeMethod;
import static jp.xet.springconfig.aws.v1.AwsClientV1Util.build;
import static jp.xet.springconfig.aws.v1.AwsClientV1Util.configureClientConfiguration;
import static jp.xet.springconfig.aws.v1.AwsClientV1Util.configureCredentialsProvider;
import static jp.xet.springconfig.aws.v1.AwsClientV1Util.configureEndpointConfiguration;
import static jp.xet.springconfig.aws.v1.AwsClientV1Util.configureRegion;
import static jp.xet.springconfig.aws.v1.AwsClientV1Util.createBuilder;

import java.util.Map;
import java.util.Optional;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.config.AbstractFactoryBean;

import com.amazonaws.auth.AWSCredentialsProvider;

import jp.xet.springconfig.aws.v1.AwsClientV1Configuration.AwsClientV1Properties;
import jp.xet.springconfig.aws.v1.AwsClientV1Configuration.AwsS3ClientV1Properties;

/**
 * Spring factory bean class of AWS client v1.
 *
 * @param <T> type of AWS client v1
 * @author miyamoto.daisuke
 */
@Slf4j
@RequiredArgsConstructor
class AwsClientV1FactoryBean<T>extends AbstractFactoryBean<T> {
	
	private static final String S3_BUILDER = "com.amazonaws.services.s3.AmazonS3Builder";
	
	private static final String ENCRYPTION_CLIENT_BUILDER = "com.amazonaws.services.s3.AmazonS3EncryptionClientBuilder";
	
	static final String ENCRYPTION_MATERIALS_PROVIDER =
			"com.amazonaws.services.s3.model.EncryptionMaterialsProvider";
	
	
	private static AwsClientV1Properties getAwsClientProperties(
			Map<String, AwsClientV1Properties> map, Class<?> clientClass) {
		try {
			String servicePackageName = clientClass.getPackage().getName()
				.substring("com.amazonaws.services.".length())
				.replace('.', '-');
			String serviceNameSuffix = clientClass.getName().endsWith("Async") ? "-async" : "";
			
			return map.get(servicePackageName + serviceNameSuffix);
		} catch (IndexOutOfBoundsException e) {
			log.error("Failed to get property name: {}", clientClass);
			throw e;
		}
	}
	
	
	private final Class<?> builderClass;
	
	private final Class<T> clientClass;
	
	private final Map<String, AwsClientV1Properties> awsClientV1PropertiesMap;
	
	private final AwsS3ClientV1Properties awsS3ClientV1Properties;
	
	
	@Override
	public Class<?> getObjectType() {
		return clientClass;
	}
	
	@Override
	protected T createInstance() throws Exception {
		Object builder = createBuilder(builderClass);
		configureBuilder(builder);
		return build(builder);
	}
	
	private void configureBuilder(Object builder) {
		BeanFactory beanFactory = getBeanFactory();
		if (beanFactory == null) {
			return;
		}
		
		if (builderClass.getName().startsWith("com.amazonaws.services.s3.")) {
			configureAmazonS3ClientBuilder(builder);
		}
		
		AwsClientV1Properties config = getAwsClientProperties(awsClientV1PropertiesMap, clientClass);
		if (config == null) {
			return;
		}
		
		Optional.ofNullable(config.getCredentialsProviderBeanName())
			.ifPresent(credentialsProviderBeanName -> {
				AWSCredentialsProvider credentialsProvider =
						beanFactory.getBean(credentialsProviderBeanName, AWSCredentialsProvider.class);
				configureCredentialsProvider(builder, credentialsProvider);
			});
		
		configureClientConfiguration(builder, config.getClient());
		configureEndpointConfiguration(builder, config.getEndpoint());
		if (config.getEndpoint() == null) {
			configureRegion(builder, config.getRegion());
		}
	}
	
	private void configureAmazonS3ClientBuilder(Object builder) {
		try {
			if (Class.forName(S3_BUILDER).isAssignableFrom(builder.getClass())) {
				invokeMethod(builder, "setPathStyleAccessEnabled", awsS3ClientV1Properties.getPathStyleAccessEnabled());
				invokeMethod(builder, "setChunkedEncodingDisabled",
						awsS3ClientV1Properties.getChunkedEncodingDisabled());
				invokeMethod(builder, "setAccelerateModeEnabled", awsS3ClientV1Properties.getAccelerateModeEnabled());
				invokeMethod(builder, "setPayloadSigningEnabled", awsS3ClientV1Properties.getPayloadSigningEnabled());
				invokeMethod(builder, "setDualstackEnabled", awsS3ClientV1Properties.getDualstackEnabled());
				invokeMethod(builder, "setForceGlobalBucketAccessEnabled",
						awsS3ClientV1Properties.getForceGlobalBucketAccessEnabled());
			}
		} catch (ClassNotFoundException e) {
			log.debug(S3_BUILDER + " is not found in classpath -- ignored", e);
		}
		
		if (builderClass.getName().equals(ENCRYPTION_CLIENT_BUILDER)) {
			BeanFactory beanFactory = getBeanFactory();
			if (beanFactory != null && beanFactory.containsBean(ENCRYPTION_MATERIALS_PROVIDER)) {
				try {
					Object encryptionMaterial = beanFactory.getBean(ENCRYPTION_MATERIALS_PROVIDER);
					invokeMethod(builder, "setEncryptionMaterials", encryptionMaterial);
				} catch (IllegalStateException e) {
					log.warn(ENCRYPTION_MATERIALS_PROVIDER + " is not found in classpath -- ignored", e);
				}
			}
		}
	}
}
