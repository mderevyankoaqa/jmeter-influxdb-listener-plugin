package org.md.jmeter.influxdb.visualizer.influxdb.client;

/**
 * The utility to escape values before inserting.
 *
 *  @author Mikhail Derevyanko
 */
public class InfluxDatabaseUtility {

    /**
     * Updates not supported values.
     * @param value the string which is going to be updated.
     * @return the escaped string.
     */
    public static String getEscapedString(String value) {

        return value.replace("\n", "")
                .replace("\r", "")
                .replace(" ", "\\ ")
                .replace(",", ",\\ ")
                .replace("=", "=\\ ");
    }
}
