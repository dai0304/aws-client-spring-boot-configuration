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

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;

import org.junit.Test;
import org.junit.runner.RunWith;

import software.amazon.awssdk.services.ec2.Ec2Client;
import software.amazon.awssdk.services.ec2.model.Region;

/**
 * Integration test for {@link AwsClientV2Configuration}.
 *
 * @author miyamoto.daisuke
 * @since #version#
 */
@RunWith(SpringRunner.class)
@SpringBootTest
@ContextConfiguration
public class AwsV2ConfigurationIntegrationTest {
	
	@Test
	public void contextLoads() {
		// do nothing
	}
	
	
	@Slf4j
	@RequiredArgsConstructor
	@SpringBootApplication
	@EnableAwsClientV2(Ec2Client.class)
	@TestPropertySource(properties = "aws2.ec2.region=ap-northeast-1")
	static class TestApplication implements CommandLineRunner {
		
		/**
		 * Entry point.
		 * 
		 * @param args command line arguments
		 */
		public static void main(String[] args) { // -@cs[UncommentedMain]
			SpringApplication.run(TestApplication.class, args);
		}
		
		
		private final Ec2Client ec2;
		
		
		@Override
		public void run(String... args) throws Exception {
			ec2.describeRegions().regions().stream()
				.map(Region::regionName)
				.forEach(log::info);
		}
	}
}
