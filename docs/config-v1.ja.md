# Client configuration of AWS SDK for Java v1

## 設定プロパティ

AWS SDK for Java v1 用の Spring プロパティ名は、すべて `aws1.<service-name>` から始まります。

### リージョンを設定したい

Spring Boot のプロパティとして `aws1.<service-name>.region` の設定を行います。
ただし `EndpointConfiguration` を設定した場合はそちらの設定を優先します。

#### DynamoDB に対する設定例

```java
AmazonDynamoDB dynamoDB = AmazonDynamoDBClient.builder()
		.withRegion("eu-central-1")
		.build();
```

上記相当の DynamoDB クライアントは、次のように登録します。

```properties
aws1.dynamodbv2.region=eu-central-1
```

```java
@Configuration
@EnableAwsClientV1(AmazonDynamoDB.class)
static class ExampleRegionConfiguration {
}
```


### `EndpointConfiguration` を設定したい

Spring Boot のプロパティとして `aws1.<service-name>.endpoint.<property>` の設定を行います。

#### SNS に対する設定例

```java
AmazonSNS amazonSNS = AmazonSNSClient.builder()
		.withEndpointConfiguration(new EndpointConfiguration("http://localhost:4569", "us-east-1"))
		.build();
```

上記相当の SNS クライアントは、次のように登録します。

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


### `ClientConfiguration` を設定したい

Spring Boot のプロパティとして `aws1.<service-name>.client.<property>` の設定を行います。

#### SQS に対する設定例

```java
AmazonSQS amazonSQS = AmazonSQSClient.builder()
		.withClientConfiguration(new ClientConfiguration()
				.withConnectionTimeout(2500)
				.withSocketTimeout(25000))
		.build();
```

上記相当の SQS クライアントは、次のように登録します。

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

 
### async client の設定をしたい

Spring Boot のプロパティとして `aws1.<service-name>-async.*` の設定を行います。

`aws1.<service-name>.*` の設定は async client に影響しません。

#### SQS sync / async に対する設定例

```properties
aws1.sqs.client.socket-timeout=2000
aws1.sqs-async.client.socket-timeout=1000
```

この場合 `AmazonSQS` のソケットタイムアウトは 2 秒、
`AmazonSQSAsync` のソケットタイムアウトは 1 秒となります。

### 全クライアント共通の設定をしたい

Spring Boot のプロパティとして `aws1.default.*` の設定を行います。
ただし **service-name** に対する個別の設定をした場合はそちらの設定を優先します。

`aws1.default.*` の設定は async client に影響しません。
async client に対する共通設定は `aws1.default-async.*` をご利用ください。

#### 全体と SQS に対する設定例

```properties
aws1.default.client.socket-timeout=3000
aws1.sqs.client.socket-timeout=25000
```

この場合 SQS を除くすべてのクライアントのソケットタイムアウトは 3 秒、
SQS のソケットタイムアウトのみ 25 秒となります。

### `AmazonS3Builder` に対する `S3ClientOptions` を設定したい

Spring Boot のプロパティとして `aws1.s3.*` の設定を行います。

#### S3 に対する `S3ClientOptions` 設定例

```properties
aws1.s3.path-style-access-enabled=true
aws1.s3.chunked-encoding-disabled=true
aws1.s3.accelerate-mode-enabled=false
aws1.s3.payload-signing-enabled=true
aws1.s3.dualstack-enabled=true
aws1.s3.force-global-bucket-access-enabled=true
```

詳細は `S3ClientOptions` の javadoc を参照してください。

### `AmazonS3Encryption` クライアントを構築したい

`AmazonS3Encryption` に対するクライアントビルダーは
`com.amazonaws.services.s3.AmazonS3EncryptionClientBuilder` です。

このクライアントビルダーは `EncryptionMaterialsProvider` を要求します。
aws-client-spring-boot-configuration は、
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
