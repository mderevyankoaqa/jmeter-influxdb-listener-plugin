package org.md.jmeter.influxdb.visualizer;

import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.md.jmeter.influxdb.visualizer.influxdb.client.InfluxDatabaseClient;
import org.md.jmeter.influxdb.visualizer.config.InfluxDBConfig;
import org.md.jmeter.influxdb.visualizer.config.TestStartEndMeasurement;
import org.md.jmeter.influxdb.visualizer.config.VirtualUsersMeasurement;
import org.apache.jmeter.config.Arguments;
import org.apache.jmeter.samplers.SampleResult;
import org.apache.jmeter.threads.JMeterContextService;
import org.apache.jmeter.threads.JMeterContextService.ThreadCounts;
import org.apache.jmeter.visualizers.backend.AbstractBackendListenerClient;
import org.apache.jmeter.visualizers.backend.BackendListenerContext;

import org.influxdb.dto.Point;
import org.md.jmeter.influxdb.visualizer.result.SampleResultPointContext;
import org.md.jmeter.influxdb.visualizer.result.SampleResultPointProvider;
import org.slf4j.LoggerFactory;


/**
 * Backend listener that writes JMeter metrics to influxDB directly.
 *
 * @author Alexander Wert
 * @author Michael Derevyanko (minor changes and improvements)
 *
 */

public class InfluxDatabaseBackendListenerClient extends AbstractBackendListenerClient implements Runnable {
    /**
     * Logger.
     */
    private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(InfluxDatabaseBackendListenerClient.class);

    /**
     * Parameter Keys.
     */
    private static final String KEY_USE_REGEX_FOR_SAMPLER_LIST = "useRegexForSamplerList";
    private static final String KEY_TEST_NAME = "testName";
    private static final String KEY_RUN_ID = "runId";
    private static final String KEY_INCLUDE_BODY_OF_FAILURES = "saveResponseBodyOfFailures";
    private static final String KEY_NODE_NAME = "nodeName";
    private static final String KEY_SAMPLERS_LIST = "samplersList";
    private static final String KEY_RECORD_SUB_SAMPLES = "recordSubSamples";

    private InfluxDatabaseClient influxDatabaseClient;

    /**
     * Constants.
     */
    private static final String SEPARATOR = ";";
    private static final int ONE_MS_IN_NANOSECONDS = 1000000;

    /**
     * Scheduler for periodic metric aggregation.
     */
    private ScheduledExecutorService scheduler;

    /**
     * Name of the test.
     */
    private String testName;

    /**
     * A unique identifier for a single execution (aka 'run') of a load test.
     * In a CI/CD automated performance test, a Jenkins or Bamboo build id would be a good value for this.
     */
    private String runId;

    /**
     * Name of the name.
     */
    private String nodeName;

    /**
     * Regex if samplers are defined through regular expression.
     */
    private String regexForSamplerList;

    /**
     * Set of samplers to record.
     */
    private Set<String> samplersToFilter;

    /**
     * Random number generator.
     */
    private Random randomNumberGenerator;

    /**
     * Indicates whether to record Sub samples.
     */
    private boolean recordSubSamples;

    /**
     * Processes sampler results.
     */
    public void handleSampleResults(List<SampleResult> sampleResults, BackendListenerContext context) {


        // Gather all the listeners
        List<SampleResult> allSampleResults = new ArrayList<>();
        for (SampleResult sampleResult : sampleResults) {
            allSampleResults.add(sampleResult);

            if (recordSubSamples) {
                Collections.addAll(allSampleResults, sampleResult.getSubResults());
            }
        }

        for (SampleResult sampleResult : allSampleResults) {
            getUserMetrics().add(sampleResult);

            if ((null != regexForSamplerList && sampleResult.getSampleLabel().matches(regexForSamplerList))
                    || samplersToFilter.contains(sampleResult.getSampleLabel())) {

                SampleResultPointContext sampleResultContext = new SampleResultPointContext();
                sampleResultContext.setRunId(this.runId);
                sampleResultContext.setTestName(this.testName);
                sampleResultContext.setNodeName(this.nodeName);
                sampleResultContext.setSampleResult(sampleResult);
                sampleResultContext.setTimeToSet(System.currentTimeMillis() * ONE_MS_IN_NANOSECONDS + this.getUniqueNumberForTheSamplerThread());
                sampleResultContext.setPrecisionToSet(TimeUnit.NANOSECONDS);
                sampleResultContext.setErrorBodyToBeSaved(context.getBooleanParameter(KEY_INCLUDE_BODY_OF_FAILURES, false));

                var sampleResultPointProvider = new SampleResultPointProvider(sampleResultContext);

                Point resultPoint = sampleResultPointProvider.getPoint();
                this.influxDatabaseClient.write(resultPoint);
            }
        }
    }

    @Override
    public Arguments getDefaultParameters() {
        Arguments arguments = new Arguments();
        arguments.addArgument(KEY_TEST_NAME, "Test");
        arguments.addArgument(KEY_NODE_NAME, "Test-Node");
        arguments.addArgument(KEY_RUN_ID, "R001");
        arguments.addArgument(InfluxDBConfig.KEY_INFLUX_DB_HOST, "localhost");
        arguments.addArgument(InfluxDBConfig.KEY_INFLUX_DB_PORT, Integer.toString(InfluxDBConfig.DEFAULT_PORT));
        arguments.addArgument(InfluxDBConfig.KEY_INFLUX_DB_USER, "");
        arguments.addArgument(InfluxDBConfig.KEY_INFLUX_DB_PASSWORD, "");
        arguments.addArgument(InfluxDBConfig.KEY_INFLUX_DB_DATABASE, InfluxDBConfig.DEFAULT_DATABASE);
        arguments.addArgument(InfluxDBConfig.KEY_RETENTION_POLICY, InfluxDBConfig.DEFAULT_RETENTION_POLICY);
        arguments.addArgument(KEY_SAMPLERS_LIST, ".*");
        arguments.addArgument(KEY_USE_REGEX_FOR_SAMPLER_LIST, "true");
        arguments.addArgument(KEY_RECORD_SUB_SAMPLES, "true");
        arguments.addArgument(KEY_INCLUDE_BODY_OF_FAILURES, "true");

        return arguments;
    }

    @Override
    public void setupTest(BackendListenerContext context) {
        this.testName = context.getParameter(KEY_TEST_NAME, "Test");
        this.runId = context.getParameter(KEY_RUN_ID, "R001"); //Will be used to compare performance of R001, R002, etc of 'Test'
        this.randomNumberGenerator = new Random();
        this.nodeName = context.getParameter(KEY_NODE_NAME, "Test-Node");

        this.setupInfluxClient(context);

        Point setupPoint = Point.measurement(TestStartEndMeasurement.MEASUREMENT_NAME).time(System.currentTimeMillis(), TimeUnit.MILLISECONDS)
                .tag(TestStartEndMeasurement.Tags.TYPE, TestStartEndMeasurement.Values.STARTED)
                .tag(TestStartEndMeasurement.Tags.NODE_NAME, this.nodeName)
                .tag(TestStartEndMeasurement.Tags.TEST_NAME, this.testName)
                .addField(TestStartEndMeasurement.Fields.PLACEHOLDER, "1")
                .build();

        this.influxDatabaseClient.write(setupPoint);

        this.parseSamplers(context);
        this.scheduler = Executors.newScheduledThreadPool(1);

        this.scheduler.scheduleAtFixedRate(this, 1, 1, TimeUnit.SECONDS);

        // Indicates whether to write sub sample records to the database
        this.recordSubSamples = Boolean.parseBoolean(context.getParameter(KEY_RECORD_SUB_SAMPLES, "false"));
    }

    @Override
    public void teardownTest(BackendListenerContext context) throws Exception {
        LOGGER.info("Shutting down influxDB scheduler...");
        this.scheduler.shutdown();

        addVirtualUsersMetrics(0, 0, 0, 0, JMeterContextService.getThreadCounts().finishedThreads);

        Point teardownPoint = Point.measurement(TestStartEndMeasurement.MEASUREMENT_NAME).time(System.currentTimeMillis(), TimeUnit.MILLISECONDS)
                .tag(TestStartEndMeasurement.Tags.TYPE, TestStartEndMeasurement.Values.FINISHED)
                .tag(TestStartEndMeasurement.Tags.NODE_NAME, this.nodeName)
                .tag(TestStartEndMeasurement.Tags.RUN_ID, this.runId)
                .tag(TestStartEndMeasurement.Tags.TEST_NAME, this.testName)
                .addField(TestStartEndMeasurement.Fields.PLACEHOLDER, "1")
                .build();

        this.influxDatabaseClient.write(teardownPoint);
        this.influxDatabaseClient.disableBatch();

        try {
            this.scheduler.awaitTermination(30, TimeUnit.SECONDS);
            LOGGER.info("influxDB scheduler terminated!");
        } catch (InterruptedException e) {
            LOGGER.error("Error waiting for end of scheduler " + e);
        }

        this.samplersToFilter.clear();
        super.teardownTest(context);
    }

    /**
     * Periodically writes virtual users metrics to influxDB.
     */
    public void run() {
        ThreadCounts tc = JMeterContextService.getThreadCounts();

        this.addVirtualUsersMetrics(getUserMetrics().getMinActiveThreads(),
                getUserMetrics().getMeanActiveThreads(),
                getUserMetrics().getMaxActiveThreads(),
                tc.startedThreads,
                tc.finishedThreads);
    }

    /**
     * Setup influxDB client.
     *
     * @param context {@link BackendListenerContext}.
     */
    private void setupInfluxClient(BackendListenerContext context) {

        this.influxDatabaseClient = new InfluxDatabaseClient(context, LOGGER);
        this.influxDatabaseClient.setupInfluxClient();

        // create database from the context
        this.influxDatabaseClient.createDatabaseIfNotExistent();
    }

    /**
     * Parses list of samplers.
     *
     * @param context {@link BackendListenerContext}.
     */
    private void parseSamplers(BackendListenerContext context) {

        //List of samplers to record.
        String samplersList = context.getParameter(KEY_SAMPLERS_LIST, "");
        this.samplersToFilter = new HashSet<>();

        if (context.getBooleanParameter(KEY_USE_REGEX_FOR_SAMPLER_LIST, false)) {
            this.regexForSamplerList = samplersList;
        } else {
            this.regexForSamplerList = null;
            String[] samplers = samplersList.split(SEPARATOR);

            this.samplersToFilter = new HashSet<>();
            Collections.addAll(this.samplersToFilter, samplers);
        }
    }

    /**
     * Writes thread metrics.
     */
    private void addVirtualUsersMetrics(int minActiveThreads, int meanActiveThreads, int maxActiveThreads, int startedThreads, int finishedThreads) {
        Point virtualUsersMetricsPoint = Point.measurement(VirtualUsersMeasurement.MEASUREMENT_NAME).time(System.currentTimeMillis(), TimeUnit.MILLISECONDS)
        .addField(VirtualUsersMeasurement.Fields.MIN_ACTIVE_THREADS, minActiveThreads)
        .addField(VirtualUsersMeasurement.Fields.MAX_ACTIVE_THREADS, maxActiveThreads)
        .addField(VirtualUsersMeasurement.Fields.MEAN_ACTIVE_THREADS, meanActiveThreads)
        .addField(VirtualUsersMeasurement.Fields.STARTED_THREADS, startedThreads)
        .addField(VirtualUsersMeasurement.Fields.FINISHED_THREADS, finishedThreads)
        .tag(VirtualUsersMeasurement.Tags.NODE_NAME, this.nodeName)
        .tag(VirtualUsersMeasurement.Tags.TEST_NAME, this.testName)
        .tag(VirtualUsersMeasurement.Tags.RUN_ID, this.runId).build();

        this.influxDatabaseClient.write(virtualUsersMetricsPoint);
    }

    /**
     * Try to get a unique number for the sampler thread.
     */
    private int getUniqueNumberForTheSamplerThread() {

        return randomNumberGenerator.nextInt(ONE_MS_IN_NANOSECONDS);
    }
}
