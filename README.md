# aws-client-spring-boot-configuration

Many AWS client classes are defined, and each client instance has its own configurations.

Usualy, to use the AWS client in the Spring environment, describe the Java configuration as following:

```java
@Configuration
class AwsClientConfiguration {
  
  @Bean
  AmazonS3 amazonS3() {
    // unconfigurable!!
    return AmazonS3ClientBuilder.defaultClient();
  }
  
  @Bean
  AmazonSQS amazonSQS() {
    ClientConfiguration clientConfig = new ClientConfiguration()
        .withConnectionTimeout(2500) // hard-coded!!
        .withSocketTimeout(25000);
    return AmazonSQSClientBuilder.standard()
        .withClientConfiguration(clientConfig)
        .build();
  }
  
  @Bean
  AmazonSNS amazonSNS(
      @Value("aws.sns.endpoint.service-endpoint") String serviceEndpoint,
      @Value("aws.sns.endpoint.signing-region") String signingRegion) {
    EndpointConfiguration endpointConfig = new EndpointConfiguration(serviceEndpoint, signingRegion);
    return AmazonSNSClientBuilder.standard()
        .withEndpointConfiguration(endpointConfig)
        .build();
  }
  
  @Bean
  AmazonDynamoDB amazonDynamoDB(
      // inconsisitent property name!!
      @Value("dynamodb.region") String region) {
    return AmazonDynamoDBClientBuilder.standard()
        .withRegion(region)
        .build();
  }
}
```

aws-client-spring-boot-configuration standardizes and makes easy these configuration.

```java
@Configuration
@EnableAwsClient({
  AmazonS3.class,
  AmazonSQS.class,
  AmazonSNS.class,
  AmazonDynamoDB.class
})
class AwsClientConfiguration {
}
```

```properties
aws.sqs.client.connection-timeout=2500
aws.sqs.client.socket-timeout=25000
```


## Environment Prerequisites

* [Spring Boot](https://spring.io/projects/spring-boot) 2.0.x or 2.1.x
* [AWS SDK for Java](https://aws.amazon.com/jp/sdkforjava/) 1.11.x


## Auto client registration

aws-client-spring-boot-configuration registers AWS clients specified by `@EnableAwsClient` annotation.

The bean name to be registered is the FQCN of the AWS client interface.


## Client configuration

Each AWS client has **service-name**.
**service-name** is the segment after `com.amazonaws.services.`
of the Java package name to which the AWS client interface belongs.

For example, **service-name** for `com.amazonaws.services.s3.AmazonS3` is` s3`.

Note: At this time, there are no AWS clients belonging to multiplehierarchical subpackages,
but if a client such as `com.amazonaws.services.foo.bar.BazQux` appears in the future,
`.` should be replaced to `-` in it.
Specifically, `foo-bar` is **service-name** for this client.

### How to set `ClientConfiguration`?

Set `aws.<service-name>.client.<property>` as the property of Spring Boot.

#### Configuration example for SQS

```properties
aws.sqs.client.connection-timeout=2500
aws.sqs.client.socket-timeout=25000
```
 
### How to set `EndpointConfiguration`?

Set `aws.<service-name>.endpoint.<property>` as the property of Spring Boot.

#### Configuration example for SNS

```properties
aws.sns.endpoint.service-endpoint=http://localhost:4569
aws.sns.endpoint.signing-region=us-east-1
```

### How to set region?

Set `aws.<service-name>.region` as the property of Spring Boot.
However, if you set `EndpointConfiguration`, that setting takes precedence.

#### Configuration example for DynamoDB

```properties
aws.dynamodbv2.region=eu-central-1
```

### How to make individual settings with sync client and async client?

Set `aws.<service-name>-async.*` as the property of Spring Boot.

#### Configuration example for SQS sync / async clients

```properties
aws.sqs.client.socket-timeout=2000
aws.sqs-async.client.socket-timeout=1000
```

In this case, the socket timeout for `AmazonSQS` is 2 seconds,
and the socket timeout for `AmazonSQSAsync` is a second.

### Default configuration for all clients

Set `aws.default.*` as the property of Spring Boot.
However, if you set individual settings for **service-name**, that setting takes precedence.

#### Configuration example for all and SQS

```properties
aws.default.client.socket-timeout=3000
aws.sqs.client.socket-timeout=25000
```

In this case, the socket timeout for all clients except SQS is 3 seconds,
and the socket timeout for SQS is only 25 seconds.

### How do you set `S3ClientOptions` for `AmazonS3Builder`s?

Set `aws.s3.*` as the property of Spring Boot.

#### Example of `S3ClientOptions` setting for S3

```properties
aws.s3.path-style-access-enabled=true
aws.s3.chunked-encoding-disabled=true
aws.s3.accelerate-mode-enabled=false
aws.s3.payload-signing-enabled=true
aws.s3.dualstack-enabled=true
aws.s3.force-global-bucket-access-enabled=true
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


## Contribution

1. Fork ([https://github.com/dai0304/aws-client-spring-boot-configuration/fork](https://github.com/dai0304/aws-client-spring-boot-configuration/fork))
2. Create a feature branch named like `feature/something_awesome_feature` from `development` branch
3. Commit your changes
4. Rebase your local changes against the `develop` branch
5. Create new Pull Request


## License

Copyright (C) 2018 Daisuke Miyamoto

Distributed under the Apache License v2.0. See the file copyright/LICENSE.txt.
