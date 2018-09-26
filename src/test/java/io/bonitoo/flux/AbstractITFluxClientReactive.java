/*
 * The MIT License
 * Copyright © 2018
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package io.bonitoo.flux;

import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.Nonnull;

import io.bonitoo.flux.option.FluxConnectionOptions;

import org.influxdb.InfluxDB;
import org.influxdb.InfluxDBFactory;
import org.influxdb.dto.Query;
import org.influxdb.dto.QueryResult;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;

/**
 * @author Jakub Bednar (bednar@github) (28/06/2018 08:20)
 */
public abstract class AbstractITFluxClientReactive {

    private static final Logger LOG = Logger.getLogger(AbstractITFluxClientReactive.class.getName());

    protected static final String DATABASE_NAME = "flux_database";

    protected FluxClientReactive fluxClient;
    protected InfluxDB influxDB;

    @BeforeEach
    protected void setUp() {

        String fluxIP = System.getenv().getOrDefault("FLUX_IP", "127.0.0.1");
        String fluxPort = System.getenv().getOrDefault("FLUX_PORT_API", "8093");
        String fluxURL = "http://" + fluxIP + ":" + fluxPort;
        LOG.log(Level.FINEST, "Flux URL: {0}", fluxURL);

        FluxConnectionOptions options = FluxConnectionOptions.builder()
                .url(fluxURL)
                .orgID("00")
                .build();

        fluxClient = FluxClientReactiveFactory.connect(options);

        String influxdbIP = System.getenv().getOrDefault("INFLUXDB_IP", "127.0.0.1");
        String influxdbPort = System.getenv().getOrDefault("INFLUXDB_PORT_API", "8086");
        String influxURL = "http://" + influxdbIP + ":" + influxdbPort;
        LOG.log(Level.FINEST, "Influx URL: {0}", influxURL);

        influxDB = InfluxDBFactory.connect(influxURL);
        influxDB.setDatabase(DATABASE_NAME);

        simpleQuery("CREATE DATABASE " + DATABASE_NAME);
    }

    @AfterEach
    protected void after() {

        simpleQuery("DROP DATABASE " + DATABASE_NAME);

        fluxClient.close();
    }

    protected void waitToFlux() {
        try {
            Thread.sleep(1_000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    private void simpleQuery(@Nonnull final String simpleQuery) {

        Objects.requireNonNull(simpleQuery, "SimpleQuery is required");
        QueryResult result = influxDB.query(new Query(simpleQuery, DATABASE_NAME));

        LOG.log(Level.FINEST, "Simple query: {0} result: {1}", new Object[]{simpleQuery, result});
    }
}