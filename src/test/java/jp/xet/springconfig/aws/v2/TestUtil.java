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

import org.springframework.test.util.ReflectionTestUtils;

import software.amazon.awssdk.core.client.config.SdkClientConfiguration;
import software.amazon.awssdk.http.apache.ApacheHttpClient;
import software.amazon.awssdk.http.apache.internal.ApacheHttpRequestConfig;
import software.amazon.awssdk.http.nio.netty.NettyNioAsyncHttpClient;
import software.amazon.awssdk.http.nio.netty.internal.NettyConfiguration;
import software.amazon.awssdk.utils.AttributeMap;

class TestUtil {
	
	static SdkClientConfiguration extractClientConfig(Object client) {
		return (SdkClientConfiguration) ReflectionTestUtils.getField(client, "clientConfiguration");
	}
	
	static ApacheHttpRequestConfig extractRequestConfig(ApacheHttpClient apacheHttpClient) {
		return (ApacheHttpRequestConfig) ReflectionTestUtils.getField(apacheHttpClient, "requestConfig");
	}
	
	static AttributeMap extractResolvedOptions(ApacheHttpClient apacheHttpClient) {
		return (AttributeMap) ReflectionTestUtils.getField(apacheHttpClient, "resolvedOptions");
	}
	
	static NettyConfiguration extractConfig(NettyNioAsyncHttpClient nettyNioAsyncHttpClient) {
		return (NettyConfiguration) ReflectionTestUtils.getField(nettyNioAsyncHttpClient, "configuration");
	}
}
