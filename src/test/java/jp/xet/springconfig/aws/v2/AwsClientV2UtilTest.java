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

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.time.Duration;

import org.junit.Test;

import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.awscore.client.config.AwsClientOption;
import software.amazon.awssdk.core.ClientType;
import software.amazon.awssdk.core.client.config.ClientOverrideConfiguration;
import software.amazon.awssdk.core.client.config.SdkAdvancedClientOption;
import software.amazon.awssdk.core.client.config.SdkClientConfiguration;
import software.amazon.awssdk.core.client.config.SdkClientOption;
import software.amazon.awssdk.core.retry.RetryPolicy;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.ec2.Ec2Client;
import software.amazon.awssdk.services.ec2.Ec2ClientBuilder;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3ClientBuilder;
import software.amazon.awssdk.services.s3.S3Configuration;

public class AwsClientV2UtilTest {
	
	@Test
	public void testBuildEc2Client() {
		// exercise
		Object builder = AwsClientV2Util.createBuilder(Ec2Client.class);
		AwsClientV2Util.configureRegion(builder, "ap-northeast-2");
		Object actual = AwsClientV2Util.build(builder);
		// verify
		assertThat(builder).isInstanceOf(Ec2ClientBuilder.class);
		assertThat(actual).isInstanceOfSatisfying(Ec2Client.class, client -> {
			SdkClientConfiguration clientConfig = TestUtil.extractClientConfig(client);
			
			assertThat(clientConfig.option(AwsClientOption.AWS_REGION)).isEqualTo(Region.of("ap-northeast-2"));
			assertThat(clientConfig.option(AwsClientOption.SIGNING_REGION)).isEqualTo(Region.of("ap-northeast-2"));
			assertThat(clientConfig.option(AwsClientOption.CREDENTIALS_PROVIDER))
				.isInstanceOf(DefaultCredentialsProvider.class);
			assertThat(clientConfig.option(AwsClientOption.SERVICE_SIGNING_NAME)).isEqualTo("ec2");
			
			assertThat(clientConfig.option(SdkClientOption.ADDITIONAL_HTTP_HEADERS)).isEmpty();
			assertThat(clientConfig.option(SdkClientOption.RETRY_POLICY))
				.satisfies(policy -> assertThat(policy).returns(3, RetryPolicy::numRetries));
			assertThat(clientConfig.option(SdkClientOption.EXECUTION_INTERCEPTORS)).isNotNull();
			assertThat(clientConfig.option(SdkClientOption.ENDPOINT))
				.isEqualTo(URI.create("https://ec2.ap-northeast-2.amazonaws.com"));
			assertThat(clientConfig.option(SdkClientOption.SERVICE_CONFIGURATION)).isNull();
			assertThat(clientConfig.option(SdkClientOption.CRC32_FROM_COMPRESSED_DATA_ENABLED)).isFalse();
			assertThat(clientConfig.option(SdkClientOption.SCHEDULED_EXECUTOR_SERVICE)).isNotNull();
			assertThat(clientConfig.option(SdkClientOption.ASYNC_HTTP_CLIENT)).isNull();
			assertThat(clientConfig.option(SdkClientOption.SYNC_HTTP_CLIENT)).isNotNull();
			assertThat(clientConfig.option(SdkClientOption.CLIENT_TYPE)).isEqualTo(ClientType.SYNC);
			assertThat(clientConfig.option(SdkClientOption.API_CALL_ATTEMPT_TIMEOUT)).isNull();
			assertThat(clientConfig.option(SdkClientOption.API_CALL_TIMEOUT)).isNull();
			assertThat(clientConfig.option(SdkClientOption.SERVICE_NAME)).isEqualTo("Ec2");
			
			assertThat(clientConfig.option(SdkAdvancedClientOption.USER_AGENT_PREFIX)).startsWith("aws-sdk-java/2.");
			assertThat(clientConfig.option(SdkAdvancedClientOption.USER_AGENT_SUFFIX)).isEmpty();
			assertThat(clientConfig.option(SdkAdvancedClientOption.SIGNER)).isNotNull();
			assertThat(clientConfig.option(SdkAdvancedClientOption.DISABLE_HOST_PREFIX_INJECTION)).isFalse();
		});
	}
	
	@Test
	public void testBuildS3Client_Default() {
		// exercise
		Object builder = AwsClientV2Util.createBuilder(S3Client.class);
		if (System.getenv("CIRCLECI") == null) {
			AwsClientV2Util.configureRegion(builder, "us-east-1");
		}
		AwsClientV2Util.configureServiceConfiguration(builder, S3Configuration.builder().build());
		Object actual = AwsClientV2Util.build(builder);
		// verify
		assertThat(builder).isInstanceOf(S3ClientBuilder.class);
		assertThat(actual).isInstanceOfSatisfying(S3Client.class, client -> {
			SdkClientConfiguration clientConfig = TestUtil.extractClientConfig(client);
			
			assertThat(clientConfig.option(AwsClientOption.AWS_REGION)).isEqualTo(Region.of("us-east-1"));
			assertThat(clientConfig.option(AwsClientOption.SIGNING_REGION)).isEqualTo(Region.of("us-east-1"));
			assertThat(clientConfig.option(AwsClientOption.CREDENTIALS_PROVIDER))
				.isInstanceOf(DefaultCredentialsProvider.class);
			assertThat(clientConfig.option(AwsClientOption.SERVICE_SIGNING_NAME)).isEqualTo("s3");
			
			assertThat(clientConfig.option(SdkClientOption.ADDITIONAL_HTTP_HEADERS)).isEmpty();
			assertThat(clientConfig.option(SdkClientOption.RETRY_POLICY))
				.satisfies(policy -> assertThat(policy).returns(3, RetryPolicy::numRetries));
			assertThat(clientConfig.option(SdkClientOption.EXECUTION_INTERCEPTORS)).isNotNull();
			assertThat(clientConfig.option(SdkClientOption.ENDPOINT))
				.isEqualTo(URI.create("https://s3.amazonaws.com"));
			assertThat(clientConfig.option(SdkClientOption.SERVICE_CONFIGURATION))
				.isInstanceOfSatisfying(S3Configuration.class, s3Config -> assertThat(s3Config)
					.returns(false, S3Configuration::pathStyleAccessEnabled)
					.returns(true, S3Configuration::chunkedEncodingEnabled)
					.returns(false, S3Configuration::accelerateModeEnabled)
					.returns(false, S3Configuration::dualstackEnabled)
					.returns(true, S3Configuration::checksumValidationEnabled));
			assertThat(clientConfig.option(SdkClientOption.CRC32_FROM_COMPRESSED_DATA_ENABLED)).isFalse();
			assertThat(clientConfig.option(SdkClientOption.SCHEDULED_EXECUTOR_SERVICE)).isNotNull();
			assertThat(clientConfig.option(SdkClientOption.ASYNC_HTTP_CLIENT)).isNull();
			assertThat(clientConfig.option(SdkClientOption.SYNC_HTTP_CLIENT)).isNotNull();
			assertThat(clientConfig.option(SdkClientOption.CLIENT_TYPE)).isEqualTo(ClientType.SYNC);
			assertThat(clientConfig.option(SdkClientOption.API_CALL_ATTEMPT_TIMEOUT)).isNull();
			assertThat(clientConfig.option(SdkClientOption.API_CALL_TIMEOUT)).isNull();
			assertThat(clientConfig.option(SdkClientOption.SERVICE_NAME)).isEqualTo("S3");
			
			assertThat(clientConfig.option(SdkAdvancedClientOption.USER_AGENT_PREFIX)).startsWith("aws-sdk-java/2.");
			assertThat(clientConfig.option(SdkAdvancedClientOption.USER_AGENT_SUFFIX)).isEmpty();
			assertThat(clientConfig.option(SdkAdvancedClientOption.SIGNER)).isNotNull();
			assertThat(clientConfig.option(SdkAdvancedClientOption.DISABLE_HOST_PREFIX_INJECTION)).isFalse();
		});
	}
	
	@Test
	public void testBuildS3Client() {
		// exercise
		Object builder = AwsClientV2Util.createBuilder(S3Client.class);
		AwsClientV2Util.configureRegion(builder, "ap-northeast-2");
		AwsClientV2Util.configureClientOverrideConfiguration(builder, ClientOverrideConfiguration.builder()
			.apiCallAttemptTimeout(Duration.ofSeconds(2))
			.build());
		AwsClientV2Util.configureServiceConfiguration(builder, S3Configuration.builder()
			.pathStyleAccessEnabled(true)
			.chunkedEncodingEnabled(false)
			.build());
		Object actual = AwsClientV2Util.build(builder);
		// verify
		assertThat(builder).isInstanceOf(S3ClientBuilder.class);
		assertThat(actual).isInstanceOfSatisfying(S3Client.class, client -> {
			SdkClientConfiguration clientConfig = TestUtil.extractClientConfig(client);
			
			assertThat(clientConfig.option(AwsClientOption.AWS_REGION)).isEqualTo(Region.of("ap-northeast-2"));
			assertThat(clientConfig.option(AwsClientOption.SIGNING_REGION)).isEqualTo(Region.of("ap-northeast-2"));
			assertThat(clientConfig.option(AwsClientOption.CREDENTIALS_PROVIDER))
				.isInstanceOf(DefaultCredentialsProvider.class);
			assertThat(clientConfig.option(AwsClientOption.SERVICE_SIGNING_NAME)).isEqualTo("s3");
			
			assertThat(clientConfig.option(SdkClientOption.ADDITIONAL_HTTP_HEADERS)).isEmpty();
			assertThat(clientConfig.option(SdkClientOption.RETRY_POLICY))
				.satisfies(policy -> assertThat(policy).returns(3, RetryPolicy::numRetries));
			assertThat(clientConfig.option(SdkClientOption.EXECUTION_INTERCEPTORS)).isNotNull();
			assertThat(clientConfig.option(SdkClientOption.ENDPOINT))
				.isEqualTo(URI.create("https://s3.ap-northeast-2.amazonaws.com"));
			assertThat(clientConfig.option(SdkClientOption.SERVICE_CONFIGURATION))
				.isInstanceOfSatisfying(S3Configuration.class, s3Config -> assertThat(s3Config)
					.returns(true, S3Configuration::pathStyleAccessEnabled)
					.returns(false, S3Configuration::chunkedEncodingEnabled)
					.returns(false, S3Configuration::accelerateModeEnabled)
					.returns(false, S3Configuration::dualstackEnabled)
					.returns(true, S3Configuration::checksumValidationEnabled));
			assertThat(clientConfig.option(SdkClientOption.CRC32_FROM_COMPRESSED_DATA_ENABLED)).isFalse();
			assertThat(clientConfig.option(SdkClientOption.SCHEDULED_EXECUTOR_SERVICE)).isNotNull();
			assertThat(clientConfig.option(SdkClientOption.ASYNC_HTTP_CLIENT)).isNull();
			assertThat(clientConfig.option(SdkClientOption.SYNC_HTTP_CLIENT)).isNotNull();
			assertThat(clientConfig.option(SdkClientOption.CLIENT_TYPE)).isEqualTo(ClientType.SYNC);
			assertThat(clientConfig.option(SdkClientOption.API_CALL_ATTEMPT_TIMEOUT)).isEqualTo(Duration.ofSeconds(2));
			assertThat(clientConfig.option(SdkClientOption.API_CALL_TIMEOUT)).isNull();
			assertThat(clientConfig.option(SdkClientOption.SERVICE_NAME)).isEqualTo("S3");
			
			assertThat(clientConfig.option(SdkAdvancedClientOption.USER_AGENT_PREFIX)).startsWith("aws-sdk-java/2.");
			assertThat(clientConfig.option(SdkAdvancedClientOption.USER_AGENT_SUFFIX)).isEmpty();
			assertThat(clientConfig.option(SdkAdvancedClientOption.SIGNER)).isNotNull();
			assertThat(clientConfig.option(SdkAdvancedClientOption.DISABLE_HOST_PREFIX_INJECTION)).isFalse();
		});
	}
}
