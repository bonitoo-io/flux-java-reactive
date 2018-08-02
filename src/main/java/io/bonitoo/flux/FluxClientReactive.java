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

import java.util.Map;
import javax.annotation.Nonnull;

import io.bonitoo.flux.events.AbstractFluxEvent;
import io.bonitoo.flux.mapper.FluxResult;
import io.bonitoo.flux.options.FluxOptions;

import io.reactivex.Flowable;
import io.reactivex.Maybe;
import io.reactivex.Observable;
import io.reactivex.annotations.Experimental;
import okhttp3.logging.HttpLoggingInterceptor;
import org.reactivestreams.Publisher;

/**
 * The reactive client for the Flux service.
 *
 * @author Jakub Bednar (bednar@github) (26/06/2018 11:07)
 * @since 3.0.0
 */
@Experimental
public interface FluxClientReactive {

    /**
     * Execute a Flux against the Flux service.
     *
     * @param query the flux query to execute
     * @return {@link Flowable} emitting a {@link FluxResult} which are matched the query or
     * {@link Flowable#empty()} if none found.
     */
    Flowable<FluxResult> flux(@Nonnull final String query);

    /**
     * Execute a Flux against the Flux service.
     *
     * @param query   the flux query to execute
     * @param options the options for the query
     * @return {@link Flowable} emitting a {@link FluxResult} which are matched the query or
     * {@link Flowable#empty()} if none found.
     */
    Flowable<FluxResult> flux(@Nonnull final String query, @Nonnull final FluxOptions options);

    /**
     * Execute a Flux against the Flux service.
     *
     * @param query the flux query to execute
     * @return {@link Flowable} emitting a {@link FluxResult} which are matched the query or
     * {@link Flowable#empty()} if none found.
     */
    Flowable<FluxResult> flux(@Nonnull final Flux query);

    /**
     * Execute a Flux against the Flux service.
     *
     * @param query   the flux query to execute
     * @param options the options for the query
     * @return {@link Flowable} emitting a {@link FluxResult} which are matched the query or
     * {@link Flowable#empty()} if none found.
     */
    Flowable<FluxResult> flux(@Nonnull final Flux query, @Nonnull final FluxOptions options);

    /**
     * Execute a Flux against the Flux service.
     *
     * @param query      the flux query to execute
     * @param properties named properties
     * @return {@link Flowable} emitting a {@link FluxResult} which are matched the query or
     * {@link Flowable#empty()} if none found.
     */
    Flowable<FluxResult> flux(@Nonnull final Flux query, @Nonnull final Map<String, Object> properties);

    /**
     * Execute a Flux against the Flux service.
     *
     * @param query      the flux query to execute
     * @param properties named properties
     * @param options    the options for the query
     * @return {@link Flowable} emitting a {@link FluxResult} which are matched the query or
     * {@link Flowable#empty()} if none found.
     */
    Flowable<FluxResult> flux(@Nonnull final Flux query,
                              @Nonnull final Map<String, Object> properties,
                              @Nonnull final FluxOptions options);

    /**
     * Execute a Flux against the Flux service.
     *
     * @param queryStream the flux query to execute
     * @param properties  named properties
     * @return {@link Flowable} emitting a {@link FluxResult} which are matched the query or
     * {@link Flowable#empty()} if none found.
     */
    Flowable<FluxResult> flux(@Nonnull final Publisher<Flux> queryStream,
                              @Nonnull final Map<String, Object> properties);

    /**
     * Execute a Flux against the Flux service.
     *
     * @param queryStream the flux query to execute
     * @param properties  named properties
     * @param options     the options for the query
     * @return {@link Flowable} emitting a {@link FluxResult} which are matched the query or
     * {@link Flowable#empty()} if none found.
     */
    Flowable<FluxResult> flux(@Nonnull final Publisher<Flux> queryStream,
                              @Nonnull final Map<String, Object> properties,
                              @Nonnull final FluxOptions options);

    /**
     * Listen the events produced by {@link FluxClientReactive}.
     *
     * @param eventType type of event to listen
     * @param <T>       type of event to listen
     * @return lister for {@code eventType} events
     */
    @Nonnull
    <T extends AbstractFluxEvent> Observable<T> listenEvents(@Nonnull Class<T> eventType);

    /**
     * Enable Gzip compress for http request body.
     *
     * @return the FluxClientReactive instance to be able to use it in a fluent manner.
     */
    @Nonnull
    FluxClientReactive enableGzip();

    /**
     * Disable Gzip compress for http request body.
     *
     * @return the FluxClientReactive instance to be able to use it in a fluent manner.
     */
    @Nonnull
    FluxClientReactive disableGzip();

    /**
     * Returns whether Gzip compress for http request body is enabled.
     *
     * @return true if gzip is enabled.
     */
    boolean isGzipEnabled();

    /**
     * Check the status of Flux Server.
     *
     * @return {@link Boolean#TRUE} if server is healthy otherwise return {@link Boolean#FALSE}
     */
    @Nonnull
    Maybe<Boolean> ping();

    /**
     * @return the {@link HttpLoggingInterceptor.Level} that is used for logging requests and responses
     */
    @Nonnull
    HttpLoggingInterceptor.Level getLogLevel();

    /**
     * Set the log level for the request and response information.
     *
     * @param logLevel the log level to set.
     * @return the FluxClientReactive instance to be able to use it in a fluent manner.
     */
    @Nonnull
    FluxClientReactive setLogLevel(@Nonnull final HttpLoggingInterceptor.Level logLevel);

    /**
     * Dispose all event listeners before shutdown.
     *
     * @return the FluxClientReactive instance to be able to use it in a fluent manner.
     */
    @Nonnull
    FluxClientReactive close();

    /**
     * @return {@link Boolean#TRUE} if all listeners are disposed
     */
    boolean isClosed();
}
