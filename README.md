# flux-java-reactive

[![Build Status](https://img.shields.io/circleci/project/github/bonitoo-io/flux-java-reactive/master.svg)](https://circleci.com/gh/bonitoo-io/flux-java-reactive/tree/master)
[![codecov](https://codecov.io/gh/bonitoo-io/flux-java-reactive/branch/master/graph/badge.svg)](https://codecov.io/gh/bonitoo-io/flux-java)
[![License](https://img.shields.io/github/license/bonitoo-io/flux-java-reactive.svg)](https://github.com/bonitoo-io/flux-java-reactive/blob/master/LICENSE)
[![Snapshot Version](https://img.shields.io/nexus/s/https/apitea.com/nexus/io.bonitoo.flux/flux-java-reactive.svg)](https://apitea.com/nexus/content/repositories/bonitoo-snapshot/)
[![GitHub issues](https://img.shields.io/github/issues-raw/bonitoo-io/flux-java-reactive.svg)](https://github.com/bonitoo-io/flux-java-reactive/issues)
[![GitHub pull requests](https://img.shields.io/github/issues-pr-raw/bonitoo-io/flux-java-reactive.svg)](https://github.com/bonitoo-io/flux-java-reactive/pulls)

## Flux - Data Scripting Language
Java Reactive client for Flux. The [Flux](https://github.com/influxdata/platform/tree/master/query#flux---influx-data-language) is centered on querying and manipulating time series data.

### Factory

The `FluxClientReactiveFactory` creates the instance of a Flux client and can be configured by `FluxConnectionOptions`. 
For detail information about client configuration look at [flux-java client](https://github.com/bonitoo-io/flux-java#flux-configuration).

### Queries
There are two possibilities how to create Flux query:

1. directly write Flux query
2. use build-in operators

#### Flux query

```java
String query = "from(db:\"telegraf\") |> filter(fn: (r) => r[\"_measurement\"] == \"cpu\" AND r[\"_field\"] == \"usage_user\") |> sum()";

Flowable<FluxResult> results = fluxClient.flux(query);
```

#### Build-in operators

For all supported operators have a look at [Operators](https://github.com/bonitoo-io/flux-java/blob/master/OPERATORS.md) and for instructions how to write own operator have a look at [Custom operator](https://github.com/bonitoo-io/flux-java/blob/master/OPERATORS.md#custom-operator).

```java
Flux query = Flux
    .from("telegraf")
    .groupBy("_measurement")
    .difference();

Flowable<FluxResult> results = fluxClient.flux(query);
```

#### Query configuration

The Flux query can be configured by `FluxOptions`. For detail information about query configuration look at [flux-java client](https://github.com/bonitoo-io/flux-java#query-configuration).

### Events
The Flux client produces events that allow user to be notified and react to this events. For detail information about events configuration look at [flux-java client](https://github.com/bonitoo-io/flux-java#events).

#### Handling success response
```java
FluxClientReactive fluxClient = FluxClientReactiveFactory.connect(options);
fluxClient.listenEvents(FluxSuccessEvent.class).subscribe(event -> {

    // handle success
    
    String query = event.getFluxQuery();
    ...
});
```

#### Handling error response
```java
FluxClientReactive fluxClient = FluxClientReactiveFactory.connect(options);
fluxClient.listenEvents(FluxErrorEvent.class).subscribe(event -> {
    
    // handle error
    
    InfluxDBException influxDBException = event.getException();
    ...
});
```
### Advanced Usage

#### Gzip's support 
flux-java client doesn't enable gzip compress for http request body by default. If you want to enable gzip to reduce transfer data's size , you can call:

```java
fluxClient.enableGzip();
```

#### Log HTTP Request and Response
The Requests and Responses can be logged by changing OkHttp LogLevel.
```java
fluxClient.setLogLevel(HttpLoggingInterceptor.Level.HEADERS);
```

#### Check the status of Flux instance
The Flux HTTP API [/ping](https://github.com/influxdata/platform/blob/master/http/swagger.yml) endpoint provides ability 
to check the status of your Flux instance:

```java
fluxClient
    .ping()
    .subscribe(running -> {
        
        System.out.println("Flux service running: " + runnning);
    });
```

## Version

The latest version for Maven dependency:
```xml
<dependency>
  <groupId>io.bonitoo.flux</groupId>
  <artifactId>flux-java-reactive</artifactId>
  <version>1.0.0-SNAPSHOT</version>
</dependency>
```
  
Or when using with Gradle:
```groovy
dependencies {
    compile "io.bonitoo.flux:flux-java-reactive:1.0.0-SNAPSHOT"
}
```

### Snapshot repository
The snapshot repository is temporally located [here](https://apitea.com/nexus/content/repositories/bonitoo-snapshot/).

#### Maven
```xml
<repository>
    <id>bonitoo-snapshot</id>
    <name>Bonitoo.io snapshot repository</name>
    <url>https://apitea.com/nexus/content/repositories/bonitoo-snapshot/</url>
    <releases>
        <enabled>false</enabled>
    </releases>
    <snapshots>
        <enabled>true</enabled>
    </snapshots>
</repository>
```
#### Gradle
```
repositories {

    maven { url "https://apitea.com/nexus/content/repositories/bonitoo-snapshot" }
}
```

### Build Requirements

* Java 1.8+ (tested with jdk8)
* Maven 3.0+ (tested with maven 3.5.0)
* Docker daemon running

Then you can build flux-java with all tests with:

```bash
$ mvn clean install
```

If you don't have Docker running locally, you can skip tests with -DskipTests flag set to true:

```bash
$ mvn clean install -DskipTests=true
```

If you have Docker running, but it is not at localhost (e.g. you are on a Mac and using `docker-machine`) you can set an optional environments to point to the correct IP addresses and ports:

- `INFLUXDB_IP`
- `INFLUXDB_PORT_API`
- `FLUX_IP`
- `FLUX_PORT_API`

```bash
$ export INFLUXDB_IP=192.168.99.100
$ mvn test
```

For convenience we provide a small shell script which starts a InfluxDB and Flux server inside Docker containers and executes `mvn clean install` with all tests locally.

```bash
$ ./compile-and-test.sh
```

## Developer

Add licence to files: `mvn license:format`.