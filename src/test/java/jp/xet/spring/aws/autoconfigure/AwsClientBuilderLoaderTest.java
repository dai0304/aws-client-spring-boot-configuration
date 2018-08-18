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
package jp.xet.spring.aws.autoconfigure;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Set;

import lombok.extern.slf4j.Slf4j;

import org.junit.Test;

/**
 * Test for {@link AwsClientBuilderLoader}.
 *
 * @author miyamoto.daisuke
 * @since #version#
 */
@Slf4j
public class AwsClientBuilderLoaderTest {
	
	@Test
	public void testLoadBuilderNames() {
		// exercise
		Set<String> actual = AwsClientBuilderLoader.loadBuilderNames();
		// verify
		assertThat(actual).contains(
				"com.amazonaws.services.s3.AmazonS3ClientBuilder",
				"com.amazonaws.services.sqs.AmazonSQSClientBuilder",
				"com.amazonaws.services.sqs.AmazonSQSAsyncClientBuilder",
				"com.amazonaws.services.sns.AmazonSNSClientBuilder",
				"com.amazonaws.services.sns.AmazonSNSAsyncClientBuilder",
				"com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder",
				"com.amazonaws.services.elasticloadbalancing.AmazonElasticLoadBalancingClientBuilder",
				"com.amazonaws.services.elasticloadbalancingv2.AmazonElasticLoadBalancingClientBuilder");
	}
}
