# aws-client-spring-boot-configuration

[AWS SDK for Java](https://docs.aws.amazon.com/sdk-for-java/index.html) には数多くの
クライアントクラスが定義されており、各々のクライアントインスタンスが個別に設定値を持っています。

通常、[Spring](http://spring.io/) 環境で AWS SDK のクライアントを利用する場合は、次のように Java configuration を記述します。

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

aws-client-spring-boot-configuration はこれらの設定を省力化・標準化することにより、
AWS クライアントを簡単に利用できるようにします。

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


## 環境要件

* [Spring Boot](https://spring.io/projects/spring-boot) 2.0+
* [AWS SDK for Java](https://aws.amazon.com/jp/sdkforjava/) 1.11+ or 2.1+

AWS SDK for Java の v1 と v2 で利用すべきアノテーション及びプロパティ名が異なります。


## クライアントの登録 (共通)

aws-client-spring-boot-configuration は、アノテーションで指定した AWS クライアントを bean 登録します。

AWS SDK for Java v1 のクライアントは `@EnableAwsClientV1` によって、
AWS SDK for Java v2 のクライアントは `@EnableAwsClientV2` によって登録できます。

bean 名には、AWS クライアントインターフェースの FQCN を使います。


## AWSクライアントサービス名 (共通)

AWS クライアントはそれぞれ **service-name** を持ちます。
**service-name** は AWS クライアントインターフェイスが属する Java パッケージ名の、
`com.amazonaws.services.` または `software.amazon.awssdk.services.` 以降のセグメントです。
`.` は `-` に置き換えます。

例えば、`com.amazonaws.services.s3.AmazonS3` や `software.amazon.awssdk.services.s3.S3Client` の
**service-name** は `s3` です。
また、`software.amazon.awssdk.services.dynamodb.streams.DynamoDbStreamsClient` の
**service-name** は `dynamodb-streams` です。


## AWS SDK for Java バージョン毎の設定

* [AWS SDK for Java v1](docs/config-v1.ja.md)
* [AWS SDK for Java v2](docs/config-v2.ja.md)


## Contribution

1. Fork ([https://github.com/dai0304/aws-client-spring-boot-configuration/fork](https://github.com/dai0304/aws-client-spring-boot-configuration/fork))
2. Create a feature branch named like `feature/something_awesome_feature` from `development` branch
3. Commit your changes
4. Rebase your local changes against the `develop` branch
5. Create new Pull Request


## License

Copyright (C) 2018 Daisuke Miyamoto

Distributed under the Apache License v2.0. See the file copyright/LICENSE.txt.
