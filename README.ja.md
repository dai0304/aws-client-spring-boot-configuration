# aws-client-spring-boot-autoconfigure

AWS には数多くのクライアントクラスが定義されており、各々のクライアントインスタンスが個別に設定値を持っています。

通常、Spring 環境で AWS クライアントを利用する場合は、次のように Java configuration を記述します。

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

aws-client-spring-boot-autoconfigure はこれらの設定を省力化・標準化することにより、
AWS クライアントを簡単に利用できるようにします。

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


## 環境要件

* [Spring Boot](https://spring.io/projects/spring-boot) 2.0.x
* [AWS SDK for Java](https://aws.amazon.com/jp/sdkforjava/) 1.11.x


## クライアントの登録

aws-client-spring-boot-autoconfigure は、`@EnableAwsClient` アノテーションで指定した AWS クライアントを bean 登録します。

登録する bean 名には、AWS クライアントインターフェースの FQCN を使います。


## クライアントの設定

AWS クライアントはそれぞれ **service-name** を持ちます。
**service-name** は AWS クライアントインターフェイスが属する Java パッケージ名の、
`com.amazonaws.services.` 以降のセグメントです。

例えば、`com.amazonaws.services.s3.AmazonS3` の **service-name** は `s3` です。

Note: 現時点では、複数階層のサブパッケージに属する AWS クライアントは存在しませんが、
将来 `com.amazonaws.services.foo.bar.BazQux` のようなクライアントが登場した場合は、
`.` を `-` に置き換えます。
具体的には `foo-bar` がこのクライアントの **service-name** です。

### `ClientConfiguration` を設定したい

Spring Boot のプロパティとして `aws.<service-name>.client.<property>` の設定を行います。

#### SQS に対する設定例

```properties
aws.sqs.client.connection-timeout=2500
aws.sqs.client.socket-timeout=25000
```
 
### `EndpointConfiguration` を設定したい

Spring Boot のプロパティとして `aws.<service-name>.endpoint.<property>` の設定を行います。

#### SNS に対する設定例

```properties
aws.sns.endpoint.service-endpoint=http://localhost:4569
aws.sns.endpoint.signing-region=us-east-1
```

### リージョンを設定したい

Spring Boot のプロパティとして `aws.<service-name>.region` の設定を行います。
ただし `EndpointConfiguration` を設定した場合はそちらの設定を優先します。

#### DynamoDB に対する設定例

```properties
aws.dynamodbv2.region=eu-central-1
```

### sync client と async client で個別の設定をしたい

Spring Boot のプロパティとして `aws.<service-name>-async.*` の設定を行います。

#### SQS sync / async に対する設定例

```properties
aws.sqs.client.socket-timeout=2000
aws.sqs-async.client.socket-timeout=1000
```

この場合 `AmazonSQS` のソケットタイムアウトは 2 秒、
`AmazonSQSAsync` のソケットタイムアウトは 1 秒となります。

### 全クライアント共通の設定をしたい

Spring Boot のプロパティとして `aws.default.*` の設定を行います。
ただし **service-name** に対する個別の設定をした場合はそちらの設定を優先します。

#### 全体と SQS に対する設定例

```properties
aws.default.client.socket-timeout=3000
aws.sqs.client.socket-timeout=25000
```

この場合SQS を除くすべてのクライアントのソケットタイムアウトは 3 秒、
SQS のソケットタイムアウトのみ 25 秒となります。

### `AmazonS3Builder` に対する `S3ClientOptions` を設定したい

Spring Boot のプロパティとして `aws.s3.*` の設定を行います。

#### S3 に対する `S3ClientOptions` 設定例

```properties
aws.s3.path-style-access-enabled=true
aws.s3.chunked-encoding-disabled=true
aws.s3.accelerate-mode-enabled=false
aws.s3.payload-signing-enabled=true
aws.s3.dualstack-enabled=true
aws.s3.force-global-bucket-access-enabled=true
```

詳細は `S3ClientOptions` の javadoc を参照してください。

### `AmazonS3Encryption` クライアントを構築したい

`AmazonS3Encryption` に対するクライアントビルダーは
`com.amazonaws.services.s3.AmazonS3EncryptionClientBuilder` です。

このクライアントビルダーは `EncryptionMaterialsProvider` を要求します。
aws-client-spring-boot-autoconfigure は、
`com.amazonaws.services.s3.model.EncryptionMaterialsProvider`
という名前を持つ bean を `EncryptionMaterialsProvider` として利用し、
`AmazonS3Encryption` をビルドします。

なお、現在 `CryptoConfiguration` と `AWSKMS` の設定はできません。
Pull Request をお待ちしております。


## 制限事項

### `AWSCredentialsProvider`, `RequestMetricCollector`, `RequestHandler2` の設定はできません

各クライアントに対する `AWSCredentialsProvider`, `RequestMetricCollector`, `RequestHandler2` の設定はできません。
Pull Request をお待ちしております。

ただし、我々はクライアントに対してカスタムした `AWSCredentialsProvider` を設定すべきではないと考えています。
もし必要があれば、issue にてディスカッションが必要です。

### 同一パッケージに属する sync / async 以外の複数クライアントで個別の設定はできません

例えば次のクライアントは別のものですが、**service-name** が同一 `dynamodbv2` であるため、
それぞれに異なった設定はできません。

* `com.amazonaws.services.dynamodbv2.AmazonDynamoDB`
* `com.amazonaws.services.dynamodbv2.AmazonDynamoDBStreams`

### `AmazonKinesisVideoPutMedia` の設定はできません

`AmazonKinesisVideoPutMediaClientBuilder` は `AwsClientBuilder` のサブタイプではありません。
クライアントの登録はサポートしますが、プロパティからの設定を行うことはできません。


## Contribution

1. Fork ([https://github.com/dai0304/aws-client-spring-boot-autoconfigure/fork](https://github.com/dai0304/aws-client-spring-boot-autoconfigure/fork))
2. Create a feature branch named like `feature/something_awesome_feature` from `development` branch
3. Commit your changes
4. Rebase your local changes against the `develop` branch
5. Create new Pull Request


## License

Copyright (C) 2018 Daisuke Miyamoto

Distributed under the Apache License v2.0. See the file copyright/LICENSE.txt.
