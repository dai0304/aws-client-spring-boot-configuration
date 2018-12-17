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
import software.amazon.awssdk.core.client.config.ClientOverrideConfiguration;
import software.amazon.awssdk.http.SdkHttpClient;
import software.amazon.awssdk.http.apache.ApacheHttpClient;
import software.amazon.awssdk.http.apache.ProxyConfiguration;
import software.amazon.awssdk.http.async.SdkAsyncHttpClient;
import software.amazon.awssdk.http.nio.netty.NettyNioAsyncHttpClient;
import software.amazon.awssdk.http.nio.netty.SdkEventLoopGroup;

/**
 * Spring factory bean class of AWS client v2.
 *
 * @param <T> type of AWS client v2
 * @author miyamoto.daisuke
 */
@Slf4j
@RequiredArgsConstructor
class AwsClientV2FactoryBean<T>extends AbstractFactoryBean<T> {
	
	private static final String S3_CLIENT = "software.amazon.awssdk.services.s3.S3Client";
	
	private static final String S3_CONFIG = "software.amazon.awssdk.services.s3.S3Configuration";
	
	
	private static AwsClientV2Properties getAwsClientProperties(
			Map<String, AwsClientV2Properties> map, Class<?> clientClass) {
		try {
			String servicePackageName = clientClass.getPackage().getName()
				.substring("software.amazon.awssdk.services.".length())
				.replace('.', '-');
			String serviceNameSuffix = clientClass.getName().endsWith("AsyncClient") ? "-async" : "";
			
			return map.get(servicePackageName + serviceNameSuffix);
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
		Object builder = createBuilder(clientClass);
		configureBuilder(builder);
		return build(builder);
	}
	
	private void configureBuilder(Object builder) {
		BeanFactory beanFactory = getBeanFactory();
		if (beanFactory == null) {
			return;
		}
		
		if (clientClass.getName().equals(S3_CLIENT)) {
			configureAmazonS3ClientBuilder(builder);
		}
		
		AwsClientV2Properties config = getAwsClientProperties(awsClientV2PropertiesMap, clientClass);
		if (config == null) {
			return;
		}
		
		Optional.ofNullable(config.getEndpoint())
			.ifPresent(endpoint -> configureEndpoint(builder, endpoint));
		
		Optional.ofNullable(config.getRegion())
			.ifPresent(region -> configureRegion(builder, region));
		
		configureSdkHttpClientBuilder(builder, config, beanFactory);
		
		Optional.ofNullable(config.getCredentialsProviderBeanName())
			.ifPresent(credentialsProviderBeanName -> {
				AwsCredentialsProvider credentialsProvider =
						beanFactory.getBean(credentialsProviderBeanName, AwsCredentialsProvider.class);
				configureCredentialsProvider(builder, credentialsProvider);
			});
		
		Optional.ofNullable(config.getClientOverrideConfigurationBeanName())
			.ifPresent(clientOverrideConfigurationBeanName -> {
				ClientOverrideConfiguration clientOverrideConfiguration =
						beanFactory.getBean(clientOverrideConfigurationBeanName, ClientOverrideConfiguration.class);
				configureClientOverrideConfiguration(builder, clientOverrideConfiguration);
			});
		
		Optional.ofNullable(config.getHttpClientBeanName())
			.ifPresent(httpClientBeanName -> {
				SdkHttpClient sdkHttpClient = beanFactory.getBean(httpClientBeanName, SdkHttpClient.class);
				configureHttpClient(builder, sdkHttpClient);
			});
	}
	
	private void configureSdkHttpClientBuilder(Object builder, AwsClientV2Properties config, BeanFactory beanFactory) {
		if (builder instanceof SdkSyncClientBuilder) {
			Optional.ofNullable(config.getApacheHttpClientBuilder())
				.map(sdkClientConfig -> {
					ApacheHttpClient.Builder apacheHttpClientBuilder = ApacheHttpClient.builder()
						.socketTimeout(sdkClientConfig.getSocketTimeout())
						.connectionTimeout(sdkClientConfig.getConnectionTimeout())
						.maxConnections(sdkClientConfig.getMaxConnections())
						.expectContinueEnabled(sdkClientConfig.getExpectContinueEnabled())
						.connectionTimeToLive(sdkClientConfig.getConnectionTimeToLive())
						.connectionMaxIdleTime(sdkClientConfig.getConnectionMaxIdleTime())
						.useIdleConnectionReaper(sdkClientConfig.getUseIdleConnectionReaper());
					Optional.ofNullable(sdkClientConfig.getConnectionAcquisitionTimeout())
						.ifPresent(apacheHttpClientBuilder::connectionAcquisitionTimeout);
					if (sdkClientConfig.getProxyConfiguration() != null) {
						apacheHttpClientBuilder.proxyConfiguration(ProxyConfiguration.builder()
							.endpoint(sdkClientConfig.getProxyConfiguration().getEndpoint())
							.username(sdkClientConfig.getProxyConfiguration().getUsername())
							.password(sdkClientConfig.getProxyConfiguration().getPassword())
							.ntlmDomain(sdkClientConfig.getProxyConfiguration().getNtlmDomain())
							.ntlmWorkstation(sdkClientConfig.getProxyConfiguration().getNtlmWorkstation())
							.preemptiveBasicAuthenticationEnabled(
									sdkClientConfig.getProxyConfiguration().getPreemptiveBasicAuthenticationEnabled())
							.useSystemPropertyValues(
									sdkClientConfig.getProxyConfiguration().getUseSystemPropertyValues())
							.build());
					}
					return apacheHttpClientBuilder;
				})
				.ifPresent(sdkHttpClientBuilder -> configureHttpSyncClientBuilder(builder, sdkHttpClientBuilder));
		}
		
		if (builder instanceof SdkAsyncClientBuilder) {
			Optional.ofNullable(config.getNettyNioAsyncHttpClientBuilder())
				.map(sdkClientConfig -> {
					NettyNioAsyncHttpClient.Builder nettyNioAsyncHttpClientBuilder = NettyNioAsyncHttpClient.builder()
						.maxConcurrency(sdkClientConfig.getMaxConcurrency())
						.maxPendingConnectionAcquires(sdkClientConfig.getMaxPendingConnectionAcquires())
						.protocol(sdkClientConfig.getProtocol())
						.maxHttp2Streams(sdkClientConfig.getMaxHttp2Streams());
					Optional.ofNullable(sdkClientConfig.getReadTimeout())
						.ifPresent(nettyNioAsyncHttpClientBuilder::readTimeout);
					Optional.ofNullable(sdkClientConfig.getWriteTimeout())
						.ifPresent(nettyNioAsyncHttpClientBuilder::writeTimeout);
					Optional.ofNullable(sdkClientConfig.getConnectionAcquisitionTimeout())
						.ifPresent(nettyNioAsyncHttpClientBuilder::connectionAcquisitionTimeout);
					Optional.ofNullable(sdkClientConfig.getConnectionTimeout())
						.ifPresent(nettyNioAsyncHttpClientBuilder::connectionTimeout);
					if (sdkClientConfig.getEventLoopGroupBeanName() != null) {
						SdkEventLoopGroup eventLoopGroup = beanFactory.getBean(
								sdkClientConfig.getEventLoopGroupBeanName(), SdkEventLoopGroup.class);
						nettyNioAsyncHttpClientBuilder.eventLoopGroup(eventLoopGroup);
					}
					if (sdkClientConfig.getEventLoopGroupBuilderBeanName() != null) {
						SdkEventLoopGroup.Builder eventLoopGroupBuilder = beanFactory.getBean(
								sdkClientConfig.getEventLoopGroupBeanName(), SdkEventLoopGroup.Builder.class);
						nettyNioAsyncHttpClientBuilder.eventLoopGroupBuilder(eventLoopGroupBuilder);
					}
					return nettyNioAsyncHttpClientBuilder;
				})
				.ifPresent(sdkHttpClientBuilder -> configureHttpAsyncClientBuilder(builder, sdkHttpClientBuilder));
		}
		
		Optional.ofNullable(config.getHttpClientBuilderBeanName())
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
