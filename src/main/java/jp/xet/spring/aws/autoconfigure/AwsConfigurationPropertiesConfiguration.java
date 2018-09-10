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

import java.util.HashMap;

import lombok.Data;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.ConfigurationPropertiesBindingPostProcessor;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.amazonaws.ClientConfiguration;
import com.amazonaws.client.builder.AwsClientBuilder.EndpointConfiguration;

/**
 * TODO miyamoto.daisuke.
 *
 * @author miyamoto.daisuke
 * @since #version#
 */
@Configuration
@EnableConfigurationProperties
public class AwsConfigurationPropertiesConfiguration {
	
	@Bean
	public AwsClientPropertiesMap awsClientPropertiesMap(ApplicationContext applicationContext) throws Exception {
		ConfigurationPropertiesBindingPostProcessor p = new ConfigurationPropertiesBindingPostProcessor();
		p.setApplicationContext(applicationContext);
		p.afterPropertiesSet();
		
		AwsClientPropertiesMap map = new AwsClientPropertiesMap();
		p.postProcessBeforeInitialization(map, "awsClientPropertiesMap");
		return map;
	}
	
	@Bean
	public AwsS3ClientProperties awsS3ClientProperties(ApplicationContext applicationContext) throws Exception {
		ConfigurationPropertiesBindingPostProcessor p = new ConfigurationPropertiesBindingPostProcessor();
		p.setApplicationContext(applicationContext);
		p.afterPropertiesSet();
		
		AwsS3ClientProperties awsS3ClientProperties = new AwsS3ClientProperties();
		p.postProcessBeforeInitialization(awsS3ClientProperties, "awsS3ClientProperties");
		return awsS3ClientProperties;
	}
	
	@SuppressWarnings("serial")
	@ConfigurationProperties(value = "aws", ignoreInvalidFields = true)
	static class AwsClientPropertiesMap extends HashMap<String, AwsClientProperties> {
	}
	
	
	@Data
	static class AwsClientProperties {
		
		private ClientConfiguration client;
		
		private MutableEndpointConfiguration endpoint;
		
		private String region;
		
		private boolean enabled = true;
		
		
		EndpointConfiguration getEndpoint() {
			return endpoint == null ? null : endpoint.toEndpointConfiguration();
		}
	}
	
	/**
	 * @see <a href="https://github.com/spring-projects/spring-boot/issues/8762">spring-boot#8762</a>
	 */
	@Data
	static class MutableEndpointConfiguration {
		
		private String serviceEndpoint;
		
		private String signingRegion;
		
		
		EndpointConfiguration toEndpointConfiguration() {
			if (serviceEndpoint != null) {
				return new EndpointConfiguration(serviceEndpoint, signingRegion);
			}
			return null;
		}
	}
	
	@Data
	@ConfigurationProperties(value = "aws.s3", ignoreInvalidFields = true)
	static class AwsS3ClientProperties {
		
		private Boolean pathStyleAccessEnabled;
		
		private Boolean chunkedEncodingDisabled;
		
		private Boolean accelerateModeEnabled;
		
		private Boolean payloadSigningEnabled;
		
		private Boolean dualstackEnabled;
		
		private Boolean forceGlobalBucketAccessEnabled;
		
	}
}
