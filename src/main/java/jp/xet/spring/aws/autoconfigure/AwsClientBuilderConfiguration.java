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

import static jp.xet.spring.aws.autoconfigure.InternalReflectionUtil.invokeMethod;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.amazonaws.ClientConfiguration;
import com.amazonaws.client.builder.AwsClientBuilder.EndpointConfiguration;

/**
 * Spring configuration class to configure AWS client builders.
 *
 * @author miyamoto.daisuke
 * @since #version#
 */
@Slf4j
@RequiredArgsConstructor
@Configuration
public class AwsClientBuilderConfiguration implements BeanFactoryAware {
	
	private static final String DEFAULT_NAME = "default";
	
	private static final String S3_BUILDER = "com.amazonaws.services.s3.AmazonS3Builder";
	
	private static final String ENCRYPTION_CLIENT_BUILDER = "com.amazonaws.services.s3.AmazonS3EncryptionClientBuilder";
	
	static final String ENCRYPTION_MATERIALS_PROVIDER = "com.amazonaws.services.s3.model.EncryptionMaterialsProvider";
	
	
	private static Optional<AwsClientProperties> getAwsClientProperties(
			Map<String, AwsClientProperties> stringAwsClientPropertiesMap, Class<?> clientClass) {
		try {
			String servicePackageName = clientClass.getPackage().getName()
				.substring("com.amazonaws.services.".length())
				.replace('.', '-');
			
			if (clientClass.getName().endsWith("Async")) {
				AwsClientProperties asyncProperties = stringAwsClientPropertiesMap.get(servicePackageName + "-async");
				if (asyncProperties != null) {
					return Optional.of(asyncProperties);
				}
			}
			AwsClientProperties serviceProperties = stringAwsClientPropertiesMap.get(servicePackageName);
			if (serviceProperties != null) {
				return Optional.of(serviceProperties);
			}
			return Optional.ofNullable(stringAwsClientPropertiesMap.get(DEFAULT_NAME));
		} catch (IndexOutOfBoundsException e) {
			log.error("Failed to get property name: {}", clientClass);
			throw e;
		}
	}
	
	
	private ConfigurableBeanFactory beanFactory;
	
	
	@Override
	public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
		this.beanFactory = (ConfigurableBeanFactory) beanFactory;
	}
	
	boolean isConfigurable(String builderClassName) {
		if (builderClassName.equals(ENCRYPTION_CLIENT_BUILDER)
				&& beanFactory.containsBean(ENCRYPTION_MATERIALS_PROVIDER) == false) {
			log.debug("Skip " + ENCRYPTION_CLIENT_BUILDER + " -- " + ENCRYPTION_MATERIALS_PROVIDER
					+ " is not configured");
			return false;
		}
		return true;
	}
	
	void configureBuilder(String builderClassName, Class<?> clientClass, Object builder) {
		if (builderClassName.startsWith("com.amazonaws.services.s3.")) {
			configureAmazonS3ClientBuilder(builderClassName, builder);
		}
		
		Map<String, AwsClientProperties> map = awsClientPropertiesMap();
		Optional<AwsClientProperties> specificConfig = getAwsClientProperties(map, clientClass);
		Optional<AwsClientProperties> defaultConfig = Optional.ofNullable(map.get(DEFAULT_NAME));
		
		ClientConfiguration clientConfiguration = specificConfig.map(AwsClientProperties::getClient)
			.orElseGet(() -> defaultConfig.map(AwsClientProperties::getClient).orElse(null));
		AwsClientUtil.configureClientConfiguration(builder, clientConfiguration);
		
		EndpointConfiguration endpointConfiguration = specificConfig.map(AwsClientProperties::getEndpoint)
			.orElseGet(() -> defaultConfig.map(AwsClientProperties::getEndpoint).orElse(null));
		if (endpointConfiguration != null) {
			AwsClientUtil.configureEndpointConfiguration(builder, endpointConfiguration);
			return;
		}
		
		String region = specificConfig.map(AwsClientProperties::getRegion)
			.orElseGet(() -> defaultConfig.map(AwsClientProperties::getRegion).orElse(null));
		if (region != null) {
			AwsClientUtil.configureRegion(builder, region);
		}
		
		boolean enabled = specificConfig.map(AwsClientProperties::isEnabled)
			.orElseGet(() -> defaultConfig.map(AwsClientProperties::isEnabled).orElse(true));
		if (enabled == false) {
			throw new ClientDisabledException();
		}
	}
	
	private void configureAmazonS3ClientBuilder(String builderClassName, Object builder) {
		try {
			if (Class.forName(S3_BUILDER).isAssignableFrom(builder.getClass())) {
				AwsS3ClientProperties s3Config = awsS3ClientProperties();
				invokeMethod(builder, "setPathStyleAccessEnabled", s3Config.getPathStyleAccessEnabled());
				invokeMethod(builder, "setChunkedEncodingDisabled", s3Config.getChunkedEncodingDisabled());
				invokeMethod(builder, "setAccelerateModeEnabled", s3Config.getAccelerateModeEnabled());
				invokeMethod(builder, "setPayloadSigningEnabled", s3Config.getPayloadSigningEnabled());
				invokeMethod(builder, "setDualstackEnabled", s3Config.getDualstackEnabled());
				invokeMethod(builder, "setForceGlobalBucketAccessEnabled",
						s3Config.getForceGlobalBucketAccessEnabled());
			}
		} catch (ClassNotFoundException e) {
			log.warn(S3_BUILDER + " is not found in classpath -- ignored", e);
		}
		
		if (builderClassName.equals(ENCRYPTION_CLIENT_BUILDER)
				&& beanFactory.containsBean(ENCRYPTION_MATERIALS_PROVIDER)) {
			try {
				Object encryptionMaterial = beanFactory.getBean(ENCRYPTION_MATERIALS_PROVIDER);
				invokeMethod(builder, "setEncryptionMaterials", encryptionMaterial);
			} catch (IllegalStateException e) {
				log.warn(ENCRYPTION_MATERIALS_PROVIDER + " is not found in classpath -- ignored", e);
			}
		}
	}
	
	@Bean
	@ConfigurationProperties(value = "aws", ignoreInvalidFields = true)
	public Map<String, AwsClientProperties> awsClientPropertiesMap() {
		return new HashMap<>();
	}
	
	@Bean
	public AwsS3ClientProperties awsS3ClientProperties() {
		return new AwsS3ClientProperties();
	}
	
	
	@Data
	private static class AwsClientProperties {
		
		private ClientConfiguration client;
		
		private MutableEndpointConfiguration endpoint;
		
		private String region;
		
		private boolean enabled = true;
		
		
		private EndpointConfiguration getEndpoint() {
			return endpoint == null ? null : endpoint.toEndpointConfiguration();
		}
	}
	
	/**
	 * @see <a href="https://github.com/spring-projects/spring-boot/issues/8762">spring-boot#8762</a>
	 */
	@Data
	private static class MutableEndpointConfiguration {
		
		private String serviceEndpoint;
		
		private String signingRegion;
		
		
		EndpointConfiguration toEndpointConfiguration() {
			if (serviceEndpoint != null) {
				return new EndpointConfiguration(serviceEndpoint, signingRegion);
			}
			return null;
		}
	}
	
	@Data
	@ConfigurationProperties(value = "aws.s3", ignoreInvalidFields = true)
	private static class AwsS3ClientProperties {
		
		private Boolean pathStyleAccessEnabled;
		
		private Boolean chunkedEncodingDisabled;
		
		private Boolean accelerateModeEnabled;
		
		private Boolean payloadSigningEnabled;
		
		private Boolean dualstackEnabled;
		
		private Boolean forceGlobalBucketAccessEnabled;
		
	}
}
