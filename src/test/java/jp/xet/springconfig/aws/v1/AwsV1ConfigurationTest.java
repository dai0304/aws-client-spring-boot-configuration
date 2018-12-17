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
package jp.xet.springconfig.aws.v1; // NOPMD CouplingBetweenObjects

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import java.net.URI;

import lombok.extern.slf4j.Slf4j;

import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.util.ReflectionTestUtils;

import org.junit.Test;

import com.amazonaws.AmazonWebServiceClient;
import com.amazonaws.services.alexaforbusiness.AmazonAlexaForBusiness;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClient;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBStreams;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBStreamsClient;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.AmazonS3Encryption;
import com.amazonaws.services.s3.S3ClientOptions;
import com.amazonaws.services.s3.model.EncryptionMaterialsProvider;
import com.amazonaws.services.sns.AmazonSNS;
import com.amazonaws.services.sns.AmazonSNSAsync;
import com.amazonaws.services.sns.AmazonSNSAsyncClient;
import com.amazonaws.services.sns.AmazonSNSClient;
import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.AmazonSQSAsync;
import com.amazonaws.services.sqs.AmazonSQSAsyncClient;
import com.amazonaws.services.sqs.AmazonSQSClient;

/**
 * Test for all AWS SDK v1 configuration.
 *
 * @author miyamoto.daisuke
 * @since #version#
 */
@Slf4j
public class AwsV1ConfigurationTest {
	
	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
		.withConfiguration(AutoConfigurations.of(AwsClientV1Configuration.class));
	
	
	@Configuration
	@EnableAwsClientV1({
		AmazonS3.class,
		AmazonSQS.class,
		AmazonSNS.class
	})
	@EnableConfigurationProperties
	static class ExampleS3SqsSnsConfiguration {
	}
	
	
	@Test
	public void defaultClient_SyncOnly() {
		this.contextRunner.withUserConfiguration(ExampleS3SqsSnsConfiguration.class).run(context -> {
			assertThat(context).hasSingleBean(AmazonS3.class)
				.getBeanNames(AmazonS3.class).containsExactlyInAnyOrder(AmazonS3.class.getName());
			assertThat(context).hasSingleBean(AmazonSQS.class)
				.getBeanNames(AmazonSQS.class).containsExactlyInAnyOrder(AmazonSQS.class.getName());
			assertThat(context).hasSingleBean(AmazonSNS.class)
				.getBeanNames(AmazonSNS.class).containsExactlyInAnyOrder(AmazonSNS.class.getName());
			// assertThat(context).doesNotHaveBean(AmazonS3Async.class);
			assertThat(context).doesNotHaveBean(AmazonSQSAsync.class);
			assertThat(context).doesNotHaveBean(AmazonSNSAsync.class);
			assertThat(context).getBean(AmazonS3.class)
				.isInstanceOfSatisfying(AmazonS3Client.class, this::assertDefaultClientConfiguration);
			assertThat(context).getBean(AmazonSQS.class)
				.isInstanceOfSatisfying(AmazonSQSClient.class, this::assertDefaultClientConfiguration);
			assertThat(context).getBean(AmazonSNS.class)
				.isInstanceOfSatisfying(AmazonSNSClient.class, this::assertDefaultClientConfiguration);
			
			// not defined in the annotation
			assertThat(context).doesNotHaveBean(AmazonAlexaForBusiness.class);
			
			// not in classpath
			assertThat(context).doesNotHaveBean("com.amazonaws.services.simpledb.AmazonSimpleDB");
		});
	}
	
	private void assertDefaultClientConfiguration(AmazonWebServiceClient client) {
		assertThat(client.getClientConfiguration()).isNotNull();
		assertThat(client.getClientConfiguration().getSocketTimeout()).isEqualTo(50000); // default
	}
	
	
	@Configuration
	@EnableAwsClientV1({
		AmazonSNS.class,
		AmazonSNSAsync.class
	})
	@EnableConfigurationProperties
	static class ExampleSyncAndAsyncSnsConfiguration {
	}
	
	
	@Test
	public void defaultClient_BothSyncAndAsync() {
		this.contextRunner.withUserConfiguration(ExampleSyncAndAsyncSnsConfiguration.class).run(context -> {
			assertThat(context).hasBean(AmazonSNS.class.getName());
			assertThat(context).hasBean(AmazonSNSAsync.class.getName());
		});
	}
	
	
	@Configuration
	@EnableAwsClientV1(AmazonSNSAsync.class)
	@EnableConfigurationProperties
	static class ExampleAsyncSnsOnlyConfiguration {
	}
	
	
	@Test
	public void defaultClient_AsyncOnly() {
		this.contextRunner.withUserConfiguration(ExampleAsyncSnsOnlyConfiguration.class).run(context -> {
			assertThat(context).doesNotHaveBean(AmazonSNS.class.getName());
			assertThat(context).hasBean(AmazonSNSAsync.class.getName());
		});
	}
	
	@Test
	public void defaultConfigurationAndOverride() {
		this.contextRunner.withUserConfiguration(ExampleS3SqsSnsConfiguration.class)
			.withPropertyValues("aws1.default.client.socket-timeout=123")
			.withPropertyValues("aws1.default.region=us-east-1")
			.withPropertyValues("aws1.sqs.region=eu-central-1")
			.withPropertyValues("aws1.sns.client.socket-timeout=456")
			.run(context -> {
				assertThat(context.getBean(AmazonS3.class)).isInstanceOfSatisfying(AmazonS3Client.class, client -> {
					assertThat(client.getClientConfiguration().getSocketTimeout())
						.isEqualTo(123); // aws1.default.client.socket-timeout
					assertThat(client).hasFieldOrPropertyWithValue("endpoint",
							URI.create("https://s3.amazonaws.com")); // default
				});
				assertThat(context.getBean(AmazonSQS.class)).isInstanceOfSatisfying(AmazonSQSClient.class, client -> {
					assertThat(client.getClientConfiguration().getSocketTimeout())
						.isEqualTo(123); // aws1.default.client.socket-timeout
					assertThat(client).hasFieldOrPropertyWithValue("endpoint",
							URI.create("https://sqs.eu-central-1.amazonaws.com")); // aws1.sqs.region
				});
				assertThat(context.getBean(AmazonSNS.class)).isInstanceOfSatisfying(AmazonSNSClient.class, client -> {
					assertThat(client.getClientConfiguration().getSocketTimeout())
						.isEqualTo(456); // aws1.sns.client.socket-timeout
					assertThat(client).hasFieldOrPropertyWithValue("endpoint",
							URI.create("https://sns.us-east-1.amazonaws.com")); // aws1.default.region
				});
			});
	}
	
	
	@Configuration
	@EnableAwsClientV1({
		AmazonSQS.class,
		AmazonSQSAsync.class,
		AmazonSNS.class,
		AmazonSNSAsync.class
	})
	@EnableConfigurationProperties
	static class ExampleSqsSnsSyncAsyncConfiguration {
	}
	
	
	@Test
	public void asyncConfiguration() {
		int defaultAsyncSocketTimeout = 1;
		String defaultAsyncEndpoint = "http://localhost:60001";
		int sqsSocketTimeout = 2;
		String sqsEndpoint = "http://localhost:60002";
		int snsSocketTimeout = 3;
		String snsEndpoint = "http://localhost:60003";
		int snsAsyncSocketTimeout = 4;
		String snsAsyncEndpoint = "http://localhost:60004";
		this.contextRunner.withUserConfiguration(ExampleSqsSnsSyncAsyncConfiguration.class)
			.withPropertyValues("aws1.default-async.client.socket-timeout=" + defaultAsyncSocketTimeout)
			.withPropertyValues("aws1.default-async.endpoint.service-endpoint=" + defaultAsyncEndpoint)
			.withPropertyValues("aws1.sqs.client.socket-timeout=" + sqsSocketTimeout)
			.withPropertyValues("aws1.sqs.endpoint.service-endpoint=" + sqsEndpoint)
			.withPropertyValues("aws1.sns.client.socket-timeout=" + snsSocketTimeout)
			.withPropertyValues("aws1.sns.endpoint.service-endpoint=" + snsEndpoint)
			.withPropertyValues("aws1.sns-async.client.socket-timeout=" + snsAsyncSocketTimeout)
			.withPropertyValues("aws1.sns-async.endpoint.service-endpoint=" + snsAsyncEndpoint)
			.run(context -> {
				assertThat(context.getBean(AmazonSQS.class.getName()))
					.isInstanceOfSatisfying(AmazonSQSClient.class, client -> {
						// use aws.sqs.* (present)
						assertThat(client).hasFieldOrPropertyWithValue("endpoint", URI.create(sqsEndpoint));
						assertThat(client.getClientConfiguration().getSocketTimeout()).isEqualTo(sqsSocketTimeout);
					});
				assertThat(context.getBean(AmazonSQSAsync.class))
					.isInstanceOfSatisfying(AmazonSQSAsyncClient.class, client -> {
						// use aws.sqs-async.* (absent) -> aws.default-async.*
						assertThat(client).hasFieldOrPropertyWithValue("endpoint", URI.create(defaultAsyncEndpoint));
						assertThat(client.getClientConfiguration().getSocketTimeout())
							.isEqualTo(defaultAsyncSocketTimeout);
					});
				assertThat(context.getBean(AmazonSNS.class.getName()))
					.isInstanceOfSatisfying(AmazonSNSClient.class, client -> {
						// use aws.sns.* (present)
						assertThat(client).hasFieldOrPropertyWithValue("endpoint", URI.create(snsEndpoint));
						assertThat(client.getClientConfiguration().getSocketTimeout()).isEqualTo(snsSocketTimeout);
					});
				assertThat(context.getBean(AmazonSNSAsync.class))
					.isInstanceOfSatisfying(AmazonSNSAsyncClient.class, client -> {
						// use aws.sns-async.* (present)
						assertThat(client).hasFieldOrPropertyWithValue("endpoint", URI.create(snsAsyncEndpoint));
						assertThat(client.getClientConfiguration().getSocketTimeout())
							.isEqualTo(snsAsyncSocketTimeout);
					});
			});
	}
	
	
	@Configuration
	@EnableAwsClientV1(AmazonS3.class)
	@EnableConfigurationProperties
	static class ExampleS3Configuration {
	}
	
	
	@Test
	public void s3Configurations_default() {
		this.contextRunner.withUserConfiguration(ExampleS3Configuration.class).run(context -> {
			assertThat(context.getBean(AmazonS3.class)).isInstanceOfSatisfying(AmazonS3Client.class, client -> {
				S3ClientOptions clientOptions =
						(S3ClientOptions) ReflectionTestUtils.getField(client, "clientOptions");
				assertThat(clientOptions)
					.returns(false, S3ClientOptions::isPathStyleAccess)
					.returns(false, S3ClientOptions::isChunkedEncodingDisabled)
					.returns(false, S3ClientOptions::isAccelerateModeEnabled)
					.returns(false, S3ClientOptions::isPathStyleAccess)
					.returns(false, S3ClientOptions::isDualstackEnabled)
					.returns(false, S3ClientOptions::isForceGlobalBucketAccessEnabled);
			});
		});
	}
	
	@Test
	public void s3Configurations() {
		this.contextRunner.withUserConfiguration(ExampleS3Configuration.class)
			.withPropertyValues("aws1.s3.path-style-access-enabled=true")
			.withPropertyValues("aws1.s3.chunked-encoding-disabled=true")
			.withPropertyValues("aws1.s3.accelerate-mode-enabled=false")
			.withPropertyValues("aws1.s3.payload-signing-enabled=true")
			.withPropertyValues("aws1.s3.dualstack-enabled=true")
			.withPropertyValues("aws1.s3.force-global-bucket-access-enabled=true")
			.run(context -> {
				assertThat(context.getBean(AmazonS3.class)).isInstanceOfSatisfying(AmazonS3Client.class, client -> {
					S3ClientOptions clientOptions =
							(S3ClientOptions) ReflectionTestUtils.getField(client, "clientOptions");
					assertThat(clientOptions)
						.returns(true, S3ClientOptions::isPathStyleAccess)
						.returns(true, S3ClientOptions::isChunkedEncodingDisabled)
						.returns(false, S3ClientOptions::isAccelerateModeEnabled)
						.returns(true, S3ClientOptions::isPathStyleAccess)
						.returns(true, S3ClientOptions::isDualstackEnabled)
						.returns(true, S3ClientOptions::isForceGlobalBucketAccessEnabled);
				});
			});
	}
	
	
	@Configuration
	@EnableAwsClientV1({
		AmazonDynamoDB.class,
		AmazonDynamoDBStreams.class,
		com.amazonaws.services.elasticloadbalancing.AmazonElasticLoadBalancing.class,
		com.amazonaws.services.elasticloadbalancingv2.AmazonElasticLoadBalancing.class
	})
	@EnableConfigurationProperties
	static class ExampleSameServiceAnotherServiceConfiguration {
	}
	
	
	@Test
	public void clientConfigurations() {
		int dynamoDbSocketTimeout = 5;
		String dynamoDbEndpoint = "http://localhost:60005";
		int elbv1SocketTimeout = 6;
		String elbv1Endpoint = "http://localhost:60006";
		int elbv2SocketTimeout = 7;
		String elbv2Endpoint = "http://localhost:60007";
		this.contextRunner.withUserConfiguration(ExampleSameServiceAnotherServiceConfiguration.class)
			.withPropertyValues("aws1.dynamodbv2.client.socket-timeout=" + dynamoDbSocketTimeout)
			.withPropertyValues("aws1.dynamodbv2.endpoint.service-endpoint=" + dynamoDbEndpoint)
			.withPropertyValues("aws1.elasticloadbalancing.client.socket-timeout=" + elbv1SocketTimeout)
			.withPropertyValues("aws1.elasticloadbalancing.endpoint.service-endpoint=" + elbv1Endpoint)
			.withPropertyValues("aws1.elasticloadbalancingv2.client.socket-timeout=" + elbv2SocketTimeout)
			.withPropertyValues("aws1.elasticloadbalancingv2.endpoint.service-endpoint=" + elbv2Endpoint)
			.run(context -> {
				assertThat(context.getBean(AmazonDynamoDB.class))
					.isInstanceOfSatisfying(AmazonDynamoDBClient.class, client -> {
						assertThat(client).hasFieldOrPropertyWithValue("endpoint", URI.create(dynamoDbEndpoint));
						assertThat(client.getClientConfiguration().getSocketTimeout()).isEqualTo(dynamoDbSocketTimeout);
					});
				assertThat(context.getBean(AmazonDynamoDBStreams.class))
					.isInstanceOfSatisfying(AmazonDynamoDBStreamsClient.class, client -> {
						assertThat(client).hasFieldOrPropertyWithValue("endpoint", URI.create(dynamoDbEndpoint));
						assertThat(client.getClientConfiguration().getSocketTimeout()).isEqualTo(dynamoDbSocketTimeout);
					});
				assertThat(context
					.getBean(com.amazonaws.services.elasticloadbalancing.AmazonElasticLoadBalancing.class))
						.isInstanceOfSatisfying(
								com.amazonaws.services.elasticloadbalancing.AmazonElasticLoadBalancingClient.class,
								client -> {
									assertThat(client)
										.hasFieldOrPropertyWithValue("endpoint", URI.create(elbv1Endpoint));
									assertThat(client.getClientConfiguration().getSocketTimeout())
										.isEqualTo(elbv1SocketTimeout);
								});
				assertThat(context
					.getBean(com.amazonaws.services.elasticloadbalancingv2.AmazonElasticLoadBalancing.class))
						.isInstanceOfSatisfying(
								com.amazonaws.services.elasticloadbalancingv2.AmazonElasticLoadBalancingClient.class,
								client -> {
									assertThat(client)
										.hasFieldOrPropertyWithValue("endpoint", URI.create(elbv2Endpoint));
									assertThat(client.getClientConfiguration().getSocketTimeout())
										.isEqualTo(elbv2SocketTimeout);
								});
				assertThat(context).doesNotHaveBean(AmazonAlexaForBusiness.class.getName());
			});
	}
	
	
	@Configuration
	@EnableAwsClientV1(AmazonS3.class)
	@EnableConfigurationProperties
	static class ExampleUserConfiguration {
		
		@Bean("com.amazonaws.services.s3.AmazonS3")
		public AmazonS3 mockClient() {
			return mock(AmazonS3.class);
		}
	}
	
	
	@Test
	public void defaultServiceBacksOff() {
		this.contextRunner.withUserConfiguration(ExampleUserConfiguration.class).run(context -> {
			assertThat(context).hasSingleBean(AmazonS3.class).getBean(AmazonS3.class)
				.isSameAs(context.getBean(ExampleUserConfiguration.class).mockClient());
		});
	}
	
	
	@Configuration
	@EnableAwsClientV1({
		AmazonS3.class,
		AmazonS3Encryption.class
	})
	@EnableConfigurationProperties
	static class ExampleS3EncryptionConfiguration {
		
		@Bean("com.amazonaws.services.s3.model.EncryptionMaterialsProvider")
		public EncryptionMaterialsProvider encryptionMaterialsProvider() {
			return mock(EncryptionMaterialsProvider.class);
		}
	}
	
	
	@Test
	public void s3EncryptionClient() {
		this.contextRunner.withUserConfiguration(ExampleS3EncryptionConfiguration.class).run(context -> {
			assertThat(context).hasBean(AmazonS3.class.getName());
			assertThat(context).hasBean(AmazonS3Encryption.class.getName());
		});
	}
}
