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
import static jp.xet.springconfig.aws.v2.AwsClientV2Util.build;
import static jp.xet.springconfig.aws.v2.AwsClientV2Util.configureClientOverrideConfiguration;
import static jp.xet.springconfig.aws.v2.AwsClientV2Util.configureCredentialsProvider;
import static jp.xet.springconfig.aws.v2.AwsClientV2Util.configureEndpoint;
import static jp.xet.springconfig.aws.v2.AwsClientV2Util.configureHttpAsyncClientBuilder;
import static jp.xet.springconfig.aws.v2.AwsClientV2Util.configureHttpClient;
import static jp.xet.springconfig.aws.v2.AwsClientV2Util.configureHttpSyncClientBuilder;
import static jp.xet.springconfig.aws.v2.AwsClientV2Util.configureRegion;
import static jp.xet.springconfig.aws.v2.AwsClientV2Util.configureServiceConfiguration;
import static jp.xet.springconfig.aws.v2.AwsClientV2Util.createBuilder;

import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.config.AbstractFactoryBean;

import jp.xet.springconfig.aws.v2.AwsClientV2Configuration.AwsClientV2Properties;
import jp.xet.springconfig.aws.v2.AwsClientV2Configuration.AwsS3ClientV2Properties;

import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.core.SdkClient;
import software.amazon.awssdk.core.ServiceConfiguration;
import software.amazon.awssdk.core.client.builder.SdkAsyncClientBuilder;
import software.amazon.awssdk.core.client.builder.SdkSyncClientBuilder;
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
	
	private static <T> Optional<T> getValue(AwsClientV2Properties specificConfig, AwsClientV2Properties defaultConfig,
			Function<AwsClientV2Properties, T> f) {
		T t = Optional.ofNullable(specificConfig).map(f)
			.orElseGet(() -> Optional.ofNullable(defaultConfig).map(f)
				.orElse(null));
		return Optional.ofNullable(t);
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
		Object builder = createBuilder(clientClass);
		configureBuilder(builder);
		return build(builder);
	}
	
	private void configureBuilder(Object builder) {
		AwsClientV2Properties specificConfig = getAwsClientProperties(awsClientV2PropertiesMap, clientClass);
		AwsClientV2Properties defaultConfig = awsClientV2PropertiesMap.get(DEFAULT_NAME);
		
		getValue(specificConfig, defaultConfig, AwsClientV2Properties::getClient)
			.ifPresent(clientConfiguration -> configureClientOverrideConfiguration(builder, clientConfiguration));
		
		getValue(specificConfig, defaultConfig, AwsClientV2Properties::getEndpoint)
			.ifPresent(endpoint -> configureEndpoint(builder, endpoint));
		
		getValue(specificConfig, defaultConfig, AwsClientV2Properties::getRegion)
			.ifPresent(region -> configureRegion(builder, region));
		
		BeanFactory beanFactory = getBeanFactory();
		if (beanFactory != null) {
			getValue(specificConfig, defaultConfig, AwsClientV2Properties::getHttpClientBuilderBeanName)
				.ifPresent(httpClientBuilderBeanName -> {
					if (builder instanceof SdkSyncClientBuilder) {
						SdkHttpClient.Builder<?> sdkHttpClientBuilder =
								beanFactory.getBean(httpClientBuilderBeanName, SdkHttpClient.Builder.class);
						configureHttpSyncClientBuilder(builder, sdkHttpClientBuilder);
					}
					if (builder instanceof SdkAsyncClientBuilder) {
						SdkAsyncHttpClient.Builder<?> sdkHttpClientBuilder =
								beanFactory.getBean(httpClientBuilderBeanName, SdkAsyncHttpClient.Builder.class);
						configureHttpAsyncClientBuilder(builder, sdkHttpClientBuilder);
					}
				});
			
			getValue(specificConfig, defaultConfig, AwsClientV2Properties::getHttpClientBeanName)
				.ifPresent(httpClientBeanName -> {
					SdkHttpClient sdkHttpClient = beanFactory.getBean(httpClientBeanName, SdkHttpClient.class);
					configureHttpClient(builder, sdkHttpClient);
				});
			
			getValue(specificConfig, defaultConfig, AwsClientV2Properties::getCredentialsProviderBeanName)
				.ifPresent(credentialsProviderBeanName -> {
					AwsCredentialsProvider credentialsProvider =
							beanFactory.getBean(credentialsProviderBeanName, AwsCredentialsProvider.class);
					configureCredentialsProvider(builder, credentialsProvider);
				});
		}
		
		if (clientClass.getName().equals(S3_CLIENT)) {
			configureAmazonS3ClientBuilder(builder);
		}
	}
	
	@Override
	protected void destroyInstance(T instance) throws Exception {
		if (instance instanceof SdkClient) {
			((SdkClient) instance).close();
		}
	}
	
	private void configureAmazonS3ClientBuilder(Object builder) {
		try {
			Object configBuilder = createBuilder(Class.forName(S3_CONFIG));
			invokeMethod(configBuilder, "pathStyleAccessEnabled", awsS3ClientV2Properties.getPathStyleAccessEnabled());
			invokeMethod(configBuilder, "chunkedEncodingEnabled", awsS3ClientV2Properties.getChunkedEncodingEnabled());
			invokeMethod(configBuilder, "accelerateModeEnabled", awsS3ClientV2Properties.getAccelerateModeEnabled());
			invokeMethod(configBuilder, "dualstackEnabled", awsS3ClientV2Properties.getDualstackEnabled());
			ServiceConfiguration serviceConfiguration = build(configBuilder);
			configureServiceConfiguration(builder, serviceConfiguration);
		} catch (ClassNotFoundException e) {
			log.debug(S3_CONFIG + " is not found in classpath -- ignored", e);
		}
	}
}
