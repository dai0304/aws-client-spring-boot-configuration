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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.net.URI;

import lombok.extern.slf4j.Slf4j;

import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import org.junit.Test;

import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.awscore.client.config.AwsClientOption;
import software.amazon.awssdk.core.SdkClient;
import software.amazon.awssdk.core.client.config.SdkClientConfiguration;
import software.amazon.awssdk.core.client.config.SdkClientOption;
import software.amazon.awssdk.http.SdkHttpClient;
import software.amazon.awssdk.http.apache.ApacheHttpClient;
import software.amazon.awssdk.http.async.SdkAsyncHttpClient;
import software.amazon.awssdk.http.nio.netty.NettyNioAsyncHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.ec2.Ec2AsyncClient;
import software.amazon.awssdk.services.ec2.Ec2Client;
import software.amazon.awssdk.services.mturk.MTurkClient;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.sns.SnsAsyncClient;
import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.awssdk.services.sqs.SqsAsyncClient;
import software.amazon.awssdk.services.sqs.SqsClient;

/**
 * Test for all AWS SDK v2 configuration.
 *
 * @author miyamoto.daisuke
 * @since #version#
 */
@Slf4j
public class AwsV2ConfigurationTest {
	
	private static final AwsCredentialsProvider MOCK_CREDENTIALS_PROVIDER = mock(AwsCredentialsProvider.class);
	
	private static final SdkAsyncHttpClient MOCK_SDK_ASYNC_HTTP_CLIENT = mock(SdkAsyncHttpClient.class);
	
	private static final SdkAsyncHttpClient MOCK_SDK_ASYNC_HTTP_CLIENT_FOR_EC2 = mock(SdkAsyncHttpClient.class);
	
	private static final SdkHttpClient MOCK_SDK_HTTP_CLIENT = mock(SdkHttpClient.class);
	
	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
		.withConfiguration(AutoConfigurations.of(AwsClientV2Configuration.class));
	
	
	@Configuration
	@EnableAwsClientV2({
		S3Client.class,
		SqsClient.class,
		SnsClient.class
	})
	@EnableConfigurationProperties
	static class ExampleS3SqsSnsConfiguration {
	}
	
	
	@Test
	public void defaultClient_SyncOnly() {
		this.contextRunner.withUserConfiguration(ExampleS3SqsSnsConfiguration.class).run(context -> {
			assertThat(context).hasSingleBean(S3Client.class)
				.getBeanNames(S3Client.class).containsExactlyInAnyOrder(S3Client.class.getName());
			assertThat(context).hasSingleBean(SqsClient.class)
				.getBeanNames(SqsClient.class).containsExactlyInAnyOrder(SqsClient.class.getName());
			assertThat(context).hasSingleBean(SnsClient.class)
				.getBeanNames(SnsClient.class).containsExactlyInAnyOrder(SnsClient.class.getName());
			assertThat(context).doesNotHaveBean(S3AsyncClient.class);
			assertThat(context).doesNotHaveBean(SqsAsyncClient.class);
			assertThat(context).doesNotHaveBean(SnsAsyncClient.class);
			assertThat(context).getBean(S3Client.class).satisfies(this::assertDefaultClientConfiguration);
			assertThat(context).getBean(SqsClient.class).satisfies(this::assertDefaultClientConfiguration);
			assertThat(context).getBean(SnsClient.class).satisfies(this::assertDefaultClientConfiguration);
			
			assertThat(context).doesNotHaveBean(MTurkClient.class);
			
			// not in classpath
			assertThat(context).doesNotHaveBean("software.amazon.awssdk.services.simpledb.SimpleDbClient");
		});
	}
	
	private void assertDefaultClientConfiguration(SdkClient client) {
		SdkClientConfiguration clientConfiguration = TestUtil.extractClientConfig(client);
		assertThat(clientConfiguration).isNotNull();
		assertThat(clientConfiguration.option(SdkClientOption.API_CALL_ATTEMPT_TIMEOUT)).isNull(); // default
	}
	
	
	@Configuration
	@EnableAwsClientV2({
		SnsClient.class,
		SnsAsyncClient.class
	})
	@EnableConfigurationProperties
	static class ExampleSyncAndAsyncSnsConfiguration {
	}
	
	
	@Test
	public void defaultClient_BothSyncAndAsync() {
		this.contextRunner.withUserConfiguration(ExampleSyncAndAsyncSnsConfiguration.class).run(context -> {
			assertThat(context).hasBean(SnsClient.class.getName());
			assertThat(context).hasBean(SnsAsyncClient.class.getName());
		});
	}
	
	
	@Configuration
	@EnableAwsClientV2(SnsAsyncClient.class)
	@EnableConfigurationProperties
	static class ExampleAsyncSnsOnlyConfiguration {
	}
	
	
	@Test
	public void defaultClient_AsyncOnly() {
		this.contextRunner.withUserConfiguration(ExampleAsyncSnsOnlyConfiguration.class).run(context -> {
			assertThat(context).doesNotHaveBean(SnsClient.class.getName());
			assertThat(context).hasBean(SnsAsyncClient.class.getName());
		});
	}
	
	@Test
	public void defaultConfigurationAndOverride() {
		this.contextRunner.withUserConfiguration(ExampleS3SqsSnsConfiguration.class)
			.withPropertyValues("aws2.default.region=ap-northeast-1")
			.withPropertyValues("aws2.sqs.region=eu-central-1")
			.withPropertyValues("aws2.sns.endpoint=http://localhost:60003")
			.run(context -> {
				assertThat(context.getBean(S3Client.class)).satisfies(client -> {
					SdkClientConfiguration clientConfiguration = TestUtil.extractClientConfig(client);
					assertThat(clientConfiguration.option(AwsClientOption.AWS_REGION))
						.isEqualTo(Region.AP_NORTHEAST_1); // aws2.default.region
					assertThat(clientConfiguration.option(SdkClientOption.ENDPOINT))
						.isEqualTo(URI.create("https://s3.ap-northeast-1.amazonaws.com")); // aws2.default.region
				});
				assertThat(context.getBean(SqsClient.class)).satisfies(client -> {
					SdkClientConfiguration clientConfiguration = TestUtil.extractClientConfig(client);
					assertThat(clientConfiguration.option(AwsClientOption.AWS_REGION))
						.isEqualTo(Region.EU_CENTRAL_1); // override by aws2.sqs.region
					assertThat(clientConfiguration.option(SdkClientOption.ENDPOINT))
						.isEqualTo(URI.create("https://sqs.eu-central-1.amazonaws.com")); // aws2.sqs.region
				});
				assertThat(context.getBean(SnsClient.class)).satisfies(client -> {
					SdkClientConfiguration clientConfiguration = TestUtil.extractClientConfig(client);
					assertThat(clientConfiguration.option(AwsClientOption.AWS_REGION))
						.isEqualTo(Region.AP_NORTHEAST_1); // aws2.default.region
					assertThat(clientConfiguration.option(SdkClientOption.ENDPOINT))
						.isEqualTo(URI.create("http://localhost:60003")); // aws2.sns.endpoint
				});
			});
	}
	
	
	@Configuration
	@EnableAwsClientV2(Ec2Client.class)
	@EnableConfigurationProperties
	static class ExampleCredentialsProviderConfiguration {
		
		@Bean
		public AwsCredentialsProvider exampleAwsCredentialsProvider() {
			return MOCK_CREDENTIALS_PROVIDER;
		}
	}
	
	
	@Test
	public void credentialsProviderConfiguration() {
		this.contextRunner.withUserConfiguration(ExampleCredentialsProviderConfiguration.class)
			.withPropertyValues("aws2.ec2.credentials-provider-bean-name=exampleAwsCredentialsProvider")
			.run(context -> {
				assertThat(context.getBean(Ec2Client.class)).satisfies(client -> {
					SdkClientConfiguration clientConfiguration = TestUtil.extractClientConfig(client);
					assertThat(clientConfiguration.option(AwsClientOption.CREDENTIALS_PROVIDER))
						.isEqualTo(MOCK_CREDENTIALS_PROVIDER);
				});
			});
	}
	
	
	@Configuration
	@EnableAwsClientV2({
		Ec2Client.class,
		Ec2AsyncClient.class,
		SqsClient.class,
		SqsAsyncClient.class
	})
	@EnableConfigurationProperties
	static class ExampleSdkHttpClientBuilderConfiguration {
		
		@Bean
		public SdkHttpClient.Builder<?> exampleSdkHttpClientBuilder() {
			SdkHttpClient.Builder<?> builder = mock(SdkHttpClient.Builder.class);
			when(builder.buildWithDefaults(any())).thenReturn(MOCK_SDK_HTTP_CLIENT);
			return builder;
		}
		
		@Bean
		public SdkAsyncHttpClient.Builder<?> exampleSdkAsyncHttpClientBuilder() {
			SdkAsyncHttpClient.Builder<?> builder = mock(SdkAsyncHttpClient.Builder.class);
			when(builder.buildWithDefaults(any())).thenReturn(MOCK_SDK_ASYNC_HTTP_CLIENT);
			return builder;
		}
		
		@Bean
		public SdkAsyncHttpClient.Builder<?> exampleSdkAsyncHttpClientBuilderForEc2() {
			SdkAsyncHttpClient.Builder<?> builder = mock(SdkAsyncHttpClient.Builder.class);
			when(builder.buildWithDefaults(any())).thenReturn(MOCK_SDK_ASYNC_HTTP_CLIENT_FOR_EC2);
			return builder;
		}
	}
	
	
	@Test
	public void sdkHttpClientBuilderConfiguration() {
		this.contextRunner.withUserConfiguration(ExampleSdkHttpClientBuilderConfiguration.class)
			.withPropertyValues("aws2.ec2.http-client-builder-bean-name=exampleSdkHttpClientBuilder")
			.run(context -> {
				assertThat(context.getBean(Ec2Client.class)).satisfies(client -> {
					SdkClientConfiguration clientConfiguration = TestUtil.extractClientConfig(client);
					assertThat(clientConfiguration.option(SdkClientOption.SYNC_HTTP_CLIENT))
						.isSameAs(MOCK_SDK_HTTP_CLIENT);
				});
				assertThat(context.getBean(Ec2AsyncClient.class)).satisfies(client -> {
					SdkClientConfiguration clientConfiguration = TestUtil.extractClientConfig(client);
					assertThat(clientConfiguration.option(SdkClientOption.ASYNC_HTTP_CLIENT))
						.isInstanceOf(NettyNioAsyncHttpClient.class);
				});
				assertThat(context.getBean(SqsClient.class)).satisfies(client -> {
					SdkClientConfiguration clientConfiguration = TestUtil.extractClientConfig(client);
					assertThat(clientConfiguration.option(SdkClientOption.SYNC_HTTP_CLIENT))
						.isInstanceOf(ApacheHttpClient.class);
				});
				assertThat(context.getBean(SqsAsyncClient.class)).satisfies(client -> {
					SdkClientConfiguration clientConfiguration = TestUtil.extractClientConfig(client);
					assertThat(clientConfiguration.option(SdkClientOption.ASYNC_HTTP_CLIENT))
						.isInstanceOf(NettyNioAsyncHttpClient.class);
				});
			});
	}
	
	@Test
	public void sdkHttpClientBuilderDefaultConfiguration() {
		this.contextRunner.withUserConfiguration(ExampleSdkHttpClientBuilderConfiguration.class)
			.withPropertyValues("aws2.ec2.http-client-builder-bean-name=exampleSdkHttpClientBuilder")
			.withPropertyValues("aws2.ec2-async.http-client-builder-bean-name=exampleSdkAsyncHttpClientBuilder")
			.withPropertyValues("aws2.sqs.http-client-builder-bean-name=exampleSdkHttpClientBuilder")
			.withPropertyValues("aws2.sqs-async.http-client-builder-bean-name=exampleSdkAsyncHttpClientBuilder")
			.run(context -> {
				assertThat(context.getBean(Ec2Client.class)).satisfies(client -> {
					SdkClientConfiguration clientConfiguration = TestUtil.extractClientConfig(client);
					assertThat(clientConfiguration.option(SdkClientOption.SYNC_HTTP_CLIENT))
						.isSameAs(MOCK_SDK_HTTP_CLIENT);
				});
				assertThat(context.getBean(Ec2AsyncClient.class)).satisfies(client -> {
					SdkClientConfiguration clientConfiguration = TestUtil.extractClientConfig(client);
					assertThat(clientConfiguration.option(SdkClientOption.ASYNC_HTTP_CLIENT))
						.isSameAs(MOCK_SDK_ASYNC_HTTP_CLIENT);
				});
				assertThat(context.getBean(SqsClient.class)).satisfies(client -> {
					SdkClientConfiguration clientConfiguration = TestUtil.extractClientConfig(client);
					assertThat(clientConfiguration.option(SdkClientOption.SYNC_HTTP_CLIENT))
						.isSameAs(MOCK_SDK_HTTP_CLIENT);
				});
				assertThat(context.getBean(SqsAsyncClient.class)).satisfies(client -> {
					SdkClientConfiguration clientConfiguration = TestUtil.extractClientConfig(client);
					assertThat(clientConfiguration.option(SdkClientOption.ASYNC_HTTP_CLIENT))
						.isSameAs(MOCK_SDK_ASYNC_HTTP_CLIENT);
				});
			});
	}
	
	@Test
	public void sdkHttpClientBuilderDefaultConfigurationOverride() {
		this.contextRunner.withUserConfiguration(ExampleSdkHttpClientBuilderConfiguration.class)
			.withPropertyValues("aws2.ec2.http-client-builder-bean-name=exampleSdkHttpClientBuilder")
			.withPropertyValues("aws2.ec2-async.http-client-builder-bean-name=exampleSdkAsyncHttpClientBuilderForEc2")
			.withPropertyValues("aws2.sqs.http-client-builder-bean-name=exampleSdkHttpClientBuilder")
			.withPropertyValues("aws2.sqs-async.http-client-builder-bean-name=exampleSdkAsyncHttpClientBuilder")
			.run(context -> {
				assertThat(context.getBean(Ec2Client.class)).satisfies(client -> {
					SdkClientConfiguration clientConfiguration = TestUtil.extractClientConfig(client);
					assertThat(clientConfiguration.option(SdkClientOption.SYNC_HTTP_CLIENT))
						.isSameAs(MOCK_SDK_HTTP_CLIENT);
				});
				assertThat(context.getBean(Ec2AsyncClient.class)).satisfies(client -> {
					SdkClientConfiguration clientConfiguration = TestUtil.extractClientConfig(client);
					assertThat(clientConfiguration.option(SdkClientOption.ASYNC_HTTP_CLIENT))
						.isSameAs(MOCK_SDK_ASYNC_HTTP_CLIENT_FOR_EC2);
				});
				assertThat(context.getBean(SqsClient.class)).satisfies(client -> {
					SdkClientConfiguration clientConfiguration = TestUtil.extractClientConfig(client);
					assertThat(clientConfiguration.option(SdkClientOption.SYNC_HTTP_CLIENT))
						.isSameAs(MOCK_SDK_HTTP_CLIENT);
				});
				assertThat(context.getBean(SqsAsyncClient.class)).satisfies(client -> {
					SdkClientConfiguration clientConfiguration = TestUtil.extractClientConfig(client);
					assertThat(clientConfiguration.option(SdkClientOption.ASYNC_HTTP_CLIENT))
						.isSameAs(MOCK_SDK_ASYNC_HTTP_CLIENT);
				});
			});
	}
	
	
	@Configuration
	@EnableAwsClientV2({
		SqsClient.class,
		SqsAsyncClient.class,
		SnsClient.class,
		SnsAsyncClient.class
	})
	@EnableConfigurationProperties
	static class ExampleSqsSnsSyncAsyncConfiguration {
	}
	
	
	@Test
	public void asyncConfiguration() {
		String defaultRegion = "eu-west-2";
		String sqsRegion = "sa-east-1";
		String sqsEndpoint = "http://localhost:60002";
		String snsRegion = "ca-central-1";
		String snsEndpoint = "http://localhost:60003";
		String snsAsyncRegion = "ap-south-1";
		String snsAsyncEndpoint = "http://localhost:60004";
		
		System.setProperty("aws.region", defaultRegion);
		this.contextRunner.withUserConfiguration(ExampleSqsSnsSyncAsyncConfiguration.class)
			.withPropertyValues("aws2.sqs.region=" + sqsRegion)
			.withPropertyValues("aws2.sqs.endpoint=" + sqsEndpoint)
			.withPropertyValues("aws2.sns.region=" + snsRegion)
			.withPropertyValues("aws2.sns.endpoint=" + snsEndpoint)
			.withPropertyValues("aws2.sns-async.region=" + snsAsyncRegion)
			.withPropertyValues("aws2.sns-async.endpoint=" + snsAsyncEndpoint)
			.run(context -> {
				assertThat(context.getBean(SqsClient.class.getName()))
					.satisfies(client -> {
						// use aws.sqs.* (present)
						SdkClientConfiguration clientConfiguration = TestUtil.extractClientConfig(client);
						assertThat(clientConfiguration.option(AwsClientOption.AWS_REGION))
							.isEqualTo(Region.of(sqsRegion));
						assertThat(clientConfiguration.option(SdkClientOption.ENDPOINT))
							.isEqualTo(URI.create(sqsEndpoint));
					});
				assertThat(context.getBean(SqsAsyncClient.class))
					.satisfies(client -> {
						// use aws.sqs-async.* (present) -> aws.default-async.*
						SdkClientConfiguration clientConfiguration = TestUtil.extractClientConfig(client);
						assertThat(clientConfiguration.option(AwsClientOption.AWS_REGION))
							.isEqualTo(Region.of(defaultRegion));
						assertThat(clientConfiguration.option(SdkClientOption.ENDPOINT))
							.isEqualTo(URI.create("https://sqs." + defaultRegion + ".amazonaws.com"));
					});
				assertThat(context.getBean(SnsClient.class.getName()))
					.satisfies(client -> {
						// use aws.sns.* (present)
						SdkClientConfiguration clientConfiguration = TestUtil.extractClientConfig(client);
						assertThat(clientConfiguration.option(AwsClientOption.AWS_REGION))
							.isEqualTo(Region.of(snsRegion));
						assertThat(clientConfiguration.option(SdkClientOption.ENDPOINT))
							.isEqualTo(URI.create(snsEndpoint));
					});
				assertThat(context.getBean(SnsAsyncClient.class))
					.satisfies(client -> {
						// use aws.sns-async.* (present)
						SdkClientConfiguration clientConfiguration = TestUtil.extractClientConfig(client);
						assertThat(clientConfiguration.option(AwsClientOption.AWS_REGION))
							.isEqualTo(Region.of(snsAsyncRegion));
						assertThat(clientConfiguration.option(SdkClientOption.ENDPOINT))
							.isEqualTo(URI.create(snsAsyncEndpoint));
					});
			});
		System.clearProperty("aws.region");
	}
	
	@Test
	public void s3Configurations_default() {
		this.contextRunner.withUserConfiguration(ExampleS3Configuration.class).run(context -> {
			assertThat(context.getBean(S3Client.class)).satisfies(client -> {
				SdkClientConfiguration clientConfiguration = TestUtil.extractClientConfig(client);
				assertThat(clientConfiguration.option(SdkClientOption.SERVICE_CONFIGURATION))
					.isInstanceOfSatisfying(S3Configuration.class, s3Config -> {
						assertThat(s3Config)
							.returns(false, S3Configuration::pathStyleAccessEnabled)
							.returns(true, S3Configuration::chunkedEncodingEnabled)
							.returns(false, S3Configuration::accelerateModeEnabled)
							.returns(false, S3Configuration::dualstackEnabled)
							.returns(true, S3Configuration::checksumValidationEnabled);
					});
			});
		});
	}
	
	
	@Configuration
	@EnableAwsClientV2(S3Client.class)
	@EnableConfigurationProperties
	static class ExampleS3Configuration {
	}
	
	
	@Test
	public void s3Configurations() {
		this.contextRunner.withUserConfiguration(ExampleS3Configuration.class)
			.withPropertyValues("aws2.s3.path-style-access-enabled=true")
			.withPropertyValues("aws2.s3.chunked-encoding-enabled=false")
			.withPropertyValues("aws2.s3.accelerate-mode-enabled=false")
			.withPropertyValues("aws2.s3.dualstack-enabled=true")
			.withPropertyValues("aws2.s3.checksum-validation-enabled=true")
			.run(context -> {
				assertThat(context.getBean(S3Client.class)).satisfies(client -> {
					SdkClientConfiguration clientConfiguration = TestUtil.extractClientConfig(client);
					assertThat(clientConfiguration.option(SdkClientOption.SERVICE_CONFIGURATION))
						.isInstanceOfSatisfying(S3Configuration.class, s3Config -> {
							assertThat(s3Config)
								.returns(true, S3Configuration::pathStyleAccessEnabled)
								.returns(false, S3Configuration::chunkedEncodingEnabled)
								.returns(false, S3Configuration::accelerateModeEnabled)
								.returns(true, S3Configuration::dualstackEnabled)
								.returns(true, S3Configuration::checksumValidationEnabled);
						});
				});
			});
	}
	
	
	@Configuration
	@EnableAwsClientV2(S3Client.class)
	@EnableConfigurationProperties
	static class ExampleUserConfiguration {
		
		@Bean("software.amazon.awssdk.services.s3.S3Client")
		public S3Client mockClient() {
			return mock(S3Client.class);
		}
	}
	
	
	@Test
	public void defaultServiceBacksOff() {
		this.contextRunner.withUserConfiguration(ExampleUserConfiguration.class).run(context -> {
			assertThat(context).hasSingleBean(S3Client.class).getBean(S3Client.class)
				.isSameAs(context.getBean(ExampleUserConfiguration.class).mockClient());
		});
	}
}
