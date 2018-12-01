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

import java.net.URI;
import java.util.HashMap;

import lombok.Data;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import software.amazon.awssdk.core.client.config.ClientOverrideConfiguration;

/**
 * Spring configuration for AWS Clients.
 * 
 * <ul>
 *     <li>{@code aws2.<service-package-name>[-async].endpoint} - The service endpoint either with
 *         or without the protocol (e.g. https://sns.us-west-1.amazonaws.com or sns.us-west-1.amazonaws.com) (string)</li>
 *     <li>{@code aws2.<service-package-name>[-async].region} - the region to be used by the client.
 *         This will be used to determine both the service endpoint (eg: https://sns.us-west-1.amazonaws.com)
 *         and signing region (eg: us-west-1) for requests. (string)</li>
 *     <li>{@code aws2.<service-package-name>[-async].credentials-provider-bean-name} - The spring bean name of
 *         {@link software.amazon.awssdk.auth.credentials.AwsCredentialsProvider} (string)</li>
 *     <li>{@code aws2.<service-package-name>[-async].http-client-bean-name} - The spring bean name of
 *         {@link software.amazon.awssdk.http.SdkHttpClient}
 *         or {@link software.amazon.awssdk.http.async.SdkAsyncHttpClient} (string)</li>
 *     <li>{@code aws2.<service-package-name>[-async].http-client-builder-bean-name} - The spring bean name of
 *         {@link software.amazon.awssdk.http.SdkHttpClient.Builder}
 *         or {@link software.amazon.awssdk.http.async.SdkAsyncHttpClient.Builder} (string)</li>
 * </ul>
 * 
 * <h3>Default client configurations.</h3>
 * 
 * <ul>
 *     <li>{@code aws2.default[-async].endpoint} - The service endpoint either with
 *         or without the protocol (e.g. https://sns.us-west-1.amazonaws.com or sns.us-west-1.amazonaws.com) (string)</li>
 *     <li>{@code aws2.default[-async].region} - the region to be used by the client.
 *         This will be used to determine both the service endpoint (eg: https://sns.us-west-1.amazonaws.com)
 *         and signing region (eg: us-west-1) for requests. (string)</li>
 *     <li>{@code aws2.default[-async].credentials-provider-bean-name} - The spring bean name of
 *         {@link software.amazon.awssdk.auth.credentials.AwsCredentialsProvider} (string)</li>
 *     <li>{@code aws2.default[-async].http-client-bean-name} - The spring bean name of
 *         {@link software.amazon.awssdk.http.SdkHttpClient}
 *         or {@link software.amazon.awssdk.http.async.SdkAsyncHttpClient} (string)</li>
 *     <li>{@code aws2.default[-async].http-client-builder-bean-name} - The spring bean name of
 *         {@link software.amazon.awssdk.http.SdkHttpClient.Builder}
 *         or {@link software.amazon.awssdk.http.async.SdkAsyncHttpClient.Builder} (string)</li>
 * </ul>
 * 
 * <h3>S3 client specific configurations.</h3>
 * 
 * <ul>
 *     <li>{@code aws2.s3.path-style-access-enabled} - Configures the client to use path-style access
 *         for all requests. (boolean)</li>
 *     <li>{@code aws2.s3.chunked-encoding-enabled} - Configures the client to enable chunked encoding
 *         for all requests. (boolean)</li>
 *     <li>{@code aws2.s3.accelerate-mode-enabled} - Configures the client to use S3 accelerate endpoint
 *         for all requests. (boolean)</li>
 *     <li>{@code aws2.s3.payload-signing-enabled} - Configures the client to sign payloads in all situations. (boolean)</li>
 *     <li>{@code aws2.s3.dualstack-enabled} - Configures the client to use Amazon S3 dualstack mode
 *         for all requests. (boolean)</li>
 * </ul>
 *
 * @author miyamoto.daisuke
 * @since #version#
 */
@Configuration
class AwsClientV2Configuration {
	
	@Bean
	public static AwsClientPropertiesMap awsClientV2PropertiesMap() {
		return new AwsClientPropertiesMap();
	}
	
	@Bean
	public static AwsS3ClientV2Properties awsS3ClientV2Properties() {
		return new AwsS3ClientV2Properties();
	}
	
	
	@SuppressWarnings("serial")
	@ConfigurationProperties(value = "aws2", ignoreInvalidFields = true)
	private static class AwsClientPropertiesMap extends HashMap<String, AwsClientV2Properties> {
	}
	
	@Data
	static class AwsClientV2Properties {
		
		private ClientOverrideConfiguration client;
		
		private String region;
		
		private URI endpoint;
		
		private String credentialsProviderBeanName;
		
		private String httpClientBeanName;
		
		private String httpClientBuilderBeanName;
	}
	
	@Data
	@ConfigurationProperties(value = "aws2.s3", ignoreInvalidFields = true)
	static class AwsS3ClientV2Properties {
		
		private Boolean pathStyleAccessEnabled;
		
		private Boolean chunkedEncodingEnabled;
		
		private Boolean accelerateModeEnabled;
		
		private Boolean payloadSigningEnabled;
		
		private Boolean dualstackEnabled;
	}
}
