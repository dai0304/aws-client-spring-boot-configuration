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

import java.io.Serializable;
import java.net.URI;
import java.security.SecureRandom;
import java.util.Locale;

import lombok.extern.slf4j.Slf4j;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.util.ReflectionTestUtils;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.amazonaws.AmazonWebServiceClient;
import com.amazonaws.ClientConfiguration;
import com.amazonaws.Protocol;
import com.amazonaws.SystemDefaultDnsResolver;
import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.handlers.RequestHandler2;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.alexaforbusiness.AmazonAlexaForBusiness;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClient;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBStreams;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBStreamsClient;
import com.amazonaws.services.ec2.AmazonEC2;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.AmazonS3Encryption;
import com.amazonaws.services.s3.AmazonS3EncryptionClient;
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
	
	private static final String SYSTEM_PROPERTY_REGION = Regions.US_EAST_1.getName();
	
	private static final AWSCredentialsProvider MOCK_CREDENTIALS_PROVIDER = mock(AWSCredentialsProvider.class);
	
	private static final RequestHandler2 MOCK_HANDLER1 = mock(RequestHandler2.class);
	
	private static final RequestHandler2 MOCK_HANDLER2 = mock(RequestHandler2.class);
	
	private static final RequestHandler2 MOCK_HANDLER3 = mock(RequestHandler2.class);
	
	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner();
	
	
	@Before
	public void setUp() throws Exception {
		System.setProperty("aws.region", SYSTEM_PROPERTY_REGION);
		System.setProperty("http.nonProxyHosts", "local|*.local|169.254/16|*.169.254/16");
	}
	
	@After
	public void tearDown() throws Exception {
		System.clearProperty("aws.region");
		System.clearProperty("http.nonProxyHosts");
	}
	
	
	@Configuration
	@EnableAwsClientV1(Serializable.class)
	@EnableConfigurationProperties
	static class ExampleInvalidConfiguration {
	}
	
	
	@Test
	public void invalidClientConfig() {
		contextRunner.withUserConfiguration(ExampleInvalidConfiguration.class)
			.run(context -> assertThat(context).hasFailed());
	}
	
	
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
		contextRunner.withUserConfiguration(ExampleS3SqsSnsConfiguration.class).run(context -> {
			assertThat(context)
				.hasSingleBean(AmazonS3.class)
				.getBeanNames(AmazonS3.class).containsExactlyInAnyOrder(AmazonS3.class.getName());
			assertThat(context).getBean(AmazonS3.class)
				.isInstanceOfSatisfying(AmazonS3Client.class, this::isDefaultConfig);
			
			assertThat(context)
				.hasSingleBean(AmazonSQS.class)
				.getBeanNames(AmazonSQS.class).containsExactlyInAnyOrder(AmazonSQS.class.getName());
			assertThat(context).getBean(AmazonSQS.class)
				.isInstanceOfSatisfying(AmazonSQSClient.class, this::isDefaultConfig);
			
			assertThat(context)
				.hasSingleBean(AmazonSNS.class)
				.getBeanNames(AmazonSNS.class).containsExactlyInAnyOrder(AmazonSNS.class.getName());
			assertThat(context).getBean(AmazonSNS.class)
				.isInstanceOfSatisfying(AmazonSNSClient.class, this::isDefaultConfig);
			
			// not defined in the annotation
			assertThat(context).doesNotHaveBean(AmazonSQSAsync.class);
			assertThat(context).doesNotHaveBean(AmazonSNSAsync.class);
			assertThat(context).doesNotHaveBean(AmazonAlexaForBusiness.class);
			assertThat(context).doesNotHaveBean("com.amazonaws.services.simpledb.AmazonSimpleDB");
		});
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
		contextRunner.withUserConfiguration(ExampleSyncAndAsyncSnsConfiguration.class).run(context -> {
			assertThat(context).hasBean(AmazonSNS.class.getName())
				.getBean(AmazonSNS.class.getName())
				.isInstanceOfSatisfying(AmazonSNSClient.class, this::isDefaultConfig);
			assertThat(context).hasBean(AmazonSNSAsync.class.getName())
				.getBean(AmazonSNSAsync.class.getName())
				.isInstanceOfSatisfying(AmazonSNSAsyncClient.class, this::isDefaultConfig);
		});
	}
	
	
	@Configuration
	@EnableAwsClientV1(AmazonSNSAsync.class)
	@EnableConfigurationProperties
	static class ExampleAsyncSnsOnlyConfiguration {
	}
	
	
	@Test
	public void defaultClient_AsyncOnly() {
		contextRunner.withUserConfiguration(ExampleAsyncSnsOnlyConfiguration.class).run(context -> {
			assertThat(context).doesNotHaveBean(AmazonSNS.class.getName());
			assertThat(context).hasBean(AmazonSNSAsync.class.getName())
				.getBean(AmazonSNSAsync.class.getName())
				.isInstanceOfSatisfying(AmazonSNSAsyncClient.class, this::isDefaultConfig);
		});
	}
	
	@Test
	public void configuredClient_SyncOnly() {
		contextRunner.withUserConfiguration(ExampleS3SqsSnsConfiguration.class)
			.withPropertyValues("aws1.sqs.client.socket-timeout=123")
			.withPropertyValues("aws1.sqs.region=eu-central-1")
			.withPropertyValues("aws1.sns.client.socket-timeout=456")
			.withPropertyValues("aws1.sns.region=us-east-1")
			.run(context -> {
				assertThat(context.getBean(AmazonS3.class))
					.isInstanceOfSatisfying(AmazonS3Client.class, this::isDefaultConfig);
				assertThat(context.getBean(AmazonSQS.class)).isInstanceOfSatisfying(AmazonSQSClient.class, client -> {
					assertThat(client.getClientConfiguration().getSocketTimeout())
						.isEqualTo(123); // aws1.sqs.client.socket-timeout
					assertThat(client).hasFieldOrPropertyWithValue("endpoint",
							URI.create("https://sqs.eu-central-1.amazonaws.com")); // aws1.sqs.region
				});
				assertThat(context.getBean(AmazonSNS.class)).isInstanceOfSatisfying(AmazonSNSClient.class, client -> {
					assertThat(client.getClientConfiguration().getSocketTimeout())
						.isEqualTo(456); // aws1.sns.client.socket-timeout
					assertThat(client).hasFieldOrPropertyWithValue("endpoint",
							URI.create("https://sns.us-east-1.amazonaws.com")); // aws1.sns.region
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
	public void configuredClient_BothSyncAndAsync() {
		int sqsAsyncSocketTimeout = 1;
		String sqsAsyncEndpoint = "http://localhost:60001";
		int sqsSocketTimeout = 2;
		String sqsEndpoint = "http://localhost:60002";
		int snsSocketTimeout = 3;
		String snsEndpoint = "http://localhost:60003";
		int snsAsyncSocketTimeout = 4;
		String snsAsyncEndpoint = "http://localhost:60004";
		contextRunner.withUserConfiguration(ExampleSqsSnsSyncAsyncConfiguration.class)
			.withPropertyValues("aws1.sqs.client.socket-timeout=" + sqsSocketTimeout)
			.withPropertyValues("aws1.sqs.endpoint.service-endpoint=" + sqsEndpoint)
			.withPropertyValues("aws1.sqs-async.client.socket-timeout=" + sqsAsyncSocketTimeout)
			.withPropertyValues("aws1.sqs-async.endpoint.service-endpoint=" + sqsAsyncEndpoint)
			.withPropertyValues("aws1.sns.client.socket-timeout=" + snsSocketTimeout)
			.withPropertyValues("aws1.sns.endpoint.service-endpoint=" + snsEndpoint)
			.withPropertyValues("aws1.sns-async.client.socket-timeout=" + snsAsyncSocketTimeout)
			.withPropertyValues("aws1.sns-async.endpoint.service-endpoint=" + snsAsyncEndpoint)
			.run(context -> {
				assertThat(context.getBean(AmazonSQS.class.getName()))
					.isInstanceOfSatisfying(AmazonSQSClient.class, client -> {
						// use aws.sqs.*
						assertThat(client).hasFieldOrPropertyWithValue("endpoint", URI.create(sqsEndpoint));
						assertThat(client.getClientConfiguration().getSocketTimeout()).isEqualTo(sqsSocketTimeout);
					});
				assertThat(context.getBean(AmazonSQSAsync.class))
					.isInstanceOfSatisfying(AmazonSQSAsyncClient.class, client -> {
						// use aws.sqs-async.*
						assertThat(client).hasFieldOrPropertyWithValue("endpoint", URI.create(sqsAsyncEndpoint));
						assertThat(client.getClientConfiguration().getSocketTimeout())
							.isEqualTo(sqsAsyncSocketTimeout);
					});
				assertThat(context.getBean(AmazonSNS.class.getName()))
					.isInstanceOfSatisfying(AmazonSNSClient.class, client -> {
						// use aws.sns.*
						assertThat(client).hasFieldOrPropertyWithValue("endpoint", URI.create(snsEndpoint));
						assertThat(client.getClientConfiguration().getSocketTimeout()).isEqualTo(snsSocketTimeout);
					});
				assertThat(context.getBean(AmazonSNSAsync.class))
					.isInstanceOfSatisfying(AmazonSNSAsyncClient.class, client -> {
						// use aws.sns-async.*
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
	public void defaultS3Client() {
		contextRunner.withUserConfiguration(ExampleS3Configuration.class).run(context -> {
			assertThat(context.getBean(AmazonS3.class))
				.isInstanceOfSatisfying(AmazonS3Client.class, this::isDefaultConfig)
				.isInstanceOfSatisfying(AmazonS3Client.class, client -> {
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
	public void configuredS3Client() {
		contextRunner.withUserConfiguration(ExampleS3Configuration.class)
			.withPropertyValues("aws1.s3.path-style-access-enabled=true")
			.withPropertyValues("aws1.s3.chunked-encoding-disabled=true")
			.withPropertyValues("aws1.s3.accelerate-mode-enabled=false")
			.withPropertyValues("aws1.s3.payload-signing-enabled=true")
			.withPropertyValues("aws1.s3.dualstack-enabled=true")
			.withPropertyValues("aws1.s3.force-global-bucket-access-enabled=true")
			.run(context -> {
				assertThat(context.getBean(AmazonS3.class))
					.isInstanceOfSatisfying(AmazonS3Client.class, this::isDefaultConfig)
					.isInstanceOfSatisfying(AmazonS3Client.class, client -> {
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
		AmazonDynamoDBStreams.class
	})
	@EnableConfigurationProperties
	static class ExampleDynamoDbConfiguration {
	}
	
	
	@Test
	public void clientConfigurations() {
		int dynamoDbSocketTimeout = 5;
		String dynamoDbEndpoint = "http://localhost:60005";
		contextRunner.withUserConfiguration(ExampleDynamoDbConfiguration.class)
			.withPropertyValues("aws1.dynamodbv2.client.socket-timeout=" + dynamoDbSocketTimeout)
			.withPropertyValues("aws1.dynamodbv2.endpoint.service-endpoint=" + dynamoDbEndpoint)
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
		contextRunner.withUserConfiguration(ExampleUserConfiguration.class).run(context -> {
			assertThat(context).hasSingleBean(AmazonS3.class).getBean(AmazonS3.class)
				.isSameAs(context.getBean(ExampleUserConfiguration.class).mockClient());
		});
	}
	
	
	@Configuration
	@EnableAwsClientV1(AmazonEC2.class)
	@EnableConfigurationProperties
	static class ExampleCredentialsProviderConfiguration {
		
		@Bean
		public AWSCredentialsProvider exampleAwsCredentialsProvider() {
			return MOCK_CREDENTIALS_PROVIDER;
		}
	}
	
	
	@Test
	public void credentialsProviderConfiguration() {
		contextRunner.withUserConfiguration(ExampleCredentialsProviderConfiguration.class)
			.withPropertyValues("aws1.ec2.credentials-provider-bean-name=exampleAwsCredentialsProvider")
			.run(context -> {
				assertThat(context.getBean(AmazonEC2.class)).satisfies(client -> {
					assertThat(TestUtil.extractCredentialsProvider(client))
						.isEqualTo(MOCK_CREDENTIALS_PROVIDER);
				});
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
		contextRunner.withUserConfiguration(ExampleS3EncryptionConfiguration.class).run(context -> {
			assertThat(context).hasBean(AmazonS3.class.getName())
				.getBean(AmazonS3.class.getName())
				.isInstanceOfSatisfying(AmazonS3Client.class, this::isDefaultConfig);
			assertThat(context).hasBean(AmazonS3Encryption.class.getName())
				.getBean(AmazonS3Encryption.class.getName())
				.isInstanceOfSatisfying(AmazonS3EncryptionClient.class, this::isDefaultConfig);
		});
	}
	
	
	@Configuration
	@EnableAwsClientV1(AmazonDynamoDB.class)
	@EnableConfigurationProperties
	static class ExampleRequestHandlerConfiguration {
		
		@Bean
		public RequestHandler2 handler1() {
			return MOCK_HANDLER1;
		}
		
		@Bean
		public RequestHandler2 handler2() {
			return MOCK_HANDLER2;
		}
		
		@Bean
		public RequestHandler2 handler3() {
			return MOCK_HANDLER3;
		}
	}
	
	
	@Test
	public void requestHandlerDynamoDbClient() {
		contextRunner.withUserConfiguration(ExampleRequestHandlerConfiguration.class)
			.withPropertyValues("aws1.dynamodbv2.request-handler-bean-names=handler1,handler3")
			.run(context -> {
				assertThat(context.getBean(AmazonDynamoDB.class)).satisfies(client -> {
					assertThat(TestUtil.extractRequestHandlers(client))
						.containsExactlyInAnyOrder(MOCK_HANDLER1, MOCK_HANDLER3);
				});
			});
	}
	
	// utilities
	
	private void isDefaultConfig(AmazonWebServiceClient client) {
		ClientConfiguration config = client.getClientConfiguration();
		assertThat(config).isNotNull()
			.returns(ClientConfiguration.DEFAULT_USER_AGENT, ClientConfiguration::getUserAgentPrefix)
			.returns(null, ClientConfiguration::getUserAgentSuffix)
			.returns(-1, ClientConfiguration::getMaxErrorRetry)
			.returns(null, ClientConfiguration::getLocalAddress)
			.returns(ClientConfiguration.DEFAULT_RETRY_POLICY, ClientConfiguration::getRetryPolicy)
			.returns(Protocol.HTTPS, ClientConfiguration::getProtocol)
			.returns(null, ClientConfiguration::getProxyHost)
			.returns(-1, ClientConfiguration::getProxyPort)
			.returns(null, ClientConfiguration::getProxyUsername)
			.returns(null, ClientConfiguration::getProxyPassword)
			.returns(null, ClientConfiguration::getProxyDomain)
			.returns(null, ClientConfiguration::getProxyWorkstation)
			.returns("local|*.local|169.254/16|*.169.254/16", ClientConfiguration::getNonProxyHosts)
			.returns(null, ClientConfiguration::getProxyAuthenticationMethods)
			.returns(ClientConfiguration.DEFAULT_DISABLE_SOCKET_PROXY, ClientConfiguration::disableSocketProxy)
			.returns(false, ClientConfiguration::isPreemptiveBasicProxyAuth)
			.returns(ClientConfiguration.DEFAULT_MAX_CONNECTIONS, ClientConfiguration::getMaxConnections)
			.returns(ClientConfiguration.DEFAULT_SOCKET_TIMEOUT, ClientConfiguration::getSocketTimeout)
			.returns(ClientConfiguration.DEFAULT_CONNECTION_TIMEOUT, ClientConfiguration::getConnectionTimeout)
			.returns(ClientConfiguration.DEFAULT_REQUEST_TIMEOUT, ClientConfiguration::getRequestTimeout)
			.returns(ClientConfiguration.DEFAULT_THROTTLE_RETRIES, ClientConfiguration::useThrottledRetries)
			.returns(new int[] {
				0,
				0,
			}, ClientConfiguration::getSocketBufferSizeHints)
			.returns(ClientConfiguration.DEFAULT_USE_REAPER, ClientConfiguration::useReaper)
			.returns(ClientConfiguration.DEFAULT_USE_GZIP, ClientConfiguration::useGzip)
			.returns(null, ClientConfiguration::getSignerOverride)
			.returns(ClientConfiguration.DEFAULT_CONNECTION_TTL, ClientConfiguration::getConnectionTTL)
			.returns(ClientConfiguration.DEFAULT_CONNECTION_MAX_IDLE_MILLIS,
					ClientConfiguration::getConnectionMaxIdleMillis)
			.returns(ClientConfiguration.DEFAULT_VALIDATE_AFTER_INACTIVITY_MILLIS,
					ClientConfiguration::getValidateAfterInactivityMillis)
			.returns(ClientConfiguration.DEFAULT_TCP_KEEP_ALIVE, ClientConfiguration::useTcpKeepAlive)
			.returns(ClientConfiguration.DEFAULT_CACHE_RESPONSE_METADATA, ClientConfiguration::getCacheResponseMetadata)
			.returns(ClientConfiguration.DEFAULT_RESPONSE_METADATA_CACHE_SIZE,
					ClientConfiguration::getResponseMetadataCacheSize)
			.returns(ClientConfiguration.DEFAULT_USE_EXPECT_CONTINUE, ClientConfiguration::isUseExpectContinue)
			.returns(ClientConfiguration.DEFAULT_MAX_CONSECUTIVE_RETRIES_BEFORE_THROTTLING,
					ClientConfiguration::getMaxConsecutiveRetriesBeforeThrottling);
		assertThat(config.getDnsResolver()).isInstanceOf(SystemDefaultDnsResolver.class);
		assertThat(config.getSecureRandom()).isInstanceOf(SecureRandom.class);
		assertThat(config.getHeaders()).isEmpty();
		
		assertThat(TestUtil.extractSigningRegion(client)).isEqualTo(SYSTEM_PROPERTY_REGION);
		
		String endpointPrefix = TestUtil.extractEndpointPrefix(client);
		String expectedHost;
		if (endpointPrefix.equals("s3") && SYSTEM_PROPERTY_REGION.equals("us-east-1")) {
			expectedHost = "s3.amazonaws.com";
		} else {
			expectedHost = String.format(Locale.ENGLISH, "%s.%s.amazonaws.com",
					endpointPrefix, SYSTEM_PROPERTY_REGION);
		}
		assertThat(TestUtil.extractEndpoint(client)).hasScheme("https")
			.hasHost(expectedHost)
			.hasNoPort().hasNoUserInfo().hasPath("").hasNoParameters().hasNoQuery().hasNoFragment();
	}
}
