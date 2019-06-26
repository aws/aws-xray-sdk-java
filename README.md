[![Build Status](https://travis-ci.org/aws/aws-xray-sdk-java.svg?branch=master)](https://travis-ci.org/aws/aws-xray-sdk-java)

# AWS X-Ray SDK for Java

![Screenshot of the AWS X-Ray console](/images/example_servicemap.png?raw=true)

## Installing

The AWS X-Ray SDK for Java is compatible with Java 8.

Add the AWS X-Ray SDK dependencies to your pom.xml:

```
<dependency>
  <groupId>com.amazonaws</groupId>
  <artifactId>aws-xray-recorder-sdk-core</artifactId>
  <version>2.2.1</version>
</dependency>
<dependency>
  <groupId>com.amazonaws</groupId>
  <artifactId>aws-xray-recorder-sdk-apache-http</artifactId>
  <version>2.2.1</version>
</dependency>
<dependency>
  <groupId>com.amazonaws</groupId>
  <artifactId>aws-xray-recorder-sdk-aws-sdk</artifactId>
  <version>2.2.1</version>
</dependency>
<dependency>
  <groupId>com.amazonaws</groupId>
  <artifactId>aws-xray-recorder-sdk-aws-sdk-v2</artifactId>
  <version>2.2.1</version>
</dependency>
<dependency>
  <groupId>com.amazonaws</groupId>
  <artifactId>aws-xray-recorder-sdk-aws-sdk-instrumentor</artifactId>
  <version>2.2.1</version>
</dependency>
<dependency>
  <groupId>com.amazonaws</groupId>
  <artifactId>aws-xray-recorder-sdk-aws-sdk-v2-instrumentor</artifactId>
  <version>2.2.1</version>
</dependency>
<dependency>
  <groupId>com.amazonaws</groupId>
  <artifactId>aws-xray-recorder-sdk-sql-mysql</artifactId>
  <version>2.2.1</version>
</dependency>
<dependency>
  <groupId>com.amazonaws</groupId>
  <artifactId>aws-xray-recorder-sdk-sql-postgresql</artifactId>
  <version>2.2.1</version>
</dependency>
<dependency>
  <groupId>com.amazonaws</groupId>
  <artifactId>aws-xray-recorder-sdk-spring</artifactId>
  <version>2.2.1</version>
</dependency>
```

## Getting Help

Please use these community resources for getting help. We use the GitHub issues for tracking bugs and feature requests.

* Ask a question in the [AWS X-Ray Forum](https://forums.aws.amazon.com/forum.jspa?forumID=241&start=0).
* Open a support ticket with [AWS Support](http://docs.aws.amazon.com/awssupport/latest/user/getting-started.html).
* If you think you may have found a bug, please open an [issue](https://github.com/aws/aws-xray-sdk-java/issues/new).

## Opening Issues

If you encounter a bug with the AWS X-Ray SDK for Java we would like to hear about it. Search the [existing issues](https://github.com/aws/aws-xray-sdk-java/issues) and see if others are also experiencing the issue before opening a new issue. Please include the version of AWS X-Ray SDK for Java, AWS SDK for Java, JDK, and OS you’re using. Please also include repro case when appropriate.

The GitHub issues are intended for bug reports and feature requests. For help and questions with using AWS X-Ray SDK for Java please make use of the resources listed in the [Getting Help](https://github.com/aws/aws-xray-sdk-java#getting-help) section. Keeping the list of open issues lean will help us respond in a timely manner.

## Documentation

The [developer guide](https://docs.aws.amazon.com/xray/latest/devguide/xray-sdk-java.html) provides in-depth guidance on using the AWS X-Ray service and the AWS X-Ray SDK for Java.

See [awslabs/eb-java-scorekeep](https://github.com/awslabs/eb-java-scorekeep/tree/xray) for a sample application that uses this SDK.

## Quick Start

### Intercept incoming HTTP requests

For many applications, work on a task begins with an incoming HTTP request.

There are a few different options for intercepting this incoming HTTP request.

##### Applications using `javax.servlet` may utilize the `AWSXRayServletFilter`
Add the filter in Tomcat's `web.xml`:
```
  <filter>
     <filter-name>AWSXRayServletFilter</filter-name>
     <filter-class>com.amazonaws.xray.javax.servlet.AWSXRayServletFilter</filter-class>
     <init-param>
        <param-name>fixedName</param-name>
        <param-value>defaultSegmentName</param-value>
     </init-param>
  </filter>
  <filter-mapping>
     <filter-name>AWSXRayServletFilter</filter-name>
     <url-pattern>*</url-pattern>
  </filter-mapping>
```
Alternatively, Spring users may add the `AWSXRayServletFilter` to their `WebConfig`:
```
@Configuration
public class WebConfig {

    ...

    @Bean
    public Filter TracingFilter() {
        return new AWSXRayServletFilter(new FixedSegmentNamingStrategy("defaultSegmentName"));
    }
}
```
The servlet filter will fail to serve incoming requests if a `SegmentNamingStrategy` is not supplied, either through web.xml init-params or through the constructor.

##### Applications not using `javax.servlet` may include custom interceptors to begin and end trace segments

Directly call `beginSegment` and `endSegment` as necessary. *Note:* this method requires additional work to ensure that the `X-Amzn-Trace-Id` header is properly propogated and sufficient information about the request and response is captured with the segment.

### Intercept AWS requests

Applications may make calls to Amazon Web Services. Included in the X-Ray SDK is an extension of the AWS SDK's `RequestHandler2`.

To instrument an example instance of `AmazonWebServiceClient`:

```
AmazonDynamoDBClient tracedDynamoClient = 
    new AmazonDynamoDBClient().standard().withRequestHandlers(new TracingHandler()).withRegion(Regions.US_EAST_1).build();
```

### Intercept outgoing HTTP requests

Applications may make downstream HTTP calls to communicate with other applications. If these downstream applications are also traced, trace context information will need to be passed so that the trace segments may be properly grouped into a single trace. 

The following options are available for ensuring these downstream calls include trace context information as well as locally generate the appropriate trace subsegments.

##### Applications using Apache's `HttpClient` library may utilize proxy classes included in `com.amazonaws.xray.proxies.apache.http`

Change the import line for your `DefaultHttpClient` or `HttpClientBuilder` to the appropriate proxy import. Continue to use the class as normal; method signatures do not change.

```
// Change the import
import com.amazonaws.xray.proxies.apache.http.DefaultHttpClient;
...
// Keep the invocation
HttpClient httpClient = new DefaultHttpClient();
httpClient.execute(request);
```

### Intercept custom methods

It may be useful to further decorate portions of an application for which performance is critical. Generating subsegments around these hot spots will help in understanding their impact on application performance. There are a few different styles available for tracing custom methods.

##### Using traced closures

```
import com.amazonaws.xray.AWSXRayRecorder;
import com.amazonaws.xray.AWSXRayRecorderBuilder;
...
AWSXRayRecorder xrayRecorder = AWSXRayRecorderBuilder.defaultRecorder();
...
xrayRecorder.createSubsegment("getMovies" (subsegment) -> {
    doSomething();
});
```
##### Using explicit calls to begin and end subsegments.
```
Subsegment subsegment = xrayRecorder.beginSubsegment("providedMovie");
try {
    doSomething();
    throw new RuntimeException("user error");
} catch (RuntimeException e) {
    subsegment.addException(e);
    subsegment.setError(true);
} finally {
    xrayRecorder.endSubsegment();
}
```
Note that in the closure-based example above, exceptions are intercepted automatically.

## Building From Source

Once you check out the code from GitHub, you can build it using Maven. To disable the GPG-signing in the build, use:

```
mvn clean install -Dgpg.skip=true
```

## License

The AWS X-Ray SDK for Java is licensed under the Apache 2.0 License. See LICENSE and NOTICE.txt for more information.
