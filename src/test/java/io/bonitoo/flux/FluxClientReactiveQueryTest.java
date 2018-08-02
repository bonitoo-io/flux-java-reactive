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
import java.util.HashMap;
import javax.annotation.Nonnull;

import io.bonitoo.flux.events.FluxErrorEvent;
import io.bonitoo.flux.events.FluxSuccessEvent;
import io.bonitoo.flux.mapper.FluxResult;
import io.bonitoo.flux.mapper.Record;
import io.bonitoo.flux.options.FluxCsvParserOptions;
import io.bonitoo.flux.options.FluxOptions;
import io.bonitoo.flux.options.query.TaskOption;

import io.reactivex.Flowable;
import io.reactivex.observers.TestObserver;
import okhttp3.mockwebserver.MockResponse;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.platform.runner.JUnitPlatform;
import org.junit.runner.RunWith;

/**
 * @author Jakub Bednar (bednar@github) (26/06/2018 13:54)
 */
@RunWith(JUnitPlatform.class)
class FluxClientReactiveQueryTest extends AbstractFluxClientReactiveTest {

    @Test
    void successFluxResponseEvent() {

        fluxServer.enqueue(createResponse());

        TestObserver<FluxSuccessEvent> listener = fluxClient
                .listenEvents(FluxSuccessEvent.class)
                .test();

        Flowable<FluxResult> results = fluxClient.flux(Flux.from("flux_database"));
        results
                .take(1)
                .test()
                .assertValueCount(1);

        listener.assertValueCount(1).assertValue(event -> {

            Assertions.assertThat(event.getFluxQuery()).isEqualTo("from(db:\"flux_database\")");
            Assertions.assertThat(event.getOptions()).isNotNull();

            return true;
        });
    }

    @Test
    void queryWithOptions() {

        fluxServer.enqueue(createResponse());

        TestObserver<FluxSuccessEvent> listener = fluxClient
                .listenEvents(FluxSuccessEvent.class)
                .test();

        TaskOption task = TaskOption.builder("foo")
                .every(1L, ChronoUnit.HOURS)
                .delay(10L, ChronoUnit.MINUTES)
                .cron("0 2 * * *")
                .retry(5)
                .build();

        FluxOptions options = FluxOptions.builder().addOption(task).build();

        Flowable<FluxResult> results = fluxClient.flux(Flux.from("flux_database"), options);
        results
                .take(1)
                .test()
                .assertValueCount(1);

        listener.assertValueCount(1).assertValue(event -> {

            String expected = "option task = {name: \"foo\", every: 1h, delay: 10m, cron: \"0 2 * * *\", retry: 5} "
                    + "from(db:\"flux_database\")";

            Assertions.assertThat(event.getFluxQuery()).isEqualToIgnoringWhitespace(expected);
            Assertions.assertThat(event.getOptions()).isNotNull();

            return true;
        });
    }

    @Test
    void queryWithParameters() throws InterruptedException {

        fluxServer.enqueue(createResponse());

        HashMap<String, Object> properties = new HashMap<>();
        properties.put("n", 5);

        Flowable<FluxResult> results = fluxClient.flux(Flux.from("flux_database").limit().withPropertyNamed("n"), properties);
        results
                .take(1)
                .test()
                .assertValueCount(1);

        Assertions.assertThat(fluxServer.takeRequest().getRequestUrl().queryParameter("q"))
                .isEqualTo("from(db:\"flux_database\")|> limit(n: 5)");
    }

    @Test
    void queryPublisherWithParameters() throws InterruptedException {

        fluxServer.enqueue(createResponse());

        HashMap<String, Object> properties = new HashMap<>();
        properties.put("n", 5);

        Flux query = Flux.from("flux_database").limit().withPropertyNamed("n");
        Flowable<FluxResult> results = fluxClient.flux(Flowable.just(query), properties);
        results
                .take(1)
                .test()
                .assertValueCount(1);

        Assertions.assertThat(fluxServer.takeRequest().getRequestUrl().queryParameter("q"))
                .isEqualTo("from(db:\"flux_database\")|> limit(n: 5)");
    }

    @Test
    void stringQuery() throws InterruptedException {

        fluxServer.enqueue(createResponse());

        String query = "from(db:\"telegraf\") |> " +
                "filter(fn: (r) => r[\"_measurement\"] == \"cpu\" AND r[\"_field\"] == \"usage_user\") |> sum()";

        Flowable<FluxResult> results = fluxClient.flux(query);
        results
                .take(1)
                .test()
                .assertValueCount(1);

        Assertions.assertThat(fluxServer.takeRequest().getRequestUrl().queryParameter("q")).isEqualTo(query);
    }

    @Test
    void errorFluxResponseEvent() {

        String influxDBError = "must pass organization name or ID as string in orgName or orgID parameter";
        fluxServer.enqueue(createErrorResponse(influxDBError));

        TestObserver<FluxErrorEvent> listener = fluxClient
                .listenEvents(FluxErrorEvent.class)
                .test();

        Flowable<FluxResult> results = fluxClient.flux(Flux.from("flux_database"));
        results
                .take(1)
                .test()
                .assertError(FluxException.class)
                .assertErrorMessage(influxDBError);

        listener
                .assertValueCount(1)
                .assertValue(event -> {

                    Assertions.assertThat(event.getFluxQuery()).isEqualTo("from(db:\"flux_database\")");
                    Assertions.assertThat(event.getOptions()).isNotNull();
                    Assertions.assertThat(event.getException()).isInstanceOf(FluxException.class);
                    Assertions.assertThat(event.getException()).hasMessage(influxDBError);

                    return true;
                });
    }

    @Test
    void parsingToFluxResult() {

        fluxServer.enqueue(createResponse());

        Flowable<FluxResult> results = fluxClient.flux(Flux.from("flux_database"));
        results
                .take(1)
                .test()
                .assertValueCount(1)
                .assertValue(fluxResult -> {

                    Assertions.assertThat(fluxResult).isNotNull();
                    return true;
                });
    }

    @Test
    void parsingToFluxResultMultiTable() {

        fluxServer.enqueue(createMultiTableResponse());

        Flowable<FluxResult> results = fluxClient.flux(Flux.from("flux_database"));
        results
                .take(1)
                .test()
                .assertValueCount(1)
                .assertValue(fluxResult -> {

                    Assertions.assertThat(fluxResult).isNotNull();
                    Assertions.assertThat(fluxResult.getTables().size() == 4).isTrue();

                    {
                        Record rec = fluxResult.getTables().get(0).getRecords().get(0);

                        Assertions.assertThat(rec.getValue()).isEqualTo(50d);
                        Assertions.assertThat(rec.getField()).isEqualTo("cpu_usage");

                        Assertions.assertThat(rec.getStart()).isEqualTo(Instant.parse("1677-09-21T00:12:43.145224192Z"));
                        Assertions.assertThat(rec.getStop()).isEqualTo(Instant.parse("2018-06-27T06:22:38.347437344Z"));
                        Assertions.assertThat(rec.getTime()).isEqualTo(Instant.parse("2018-06-27T05:56:40.001Z"));

                        Assertions.assertThat(rec.getMeasurement()).isEqualTo("server_performance");
                        Assertions.assertThat(rec.getTags().get("location")).isEqualTo("Area 1° 10' \"20");
                        Assertions.assertThat(rec.getTags().get("production_usage")).isEqualTo("false");
                    }

                    {
                        Record rec = fluxResult.getTables().get(2).getRecords().get(0);

                        Assertions.assertThat(rec.getValue()).isEqualTo("Server no. 1");
                        Assertions.assertThat(rec.getField()).isEqualTo("server description");

                        Assertions.assertThat(rec.getStart()).isEqualTo(Instant.parse("1677-09-21T00:12:43.145224192Z"));
                        Assertions.assertThat(rec.getStop()).isEqualTo(Instant.parse("2018-06-27T06:22:38.347437344Z"));
                        Assertions.assertThat(rec.getTime()).isEqualTo(Instant.parse("2018-06-27T05:56:40.001Z"));

                        Assertions.assertThat(rec.getMeasurement()).isEqualTo("server_performance");
                        Assertions.assertThat(rec.getTags().get("location")).isEqualTo("Area 1° 10' \"20");
                        Assertions.assertThat(rec.getTags().get("production_usage")).isEqualTo("false");
                    }

                    return true;
                });
    }

    @Test
    void parsingToFluxResultMultiRecordValues() {

        String data = "#datatype,string,long,dateTime:RFC3339,dateTime:RFC3339,string,string,string,string,long,long,string\n"
                + "#group,false,false,true,true,true,true,true,true,false,false,false\n"
                + "#default,_result,,,,,,,,,,\n"
                + ",result,table,_start,_stop,_field,_measurement,host,region,_value2,value1,value_str\n"
                + ",,0,1677-09-21T00:12:43.145224192Z,2018-07-16T11:21:02.547596934Z,free,mem,A,west,121,11,test";

        fluxServer.enqueue(createResponse(data));

        FluxCsvParserOptions parserOptions = FluxCsvParserOptions.builder()
                .valueDestinations("value1", "_value2", "value_str")
                .build();

        FluxOptions queryOptions = FluxOptions.builder()
                .parserOptions(parserOptions)
                .build();

        Flowable<FluxResult> results = fluxClient.flux(Flux.from("flux_database"), queryOptions);
        results
                .take(1)
                .test()
                .assertValueCount(1)
                .assertValue(fluxResult -> {

                    Assertions.assertThat(fluxResult.getTables().get(0).getRecords()).hasSize(1);

                    Record record = fluxResult.getTables().get(0).getRecords().get(0);
                    Assertions.assertThat(record.getTags()).hasSize(2);
                    Assertions.assertThat(record.getTags())
                            .hasEntrySatisfying("host", value -> Assertions.assertThat(value).isEqualTo("A"))
                            .hasEntrySatisfying("region", value -> Assertions.assertThat(value).isEqualTo("west"));
                    Assertions.assertThat(record.getValues()).hasSize(3);
                    Assertions.assertThat(record.getValue()).isEqualTo(11L);
                    Assertions.assertThat(record.getValues())
                            .hasEntrySatisfying("value1", value -> Assertions.assertThat(value).isEqualTo(11L))
                            .hasEntrySatisfying("_value2", value -> Assertions.assertThat(value).isEqualTo(121L))
                            .hasEntrySatisfying("value_str", value -> Assertions.assertThat(value).isEqualTo("test"));

                    return true;
                });
    }

    @Nonnull
    private MockResponse createMultiTableResponse() {

        String data =
                "#datatype,string,long,dateTime:RFC3339,dateTime:RFC3339,dateTime:RFC3339,double,string,string,string,string\n"
                        + "#group,false,false,true,true,false,false,true,true,true,true\n"
                        + "#default,_result,,,,,,,,,\n"
                        + ",result,table,_start,_stop,_time,_value,_field,_measurement,location,production_usage\n"
                        + ",,0,1677-09-21T00:12:43.145224192Z,2018-06-27T06:22:38.347437344Z,2018-06-27T05:56:40.001Z,50,cpu_usage,server_performance,\"Area 1° 10' \"\"20\",false\n"
                        + "\n"
                        + "#datatype,string,long,dateTime:RFC3339,dateTime:RFC3339,dateTime:RFC3339,long,string,string,string,string\n"
                        + "#group,false,false,true,true,false,false,true,true,true,true\n"
                        + "#default,_result,,,,,,,,,\n"
                        + ",result,table,_start,_stop,_time,_value,_field,_measurement,location,production_usage\n"
                        + ",,1,1677-09-21T00:12:43.145224192Z,2018-06-27T06:22:38.347437344Z,2018-06-27T05:56:40.001Z,1,rackNumber,server_performance,\"Area 1° 10' \"\"20\",false\n"
                        + "\n"
                        + "#datatype,string,long,dateTime:RFC3339,dateTime:RFC3339,dateTime:RFC3339,string,string,string,string,string\n"
                        + "#group,false,false,true,true,false,false,true,true,true,true\n"
                        + "#default,_result,,,,,,,,,\n"
                        + ",result,table,_start,_stop,_time,_value,_field,_measurement,location,production_usage\n"
                        + ",,2,1677-09-21T00:12:43.145224192Z,2018-06-27T06:22:38.347437344Z,2018-06-27T05:56:40.001Z,Server no. 1,server description,server_performance,\"Area 1° 10' \"\"20\",false\n"
                        + "\n"
                        + "#datatype,string,long,dateTime:RFC3339,dateTime:RFC3339,dateTime:RFC3339,long,string,string,string,string\n"
                        + "#group,false,false,true,true,false,false,true,true,true,true\n"
                        + "#default,_result,,,,,,,,,\n"
                        + ",result,table,_start,_stop,_time,_value,_field,_measurement,location,production_usage\n"
                        + ",,3,1677-09-21T00:12:43.145224192Z,2018-06-27T06:22:38.347437344Z,2018-06-27T05:56:40.001Z,10000,upTime,server_performance,\"Area 1° 10' \"\"20\",false";

        return createResponse(data);
    }

    @Nonnull
    private MockResponse createResponse() {
        String data = "#datatype,string,long,dateTime:RFC3339,dateTime:RFC3339,dateTime:RFC3339,string,string,double\n"
                + ",result,table,_start,_stop,_time,region,host,_value\n"
                + ",mean,0,2018-05-08T20:50:00Z,2018-05-08T20:51:00Z,2018-05-08T20:50:00Z,east,A,15.43\n"
                + ",mean,0,2018-05-08T20:50:00Z,2018-05-08T20:51:00Z,2018-05-08T20:50:20Z,east,B,59.25\n"
                + ",mean,0,2018-05-08T20:50:00Z,2018-05-08T20:51:00Z,2018-05-08T20:50:40Z,east,C,52.62\n"
                + ",mean,1,2018-05-08T20:50:00Z,2018-05-08T20:51:00Z,2018-05-08T20:50:00Z,west,A,62.73\n"
                + ",mean,1,2018-05-08T20:50:00Z,2018-05-08T20:51:00Z,2018-05-08T20:50:20Z,west,B,12.83\n"
                + ",mean,1,2018-05-08T20:50:00Z,2018-05-08T20:51:00Z,2018-05-08T20:50:40Z,west,C,51.62";

        return createResponse(data);
    }

    @Nonnull
    private MockResponse createResponse(final String data) {

        return new MockResponse()
                .setHeader("Content-Type", "text/csv; charset=utf-8")
                .setHeader("Date", "Tue, 26 Jun 2018 13:15:01 GMT")
                .setChunkedBody(data, data.length());
    }
}