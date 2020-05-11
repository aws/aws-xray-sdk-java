# Change Log

## 2.5.0 - 2020-05-11
- Added Spring interceptor without Spring data dependency
[PR 115](https://github.com/aws/aws-xray-sdk-java/pull/115)
- Handled null responses in sampling API requests
[PR 122](https://github.com/aws/aws-xray-sdk-java/pull/122)
- Fix NPE while handling LOG_ERROR in Spring module
[PR 125](https://github.com/aws/aws-xray-sdk-java/pull/125)
- Support trace ID injection during context propagation
[PR 127](https://github.com/aws/aws-xray-sdk-java/pull/127)
- Added IgnoreContextMissingStrategy
[PR 129](https://github.com/aws/aws-xray-sdk-java/pull/129)
- Fixed implementation of isWrapperFor and unwrap
[PR 131](https://github.com/aws/aws-xray-sdk-java/pull/131)
- Fixed subsegment streaming in Lambda
[PR 133](https://github.com/aws/aws-xray-sdk-java/pull/133)
- Added fully qualified & configurable trace ID injection
[PR 135](https://github.com/aws/aws-xray-sdk-java/pull/135)
- Fixed implementation of TracingStatement when no segment present
[PR 137](https://github.com/aws/aws-xray-sdk-java/pull/137)
- Improved Docker ID discovery in DockerUtils
[PR 141](https://github.com/aws/aws-xray-sdk-java/pull/141)
- Performance improvements to equals and hashcode usages
[PR 142](https://github.com/aws/aws-xray-sdk-java/pull/142)

## 2.4.0 - 2019-11-21
- Fix tags in pom.xml
[PR 83](https://github.com/aws/aws-xray-sdk-java/pull/87)
- Add default protected constructor for HTTPClientBuilder
[PR 90](https://github.com/aws/aws-xray-sdk-java/pull/90)
- Add subtree streaming for subsegments
[PR 91](https://github.com/aws/aws-xray-sdk-java/pull/91)
- Add a benchmarking package for timing
[PR 75](https://github.com/aws/aws-xray-sdk-java/pull/75)
- Capture throwables instead of just errors in XRayServletFilter
[PR 100](https://github.com/aws/aws-xray-sdk-java/pull/100)
- Change sonatype endpoint to dedicated AWS one
[PR 105](https://github.com/aws/aws-xray-sdk-java/pull/105)
- Add generic SQL recorder module
[PR 107](https://github.com/aws/aws-xray-sdk-java/pull/107)
- Add support for segment-level metrics and integration with logs
[PR 110](https://github.com/aws/aws-xray-sdk-java/pull/110)
  - Add log4j module
  - Add slf4j module
  - Add metrics module
- Upgrade Maven Javadoc plugin & disable javadoc linting
[PR 111](https://github.com/aws/aws-xray-sdk-java/pull/111)

## 2.3.0 - 2019-07-18
- Add SNS service to AWS SDK operation whitelist JSON
[PR 85](https://github.com/aws/aws-xray-sdk-java/pull/85)
- Allow specification of Daemon configuration in UDPEmitter
[PR 80](https://github.com/aws/aws-xray-sdk-java/pull/80)
- Add support for JDK 11
[PR 78](https://github.com/aws/aws-xray-sdk-java/pull/78)
- Make TraceInterceptor.entityKey public
[PR 73](https://github.com/aws/aws-xray-sdk-java/pull/73)

## 2.2.1 - 2019-02-07
- Fixed BOM package to include new artifacts introduced in 2.2.0.

## 2.2.0 - 2019-02-07
- Fixed a race condition with sampling reservoir.
[PR 47](https://github.com/aws/aws-xray-sdk-java/pull/47)
- Cleaned up some duplicated code around handling context missing strategy.
[PR 50](https://github.com/aws/aws-xray-sdk-java/pull/50)
- Fixed a race condition with sampling reservoir that could lead to `IndexOutOfBoundsException`.
[PR 52](https://github.com/aws/aws-xray-sdk-java/pull/52)
- Fixed a race condition where the `AWSXRayServletFilter` would eagerly retrieve the global recorder on instantiation, causing custom recorders to sometimes be ignored.
[PR 53](https://github.com/aws/aws-xray-sdk-java/pull/53)
- Added support for instrumenting the AWS SDK for Java V2.
[PR 56](https://github.com/aws/aws-xray-sdk-java/pull/56)
- Fixed an issue where segments were not being cleaned up when servlets were processed asynchronously.
[PR 57](https://github.com/aws/aws-xray-sdk-java/pull/57)
- Fixed an issue where extra recorders were created when using `CentralizedSamplingStrategy` without a plugin setting an origin value.
[PR 59](https://github.com/aws/aws-xray-sdk-java/pull/59)

## 2.1.0 - 2018-11-20
- Fixed a race condition on sampling pollers start.
- The sampling pollers now also log `Error` in addition to `Exception`.
- Added a public API to `CentralizedSamplingStrategy` to shutdown pollers for clean exit.
- Fixed a race condition between `TracingHandler` and `AWSXRayRecorder` which could cause an NPE on AWS calls capture. [ISSUE29](https://github.com/aws/aws-xray-sdk-java/issues/29)
- Fixed a NPE bug in spring X-Ray interceptor when context is missing. [ISSUE41](https://github.com/aws/aws-xray-sdk-java/issues/41)
- Fixed a NPE bug in `DaemonConfig` when system property is used to set up the daemon address. [ISSUE40](https://github.com/aws/aws-xray-sdk-java/issues/40)
- Removed an unused dependency. [ISSUE39](https://github.com/aws/aws-xray-sdk-java/issues/39)
- Removed unnecessary credentials retrieval for AWS client used by sampling pollers. [PR34](https://github.com/aws/aws-xray-sdk-java/pull/34)
- Fixed the UDP address setter not work correctly on `DaemonConfig`.
- Catching an NPE when the SDK property file is not reachable on unit tests.

## 2.0.1 - 2018-09-06
### Changed
- Fixed a bug that caused XRay sampling rules fetching to fail when AWS SDK instrumentor is included. [ISSUE25](https://github.com/aws/aws-xray-sdk-java/issues/25)

## 2.0.0 - 2018-08-28
### Backwards incompatible change
- Default Sampling Strategy has been updated to Centralized Sampling Strategy which gets sampling rules from X-Ray backend instead of from a static JSON file. [More Information](https://docs.aws.amazon.com/xray/latest/devguide/xray-console-sampling.html)
- Implement two new class `RulePoller` and `TargetPoller` to periodically updating sampling rules and sampling targets through TCP connection.
- Fallback to Localized Sampling Strategy when centralized sampling rules are not available.
- In order to disable Centralized Sampling Strategy, provide `withSamplingStrategy` with Localized one.
- Update `DefaultSamplingRules.json` file. i.e. `service_name` has been replaced to `host` and `version` changed to `2`. SDK still supports `v1` JSON file. If `v1` version JSON has been provided, `shouldTrace` will treat `service_name` value as `host` value.
- Update `shouldTrace` method to take only one parameter defined in `samplingRequest`.
- Add a new class called `DaemonConfiguration` and both classes `UDPEmitter` and `XRayClient` are depending on it. `setDaemonAddress` method has been moved to class `DaemonConfiguration`.
### Added
- Environment variable `AWS_TRACING_DAEMON_ADDRESS` now takes a value of the form '127.0.0.1:2000' or 'tcp:127.0.0.1:2000 udp:127.0.0.2:2001'. The former one means UDP and TCP are running at the same address and the later one specify individual addresses for TCP and UDP connection. By default it assumes a X-Ray daemon running at 127.0.0.1:2000 listening to both UDP and TCP traffic.
- Update `DefaultOperationParameterWhitelist.json` with S3 support. [PR9](https://github.com/aws/aws-xray-sdk-java/pull/9)
- Update `README` with correct `defaultRecorder` method. [PR10](https://github.com/aws/aws-xray-sdk-java/pull/10)
- Link scorekeep sample application in `README`. [PR12](https://github.com/aws/aws-xray-sdk-java/pull/12)
- Add missing open sourcing standard files. [PR14](https://github.com/aws/aws-xray-sdk-java/pull/14)
- Add travis CI. [PR16](https://github.com/aws/aws-xray-sdk-java/pull/16)
- Update `getUrl` method do not concatenate if the url in request is already absolute. [PR20](https://github.com/aws/aws-xray-sdk-java/pull/20)

## 1.3.1 - 2018-01-12
### Changed
- Fixed a bug in `AbstractXRayInterceptor` so that `generateMetadata` can be overriden. [PR6](https://github.com/aws/aws-xray-sdk-java/pull/6)

## 1.3.0 - 2018-01-08
### Added
- Support for Spring Framework which enables the usage of aspects to trace requests down a call stack. Classes can either implement an interface or be annotated to identify themselves as available to the aspect for tracing. [PR1](https://github.com/aws/aws-xray-sdk-java/pull/1)

## 1.2.2 - 2017-12-05
### Changed
- Fixed a bug which caused certain non-sampled segments to be emitted to the X-Ray daemon. This issue occurred only when the segment began as sampled and was manually overridden to non-sampled using `setSampled(false)`.

## 1.2.1 - 2017-11-20
### Changed
- Fixed a bug in the exception serialization logic that occurred when an added exception had one or more 'cause' exceptions.

## 1.2.0 - 2017-08-11
### Added
- Additional methods added to the `SegmentContext` interface. The `SegmentContext` interface now supports overriding logic for beginning and ending a `Segment`. This allows customers to use an alternative to `ThreadLocal` values to pass `SegmentContext` throughout program execution.
### Changed
- Various string fixes.
- Exposed the exceptionless version of the `close()` method on the more generic `Entity` type. Generic types may now be used in try-with-resources blocks without requiring a `catch` block.
- Ignoring subsegment generation on S3 presign request API call.

## 1.1.2 - 2017-06-29
### Added
- Attempting to modify or re-emit a `Segment` now throws an unchecked `AlreadyEmittedException`.
### Changed
- Fixed a concurrent modification exception which occured when many exceptions were being added to a single subsegment at once.
- Various string fixes.

## 1.1.1 - 2017-05-17
### Added
- Configuration values set using the environment variables `AWS_XRAY_TRACING_NAME`, `AWS_XRAY_CONTEXT_MISSING`, and `AWS_XRAY_DAEMON_ADDRESS` can now also be set using Java system properties.
  - The corresponding property keys are `-Dcom.amazonaws.xray.strategy.tracingName`, `-Dcom.amazonaws.xray.strategy.contextMissingStrategy`, and `-Dcom.amazonaws.xray.emitters.daemonAddress` respectively.
  - Configuration values set using environment variables take precedence over those set using Java system properties, in turn taking precedence over any configuration values set in code.
### Changed
- Modification or re-emittance of segments or subsegments after they have already been emitted to the X-Ray daemon now results in an AlreadyModifiedException / log message (depending on the `ContextMissingStrategy` in use).

## 1.1.0 - 2017-04-19
### Added
- Support for tracing within AWS Lambda functions
- The provided AWSXRayServletFilter now supports asynchronous `HttpServletRequest`s

### Changed
- Subsegments representing calls to S3 that result in status codes 304 or 412 will no longer be considered as `fault`s.
- Information about the runtime environment (JVM name and version) is now added to segments under the `service` namespace.

## 1.0.6-beta - 2017-03-30
### Added
- Added an additional constructor to `DefaultThrowableSerializationStrategy` that allows overriding of the `Throwable` superclass types which are considered to be remote.
- Added more runtime information to the `aws.xray` namespace on segments.
- Added a `ContextMissingStrategy` member to the `AWSXRayRecorder` class. This allows configuration of the exception behavior exhibited when trace context is not properly propagated. The behavior can be configured in code. Alternatively, the environment variable `AWS_XRAY_CONTEXT_MISSING` can be used (overrides any modes set in code). Valid values for this environment variable are currently (case insensitive) `RUNTIME_ERROR` and `LOG_ERROR`. The default behavior remains, `DefaultContextMissingStrategy` extends `RuntimeErrorContextMissingStrategy`; i.e. by default, an exception will be thrown on missing context.
### Changed
- **BREAKING** Changed the `currentEntityId` and `currentTraceId` methods from static to instance-level methods, in order to have them support the configurable `contextMissingBehavior`.
### Removed
- **BREAKING** Removed support for the `XRAY_TRACING_NAME` environment variable.
- **BREAKING** Removed the use of `InheritableThreadLocal` values to store trace context, in favor of regular `ThreadLocal` values. This change was made to improve the safety of the way the SDK interacts with thread pools and other collections of long-lived threads.
- Removed behavior in which calls to `AWSXRayRecorder.getThreadLocal()` would throw a `SegmentNotFoundException` when the thread local value contained null. The method now returns null instead of throwing an exception.

## 1.0.5-beta - 2017-03-06
### Added
- Added the `getCurrentSegmentOptional` and `getCurrentSubsegmentOptional` methods to the `AWSXRay` and `AWSXRayRecorder` classes.
- Added pertinent parameters to subsegments wrapping AWS Lambda Invoke and InvokeAsync operations.
- Added the `beginDummySegment()` method to the `AWSXRay` class.

### Changed
- Changed the `sdk` key to `sdk_version` in the `aws.xray` segment property.
- Changed the `getCurrentSegment` method in the `AWSXRayRecorder` class to throw a `SegmentNotFoundException` if no segment is found.
- Changed the behavior of the `LocalizedSamplingStrategy` class to require that all loaded sampling rule JSON files include default `fixed_target` and `rate` values under the `default` namespace. The file must now be versioned, this release supports `"version": 1` of the sampling rules document.
  - An example is provided in `aws-xray-recorder-sdk-core/src/main/resources/com/amazonaws/xray/strategy/sampling/DefaultSamplingRules.json`.
- Changed the behavior of the `aws-xray-recorder-sdk-apache-http` submodule to flag its generated subsegments with error/throttle/fault values based on response codes received from downstream HTTP services.

### Removed
- Removed the `attribute_names_substituted` key that was previously added to subsegments wrapping some DynamoDB operations.
- Removed the single `URL` parameter constructor from the `DefaultSamplingStrategy` class. To pass a custom sampling rules file, use the `LocalizedSamplingStrategy`.

## 1.0.4-beta - 2017-02-13
### Changed
- Fixed a bug where the continuous injection of thread-local values to other threads caused a memory leak. This change removes the `TraceReference` class.
  - `AWSXRayRecorder.infectThreadLocal` is renamed to `AWSXRayRecorder.injectThreadLocal`.
  - `AWSXRayRecorder.getThreadLocal()` now returns an `Entity` rather than a `TraceReference`.
  - `AWSXRayRecorder.injectThreadLocal(Entity entity)` now accepts an `Entity` rather than a `TraceReference`.

## 1.0.3-beta - 2017-01-24
- No change

## 1.0.2-beta - 2017-01-24
### Added
- Added the `SegmentNamingStrategy` interface. Instantation of the `AWSXRayServletFilter` now requires an instance of `SegmentNamingStrategy`. A shorthand constructor which accepts a single `String` is also provided to simplify use of the `FixedSegmentNamingStrategy`.
- Added the `FixedSegmentNamingStrategy` and `DynamicSegmentNamingStrategy` strategies.

### Changed
- Fixed a bug in the behavior of the custom pattern generation used in sampling rules and the `DynamicNamingStrategy`.
- Changed the segment key under which the SDK version is recorded to "xray".
- Fixed a bug whereby segments with more than 100 subsegments were not properly being streamed to the service.
- Changed the behavior of the `setError` method in the `EntityImpl` class to no longer also modify the `fault` value.
- Changed the environment variable key to modify the target daemon address / port to `AWS_XRAY_DAEMON_ADDRESS`. The value is expected to be of the form `ip:port`.
- Changed the environment variable key to override the segment name from `XRAY_TRACING_NAME` to `AWS_XRAY_TRACING_NAME`. (`XRAY_TRACING_NAME` will still be supported until the non-beta release).
- Moved the `precursor_ids` property to be a member of only the `Subsegment` type.

## 1.0.1-beta - 2016-12-15
### Added
- Added the SDK version to generated segment documents.
- Added functionality to the `DummySegment` class in order to pass the Trace ID to downstream services in all cases, including those in which the current request is not sampled.

### Changed
- Fixed a bug in AWSXRayServletFilter that prevented the creation of subsegments for some services running behind ALBs.
- Updated the dependency on `com.amazonaws:aws-java-sdk-core` from version 1.11.60 to 1.11.67.
- Modified the signatures of the `close()` methods for `SegmentImpl` and `SubsegmentImpl` to not throw any exceptions.

### Removed
- Removed redundant HTTP status code parsing logic from the `apache-http` submodule. The AWS X-Ray service handles setting the fault, error, and throttle flags based on subsegments' provided HTTP status code.
