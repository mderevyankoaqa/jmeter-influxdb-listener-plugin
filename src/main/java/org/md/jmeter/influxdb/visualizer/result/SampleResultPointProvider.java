package org.md.jmeter.influxdb.visualizer.result;

import org.md.jmeter.influxdb.visualizer.influxdb.client.InfluxDatabaseUtility;
import org.md.jmeter.influxdb.visualizer.config.RequestMeasurement;
import org.apache.jmeter.visualizers.backend.SamplerMetric;
import org.influxdb.dto.Point;

/**
 * The provider of the Influxdb {@link Point} based on the sample result.
 *
 * @author Mikhail Derevyanko
 */
public class SampleResultPointProvider {

    private final SampleResultPointContext sampleResultContext;
    private final String assertionFailureMessage;

    private Point errorPoint;

    /**
     * Creates the new instance of the {@link SampleResultPointProvider}.
     * @param sampleResultContext the {@link SampleResultPointContext}.
     */
    public SampleResultPointProvider(SampleResultPointContext sampleResultContext) {
        this.sampleResultContext = sampleResultContext;

        this.assertionFailureMessage = this.sampleResultContext.getSampleResult().getFirstAssertionFailureMessage();
    }

    /**
     * Gets {@link Point}, returns the OK or KO jmeter point, depends from the sample result.
     * @return {@link Point} to save.
     */
    public Point getPoint() {

        if (this.assertionFailureMessage == null) {
            return this.getOKPointBuilder()
                    .build();
        } else {
            return this.getErrorPoint();
        }
    }

    /**
     * Gets KO jmeter {@link Point}, saves the assertion message and response error body - depends from the settings.
     * @return KO jmeter {@link Point}.
     */
    private Point getErrorPoint() {

        if (this.sampleResultContext.isErrorBodyToBeSaved()) {
            this.errorPoint = this.getOKPointBuilder()
                    .tag(RequestMeasurement.Tags.ERROR_MSG, this.assertionFailureMessage)
                    .tag(RequestMeasurement.Tags.ERROR_RESPONSE_BODY, this.getErrorBody())
                    .build();
        }

        if (!this.sampleResultContext.isErrorBodyToBeSaved()) {
            this.errorPoint = this.getOKPointBuilder()
                    .tag(RequestMeasurement.Tags.ERROR_MSG, this.assertionFailureMessage)
                    .build();

        }

        return this.errorPoint;
    }

    /**
     * Gets error body.
     * @return returns body of the failed response.
     */
    private String getErrorBody()
    {
        String errorBody = this.sampleResultContext.getSampleResult().getResponseDataAsString();
        if(errorBody != null && !errorBody.isEmpty())
        {
            return  InfluxDatabaseUtility.getEscapedString(errorBody);
        }

        return "ErrorBodyIsEmpty.";
    }

    /**
     * Builds the OK jmeter {@link Point}.
     * @return OK jmeter {@link Point}.
     */
    private Point.Builder getOKPointBuilder() {

        SamplerMetric samplerMetric = new SamplerMetric();
        samplerMetric.add(this.sampleResultContext.getSampleResult());

        return Point.measurement(RequestMeasurement.MEASUREMENT_NAME).time(this.sampleResultContext.getTimeToSet(), this.sampleResultContext.getPrecisionToSet())
                .tag(RequestMeasurement.Tags.REQUEST_NAME, this.sampleResultContext.getSampleResult().getSampleLabel())
                .tag(RequestMeasurement.Tags.RUN_ID, this.sampleResultContext.getRunId())
                .tag(RequestMeasurement.Tags.TEST_NAME, this.sampleResultContext.getTestName())
                .tag(RequestMeasurement.Tags.NODE_NAME, this.sampleResultContext.getNodeName())
                .tag(RequestMeasurement.Tags.RESULT_CODE, this.sampleResultContext.getSampleResult().getResponseCode())
                .addField(RequestMeasurement.Fields.ERROR_COUNT, this.sampleResultContext.getSampleResult().getErrorCount())
                .addField(RequestMeasurement.Fields.THREAD_NAME, this.sampleResultContext.getSampleResult().getThreadName())
                .addField(RequestMeasurement.Fields.REQUEST_COUNT, samplerMetric.getTotal())
                .addField(RequestMeasurement.Fields.RECEIVED_BYTES, samplerMetric.getReceivedBytes())
                .addField(RequestMeasurement.Fields.SENT_BYTES, samplerMetric.getSentBytes())
                .addField(RequestMeasurement.Fields.RESPONSE_TIME, this.sampleResultContext.getSampleResult().getTime())
                .addField(RequestMeasurement.Fields.HITS, samplerMetric.getHits());
    }
}
