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

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.TimeUnit;

import io.bonitoo.flux.dto.FluxRecord;
import io.bonitoo.flux.operator.restriction.Restrictions;

import io.reactivex.Flowable;
import org.assertj.core.api.Assertions;
import org.influxdb.annotation.Column;
import org.influxdb.annotation.Measurement;
import org.influxdb.dto.Point;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledIfSystemProperty;
import org.junit.platform.runner.JUnitPlatform;
import org.junit.runner.RunWith;

/**
 * @author Jakub Bednar (bednar@github) (28/06/2018 07:59)
 */
@RunWith(JUnitPlatform.class)
class ITFluxClientReactive extends AbstractITFluxClientReactive {

    @BeforeEach
    void prepareDate() {
        Point point1 = Point.measurement("mem")
                .tag("host", "A").tag("region", "west")
                .addField("free", 10)
                .time(10, TimeUnit.SECONDS)
                .build();
        Point point2 = Point.measurement("mem")
                .tag("host", "A").tag("region", "west")
                .addField("free", 11)
                .time(20, TimeUnit.SECONDS)
                .build();

        Point point3 = Point.measurement("mem")
                .tag("host", "B").tag("region", "west")
                .addField("free", 20)
                .time(10, TimeUnit.SECONDS)
                .build();
        Point point4 = Point.measurement("mem")
                .tag("host", "B").tag("region", "west")
                .addField("free", 22)
                .time(20, TimeUnit.SECONDS)
                .build();

        Point point5 = Point.measurement("cpu")
                .tag("host", "A").tag("region", "west")
                .addField("user_usage", 45)
                .addField("usage_system", 35)
                .time(10, TimeUnit.SECONDS)
                .build();
        Point point6 = Point.measurement("cpu")
                .tag("host", "A").tag("region", "west")
                .addField("user_usage", 49)
                .addField("usage_system", 38)
                .time(20, TimeUnit.SECONDS)
                .build();

        influxDB.write(point1);
        influxDB.write(point2);
        influxDB.write(point3);
        influxDB.write(point4);
        influxDB.write(point5);
        influxDB.write(point6);

        waitToFlux();
    }

    @Test
    void oneToOneTable() {

        //
        // CURL
        //
        // curl -i -XPOST --data-urlencode 'q=from(bucket: "flux_database") |> range(start:0) |>
        // filter(fn:(r) => r._measurement == "mem" and r._field == "free") |> sum()'
        // --data-urlencode "orgName=0" http://localhost:8093/v1/query

        Restrictions restriction = Restrictions
                .and(Restrictions.measurement().equal("mem"), Restrictions.field().equal("free"));

        Flux flux = Flux.from(DATABASE_NAME)
                .range(Instant.EPOCH)
                .filter(restriction)
                .sum();

        Flowable<FluxRecord> results = fluxClient.flux(flux);

        results
                .test()
                .assertValueCount(2)
                .assertValueAt(0, fluxRecord -> {

                    Assertions.assertThat(fluxRecord.getMeasurement()).isEqualTo("mem");
                    Assertions.assertThat(fluxRecord.getField()).isEqualTo("free");

                    Assertions.assertThat(fluxRecord.getStart()).isEqualTo(Instant.EPOCH);
                    Assertions.assertThat(fluxRecord.getStop()).isNotNull();
                    Assertions.assertThat(fluxRecord.getTime()).isNull();

                    Assertions.assertThat(fluxRecord.getValue()).isEqualTo(21L);

                    Assertions.assertThat(fluxRecord.getValues())
                            .hasEntrySatisfying("host", value -> Assertions.assertThat(value).isEqualTo("A"))
                            .hasEntrySatisfying("region", value -> Assertions.assertThat(value).isEqualTo("west"));

                    return true;
                })
                .assertValueAt(1, fluxRecord -> {

                    // Record 2
                    Assertions.assertThat(fluxRecord.getMeasurement()).isEqualTo("mem");
                    Assertions.assertThat(fluxRecord.getField()).isEqualTo("free");

                    Assertions.assertThat(fluxRecord.getStart()).isEqualTo(Instant.EPOCH);
                    Assertions.assertThat(fluxRecord.getStop()).isNotNull();
                    Assertions.assertThat(fluxRecord.getTime()).isNull();

                    Assertions.assertThat(fluxRecord.getValue()).isEqualTo(42L);

                    Assertions.assertThat(fluxRecord.getValues())
                            .hasEntrySatisfying("host", value -> Assertions.assertThat(value).isEqualTo("B"))
                            .hasEntrySatisfying("region", value -> Assertions.assertThat(value).isEqualTo("west"));

                    return true;
                });
    }

    @Test
    void oneToManyTable() {

        //
        // CURL
        //
        // curl -i -XPOST --data-urlencode 'q=from(bucket: "flux_database") |> range(start:0) |>
        // filter(fn:(r) => r._measurement == "mem" and r._field == "free") |> window(every:10s)'
        //  --data-urlencode "orgName=0" http://localhost:8093/v1/query

        // #datatype,string,long,dateTime:RFC3339,dateTime:RFC3339,dateTime:RFC3339,long,string,string,string,string
        // #group,false,false,true,true,false,false,true,true,true,true
        // #default,_result,,,,,,,,,
        // ,result,table,_start,_stop,_time,_value,_field,_measurement,host,region
        // ,,0,1970-01-01T00:00:10Z,1970-01-01T00:00:20Z,1970-01-01T00:00:10Z,10,free,mem,A,west
        // ,,1,1970-01-01T00:00:10Z,1970-01-01T00:00:20Z,1970-01-01T00:00:10Z,20,free,mem,B,west
        // ,,2,1970-01-01T00:00:20Z,1970-01-01T00:00:30Z,1970-01-01T00:00:20Z,11,free,mem,A,west
        // ,,3,1970-01-01T00:00:20Z,1970-01-01T00:00:30Z,1970-01-01T00:00:20Z,22,free,mem,B,west

        Restrictions restriction = Restrictions
                .and(Restrictions.measurement().equal("mem"), Restrictions.field().equal("free"));

        Flux flux = Flux.from(DATABASE_NAME)
                .range(Instant.EPOCH)
                .filter(restriction)
                .window(10L, ChronoUnit.SECONDS);

        Flowable<FluxRecord> results = fluxClient.flux(flux);

        results
                .test()
                .assertValueCount(4)
                .assertValueAt(0, fluxRecord -> {

                    Assertions.assertThat(fluxRecord.getMeasurement()).isEqualTo("mem");
                    Assertions.assertThat(fluxRecord.getField()).isEqualTo("free");

                    Assertions.assertThat(fluxRecord.getValues())
                            .hasEntrySatisfying("host", value -> Assertions.assertThat(value).isEqualTo("A"))
                            .hasEntrySatisfying("region", value -> Assertions.assertThat(value).isEqualTo("west"));

                    Assertions.assertThat(fluxRecord.getValue()).isEqualTo(10L);

                    return true;
                })
                .assertValueAt(1, fluxRecord -> {

                    Assertions.assertThat(fluxRecord.getMeasurement()).isEqualTo("mem");
                    Assertions.assertThat(fluxRecord.getField()).isEqualTo("free");

                    Assertions.assertThat(fluxRecord.getValues())
                            .hasEntrySatisfying("host", value -> Assertions.assertThat(value).isEqualTo("B"))
                            .hasEntrySatisfying("region", value -> Assertions.assertThat(value).isEqualTo("west"));

                    Assertions.assertThat(fluxRecord.getValue()).isEqualTo(20L);

                    return true;
                })
                .assertValueAt(2, fluxRecord -> {


                    Assertions.assertThat(fluxRecord.getMeasurement()).isEqualTo("mem");
                    Assertions.assertThat(fluxRecord.getField()).isEqualTo("free");

                    Assertions.assertThat(fluxRecord.getValues())
                            .hasEntrySatisfying("host", value -> Assertions.assertThat(value).isEqualTo("A"))
                            .hasEntrySatisfying("region", value -> Assertions.assertThat(value).isEqualTo("west"));

                    Assertions.assertThat(fluxRecord.getValue()).isEqualTo(11L);

                    return true;
                })
                .assertValueAt(3, fluxRecord -> {

                    Assertions.assertThat(fluxRecord.getMeasurement()).isEqualTo("mem");
                    Assertions.assertThat(fluxRecord.getField()).isEqualTo("free");

                    Assertions.assertThat(fluxRecord.getValues())
                            .hasEntrySatisfying("host", value -> Assertions.assertThat(value).isEqualTo("B"))
                            .hasEntrySatisfying("region", value -> Assertions.assertThat(value).isEqualTo("west"));

                    Assertions.assertThat(fluxRecord.getValue()).isEqualTo(22L);

                    return true;
                });
    }

    @Test
    void manyToOne() {

        //
        // CURL
        //
        // curl -i -XPOST --data-urlencode 'q=from(bucket: "flux_database") |> range(start:0)
        // |> filter(fn:(r) => r._measurement == "mem" and r._field == "free") |> window(every:10s)
        // |> group(by:["region"])' --data-urlencode "orgName=0" http://localhost:8093/v1/query

        // #datatype,string,long,dateTime:RFC3339,dateTime:RFC3339,dateTime:RFC3339,long,string,string,string,string
        // #group,false,false,false,false,false,false,false,false,false,true
        // #default,_result,,,,,,,,,
        // ,result,table,_start,_stop,_time,_value,_field,_measurement,host,region
        // ,,0,1970-01-01T00:00:10Z,1970-01-01T00:00:20Z,1970-01-01T00:00:10Z,10,free,mem,A,west
        // ,,0,1970-01-01T00:00:10Z,1970-01-01T00:00:20Z,1970-01-01T00:00:10Z,20,free,mem,B,west
        // ,,0,1970-01-01T00:00:20Z,1970-01-01T00:00:30Z,1970-01-01T00:00:20Z,11,free,mem,A,west
        // ,,0,1970-01-01T00:00:20Z,1970-01-01T00:00:30Z,1970-01-01T00:00:20Z,22,free,mem,B,west

        Restrictions restriction = Restrictions
                .and(Restrictions.measurement().equal("mem"), Restrictions.field().equal("free"));

        Flux flux = Flux.from(DATABASE_NAME)
                .range(Instant.EPOCH)
                .filter(restriction)
                .window(10L, ChronoUnit.SECONDS)
                .groupBy("region");

        Flowable<FluxRecord> results = fluxClient.flux(flux);

        results
                .test()
                .assertValueCount(4)
                .assertValueAt(0, fluxRecord -> {

                    // Record1
                    Assertions.assertThat(fluxRecord.getMeasurement()).isEqualTo("mem");
                    Assertions.assertThat(fluxRecord.getField()).isEqualTo("free");

                    Assertions.assertThat(fluxRecord.getValues())
                            .hasEntrySatisfying("host", value -> Assertions.assertThat(value).isEqualTo("A"))
                            .hasEntrySatisfying("region", value -> Assertions.assertThat(value).isEqualTo("west"));

                    Assertions.assertThat(fluxRecord.getValue()).isEqualTo(10L);

                    return true;
                })
                .assertValueAt(1, fluxRecord -> {

                    Assertions.assertThat(fluxRecord.getMeasurement()).isEqualTo("mem");
                    Assertions.assertThat(fluxRecord.getField()).isEqualTo("free");

                    Assertions.assertThat(fluxRecord.getValues())
                            .hasEntrySatisfying("host", value -> Assertions.assertThat(value).isEqualTo("B"))
                            .hasEntrySatisfying("region", value -> Assertions.assertThat(value).isEqualTo("west"));

                    Assertions.assertThat(fluxRecord.getValue()).isEqualTo(20L);

                    return true;
                })
                .assertValueAt(2, fluxRecord -> {

                    // Record3
                    Assertions.assertThat(fluxRecord.getMeasurement()).isEqualTo("mem");
                    Assertions.assertThat(fluxRecord.getField()).isEqualTo("free");

                    Assertions.assertThat(fluxRecord.getValues())
                            .hasEntrySatisfying("host", value -> Assertions.assertThat(value).isEqualTo("A"))
                            .hasEntrySatisfying("region", value -> Assertions.assertThat(value).isEqualTo("west"));

                    Assertions.assertThat(fluxRecord.getValue()).isEqualTo(11L);

                    return true;
                })
                .assertValueAt(3, fluxRecord -> {

                    // Record4
                    Assertions.assertThat(fluxRecord.getMeasurement()).isEqualTo("mem");
                    Assertions.assertThat(fluxRecord.getField()).isEqualTo("free");

                    Assertions.assertThat(fluxRecord.getValues())
                            .hasEntrySatisfying("host", value -> Assertions.assertThat(value).isEqualTo("B"))
                            .hasEntrySatisfying("region", value -> Assertions.assertThat(value).isEqualTo("west"));

                    Assertions.assertThat(fluxRecord.getValue()).isEqualTo(22L);

                    return true;
                })
        ;
    }

    @Test
    @DisabledIfSystemProperty(named = "FLUX_DISABLE", matches = "true")
    void ping() {

        fluxClient
                .ping()
                .test()
                .assertValue(true);
    }

    @Measurement(name = "mem")
    public static class Memory {

        @Column(name = "time")
        private Instant time;

        @Column(name = "free")
        private Long free;

        @Column(name = "host", tag = true)
        private String host;

        @Column(name = "region", tag = true)
        private String region;
    }
}
