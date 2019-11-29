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
package jp.xet.springconfig.aws.v1;

import java.net.URI;
import java.util.List;

import org.springframework.test.util.ReflectionTestUtils;

import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.handlers.RequestHandler2;

class TestUtil {
	
	static String extractSigningRegion(Object client) {
		return (String) ReflectionTestUtils.getField(client, "signingRegion");
	}
	
	static URI extractEndpoint(Object client) {
		return (URI) ReflectionTestUtils.getField(client, "endpoint");
	}
	
	static String extractEndpointPrefix(Object client) {
		return (String) ReflectionTestUtils.getField(client, "endpointPrefix");
	}
	
	static AWSCredentialsProvider extractCredentialsProvider(Object client) {
		return (AWSCredentialsProvider) ReflectionTestUtils.getField(client, "awsCredentialsProvider");
	}
	
	@SuppressWarnings("unchecked")
	static List<RequestHandler2> extractRequestHandlers(Object client) {
		return (List<RequestHandler2>) ReflectionTestUtils.getField(client, "requestHandler2s");
	}
	
	//requestHandler2s
}
