package org.md.jmeter.influxdb.visualizer.influxdb.client;

import org.md.jmeter.influxdb.visualizer.config.InfluxDBConfig;
import okhttp3.OkHttpClient;
import org.apache.jmeter.visualizers.backend.BackendListenerContext;
import org.influxdb.BatchOptions;
import org.influxdb.InfluxDB;
import org.influxdb.InfluxDBFactory;
import org.influxdb.dto.Point;
import org.influxdb.dto.Query;
import org.influxdb.dto.QueryResult;
import org.influxdb.impl.Preconditions;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * The client to work with Influx DB 1.8 or less.
 *
 *  @author Mikhail Derevyanko
 */
public class InfluxDatabaseClient {

    private static org.slf4j.Logger LOGGER;
    private final InfluxDBConfig influxDBConfig;
    private InfluxDB influxDB;

    /**
     * Creates a new instance of the @link InfluxDatabaseClient.
     * @param context {@link BackendListenerContext}
     * @param logger {@link Logger}
     */
    public InfluxDatabaseClient(BackendListenerContext context, Logger logger) {

        LOGGER = logger;
        this.influxDBConfig = new InfluxDBConfig(context);
    }

    /**
     * Creates the Influx DB client instance.
     */
    public void setupInfluxClient() {

        OkHttpClient.Builder httpClient = new OkHttpClient.Builder()
                .connectTimeout(1, TimeUnit.MINUTES)
                .readTimeout(1, TimeUnit.MINUTES)
                .writeTimeout(1, TimeUnit.MINUTES)
                .retryOnConnectionFailure(true);

        // check that secure service is going to be used
        if (this.influxDBConfig.getInfluxUser() != null && !this.influxDBConfig.getInfluxUser().isEmpty()) {

            Preconditions.checkNonEmptyString(this.influxDBConfig.getInfluxPassword(), "password");
            this.influxDB = InfluxDBFactory.connect(this.influxDBConfig.getInfluxDBURL(), this.influxDBConfig.getInfluxUser(), this.influxDBConfig.getInfluxPassword(), httpClient);
            LOGGER.info("Influx db client has been created to use login and password!");
        }
        else {
            this.influxDB = InfluxDBFactory.connect(this.influxDBConfig.getInfluxDBURL(), httpClient);
            LOGGER.info("Influx db client has been created!");
        }

        this.influxDB.enableBatch(BatchOptions.DEFAULTS);
    }

    /**
     * Creates the influxdb instance if it does not exist, the db name used from {@link InfluxDBConfig}.
     */
    public void createDatabaseIfNotExistent() {
        List<String> dbNames = this.getAllDatabases();

        if (!dbNames.contains(influxDBConfig.getInfluxDatabase())) {
            this.createDatabase(influxDBConfig.getInfluxDatabase());
        }
    }

    /**
     * Gets all Influx DB names.
     * @return the list of the DB names.
     */
    public List<String> getAllDatabases() {

        QueryResult result = this.influxDB.query(new Query("SHOW DATABASES"));

        List<List<Object>> databaseNames = result.getResults().get(0).getSeries().get(0).getValues();
        List<String> databases = new ArrayList<>();
        if (databaseNames != null) {
            for (List<Object> database : databaseNames) {
                databases.add(database.get(0).toString());
            }
        }
        return databases;
    }

    /**
     * Creates database.
     * @param name the DB name.
     */
    public void createDatabase(final String name) {
        Preconditions.checkNonEmptyString(name, "name");
        String createDatabaseQueryString = String.format("CREATE DATABASE \"%s\"", name);

        try {
            this.influxDB.query(new Query(createDatabaseQueryString));
        }
        catch (Exception e)
        {
            LOGGER.error("Failed to create database: " + name, e);
        }
    }

    /**
     * Writes the {@link Point}.
     * @param point the Influxdb {@link Point}.
     */
    public void write(Point point)
    {
        try {
            this.influxDB.write(influxDBConfig.getInfluxDatabase(), influxDBConfig.getInfluxRetentionPolicy(), point);
        }
        catch (Exception e)
        {
            LOGGER.error("Failed writing to influx", e);
        }
    }

    /**
     * Disables batch.
     */
    public void disableBatch()
    {
        this.influxDB.disableBatch();
    }
}
