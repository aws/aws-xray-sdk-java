# Benchmarking
Benchmarking the Java SDK is necessary to help isolate performance issues and bottlenecks. One of the key factors when on-boarding with X-Ray is knowing the performance impact on an existing application. In this readme, we will demonstrate how to use the benchmarking features in our SDK to get a feel for the performance impact that will be made on a target application. We will also be comparing the performances of the same tests run under different versions of the SDK. We use the micro-benchmarking framework JMH to help us benchmark the SDK.

## Running the Benchmark
To run the benchmark, make sure you have Maven and Java 8 or above installed. With Maven, it will download the necessary X-Ray dependencies that the benchmark uses as well as the JMH core and annotation processors that this benchmark uses.

To run the benchmark, use your favorite shell to go into the directory. Then run the following commands:
```BASH
cd aws-xray-sdk-java/aws-xray-recorder-sdk-benchmark/
mvn clean && mvn package
java -jar target/benchmark.jar
```

You should then start seeing the output of the benchmark:
```
# VM invoker: /Library/Java/JavaVirtualMachines/jdk1.8.0_202.jdk/Contents/Home/jre/bin/java
# VM options: <none>
# Warmup: 20 iterations, 1 s each
# Measurement: 20 iterations, 1 s each
# Timeout: 10 min per iteration
# Threads: 1 thread, will synchronize iterations
# Benchmark mode: Throughput, ops/time
# Benchmark: com.amazonaws.xray.AWSXRayRecorderBenchmark.beginDummySegmentBenchmark

# Run progress: 0.00% complete, ETA 01:06:01
# Fork: 1 of 1
# Warmup Iteration   1: ≈ 10⁻³ ops/ns
# Warmup Iteration   2: ≈ 10⁻³ ops/ns
# Warmup Iteration   3: ≈ 10⁻³ ops/ns
# Warmup Iteration   4: ≈ 10⁻³ ops/ns
# Warmup Iteration   5: ≈ 10⁻³ ops/ns
# Warmup Iteration   6: ≈ 10⁻³ ops/ns
# Warmup Iteration   7: ≈ 10⁻³ ops/ns
# Warmup Iteration   8: 0.001 ops/ns
# Warmup Iteration   9: 0.001 ops/ns
# Warmup Iteration  10: 0.001 ops/ns
...
```

If you wish to run a specific benchmark, please run the following command:
```
java -jar target/benchmarks.jar <Benchmark_name_here>

# Example: java -jar target/benchmarks.jar com.amazonaws.xray.AWSXRayRecorderBenchmark.beginDummySegmentBenchmark
```

## Benchmark Results
These benchmarks were ran on a **m5.xlarge machine, with 16 GB of memory and 4 vCPUs**. For each benchmark, we first did a dry run of 20 iterations, spending 1 second on each. Afterwards, we then measured the amount of time it took for an additional 20 iterations for 1 second each. Each benchmark was ran under one thread in its own fork. 

In summary, we found that the execution time for X-Ray in a typical instrumented request-response lifecycle is approximately **15 microseconds**. The breakdown of this number will be explained below.

### Benchmark Analysis
The benchmark numbers individually do not really give us a good sense of how the SDK as a whole performs. We wrote these benchmarks with the goal of getting a feel for how a specific X-Ray SDK method performs when individually measured. It is only valuable when we combine these tests in a way where we can mimic the overall behavior of X-Ray in a typical environment. These benchmarks are broken down into various components that make up individual behaviors in an X-Ray instrumented environment. This will be a somewhat technical explanation of the benchmarking results; if you would like to see the results and how it would impact your service, please scroll down to the [Extrapolating the Results paragraph](#extrapolating-the-results-to-a-typical-instrumented-request).

We have broken down the benchmarks into four main components:

* Recorder Benchmarks
* Entity Benchmarks
* Entity Serializer Benchmarks
* Sampling Benchmarks

There are four types of modes that are benchmarked:

* Average Throughput - The amount of nanoseconds it took on average to run the benchmark.
* Sample - A random benchmark result is chosen from the set.
* Single Shot - The benchmark is ran once with no initialization period.
* Throughput - The amount of operations that were ran in a nanosecond.

In a typical X-Ray use case, these four major components in are pieced together in various combinations to perform the main functionality of generating entities, making sampling decisions, and serializing those entities.

#### Recorder
The recorder benchmarks form a class of benchmarks that are generally higher level than some of the other components. Some of these benchmarks utilize many of the other components. For example, the BeginEndSegmentBenchmarks touch base on the Entity benchmarks as well as the Entity Serializer benchmarks. The whole lifecycle creates the entity, which includes the tasks of constructing the entity object, generating a Trace ID, and then ends it, finishing over to its serialization stage. These benchmarks mainly revolve around begin/end segments and subsegments.

There are currently four kinds of benchmarks created:
* Begin Benchmarks
    * For a segment, this begins in a empty context and calls BeginSegment
    * For a subsegment, this begins in a context that already has a parent segment.
* End Benchmarks
    * For a segment, this ends in a context that has a segment already populated.
    * For a subsegment, this ends a subsegment in context that has a segment already populated with a child subsegment.
* Begin-End Segment Benchmarks
    * Begins and ends a segment in one go in an empty context.
* Begin-End Segment-Subsegment benchmarks
    * Begins a segment, begins a subsegment, ends that subsegment, and ends the segment.
    
#### Entity
The entity benchmarks encapsulate the lower-level data-model operations; these are the objects that represent the target application service. It includes the Segment, Subsegment, and their base class the Entity. It has benchmarks for entity construction and data storage.

#### Entity Serializer
These benchmarks test serialization for different variations of entities. The serialization process occurs when the segment/subsegments are closed and needs to be sent to the Daemon. We have written benchmarks that tested entities with different numbers of children as well as entities with different generations of subsegments.

For example, serializeFourGenerationSegment tests the serialization of a Segment which has four generations of subsegments. An example looks like this:

> Root Segment -> Subsegment -> Subsegment -> Subsegment -> Subsegment

_where each arrow represents a descendant._

The child benchmarks test for the serialization of a Segment who has a certain number of children; this means that there is only one generation.

The serializeFourChildSegment benchmark looks like this:

> Root Segment -> [Subsegment A, Subsegment B, Subsegment C, Subsegment D]

_where the arrow represents a descendant, and the brackets represent a single generation of siblings._

#### Sampling
The sampling benchmarks focus on the two current sampling decisions we support today: Centralized Sampling and Localized Sampling. We tested two different scenarios for both sampling strategies; one was with default sampling rules (1 reservoir and 5% sampling rate) and one with a sampling rule to not sample (0 reservoir and 0% sampling rate). A fake request was generated that resembled a typical HTTP Request to a service and this was used to determine the sampling decision. In either case, the sampling rule matched the described request. 

#### Results 
The results here are based on the data in [this table](#benchmark-data-by-sdk-version).

For the recorder, the most expensive operation is the EndSegment() operation. This makes sense because it encapsulates really the two most expensive operations we have in the SDK: entity serialization and network data transmission. Serialization alone of a single segment with no children took 3055.384 nanoseconds; this is of the total 7480.519 nanoseconds it took to end that same segment. My bold guess is that the other half was the time it took for UDP data transmission. We should definitely revisit this benchmark to confirm this.

Let’s take a look at the scalability of entity serialization since it takes up such a big portion of the performance. We’ll look at a segment with n children as well as a segment with n generations.

For a segment that has one child, it took **5115.504 nanoseconds** to serialize.

Two Children, it took **6798.185**

Three Children: **8688.281**

Four Children: **10602.465**


This looks like it’s pretty linear. Comparing that with vertical growth:

Two generations (Segment -> Child -> Child): **6628.881**

Three Generations: **8471.203**

Four Generations: **10060.976**


Both have approximately the same the performance complexity. We can roughly say that each additional subsegment added to a segment will take 1.5 microseconds longer to serialize (Keep in mind this is only for serialization; there could be other factors that increase the overall performance of the EndSegment method).

In version 2.0.x, we experienced a major performance bottleneck with the [recorder re-initialization issue](https://github.com/aws/aws-xray-sdk-java/issues/46) that has been fixed. Looking at the data, Centralized Sampling took **823736.245 ± 324446.993 ns**, which is approximately **1.1** ms in the worst case. Luckily by version 2.2.x, we reduced it down to approximately 100-200 nanoseconds. Every other benchmark result when comparing versions 1.3.x, 2.0.x, and 2.2.x look similar.

No other benchmarking results stand out. They are all either take largely less time to execute or implicitly perform serialization.

#### Extrapolating the Results to a Typical Instrumented Request
With the data out, we will take this time to use the data to extrapolate it into a typical use case.

The typical lifecycle within an instrumented application generally looks like this:

* Make Sampling Decision
* Begin Segment (This is usually when a request is handled in the service)
    * Put Annotation in Segment
    * Put Metadata in Segment
    * Begin Subsegment (n times) (This is usually when the service makes any downstream calls)
        * Put Annotation in Subsegment
        * Put Metadata in Subsegment
    * End Subsegment (n times)
* End Segment

Let's say we have the following instrumented RESTful login service that uses centralized sampling with default rules; assume everything happens in sequential order:

* Customer logins
    * We authenticate the customer credentials with DynamoDB.
    * We modify the last login time of the DynamoDB item for the customer.
    * We load an HTML file from S3.
* Return Content of the HTML file to the customer

In this scenario the following would happen:

* Make a Sampling Decision, Begin Segment
    * Begin Subsegment -> Populate Metadata, Http, or AWS Fields (lets say 5 fields) -> End Subsegment
    * Begin Subsegment -> Populate Metadata, Http, or AWS Fields (lets say 5 fields) -> End Subsegment
    * Begin Subsegment -> Populate Metadata, Http, or AWS Fields (lets say 5 fields) -> End Subsegment
* End Segment

In this example, we'll be using version 2.2.x's benchmarking results as a reference.

* CentralizedSamplingStrategyBenchmark.defaultSamplingRuleBenchmark, beginSegmentBenchmark
    * beginSubsegmentBenchmark -> putMetadataBenchmark * 5 -> endSubsegmentBenchmark
    * beginSubsegmentBenchmark -> putMetadataBenchmark * 5 -> endSubsegmentBenchmark
    * beginSubsegmentBenchmark -> putMetadataBenchmark * 5 -> endSubsegmentBenchmark
* endSegmentBenchmark

Using the average throughput table as a reference, we can see that the aggregated result is approximately:

* 113.674 + 1636.812
    * \+ 866.134 + (160.012 * 5) + 385.743
    * \+ 866.134 + (160.012 * 5) + 385.743
    * \+ 866.134 + (160.012 * 5) + 385.743
* \+ 7478.520

This sums to about **15384.817** nanoseconds (or about **15 microseconds**), representing the amount of time X-Ray spends performing X-Ray related functions for that example request. As you could see, the longest single X-Ray operation in this request was EndSegment, and it makes sense because it involves around getting the current segment stored in the context, serializing it and its child entities, and then sending the serialized data to a UDP socket.

In the latest version of our SDK (2.2.x), the typical request-response lifecycle time is in the order of magnitude of tens of microseconds. This execution time is small when compared to the overhead of network IO. This is a major improvement over version 2.0.x which had the mentioned performance issue. The SDK should now be stable and working as expected as evident with the benchmarking data.

### Benchmark Data by SDK Version
#### 1.3.x
<details><summary>Show</summary>
<p>

```
Benchmark                                                                            Mode     Cnt       Score        Error   Units
AWSXRayRecorderBenchmark.beginDummySegmentBenchmark                                 thrpt      20       0.001 ±      0.001  ops/ns
AWSXRayRecorderBenchmark.beginEndDummySegmentBenchmark                              thrpt      20       0.001 ±      0.001  ops/ns
AWSXRayRecorderBenchmark.beginEndDummySegmentSubsegmentBenchmark                    thrpt      20      ≈ 10⁻³               ops/ns
AWSXRayRecorderBenchmark.beginEndSegmentBenchmark                                   thrpt      20      ≈ 10⁻⁴               ops/ns
AWSXRayRecorderBenchmark.beginEndSegmentSubsegmentBenchmark                         thrpt      20      ≈ 10⁻⁴               ops/ns
AWSXRayRecorderBenchmark.beginSegmentBenchmark                                      thrpt      20       0.001 ±      0.001  ops/ns
AWSXRayRecorderBenchmark.beginSubsegmentBenchmark                                   thrpt      20       0.001 ±      0.001  ops/ns
AWSXRayRecorderBenchmark.beginSubsegmentDummyParentBenchmark                        thrpt      20       0.001 ±      0.001  ops/ns
AWSXRayRecorderBenchmark.endDummySegmentBenchmark                                   thrpt      20       0.002 ±      0.001  ops/ns
AWSXRayRecorderBenchmark.endSegmentBenchmark                                        thrpt      20      ≈ 10⁻⁴               ops/ns
AWSXRayRecorderBenchmark.endSegmentNoChildBenchmark                                 thrpt      20      ≈ 10⁻⁴               ops/ns
AWSXRayRecorderBenchmark.endSegmentWithChildBenchmark                               thrpt      20      ≈ 10⁻⁴               ops/ns
AWSXRayRecorderBenchmark.endSubsegmentBenchmark                                     thrpt      20       0.003 ±      0.001  ops/ns
AWSXRayRecorderBenchmark.endSubsegmentDummyParentBenchmark                          thrpt      20       0.003 ±      0.001  ops/ns
AWSXRayRecorderBenchmark.getSegmentBenchmark                                        thrpt      20       0.005 ±      0.001  ops/ns
AWSXRayRecorderBenchmark.getSubsegmentBenchmark                                     thrpt      20       0.006 ±      0.001  ops/ns
entities.EntityBenchmark.constructSegmentBenchmark                                  thrpt      20       0.001 ±      0.001  ops/ns
entities.EntityBenchmark.constructSubsegmentPutInSegmentBenchmark                   thrpt      20       0.002 ±      0.001  ops/ns
entities.EntityBenchmark.putAnnotationBenchmark                                     thrpt      20       0.013 ±      0.001  ops/ns
entities.EntityBenchmark.putExceptionSegmentBenchmark                               thrpt      20       0.001 ±      0.001  ops/ns
entities.EntityBenchmark.putMetadataBenchmark                                       thrpt      20       0.006 ±      0.001  ops/ns
entities.EntitySerializerBenchmark.serializeFourChildSegment                        thrpt      20      ≈ 10⁻⁴               ops/ns
entities.EntitySerializerBenchmark.serializeFourGenerationSegment                   thrpt      20      ≈ 10⁻⁴               ops/ns
entities.EntitySerializerBenchmark.serializeOneChildSegment                         thrpt      20      ≈ 10⁻⁴               ops/ns
entities.EntitySerializerBenchmark.serializeThreeChildSegment                       thrpt      20      ≈ 10⁻⁴               ops/ns
entities.EntitySerializerBenchmark.serializeThreeGenerationSegment                  thrpt      20      ≈ 10⁻⁴               ops/ns
entities.EntitySerializerBenchmark.serializeTwoChildSegment                         thrpt      20      ≈ 10⁻⁴               ops/ns
entities.EntitySerializerBenchmark.serializeTwoGenerationSegment                    thrpt      20      ≈ 10⁻⁴               ops/ns
entities.EntitySerializerBenchmark.serializeZeroChildSegment                        thrpt      20      ≈ 10⁻³               ops/ns
strategy.sampling.LocalizedSamplingStrategyBenchmark.defaultSamplingRuleBenchmark   thrpt      20       0.014 ±      0.001  ops/ns
strategy.sampling.LocalizedSamplingStrategyBenchmark.noSampleSamplingBenchmark      thrpt      20       0.010 ±      0.001  ops/ns
AWSXRayRecorderBenchmark.beginDummySegmentBenchmark                                  avgt      20     930.006 ±      1.430   ns/op
AWSXRayRecorderBenchmark.beginEndDummySegmentBenchmark                               avgt      20    1338.062 ±     36.102   ns/op
AWSXRayRecorderBenchmark.beginEndDummySegmentSubsegmentBenchmark                     avgt      20    2494.100 ±      1.864   ns/op
AWSXRayRecorderBenchmark.beginEndSegmentBenchmark                                    avgt      20    9020.679 ±     25.179   ns/op
AWSXRayRecorderBenchmark.beginEndSegmentSubsegmentBenchmark                          avgt      20   12284.696 ±    505.733   ns/op
AWSXRayRecorderBenchmark.beginSegmentBenchmark                                       avgt      20    1649.248 ±      2.289   ns/op
AWSXRayRecorderBenchmark.beginSubsegmentBenchmark                                    avgt      20     866.134 ±      3.796   ns/op
AWSXRayRecorderBenchmark.beginSubsegmentDummyParentBenchmark                         avgt      20     928.351 ±     57.592   ns/op
AWSXRayRecorderBenchmark.endDummySegmentBenchmark                                    avgt      20     448.467 ±      5.861   ns/op
AWSXRayRecorderBenchmark.endSegmentBenchmark                                         avgt      20    7478.520 ±     26.655   ns/op
AWSXRayRecorderBenchmark.endSegmentNoChildBenchmark                                  avgt      20    7343.836 ±    230.851   ns/op
AWSXRayRecorderBenchmark.endSegmentWithChildBenchmark                                avgt      20    9202.114 ±    267.064   ns/op
AWSXRayRecorderBenchmark.endSubsegmentBenchmark                                      avgt      20     385.743 ±      2.821   ns/op
AWSXRayRecorderBenchmark.endSubsegmentDummyParentBenchmark                           avgt      20     336.228 ±      2.557   ns/op
AWSXRayRecorderBenchmark.getSegmentBenchmark                                         avgt      20     209.839 ±     10.374   ns/op
AWSXRayRecorderBenchmark.getSubsegmentBenchmark                                      avgt      20     173.015 ±      2.319   ns/op
entities.EntityBenchmark.constructSegmentBenchmark                                   avgt      20     978.648 ±      3.556   ns/op
entities.EntityBenchmark.constructSubsegmentPutInSegmentBenchmark                    avgt      20     613.445 ±    132.987   ns/op
entities.EntityBenchmark.putAnnotationBenchmark                                      avgt      20      84.141 ±      2.014   ns/op
entities.EntityBenchmark.putExceptionSegmentBenchmark                                avgt      20     800.592 ±      2.352   ns/op
entities.EntityBenchmark.putMetadataBenchmark                                        avgt      20     160.012 ±      1.629   ns/op
entities.EntitySerializerBenchmark.serializeFourChildSegment                         avgt      20   10454.440 ±     33.857   ns/op
entities.EntitySerializerBenchmark.serializeFourGenerationSegment                    avgt      20   10060.976 ±     45.191   ns/op
entities.EntitySerializerBenchmark.serializeOneChildSegment                          avgt      20    5155.852 ±    265.627   ns/op
entities.EntitySerializerBenchmark.serializeThreeChildSegment                        avgt      20    8791.764 ±    189.374   ns/op
entities.EntitySerializerBenchmark.serializeThreeGenerationSegment                   avgt      20    8497.261 ±     50.650   ns/op
entities.EntitySerializerBenchmark.serializeTwoChildSegment                          avgt      20    7174.814 ±    529.818   ns/op
entities.EntitySerializerBenchmark.serializeTwoGenerationSegment                     avgt      20    6585.686 ±     26.487   ns/op
entities.EntitySerializerBenchmark.serializeZeroChildSegment                         avgt      20    3034.740 ±     20.371   ns/op
strategy.sampling.LocalizedSamplingStrategyBenchmark.defaultSamplingRuleBenchmark    avgt      20      71.837 ±      3.236   ns/op
strategy.sampling.LocalizedSamplingStrategyBenchmark.noSampleSamplingBenchmark       avgt      20      98.047 ±      0.179   ns/op
AWSXRayRecorderBenchmark.beginDummySegmentBenchmark                                sample  264885    1007.384 ±     37.568   ns/op
AWSXRayRecorderBenchmark.beginEndDummySegmentBenchmark                             sample  204104    1428.892 ±     54.666   ns/op
AWSXRayRecorderBenchmark.beginEndDummySegmentSubsegmentBenchmark                   sample  242469    2412.564 ±     48.628   ns/op
AWSXRayRecorderBenchmark.beginEndSegmentBenchmark                                  sample  271082    9035.149 ±     66.519   ns/op
AWSXRayRecorderBenchmark.beginEndSegmentSubsegmentBenchmark                        sample  203521   12075.350 ±     85.069   ns/op
AWSXRayRecorderBenchmark.beginSegmentBenchmark                                     sample  343473    1615.833 ±     32.173   ns/op
AWSXRayRecorderBenchmark.beginSubsegmentBenchmark                                  sample  227203     892.790 ±     28.473   ns/op
AWSXRayRecorderBenchmark.beginSubsegmentDummyParentBenchmark                       sample  211253     974.702 ±     44.365   ns/op
AWSXRayRecorderBenchmark.endDummySegmentBenchmark                                  sample  268058     453.411 ±     13.522   ns/op
AWSXRayRecorderBenchmark.endSegmentBenchmark                                       sample  268219    7414.041 ±     50.324   ns/op
AWSXRayRecorderBenchmark.endSegmentNoChildBenchmark                                sample  262589    7502.169 ±     48.535   ns/op
AWSXRayRecorderBenchmark.endSegmentWithChildBenchmark                              sample  206155    9390.585 ±     70.031   ns/op
AWSXRayRecorderBenchmark.endSubsegmentBenchmark                                    sample  205664     372.220 ±     13.495   ns/op
AWSXRayRecorderBenchmark.endSubsegmentDummyParentBenchmark                         sample  270074     370.760 ±      7.143   ns/op
AWSXRayRecorderBenchmark.getSegmentBenchmark                                       sample  222120     193.887 ±      0.556   ns/op
AWSXRayRecorderBenchmark.getSubsegmentBenchmark                                    sample  219070     171.070 ±      0.499   ns/op
entities.EntityBenchmark.constructSegmentBenchmark                                 sample  303499     930.151 ±     17.919   ns/op
entities.EntityBenchmark.constructSubsegmentPutInSegmentBenchmark                  sample  390103     540.864 ±     10.458   ns/op
entities.EntityBenchmark.putAnnotationBenchmark                                    sample  274287      84.191 ±     10.058   ns/op
entities.EntityBenchmark.putExceptionSegmentBenchmark                              sample  341112     873.304 ±     36.024   ns/op
entities.EntityBenchmark.putMetadataBenchmark                                      sample  261556     159.797 ±      0.497   ns/op
entities.EntitySerializerBenchmark.serializeFourChildSegment                       sample  285109   10578.761 ±     48.766   ns/op
entities.EntitySerializerBenchmark.serializeFourGenerationSegment                  sample  329896   10075.489 ±     44.027   ns/op
entities.EntitySerializerBenchmark.serializeOneChildSegment                        sample  324308    5361.372 ±    207.563   ns/op
entities.EntitySerializerBenchmark.serializeThreeChildSegment                      sample  296002    8668.409 ±     39.159   ns/op
entities.EntitySerializerBenchmark.serializeThreeGenerationSegment                 sample  342760    8468.849 ±     39.072   ns/op
entities.EntitySerializerBenchmark.serializeTwoChildSegment                        sample  311855    6722.815 ±     36.229   ns/op
entities.EntitySerializerBenchmark.serializeTwoGenerationSegment                   sample  376537    6783.476 ±     31.444   ns/op
entities.EntitySerializerBenchmark.serializeZeroChildSegment                       sample  366788    2988.778 ±     21.014   ns/op
strategy.sampling.LocalizedSamplingStrategyBenchmark.defaultSamplingRuleBenchmark  sample  257824     114.778 ±      0.279   ns/op
strategy.sampling.LocalizedSamplingStrategyBenchmark.noSampleSamplingBenchmark     sample  333933     150.760 ±      5.409   ns/op
AWSXRayRecorderBenchmark.beginDummySegmentBenchmark                                    ss      20   71780.850 ±  13043.936   ns/op
AWSXRayRecorderBenchmark.beginEndDummySegmentBenchmark                                 ss      20  106716.200 ±  15338.184   ns/op
AWSXRayRecorderBenchmark.beginEndDummySegmentSubsegmentBenchmark                       ss      20  173875.150 ±  16590.641   ns/op
AWSXRayRecorderBenchmark.beginEndSegmentBenchmark                                      ss      20  301932.850 ±  61829.854   ns/op
AWSXRayRecorderBenchmark.beginEndSegmentSubsegmentBenchmark                            ss      20  388685.150 ±  38089.192   ns/op
AWSXRayRecorderBenchmark.beginSegmentBenchmark                                         ss      20  106415.100 ±  22736.487   ns/op
AWSXRayRecorderBenchmark.beginSubsegmentBenchmark                                      ss      20   50527.450 ±  13094.358   ns/op
AWSXRayRecorderBenchmark.beginSubsegmentDummyParentBenchmark                           ss      20   53030.550 ±  14692.622   ns/op
AWSXRayRecorderBenchmark.endDummySegmentBenchmark                                      ss      20   44701.600 ±   9207.924   ns/op
AWSXRayRecorderBenchmark.endSegmentBenchmark                                           ss      20  317982.450 ± 481059.745   ns/op
AWSXRayRecorderBenchmark.endSegmentNoChildBenchmark                                    ss      20  203383.600 ±  65826.428   ns/op
AWSXRayRecorderBenchmark.endSegmentWithChildBenchmark                                  ss      20  232714.050 ±  21600.059   ns/op
AWSXRayRecorderBenchmark.endSubsegmentBenchmark                                        ss      20   43734.300 ±  14613.564   ns/op
AWSXRayRecorderBenchmark.endSubsegmentDummyParentBenchmark                             ss      20   38158.150 ±  13030.108   ns/op
AWSXRayRecorderBenchmark.getSegmentBenchmark                                           ss      20   18086.550 ±   2397.587   ns/op
AWSXRayRecorderBenchmark.getSubsegmentBenchmark                                        ss      20   13893.700 ±   2565.680   ns/op
entities.EntityBenchmark.constructSegmentBenchmark                                     ss      20   34599.050 ±   6171.982   ns/op
entities.EntityBenchmark.constructSubsegmentPutInSegmentBenchmark                      ss      20   19230.250 ±   7912.950   ns/op
entities.EntityBenchmark.putAnnotationBenchmark                                        ss      20    1942.150 ±    137.822   ns/op
entities.EntityBenchmark.putExceptionSegmentBenchmark                                  ss      20   80286.550 ±  12801.908   ns/op
entities.EntityBenchmark.putMetadataBenchmark                                          ss      20    5165.400 ±    649.324   ns/op
entities.EntitySerializerBenchmark.serializeFourChildSegment                           ss      20  264698.350 ±  77626.498   ns/op
entities.EntitySerializerBenchmark.serializeFourGenerationSegment                      ss      20  298566.900 ±  44469.691   ns/op
entities.EntitySerializerBenchmark.serializeOneChildSegment                            ss      20  217949.450 ±  19814.233   ns/op
entities.EntitySerializerBenchmark.serializeThreeChildSegment                          ss      20  260486.550 ±  41310.794   ns/op
entities.EntitySerializerBenchmark.serializeThreeGenerationSegment                     ss      20  269507.400 ±  35183.580   ns/op
entities.EntitySerializerBenchmark.serializeTwoChildSegment                            ss      20  234306.050 ±  25350.092   ns/op
entities.EntitySerializerBenchmark.serializeTwoGenerationSegment                       ss      20  237843.700 ±  23343.518   ns/op
entities.EntitySerializerBenchmark.serializeZeroChildSegment                           ss      20  168435.600 ±  23087.524   ns/op
strategy.sampling.LocalizedSamplingStrategyBenchmark.defaultSamplingRuleBenchmark      ss      20   15432.150 ±   2119.627   ns/op
strategy.sampling.LocalizedSamplingStrategyBenchmark.noSampleSamplingBenchmark         ss      20   30223.750 ±   3854.294   ns/op
```
</p>
</details>

#### 2.0.x
<details><summary>Show</summary>
<p>

```
Benchmark                                                                              Mode     Cnt        Score        Error   Units
AWSXRayRecorderBenchmark.beginDummySegmentBenchmark                                   thrpt      20        0.001 ±      0.001  ops/ns
AWSXRayRecorderBenchmark.beginEndDummySegmentBenchmark                                thrpt      20        0.001 ±      0.001  ops/ns
AWSXRayRecorderBenchmark.beginEndDummySegmentSubsegmentBenchmark                      thrpt      20       ≈ 10⁻³               ops/ns
AWSXRayRecorderBenchmark.beginEndSegmentBenchmark                                     thrpt      20       ≈ 10⁻⁴               ops/ns
AWSXRayRecorderBenchmark.beginEndSegmentSubsegmentBenchmark                           thrpt      20       ≈ 10⁻⁴               ops/ns
AWSXRayRecorderBenchmark.beginSegmentBenchmark                                        thrpt      20        0.001 ±      0.001  ops/ns
AWSXRayRecorderBenchmark.beginSubsegmentBenchmark                                     thrpt      20        0.001 ±      0.001  ops/ns
AWSXRayRecorderBenchmark.beginSubsegmentDummyParentBenchmark                          thrpt      20        0.001 ±      0.001  ops/ns
AWSXRayRecorderBenchmark.endDummySegmentBenchmark                                     thrpt      20        0.002 ±      0.001  ops/ns
AWSXRayRecorderBenchmark.endSegmentBenchmark                                          thrpt      20       ≈ 10⁻⁴               ops/ns
AWSXRayRecorderBenchmark.endSegmentNoChildBenchmark                                   thrpt      20       ≈ 10⁻⁴               ops/ns
AWSXRayRecorderBenchmark.endSegmentWithChildBenchmark                                 thrpt      20       ≈ 10⁻⁴               ops/ns
AWSXRayRecorderBenchmark.endSubsegmentBenchmark                                       thrpt      20        0.003 ±      0.001  ops/ns
AWSXRayRecorderBenchmark.endSubsegmentDummyParentBenchmark                            thrpt      20        0.003 ±      0.001  ops/ns
AWSXRayRecorderBenchmark.getSegmentBenchmark                                          thrpt      20        0.005 ±      0.001  ops/ns
AWSXRayRecorderBenchmark.getSubsegmentBenchmark                                       thrpt      20        0.006 ±      0.001  ops/ns
entities.EntityBenchmark.constructSegmentBenchmark                                    thrpt      20        0.001 ±      0.001  ops/ns
entities.EntityBenchmark.constructSubsegmentPutInSegmentBenchmark                     thrpt      20        0.002 ±      0.001  ops/ns
entities.EntityBenchmark.putAnnotationBenchmark                                       thrpt      20        0.012 ±      0.001  ops/ns
entities.EntityBenchmark.putExceptionSegmentBenchmark                                 thrpt      20        0.001 ±      0.001  ops/ns
entities.EntityBenchmark.putMetadataBenchmark                                         thrpt      20        0.006 ±      0.001  ops/ns
entities.EntitySerializerBenchmark.serializeFourChildSegment                          thrpt      20       ≈ 10⁻⁴               ops/ns
entities.EntitySerializerBenchmark.serializeFourGenerationSegment                     thrpt      20       ≈ 10⁻⁴               ops/ns
entities.EntitySerializerBenchmark.serializeOneChildSegment                           thrpt      20       ≈ 10⁻⁴               ops/ns
entities.EntitySerializerBenchmark.serializeThreeChildSegment                         thrpt      20       ≈ 10⁻⁴               ops/ns
entities.EntitySerializerBenchmark.serializeThreeGenerationSegment                    thrpt      20       ≈ 10⁻⁴               ops/ns
entities.EntitySerializerBenchmark.serializeTwoChildSegment                           thrpt      20       ≈ 10⁻⁴               ops/ns
entities.EntitySerializerBenchmark.serializeTwoGenerationSegment                      thrpt      20       ≈ 10⁻⁴               ops/ns
entities.EntitySerializerBenchmark.serializeZeroChildSegment                          thrpt      20       ≈ 10⁻³               ops/ns
strategy.sampling.CentralizedSamplingStrategyBenchmark.defaultSamplingRuleBenchmark   thrpt      20       ≈ 10⁻⁶               ops/ns
strategy.sampling.CentralizedSamplingStrategyBenchmark.noSampleSamplingBenchmark      thrpt      20       ≈ 10⁻⁶               ops/ns
strategy.sampling.LocalizedSamplingStrategyBenchmark.defaultSamplingRuleBenchmark     thrpt      20        0.015 ±      0.001  ops/ns
strategy.sampling.LocalizedSamplingStrategyBenchmark.noSampleSamplingBenchmark        thrpt      20        0.007 ±      0.001  ops/ns
AWSXRayRecorderBenchmark.beginDummySegmentBenchmark                                    avgt      20      932.213 ±      1.533   ns/op
AWSXRayRecorderBenchmark.beginEndDummySegmentBenchmark                                 avgt      20     1375.375 ±    141.220   ns/op
AWSXRayRecorderBenchmark.beginEndDummySegmentSubsegmentBenchmark                       avgt      20     2478.136 ±      2.670   ns/op
AWSXRayRecorderBenchmark.beginEndSegmentBenchmark                                      avgt      20     9127.754 ±     19.352   ns/op
AWSXRayRecorderBenchmark.beginEndSegmentSubsegmentBenchmark                            avgt      20    12257.246 ±    337.792   ns/op
AWSXRayRecorderBenchmark.beginSegmentBenchmark                                         avgt      20     1647.842 ±      2.535   ns/op
AWSXRayRecorderBenchmark.beginSubsegmentBenchmark                                      avgt      20      866.843 ±      3.770   ns/op
AWSXRayRecorderBenchmark.beginSubsegmentDummyParentBenchmark                           avgt      20      925.648 ±     57.932   ns/op
AWSXRayRecorderBenchmark.endDummySegmentBenchmark                                      avgt      20      444.140 ±      3.100   ns/op
AWSXRayRecorderBenchmark.endSegmentBenchmark                                           avgt      20     7442.642 ±     14.195   ns/op
AWSXRayRecorderBenchmark.endSegmentNoChildBenchmark                                    avgt      20     7473.374 ±    274.008   ns/op
AWSXRayRecorderBenchmark.endSegmentWithChildBenchmark                                  avgt      20     9311.713 ±     31.022   ns/op
AWSXRayRecorderBenchmark.endSubsegmentBenchmark                                        avgt      20      352.493 ±      1.498   ns/op
AWSXRayRecorderBenchmark.endSubsegmentDummyParentBenchmark                             avgt      20      345.812 ±     26.341   ns/op
AWSXRayRecorderBenchmark.getSegmentBenchmark                                           avgt      20      197.065 ±      1.262   ns/op
AWSXRayRecorderBenchmark.getSubsegmentBenchmark                                        avgt      20      175.089 ±      3.163   ns/op
entities.EntityBenchmark.constructSegmentBenchmark                                     avgt      20     1120.922 ±    177.050   ns/op
entities.EntityBenchmark.constructSubsegmentPutInSegmentBenchmark                      avgt      20      571.394 ±      2.378   ns/op
entities.EntityBenchmark.putAnnotationBenchmark                                        avgt      20       79.732 ±      0.670   ns/op
entities.EntityBenchmark.putExceptionSegmentBenchmark                                  avgt      20      787.575 ±     18.291   ns/op
entities.EntityBenchmark.putMetadataBenchmark                                          avgt      20      156.425 ±      1.451   ns/op
entities.EntitySerializerBenchmark.serializeFourChildSegment                           avgt      20    10516.215 ±     45.882   ns/op
entities.EntitySerializerBenchmark.serializeFourGenerationSegment                      avgt      20    10207.415 ±     39.337   ns/op
entities.EntitySerializerBenchmark.serializeOneChildSegment                            avgt      20     5543.381 ±   1486.911   ns/op
entities.EntitySerializerBenchmark.serializeThreeChildSegment                          avgt      20     8764.877 ±     94.731   ns/op
entities.EntitySerializerBenchmark.serializeThreeGenerationSegment                     avgt      20     8429.500 ±     27.255   ns/op
entities.EntitySerializerBenchmark.serializeTwoChildSegment                            avgt      20     6676.010 ±    128.959   ns/op
entities.EntitySerializerBenchmark.serializeTwoGenerationSegment                       avgt      20     6624.364 ±     35.145   ns/op
entities.EntitySerializerBenchmark.serializeZeroChildSegment                           avgt      20     3025.951 ±     19.453   ns/op
strategy.sampling.CentralizedSamplingStrategyBenchmark.defaultSamplingRuleBenchmark    avgt      20   823736.245 ± 324446.993   ns/op
strategy.sampling.CentralizedSamplingStrategyBenchmark.noSampleSamplingBenchmark       avgt      20   791788.528 ± 341848.672   ns/op
strategy.sampling.LocalizedSamplingStrategyBenchmark.defaultSamplingRuleBenchmark      avgt      20       66.956 ±      0.197   ns/op
strategy.sampling.LocalizedSamplingStrategyBenchmark.noSampleSamplingBenchmark         avgt      20      148.325 ±     10.031   ns/op
AWSXRayRecorderBenchmark.beginDummySegmentBenchmark                                  sample  280775      951.650 ±     37.475   ns/op
AWSXRayRecorderBenchmark.beginEndDummySegmentBenchmark                               sample  200595     1381.797 ±     43.789   ns/op
AWSXRayRecorderBenchmark.beginEndDummySegmentSubsegmentBenchmark                     sample  240656     2362.696 ±     42.796   ns/op
AWSXRayRecorderBenchmark.beginEndSegmentBenchmark                                    sample  268840     9091.674 ±     66.711   ns/op
AWSXRayRecorderBenchmark.beginEndSegmentSubsegmentBenchmark                          sample  206472    11910.439 ±     90.948   ns/op
AWSXRayRecorderBenchmark.beginSegmentBenchmark                                       sample  337004     1728.847 ±     50.414   ns/op
AWSXRayRecorderBenchmark.beginSubsegmentBenchmark                                    sample  224892      980.078 ±     48.343   ns/op
AWSXRayRecorderBenchmark.beginSubsegmentDummyParentBenchmark                         sample  225620      923.680 ±     43.547   ns/op
AWSXRayRecorderBenchmark.endDummySegmentBenchmark                                    sample  255565      521.729 ±     22.663   ns/op
AWSXRayRecorderBenchmark.endSegmentBenchmark                                         sample  270729     7336.683 ±     56.541   ns/op
AWSXRayRecorderBenchmark.endSegmentNoChildBenchmark                                  sample  269507     7330.511 ±     52.730   ns/op
AWSXRayRecorderBenchmark.endSegmentWithChildBenchmark                                sample  201638     9423.119 ±     62.879   ns/op
AWSXRayRecorderBenchmark.endSubsegmentBenchmark                                      sample  204011      371.572 ±     10.796   ns/op
AWSXRayRecorderBenchmark.endSubsegmentDummyParentBenchmark                           sample  270390      370.156 ±      0.708   ns/op
AWSXRayRecorderBenchmark.getSegmentBenchmark                                         sample  215549      199.410 ±      0.661   ns/op
AWSXRayRecorderBenchmark.getSubsegmentBenchmark                                      sample  215148      176.346 ±     10.327   ns/op
entities.EntityBenchmark.constructSegmentBenchmark                                   sample  308366      978.141 ±     32.858   ns/op
entities.EntityBenchmark.constructSubsegmentPutInSegmentBenchmark                    sample  394908      672.604 ±     38.863   ns/op
entities.EntityBenchmark.putAnnotationBenchmark                                      sample  275641       82.524 ±      0.405   ns/op
entities.EntityBenchmark.putExceptionSegmentBenchmark                                sample  344055      793.917 ±     22.564   ns/op
entities.EntityBenchmark.putMetadataBenchmark                                        sample  268312      156.575 ±      0.348   ns/op
entities.EntitySerializerBenchmark.serializeFourChildSegment                         sample  276738    10759.065 ±     40.994   ns/op
entities.EntitySerializerBenchmark.serializeFourGenerationSegment                    sample  336792    10115.702 ±     45.922   ns/op
entities.EntitySerializerBenchmark.serializeOneChildSegment                          sample  331540     5152.807 ±     24.741   ns/op
entities.EntitySerializerBenchmark.serializeThreeChildSegment                        sample  292610     8827.096 ±     55.710   ns/op
entities.EntitySerializerBenchmark.serializeThreeGenerationSegment                   sample  357109     8261.909 ±     34.359   ns/op
entities.EntitySerializerBenchmark.serializeTwoChildSegment                          sample  312868     6850.871 ±     39.525   ns/op
entities.EntitySerializerBenchmark.serializeTwoGenerationSegment                     sample  370658     6915.767 ±     36.542   ns/op
entities.EntitySerializerBenchmark.serializeZeroChildSegment                         sample  356862     3084.496 ±     34.069   ns/op
strategy.sampling.CentralizedSamplingStrategyBenchmark.defaultSamplingRuleBenchmark  sample   27201   734722.616 ± 110761.289   ns/op
strategy.sampling.CentralizedSamplingStrategyBenchmark.noSampleSamplingBenchmark     sample   27415   742860.436 ± 106937.420   ns/op
strategy.sampling.LocalizedSamplingStrategyBenchmark.defaultSamplingRuleBenchmark    sample  271443      101.288 ±      0.349   ns/op
strategy.sampling.LocalizedSamplingStrategyBenchmark.noSampleSamplingBenchmark       sample  250533      188.131 ±      7.420   ns/op
AWSXRayRecorderBenchmark.beginDummySegmentBenchmark                                      ss      20    68370.650 ±  11627.071   ns/op
AWSXRayRecorderBenchmark.beginEndDummySegmentBenchmark                                   ss      20   100117.800 ±  12494.162   ns/op
AWSXRayRecorderBenchmark.beginEndDummySegmentSubsegmentBenchmark                         ss      20   158388.000 ±  34965.201   ns/op
AWSXRayRecorderBenchmark.beginEndSegmentBenchmark                                        ss      20   345795.250 ±  76751.481   ns/op
AWSXRayRecorderBenchmark.beginEndSegmentSubsegmentBenchmark                              ss      20   593120.900 ± 518445.478   ns/op
AWSXRayRecorderBenchmark.beginSegmentBenchmark                                           ss      20    90836.150 ±   6853.108   ns/op
AWSXRayRecorderBenchmark.beginSubsegmentBenchmark                                        ss      20    47766.900 ±   7274.915   ns/op
AWSXRayRecorderBenchmark.beginSubsegmentDummyParentBenchmark                             ss      20   190739.350 ± 552172.899   ns/op
AWSXRayRecorderBenchmark.endDummySegmentBenchmark                                        ss      20    51147.600 ±   4794.063   ns/op
AWSXRayRecorderBenchmark.endSegmentBenchmark                                             ss      20   197417.150 ±  48739.834   ns/op
AWSXRayRecorderBenchmark.endSegmentNoChildBenchmark                                      ss      20   197504.900 ±  57174.870   ns/op
AWSXRayRecorderBenchmark.endSegmentWithChildBenchmark                                    ss      20   274234.400 ±  39891.232   ns/op
AWSXRayRecorderBenchmark.endSubsegmentBenchmark                                          ss      20    38756.900 ±   8849.498   ns/op
AWSXRayRecorderBenchmark.endSubsegmentDummyParentBenchmark                               ss      20    25866.500 ±   4889.527   ns/op
AWSXRayRecorderBenchmark.getSegmentBenchmark                                             ss      20    13732.600 ±   3244.993   ns/op
AWSXRayRecorderBenchmark.getSubsegmentBenchmark                                          ss      20    16197.700 ±   6158.938   ns/op
entities.EntityBenchmark.constructSegmentBenchmark                                       ss      20    49008.100 ±   4527.988   ns/op
entities.EntityBenchmark.constructSubsegmentPutInSegmentBenchmark                        ss      20    17926.300 ±   7054.509   ns/op
entities.EntityBenchmark.putAnnotationBenchmark                                          ss      20     1646.300 ±     49.005   ns/op
entities.EntityBenchmark.putExceptionSegmentBenchmark                                    ss      20    52532.100 ±   8539.397   ns/op
entities.EntityBenchmark.putMetadataBenchmark                                            ss      20     4426.000 ±    522.927   ns/op
entities.EntitySerializerBenchmark.serializeFourChildSegment                             ss      20   270610.450 ±  41210.088   ns/op
entities.EntitySerializerBenchmark.serializeFourGenerationSegment                        ss      20   286474.600 ±  60631.433   ns/op
entities.EntitySerializerBenchmark.serializeOneChildSegment                              ss      20   204155.850 ±  58185.492   ns/op
entities.EntitySerializerBenchmark.serializeThreeChildSegment                            ss      20   268726.300 ±  88452.746   ns/op
entities.EntitySerializerBenchmark.serializeThreeGenerationSegment                       ss      20   247986.600 ±  42356.471   ns/op
entities.EntitySerializerBenchmark.serializeTwoChildSegment                              ss      20   214421.200 ±  48320.247   ns/op
entities.EntitySerializerBenchmark.serializeTwoGenerationSegment                         ss      20   170816.850 ±  26840.355   ns/op
entities.EntitySerializerBenchmark.serializeZeroChildSegment                             ss      20   154021.600 ±  17279.524   ns/op
strategy.sampling.CentralizedSamplingStrategyBenchmark.defaultSamplingRuleBenchmark      ss      20  3793324.000 ± 656772.473   ns/op
strategy.sampling.CentralizedSamplingStrategyBenchmark.noSampleSamplingBenchmark         ss      20  3766371.300 ± 793959.189   ns/op
strategy.sampling.LocalizedSamplingStrategyBenchmark.defaultSamplingRuleBenchmark        ss      20    11442.200 ±    586.758   ns/op
strategy.sampling.LocalizedSamplingStrategyBenchmark.noSampleSamplingBenchmark           ss      20    28641.600 ±   2647.503   ns/op
```
</p>
</details>

#### 2.2.x
<details><summary>Show</summary>
<p>

```
Benchmark                                                                              Mode     Cnt       Score        Error   Units
AWSXRayRecorderBenchmark.beginDummySegmentBenchmark                                   thrpt      20       0.001 ±      0.001  ops/ns
AWSXRayRecorderBenchmark.beginEndDummySegmentBenchmark                                thrpt      20       0.001 ±      0.001  ops/ns
AWSXRayRecorderBenchmark.beginEndDummySegmentSubsegmentBenchmark                      thrpt      20      ≈ 10⁻³               ops/ns
AWSXRayRecorderBenchmark.beginEndSegmentBenchmark                                     thrpt      20      ≈ 10⁻⁴               ops/ns
AWSXRayRecorderBenchmark.beginEndSegmentSubsegmentBenchmark                           thrpt      20      ≈ 10⁻⁴               ops/ns
AWSXRayRecorderBenchmark.beginSegmentBenchmark                                        thrpt      20       0.001 ±      0.001  ops/ns
AWSXRayRecorderBenchmark.beginSubsegmentBenchmark                                     thrpt      20       0.001 ±      0.001  ops/ns
AWSXRayRecorderBenchmark.beginSubsegmentDummyParentBenchmark                          thrpt      20       0.001 ±      0.001  ops/ns
AWSXRayRecorderBenchmark.endDummySegmentBenchmark                                     thrpt      20       0.002 ±      0.001  ops/ns
AWSXRayRecorderBenchmark.endSegmentBenchmark                                          thrpt      20      ≈ 10⁻⁴               ops/ns
AWSXRayRecorderBenchmark.endSegmentNoChildBenchmark                                   thrpt      20      ≈ 10⁻⁴               ops/ns
AWSXRayRecorderBenchmark.endSegmentWithChildBenchmark                                 thrpt      20      ≈ 10⁻⁴               ops/ns
AWSXRayRecorderBenchmark.endSubsegmentBenchmark                                       thrpt      20       0.003 ±      0.001  ops/ns
AWSXRayRecorderBenchmark.endSubsegmentDummyParentBenchmark                            thrpt      20       0.003 ±      0.001  ops/ns
AWSXRayRecorderBenchmark.getSegmentBenchmark                                          thrpt      20       0.005 ±      0.001  ops/ns
AWSXRayRecorderBenchmark.getSubsegmentBenchmark                                       thrpt      20       0.006 ±      0.001  ops/ns
entities.EntityBenchmark.constructSegmentBenchmark                                    thrpt      20       0.001 ±      0.001  ops/ns
entities.EntityBenchmark.constructSubsegmentPutInSegmentBenchmark                     thrpt      20       0.002 ±      0.001  ops/ns
entities.EntityBenchmark.putAnnotationBenchmark                                       thrpt      20       0.012 ±      0.001  ops/ns
entities.EntityBenchmark.putExceptionSegmentBenchmark                                 thrpt      20       0.001 ±      0.001  ops/ns
entities.EntityBenchmark.putMetadataBenchmark                                         thrpt      20       0.006 ±      0.001  ops/ns
entities.EntitySerializerBenchmark.serializeFourChildSegment                          thrpt      20      ≈ 10⁻⁴               ops/ns
entities.EntitySerializerBenchmark.serializeFourGenerationSegment                     thrpt      20      ≈ 10⁻⁴               ops/ns
entities.EntitySerializerBenchmark.serializeOneChildSegment                           thrpt      20      ≈ 10⁻⁴               ops/ns
entities.EntitySerializerBenchmark.serializeThreeChildSegment                         thrpt      20      ≈ 10⁻⁴               ops/ns
entities.EntitySerializerBenchmark.serializeThreeGenerationSegment                    thrpt      20      ≈ 10⁻⁴               ops/ns
entities.EntitySerializerBenchmark.serializeTwoChildSegment                           thrpt      20      ≈ 10⁻⁴               ops/ns
entities.EntitySerializerBenchmark.serializeTwoGenerationSegment                      thrpt      20      ≈ 10⁻⁴               ops/ns
entities.EntitySerializerBenchmark.serializeZeroChildSegment                          thrpt      20      ≈ 10⁻³               ops/ns
strategy.sampling.CentralizedSamplingStrategyBenchmark.defaultSamplingRuleBenchmark   thrpt      20       0.009 ±      0.001  ops/ns
strategy.sampling.CentralizedSamplingStrategyBenchmark.noSampleSamplingBenchmark      thrpt      20       0.005 ±      0.001  ops/ns
strategy.sampling.LocalizedSamplingStrategyBenchmark.defaultSamplingRuleBenchmark     thrpt      20       0.014 ±      0.002  ops/ns
strategy.sampling.LocalizedSamplingStrategyBenchmark.noSampleSamplingBenchmark        thrpt      20       0.006 ±      0.001  ops/ns
AWSXRayRecorderBenchmark.beginDummySegmentBenchmark                                    avgt      20    1116.352 ±     65.138   ns/op
AWSXRayRecorderBenchmark.beginEndDummySegmentBenchmark                                 avgt      20    1384.115 ±    116.348   ns/op
AWSXRayRecorderBenchmark.beginEndDummySegmentSubsegmentBenchmark                       avgt      20    2548.739 ±     17.057   ns/op
AWSXRayRecorderBenchmark.beginEndSegmentBenchmark                                      avgt      20    9229.839 ±     21.222   ns/op
AWSXRayRecorderBenchmark.beginEndSegmentSubsegmentBenchmark                            avgt      20   12106.073 ±    258.082   ns/op
AWSXRayRecorderBenchmark.beginSegmentBenchmark                                         avgt      20    1636.812 ±      2.025   ns/op
AWSXRayRecorderBenchmark.beginSubsegmentBenchmark                                      avgt      20     889.173 ±      2.707   ns/op
AWSXRayRecorderBenchmark.beginSubsegmentDummyParentBenchmark                           avgt      20     884.073 ±     17.982   ns/op
AWSXRayRecorderBenchmark.endDummySegmentBenchmark                                      avgt      20     445.395 ±      3.020   ns/op
AWSXRayRecorderBenchmark.endSegmentBenchmark                                           avgt      20    7399.554 ±     19.442   ns/op
AWSXRayRecorderBenchmark.endSegmentNoChildBenchmark                                    avgt      20    7480.519 ±    341.880   ns/op
AWSXRayRecorderBenchmark.endSegmentWithChildBenchmark                                  avgt      20    9194.198 ±     15.454   ns/op
AWSXRayRecorderBenchmark.endSubsegmentBenchmark                                        avgt      20     349.199 ±      2.644   ns/op
AWSXRayRecorderBenchmark.endSubsegmentDummyParentBenchmark                             avgt      20     359.009 ±     86.696   ns/op
AWSXRayRecorderBenchmark.getSegmentBenchmark                                           avgt      20     200.958 ±      2.204   ns/op
AWSXRayRecorderBenchmark.getSubsegmentBenchmark                                        avgt      20     173.120 ±      1.907   ns/op
entities.EntityBenchmark.constructSegmentBenchmark                                     avgt      20    1035.187 ±     23.819   ns/op
entities.EntityBenchmark.constructSubsegmentPutInSegmentBenchmark                      avgt      20     571.755 ±      2.414   ns/op
entities.EntityBenchmark.putAnnotationBenchmark                                        avgt      20      80.885 ±      0.962   ns/op
entities.EntityBenchmark.putExceptionSegmentBenchmark                                  avgt      20     835.846 ±     39.917   ns/op
entities.EntityBenchmark.putMetadataBenchmark                                          avgt      20     155.620 ±      1.347   ns/op
entities.EntitySerializerBenchmark.serializeFourChildSegment                           avgt      20   10602.465 ±     39.805   ns/op
entities.EntitySerializerBenchmark.serializeFourGenerationSegment                      avgt      20   10283.689 ±    457.224   ns/op
entities.EntitySerializerBenchmark.serializeOneChildSegment                            avgt      20    5115.504 ±     27.507   ns/op
entities.EntitySerializerBenchmark.serializeThreeChildSegment                          avgt      20    8688.281 ±     42.657   ns/op
entities.EntitySerializerBenchmark.serializeThreeGenerationSegment                     avgt      20    8471.203 ±    312.402   ns/op
entities.EntitySerializerBenchmark.serializeTwoChildSegment                            avgt      20    6798.185 ±     28.915   ns/op
entities.EntitySerializerBenchmark.serializeTwoGenerationSegment                       avgt      20    6628.881 ±     25.755   ns/op
entities.EntitySerializerBenchmark.serializeZeroChildSegment                           avgt      20    3055.384 ±    103.331   ns/op
strategy.sampling.CentralizedSamplingStrategyBenchmark.defaultSamplingRuleBenchmark    avgt      20     113.674 ±      5.104   ns/op
strategy.sampling.CentralizedSamplingStrategyBenchmark.noSampleSamplingBenchmark       avgt      20     207.037 ±      6.451   ns/op
strategy.sampling.LocalizedSamplingStrategyBenchmark.defaultSamplingRuleBenchmark      avgt      20      66.541 ±      0.093   ns/op
strategy.sampling.LocalizedSamplingStrategyBenchmark.noSampleSamplingBenchmark         avgt      20     153.962 ±      5.669   ns/op
AWSXRayRecorderBenchmark.beginDummySegmentBenchmark                                  sample  284089     931.994 ±     33.393   ns/op
AWSXRayRecorderBenchmark.beginEndDummySegmentBenchmark                               sample  205393    1503.007 ±     68.511   ns/op
AWSXRayRecorderBenchmark.beginEndDummySegmentSubsegmentBenchmark                     sample  226817    2582.186 ±     60.289   ns/op
AWSXRayRecorderBenchmark.beginEndSegmentBenchmark                                    sample  260821    9391.118 ±     74.131   ns/op
AWSXRayRecorderBenchmark.beginEndSegmentSubsegmentBenchmark                          sample  206077   11962.424 ±     96.458   ns/op
AWSXRayRecorderBenchmark.beginSegmentBenchmark                                       sample  328003    1808.904 ±     54.412   ns/op
AWSXRayRecorderBenchmark.beginSubsegmentBenchmark                                    sample  233745     962.641 ±     49.752   ns/op
AWSXRayRecorderBenchmark.beginSubsegmentDummyParentBenchmark                         sample  218848     873.949 ±     18.823   ns/op
AWSXRayRecorderBenchmark.endDummySegmentBenchmark                                    sample  248482     526.263 ±    133.149   ns/op
AWSXRayRecorderBenchmark.endSegmentBenchmark                                         sample  262917    7533.645 ±     57.424   ns/op
AWSXRayRecorderBenchmark.endSegmentNoChildBenchmark                                  sample  266007    7411.349 ±     47.386   ns/op
AWSXRayRecorderBenchmark.endSegmentWithChildBenchmark                                sample  206637    9252.474 ±     69.567   ns/op
AWSXRayRecorderBenchmark.endSubsegmentBenchmark                                      sample  205960     377.350 ±     19.777   ns/op
AWSXRayRecorderBenchmark.endSubsegmentDummyParentBenchmark                           sample  276456     357.153 ±      9.942   ns/op
AWSXRayRecorderBenchmark.getSegmentBenchmark                                         sample  219355     204.551 ±     23.926   ns/op
AWSXRayRecorderBenchmark.getSubsegmentBenchmark                                      sample  224402     169.448 ±      0.306   ns/op
entities.EntityBenchmark.constructSegmentBenchmark                                   sample  310283     980.774 ±     35.691   ns/op
entities.EntityBenchmark.constructSubsegmentPutInSegmentBenchmark                    sample  385207     546.640 ±     13.767   ns/op
entities.EntityBenchmark.putAnnotationBenchmark                                      sample  280182      81.289 ±      0.341   ns/op
entities.EntityBenchmark.putExceptionSegmentBenchmark                                sample  340124     803.436 ±     24.653   ns/op
entities.EntityBenchmark.putMetadataBenchmark                                        sample  262435     164.339 ±     15.373   ns/op
entities.EntitySerializerBenchmark.serializeFourChildSegment                         sample  272837   10669.374 ±     50.429   ns/op
entities.EntitySerializerBenchmark.serializeFourGenerationSegment                    sample  333978   10188.362 ±     45.859   ns/op
entities.EntitySerializerBenchmark.serializeOneChildSegment                          sample  328413    5131.806 ±     19.300   ns/op
entities.EntitySerializerBenchmark.serializeThreeChildSegment                        sample  295515    8678.958 ±     43.823   ns/op
entities.EntitySerializerBenchmark.serializeThreeGenerationSegment                   sample  355647    8479.553 ±     36.573   ns/op
entities.EntitySerializerBenchmark.serializeTwoChildSegment                          sample  311291    6868.064 ±     34.837   ns/op
entities.EntitySerializerBenchmark.serializeTwoGenerationSegment                     sample  365130    7016.832 ±     36.956   ns/op
entities.EntitySerializerBenchmark.serializeZeroChildSegment                         sample  359870    2980.228 ±     28.911   ns/op
strategy.sampling.CentralizedSamplingStrategyBenchmark.defaultSamplingRuleBenchmark  sample  337373     147.561 ±      8.108   ns/op
strategy.sampling.CentralizedSamplingStrategyBenchmark.noSampleSamplingBenchmark     sample  375039     240.887 ±      0.367   ns/op
strategy.sampling.LocalizedSamplingStrategyBenchmark.defaultSamplingRuleBenchmark    sample  272117     102.284 ±      7.177   ns/op
strategy.sampling.LocalizedSamplingStrategyBenchmark.noSampleSamplingBenchmark       sample  251570     185.437 ±      7.562   ns/op
AWSXRayRecorderBenchmark.beginDummySegmentBenchmark                                      ss      20   71174.150 ±  16424.521   ns/op
AWSXRayRecorderBenchmark.beginEndDummySegmentBenchmark                                   ss      20  108678.650 ±  12724.120   ns/op
AWSXRayRecorderBenchmark.beginEndDummySegmentSubsegmentBenchmark                         ss      20  163669.950 ±  19524.146   ns/op
AWSXRayRecorderBenchmark.beginEndSegmentBenchmark                                        ss      20  318049.550 ±  69059.912   ns/op
AWSXRayRecorderBenchmark.beginEndSegmentSubsegmentBenchmark                              ss      20  504465.650 ± 517056.690   ns/op
AWSXRayRecorderBenchmark.beginSegmentBenchmark                                           ss      20   90568.900 ±   7221.828   ns/op
AWSXRayRecorderBenchmark.beginSubsegmentBenchmark                                        ss      20   46093.500 ±   6820.556   ns/op
AWSXRayRecorderBenchmark.beginSubsegmentDummyParentBenchmark                             ss      20   47105.600 ±   9366.854   ns/op
AWSXRayRecorderBenchmark.endDummySegmentBenchmark                                        ss      20   46869.850 ±   8903.325   ns/op
AWSXRayRecorderBenchmark.endSegmentBenchmark                                             ss      20  279840.150 ±  68061.636   ns/op
AWSXRayRecorderBenchmark.endSegmentNoChildBenchmark                                      ss      20  263707.150 ±  26406.598   ns/op
AWSXRayRecorderBenchmark.endSegmentWithChildBenchmark                                    ss      20  274557.600 ±  67514.350   ns/op
AWSXRayRecorderBenchmark.endSubsegmentBenchmark                                          ss      20   43848.350 ±  21195.445   ns/op
AWSXRayRecorderBenchmark.endSubsegmentDummyParentBenchmark                               ss      20   28571.650 ±   6948.929   ns/op
AWSXRayRecorderBenchmark.getSegmentBenchmark                                             ss      20   16270.700 ±   5463.849   ns/op
AWSXRayRecorderBenchmark.getSubsegmentBenchmark                                          ss      20   14082.550 ±   3384.261   ns/op
entities.EntityBenchmark.constructSegmentBenchmark                                       ss      20   35302.550 ±   8108.275   ns/op
entities.EntityBenchmark.constructSubsegmentPutInSegmentBenchmark                        ss      20   18459.800 ±   7297.470   ns/op
entities.EntityBenchmark.putAnnotationBenchmark                                          ss      20    1722.250 ±    509.794   ns/op
entities.EntityBenchmark.putExceptionSegmentBenchmark                                    ss      20   53393.800 ±   8129.174   ns/op
entities.EntityBenchmark.putMetadataBenchmark                                            ss      20    7369.300 ±   1118.205   ns/op
entities.EntitySerializerBenchmark.serializeFourChildSegment                             ss      20  253479.750 ±  35969.549   ns/op
entities.EntitySerializerBenchmark.serializeFourGenerationSegment                        ss      20  265888.600 ±  53059.660   ns/op
entities.EntitySerializerBenchmark.serializeOneChildSegment                              ss      20  216416.850 ±  79926.449   ns/op
entities.EntitySerializerBenchmark.serializeThreeChildSegment                            ss      20  174684.200 ±  38101.401   ns/op
entities.EntitySerializerBenchmark.serializeThreeGenerationSegment                       ss      20  232019.050 ±  96938.539   ns/op
entities.EntitySerializerBenchmark.serializeTwoChildSegment                              ss      20  203800.250 ±  34897.374   ns/op
entities.EntitySerializerBenchmark.serializeTwoGenerationSegment                         ss      20  224530.800 ±  29306.559   ns/op
entities.EntitySerializerBenchmark.serializeZeroChildSegment                             ss      20  182037.400 ±  21712.052   ns/op
strategy.sampling.CentralizedSamplingStrategyBenchmark.defaultSamplingRuleBenchmark      ss      20   53129.100 ±  88328.543   ns/op
strategy.sampling.CentralizedSamplingStrategyBenchmark.noSampleSamplingBenchmark         ss      20   24411.000 ±   2299.247   ns/op
strategy.sampling.LocalizedSamplingStrategyBenchmark.defaultSamplingRuleBenchmark        ss      20   11676.300 ±   1166.085   ns/op
strategy.sampling.LocalizedSamplingStrategyBenchmark.noSampleSamplingBenchmark           ss      20   27204.750 ±   2544.664   ns/op
```
</p>
</details>

#### 2.4.x
<details><summary>Show</summary>
<p>

```
Benchmark                                                                              Mode     Cnt       Score        Error   Units
AWSXRayRecorderBenchmark.beginDummySegmentBenchmark                                   thrpt      20       0.001 ±      0.001  ops/ns
AWSXRayRecorderBenchmark.beginEndDummySegmentBenchmark                                thrpt      20       0.001 ±      0.001  ops/ns
AWSXRayRecorderBenchmark.beginEndDummySegmentSubsegmentBenchmark                      thrpt      20      ≈ 10⁻³               ops/ns
AWSXRayRecorderBenchmark.beginEndSegmentBenchmark                                     thrpt      20      ≈ 10⁻⁴               ops/ns
AWSXRayRecorderBenchmark.beginEndSegmentSubsegmentBenchmark                           thrpt      20      ≈ 10⁻⁴               ops/ns
AWSXRayRecorderBenchmark.beginSegmentBenchmark                                        thrpt      20       0.001 ±      0.001  ops/ns
AWSXRayRecorderBenchmark.beginSubsegmentBenchmark                                     thrpt      20       0.001 ±      0.001  ops/ns
AWSXRayRecorderBenchmark.beginSubsegmentDummyParentBenchmark                          thrpt      20       0.001 ±      0.001  ops/ns
AWSXRayRecorderBenchmark.endDummySegmentBenchmark                                     thrpt      20       0.002 ±      0.001  ops/ns
AWSXRayRecorderBenchmark.endSegmentBenchmark                                          thrpt      20      ≈ 10⁻⁴               ops/ns
AWSXRayRecorderBenchmark.endSegmentNoChildBenchmark                                   thrpt      20      ≈ 10⁻⁴               ops/ns
AWSXRayRecorderBenchmark.endSegmentWithChildBenchmark                                 thrpt      20      ≈ 10⁻⁴               ops/ns
AWSXRayRecorderBenchmark.endSubsegmentBenchmark                                       thrpt      20       0.003 ±      0.001  ops/ns
AWSXRayRecorderBenchmark.endSubsegmentDummyParentBenchmark                            thrpt      20       0.003 ±      0.001  ops/ns
AWSXRayRecorderBenchmark.getSegmentBenchmark                                          thrpt      20       0.005 ±      0.001  ops/ns
AWSXRayRecorderBenchmark.getSubsegmentBenchmark                                       thrpt      20       0.006 ±      0.001  ops/ns
entities.EntityBenchmark.constructSegmentBenchmark                                    thrpt      20       0.001 ±      0.001  ops/ns
entities.EntityBenchmark.constructSubsegmentPutInSegmentBenchmark                     thrpt      20       0.002 ±      0.001  ops/ns
entities.EntityBenchmark.putAnnotationBenchmark                                       thrpt      20       0.011 ±      0.001  ops/ns
entities.EntityBenchmark.putExceptionSegmentBenchmark                                 thrpt      20       0.001 ±      0.001  ops/ns
entities.EntityBenchmark.putMetadataBenchmark                                         thrpt      20       0.006 ±      0.001  ops/ns
entities.EntitySerializerBenchmark.serializeFourChildSegment                          thrpt      20      ≈ 10⁻⁴               ops/ns
entities.EntitySerializerBenchmark.serializeFourGenerationSegment                     thrpt      20      ≈ 10⁻⁴               ops/ns
entities.EntitySerializerBenchmark.serializeOneChildSegment                           thrpt      20      ≈ 10⁻⁴               ops/ns
entities.EntitySerializerBenchmark.serializeThreeChildSegment                         thrpt      20      ≈ 10⁻⁴               ops/ns
entities.EntitySerializerBenchmark.serializeThreeGenerationSegment                    thrpt      20      ≈ 10⁻⁴               ops/ns
entities.EntitySerializerBenchmark.serializeTwoChildSegment                           thrpt      20      ≈ 10⁻⁴               ops/ns
entities.EntitySerializerBenchmark.serializeTwoGenerationSegment                      thrpt      20      ≈ 10⁻⁴               ops/ns
entities.EntitySerializerBenchmark.serializeZeroChildSegment                          thrpt      20      ≈ 10⁻³               ops/ns
strategy.sampling.CentralizedSamplingStrategyBenchmark.defaultSamplingRuleBenchmark   thrpt      20       0.009 ±      0.001  ops/ns
strategy.sampling.CentralizedSamplingStrategyBenchmark.noSampleSamplingBenchmark      thrpt      20       0.005 ±      0.001  ops/ns
strategy.sampling.LocalizedSamplingStrategyBenchmark.defaultSamplingRuleBenchmark     thrpt      20       0.015 ±      0.001  ops/ns
strategy.sampling.LocalizedSamplingStrategyBenchmark.noSampleSamplingBenchmark        thrpt      20       0.006 ±      0.001  ops/ns
AWSXRayRecorderBenchmark.beginDummySegmentBenchmark                                    avgt      20    1041.499 ±      1.949   ns/op
AWSXRayRecorderBenchmark.beginEndDummySegmentBenchmark                                 avgt      20    1565.383 ±      2.028   ns/op
AWSXRayRecorderBenchmark.beginEndDummySegmentSubsegmentBenchmark                       avgt      20    2770.433 ±      5.588   ns/op
AWSXRayRecorderBenchmark.beginEndSegmentBenchmark                                      avgt      20    9123.404 ±     16.630   ns/op
AWSXRayRecorderBenchmark.beginEndSegmentSubsegmentBenchmark                            avgt      20   12094.797 ±     43.192   ns/op
AWSXRayRecorderBenchmark.beginSegmentBenchmark                                         avgt      20    1715.721 ±      2.687   ns/op
AWSXRayRecorderBenchmark.beginSubsegmentBenchmark                                      avgt      20     950.378 ±      4.452   ns/op
AWSXRayRecorderBenchmark.beginSubsegmentDummyParentBenchmark                           avgt      20     905.941 ±      3.186   ns/op
AWSXRayRecorderBenchmark.endDummySegmentBenchmark                                      avgt      20     596.117 ±      4.522   ns/op
AWSXRayRecorderBenchmark.endSegmentBenchmark                                           avgt      20    7839.399 ±     43.860   ns/op
AWSXRayRecorderBenchmark.endSegmentNoChildBenchmark                                    avgt      20    7578.632 ±     25.965   ns/op
AWSXRayRecorderBenchmark.endSegmentWithChildBenchmark                                  avgt      20    9392.873 ±     33.571   ns/op
AWSXRayRecorderBenchmark.endSubsegmentBenchmark                                        avgt      20     426.620 ±      2.629   ns/op
AWSXRayRecorderBenchmark.endSubsegmentDummyParentBenchmark                             avgt      20     358.871 ±      2.002   ns/op
AWSXRayRecorderBenchmark.getSegmentBenchmark                                           avgt      20     203.888 ±      2.078   ns/op
AWSXRayRecorderBenchmark.getSubsegmentBenchmark                                        avgt      20     178.235 ±      1.341   ns/op
entities.EntityBenchmark.constructSegmentBenchmark                                     avgt      20    1017.563 ±      2.903   ns/op
entities.EntityBenchmark.constructSubsegmentPutInSegmentBenchmark                      avgt      20     571.089 ±      2.446   ns/op
entities.EntityBenchmark.putAnnotationBenchmark                                        avgt      20      86.213 ±      0.622   ns/op
entities.EntityBenchmark.putExceptionSegmentBenchmark                                  avgt      20     804.234 ±      4.239   ns/op
entities.EntityBenchmark.putMetadataBenchmark                                          avgt      20     165.585 ±      1.804   ns/op
entities.EntitySerializerBenchmark.serializeFourChildSegment                           avgt      20   10956.216 ±     38.724   ns/op
entities.EntitySerializerBenchmark.serializeFourGenerationSegment                      avgt      20   10655.412 ±     31.139   ns/op
entities.EntitySerializerBenchmark.serializeOneChildSegment                            avgt      20    5226.236 ±     28.453   ns/op
entities.EntitySerializerBenchmark.serializeThreeChildSegment                          avgt      20    8907.718 ±     36.280   ns/op
entities.EntitySerializerBenchmark.serializeThreeGenerationSegment                     avgt      20    8627.488 ±     34.715   ns/op
entities.EntitySerializerBenchmark.serializeTwoChildSegment                            avgt      20    7090.400 ±     31.906   ns/op
entities.EntitySerializerBenchmark.serializeTwoGenerationSegment                       avgt      20    6805.178 ±     14.657   ns/op
entities.EntitySerializerBenchmark.serializeZeroChildSegment                           avgt      20    3023.165 ±     22.132   ns/op
strategy.sampling.CentralizedSamplingStrategyBenchmark.defaultSamplingRuleBenchmark    avgt      20     111.288 ±      0.114   ns/op
strategy.sampling.CentralizedSamplingStrategyBenchmark.noSampleSamplingBenchmark       avgt      20     210.761 ±      0.270   ns/op
strategy.sampling.LocalizedSamplingStrategyBenchmark.defaultSamplingRuleBenchmark      avgt      20      72.751 ±      1.449   ns/op
strategy.sampling.LocalizedSamplingStrategyBenchmark.noSampleSamplingBenchmark         avgt      20     161.627 ±      0.069   ns/op
AWSXRayRecorderBenchmark.beginDummySegmentBenchmark                                  sample  253511    1055.618 ±     37.425   ns/op
AWSXRayRecorderBenchmark.beginEndDummySegmentBenchmark                               sample  344093    1718.683 ±     48.921   ns/op
AWSXRayRecorderBenchmark.beginEndDummySegmentSubsegmentBenchmark                     sample  209507    2831.574 ±     60.508   ns/op
AWSXRayRecorderBenchmark.beginEndSegmentBenchmark                                    sample  263710    9262.353 ±     62.869   ns/op
AWSXRayRecorderBenchmark.beginEndSegmentSubsegmentBenchmark                          sample  202903   12068.384 ±     72.265   ns/op
AWSXRayRecorderBenchmark.beginSegmentBenchmark                                       sample  324024    1741.153 ±     41.595   ns/op
AWSXRayRecorderBenchmark.beginSubsegmentBenchmark                                    sample  225716     911.156 ±     37.990   ns/op
AWSXRayRecorderBenchmark.beginSubsegmentDummyParentBenchmark                         sample  210748     941.469 ±     38.485   ns/op
AWSXRayRecorderBenchmark.endDummySegmentBenchmark                                    sample  236574     623.994 ±     20.051   ns/op
AWSXRayRecorderBenchmark.endSegmentBenchmark                                         sample  261241    7490.715 ±     51.209   ns/op
AWSXRayRecorderBenchmark.endSegmentNoChildBenchmark                                  sample  272114    7233.746 ±     50.534   ns/op
AWSXRayRecorderBenchmark.endSegmentWithChildBenchmark                                sample  206959    9171.063 ±     67.154   ns/op
AWSXRayRecorderBenchmark.endSubsegmentBenchmark                                      sample  200531     378.912 ±     13.123   ns/op
AWSXRayRecorderBenchmark.endSubsegmentDummyParentBenchmark                           sample  263603     365.118 ±     10.849   ns/op
AWSXRayRecorderBenchmark.getSegmentBenchmark                                         sample  212591     209.722 ±     18.189   ns/op
AWSXRayRecorderBenchmark.getSubsegmentBenchmark                                      sample  201929     191.968 ±      0.673   ns/op
entities.EntityBenchmark.constructSegmentBenchmark                                   sample  308810    1167.917 ±     55.308   ns/op
entities.EntityBenchmark.constructSubsegmentPutInSegmentBenchmark                    sample  388403     583.289 ±     22.434   ns/op
entities.EntityBenchmark.putAnnotationBenchmark                                      sample  287428      87.637 ±      7.405   ns/op
entities.EntityBenchmark.putExceptionSegmentBenchmark                                sample  343820     831.644 ±     29.341   ns/op
entities.EntityBenchmark.putMetadataBenchmark                                        sample  267907     164.430 ±      0.627   ns/op
entities.EntitySerializerBenchmark.serializeFourChildSegment                         sample  278945   10913.147 ±     54.193   ns/op
entities.EntitySerializerBenchmark.serializeFourGenerationSegment                    sample  334454   10367.976 ±     41.548   ns/op
entities.EntitySerializerBenchmark.serializeOneChildSegment                          sample  332754    5178.619 ±     24.284   ns/op
entities.EntitySerializerBenchmark.serializeThreeChildSegment                        sample  293438    9024.675 ±     40.063   ns/op
entities.EntitySerializerBenchmark.serializeThreeGenerationSegment                   sample  355035    8661.668 ±     36.189   ns/op
entities.EntitySerializerBenchmark.serializeTwoChildSegment                          sample  312395    7058.556 ±     32.718   ns/op
entities.EntitySerializerBenchmark.serializeTwoGenerationSegment                     sample  376709    6887.692 ±     31.219   ns/op
entities.EntitySerializerBenchmark.serializeZeroChildSegment                         sample  360486    3072.852 ±     29.020   ns/op
strategy.sampling.CentralizedSamplingStrategyBenchmark.defaultSamplingRuleBenchmark  sample  336401     149.325 ±      0.470   ns/op
strategy.sampling.CentralizedSamplingStrategyBenchmark.noSampleSamplingBenchmark     sample  359463     262.593 ±     15.510   ns/op
strategy.sampling.LocalizedSamplingStrategyBenchmark.defaultSamplingRuleBenchmark    sample  266014     110.714 ±      6.621   ns/op
strategy.sampling.LocalizedSamplingStrategyBenchmark.noSampleSamplingBenchmark       sample  222630     208.720 ±     12.643   ns/op
AWSXRayRecorderBenchmark.beginDummySegmentBenchmark                                      ss      20   84395.150 ±  18332.065   ns/op
AWSXRayRecorderBenchmark.beginEndDummySegmentBenchmark                                   ss      20  132328.650 ±  25262.608   ns/op
AWSXRayRecorderBenchmark.beginEndDummySegmentSubsegmentBenchmark                         ss      20  235990.650 ±  37859.562   ns/op
AWSXRayRecorderBenchmark.beginEndSegmentBenchmark                                        ss      20  439731.950 ± 476949.085   ns/op
AWSXRayRecorderBenchmark.beginEndSegmentSubsegmentBenchmark                              ss      20  406657.800 ±  31046.482   ns/op
AWSXRayRecorderBenchmark.beginSegmentBenchmark                                           ss      20  103035.400 ±  17920.897   ns/op
AWSXRayRecorderBenchmark.beginSubsegmentBenchmark                                        ss      20   55743.950 ±  14231.622   ns/op
AWSXRayRecorderBenchmark.beginSubsegmentDummyParentBenchmark                             ss      20   63175.900 ±  16048.124   ns/op
AWSXRayRecorderBenchmark.endDummySegmentBenchmark                                        ss      20   64224.400 ±   8068.344   ns/op
AWSXRayRecorderBenchmark.endSegmentBenchmark                                             ss      20  214629.500 ±  55811.204   ns/op
AWSXRayRecorderBenchmark.endSegmentNoChildBenchmark                                      ss      20  214615.600 ±  61501.096   ns/op
AWSXRayRecorderBenchmark.endSegmentWithChildBenchmark                                    ss      20  307790.150 ±  47987.596   ns/op
AWSXRayRecorderBenchmark.endSubsegmentBenchmark                                          ss      20   28885.500 ±   7862.089   ns/op
AWSXRayRecorderBenchmark.endSubsegmentDummyParentBenchmark                               ss      20   30340.850 ±   8560.596   ns/op
AWSXRayRecorderBenchmark.getSegmentBenchmark                                             ss      20   13515.550 ±   3236.533   ns/op
AWSXRayRecorderBenchmark.getSubsegmentBenchmark                                          ss      20   16732.500 ±   4408.320   ns/op
entities.EntityBenchmark.constructSegmentBenchmark                                       ss      20   39893.400 ±  10180.646   ns/op
entities.EntityBenchmark.constructSubsegmentPutInSegmentBenchmark                        ss      20   21437.250 ±   8907.737   ns/op
entities.EntityBenchmark.putAnnotationBenchmark                                          ss      20    2682.650 ±    545.990   ns/op
entities.EntityBenchmark.putExceptionSegmentBenchmark                                    ss      20   70976.150 ±  38090.008   ns/op
entities.EntityBenchmark.putMetadataBenchmark                                            ss      20    5444.800 ±   1039.818   ns/op
entities.EntitySerializerBenchmark.serializeFourChildSegment                             ss      20  202159.750 ±  40475.107   ns/op
entities.EntitySerializerBenchmark.serializeFourGenerationSegment                        ss      20  198535.500 ±  32971.593   ns/op
entities.EntitySerializerBenchmark.serializeOneChildSegment                              ss      20  181780.550 ±  55144.974   ns/op
entities.EntitySerializerBenchmark.serializeThreeChildSegment                            ss      20  234412.300 ±  46889.301   ns/op
entities.EntitySerializerBenchmark.serializeThreeGenerationSegment                       ss      20  261439.400 ±  41988.448   ns/op
entities.EntitySerializerBenchmark.serializeTwoChildSegment                              ss      20  170450.250 ±  22630.134   ns/op
entities.EntitySerializerBenchmark.serializeTwoGenerationSegment                         ss      20  186123.550 ±  38979.925   ns/op
entities.EntitySerializerBenchmark.serializeZeroChildSegment                             ss      20  129310.000 ±  25264.663   ns/op
strategy.sampling.CentralizedSamplingStrategyBenchmark.defaultSamplingRuleBenchmark      ss      20   28959.550 ±   2666.203   ns/op
strategy.sampling.CentralizedSamplingStrategyBenchmark.noSampleSamplingBenchmark         ss      20   34625.400 ±   6316.498   ns/op
strategy.sampling.LocalizedSamplingStrategyBenchmark.defaultSamplingRuleBenchmark        ss      20   11330.300 ±    451.807   ns/op
strategy.sampling.LocalizedSamplingStrategyBenchmark.noSampleSamplingBenchmark           ss      20   20679.850 ±   2234.130   ns/op
```

</p>
</details>

#### 2.5.x
<details><summary>Show</summary>
<p>

```
Benchmark                                                                              Mode     Cnt       Score        Error   Units
AWSXRayRecorderBenchmark.beginDummySegmentBenchmark                                   thrpt      20       0.001 ±      0.001  ops/ns
AWSXRayRecorderBenchmark.beginEndDummySegmentBenchmark                                thrpt      20       0.001 ±      0.001  ops/ns
AWSXRayRecorderBenchmark.beginEndDummySegmentSubsegmentBenchmark                      thrpt      20      ≈ 10⁻³               ops/ns
AWSXRayRecorderBenchmark.beginEndSegmentBenchmark                                     thrpt      20      ≈ 10⁻⁴               ops/ns
AWSXRayRecorderBenchmark.beginEndSegmentSubsegmentBenchmark                           thrpt      20      ≈ 10⁻⁴               ops/ns
AWSXRayRecorderBenchmark.beginSegmentBenchmark                                        thrpt      20       0.001 ±      0.001  ops/ns
AWSXRayRecorderBenchmark.beginSubsegmentBenchmark                                     thrpt      20       0.001 ±      0.001  ops/ns
AWSXRayRecorderBenchmark.beginSubsegmentDummyParentBenchmark                          thrpt      20       0.001 ±      0.001  ops/ns
AWSXRayRecorderBenchmark.endDummySegmentBenchmark                                     thrpt      20       0.002 ±      0.001  ops/ns
AWSXRayRecorderBenchmark.endSegmentBenchmark                                          thrpt      20      ≈ 10⁻⁴               ops/ns
AWSXRayRecorderBenchmark.endSegmentNoChildBenchmark                                   thrpt      20      ≈ 10⁻⁴               ops/ns
AWSXRayRecorderBenchmark.endSegmentWithChildBenchmark                                 thrpt      20      ≈ 10⁻⁴               ops/ns
AWSXRayRecorderBenchmark.endSubsegmentBenchmark                                       thrpt      20       0.002 ±      0.001  ops/ns
AWSXRayRecorderBenchmark.endSubsegmentDummyParentBenchmark                            thrpt      20       0.002 ±      0.001  ops/ns
AWSXRayRecorderBenchmark.getSegmentBenchmark                                          thrpt      20       0.005 ±      0.001  ops/ns
AWSXRayRecorderBenchmark.getSubsegmentBenchmark                                       thrpt      20       0.005 ±      0.001  ops/ns
entities.EntityBenchmark.constructSegmentBenchmark                                    thrpt      20       0.001 ±      0.001  ops/ns
entities.EntityBenchmark.constructSubsegmentPutInSegmentBenchmark                     thrpt      20       0.002 ±      0.001  ops/ns
entities.EntityBenchmark.putAnnotationBenchmark                                       thrpt      20       0.012 ±      0.001  ops/ns
entities.EntityBenchmark.putExceptionSegmentBenchmark                                 thrpt      20       0.001 ±      0.001  ops/ns
entities.EntityBenchmark.putMetadataBenchmark                                         thrpt      20       0.006 ±      0.001  ops/ns
entities.EntitySerializerBenchmark.serializeFourChildSegment                          thrpt      20      ≈ 10⁻⁴               ops/ns
entities.EntitySerializerBenchmark.serializeFourGenerationSegment                     thrpt      20      ≈ 10⁻⁴               ops/ns
entities.EntitySerializerBenchmark.serializeOneChildSegment                           thrpt      20      ≈ 10⁻⁴               ops/ns
entities.EntitySerializerBenchmark.serializeThreeChildSegment                         thrpt      20      ≈ 10⁻⁴               ops/ns
entities.EntitySerializerBenchmark.serializeThreeGenerationSegment                    thrpt      20      ≈ 10⁻⁴               ops/ns
entities.EntitySerializerBenchmark.serializeTwoChildSegment                           thrpt      20      ≈ 10⁻⁴               ops/ns
entities.EntitySerializerBenchmark.serializeTwoGenerationSegment                      thrpt      20      ≈ 10⁻⁴               ops/ns
entities.EntitySerializerBenchmark.serializeZeroChildSegment                          thrpt      20      ≈ 10⁻³               ops/ns
strategy.sampling.CentralizedSamplingStrategyBenchmark.defaultSamplingRuleBenchmark   thrpt      20       0.009 ±      0.001  ops/ns
strategy.sampling.CentralizedSamplingStrategyBenchmark.noSampleSamplingBenchmark      thrpt      20       0.005 ±      0.001  ops/ns
strategy.sampling.LocalizedSamplingStrategyBenchmark.defaultSamplingRuleBenchmark     thrpt      20       0.015 ±      0.001  ops/ns
strategy.sampling.LocalizedSamplingStrategyBenchmark.noSampleSamplingBenchmark        thrpt      20       0.006 ±      0.001  ops/ns
AWSXRayRecorderBenchmark.beginDummySegmentBenchmark                                    avgt      20    1071.637 ±      1.617   ns/op
AWSXRayRecorderBenchmark.beginEndDummySegmentBenchmark                                 avgt      20    1686.724 ±      2.438   ns/op
AWSXRayRecorderBenchmark.beginEndDummySegmentSubsegmentBenchmark                       avgt      20    3027.161 ±      6.005   ns/op
AWSXRayRecorderBenchmark.beginEndSegmentBenchmark                                      avgt      20   10037.840 ±     18.514   ns/op
AWSXRayRecorderBenchmark.beginEndSegmentSubsegmentBenchmark                            avgt      20   13575.728 ±    616.925   ns/op
AWSXRayRecorderBenchmark.beginSegmentBenchmark                                         avgt      20    1761.923 ±      2.073   ns/op
AWSXRayRecorderBenchmark.beginSubsegmentBenchmark                                      avgt      20     977.928 ±      4.826   ns/op
AWSXRayRecorderBenchmark.beginSubsegmentDummyParentBenchmark                           avgt      20    1002.999 ±      4.506   ns/op
AWSXRayRecorderBenchmark.endDummySegmentBenchmark                                      avgt      20     649.504 ±      2.929   ns/op
AWSXRayRecorderBenchmark.endSegmentBenchmark                                           avgt      20    7950.378 ±     29.176   ns/op
AWSXRayRecorderBenchmark.endSegmentNoChildBenchmark                                    avgt      20    7927.543 ±     20.633   ns/op
AWSXRayRecorderBenchmark.endSegmentWithChildBenchmark                                  avgt      20   10343.499 ±     55.806   ns/op
AWSXRayRecorderBenchmark.endSubsegmentBenchmark                                        avgt      20     492.227 ±      5.142   ns/op
AWSXRayRecorderBenchmark.endSubsegmentDummyParentBenchmark                             avgt      20     495.746 ±      3.289   ns/op
AWSXRayRecorderBenchmark.getSegmentBenchmark                                           avgt      20     218.829 ±      2.388   ns/op
AWSXRayRecorderBenchmark.getSubsegmentBenchmark                                        avgt      20     184.251 ±      2.545   ns/op
entities.EntityBenchmark.constructSegmentBenchmark                                     avgt      20    1010.009 ±      3.297   ns/op
entities.EntityBenchmark.constructSubsegmentPutInSegmentBenchmark                      avgt      20     580.289 ±      2.324   ns/op
entities.EntityBenchmark.putAnnotationBenchmark                                        avgt      20      85.209 ±      0.780   ns/op
entities.EntityBenchmark.putExceptionSegmentBenchmark                                  avgt      20     793.587 ±      1.948   ns/op
entities.EntityBenchmark.putMetadataBenchmark                                          avgt      20     162.526 ±      1.059   ns/op
entities.EntitySerializerBenchmark.serializeFourChildSegment                           avgt      20   11009.343 ±     34.616   ns/op
entities.EntitySerializerBenchmark.serializeFourGenerationSegment                      avgt      20   10449.485 ±     41.528   ns/op
entities.EntitySerializerBenchmark.serializeOneChildSegment                            avgt      20    5266.160 ±     22.793   ns/op
entities.EntitySerializerBenchmark.serializeThreeChildSegment                          avgt      20    9168.361 ±     32.335   ns/op
entities.EntitySerializerBenchmark.serializeThreeGenerationSegment                     avgt      20    8765.553 ±     23.144   ns/op
entities.EntitySerializerBenchmark.serializeTwoChildSegment                            avgt      20    7101.882 ±     30.996   ns/op
entities.EntitySerializerBenchmark.serializeTwoGenerationSegment                       avgt      20    6955.711 ±     31.126   ns/op
entities.EntitySerializerBenchmark.serializeZeroChildSegment                           avgt      20    3108.918 ±     26.333   ns/op
strategy.sampling.CentralizedSamplingStrategyBenchmark.defaultSamplingRuleBenchmark    avgt      20     112.428 ±      0.182   ns/op
strategy.sampling.CentralizedSamplingStrategyBenchmark.noSampleSamplingBenchmark       avgt      20     211.965 ±      0.123   ns/op
strategy.sampling.LocalizedSamplingStrategyBenchmark.defaultSamplingRuleBenchmark      avgt      20      67.493 ±      0.108   ns/op
strategy.sampling.LocalizedSamplingStrategyBenchmark.noSampleSamplingBenchmark         avgt      20     166.294 ±      0.074   ns/op
AWSXRayRecorderBenchmark.beginDummySegmentBenchmark                                  sample  242852    1015.217 ±     25.865   ns/op
AWSXRayRecorderBenchmark.beginEndDummySegmentBenchmark                               sample  336762    1637.734 ±     28.429   ns/op
AWSXRayRecorderBenchmark.beginEndDummySegmentSubsegmentBenchmark                     sample  386427    3010.789 ±     38.928   ns/op
AWSXRayRecorderBenchmark.beginEndSegmentBenchmark                                    sample  244055   10001.042 ±     68.036   ns/op
AWSXRayRecorderBenchmark.beginEndSegmentSubsegmentBenchmark                          sample  368555   13283.377 ±     61.617   ns/op
AWSXRayRecorderBenchmark.beginSegmentBenchmark                                       sample  314668    1726.402 ±     37.356   ns/op
AWSXRayRecorderBenchmark.beginSubsegmentBenchmark                                    sample  210346     953.963 ±     24.556   ns/op
AWSXRayRecorderBenchmark.beginSubsegmentDummyParentBenchmark                         sample  380740    1040.476 ±     26.411   ns/op
AWSXRayRecorderBenchmark.endDummySegmentBenchmark                                    sample  224249     649.915 ±     17.886   ns/op
AWSXRayRecorderBenchmark.endSegmentBenchmark                                         sample  244845    8142.884 ±     45.146   ns/op
AWSXRayRecorderBenchmark.endSegmentNoChildBenchmark                                  sample  250899    7915.172 ±     47.638   ns/op
AWSXRayRecorderBenchmark.endSegmentWithChildBenchmark                                sample  376766   10086.163 ±     40.600   ns/op
AWSXRayRecorderBenchmark.endSubsegmentBenchmark                                      sample  348544     552.085 ±     18.241   ns/op
AWSXRayRecorderBenchmark.endSubsegmentDummyParentBenchmark                           sample  233977     500.227 ±     22.688   ns/op
AWSXRayRecorderBenchmark.getSegmentBenchmark                                         sample  387181     216.369 ±      8.575   ns/op
AWSXRayRecorderBenchmark.getSubsegmentBenchmark                                      sample  197549     183.280 ±     17.504   ns/op
entities.EntityBenchmark.constructSegmentBenchmark                                   sample  312466    1043.138 ±     43.046   ns/op
entities.EntityBenchmark.constructSubsegmentPutInSegmentBenchmark                    sample  197733     541.791 ±     22.920   ns/op
entities.EntityBenchmark.putAnnotationBenchmark                                      sample  286457      88.174 ±      8.075   ns/op
entities.EntityBenchmark.putExceptionSegmentBenchmark                                sample  335182     793.091 ±      7.267   ns/op
entities.EntityBenchmark.putMetadataBenchmark                                        sample  266331     163.341 ±      9.606   ns/op
entities.EntitySerializerBenchmark.serializeFourChildSegment                         sample  275182   11135.491 ±     34.807   ns/op
entities.EntitySerializerBenchmark.serializeFourGenerationSegment                    sample  329707   10591.299 ±     45.417   ns/op
entities.EntitySerializerBenchmark.serializeOneChildSegment                          sample  337107    5047.245 ±     34.059   ns/op
entities.EntitySerializerBenchmark.serializeThreeChildSegment                        sample  294936    9187.052 ±     47.474   ns/op
entities.EntitySerializerBenchmark.serializeThreeGenerationSegment                   sample  354335    8765.936 ±     38.804   ns/op
entities.EntitySerializerBenchmark.serializeTwoChildSegment                          sample  310739    7156.495 ±     33.745   ns/op
entities.EntitySerializerBenchmark.serializeTwoGenerationSegment                     sample  377110    7105.078 ±     35.192   ns/op
entities.EntitySerializerBenchmark.serializeZeroChildSegment                         sample  359107    3168.870 ±     25.759   ns/op
strategy.sampling.CentralizedSamplingStrategyBenchmark.defaultSamplingRuleBenchmark  sample  335996     154.190 ±      0.403   ns/op
strategy.sampling.CentralizedSamplingStrategyBenchmark.noSampleSamplingBenchmark     sample  363307     253.887 ±     10.525   ns/op
strategy.sampling.LocalizedSamplingStrategyBenchmark.defaultSamplingRuleBenchmark    sample  248667     112.538 ±      0.356   ns/op
strategy.sampling.LocalizedSamplingStrategyBenchmark.noSampleSamplingBenchmark       sample  226218     206.876 ±      0.707   ns/op
AWSXRayRecorderBenchmark.beginDummySegmentBenchmark                                      ss      20   89350.150 ±  13597.430   ns/op
AWSXRayRecorderBenchmark.beginEndDummySegmentBenchmark                                   ss      20  133595.150 ±  28575.175   ns/op
AWSXRayRecorderBenchmark.beginEndDummySegmentSubsegmentBenchmark                         ss      20  215337.450 ±  30660.002   ns/op
AWSXRayRecorderBenchmark.beginEndSegmentBenchmark                                        ss      20  310199.000 ±  65315.322   ns/op
AWSXRayRecorderBenchmark.beginEndSegmentSubsegmentBenchmark                              ss      20  706148.500 ± 555291.570   ns/op
AWSXRayRecorderBenchmark.beginSegmentBenchmark                                           ss      20  109194.150 ±  16547.259   ns/op
AWSXRayRecorderBenchmark.beginSubsegmentBenchmark                                        ss      20   74353.650 ±  16585.893   ns/op
AWSXRayRecorderBenchmark.beginSubsegmentDummyParentBenchmark                             ss      20   56546.800 ±   9855.371   ns/op
AWSXRayRecorderBenchmark.endDummySegmentBenchmark                                        ss      20   63401.700 ±  31986.633   ns/op
AWSXRayRecorderBenchmark.endSegmentBenchmark                                             ss      20  230165.050 ±  95629.454   ns/op
AWSXRayRecorderBenchmark.endSegmentNoChildBenchmark                                      ss      20  211758.250 ±  61098.566   ns/op
AWSXRayRecorderBenchmark.endSegmentWithChildBenchmark                                    ss      20  381676.550 ±  35814.059   ns/op
AWSXRayRecorderBenchmark.endSubsegmentBenchmark                                          ss      20   46724.500 ±  16863.497   ns/op
AWSXRayRecorderBenchmark.endSubsegmentDummyParentBenchmark                               ss      20   47818.950 ±  21172.022   ns/op
AWSXRayRecorderBenchmark.getSegmentBenchmark                                             ss      20   14838.200 ±   2342.925   ns/op
AWSXRayRecorderBenchmark.getSubsegmentBenchmark                                          ss      20   11440.950 ±   1306.591   ns/op
entities.EntityBenchmark.constructSegmentBenchmark                                       ss      20   35744.300 ±   4598.287   ns/op
entities.EntityBenchmark.constructSubsegmentPutInSegmentBenchmark                        ss      20   21751.400 ±   9986.561   ns/op
entities.EntityBenchmark.putAnnotationBenchmark                                          ss      20    2336.050 ±    823.172   ns/op
entities.EntityBenchmark.putExceptionSegmentBenchmark                                    ss      20   65748.150 ±  16070.384   ns/op
entities.EntityBenchmark.putMetadataBenchmark                                            ss      20    5710.450 ±   1003.484   ns/op
entities.EntitySerializerBenchmark.serializeFourChildSegment                             ss      20  234470.100 ±  49966.050   ns/op
entities.EntitySerializerBenchmark.serializeFourGenerationSegment                        ss      20  261763.900 ±  55917.040   ns/op
entities.EntitySerializerBenchmark.serializeOneChildSegment                              ss      20  161227.250 ±  26976.029   ns/op
entities.EntitySerializerBenchmark.serializeThreeChildSegment                            ss      20  199603.850 ±  38806.758   ns/op
entities.EntitySerializerBenchmark.serializeThreeGenerationSegment                       ss      20  192289.600 ±  43403.362   ns/op
entities.EntitySerializerBenchmark.serializeTwoChildSegment                              ss      20  177638.300 ±  32304.742   ns/op
entities.EntitySerializerBenchmark.serializeTwoGenerationSegment                         ss      20  219824.150 ±  40251.217   ns/op
entities.EntitySerializerBenchmark.serializeZeroChildSegment                             ss      20  145212.050 ±  32210.350   ns/op
strategy.sampling.CentralizedSamplingStrategyBenchmark.defaultSamplingRuleBenchmark      ss      20   21789.650 ±   3390.080   ns/op
strategy.sampling.CentralizedSamplingStrategyBenchmark.noSampleSamplingBenchmark         ss      20   38342.050 ±   3327.868   ns/op
strategy.sampling.LocalizedSamplingStrategyBenchmark.defaultSamplingRuleBenchmark        ss      20   21527.100 ±   2332.975   ns/op
strategy.sampling.LocalizedSamplingStrategyBenchmark.noSampleSamplingBenchmark           ss      20   28448.850 ±   2261.665   ns/op
```

</p>
</details>

### Score Comparison By Version

#### Benchmark Mode: Average Time
<details><summary>Show</summary>
<p>

<table>
<thead>
<tr>
<th>Test Name</th>
<th>Version 1.3.x</th>
<th>Version 2.0.x</th>
<th>Version 2.2.x</th>
<th>Version 2.4.x</th>
<th>Version 2.5.x</th>
</tr>
</thead>
<tbody>

<tr>
<td>AWSXRayRecorderBenchmark.beginDummySegmentBenchmark</td>
<td> 930.006 ± 1.430 ns/op</td>
<td> 932.213 ± 1.533 ns/op</td>
<td> 1116.352 ± 65.138 ns/op</td>
<td> 1041.499 ± 1.949 ns/op</td>
<td> 1071.637 ±(99.9%) 1.617 ns/op</td>
</tr>
<tr>
<td>AWSXRayRecorderBenchmark.beginEndDummySegmentBenchmark</td>
<td> 1338.062 ± 36.102 ns/op</td>
<td> 1375.375 ± 141.220 ns/op</td>
<td> 1384.115 ± 116.348 ns/op</td>
<td> 1565.383 ± 2.028 ns/op</td>
<td> 1686.724 ±(99.9%) 2.438 ns/op</td>
</tr>
<tr>
<td>AWSXRayRecorderBenchmark.beginEndDummySegmentSubsegmentBenchmark</td>
<td> 2494.100 ± 1.864 ns/op</td>
<td> 2478.136 ± 2.670 ns/op</td>
<td> 2548.739 ± 17.057 ns/op</td>
<td> 2770.433 ± 5.588 ns/op</td>
<td> 3027.161 ±(99.9%) 6.005 ns/op</td>
</tr>
<tr>
<td>AWSXRayRecorderBenchmark.beginEndSegmentBenchmark</td>
<td> 9020.679 ± 25.179 ns/op</td>
<td> 9127.754 ± 19.352 ns/op</td>
<td> 9229.839 ± 21.222 ns/op</td>
<td> 9123.404 ± 16.630 ns/op</td>
<td> 10037.840 ±(99.9%) 18.514 ns/op</td>
</tr>
<tr>
<td>AWSXRayRecorderBenchmark.beginEndSegmentSubsegmentBenchmark</td>
<td> 12284.696 ± 505.733 ns/op</td>
<td> 12257.246 ± 337.792 ns/op</td>
<td> 12106.073 ± 258.082 ns/op</td>
<td> 12094.797 ± 43.192 ns/op</td>
<td> 13575.728 ±(99.9%) 616.925 ns/op</td>
</tr>
<tr>
<td>AWSXRayRecorderBenchmark.beginSegmentBenchmark</td>
<td> 1649.248 ± 2.289 ns/op</td>
<td> 1647.842 ± 2.535 ns/op</td>
<td> 1636.812 ± 2.025 ns/op</td>
<td> 1715.721 ± 2.687 ns/op</td>
<td> 1761.923 ±(99.9%) 2.073 ns/op</td>
</tr>
<tr>
<td>AWSXRayRecorderBenchmark.beginSubsegmentBenchmark</td>
<td> 866.134 ± 3.796 ns/op</td>
<td> 866.843 ± 3.770 ns/op</td>
<td> 889.173 ± 2.707 ns/op</td>
<td> 950.378 ± 4.452 ns/op</td>
<td> 977.928 ±(99.9%) 4.826 ns/op</td>
</tr>
<tr>
<td>AWSXRayRecorderBenchmark.beginSubsegmentDummyParentBenchmark</td>
<td> 928.351 ± 57.592 ns/op</td>
<td> 925.648 ± 57.932 ns/op</td>
<td> 884.073 ± 17.982 ns/op</td>
<td> 905.941 ± 3.186 ns/op</td>
<td> 1002.999 ±(99.9%) 4.506 ns/op</td>
</tr>
<tr>
<td>AWSXRayRecorderBenchmark.endDummySegmentBenchmark</td>
<td> 448.467 ± 5.861 ns/op</td>
<td> 444.140 ± 3.100 ns/op</td>
<td> 445.395 ± 3.020 ns/op</td>
<td> 596.117 ± 4.522 ns/op</td>
<td> 649.504 ±(99.9%) 2.929 ns/op</td>
</tr>
<tr>
<td>AWSXRayRecorderBenchmark.endSegmentBenchmark</td>
<td> 7478.520 ± 26.655 ns/op</td>
<td> 7442.642 ± 14.195 ns/op</td>
<td> 7399.554 ± 19.442 ns/op</td>
<td> 7839.399 ± 43.860 ns/op</td>
<td> 7950.378 ±(99.9%) 29.176 ns/op</td>
</tr>
<tr>
<td>AWSXRayRecorderBenchmark.endSegmentNoChildBenchmark</td>
<td> 7343.836 ± 230.851 ns/op</td>
<td> 7473.374 ± 274.008 ns/op</td>
<td> 7480.519 ± 341.880 ns/op</td>
<td> 7578.632 ± 25.965 ns/op</td>
<td> 7927.543 ±(99.9%) 20.633 ns/op</td>
</tr>
<tr>
<td>AWSXRayRecorderBenchmark.endSegmentWithChildBenchmark</td>
<td> 9202.114 ± 267.064 ns/op</td>
<td> 9311.713 ± 31.022 ns/op</td>
<td> 9194.198 ± 15.454 ns/op</td>
<td> 9392.873 ± 33.571 ns/op</td>
<td> 10343.499 ±(99.9%) 55.806 ns/op</td>
</tr>
<tr>
<td>AWSXRayRecorderBenchmark.endSubsegmentBenchmark</td>
<td> 385.743 ± 2.821 ns/op</td>
<td> 352.493 ± 1.498 ns/op</td>
<td> 349.199 ± 2.644 ns/op</td>
<td> 426.620 ± 2.629 ns/op</td>
<td> 492.227 ±(99.9%) 5.142 ns/op</td>
</tr>
<tr>
<td>AWSXRayRecorderBenchmark.endSubsegmentDummyParentBenchmark</td>
<td> 336.228 ± 2.557 ns/op</td>
<td> 345.812 ± 26.341 ns/op</td>
<td> 359.009 ± 86.696 ns/op</td>
<td> 358.871 ± 2.002 ns/op</td>
<td> 495.746 ±(99.9%) 3.289 ns/op</td>
</tr>
<tr>
<td>AWSXRayRecorderBenchmark.getSegmentBenchmark</td>
<td> 209.839 ± 10.374 ns/op</td>
<td> 197.065 ± 1.262 ns/op</td>
<td> 200.958 ± 2.204 ns/op</td>
<td> 203.888 ± 2.078 ns/op</td>
<td> 218.829 ±(99.9%) 2.388 ns/op</td>
</tr>
<tr>
<td>AWSXRayRecorderBenchmark.getSubsegmentBenchmark</td>
<td> 173.015 ± 2.319 ns/op</td>
<td> 175.089 ± 3.163 ns/op</td>
<td> 173.120 ± 1.907 ns/op</td>
<td> 178.235 ± 1.341 ns/op</td>
<td> 184.251 ±(99.9%) 2.545 ns/op</td>
</tr>
<tr>
<td>entities.EntityBenchmark.constructSegmentBenchmark</td>
<td> 978.648 ± 3.556 ns/op</td>
<td> 1120.922 ± 177.050 ns/op</td>
<td> 1035.187 ± 23.819 ns/op</td>
<td> 1017.563 ± 2.903 ns/op</td>
<td> 1010.009 ±(99.9%) 3.297 ns/op</td>
</tr>
<tr>
<td>entities.EntityBenchmark.constructSubsegmentPutInSegmentBenchmark</td>
<td> 613.445 ± 132.987 ns/op</td>
<td> 571.394 ± 2.378 ns/op</td>
<td> 571.755 ± 2.414 ns/op</td>
<td> 571.089 ± 2.446 ns/op</td>
<td> 580.289 ±(99.9%) 2.324 ns/op</td>
</tr>
<tr>
<td>entities.EntityBenchmark.putAnnotationBenchmark</td>
<td> 84.141 ± 2.014 ns/op</td>
<td> 79.732 ± 0.670 ns/op</td>
<td> 80.885 ± 0.962 ns/op</td>
<td> 86.213 ± 0.622 ns/op</td>
<td> 85.209 ±(99.9%) 0.780 ns/op</td>
</tr>
<tr>
<td>entities.EntityBenchmark.putExceptionSegmentBenchmark</td>
<td> 800.592 ± 2.352 ns/op</td>
<td> 787.575 ± 18.291 ns/op</td>
<td> 835.846 ± 39.917 ns/op</td>
<td> 804.234 ± 4.239 ns/op</td>
<td> 793.587 ±(99.9%) 1.948 ns/op</td>
</tr>
<tr>
<td>entities.EntityBenchmark.putMetadataBenchmark</td>
<td> 160.012 ± 1.629 ns/op</td>
<td> 156.425 ± 1.451 ns/op</td>
<td> 155.620 ± 1.347 ns/op</td>
<td> 165.585 ± 1.084 ns/op</td>
<td> 162.526 ±(99.9%) 1.059 ns/op</td>
</tr>
<tr>
<td>entities.EntitySerializerBenchmark.serializeFourChildSegment</td>
<td> 10454.440 ± 33.857 ns/op</td>
<td> 10516.215 ± 45.882 ns/op</td>
<td> 10602.465 ± 39.805 ns/op</td>
<td> 10956.216 ± 38.724 ns/op</td>
<td> 11009.343 ±(99.9%) 34.616 ns/op</td>
</tr>
<tr>
<td>entities.EntitySerializerBenchmark.serializeFourGenerationSegment</td>
<td> 10060.976 ± 45.191 ns/op</td>
<td> 10207.415 ± 39.337 ns/op</td>
<td> 10283.689 ± 457.224 ns/op</td>
<td> 10655.412 ± 31.139 ns/op</td>
<td> 10449.485 ±(99.9%) 41.528 ns/op</td>
</tr>
<tr>
<td>entities.EntitySerializerBenchmark.serializeOneChildSegment</td>
<td> 5155.852 ± 265.627 ns/op</td>
<td> 5543.381 ± 1486.911 ns/op</td>
<td> 5115.504 ± 27.507 ns/op</td>
<td> 5226.236 ± 28.453 ns/op</td>
<td> 5266.160 ±(99.9%) 22.793 ns/op</td>
</tr>
<tr>
<td>entities.EntitySerializerBenchmark.serializeThreeChildSegment</td>
<td> 8791.764 ± 189.374 ns/op</td>
<td> 8764.877 ± 94.731 ns/op</td>
<td> 8688.281 ± 42.657 ns/op</td>
<td> 8907.718 ± 36.280 ns/op</td>
<td> 9168.361 ±(99.9%) 32.335 ns/op</td>
</tr>
<tr>
<td>entities.EntitySerializerBenchmark.serializeThreeGenerationSegment</td>
<td> 8497.261 ± 50.650 ns/op</td>
<td> 8429.500 ± 27.255 ns/op</td>
<td> 8471.203 ± 312.402 ns/op</td>
<td> 8627.488 ± 34.715 ns/op</td>
<td> 8765.553 ±(99.9%) 23.144 ns/op</td>
</tr>
<tr>
<td>entities.EntitySerializerBenchmark.serializeTwoChildSegment</td>
<td> 7174.814 ± 529.818 ns/op</td>
<td> 6676.010 ± 128.959 ns/op</td>
<td> 6798.185 ± 28.915 ns/op</td>
<td> 7090.400 ± 31.906 ns/op</td>
<td> 7101.882 ±(99.9%) 30.996 ns/op</td>
</tr>
<tr>
<td>entities.EntitySerializerBenchmark.serializeTwoGenerationSegment</td>
<td> 6585.686 ± 26.487 ns/op</td>
<td> 6624.364 ± 35.145 ns/op</td>
<td> 6628.881 ± 25.755 ns/op</td>
<td> 6805.178 ± 14.657 ns/op</td>
<td> 6955.711 ±(99.9%) 31.126 ns/op</td>
</tr>
<tr>
<td>entities.EntitySerializerBenchmark.serializeZeroChildSegment</td>
<td> 3034.740 ± 20.371 ns/op</td>
<td> 3025.951 ± 19.453 ns/op</td>
<td> 3055.384 ± 103.331 ns/op</td>
<td> 3023.165 ± 22.132 ns/op</td>
<td> 3108.918 ±(99.9%) 26.333 ns/op</td>
</tr>
<tr>
<td>strategy.sampling.CentralizedSamplingStrategyBenchmark.defaultSamplingRuleBenchmark</td>
<td> </td>
<td> 823736.245 ± 324446.993 ns/op</td>
<td> 113.674 ± 5.104 ns/op</td>
<td> 111.288 ± 0.114 ns/op</td>
<td> 112.428 ±(99.9%) 0.182 ns/op</td>
</tr>
<tr>
<td>strategy.sampling.CentralizedSamplingStrategyBenchmark.noSampleSamplingBenchmark</td>
<td> </td>
<td> 791788.528 ± 341848.672 ns/op</td>
<td> 207.037 ± 6.451 ns/op</td>
<td> 210.761 ± 0.270 ns/op</td>
<td> 211.965 ±(99.9%) 0.123 ns/op</td>
</tr>
<tr>
<td>strategy.sampling.LocalizedSamplingStrategyBenchmark.defaultSamplingRuleBenchmark</td>
<td> 71.837 ± 3.236 ns/op</td>
<td> 66.956 ± 0.197 ns/op</td>
<td> 66.541 ± 0.093 ns/op</td>
<td> 72.751 ± 1.449 ns/op</td>
<td> 67.493 ±(99.9%) 0.108 ns/op</td>
</tr>
<tr>
<td>strategy.sampling.LocalizedSamplingStrategyBenchmark.noSampleSamplingBenchmark</td>
<td> 98.047 ± 0.179 ns/op</td>
<td> 148.325 ± 10.031 ns/op</td>
<td> 153.962 ± 5.669 ns/op</td>
<td> 161.627 ± 0.069 ns/op</td>
<td> 166.294 ±(99.9%) 0.074 ns/op</td>
</tr>
</tbody>
</table>
</p>
</details>

#### Benchmark Mode: Sample
<details><summary>Show</summary>
<p>

<table>
<thead>
<tr>
<th>Test Name</th>
<th>Version 1.3.x</th>
<th>Version 2.0.x</th>
<th>Version 2.2.x</th>
<th>Version 2.4.x</th>
<th>Version 2.5.x</th>
</tr>
</thead>
<tbody>

<tr>

<td>AWSXRayRecorderBenchmark.beginDummySegmentBenchmark</td>
<td> 1007.384 ± 37.568 ns/op</td>
<td> 951.650 ± 37.475 ns/op</td>
<td> 931.994 ± 33.393 ns/op</td>
<td> 1055.618 ± 37.425 ns/op</td>
<td> 1015.217 ±(99.9%) 25.865 ns/op</td>
</tr>
<tr>

<td>AWSXRayRecorderBenchmark.beginEndDummySegmentBenchmark</td>
<td> 1428.892 ± 54.666 ns/op</td>
<td> 1381.797 ± 43.789 ns/op</td>
<td> 1503.007 ± 68.511 ns/op</td>
<td> 1718.683 ± 48.921 ns/op</td>
<td> 1637.734 ±(99.9%) 28.429 ns/op</td>
</tr>
<tr>

<td>AWSXRayRecorderBenchmark.beginEndDummySegmentSubsegmentBenchmark</td>
<td> 2412.564 ± 48.628 ns/op</td>
<td> 2362.696 ± 42.796 ns/op</td>
<td> 2582.186 ± 60.289 ns/op</td>
<td> 2831.574 ± 60.508 ns/op</td>
<td> 3010.789 ±(99.9%) 38.928 ns/op</td>
</tr>
<tr>

<td>AWSXRayRecorderBenchmark.beginEndSegmentBenchmark</td>
<td> 9035.149 ± 66.519 ns/op</td>
<td> 9091.674 ± 66.711 ns/op</td>
<td> 9391.118 ± 74.131 ns/op</td>
<td> 9262.353 ± 62.869 ns/op</td>
<td> 10001.042 ±(99.9%) 68.036 ns/op</td>
</tr>
<tr>

<td>AWSXRayRecorderBenchmark.beginEndSegmentSubsegmentBenchmark</td>
<td> 12075.350 ± 85.069 ns/op</td>
<td> 11910.439 ± 90.948 ns/op</td>
<td> 11962.424 ± 96.458 ns/op</td>
<td> 12068.384 ± 72.265 ns/op</td>
<td> 13283.377 ±(99.9%) 61.617 ns/op</td>
</tr>
<tr>

<td>AWSXRayRecorderBenchmark.beginSegmentBenchmark</td>
<td> 1615.833 ± 32.173 ns/op</td>
<td> 1728.847 ± 50.414 ns/op</td>
<td> 1808.904 ± 54.412 ns/op</td>
<td> 1741.153 ± 41.595 ns/op</td>
<td> 1726.402 ±(99.9%) 37.356 ns/op</td>
</tr>
<tr>

<td>AWSXRayRecorderBenchmark.beginSubsegmentBenchmark</td>
<td> 892.790 ± 28.473 ns/op</td>
<td> 980.078 ± 48.343 ns/op</td>
<td> 962.641 ± 49.752 ns/op</td>
<td> 911.156 ± 37.990 ns/op</td>
<td> 953.963 ±(99.9%) 24.556 ns/op</td>
</tr>
<tr>

<td>AWSXRayRecorderBenchmark.beginSubsegmentDummyParentBenchmark</td>
<td> 974.702 ± 44.365 ns/op</td>
<td> 923.680 ± 43.547 ns/op</td>
<td> 873.949 ± 18.823 ns/op</td>
<td> 941.469 ± 38.485 ns/op</td>
<td> 1040.476 ±(99.9%) 26.411 ns/op</td>
</tr>
<tr>

<td>AWSXRayRecorderBenchmark.endDummySegmentBenchmark</td>
<td> 453.411 ± 13.522 ns/op</td>
<td> 521.729 ± 22.663 ns/op</td>
<td> 526.263 ± 133.149 ns/op</td>
<td> 623.994 ± 20.051 ns/op</td>
<td> 649.915 ±(99.9%) 17.886 ns/op</td>
</tr>
<tr>

<td>AWSXRayRecorderBenchmark.endSegmentBenchmark</td>
<td> 7414.041 ± 50.324 ns/op</td>
<td> 7336.683 ± 56.541 ns/op</td>
<td> 7533.645 ± 57.424 ns/op</td>
<td> 7490.715 ± 51.209 ns/op</td>
<td> 8142.884 ±(99.9%) 45.146 ns/op</td>
</tr>
<tr>

<td>AWSXRayRecorderBenchmark.endSegmentNoChildBenchmark</td>
<td> 7502.169 ± 48.535 ns/op</td>
<td> 7330.511 ± 52.730 ns/op</td>
<td> 7411.349 ± 47.386 ns/op</td>
<td> 7233.746 ± 50.534 ns/op</td>
<td> 7915.172 ±(99.9%) 47.638 ns/op</td>
</tr>
<tr>

<td>AWSXRayRecorderBenchmark.endSegmentWithChildBenchmark</td>
<td> 9390.585 ± 70.031 ns/op</td>
<td> 9423.119 ± 62.879 ns/op</td>
<td> 9252.474 ± 69.567 ns/op</td>
<td> 9171.063 ± 67.154 ns/op</td>
<td> 10086.163 ±(99.9%) 40.600 ns/op</td>
</tr>
<tr>

<td>AWSXRayRecorderBenchmark.endSubsegmentBenchmark</td>
<td> 372.220 ± 13.495 ns/op</td>
<td> 371.572 ± 10.796 ns/op</td>
<td> 377.350 ± 19.777 ns/op</td>
<td> 378.912 ± 13.123 ns/op</td>
<td> 552.085 ±(99.9%) 18.241 ns/op</td>
</tr>
<tr>

<td>AWSXRayRecorderBenchmark.endSubsegmentDummyParentBenchmark</td>
<td> 370.760 ± 7.143 ns/op</td>
<td> 370.156 ± 0.708 ns/op</td>
<td> 357.153 ± 9.942 ns/op</td>
<td> 365.118 ± 10.849 ns/op</td>
<td> 500.227 ±(99.9%) 22.688 ns/op</td>
</tr>
<tr>

<td>AWSXRayRecorderBenchmark.getSegmentBenchmark</td>
<td> 193.887 ± 0.556 ns/op</td>
<td> 199.410 ± 0.661 ns/op</td>
<td> 204.551 ± 23.926 ns/op</td>
<td> 209.722 ± 18.189 ns/op</td>
<td> 216.369 ±(99.9%) 8.575 ns/op</td>
</tr>
<tr>

<td>AWSXRayRecorderBenchmark.getSubsegmentBenchmark</td>
<td> 171.070 ± 0.499 ns/op</td>
<td> 176.346 ± 10.327 ns/op</td>
<td> 169.448 ± 0.306 ns/op</td>
<td> 191.968 ± 0.673 ns/op</td>
<td> 183.280 ±(99.9%) 17.504 ns/op</td>
</tr>
<tr>

<td>entities.EntityBenchmark.constructSegmentBenchmark</td>
<td> 930.151 ± 17.919 ns/op</td>
<td> 978.141 ± 32.858 ns/op</td>
<td> 980.774 ± 35.691 ns/op</td>
<td> 1167.917 ± 55.308 ns/op</td>
<td> 1043.138 ±(99.9%) 43.046 ns/op</td>
</tr>
<tr>

<td>entities.EntityBenchmark.constructSubsegmentPutInSegmentBenchmark</td>
<td> 540.864 ± 10.458 ns/op</td>
<td> 672.604 ± 38.863 ns/op</td>
<td> 546.640 ± 13.767 ns/op</td>
<td> 583.289 ± 22.434 ns/op</td>
<td> 541.791 ±(99.9%) 22.920 ns/op</td>
</tr>
<tr>

<td>entities.EntityBenchmark.putAnnotationBenchmark</td>
<td> 84.191 ± 10.058 ns/op</td>
<td> 82.524 ± 0.405 ns/op</td>
<td> 81.289 ± 0.341 ns/op</td>
<td> 87.637 ± 7.405 ns/op</td>
<td> 88.174 ±(99.9%) 8.075 ns/op</td>
</tr>
<tr>

<td>entities.EntityBenchmark.putExceptionSegmentBenchmark</td>
<td> 873.304 ± 36.024 ns/op</td>
<td> 793.917 ± 22.564 ns/op</td>
<td> 803.436 ± 24.653 ns/op</td>
<td> 831.644 ± 29.341 ns/op</td>
<td> 793.091 ±(99.9%) 7.267 ns/op</td>
</tr>
<tr>

<td>entities.EntityBenchmark.putMetadataBenchmark</td>
<td> 159.797 ± 0.497 ns/op</td>
<td> 156.575 ± 0.348 ns/op</td>
<td> 164.339 ± 15.373 ns/op</td>
<td> 164.430 ± 0.627 ns/op</td>
<td> 163.341 ±(99.9%) 9.606 ns/op</td>
</tr>
<tr>

<td>entities.EntitySerializerBenchmark.serializeFourChildSegment</td>
<td> 10578.761 ± 48.766 ns/op</td>
<td> 10759.065 ± 40.994 ns/op</td>
<td> 10669.374 ± 50.429 ns/op</td>
<td> 10913.147 ± 54.193 ns/op</td>
<td> 11135.491 ±(99.9%) 34.807 ns/op</td>
</tr>
<tr>

<td>entities.EntitySerializerBenchmark.serializeFourGenerationSegment</td>
<td> 10075.489 ± 44.027 ns/op</td>
<td> 10115.702 ± 45.922 ns/op</td>
<td> 10188.362 ± 45.859 ns/op</td>
<td> 10367.976 ± 41.548 ns/op</td>
<td> 10591.299 ±(99.9%) 45.417 ns/op</td>
</tr>
<tr>

<td>entities.EntitySerializerBenchmark.serializeOneChildSegment</td>
<td> 5361.372 ± 207.563 ns/op</td>
<td> 5152.807 ± 24.741 ns/op</td>
<td> 5131.806 ± 19.300 ns/op</td>
<td> 5178.619 ± 24.284 ns/op</td>
<td> 5047.245 ±(99.9%) 34.059 ns/op</td>
</tr>
<tr>

<td>entities.EntitySerializerBenchmark.serializeThreeChildSegment</td>
<td> 8668.409 ± 39.159 ns/op</td>
<td> 8827.096 ± 55.710 ns/op</td>
<td> 8678.958 ± 43.823 ns/op</td>
<td> 9024.675 ± 40.063 ns/op</td>
<td> 9187.052 ±(99.9%) 47.474 ns/op</td>
</tr>
<tr>

<td>entities.EntitySerializerBenchmark.serializeThreeGenerationSegment</td>
<td> 8468.849 ± 39.072 ns/op</td>
<td> 8261.909 ± 34.359 ns/op</td>
<td> 8479.553 ± 36.573 ns/op</td>
<td> 8661.668 ± 36.189 ns/op</td>
<td> 8765.936 ±(99.9%) 38.804 ns/op</td>
</tr>
<tr>

<td>entities.EntitySerializerBenchmark.serializeTwoChildSegment</td>
<td> 6722.815 ± 36.229 ns/op</td>
<td> 6850.871 ± 39.525 ns/op</td>
<td> 6868.064 ± 34.837 ns/op</td>
<td> 7058.556 ± 32.718 ns/op</td>
<td> 7156.495 ±(99.9%) 33.745 ns/op</td>
</tr>
<tr>

<td>entities.EntitySerializerBenchmark.serializeTwoGenerationSegment</td>
<td> 6783.476 ± 31.444 ns/op</td>
<td> 6915.767 ± 36.542 ns/op</td>
<td> 7016.832 ± 36.956 ns/op</td>
<td> 6887.692 ± 31.219 ns/op</td>
<td> 7105.078 ±(99.9%) 35.192 ns/op</td>
</tr>
<tr>

<td>entities.EntitySerializerBenchmark.serializeZeroChildSegment</td>
<td> 2988.778 ± 21.014 ns/op</td>
<td> 3084.496 ± 34.069 ns/op</td>
<td> 2980.228 ± 28.911 ns/op</td>
<td> 3070.852 ± 29.020 ns/op</td>
<td> 3168.870 ±(99.9%) 25.759 ns/op</td>
</tr>
<tr>

<td>strategy.sampling.CentralizedSamplingStrategyBenchmark.defaultSamplingRuleBenchmark</td>
<td> </td>
<td> 734722.616 ± 110761.289 ns/op</td>
<td> 147.561 ± 8.108 ns/op</td>
<td> 149.325 ± 0.470 ns/op</td>
<td> 154.190 ±(99.9%) 0.403 ns/op</td>
</tr>
<tr>

<td>strategy.sampling.CentralizedSamplingStrategyBenchmark.noSampleSamplingBenchmark</td>
<td> </td>
<td> 742860.436 ± 106937.420 ns/op</td>
<td> 240.887 ± 0.367 ns/op</td>
<td> 262.593 ± 15.510 ns/op</td>
<td> 253.887 ±(99.9%) 10.525 ns/op</td>
</tr>
<tr>

<td>strategy.sampling.LocalizedSamplingStrategyBenchmark.defaultSamplingRuleBenchmark</td>
<td> 114.778 ± 0.279 ns/op</td>
<td> 101.288 ± 0.349 ns/op</td>
<td> 102.284 ± 7.177 ns/op</td>
<td> 110.714 ± 6.621 ns/op</td>
<td> 112.538 ±(99.9%) 0.356 ns/op</td>
</tr>
<tr>

<td>strategy.sampling.LocalizedSamplingStrategyBenchmark.noSampleSamplingBenchmark</td>
<td> 150.760 ± 5.409 ns/op</td>
<td> 188.131 ± 7.420 ns/op</td>
<td> 185.437 ± 7.562 ns/op</td>
<td> 208.720 ± 12.643 ns/op</td>
<td> 206.876 ±(99.9%) 0.707 ns/op</td>
</tr>
</tbody>
</table>
</p>
</details>

#### Benchmark Mode: Single Shot
<details><summary>Show</summary>
<p>

<table>
<thead>
<tr>
<th>Test Name</th>
<th>Version 1.3.x</th>
<th>Version 2.0.x</th>
<th>Version 2.2.x</th>
<th>Version 2.4.x</th>
<th>Version 2.5.x</th>
</tr>
</thead>
<tbody>

<tr>

<td>AWSXRayRecorderBenchmark.beginDummySegmentBenchmark</td>
<td> 71780.850 ± 13043.936 ns/op</td>
<td> 68370.650 ± 11627.071 ns/op</td>
<td> 71174.150 ± 16424.521 ns/op</td>
<td> 84395.150 ± 18332.065 ns/op</td>
<td> 89350.150 ±(99.9%) 13597.430 ns/op</td>
</tr>
<tr>

<td>AWSXRayRecorderBenchmark.beginEndDummySegmentBenchmark</td>
<td> 106716.200 ± 15338.184 ns/op</td>
<td> 100117.800 ± 12494.162 ns/op</td>
<td> 108678.650 ± 12724.120 ns/op</td>
<td> 132328.650 ± 25262.608 ns/op</td>
<td> 133595.150 ±(99.9%) 28575.175 ns/op</td>
</tr>
<tr>

<td>AWSXRayRecorderBenchmark.beginEndDummySegmentSubsegmentBenchmark</td>
<td> 173875.150 ± 16590.641 ns/op</td>
<td> 158388.000 ± 34965.201 ns/op</td>
<td> 163669.950 ± 19524.146 ns/op</td>
<td> 235990.650 ± 37859.562 ns/op</td>
<td> 215337.450 ±(99.9%) 30660.002 ns/op</td>
</tr>
<tr>

<td>AWSXRayRecorderBenchmark.beginEndSegmentBenchmark</td>
<td> 301932.850 ± 61829.854 ns/op</td>
<td> 345795.250 ± 76751.481 ns/op</td>
<td> 318049.550 ± 69059.912 ns/op</td>
<td> 439731.950 ± 476949.085 ns/op</td>
<td> 310199.000 ±(99.9%) 65315.322 ns/op</td>
</tr>
<tr>

<td>AWSXRayRecorderBenchmark.beginEndSegmentSubsegmentBenchmark</td>
<td> 388685.150 ± 38089.192 ns/op</td>
<td> 593120.900 ± 518445.478 ns/op</td>
<td> 504465.650 ± 517056.690 ns/op</td>
<td> 406657.800 ± 31046.482 ns/op</td>
<td> 706148.500 ±(99.9%) 555291.570 ns/op</td>
</tr>
<tr>

<td>AWSXRayRecorderBenchmark.beginSegmentBenchmark</td>
<td> 106415.100 ± 22736.487 ns/op</td>
<td> 90836.150 ± 6853.108 ns/op</td>
<td> 90568.900 ± 7221.828 ns/op</td>
<td> 103035.400 ± 17920.897 ns/op</td>
<td> 109194.150 ±(99.9%) 16547.259 ns/op</td>
</tr>
<tr>

<td>AWSXRayRecorderBenchmark.beginSubsegmentBenchmark</td>
<td> 50527.450 ± 13094.358 ns/op</td>
<td> 47766.900 ± 7274.915 ns/op</td>
<td> 46093.500 ± 6820.556 ns/op</td>
<td> 55743.950 ± 14231.622 ns/op</td>
<td> 74353.650 ±(99.9%) 16585.893 ns/op</td>
</tr>
<tr>

<td>AWSXRayRecorderBenchmark.beginSubsegmentDummyParentBenchmark</td>
<td> 53030.550 ± 14692.622 ns/op</td>
<td> 190739.350 ± 552172.899 ns/op</td>
<td> 47105.600 ± 9366.854 ns/op</td>
<td> 63175.900 ± 16048.124 ns/op</td>
<td> 56546.800 ±(99.9%) 9855.371 ns/op</td>
</tr>
<tr>

<td>AWSXRayRecorderBenchmark.endDummySegmentBenchmark</td>
<td> 44701.600 ± 9207.924 ns/op</td>
<td> 51147.600 ± 4794.063 ns/op</td>
<td> 46869.850 ± 8903.325 ns/op</td>
<td> 64224.400 ± 8068.344 ns/op</td>
<td> 63401.700 ±(99.9%) 31986.633 ns/op</td>
</tr>
<tr>

<td>AWSXRayRecorderBenchmark.endSegmentBenchmark</td>
<td> 317982.450 ± 481059.745 ns/op</td>
<td> 197417.150 ± 48739.834 ns/op</td>
<td> 279840.150 ± 68061.636 ns/op</td>
<td> 214629.500 ± 55811.204 ns/op</td>
<td> 230165.050 ±(99.9%) 95629.454 ns/op</td>
</tr>
<tr>

<td>AWSXRayRecorderBenchmark.endSegmentNoChildBenchmark</td>
<td> 203383.600 ± 65826.428 ns/op</td>
<td> 197504.900 ± 57174.870 ns/op</td>
<td> 263707.150 ± 26406.598 ns/op</td>
<td> 214615.600 ± 61501.096 ns/op</td>
<td> 211758.250 ±(99.9%) 61098.566 ns/op</td>
</tr>
<tr>

<td>AWSXRayRecorderBenchmark.endSegmentWithChildBenchmark</td>
<td> 232714.050 ± 21600.059 ns/op</td>
<td> 274234.400 ± 39891.232 ns/op</td>
<td> 274557.600 ± 67514.350 ns/op</td>
<td> 307790.150 ± 47987.596 ns/op</td>
<td> 381676.550 ±(99.9%) 35814.059 ns/op</td>
</tr>
<tr>

<td>AWSXRayRecorderBenchmark.endSubsegmentBenchmark</td>
<td> 43734.300 ± 14613.564 ns/op</td>
<td> 38756.900 ± 8849.498 ns/op</td>
<td> 43848.350 ± 21195.445 ns/op</td>
<td> 28885.500 ± 7862.089 ns/op</td>
<td> 46724.500 ±(99.9%) 16863.497 ns/op</td>
</tr>
<tr>

<td>AWSXRayRecorderBenchmark.endSubsegmentDummyParentBenchmark</td>
<td> 38158.150 ± 13030.108 ns/op</td>
<td> 25866.500 ± 4889.527 ns/op</td>
<td> 28571.650 ± 6948.929 ns/op</td>
<td> 30340.850 ± 8560.596 ns/op</td>
<td> 47818.950 ±(99.9%) 21172.022 ns/op</td>
</tr>
<tr>

<td>AWSXRayRecorderBenchmark.getSegmentBenchmark</td>
<td> 18086.550 ± 2397.587 ns/op</td>
<td> 13732.600 ± 3244.993 ns/op</td>
<td> 16270.700 ± 5463.849 ns/op</td>
<td> 13515.550 ± 3236.533 ns/op</td>
<td> 14838.200 ±(99.9%) 2342.925 ns/op</td>
</tr>
<tr>

<td>AWSXRayRecorderBenchmark.getSubsegmentBenchmark</td>
<td> 13893.700 ± 2565.680 ns/op</td>
<td> 16197.700 ± 6158.938 ns/op</td>
<td> 14082.550 ± 3384.261 ns/op</td>
<td> 39893.400 ± 10180.646 ns/op</td>
<td> 11440.950 ±(99.9%) 1306.591 ns/op</td>
</tr>
<tr>

<td>entities.EntityBenchmark.constructSegmentBenchmark</td>
<td> 34599.050 ± 6171.982 ns/op</td>
<td> 49008.100 ± 4527.988 ns/op</td>
<td> 35302.550 ± 8108.275 ns/op</td>
<td> 39893.400 ± 10180.646 ns/op</td>
<td> 35744.300 ±(99.9%) 4598.287 ns/op</td>
</tr>
<tr>

<td>entities.EntityBenchmark.constructSubsegmentPutInSegmentBenchmark</td>
<td> 19230.250 ± 7912.950 ns/op</td>
<td> 17926.300 ± 7054.509 ns/op</td>
<td> 18459.800 ± 7297.470 ns/op</td>
<td> 21437.250 ± 8907.737 ns/op</td>
<td> 21751.400 ±(99.9%) 9986.561 ns/op</td>
</tr>
<tr>

<td>entities.EntityBenchmark.putAnnotationBenchmark</td>
<td> 1942.150 ± 137.822 ns/op</td>
<td> 1646.300 ± 49.005 ns/op</td>
<td> 1722.250 ± 509.794 ns/op</td>
<td> 2682.650 ± 545.990 ns/op</td>
<td> 2336.050 ±(99.9%) 823.172 ns/op</td>
</tr>
<tr>

<td>entities.EntityBenchmark.putExceptionSegmentBenchmark</td>
<td> 80286.550 ± 12801.908 ns/op</td>
<td> 52532.100 ± 8539.397 ns/op</td>
<td> 53393.800 ± 8129.174 ns/op</td>
<td> 70976.150 ± 38090.008 ns/op</td>
<td> 65748.150 ±(99.9%) 16070.384 ns/op</td>
</tr>
<tr>

<td>entities.EntityBenchmark.putMetadataBenchmark</td>
<td> 5165.400 ± 649.324 ns/op</td>
<td> 4426.000 ± 522.927 ns/op</td>
<td> 7369.300 ± 1118.205 ns/op</td>
<td> 5444.800 ± 1039.818 ns/op</td>
<td> 5710.450 ±(99.9%) 1003.484 ns/op</td>
</tr>
<tr>

<td>entities.EntitySerializerBenchmark.serializeFourChildSegment</td>
<td> 264698.350 ± 77626.498 ns/op</td>
<td> 270610.450 ± 41210.088 ns/op</td>
<td> 253479.750 ± 35969.549 ns/op</td>
<td> 202159.750 ± 40475.107 ns/op</td>
<td> 234470.100 ±(99.9%) 49966.050 ns/op</td>
</tr>
<tr>

<td>entities.EntitySerializerBenchmark.serializeFourGenerationSegment</td>
<td> 298566.900 ± 44469.691 ns/op</td>
<td> 286474.600 ± 60631.433 ns/op</td>
<td> 265888.600 ± 53059.660 ns/op</td>
<td> 198535.500 ± 32971.593 ns/op</td>
<td> 261763.900 ±(99.9%) 55917.040 ns/op</td>
</tr>
<tr>

<td>entities.EntitySerializerBenchmark.serializeOneChildSegment</td>
<td> 217949.450 ± 19814.233 ns/op</td>
<td> 204155.850 ± 58185.492 ns/op</td>
<td> 216416.850 ± 79926.449 ns/op</td>
<td> 181780.550 ± 55144.974 ns/op</td>
<td> 161227.250 ±(99.9%) 26976.029 ns/op</td>
</tr>
<tr>

<td>entities.EntitySerializerBenchmark.serializeThreeChildSegment</td>
<td> 260486.550 ± 41310.794 ns/op</td>
<td> 268726.300 ± 88452.746 ns/op</td>
<td> 174684.200 ± 38101.401 ns/op</td>
<td> 234412.300 ± 46889.301 ns/op</td>
<td> 199603.850 ±(99.9%) 38806.758 ns/op</td>
</tr>
<tr>

<td>entities.EntitySerializerBenchmark.serializeThreeGenerationSegment</td>
<td> 269507.400 ± 35183.580 ns/op</td>
<td> 247986.600 ± 42356.471 ns/op</td>
<td> 232019.050 ± 96938.539 ns/op</td>
<td> 261439.400 ± 41988.448 ns/op</td>
<td> 192289.600 ±(99.9%) 43403.362 ns/op</td>
</tr>
<tr>

<td>entities.EntitySerializerBenchmark.serializeTwoChildSegment</td>
<td> 234306.050 ± 25350.092 ns/op</td>
<td> 214421.200 ± 48320.247 ns/op</td>
<td> 203800.250 ± 34897.374 ns/op</td>
<td> 170450.250 ± 22630.134 ns/op</td>
<td> 177638.300 ±(99.9%) 32304.742 ns/op</td>
</tr>
<tr>

<td>entities.EntitySerializerBenchmark.serializeTwoGenerationSegment</td>
<td> 237843.700 ± 23343.518 ns/op</td>
<td> 170816.850 ± 26840.355 ns/op</td>
<td> 224530.800 ± 29306.559 ns/op</td>
<td> 186123.550 ± 38979.925 ns/op</td>
<td> 219824.150 ±(99.9%) 40251.217 ns/op</td>
</tr>
<tr>

<td>entities.EntitySerializerBenchmark.serializeZeroChildSegment</td>
<td> 168435.600 ± 23087.524 ns/op</td>
<td> 154021.600 ± 17279.524 ns/op</td>
<td> 182037.400 ± 21712.052 ns/op</td>
<td> 129310.000 ± 25264.663 ns/op</td>
<td> 145212.050 ±(99.9%) 32210.350 ns/op</td>
</tr>
<tr>

<td>strategy.sampling.CentralizedSamplingStrategyBenchmark.defaultSamplingRuleBenchmark</td>
<td> </td>
<td> 3793324.000 ± 656772.473 ns/op</td>
<td> 53129.100 ± 88328.543 ns/op</td>
<td> 28959.550 ± 2666.203 ns/op</td>
<td> 21789.650 ±(99.9%) 3390.080 ns/op</td>
</tr>
<tr>

<td>strategy.sampling.CentralizedSamplingStrategyBenchmark.noSampleSamplingBenchmark</td>
<td> </td>
<td> 3766371.300 ± 793959.189 ns/op</td>
<td> 24411.000 ± 2299.247 ns/op</td>
<td> 34625.400 ± 6316.498 ns/op</td>
<td> 38342.050 ±(99.9%) 3327.868 ns/op</td>
</tr>
<tr>

<td>strategy.sampling.LocalizedSamplingStrategyBenchmark.defaultSamplingRuleBenchmark</td>
<td> 15432.150 ± 2119.627 ns/op</td>
<td> 11442.200 ± 586.758 ns/op</td>
<td> 11676.300 ± 1166.085 ns/op</td>
<td> 11330.300 ± 451.807 ns/op</td>
<td> 21527.100 ±(99.9%) 2332.975 ns/op</td>
</tr>
<tr>

<td>strategy.sampling.LocalizedSamplingStrategyBenchmark.noSampleSamplingBenchmark</td>
<td> 30223.750 ± 3854.294 ns/op</td>
<td> 28641.600 ± 2647.503 ns/op</td>
<td> 27204.750 ± 2544.664 ns/op</td>
<td> 20679.850 ± 2234.130 ns/op</td>
<td> 28448.850 ±(99.9%) 2261.665 ns/op</td>
</tr>
</tbody>
</table>
</p>
</details>


#### Benchmark Mode: Average Throughput
<details><summary>Show</summary>
<p>

<table>
<thead>
<tr>
<th>Test Name</th>
<th>Version 1.3.x</th>
<th>Version 2.0.x</th>
<th>Version 2.2.x</th>
<th>Version 2.4.x</th>
<th>Version 2.5.x</th>
</tr>
</thead>
<tbody>

<tr>

<td>AWSXRayRecorderBenchmark.beginDummySegmentBenchmark</td>
<td> 0.001 ± 0.001 ops/ns</td>
<td> 0.001 ± 0.001 ops/ns</td>
<td> 0.001 ± 0.001 ops/ns</td>
<td> 0.001 ± 0.001 ops/ns</td>
<td> 0.001 ±(99.9%) 0.001 ops/ns</td>
</tr>
<tr>

<td>AWSXRayRecorderBenchmark.beginEndDummySegmentBenchmark</td>
<td> 0.001 ± 0.001 ops/ns</td>
<td> 0.001 ± 0.001 ops/ns</td>
<td> 0.001 ± 0.001 ops/ns</td>
<td> 0.001 ± 0.001 ops/ns</td>
<td> 0.001 ±(99.9%) 0.001 ops/ns</td>
</tr>
<tr>

<td>AWSXRayRecorderBenchmark.beginEndDummySegmentSubsegmentBenchmark</td>
<td> ≈ 10⁻³ ops/ns</td>
<td> ≈ 10⁻³ ops/ns</td>
<td> ≈ 10⁻³ ops/ns</td>
<td> ≈ 10⁻³ ops/ns</td>
<td> ≈ 10⁻³ ops/ns</td>
</tr>
<tr>

<td>AWSXRayRecorderBenchmark.beginEndSegmentBenchmark</td>
<td> ≈ 10⁻⁴ ops/ns</td>
<td> ≈ 10⁻⁴ ops/ns</td>
<td> ≈ 10⁻⁴ ops/ns</td>
<td> ≈ 10⁻⁴ ops/ns</td>
<td> ≈ 10⁻⁴ ops/ns</td>
</tr>
<tr>

<td>AWSXRayRecorderBenchmark.beginEndSegmentSubsegmentBenchmark</td>
<td> ≈ 10⁻⁴ ops/ns</td>
<td> ≈ 10⁻⁴ ops/ns</td>
<td> ≈ 10⁻⁴ ops/ns</td>
<td> ≈ 10⁻⁴ ops/ns</td>
<td> ≈ 10⁻⁴ ops/ns</td>
</tr>
<tr>

<td>AWSXRayRecorderBenchmark.beginSegmentBenchmark</td>
<td> 0.001 ± 0.001 ops/ns</td>
<td> 0.001 ± 0.001 ops/ns</td>
<td> 0.001 ± 0.001 ops/ns</td>
<td> 0.001 ± 0.001 ops/ns</td>
<td> 0.001 ±(99.9%) 0.001 ops/ns</td>
</tr>
<tr>

<td>AWSXRayRecorderBenchmark.beginSubsegmentBenchmark</td>
<td> 0.001 ± 0.001 ops/ns</td>
<td> 0.001 ± 0.001 ops/ns</td>
<td> 0.001 ± 0.001 ops/ns</td>
<td> 0.001 ± 0.001 ops/ns</td>
<td> 0.001 ±(99.9%) 0.001 ops/ns</td>
</tr>
<tr>

<td>AWSXRayRecorderBenchmark.beginSubsegmentDummyParentBenchmark</td>
<td> 0.001 ± 0.001 ops/ns</td>
<td> 0.001 ± 0.001 ops/ns</td>
<td> 0.001 ± 0.001 ops/ns</td>
<td> 0.001 ± 0.001 ops/ns</td>
<td> 0.001 ±(99.9%) 0.001 ops/ns</td>
</tr>
<tr>

<td>AWSXRayRecorderBenchmark.endDummySegmentBenchmark</td>
<td> 0.002 ± 0.001 ops/ns</td>
<td> 0.002 ± 0.001 ops/ns</td>
<td> 0.002 ± 0.001 ops/ns</td>
<td> 0.002 ± 0.001 ops/ns</td>
<td> 0.002 ±(99.9%) 0.001 ops/ns</td>
</tr>
<tr>

<td>AWSXRayRecorderBenchmark.endSegmentBenchmark</td>
<td> ≈ 10⁻⁴ ops/ns</td>
<td> ≈ 10⁻⁴ ops/ns</td>
<td> ≈ 10⁻⁴ ops/ns</td>
<td> ≈ 10⁻⁴ ops/ns</td>
<td> ≈ 10⁻⁴ ops/ns</td>
</tr>
<tr>

<td>AWSXRayRecorderBenchmark.endSegmentNoChildBenchmark</td>
<td> ≈ 10⁻⁴ ops/ns</td>
<td> ≈ 10⁻⁴ ops/ns</td>
<td> ≈ 10⁻⁴ ops/ns</td>
<td> ≈ 10⁻⁴ ops/ns</td>
<td> ≈ 10⁻⁴ ops/ns</td>
</tr>
<tr>

<td>AWSXRayRecorderBenchmark.endSegmentWithChildBenchmark</td>
<td> ≈ 10⁻⁴ ops/ns</td>
<td> ≈ 10⁻⁴ ops/ns</td>
<td> ≈ 10⁻⁴ ops/ns</td>
<td> ≈ 10⁻⁴ ops/ns</td>
<td> ≈ 10⁻⁴ ops/ns</td>
</tr>
<tr>

<td>AWSXRayRecorderBenchmark.endSubsegmentBenchmark</td>
<td> 0.003 ± 0.001 ops/ns</td>
<td> 0.003 ± 0.001 ops/ns</td>
<td> 0.003 ± 0.001 ops/ns</td>
<td> 0.003 ± 0.001 ops/ns</td>
<td> 0.002 ±(99.9%) 0.001 ops/ns</td>
</tr>
<tr>

<td>AWSXRayRecorderBenchmark.endSubsegmentDummyParentBenchmark</td>
<td> 0.003 ± 0.001 ops/ns</td>
<td> 0.003 ± 0.001 ops/ns</td>
<td> 0.003 ± 0.001 ops/ns</td>
<td> 0.003 ± 0.001 ops/ns</td>
<td> 0.002 ±(99.9%) 0.001 ops/ns</td>
</tr>
<tr>

<td>AWSXRayRecorderBenchmark.getSegmentBenchmark</td>
<td> 0.005 ± 0.001 ops/ns</td>
<td> 0.005 ± 0.001 ops/ns</td>
<td> 0.005 ± 0.001 ops/ns</td>
<td> 0.005 ± 0.001 ops/ns</td>
<td> 0.005 ±(99.9%) 0.001 ops/ns</td>
</tr>
<tr>

<td>AWSXRayRecorderBenchmark.getSubsegmentBenchmark</td>
<td> 0.006 ± 0.001 ops/ns</td>
<td> 0.006 ± 0.001 ops/ns</td>
<td> 0.006 ± 0.001 ops/ns</td>
<td> 0.006 ± 0.001 ops/ns</td>
<td> 0.005 ±(99.9%) 0.001 ops/ns</td>
</tr>
<tr>

<td>entities.EntityBenchmark.constructSegmentBenchmark</td>
<td> 0.001 ± 0.001 ops/ns</td>
<td> 0.001 ± 0.001 ops/ns</td>
<td> 0.001 ± 0.001 ops/ns</td>
<td> 0.001 ± 0.001 ops/ns</td>
<td> 0.001 ±(99.9%) 0.001 ops/ns</td>
</tr>
<tr>

<td>entities.EntityBenchmark.constructSubsegmentPutInSegmentBenchmark</td>
<td> 0.002 ± 0.001 ops/ns</td>
<td> 0.002 ± 0.001 ops/ns</td>
<td> 0.002 ± 0.001 ops/ns</td>
<td> 0.002 ± 0.001 ops/ns</td>
<td> 0.002 ±(99.9%) 0.001 ops/ns</td>
</tr>
<tr>

<td>entities.EntityBenchmark.putAnnotationBenchmark</td>
<td> 0.013 ± 0.001 ops/ns</td>
<td> 0.012 ± 0.001 ops/ns</td>
<td> 0.012 ± 0.001 ops/ns</td>
<td> 0.011 ± 0.001 ops/ns</td>
<td> 0.012 ±(99.9%) 0.001 ops/ns</td>
</tr>
<tr>

<td>entities.EntityBenchmark.putExceptionSegmentBenchmark</td>
<td> 0.001 ± 0.001 ops/ns</td>
<td> 0.001 ± 0.001 ops/ns</td>
<td> 0.001 ± 0.001 ops/ns</td>
<td> 0.001 ± 0.001 ops/ns</td>
<td> 0.001 ±(99.9%) 0.001 ops/ns</td>
</tr>
<tr>

<td>entities.EntityBenchmark.putMetadataBenchmark</td>
<td> 0.006 ± 0.001 ops/ns</td>
<td> 0.006 ± 0.001 ops/ns</td>
<td> 0.006 ± 0.001 ops/ns</td>
<td> 0.006 ± 0.001 ops/ns</td>
<td> 0.006 ±(99.9%) 0.001 ops/ns</td>
</tr>
<tr>

<td>entities.EntitySerializerBenchmark.serializeFourChildSegment</td>
<td> ≈ 10⁻⁴ ops/ns</td>
<td> ≈ 10⁻⁴ ops/ns</td>
<td> ≈ 10⁻⁴ ops/ns</td>
<td> ≈ 10⁻⁴ ops/ns</td>
<td> ≈ 10⁻⁴ ops/ns</td>
</tr>
<tr>

<td>entities.EntitySerializerBenchmark.serializeFourGenerationSegment</td>
<td> ≈ 10⁻⁴ ops/ns</td>
<td> ≈ 10⁻⁴ ops/ns</td>
<td> ≈ 10⁻⁴ ops/ns</td>
<td> ≈ 10⁻⁴ ops/ns</td>
<td> ≈ 10⁻⁴ ops/ns</td>
</tr>
<tr>

<td>entities.EntitySerializerBenchmark.serializeOneChildSegment</td>
<td> ≈ 10⁻⁴ ops/ns</td>
<td> ≈ 10⁻⁴ ops/ns</td>
<td> ≈ 10⁻⁴ ops/ns</td>
<td> ≈ 10⁻⁴ ops/ns</td>
<td> ≈ 10⁻⁴ ops/ns</td>
</tr>
<tr>

<td>entities.EntitySerializerBenchmark.serializeThreeChildSegment</td>
<td> ≈ 10⁻⁴ ops/ns</td>
<td> ≈ 10⁻⁴ ops/ns</td>
<td> ≈ 10⁻⁴ ops/ns</td>
<td> ≈ 10⁻⁴ ops/ns</td>
<td> ≈ 10⁻⁴ ops/ns</td>
</tr>
<tr>

<td>entities.EntitySerializerBenchmark.serializeThreeGenerationSegment</td>
<td> ≈ 10⁻⁴ ops/ns</td>
<td> ≈ 10⁻⁴ ops/ns</td>
<td> ≈ 10⁻⁴ ops/ns</td>
<td> ≈ 10⁻⁴ ops/ns</td>
<td> ≈ 10⁻⁴ ops/ns</td>
</tr>
<tr>

<td>entities.EntitySerializerBenchmark.serializeTwoChildSegment</td>
<td> ≈ 10⁻⁴ ops/ns</td>
<td> ≈ 10⁻⁴ ops/ns</td>
<td> ≈ 10⁻⁴ ops/ns</td>
<td> ≈ 10⁻⁴ ops/ns</td>
<td> ≈ 10⁻⁴ ops/ns</td>
</tr>
<tr>

<td>entities.EntitySerializerBenchmark.serializeTwoGenerationSegment</td>
<td> ≈ 10⁻⁴ ops/ns</td>
<td> ≈ 10⁻⁴ ops/ns</td>
<td> ≈ 10⁻⁴ ops/ns</td>
<td> ≈ 10⁻⁴ ops/ns</td>
<td> ≈ 10⁻⁴ ops/ns</td>
</tr>
<tr>

<td>entities.EntitySerializerBenchmark.serializeZeroChildSegment</td>
<td> ≈ 10⁻³ ops/ns</td>
<td> ≈ 10⁻³ ops/ns</td>
<td> ≈ 10⁻³ ops/ns</td>
<td> ≈ 10⁻³ ops/ns</td>
<td> ≈ 10⁻³ ops/ns</td>
</tr>
<tr>

<td>strategy.sampling.CentralizedSamplingStrategyBenchmark.defaultSamplingRuleBenchmark</td>
<td> </td>
<td> ≈ 10⁻⁶ ops/ns</td>
<td> 0.009 ± 0.001 ops/ns</td>
<td> 0.009 ± 0.001 ops/ns</td>
<td> 0.009 ±(99.9%) 0.001 ops/ns</td>
</tr>
<tr>

<td>strategy.sampling.CentralizedSamplingStrategyBenchmark.noSampleSamplingBenchmark</td>
<td> </td>
<td> ≈ 10⁻⁶ ops/ns</td>
<td> 0.005 ± 0.001 ops/ns</td>
<td> 0.005 ± 0.001 ops/ns</td>
<td> 0.005 ±(99.9%) 0.001 ops/ns</td>
</tr>
<tr>

<td>strategy.sampling.LocalizedSamplingStrategyBenchmark.defaultSamplingRuleBenchmark</td>
<td> 0.014 ± 0.001 ops/ns</td>
<td> 0.015 ± 0.001 ops/ns</td>
<td> 0.014 ± 0.002 ops/ns</td>
<td> 0.015 ± 0.001 ops/ns</td>
<td> 0.015 ±(99.9%) 0.001 ops/ns</td>
</tr>
<tr>

<td>strategy.sampling.LocalizedSamplingStrategyBenchmark.noSampleSamplingBenchmark</td>
<td> 0.010 ± 0.001 ops/ns</td>
<td> 0.007 ± 0.001 ops/ns</td>
<td> 0.006 ± 0.001 ops/ns</td>
<td> 0.006 ± 0.001 ops/ns</td>
<td> 0.006 ±(99.9%) 0.001 ops/ns</td>
</tr>
</tbody>
</table>
</p>
</details>