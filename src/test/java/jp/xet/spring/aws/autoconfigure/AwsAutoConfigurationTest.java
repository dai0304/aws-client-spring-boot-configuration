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
package jp.xet.spring.aws.autoconfigure; // NOPMD CouplingBetweenObjects

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

import org.junit.Ignore;
import org.junit.Test;

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
 * Test for all AWS auto configuration.
 *
 * @author miyamoto.daisuke
 * @since #version#
 */
@Slf4j
public class AwsAutoConfigurationTest {
	
	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
		.withConfiguration(AutoConfigurations.of(AwsAutoConfiguration.class));
	
	
	@Configuration
	@EnableConfigurationProperties
	static class EmptyConfiguration {
	}
	
	
	@Test
	public void defaultClient_SyncOnly() {
		this.contextRunner.withUserConfiguration(EmptyConfiguration.class).run(context -> {
			assertThat(context).hasSingleBean(AmazonS3.class);
			assertThat(context).hasSingleBean(AmazonSQS.class);
			assertThat(context).doesNotHaveBean(AmazonSQSAsync.class);
			assertThat(context).hasSingleBean(AmazonSNS.class);
			assertThat(context).doesNotHaveBean(AmazonSNSAsync.class);
			assertThat(context.getBean(AmazonS3.class)).isInstanceOfSatisfying(AmazonS3Client.class, client -> {
				assertThat(client.getClientConfiguration()).isNotNull();
				assertThat(client.getClientConfiguration().getSocketTimeout()).isEqualTo(50000); // default
			});
			assertThat(context.getBean(AmazonSQS.class)).isInstanceOfSatisfying(AmazonSQSClient.class, client -> {
				assertThat(client.getClientConfiguration()).isNotNull();
				assertThat(client.getClientConfiguration().getSocketTimeout()).isEqualTo(50000); // default
			});
			assertThat(context.getBean(AmazonSNS.class)).isInstanceOfSatisfying(AmazonSNSClient.class, client -> {
				assertThat(client.getClientConfiguration()).isNotNull();
				assertThat(client.getClientConfiguration().getSocketTimeout()).isEqualTo(50000); // default
			});
			
			// not in aws.builders
			assertThat(context).doesNotHaveBean(AmazonAlexaForBusiness.class);
			
			// not in classpath
			assertThat(context).doesNotHaveBean("com.amazonaws.services.simpledb.AmazonSimpleDB");
		});
	}
	
	@Test
	public void defaultClient_BothSyncAndAsync() {
		this.contextRunner.withUserConfiguration(EmptyConfiguration.class)
			.withPropertyValues("aws.async-enabled=true")
			.run(context -> {
				assertThat(context).hasBean(AmazonSNS.class.getName());
				assertThat(context).hasBean(AmazonSNSAsync.class.getName());
			});
	}
	
	@Test
	public void defaultClient_AsyncOnly() {
		this.contextRunner.withUserConfiguration(EmptyConfiguration.class)
			.withPropertyValues("aws.sync-enabled=false")
			.withPropertyValues("aws.async-enabled=true")
			.run(context -> {
				assertThat(context).doesNotHaveBean(AmazonSNS.class.getName());
				assertThat(context).hasBean(AmazonSNSAsync.class.getName());
			});
	}
	
	@Test
	@Ignore
	public void defaultClient_Disable() {
		this.contextRunner.withUserConfiguration(EmptyConfiguration.class)
			.withPropertyValues("aws.s3.enabled=false")
			.withPropertyValues("aws.sqs-async.enabled=false")
			.withPropertyValues("aws.sns.enabled=false")
			.run(context -> {
				assertThat(context).doesNotHaveBean(AmazonS3.class.getName());
				assertThat(context).hasBean(AmazonSQS.class.getName());
				assertThat(context).doesNotHaveBean(AmazonSQSAsync.class.getName());
				assertThat(context).doesNotHaveBean(AmazonSNS.class.getName());
				assertThat(context).doesNotHaveBean(AmazonSNSAsync.class.getName());
			});
	}
	
	@Test
	public void defaultConfigurationAndOverride() {
		this.contextRunner.withUserConfiguration(EmptyConfiguration.class)
			.withPropertyValues("aws.default.client.socket-timeout=123")
			.withPropertyValues("aws.default.region=us-east-1")
			.withPropertyValues("aws.sqs.region=eu-central-1")
			.withPropertyValues("aws.dynamodbv2.client.socket-timeout=456")
			.run(context -> {
				assertThat(context.getBean(AmazonS3.class)).isInstanceOfSatisfying(AmazonS3Client.class, client -> {
					assertThat(client.getClientConfiguration().getSocketTimeout()).isEqualTo(123); // default
					assertThat(client).hasFieldOrPropertyWithValue("endpoint",
							URI.create("https://s3.amazonaws.com")); // default
				});
				assertThat(context.getBean(AmazonSQS.class)).isInstanceOfSatisfying(AmazonSQSClient.class, client -> {
					assertThat(client.getClientConfiguration().getSocketTimeout()).isEqualTo(123); // default
					assertThat(client).hasFieldOrPropertyWithValue("endpoint",
							URI.create("https://sqs.eu-central-1.amazonaws.com")); // override
				});
				assertThat(context.getBean(AmazonDynamoDB.class.getName()))
					.isInstanceOfSatisfying(AmazonDynamoDBClient.class, client -> {
						assertThat(client.getClientConfiguration().getSocketTimeout()).isEqualTo(456); // override
						assertThat(client).hasFieldOrPropertyWithValue("endpoint",
								URI.create("https://dynamodb.us-east-1.amazonaws.com")); // default
					});
			});
	}
	
	@Test
	public void asyncConfigurationOverride() {
		int sqsSocketTimeout = 2;
		String sqsEndpoint = "http://localhost:60002";
		int snsSocketTimeout = 3;
		String snsEndpoint = "http://localhost:60003";
		int snsAsyncSocketTimeout = 4;
		String snsAsyncEndpoint = "http://localhost:60004";
		this.contextRunner.withUserConfiguration(EmptyConfiguration.class)
			.withPropertyValues("aws.async-enabled=true")
			.withPropertyValues("aws.sqs.client.socket-timeout=" + sqsSocketTimeout)
			.withPropertyValues("aws.sqs.endpoint.service-endpoint=" + sqsEndpoint)
			.withPropertyValues("aws.sns.client.socket-timeout=" + snsSocketTimeout)
			.withPropertyValues("aws.sns.endpoint.service-endpoint=" + snsEndpoint)
			.withPropertyValues("aws.sns-async.client.socket-timeout=" + snsAsyncSocketTimeout)
			.withPropertyValues("aws.sns-async.endpoint.service-endpoint=" + snsAsyncEndpoint)
			.run(context -> {
				assertThat(context.getBean(AmazonSQS.class.getName())).isInstanceOfSatisfying(AmazonSQSClient.class,
						client -> {
							// use aws.sqs.*
							assertThat(client).hasFieldOrPropertyWithValue("endpoint", URI.create(sqsEndpoint));
							assertThat(client.getClientConfiguration().getSocketTimeout()).isEqualTo(sqsSocketTimeout);
						});
				assertThat(context.getBean(AmazonSQSAsync.class.getName()))
					.isInstanceOfSatisfying(AmazonSQSAsyncClient.class, client -> {
						// use aws.sqs.*
						assertThat(client).hasFieldOrPropertyWithValue("endpoint", URI.create(sqsEndpoint));
						assertThat(client.getClientConfiguration().getSocketTimeout()).isEqualTo(sqsSocketTimeout);
					});
				assertThat(context.getBean(AmazonSNS.class.getName())).isInstanceOfSatisfying(AmazonSNSClient.class,
						client -> {
							// use aws.sns.*
							assertThat(client).hasFieldOrPropertyWithValue("endpoint", URI.create(snsEndpoint));
							assertThat(client.getClientConfiguration().getSocketTimeout()).isEqualTo(snsSocketTimeout);
						});
				assertThat(context.getBean(AmazonSNSAsync.class)).isInstanceOfSatisfying(AmazonSNSAsyncClient.class,
						client -> {
							// use aws.sns-async.*
							assertThat(client).hasFieldOrPropertyWithValue("endpoint", URI.create(snsAsyncEndpoint));
							assertThat(client.getClientConfiguration().getSocketTimeout())
								.isEqualTo(snsAsyncSocketTimeout);
						});
			});
	}
	
	@Test
	public void s3Configurations_default() {
		this.contextRunner.withUserConfiguration(EmptyConfiguration.class).run(context -> {
			assertThat(context.getBean(AmazonS3.class.getName())).isInstanceOfSatisfying(AmazonS3Client.class,
					client -> {
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
		this.contextRunner.withUserConfiguration(EmptyConfiguration.class)
			.withPropertyValues("aws.s3.path-style-access-enabled=true")
			.withPropertyValues("aws.s3.chunked-encoding-disabled=true")
			.withPropertyValues("aws.s3.accelerate-mode-enabled=false")
			.withPropertyValues("aws.s3.payload-signing-enabled=true")
			.withPropertyValues("aws.s3.dualstack-enabled=true")
			.withPropertyValues("aws.s3.force-global-bucket-access-enabled=true")
			.run(context -> {
				assertThat(context.getBean(AmazonS3.class.getName())).isInstanceOfSatisfying(AmazonS3Client.class,
						client -> {
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
	
	@Test
	public void clientConfigurations() {
		int dynamoDbSocketTimeout = 5;
		String dynamoDbEndpoint = "http://localhost:60005";
		int elbv1SocketTimeout = 6;
		String elbv1Endpoint = "http://localhost:60006";
		int elbv2SocketTimeout = 7;
		String elbv2Endpoint = "http://localhost:60007";
		this.contextRunner.withUserConfiguration(EmptyConfiguration.class)
			.withPropertyValues("aws.dynamodbv2.client.socket-timeout=" + dynamoDbSocketTimeout)
			.withPropertyValues("aws.dynamodbv2.endpoint.service-endpoint=" + dynamoDbEndpoint)
			.withPropertyValues("aws.elasticloadbalancing.client.socket-timeout=" + elbv1SocketTimeout)
			.withPropertyValues("aws.elasticloadbalancing.endpoint.service-endpoint=" + elbv1Endpoint)
			.withPropertyValues("aws.elasticloadbalancingv2.client.socket-timeout=" + elbv2SocketTimeout)
			.withPropertyValues("aws.elasticloadbalancingv2.endpoint.service-endpoint=" + elbv2Endpoint)
			.run(context -> {
				assertThat(context.getBean(AmazonDynamoDB.class.getName()))
					.isInstanceOfSatisfying(AmazonDynamoDBClient.class, client -> {
						assertThat(client).hasFieldOrPropertyWithValue("endpoint", URI.create(dynamoDbEndpoint));
						assertThat(client.getClientConfiguration().getSocketTimeout()).isEqualTo(dynamoDbSocketTimeout);
					});
				assertThat(context.getBean(AmazonDynamoDBStreams.class.getName()))
					.isInstanceOfSatisfying(AmazonDynamoDBStreamsClient.class, client -> {
						assertThat(client).hasFieldOrPropertyWithValue("endpoint", URI.create(dynamoDbEndpoint));
						assertThat(client.getClientConfiguration().getSocketTimeout()).isEqualTo(dynamoDbSocketTimeout);
					});
				assertThat(context
					.getBean(com.amazonaws.services.elasticloadbalancing.AmazonElasticLoadBalancing.class.getName()))
						.isInstanceOfSatisfying(
								com.amazonaws.services.elasticloadbalancing.AmazonElasticLoadBalancingClient.class,
								client -> {
									assertThat(client).hasFieldOrPropertyWithValue("endpoint",
											URI.create(elbv1Endpoint));
									assertThat(client.getClientConfiguration().getSocketTimeout())
										.isEqualTo(elbv1SocketTimeout);
								});
				assertThat(context
					.getBean(com.amazonaws.services.elasticloadbalancingv2.AmazonElasticLoadBalancing.class.getName()))
						.isInstanceOfSatisfying(
								com.amazonaws.services.elasticloadbalancingv2.AmazonElasticLoadBalancingClient.class,
								client -> {
									assertThat(client).hasFieldOrPropertyWithValue("endpoint",
											URI.create(elbv2Endpoint));
									assertThat(client.getClientConfiguration().getSocketTimeout())
										.isEqualTo(elbv2SocketTimeout);
								});
				assertThat(context).doesNotHaveBean(AmazonAlexaForBusiness.class.getName());
			});
	}
	
	
	@Configuration
	@EnableConfigurationProperties
	static class UserConfiguration {
		
		@Bean("com.amazonaws.services.s3.AmazonS3")
		public AmazonS3 mockClient() {
			return mock(AmazonS3.class);
		}
	}
	
	
	@Test
	public void defaultServiceBacksOff() {
		this.contextRunner.withUserConfiguration(UserConfiguration.class).run(context -> {
			assertThat(context).hasSingleBean(AmazonS3.class);
			assertThat(context.getBean(AmazonS3.class))
				.isSameAs(context.getBean(UserConfiguration.class).mockClient());
		});
	}
	
	
	@Configuration
	@EnableConfigurationProperties
	static class S3EncryptionConfiguration {
		
		@Bean("com.amazonaws.services.s3.model.EncryptionMaterialsProvider")
		public EncryptionMaterialsProvider encryptionMaterialsProvider() {
			return mock(EncryptionMaterialsProvider.class);
		}
	}
	
	
	@Test
	public void s3EncryptionClient() {
		this.contextRunner.withUserConfiguration(S3EncryptionConfiguration.class).run(context -> {
			assertThat(context).hasBean(AmazonS3.class.getName());
			assertThat(context).hasBean(AmazonS3Encryption.class.getName());
		});
	}
}
