# Client configuration of AWS SDK for Java v2

## 設定プロパティ

AWS SDK for Java v2 用の Spring プロパティ名は、すべて `aws2.<service-name>` から始まります。

### リージョンを設定したい

Spring Boot のプロパティとして `aws2.<service-name>.region` の設定を行います。

#### DynamoDB に対する設定例

```java
DynamoDbClient dynamoDB = DynamoDbClient.builder()
		.region(Region.EU_CENTRAL_1)
		.build();
```

上記相当の DynamoDB クライアントは、次のように登録します。

```properties
aws2.dynamodb.region=eu-central-1
```

```java
@Configuration
@EnableAwsClientV2(DynamoDbClient.class)
static class ExampleRegionConfiguration {
}
```


### エンドポイントを設定したい

Spring Boot のプロパティとして `aws2.<service-name>.endpoint` の設定を行います。

#### SNS に対する設定例

```java
SnsClient snsClient = SnsClient.builder()
		.endpointOverride(URI.create("http://localhost:4569"))
		.build();
```

上記相当の SNS クライアントは、次のように登録します。

```properties
aws2.sns.endpoint=http://localhost:4569
```

```java
@Configuration
@EnableAwsClientV2(SnsClient.class)
static class ExampleEndpointConfiguration {
}
```


### `AwsCredentialsProvider` を設定したい

Spring Boot のプロパティとして `aws2.<service-name>.credentials-provider-bean-name` の設定を行います。
`AwsCredentialsProvider` 型の bean を指定した名前で登録する必要があります。 

#### SQS に対する設定例

```java
StaticCredentialsProvider credProvider = StaticCredentialsProvider
		.create(AwsBasicCredentials
				.create("...", "..."));
SqsClient sqsClient = SqsClient.builder()
		.credentialsProvider(credProvider)
		.build();
```

上記相当の SQS クライアントは、次のように登録します。

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
 
### `SdkHttpClient.Builder` や `SdkAsyncHttpClient.Builder` を設定したい

Spring Boot のプロパティとして `aws2.<service-name>.http-client-builder-bean-name` の設定を行います。
同期クライアントに対しては `SdkHttpClient.Builder` 型、
非同期クライアントに対しては `SdkAsyncHttpClient.Builder` 型の bean を
指定した名前で登録する必要があります。 

#### Amazon SES に対する設定例

```java
SdkHttpClient.Builder builder = ApacheHttpClient.builder()
		.connectionTimeout(Duration.ofMinutes(2500))
		.socketTimeout(Duration.ofSeconds(25));
SesClient sqsClient = SesClient.builder()
		.httpClientBuilder(builder)
		.build();
```

上記相当の SQS クライアントは、次のように登録します。

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
 
 
### async client の設定をしたい

Spring Boot のプロパティとして `aws2.<service-name>-async.*` の設定を行います。

`aws2.<service-name>.*` の設定は async client に影響しません。

#### SQS sync / async に対する設定例

```properties
aws2.sqs.client.region=us-east-1
aws2.sqs-async.region=eu-central-1
```

この場合 `SqsClient` のリージョンは `us-east-1`、
`SqsAsyncClient` のリージョンは `eu-central-1` となります。

### 全クライアント共通の設定をしたい

Spring Boot のプロパティとして `aws2.default.*` の設定を行います。
ただし **service-name** に対する個別の設定をした場合はそちらの設定を優先します。

`aws2.default.*` の設定は async client に影響しません。
async client に対する共通設定は `aws2.default-async.*` をご利用ください。

#### 全体と SQS に対する設定例

```properties
aws2.default.region=ap-northeast-1
aws2.sqs.region=eu-west-2
```

この場合 SQS を除くすべてのクライアントのリージョンは `ap-northeast-1`、
SQS のリージョンのみ `eu-west-2` となります。

### `S3ClientBuilder` に対する `S3Configuration` (serviceConfiguration) を設定したい

Spring Boot のプロパティとして `aws2.s3.*` の設定を行います。

#### S3 に対する `S3Configuration` 設定例

```properties
aws2.s3.path-style-access-enabled=true
aws2.s3.chunked-encoding-enabled=true
aws2.s3.accelerate-mode-enabled=false
aws2.s3.dualstack-enabled=true
aws2.s3.checksum-validation-enabled=true
```

詳細は `S3Configuration` の javadoc を参照してください。
