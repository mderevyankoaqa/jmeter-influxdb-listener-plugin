package org.md.jmeter.influxdb.visualizer.config;

/**
 * Constants (Tag, Field, Measurement) names for the requests measurement.
 *
 * @author Alexander Wert
 * @author Mikhail Derevyanko (minor changes and improvements)
 */
public interface RequestMeasurement {

	/**
	 * Measurement name.
	 */
	String MEASUREMENT_NAME = "requestsRaw";

	/**
	 * Tags.
	 *
	 * @author Alexander Wert
	 * @author Mikhail Derevyanko (minor changes and improvements)
	 */
	interface Tags {
		/**
		 * Request name tag.
		 */
		String REQUEST_NAME = "requestName";

		/**
		 * Influx DB tag for a unique identifier for each execution(aka 'run') of a load test.
		 */
		String RUN_ID = "runId";

		/**
		 * Test name field.
		 */
		String TEST_NAME = "testName";

		/**
		 * Node name field.
		 */
		String NODE_NAME = "nodeName";

		/**
		 * Response code field.
		 */
		String RESULT_CODE = "responseCode";

		/**
		 * Error message.
		 */
		String ERROR_MSG = "errorMessage";

		/**
		 * Error response body.
		 */
		String ERROR_RESPONSE_BODY = "errorResponseBody";
	}

	/**
	 * Fields.
	 *
	 * @author Alexander Wert
	 */
	interface Fields {
		/**
		 * Response time field.
		 */
		String RESPONSE_TIME = "responseTime";

		/**
		 * Error count field.
		 */
		String ERROR_COUNT = "errorCount";

		/**
		 * Error count field.
		 */
		String REQUEST_COUNT = "count";

		/**
		 * Thread name field.
		 */
		String THREAD_NAME = "threadName";


		/**
		 * Node name field.
		 */
		String SENT_BYTES = "sentBytes";


		/**
		 * Node name field.
		 */
		String RECEIVED_BYTES = "receivedBytes";


		/**
		 * Node name field.
		 */
		String HITS = "hits";

	}
}
