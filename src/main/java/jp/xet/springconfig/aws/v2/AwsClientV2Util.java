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
import static jp.xet.springconfig.aws.InternalReflectionUtil.invokeStaticMethod;

import java.net.URI;

import lombok.extern.slf4j.Slf4j;

import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.core.ServiceConfiguration;
import software.amazon.awssdk.core.client.config.ClientOverrideConfiguration;
import software.amazon.awssdk.http.SdkHttpClient;
import software.amazon.awssdk.http.async.SdkAsyncHttpClient;
import software.amazon.awssdk.regions.Region;

@Slf4j
class AwsClientV2Util {
	
	/**
	 * Create AWS client builder.
	 * 
	 * @param clientClass AWS client class
	 * @return AWS client builder
	 */
	static Object createBuilder(Class<?> clientClass) {
		return invokeStaticMethod(clientClass, "builder");
	}
	
	/**
	 * @see software.amazon.awssdk.awscore.client.builder.AwsClientBuilder#credentialsProvider(AwsCredentialsProvider) 
	 */
	static void configureCredentialsProvider(Object builder, AwsCredentialsProvider credentialsProvider) {
		if (credentialsProvider == null) {
			return;
		}
		try {
			invokeMethod(builder, "credentialsProvider", credentialsProvider);
		} catch (IllegalStateException e) {
			log.warn(e.getMessage());
		}
	}
	
	/**
	 * @see software.amazon.awssdk.awscore.client.builder.AwsClientBuilder#region(Region) 
	 */
	static void configureRegion(Object builder, String region) {
		if (region == null) {
			return;
		}
		try {
			invokeMethod(builder, "region", Region.of(region));
		} catch (IllegalStateException e) {
			log.warn(e.getMessage());
		}
	}
	
	/**
	 * @see software.amazon.awssdk.core.client.builder.SdkClientBuilder#endpointOverride(URI)  
	 */
	static void configureEndpoint(Object builder, URI endpoint) {
		if (endpoint == null) {
			return;
		}
		try {
			invokeMethod(builder, "endpointOverride", endpoint);
		} catch (IllegalStateException | IllegalArgumentException e) {
			log.warn(e.getMessage());
		}
	}
	
	/**
	 * @see software.amazon.awssdk.core.client.builder.SdkClientBuilder#overrideConfiguration(ClientOverrideConfiguration)
	 */
	static void configureClientOverrideConfiguration(Object builder,
			ClientOverrideConfiguration overrideConfiguration) {
		if (overrideConfiguration == null) {
			return;
		}
		try {
			invokeMethod(builder, "overrideConfiguration", overrideConfiguration);
		} catch (IllegalStateException e) {
			log.warn(e.getMessage());
		}
	}
	
	/**
	 * @see <a href="http://bit.ly/2rgZwK4">S3BaseClientBuilder#serviceConfiguration</a>
	 */
	static void configureServiceConfiguration(Object builder, ServiceConfiguration serviceConfiguration) {
		if (serviceConfiguration == null) {
			return;
		}
		try {
			invokeMethod(builder, "serviceConfiguration", serviceConfiguration);
		} catch (IllegalStateException e) {
			log.warn(e.getMessage());
		}
	}
	
	/**
	 * @see software.amazon.awssdk.core.client.builder.SdkSyncClientBuilder#httpClientBuilder(SdkHttpClient.Builder)
	 */
	static void configureHttpSyncClientBuilder(Object builder, SdkHttpClient.Builder<?> httpClientBuilder) {
		configureHttpClientBuilder(builder, httpClientBuilder);
	}
	
	/**
	 * @see software.amazon.awssdk.core.client.builder.SdkAsyncClientBuilder#httpClientBuilder(SdkAsyncHttpClient.Builder)
	 */
	static void configureHttpAsyncClientBuilder(Object builder, SdkAsyncHttpClient.Builder<?> httpAsyncClientBuilder) {
		configureHttpClientBuilder(builder, httpAsyncClientBuilder);
	}
	
	private static void configureHttpClientBuilder(Object builder, Object httpClientBuilder) {
		if (httpClientBuilder == null) {
			return;
		}
		try {
			invokeMethod(builder, "httpClientBuilder", httpClientBuilder);
		} catch (IllegalStateException e) {
			log.warn(e.getMessage());
		}
	}
	
	/**
	 * @see software.amazon.awssdk.core.client.builder.SdkSyncClientBuilder#httpClient(SdkHttpClient) 
	 */
	static void configureHttpClient(Object builder, SdkHttpClient httpClient) {
		if (httpClient == null) {
			return;
		}
		try {
			invokeMethod(builder, "httpClient", httpClient);
		} catch (IllegalStateException e) {
			log.warn(e.getMessage());
		}
	}
	
	/**
	 * Build AWS client or config.
	 * 
	 * @param builder builder
	 * @return AWS client
	 * @see software.amazon.awssdk.utils.builder.SdkBuilder#build()
	 */
	static <T> T build(Object builder) {
		return invokeMethod(builder, "build");
	}
}
