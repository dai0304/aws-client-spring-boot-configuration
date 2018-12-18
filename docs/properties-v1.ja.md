# AWS SDK for Java v1 configuration properties

| property                                | type    | typical default *1
| --------------------------------------- | ------- | ----
| `aws1.*.region` \*2                     | string  | (auto)
| `aws1.*.credentials-provider-bean-name` \*3 | string | `null`
| `aws1.*.endpoint.service-endpoint` \*4  | string  | (auto)
| `aws1.*.endpoint.signing-region`  \*4   | string  | (auto)
| `aws1.*.client.user-agent-prefix` \*5   | string  | (auto)
| `aws1.*.client.user-agent-suffix` \*5   | string  | `null`
| `aws1.*.client.max-error-retry` \*5     | number  | `-1`
| `aws1.*.client.local-address` \*5       |         | `null`
| `aws1.*.client.protocol` \*5            | `HTTP|HTTPS` | `HTTPS`
| `aws1.*.client.proxy-host` \*5          | string  | `null`
| `aws1.*.client.proxy-port` \*5          | number  | `-1`
| `aws1.*.client.proxy-username` \*5      | string  | `null`
| `aws1.*.client.proxy-password` \*5      | string  | `null`
| `aws1.*.client.proxy-domain` \*5        | string  | `null`
| `aws1.*.client.proxy-workstation` \*5   | string  | `null`
| `aws1.*.client.non-proxy-hosts` \*5     | string  | `null`
| `aws1.*.client.disable-socket-proxy` \*5 | boolean | `false`
| `aws1.*.client.preemptive-basic-proxy-auth` \*5 | boolean | `false`
| `aws1.*.client.max-connections` \*5     | number  | `50`
| `aws1.*.client.socket-timeout` \*5      | number  | `50000`
| `aws1.*.client.connection-timeout` \*5  | number  | `10000`
| `aws1.*.client.request-timeout` \*5     | number  | `0` (infinity)
| `aws1.*.client.client-execution-timeout` \*5 | number | `0` (infinity)
| `aws1.*.client.throttle-retries` \*5    | boolean | `true`
| `aws1.*.client.use-reaper` \*5          | boolean | `true`
| `aws1.*.client.use-gzip` \*5            | boolean | `false`
| `aws1.*.client.signer-override` \*5     | string  | `null`
| `aws1.*.client.connection-TTL` \*5      | number  | `-1`
| `aws1.*.client.connection-max-idle-millis` \*5 | number | `60000`
| `aws1.*.client.validate-after-inactivity-millis` \*5 | number | `5000`
| `aws1.*.client.tcp-keep-alive` \*5      | boolean | `false`
| `aws1.*.client.cache-response-metadata` \*5 | boolean | `true`
| `aws1.*.client.response-metadata-cache-size` \*5 | number | `50`
| `aws1.*.client.use-expect-continue` \*5 | boolean | `true`
| `aws1.*.client.max-consecutive-retries-before-throttling` \*5 | number | `100`
| `aws1.s3.path-style-access-enabled` \*6 | boolean | `false`
| `aws1.s3.chunked-encoding-disabled` \*6 | boolean | `false`
| `aws1.s3.accelerate-mode-enabled` \*6   | boolean | `false`
| `aws1.s3.payload-signing-enabled` \*6   | boolean | `false`
| `aws1.s3.dualstack-enabled` \*6         | boolean | `false`
| `aws1.s3.force-global-bucket-access-enabled` \*6 | boolean | `false`

* \*1: バージョンやサービス毎にデフォルト値が異なる場合があります。参考: [PredefinedClientConfigurations](https://github.com/aws/aws-sdk-java/blob/master/aws-java-sdk-core/src/main/java/com/amazonaws/PredefinedClientConfigurations.java).
* \*2: signing-region または service-endpoint を明示的に指定した場合は、この値を無視します。 参考: [AwsClientBuilder#setRegion](https://github.com/aws/aws-sdk-java/blob/master/aws-java-sdk-core/src/main/java/com/amazonaws/client/builder/AwsClientBuilder.java#L215).
* \*3: 利用したい `AWSCredentialsProvider` の bean 名を指定します。参考: [AwsClientBuilder#setCredentials](https://github.com/aws/aws-sdk-java/blob/master/aws-java-sdk-core/src/main/java/com/amazonaws/client/builder/AwsClientBuilder.java#L110).
* \*4: 参考: [EndpointConfiguration](https://github.com/aws/aws-sdk-java/blob/master/aws-java-sdk-core/src/main/java/com/amazonaws/client/builder/AwsClientBuilder.java#L559).
* \*5: 参考: [ClientConfiguration](https://github.com/aws/aws-sdk-java/blob/master/aws-java-sdk-core/src/main/java/com/amazonaws/ClientConfiguration.java).
* \*6: 参考: [AmazonS3Builder](https://github.com/aws/aws-sdk-java/blob/master/aws-java-sdk-s3/src/main/java/com/amazonaws/services/s3/AmazonS3Builder.java)
