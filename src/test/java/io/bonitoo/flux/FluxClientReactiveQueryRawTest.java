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

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import javax.annotation.Nullable;

import io.reactivex.Flowable;
import io.reactivex.Maybe;
import okhttp3.ResponseBody;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.platform.runner.JUnitPlatform;
import org.junit.runner.RunWith;
import retrofit2.Response;

/**
 * @author Jakub Bednar (bednar@github) (03/08/2018 14:40)
 */
@RunWith(JUnitPlatform.class)
class FluxClientReactiveQueryRawTest extends AbstractFluxClientReactiveTest {

    @Test
    void queryRaw() {

        fluxServer.enqueue(createResponse());

        Maybe<Response<ResponseBody>> result = fluxClient.fluxRaw(Flux.from("telegraf"));

        assertSuccessResult(result);
    }

    @Test
    void queryRawProperties() {

        fluxServer.enqueue(createResponse());

        Map<String, Object> properties = new HashMap<>();
        properties.put("n", 10);

        Flux query = Flux
                .from("telegraf")
                .count()
                .withPropertyNamed("n");

        Maybe<Response<ResponseBody>> result = fluxClient.fluxRaw(query, properties);

        assertSuccessResult(result);
    }

    @Test
    void queryRawPublisherProperties() {

        fluxServer.enqueue(createResponse());

        Map<String, Object> properties = new HashMap<>();
        properties.put("n", 10);

        Flux query = Flux
                .from("telegraf")
                .count()
                .withPropertyNamed("n");

        Flowable<Response<ResponseBody>> fluxRawResult = fluxClient.fluxRaw(Flowable.just(query), properties);
        fluxRawResult
                .test()
                .assertValueCount(1)
                .assertValue(this::assertSuccessResponse);
    }

    @Test
    void queryRawString() {

        fluxServer.enqueue(createResponse());

        String query = "from(bucket:\"telegraf\") |> " +
                "filter(fn: (r) => r[\"_measurement\"] == \"cpu\" AND r[\"_field\"] == \"usage_user\") |> sum()";

        Maybe<Response<ResponseBody>> result = fluxClient.fluxRaw(query);

        assertSuccessResult(result);
    }

    private void assertSuccessResult(@Nullable final Maybe<Response<ResponseBody>> fluxRawResult) {

        fluxRawResult
                .test()
                .assertValueCount(1)
                .assertValue(this::assertSuccessResponse);
    }

    private boolean assertSuccessResponse(@Nullable final Response<ResponseBody> response) {

        Assertions.assertThat(response).isNotNull();
        Assertions.assertThat(response.isSuccessful()).isTrue();
        Assertions.assertThat(response.code()).isEqualTo(200);
        Assertions.assertThat(response.body()).isNotNull();
        try {
            Assertions.assertThat(createResponse().getBody().readUtf8()).contains(response.body().string());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return true;
    }
}