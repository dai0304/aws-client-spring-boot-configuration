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

import static jp.xet.springconfig.aws.InternalReflectionUtil.invokeMethod;

import java.net.URI;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.config.AbstractFactoryBean;

import jp.xet.springconfig.aws.v2.AwsClientV2Configuration.AwsClientV2Properties;
import jp.xet.springconfig.aws.v2.AwsClientV2Configuration.AwsS3ClientV2Properties;

import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.core.SdkClient;
import software.amazon.awssdk.core.ServiceConfiguration;
import software.amazon.awssdk.core.client.builder.SdkAsyncClientBuilder;
import software.amazon.awssdk.core.client.builder.SdkSyncClientBuilder;
import software.amazon.awssdk.core.client.config.ClientOverrideConfiguration;
import software.amazon.awssdk.http.SdkHttpClient;
import software.amazon.awssdk.http.async.SdkAsyncHttpClient;

/**
 * Spring configuration class to configure AWS client builders.
 *
 * @param <T> type of AWS client
 * @author miyamoto.daisuke
 */
@Slf4j
@RequiredArgsConstructor
class AwsClientV2FactoryBean<T>extends AbstractFactoryBean<T> {
	
	private static final String DEFAULT_NAME = "default";
	
	private static final String S3_CLIENT = "software.amazon.awssdk.services.s3.S3Client";
	
	private static final String S3_CONFIG = "software.amazon.awssdk.services.s3.S3Configuration";
	
	
	private static AwsClientV2Properties getAwsClientProperties(
			Map<String, AwsClientV2Properties> stringAwsClientPropertiesMap, Class<?> clientClass) {
		try {
			String servicePackageName = clientClass.getPackage().getName()
				.substring("software.amazon.awssdk.services.".length())
				.replace('.', '-');
			
			if (clientClass.getName().endsWith("AsyncClient")) {
				AwsClientV2Properties asyncProperties = stringAwsClientPropertiesMap.get(servicePackageName + "-async");
				if (asyncProperties != null) {
					return asyncProperties;
				}
			}
			AwsClientV2Properties serviceProperties = stringAwsClientPropertiesMap.get(servicePackageName);
			if (serviceProperties != null) {
				return serviceProperties;
			}
			return stringAwsClientPropertiesMap.get(DEFAULT_NAME);
		} catch (IndexOutOfBoundsException e) {
			log.error("Failed to get property name: {}", clientClass);
			throw e;
		}
	}
	
	
	private final Class<T> clientClass;
	
	private final Map<String, AwsClientV2Properties> awsClientV2PropertiesMap;
	
	private final AwsS3ClientV2Properties awsS3ClientV2Properties;
	
	
	@Override
	public Class<?> getObjectType() {
		return clientClass;
	}
	
	@Override
	protected T createInstance() throws Exception {
		Object builder = AwsClientV2Util.createBuilder(clientClass);
		
		if (clientClass.getName().equals(S3_CLIENT)) {
			configureAmazonS3ClientBuilder(builder);
		}
		
		AwsClientV2Properties specificConfig = getAwsClientProperties(awsClientV2PropertiesMap, clientClass);
		AwsClientV2Properties defaultConfig = awsClientV2PropertiesMap.get(DEFAULT_NAME);
		
		ClientOverrideConfiguration clientConfiguration =
				getValue(specificConfig, defaultConfig, AwsClientV2Properties::getClient);
		AwsClientV2Util.configureClientOverrideConfiguration(builder, clientConfiguration);
		
		URI endpoint = getValue(specificConfig, defaultConfig, AwsClientV2Properties::getEndpoint);
		if (endpoint != null) {
			AwsClientV2Util.configureEndpoint(builder, endpoint);
		}
		
		String region = getValue(specificConfig, defaultConfig, AwsClientV2Properties::getRegion);
		if (region != null) {
			AwsClientV2Util.configureRegion(builder, region);
		}
		
		String httpClientBuilderBeanName =
				getValue(specificConfig, defaultConfig, AwsClientV2Properties::getHttpClientBuilderBeanName);
		if (httpClientBuilderBeanName != null) {
			if (builder instanceof SdkSyncClientBuilder) {
				SdkHttpClient.Builder<?> sdkHttpClientBuilder =
						getBeanFactory().getBean(httpClientBuilderBeanName, SdkHttpClient.Builder.class);
				AwsClientV2Util.configureHttpSyncClientBuilder(builder, sdkHttpClientBuilder);
			}
			if (builder instanceof SdkAsyncClientBuilder) {
				SdkAsyncHttpClient.Builder<?> sdkHttpClientBuilder =
						getBeanFactory().getBean(httpClientBuilderBeanName, SdkAsyncHttpClient.Builder.class);
				AwsClientV2Util.configureHttpAsyncClientBuilder(builder, sdkHttpClientBuilder);
			}
		}
		
		String httpClientBeanName =
				getValue(specificConfig, defaultConfig, AwsClientV2Properties::getHttpClientBeanName);
		if (httpClientBeanName != null) {
			SdkHttpClient sdkHttpClient = getBeanFactory().getBean(httpClientBeanName, SdkHttpClient.class);
			AwsClientV2Util.configureHttpClient(builder, sdkHttpClient);
		}
		
		String credentialsProviderBeanName =
				getValue(specificConfig, defaultConfig, AwsClientV2Properties::getCredentialsProviderBeanName);
		if (credentialsProviderBeanName != null) {
			AwsCredentialsProvider credentialsProvider =
					getBeanFactory().getBean(credentialsProviderBeanName, AwsCredentialsProvider.class);
			AwsClientV2Util.configureCredentialsProvider(builder, credentialsProvider);
		}
		
		return AwsClientV2Util.build(builder);
	}
	
	private static <T> T getValue(AwsClientV2Properties specificConfig, AwsClientV2Properties defaultConfig,
			Function<AwsClientV2Properties, T> f) {
		return Optional.ofNullable(specificConfig).map(f)
			.orElseGet(() -> Optional.ofNullable(defaultConfig).map(f)
				.orElse(null));
	}
	
	@Override
	protected void destroyInstance(T instance) throws Exception {
		if (instance instanceof SdkClient) {
			((SdkClient) instance).close();
		}
	}
	
	private void configureAmazonS3ClientBuilder(Object builder) {
		try {
			Object configBuilder = AwsClientV2Util.createBuilder(Class.forName(S3_CONFIG));
			invokeMethod(configBuilder, "pathStyleAccessEnabled", awsS3ClientV2Properties.getPathStyleAccessEnabled());
			invokeMethod(configBuilder, "chunkedEncodingEnabled", awsS3ClientV2Properties.getChunkedEncodingEnabled());
			invokeMethod(configBuilder, "accelerateModeEnabled", awsS3ClientV2Properties.getAccelerateModeEnabled());
			invokeMethod(configBuilder, "dualstackEnabled", awsS3ClientV2Properties.getDualstackEnabled());
			ServiceConfiguration serviceConfiguration = AwsClientV2Util.build(configBuilder);
			AwsClientV2Util.configureServiceConfiguration(builder, serviceConfiguration);
		} catch (ClassNotFoundException e) {
			log.debug(S3_CONFIG + " is not found in classpath -- ignored", e);
		}
	}
}
