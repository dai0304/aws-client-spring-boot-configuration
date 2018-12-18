# AWS SDK for Java v2 configuration properties

| property                           | type    | typical default *1
| ---------------------------------- | ------- | ----
| `aws2.*.region` \*2                         | string  | (auto)
| `aws2.*.endpoint` \*3                       | string  | (auto)
| `aws2.*.credentials-provider-bean-name` \*4 | string  | `null`
| `aws2.*.client-override-configuration-bean-name`  \*5 | string  | `null`
| `aws2.*.http-client-bean-name` \*6          | string  | `null`
| `aws2.*.http-client-builder-bean-name` \*6  | string  | `null`
| `aws2.*.apache-http-client-builder.socket-timeout` \*6 \*8           | string (duration) | `30s`
| `aws2.*.apache-http-client-builder.connection-timeout` \*6 \*8       | string (duration) | `2s`
| `aws2.*.apache-http-client-builder.connection-acquisition-timeout` \*6 \*8 | string(duration) | `10s`
| `aws2.*.apache-http-client-builder.max-connections` \*6 \*8          | number  | `50`
| `aws2.*.apache-http-client-builder.proxy-configuration.endpoint` \*6 \*7 \*8 | string  | `null`
| `aws2.*.apache-http-client-builder.proxy-configuration.username` \*6 \*7 \*8 | string  | `null` 
| `aws2.*.apache-http-client-builder.proxy-configuration.password` \*6 \*7 \*8 | string  | `null` 
| `aws2.*.apache-http-client-builder.proxy-configuration.ntlm-domain` \*6 \*7 \*8 | string  | `null` 
| `aws2.*.apache-http-client-builder.proxy-configuration.ntlm-workstation` \*6 \*7 \*8 | string  | `null` 
| `aws2.*.apache-http-client-builder.proxy-configuration.non-proxy-hosts` \*6 \*7 \*8 | string  | `null` 
| `aws2.*.apache-http-client-builder.proxy-configuration.preemptive-basic-authentication-enabled` \*6 \*7 \*8 | boolean | `false`
| `aws2.*.apache-http-client-builder.proxy-configuration.use-system-property-values` \*6 \*7 \*8 | boolean | `true`
| `aws2.*.apache-http-client-builder.local-address` \*6 \*8            | string  | `null` 
| `aws2.*.apache-http-client-builder.expect-continue-enabled` \*6 \*8  | boolean  | `true`
| `aws2.*.apache-http-client-builder.connection-time-to-live` \*6 \*8  | string (duration) | `-1ms`
| `aws2.*.apache-http-client-builder.connection-max-idle-time` \*6 \*8 | string (duration) | `60s`
| `aws2.*.apache-http-client-builder.use-idle-connection-reaper` \*6 \*8 | boolean | `false`
| `aws2.*.netty-nio-async-http-client-builder.max-concurrency` \*6 \*9 | number  | `50`
| `aws2.*.netty-nio-async-http-client-builder.max-pending-connection-acquires` \*6 \*9 | number  | `10000`
| `aws2.*.netty-nio-async-http-client-builder.read-timeout` \*6 \*9 | string (duration) | `30s`
| `aws2.*.netty-nio-async-http-client-builder.write-timeout` \*6 \*9 | string (duration)  | `30s`
| `aws2.*.netty-nio-async-http-client-builder.connection-timeout` \*6 \*9 | string (duration)  | `2s`
| `aws2.*.netty-nio-async-http-client-builder.connection-acquisition-timeout` \*6 \*9 | string (duration)  | `10s`
| `aws2.*.netty-nio-async-http-client-builder.event-loop-group-bean-name` \*6 \*9 | string  | `null`
| `aws2.*.netty-nio-async-http-client-builder.event-loop-group-builder-bean-name` \*6 \*9 | string  | `null`
| `aws2.*.netty-nio-async-http-client-builder.protocol` \*6 \*9 | `HTTP|HTTPS`  | `HTTPS`
| `aws2.*.netty-nio-async-http-client-builder.max-http2-streams` \*6 \*9 | number  | `Integer.MAX_VALUE`
| `aws2.s3.path-style-access-enabled` \*10   | boolean | `false`
| `aws2.s3.accelerate-mode-enabled` \*10     | boolean | `false`
| `aws2.s3.dualstack-enabled` \*10           | boolean | `false`
| `aws2.s3.checksum-validation-enabled` \*10 | boolean | `true`
| `aws2.s3.chunked-encoding-enabled` \*10    | boolean | `true`

* \*1: The default value may be different for each version or service.
* \*2: See [AwsClientBuilder#region](https://github.com/aws/aws-sdk-java-v2/blob/master/core/aws-core/src/main/java/software/amazon/awssdk/awscore/client/builder/AwsClientBuilder.java#L66)
* \*3: See [SdkClientBuilder#endpointOverride](https://github.com/aws/aws-sdk-java-v2/blob/master/core/sdk-core/src/main/java/software/amazon/awssdk/core/client/builder/SdkClientBuilder.java#L52)
* \*4: See [AwsClientBuilder#credentialsProvider](https://github.com/aws/aws-sdk-java-v2/blob/master/core/aws-core/src/main/java/software/amazon/awssdk/awscore/client/builder/AwsClientBuilder.java#L52)
* \*5: See [SdkClientBuilder#overrideConfiguration](https://github.com/aws/aws-sdk-java-v2/blob/master/core/sdk-core/src/main/java/software/amazon/awssdk/core/client/builder/SdkClientBuilder.java#L38)
* \*6: `http-client(-builder)-bean-name` is prior to `*-http-client-builder.*`
* \*7: See [ProxyConfiguration](https://github.com/aws/aws-sdk-java-v2/blob/master/http-clients/apache-client/src/main/java/software/amazon/awssdk/http/apache/ProxyConfiguration.java)
* \*8: See [ApacheHttpClient.Builder](https://github.com/aws/aws-sdk-java-v2/blob/master/http-clients/apache-client/src/main/java/software/amazon/awssdk/http/apache/ApacheHttpClient.java#L272)
* \*9: See [NettyNioAsyncHttpClient.Builder](https://github.com/aws/aws-sdk-java-v2/blob/master/http-clients/netty-nio-client/src/main/java/software/amazon/awssdk/http/nio/netty/NettyNioAsyncHttpClient.java#L182)
* \*10: See [S3Configuration](https://github.com/aws/aws-sdk-java-v2/blob/master/services/s3/src/main/java/software/amazon/awssdk/services/s3/S3Configuration.java)
