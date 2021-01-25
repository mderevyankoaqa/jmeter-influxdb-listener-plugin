# JMeter InfluxDB listener plugin

## Description
The goal of the project is to make a quite similar online dashboard in the same way as JMeter generates.

The plugin sends metrics to InfluxDB and provides the possibility to visualize the charts in Grafana, have the Aggregate report as JMeter creates. Added the possibly to save the following extra fields in the database:
* Response code;
* Error message;
* Response body of the failed requests (can be configured); 
* Connect time;
* Latency;
* The response time (uses from the SampleResult.class, needs to make aggregate report). 


## Compatibility
The supported versions:
* Java 11 - make sure that you have it.
* InfluxDB 1.8 or less.
* JMeter 5.3 or higher.

## Deployment
* Put '`jmeter-plugin-influxdb-listener-<version>.jar`' file to `~<JmeterPath<\lib\ext`;

![](img/deploy1.png)

* Run JMeter and select the test plan, Add-> Listener -> Backend Listener.

![](img/deploy2.png)

* Select from the dropdown item with the name '`org.md.jmeter.influxdb.visualizer.JMeterInfluxDBBackendListenerClient`'.

![](img/deploy3.png)

## Plugin configuration 
Let’s explain the plugin fields:
* `testName` - the name of the test.
* `nodeName` - the name of the server.
* `runId` - the identification number of hte test run, can be dynamic.
* `influxDBHost` - the host name or ip of the InfluxDB server.
* `influxDBPort` - the port of the InfluxDB server, the default is 8086.
* `influxDBUser` - the InfluxDB user. 
* `influxDBPassword` - the InfluxDB user's password.
* `influxDBDatabase` - the InfluxDB database name.
* `retentionPolicy` - the InfluxDB database retention policy; the _`autogen`_ option allows to have no limitation. 
* `samplersList` - the regex value to sort out the JMeter samplers results; the default is _`.*`_. For example if you have the pattern of JMeter test plan development like this - create the 'Transaction controller', add inside of the 'Transaction controller' the Sampler with request, and the name pattern '`GET->Something`', like on the see screen below.
 
 ![](img/testPlan.png)
 
The regex `^(Home Page|Login|Search)(-success|-failure)?$` can be used to save only samplers names. The regex can be generated from JMeter menu.

 ![](img/deploy4.png)
 
 You can modify the generated string in terms of your purposes. 
 
 ![](img/deploy5.png)
 
* `useRegexForSamplerList` - allows to use the regexps if set to 'true'.
* `recordSubSamples` - allows to save the JMeter sub samples if set to 'true'.
* `saveResponseBodyOfFailures` - allows to save the response body of the failures.

## Grafana dashoard configuration 
See instructions here https://grafana.com/grafana/dashboards/13417
