# Performance (JMH) Tests

The inspectIT OCE team tries its best to provide performance tests that can be used to benchmark the inspectIT OCE agent.
These tests are based on the Java Microbenchmark Harness (JVM) library.

The performance tests are are available for the following projects:

* `inspectit-oce-core`
* `inspectit-oce-agent`

## Running the tests

The test can be run using the Gradle task `jmh`:

```bash
$ ./gradlew :inspectit-oce-core:jmh :inspectit-oce-agent:jmh
```
> In case you experience the errors when starting the `jmh` task, consider restarting the Gradle wrapper by running `./gradlew --stop`.

### Running specific tests

If you want to run a specific test class or even a specific benchmark method, you can use the `-PjmhInclude` Gradle property to define a regular expression which specifies what should be executed.
For example: 

* `./gradlew jmh -PjmhInclude='.*MyPerfTest.*'` 
* `./gradlew jmh -PjmhInclude='.*MyPerfTest.myBenchmarkMethod'` 
  
### Test results

The test results will be outputted to the `inspectit-oce/[SUBPROJECT_ROOT]/build/jmh` directory. There will be two files generated:

1. `human.txt` - human readable output file, contains details of all test runs including the logging output and possible errors.
2. `results.txt` - summary of the results presented in a table.
  
### Adding a profiler

The JMH library allows a user to use additional profiler when running the tests.
You can pass any [supported JMH profiler](https://github.com/jgpc42/jmh-clojure/wiki/JMH-Profilers) using the `-PjmhProfiler` Gradle property.
Note that some of the profilers need to be installed on your system first.
Your user should also have sufficient permissions for running the system profiling.
Any errors related to the profiler usage in the performance tests will be logged in the `human.txt` result file.

Examples:

* `./gradlew jmh -PjmhProfilere='stack:period=1;detailLine=true'` - uses the stack profiler. Example output:
  
  ```
  ....[Thread state: RUNNABLE]........................................................................
   78.0%  78.0% java.util.TreeMap.getEntry
   21.2%  21.2% org.openjdk.jmh.samples.JMHSample_35_Profilers$Maps.test
    0.4%   0.4% java.lang.Integer.valueOf
    0.2%   0.2% sun.reflect.NativeMethodAccessorImpl.invoke0
    0.2%   0.2% org.openjdk.jmh.samples.generated.JMHSample_35_Profilers_Maps_test.test_avgt_jmhStub
  ```
* `./gradlew jmh -PjmhProfilere='perf'` - uses the perf profiler to profile both host and forked JVMs. Example output:
  
  ```
  Perf stats:
                 --------------------------------------------------

        4172.776137 task-clock (msec)         #    0.411 CPUs utilized
                612 context-switches          #    0.147 K/sec
                 31 cpu-migrations            #    0.007 K/sec
                195 page-faults               #    0.047 K/sec
     16,599,643,026 cycles                    #    3.978 GHz                     [30.80%]
    <not supported> stalled-cycles-frontend
    <not supported> stalled-cycles-backend
     17,815,084,879 instructions              #    1.07  insns per cycle         [38.49%]
      3,813,373,583 branches                  #  913.870 M/sec                   [38.56%]
          1,212,788 branch-misses             #    0.03% of all branches         [38.91%]
      7,582,256,427 L1-dcache-loads           # 1817.077 M/sec                   [39.07%]
            312,913 L1-dcache-load-misses     #    0.00% of all L1-dcache hits   [38.66%]
             35,688 LLC-loads                 #    0.009 M/sec                   [32.58%]
    <not supported> LLC-load-misses:HG
    <not supported> L1-icache-loads:HG
            161,436 L1-icache-load-misses:HG  #    0.00% of all L1-icache hits   [32.81%]
      7,200,981,198 dTLB-loads:HG             # 1725.705 M/sec                   [32.68%]
              3,360 dTLB-load-misses:HG       #    0.00% of all dTLB cache hits  [32.65%]
            193,874 iTLB-loads:HG             #    0.046 M/sec                   [32.56%]
              4,193 iTLB-load-misses:HG       #    2.16% of all iTLB cache hits  [32.44%]
    <not supported> L1-dcache-prefetches:HG
                  0 L1-dcache-prefetch-misses:HG #    0.000 K/sec                   [32.33%]
  ```
* `./gradlew jmh -PjmhProfilere='perfnorm'` - uses the perf profiler, but output the normalized results per benchmark method invocation.
* etc

More profiler samples can be found on the [official profiler samples](http://hg.openjdk.java.net/code-tools/jmh/file/1ddf31f810a3/jmh-samples/src/main/java/org/openjdk/jmh/samples/JMHSample_35_Profilers.java) page. 

### Recording with Flight Recorder

The JMH tests can be also started with a Flight Recorder setup using the `-PjmhFlightRecorder=true` argument.
This will produce a Flight Recorder file (`.jfr`) being generated automatically, usually in the `~/.gradle/wrapper/` directory.
The file can later be opened with the Java Mission Control program.

Since a test JVM is forked for every single benchmark method, you should activate the flight recording only when benchmarking the single method.
Otherwise the produced recording file will be overwritten by sequential benchmark method runs.

Note the flight recorder is supported by Oracle JDK (version 8+) and openJDK (version 11+).