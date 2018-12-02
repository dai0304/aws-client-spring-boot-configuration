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

import java.util.Map;
import java.util.Optional;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.config.AbstractFactoryBean;

import com.amazonaws.ClientConfiguration;
import com.amazonaws.client.builder.AwsClientBuilder.EndpointConfiguration;

import jp.xet.springconfig.aws.v1.AwsClientV1Configuration.AwsClientV1Properties;
import jp.xet.springconfig.aws.v1.AwsClientV1Configuration.AwsS3ClientV1Properties;

/**
 * Spring configuration class to configure AWS client builders.
 *
 * @param <T> type of AWS client
 * @author miyamoto.daisuke
 */
@Slf4j
@RequiredArgsConstructor
class AwsClientV1FactoryBean<T>extends AbstractFactoryBean<T> {
	
	private static final String DEFAULT_NAME = "default";
	
	private static final String S3_BUILDER = "com.amazonaws.services.s3.AmazonS3Builder";
	
	private static final String ENCRYPTION_CLIENT_BUILDER = "com.amazonaws.services.s3.AmazonS3EncryptionClientBuilder";
	
	static final String ENCRYPTION_MATERIALS_PROVIDER =
			"com.amazonaws.services.s3.model.EncryptionMaterialsProvider";
	
	
	private static Optional<AwsClientV1Properties> getAwsClientProperties(
			Map<String, AwsClientV1Properties> stringAwsClientPropertiesMap, Class<?> clientClass) {
		try {
			String servicePackageName = clientClass.getPackage().getName()
				.substring("com.amazonaws.services.".length())
				.replace('.', '-');
			
			if (clientClass.getName().endsWith("Async")) {
				AwsClientV1Properties asyncProperties = stringAwsClientPropertiesMap.get(servicePackageName + "-async");
				if (asyncProperties != null) {
					return Optional.of(asyncProperties);
				}
			}
			AwsClientV1Properties serviceProperties = stringAwsClientPropertiesMap.get(servicePackageName);
			if (serviceProperties != null) {
				return Optional.of(serviceProperties);
			}
			return Optional.ofNullable(stringAwsClientPropertiesMap.get(DEFAULT_NAME));
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
		Object builder = AwsClientV1Util.createBuilder(builderClass);
		configureBuilder(builder);
		return AwsClientV1Util.buildClient(builder);
	}
	
	private void configureBuilder(Object builder) {
		Optional<AwsClientV1Properties> specificConfig = getAwsClientProperties(awsClientV1PropertiesMap, clientClass);
		Optional<AwsClientV1Properties> defaultConfig = Optional.ofNullable(awsClientV1PropertiesMap.get(DEFAULT_NAME));
		
		ClientConfiguration clientConfiguration = specificConfig.map(AwsClientV1Properties::getClient)
			.orElseGet(() -> defaultConfig.map(AwsClientV1Properties::getClient).orElse(null));
		AwsClientV1Util.configureClientConfiguration(builder, clientConfiguration);
		
		EndpointConfiguration endpointConfiguration = specificConfig.map(AwsClientV1Properties::getEndpoint)
			.orElseGet(() -> defaultConfig.map(AwsClientV1Properties::getEndpoint).orElse(null));
		if (endpointConfiguration != null) {
			AwsClientV1Util.configureEndpointConfiguration(builder, endpointConfiguration);
		} else {
			String region = specificConfig.map(AwsClientV1Properties::getRegion)
				.orElseGet(() -> defaultConfig.map(AwsClientV1Properties::getRegion).orElse(null));
			if (region != null) {
				AwsClientV1Util.configureRegion(builder, region);
			}
		}
		
		if (builderClass.getName().startsWith("com.amazonaws.services.s3.")) {
			configureAmazonS3ClientBuilder(builder);
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
