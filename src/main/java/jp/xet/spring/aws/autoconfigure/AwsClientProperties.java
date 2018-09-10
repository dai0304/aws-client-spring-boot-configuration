/*
 * Copyright 2017 the original author or authors.
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
package jp.xet.spring.aws.autoconfigure;

import lombok.Data;

import com.amazonaws.ClientConfiguration;
import com.amazonaws.client.builder.AwsClientBuilder.EndpointConfiguration;

/**
 * TODO miyamoto.daisuke.
 *
 * @author miyamoto.daisuke
 * @since #version#
 */
@Data
class AwsClientProperties {
	
	private ClientConfiguration client;
	
	private MutableEndpointConfiguration endpoint;
	
	private String region;
	
	private boolean enabled = true;
	
	
	EndpointConfiguration getEndpoint() {
		return endpoint == null ? null : endpoint.toEndpointConfiguration();
	}
}
