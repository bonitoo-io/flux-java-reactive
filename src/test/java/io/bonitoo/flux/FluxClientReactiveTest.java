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

import okhttp3.logging.HttpLoggingInterceptor;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.platform.runner.JUnitPlatform;
import org.junit.runner.RunWith;

/**
 * @author Jakub Bednar (bednar@github) (26/06/2018 13:13)
 */
@RunWith(JUnitPlatform.class)
class FluxClientReactiveTest extends AbstractFluxClientReactiveTest {

    @Test
    void gzip() {

        // default disabled
        Assertions.assertThat(fluxClient.isGzipEnabled()).isEqualTo(false);

        // enable
        fluxClient.enableGzip();
        Assertions.assertThat(fluxClient.isGzipEnabled()).isEqualTo(true);

        // disable
        fluxClient.disableGzip();
        Assertions.assertThat(fluxClient.isGzipEnabled()).isEqualTo(false);
    }

    @Test
    void logLevel() {

        // default NONE
        Assertions.assertThat(fluxClient.getLogLevel()).isEqualTo(HttpLoggingInterceptor.Level.NONE);

        // set HEADERS
        fluxClient.setLogLevel(HttpLoggingInterceptor.Level.HEADERS);

        Assertions.assertThat(fluxClient.getLogLevel()).isEqualTo(HttpLoggingInterceptor.Level.HEADERS);
    }

    @Test
    void close() {
        Assertions.assertThat(fluxClient.isClosed()).isFalse();

        fluxClient.close();

        Assertions.assertThat(fluxClient.isClosed()).isTrue();
    }
}