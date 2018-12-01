# Client configuration of AWS SDK for Java v2

## Configuration properties

Spring configuration property name for AWS SDK for Java v2 starts with `aws2.<service-name>`.

### How to set region?

Set `aws2.<service-name>.region` as the property of Spring Boot.

#### Configuration example for DynamoDB

```java
DynamoDbClient dynamoDB = DynamoDbClient.builder()
		.region(Region.EU_CENTRAL_1)
		.build();
```

The DynamoDB client equivalent to the above can be registered as follows:

```properties
aws2.dynamodb.region=eu-central-1
```

```java
@Configuration
@EnableAwsClientV2(DynamoDbClient.class)
static class ExampleRegionConfiguration {
}
```


### How to set endpoint?

Set `aws2.<service-name>.endpoint` as the property of Spring Boot.

#### Configuration example for SNS

```java
SnsClient snsClient = SnsClient.builder()
		.endpointOverride(URI.create("http://localhost:4569"))
		.build();
```

The SNS client equivalent to the above can be registered as follows:

```properties
aws2.sns.endpoint=http://localhost:4569
```

```java
@Configuration
@EnableAwsClientV2(SnsClient.class)
static class ExampleEndpointConfiguration {
}
```


### Hot to set `AwsCredentialsProvider`?

Set `aws2.<service-name>.credentials-provider-bean-name` as the property of Spring Boot.
It is required to register `AwsCredentialsProvider` bean. 

#### Configuration example for SQS

```java
StaticCredentialsProvider credProvider = StaticCredentialsProvider
		.create(AwsBasicCredentials
				.create("...", "..."));
SqsClient sqsClient = SqsClient.builder()
		.credentialsProvider(credProvider)
		.build();
```

The SQS client equivalent to the above can be registered as follows:

```properties
aws2.sqs.credentials-provider-bean-name=sqsCredentialsProvider
```

```java
@Configuration
@EnableAwsClientV2(SqsClient.class)
static class ExampleCredentialsProviderConfiguration {
	
	@Bean
	public AwsCredentialsProvider sqsCredentialsProvider() {
		return StaticCredentialsProvider
				.create(AwsBasicCredentials.create("...", "..."));
	}
}
```

### How to set `SdkHttpClient.Builder` or `SdkAsyncHttpClient.Builder`?

Set `aws2.<service-name>.http-client-builder-bean-name` as the property of Spring Boot.
It is required to register `SdkHttpClient.Builder` bean for sync-client.
If you want to register async-client, you should register `SdkAsyncHttpClient.Builder` bean.

#### Amazon SES に対する設定例

```java
SdkHttpClient.Builder builder = ApacheHttpClient.builder()
		.connectionTimeout(Duration.ofMinutes(2500))
		.socketTimeout(Duration.ofSeconds(25));
SesClient sqsClient = SesClient.builder()
		.httpClientBuilder(builder)
		.build();
```

The SES client equivalent to the above can be registered as follows:

```properties
aws2.ses.http-client-builder-bean-name=sesHttpClientBuilder
```

```java
@Configuration
@EnableAwsClientV2(SesClient.class)
static class ExampleHttpClientBuilderConfiguration {
	
	@Bean
	public SdkHttpClient.Builder sesHttpClientBuilder() {
		return ApacheHttpClient.builder()
				.connectionTimeout(Duration.ofMillis(2500))
				.socketTimeout(Duration.ofSeconds(25));
	}
}
```
 
 
### How to make individual settings with sync client and async client?

Set `aws2.<service-name>-async.*` as the property of Spring Boot.

#### Configuration example for SQS sync / async clients

```properties
aws2.sqs.client.region=us-east-1
aws2.sqs-async.region=eu-central-1
```

In this case, the region for `SqsClient` is `us-east-1`,
and the region for `SqsAsyncClient` is `eu-central-1`.

### Default configuration for all clients

Set `aws2.default.*` as the property of Spring Boot.
However, if you set individual settings for **service-name**, that setting takes precedence.

#### Configuration example for all and SQS

```properties
aws2.default.region=ap-northeast-1
aws2.sqs.region=eu-west-2
```

In this case, the region for all clients except SQS is `ap-northeast-1`,
and the region for SQS is`eu-west-2`.

### How do you set `S3Configuration` for `S3ClientBuilder`s?

Set `aws2.s3.*` as the property of Spring Boot.

#### Example of `S3Configuration` setting for S3

```properties
aws2.s3.path-style-access-enabled=true
aws2.s3.chunked-encoding-enabled=true
aws2.s3.accelerate-mode-enabled=false
aws2.s3.dualstack-enabled=true
aws2.s3.checksum-validation-enabled=true
```

For details, see the javadoc of `S3Configuration`.
