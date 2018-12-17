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
package jp.xet.springconfig.aws.v2; // NOPMD CouplingBetweenObjects

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;

import lombok.extern.slf4j.Slf4j;

import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;

import org.junit.Test;

import software.amazon.awssdk.core.client.config.SdkClientConfiguration;
import software.amazon.awssdk.core.client.config.SdkClientOption;
import software.amazon.awssdk.http.SdkHttpConfigurationOption;
import software.amazon.awssdk.http.apache.ApacheHttpClient;
import software.amazon.awssdk.http.apache.internal.ApacheHttpRequestConfig;
import software.amazon.awssdk.http.nio.netty.NettyNioAsyncHttpClient;
import software.amazon.awssdk.http.nio.netty.internal.NettyConfiguration;
import software.amazon.awssdk.services.ec2.Ec2AsyncClient;
import software.amazon.awssdk.services.ec2.Ec2Client;
import software.amazon.awssdk.services.sqs.SqsAsyncClient;
import software.amazon.awssdk.services.sqs.SqsClient;

/**
 * Test for all AWS SDK v2 configuration.
 *
 * @author miyamoto.daisuke
 * @since #version#
 */
@Slf4j
public class AwsV2ConfigurationTest_SdkClientConfig {
	
	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
		.withConfiguration(AutoConfigurations.of(AwsClientV2Configuration.class));
	
	
	@Configuration
	@EnableAwsClientV2({
		Ec2Client.class,
		Ec2AsyncClient.class,
		SqsClient.class,
		SqsAsyncClient.class
	})
	@EnableConfigurationProperties
	static class ExampleSdkHttpClientBuilderConfiguration {
	}
	
	
	@Test
	public void sdkHttpClientBuilderConfiguration() {
		this.contextRunner.withUserConfiguration(ExampleSdkHttpClientBuilderConfiguration.class)
			.withPropertyValues("aws2.ec2.apache-http-client-builder.socket-timeout=15s")
			.withPropertyValues("aws2.ec2.apache-http-client-builder.connection-timeout=1s")
			.withPropertyValues("aws2.ec2.apache-http-client-builder.max-connections=60")
			.run(context -> {
				assertThat(context.getBean(Ec2Client.class)).satisfies(client -> {
					SdkClientConfiguration clientConfiguration = TestUtil.extractClientConfig(client);
					assertThat(clientConfiguration.option(SdkClientOption.SYNC_HTTP_CLIENT))
						.isInstanceOfSatisfying(ApacheHttpClient.class, this::isConfiguredClient);
				});
				assertThat(context.getBean(Ec2AsyncClient.class)).satisfies(client -> {
					SdkClientConfiguration clientConfiguration = TestUtil.extractClientConfig(client);
					assertThat(clientConfiguration.option(SdkClientOption.ASYNC_HTTP_CLIENT))
						.isInstanceOfSatisfying(NettyNioAsyncHttpClient.class, this::isDefaultClient);
				});
				assertThat(context.getBean(SqsClient.class)).satisfies(client -> {
					SdkClientConfiguration clientConfiguration = TestUtil.extractClientConfig(client);
					assertThat(clientConfiguration.option(SdkClientOption.SYNC_HTTP_CLIENT))
						.isInstanceOfSatisfying(ApacheHttpClient.class, this::isDefaultClient);
				});
				assertThat(context.getBean(SqsAsyncClient.class)).satisfies(client -> {
					SdkClientConfiguration clientConfiguration = TestUtil.extractClientConfig(client);
					assertThat(clientConfiguration.option(SdkClientOption.ASYNC_HTTP_CLIENT))
						.isInstanceOfSatisfying(NettyNioAsyncHttpClient.class, this::isDefaultClient);
				});
			});
	}
	
	@Test
	public void sdkHttpClientBuilderDefaultConfiguration() {
		this.contextRunner.withUserConfiguration(ExampleSdkHttpClientBuilderConfiguration.class)
			.withPropertyValues("aws2.ec2.apache-http-client-builder.socket-timeout=15s")
			.withPropertyValues("aws2.ec2.apache-http-client-builder.connection-timeout=1s")
			.withPropertyValues("aws2.ec2.apache-http-client-builder.max-connections=60")
			.withPropertyValues("aws2.ec2-async.apache-http-client-builder.socket-timeout=15s")
			.withPropertyValues("aws2.ec2-async.apache-http-client-builder.connection-timeout=1s")
			.withPropertyValues("aws2.ec2-async.apache-http-client-builder.max-connections=60")
			.withPropertyValues("aws2.sqs.apache-http-client-builder.socket-timeout=15s")
			.withPropertyValues("aws2.sqs.apache-http-client-builder.connection-timeout=1s")
			.withPropertyValues("aws2.sqs.apache-http-client-builder.max-connections=60")
			.withPropertyValues("aws2.sqs-async.apache-http-client-builder.socket-timeout=15s")
			.withPropertyValues("aws2.sqs-async.apache-http-client-builder.connection-timeout=1s")
			.withPropertyValues("aws2.sqs-async.apache-http-client-builder.max-connections=60")
			.run(context -> {
				assertThat(context.getBean(Ec2Client.class)).satisfies(client -> {
					SdkClientConfiguration clientConfiguration = TestUtil.extractClientConfig(client);
					assertThat(clientConfiguration.option(SdkClientOption.SYNC_HTTP_CLIENT))
						.isInstanceOfSatisfying(ApacheHttpClient.class, this::isConfiguredClient);
				});
				assertThat(context.getBean(Ec2AsyncClient.class)).satisfies(client -> {
					SdkClientConfiguration clientConfiguration = TestUtil.extractClientConfig(client);
					assertThat(clientConfiguration.option(SdkClientOption.ASYNC_HTTP_CLIENT))
						.isInstanceOfSatisfying(NettyNioAsyncHttpClient.class, this::isDefaultClient);
				});
				assertThat(context.getBean(SqsClient.class)).satisfies(client -> {
					SdkClientConfiguration clientConfiguration = TestUtil.extractClientConfig(client);
					assertThat(clientConfiguration.option(SdkClientOption.SYNC_HTTP_CLIENT))
						.isInstanceOfSatisfying(ApacheHttpClient.class, this::isConfiguredClient);
				});
				assertThat(context.getBean(SqsAsyncClient.class)).satisfies(client -> {
					SdkClientConfiguration clientConfiguration = TestUtil.extractClientConfig(client);
					assertThat(clientConfiguration.option(SdkClientOption.ASYNC_HTTP_CLIENT))
						.isInstanceOfSatisfying(NettyNioAsyncHttpClient.class, this::isDefaultClient);
				});
			});
	}
	
	@Test
	public void sdkHttpClientBuilderDefaultConfigurationOverride() {
		this.contextRunner.withUserConfiguration(ExampleSdkHttpClientBuilderConfiguration.class)
			.withPropertyValues("aws2.ec2-async.netty-nio-async-http-client-builder.max-concurrency=123")
			.withPropertyValues("aws2.sqs.apache-http-client-builder.socket-timeout=15s")
			.withPropertyValues("aws2.sqs.apache-http-client-builder.connection-timeout=1s")
			.withPropertyValues("aws2.sqs.apache-http-client-builder.max-connections=60")
			.withPropertyValues("aws2.sqs-async.netty-nio-async-http-client-builder.max-concurrency=123")
			.withPropertyValues("aws2.sqs-async.netty-nio-async-http-client-builder.read-timeout=40s")
			.withPropertyValues("aws2.sqs-async.netty-nio-async-http-client-builder.write-timeout=50s")
			.withPropertyValues("aws2.sqs-async.netty-nio-async-http-client-builder.connection-timeout=1s")
			.run(context -> {
				assertThat(context.getBean(Ec2Client.class)).satisfies(client -> {
					SdkClientConfiguration clientConfiguration = TestUtil.extractClientConfig(client);
					assertThat(clientConfiguration.option(SdkClientOption.SYNC_HTTP_CLIENT))
						.isInstanceOfSatisfying(ApacheHttpClient.class, this::isDefaultClient);
				});
				assertThat(context.getBean(Ec2AsyncClient.class)).satisfies(client -> {
					SdkClientConfiguration clientConfiguration = TestUtil.extractClientConfig(client);
					assertThat(clientConfiguration.option(SdkClientOption.ASYNC_HTTP_CLIENT))
						.isInstanceOfSatisfying(NettyNioAsyncHttpClient.class, this::isConfiguredEc2Client);
				});
				assertThat(context.getBean(SqsClient.class)).satisfies(client -> {
					SdkClientConfiguration clientConfiguration = TestUtil.extractClientConfig(client);
					assertThat(clientConfiguration.option(SdkClientOption.SYNC_HTTP_CLIENT))
						.isInstanceOfSatisfying(ApacheHttpClient.class, this::isConfiguredClient);
				});
				assertThat(context.getBean(SqsAsyncClient.class)).satisfies(client -> {
					SdkClientConfiguration clientConfiguration = TestUtil.extractClientConfig(client);
					assertThat(clientConfiguration.option(SdkClientOption.ASYNC_HTTP_CLIENT))
						.isInstanceOfSatisfying(NettyNioAsyncHttpClient.class, this::isConfiguredClient);
				});
			});
	}
	
	private void isDefaultClient(ApacheHttpClient c) {
		assertThat(TestUtil.extractRequestConfig(c))
			.returns(Duration.ofSeconds(30), ApacheHttpRequestConfig::socketTimeout)
			.returns(Duration.ofSeconds(2), ApacheHttpRequestConfig::connectionTimeout)
			.returns(Duration.ofSeconds(10), ApacheHttpRequestConfig::connectionAcquireTimeout)
			.returns(true, ApacheHttpRequestConfig::expectContinueEnabled);
		assertThat(TestUtil.extractResolvedOptions(c))
			.returns(50, m -> m.get(SdkHttpConfigurationOption.MAX_CONNECTIONS))
			.returns(10000, m -> m.get(SdkHttpConfigurationOption.MAX_PENDING_CONNECTION_ACQUIRES));
	}
	
	private void isConfiguredClient(ApacheHttpClient c) {
		assertThat(TestUtil.extractRequestConfig(c))
			.returns(Duration.ofSeconds(15), ApacheHttpRequestConfig::socketTimeout)
			.returns(Duration.ofSeconds(1), ApacheHttpRequestConfig::connectionTimeout)
			.returns(Duration.ofSeconds(10), ApacheHttpRequestConfig::connectionAcquireTimeout)
			.returns(true, ApacheHttpRequestConfig::expectContinueEnabled);
		assertThat(TestUtil.extractResolvedOptions(c))
			.returns(60, m -> m.get(SdkHttpConfigurationOption.MAX_CONNECTIONS))
			.returns(10000, m -> m.get(SdkHttpConfigurationOption.MAX_PENDING_CONNECTION_ACQUIRES));
	}
	
	private void isDefaultClient(NettyNioAsyncHttpClient c) {
		assertThat(TestUtil.extractConfig(c))
			.returns(2000, NettyConfiguration::connectTimeoutMillis)
			.returns(10000, NettyConfiguration::connectionAcquireTimeoutMillis)
			.returns(50, NettyConfiguration::maxConnections)
			.returns(30000, NettyConfiguration::readTimeoutMillis)
			.returns(30000, NettyConfiguration::writeTimeoutMillis);
	}
	
	private void isConfiguredClient(NettyNioAsyncHttpClient c) {
		assertThat(TestUtil.extractConfig(c))
			.returns(1000, NettyConfiguration::connectTimeoutMillis)
			.returns(10000, NettyConfiguration::connectionAcquireTimeoutMillis)
			.returns(123, NettyConfiguration::maxConnections)
			.returns(40000, NettyConfiguration::readTimeoutMillis)
			.returns(50000, NettyConfiguration::writeTimeoutMillis);
	}
	
	private void isConfiguredEc2Client(NettyNioAsyncHttpClient c) {
		assertThat(TestUtil.extractConfig(c))
			.returns(2000, NettyConfiguration::connectTimeoutMillis)
			.returns(10000, NettyConfiguration::connectionAcquireTimeoutMillis)
			.returns(123, NettyConfiguration::maxConnections)
			.returns(30000, NettyConfiguration::readTimeoutMillis)
			.returns(30000, NettyConfiguration::writeTimeoutMillis);
	}
}
