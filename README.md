# aws-client-spring-boot-configuration

Many AWS client classes are defined in [AWS SDK for Java](https://docs.aws.amazon.com/sdk-for-java/index.html),
and each client instance has its own configurations.

Usually, to use the AWS client in the [Spring](http://spring.io/) environment, describe the Java configuration as following:

```java
@Configuration
public class AwsClientConfiguration {
  
  @Bean
  public AmazonS3 amazonS3() {
    // unconfigurable!!
    return AmazonS3ClientBuilder.defaultClient();
  }
  
  @Bean
  public AmazonSQS amazonSQS() {
    ClientConfiguration clientConfig = new ClientConfiguration()
        .withConnectionTimeout(2500) // hard-coded!!
        .withSocketTimeout(25000);
    return AmazonSQSClientBuilder.standard()
        .withClientConfiguration(clientConfig)
        .build();
  }
  
  @Bean
  public AmazonSNS amazonSNS(
      @Value("aws.sns.endpoint.service-endpoint") String serviceEndpoint,
      @Value("aws.sns.endpoint.signing-region") String signingRegion) {
    EndpointConfiguration endpointConfig = new EndpointConfiguration(serviceEndpoint, signingRegion);
    return AmazonSNSClientBuilder.standard()
        .withEndpointConfiguration(endpointConfig)
        .build();
  }
  
  @Bean
  public AmazonDynamoDB amazonDynamoDB(
      // inconsistent property name!!
      @Value("dynamodb.region") String region) {
    return AmazonDynamoDBClientBuilder.standard()
        .withRegion(region)
        .build();
  }
  
  @Bean
  public S3Client s3Client() {
    // AWS SDK for Java v2!!
    return S3Client.create();
  }
}
```

aws-client-spring-boot-configuration standardizes and makes easy these configuration.

```java
@Configuration
@EnableAwsClientV1({
  AmazonS3.class,
  AmazonSQS.class,
  AmazonSNS.class,
  AmazonDynamoDB.class
})
@EnableAwsClientV2({ S3Client.class })
public class AwsClientConfiguration {
}
```

```properties
aws1.sqs.client.connection-timeout=2500
aws1.sqs.client.socket-timeout=25000
```


## Environment Prerequisites

* [Spring Boot](https://spring.io/projects/spring-boot) 2.0+
* [AWS SDK for Java](https://aws.amazon.com/jp/sdkforjava/) 1.11+ or 2.1+

Annotations and property names to be used are different for AWS SDK for Java v1 and v2. 


## Client registration

aws-client-spring-boot-configuration registers AWS clients specified by the annotations.

You can register AWS SDK for Java v1 client to use `@EnableAwsClientV1`,
and v2 client can be registered by `EnableAwsClientV2`. 

The bean name to be registered is the FQCN of the AWS client interface.


## About AWS client service name

Each AWS client has **service-name**.
**service-name** is the segment after `com.amazonaws.services.`
of the Java package name to which the AWS client interface belongs.
`.` in the rest segment should be replaced with `-`

For example,
**service-name** for `com.amazonaws.services.s3.AmazonS3` or `software.amazon.awssdk.services.s3.S3Client` is` s3`,
and **service-name** for `software.amazon.awssdk.services.dynamodb.streams.DynamoDbStreamsClient` is` dynamodb-streams`.


## Client configuration

* [AWS SDK for Java v1](docs/config-v1.md)
* [AWS SDK for Java v2](docs/config-v2.md)


## Contribution

1. Fork ([https://github.com/dai0304/aws-client-spring-boot-configuration/fork](https://github.com/dai0304/aws-client-spring-boot-configuration/fork))
2. Create a feature branch named like `feature/something_awesome_feature` from `development` branch
3. Commit your changes
4. Rebase your local changes against the `develop` branch
5. Create new Pull Request


## License

Copyright (C) 2018 Daisuke Miyamoto

Distributed under the Apache License v2.0. See the file [LICENSE](LICENSE).
