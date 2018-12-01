# Client configuration of AWS SDK for Java v1

## Configuration properties

Spring configuration property name for AWS SDK for Java v1 starts with `aws1.<service-name>`.

### How to set region?

Set `aws1.<service-name>.region` as the property of Spring Boot.
However, if you set `EndpointConfiguration`, that setting takes precedence.

#### Configuration example for DynamoDB

```java
AmazonDynamoDB dynamoDB = AmazonDynamoDBClient.builder()
		.withRegion("eu-central-1")
		.build();
```

The DynamoDB client equivalent to the above can be registered as follows:

```properties
aws1.dynamodbv2.region=eu-central-1
```

```java
@Configuration
@EnableAwsClientV1(AmazonDynamoDB.class)
static class ExampleRegionConfiguration {
}
```


### How to set `EndpointConfiguration`?

Set `aws1.<service-name>.endpoint.<property>` as the property of Spring Boot.

#### Configuration example for SNS

```java
AmazonSNS amazonSNS = AmazonSNSClient.builder()
		.withEndpointConfiguration(new EndpointConfiguration("http://localhost:4569", "us-east-1"))
		.build();
```

The SNS client equivalent to the above can be registered as follows:

```properties
aws1.sns.endpoint.service-endpoint=http://localhost:4569
aws1.sns.endpoint.signing-region=us-east-1
```

```java
@Configuration
@EnableAwsClientV1(AmazonSNS.class)
static class ExampleEndpointConfiguration {
}
```


### How to set `ClientConfiguration`?

Set `aws1.<service-name>.client.<property>` as the property of Spring Boot.

#### Configuration example for SQS

```java
AmazonSQS amazonSQS = AmazonSQSClient.builder()
		.withClientConfiguration(new ClientConfiguration()
				.withConnectionTimeout(2500)
				.withSocketTimeout(25000))
		.build();
```

The SQS client equivalent to the above can be registered as follows:

```properties
aws1.sqs.client.connection-timeout=2500
aws1.sqs.client.socket-timeout=25000
```
 
```java
@Configuration
@EnableAwsClientV1(AmazonSQS.class)
static class ExampleClientConfiguration {
}
```


### How to make individual settings with sync client and async client?

Set `aws1.<service-name>-async.*` as the property of Spring Boot.

#### Configuration example for SQS sync / async clients

```properties
aws1.sqs.client.socket-timeout=2000
aws1.sqs-async.client.socket-timeout=1000
```

In this case, the socket timeout for `AmazonSQS` is 2 seconds,
and the socket timeout for `AmazonSQSAsync` is a second.

### Default configuration for all clients

Set `aws1.default.*` as the property of Spring Boot.
However, if you set individual settings for **service-name**, that setting takes precedence.

#### Configuration example for all and SQS

```properties
aws1.default.client.socket-timeout=3000
aws1.sqs.client.socket-timeout=25000
```

In this case, the socket timeout for all clients except SQS is 3 seconds,
and the socket timeout for SQS is only 25 seconds.

### How do you set `S3ClientOptions` for `AmazonS3Builder`s?

Set `aws1.s3.*` as the property of Spring Boot.

#### Example of `S3ClientOptions` setting for S3

```properties
aws1.s3.path-style-access-enabled=true
aws1.s3.chunked-encoding-disabled=true
aws1.s3.accelerate-mode-enabled=false
aws1.s3.payload-signing-enabled=true
aws1.s3.dualstack-enabled=true
aws1.s3.force-global-bucket-access-enabled=true
```

For details, see the javadoc of `S3ClientOptions`.

### How to build `AmazonS3Encryption` client?

The client builder for `AmazonS3Encryption` is
`com.amazonaws.services.s3.AmazonS3EncryptionClientBuilder`.

This client builder requires `EncryptionMaterialsProvider`.
aws-client-spring-boot-configuration builds `AmazonS3Encryption`
using a bean named `com.amazonaws.services.s3.model.EncryptionMaterialsProvider`
as `EncryptionMaterialsProvider`. 

Currently you can not set `CryptoConfiguration` and `AWSKMS`.
We are waiting for Pull Request :-)


## Limits

### Configure `AWSCredentialsProvider`, `RequestMetricCollector`, `RequestHandler2`

You can not set `AWSCredentialsProvider`, `RequestMetricCollector`, `RequestHandler2` for each client.
We are waiting for Pull Request :-)

However, we think that we should not set a custom `AWSCredentialsProvider` for the clients.
If necessary, a discussion is necessary at issue.

### Multiple clients belonging to the same package other than sync/async

For example, the following client is different, but **service-name** is the same `dynamodbv2`,
so different settings can not be made for each.

* `com.amazonaws.services.dynamodbv2.AmazonDynamoDB`
* `com.amazonaws.services.dynamodbv2.AmazonDynamoDBStreams`

### Building 'AmazonKinesisVideoPutMedia`

`AmazonKinesisVideoPutMediaClientBuilder` is not a subtype of `AwsClientBuilder`.
Although it supports client registration, it can not configure the client.
