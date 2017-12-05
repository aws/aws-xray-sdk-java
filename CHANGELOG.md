# Change Log

## 1.2.2 - 2017-12-05
### Changed
- Fixed a bug a bug which caused certain non-sampled segments to be emitted to the X-Ray daemon. This issue occurred only when the segment began as sampled and was manually overridden to non-sampled using `setSampled(false)`.

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
