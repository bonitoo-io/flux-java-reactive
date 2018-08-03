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
package io.bonitoo.flux.impl;

import java.io.EOFException;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.Nonnull;

import io.bonitoo.flux.Flux;
import io.bonitoo.flux.FluxClientReactive;
import io.bonitoo.flux.FluxException;
import io.bonitoo.flux.events.AbstractFluxEvent;
import io.bonitoo.flux.events.FluxErrorEvent;
import io.bonitoo.flux.events.FluxSuccessEvent;
import io.bonitoo.flux.mapper.FluxResult;
import io.bonitoo.flux.options.FluxConnectionOptions;
import io.bonitoo.flux.options.FluxCsvParserOptions;
import io.bonitoo.flux.options.FluxOptions;
import io.bonitoo.flux.utils.Preconditions;

import io.reactivex.BackpressureStrategy;
import io.reactivex.Flowable;
import io.reactivex.Maybe;
import io.reactivex.Observable;
import io.reactivex.functions.Function;
import io.reactivex.subjects.PublishSubject;
import okhttp3.ResponseBody;
import okhttp3.logging.HttpLoggingInterceptor;
import okio.BufferedSource;
import org.reactivestreams.Publisher;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory;

/**
 * @author Jakub Bednar (bednar@github) (26/06/2018 11:59)
 */
public class FluxClientReactiveImpl extends AbstractFluxClient<FluxServiceReactive> implements FluxClientReactive {

    private static final Logger LOG = Logger.getLogger(FluxClientReactiveImpl.class.getName());

    private final PublishSubject<Object> eventPublisher;

    public FluxClientReactiveImpl(@Nonnull final FluxConnectionOptions fluxConnectionOptions) {

        super(fluxConnectionOptions, FluxServiceReactive.class);

        this.eventPublisher = PublishSubject.create();
    }

    @Override
    protected void configure(@Nonnull final Retrofit.Builder serviceBuilder) {
        serviceBuilder.addCallAdapterFactory(RxJava2CallAdapterFactory.create());
    }

    @Nonnull
    @Override
    public Flowable<FluxResult> flux(@Nonnull final String query) {

        Preconditions.checkNonEmptyString(query, "Flux query");

        return flux(query, FluxOptions.DEFAULTS);
    }

    @Nonnull
    @Override
    public Flowable<FluxResult> flux(@Nonnull final String query, @Nonnull final FluxOptions options) {

        Preconditions.checkNonEmptyString(query, "Flux query");
        Objects.requireNonNull(options, "FluxOptions are required");

        return flux(new StringFlux(query), options);
    }

    @Nonnull
    @Override
    public Maybe<Response<ResponseBody>> fluxRaw(@Nonnull final String query) {

        Preconditions.checkNonEmptyString(query, "Flux query");

        return fluxRaw(query, FluxOptions.DEFAULTS);
    }

    @Nonnull
    @Override
    public Maybe<Response<ResponseBody>> fluxRaw(@Nonnull final String query, @Nonnull final FluxOptions options) {

        Preconditions.checkNonEmptyString(query, "Flux query");
        Objects.requireNonNull(options, "FluxOptions are required");

        return fluxRaw(new StringFlux(query), options);
    }

    @Nonnull
    @Override
    public Flowable<FluxResult> flux(@Nonnull final Flux query) {

        Objects.requireNonNull(query, "Flux query is required");

        return flux(query, FluxOptions.DEFAULTS);
    }

    @Nonnull
    @Override
    public Flowable<FluxResult> flux(@Nonnull final Flux query, @Nonnull final FluxOptions options) {

        Objects.requireNonNull(query, "Flux query is required");
        Objects.requireNonNull(options, "FluxOptions are required");

        return flux(query, new HashMap<>(), options);
    }

    @Nonnull
    @Override
    public Flowable<FluxResult> flux(@Nonnull final Flux query, @Nonnull final Map<String, Object> properties) {

        Objects.requireNonNull(query, "Flux query is required");
        Objects.requireNonNull(properties, "Parameters are required");

        return flux(query, properties, FluxOptions.DEFAULTS);
    }

    @Nonnull
    @Override
    public Flowable<FluxResult> flux(@Nonnull final Flux query,
                                     @Nonnull final Map<String, Object> properties,
                                     @Nonnull final FluxOptions options) {

        Objects.requireNonNull(query, "Flux query is required");
        Objects.requireNonNull(properties, "Parameters are required");
        Objects.requireNonNull(options, "FluxOptions are required");

        return flux(Flowable.just(query), properties, options);
    }

    @Nonnull
    @Override
    public Flowable<FluxResult> flux(@Nonnull final Publisher<Flux> queryStream,
                                     @Nonnull final Map<String, Object> properties) {
        Objects.requireNonNull(queryStream, "Flux stream is required");
        Objects.requireNonNull(properties, "Parameters are required");

        return flux(queryStream, properties, FluxOptions.DEFAULTS);
    }

    @Nonnull
    @Override
    public Flowable<FluxResult> flux(@Nonnull final Publisher<Flux> queryStream,
                                     @Nonnull final Map<String, Object> properties,
                                     @Nonnull final FluxOptions options) {

        Objects.requireNonNull(queryStream, "Flux stream is required");
        Objects.requireNonNull(properties, "Parameters are required");
        Objects.requireNonNull(options, "FluxOptions are required");

        return Flowable.fromPublisher(queryStream).concatMap((Function<Flux, Publisher<FluxResult>>) flux -> {

            //
            // Parameters
            //
            String orgID = this.fluxConnectionOptions.getOrgID();
            String query = toFluxString(flux, properties, options);

            return fluxService
                    .query(query, orgID)
                    .flatMap(
                            // success response
                            body -> chunkReader(query, this.fluxConnectionOptions, body, options.getParserOptions()),
                            // error response
                            throwable -> (observer -> {

                                FluxException fluxException = FluxException.fromCause(throwable);

                                // publish event
                                publishEvent(new FluxErrorEvent(fluxConnectionOptions, query, fluxException));
                                observer.onError(fluxException);
                            }),
                            // end of response
                            Observable::empty)
                    .toFlowable(BackpressureStrategy.BUFFER);
        });
    }

    @Nonnull
    @Override
    public Maybe<Response<ResponseBody>> fluxRaw(@Nonnull final Flux query) {

        Objects.requireNonNull(query, "Flux query is required");

        return fluxRaw(query, FluxOptions.DEFAULTS);
    }

    @Nonnull
    @Override
    public Maybe<Response<ResponseBody>> fluxRaw(@Nonnull final Flux query, @Nonnull final FluxOptions options) {

        Objects.requireNonNull(query, "Flux query is required");
        Objects.requireNonNull(options, "FluxOptions are required");

        return fluxRaw(query, new HashMap<>(), options);
    }

    @Nonnull
    @Override
    public Maybe<Response<ResponseBody>> fluxRaw(@Nonnull final Flux query,
                                                 @Nonnull final Map<String, Object> properties) {

        Objects.requireNonNull(query, "Flux query is required");
        Objects.requireNonNull(properties, "Parameters are required");

        return fluxRaw(query, properties, FluxOptions.DEFAULTS);
    }

    @Nonnull
    @Override
    public Maybe<Response<ResponseBody>> fluxRaw(@Nonnull final Flux query,
                                                 @Nonnull final Map<String, Object> properties,
                                                 @Nonnull final FluxOptions options) {

        Objects.requireNonNull(query, "Flux query is required");
        Objects.requireNonNull(properties, "Parameters are required");
        Objects.requireNonNull(options, "FluxOptions are required");

        return fluxRaw(Flowable.just(query), properties, options).singleElement();
    }

    @Nonnull
    @Override
    public Flowable<Response<ResponseBody>> fluxRaw(@Nonnull final Publisher<Flux> queryStream,
                                                    @Nonnull final Map<String, Object> properties) {

        Objects.requireNonNull(queryStream, "Flux stream is required");
        Objects.requireNonNull(properties, "Parameters are required");

        return fluxRaw(queryStream, properties, FluxOptions.DEFAULTS);
    }

    @Nonnull
    public Flowable<Response<ResponseBody>> fluxRaw(@Nonnull final Publisher<Flux> queryStream,
                                                    @Nonnull final Map<String, Object> properties,
                                                    @Nonnull final FluxOptions options) {

        Objects.requireNonNull(queryStream, "Flux stream is required");
        Objects.requireNonNull(properties, "Parameters are required");
        Objects.requireNonNull(options, "FluxOptions are required");

        return Flowable
                .fromPublisher(queryStream)
                .concatMap((Function<Flux, Publisher<Response<ResponseBody>>>) flux -> {

                    //
                    // Parameters
                    //
                    String orgID = this.fluxConnectionOptions.getOrgID();
                    String query = toFluxString(flux, properties, options);

                    return fluxService.queryRaw(query, orgID).toFlowable(BackpressureStrategy.BUFFER);
                });
    }

    @Nonnull
    @Override
    public <T extends AbstractFluxEvent> Observable<T> listenEvents(@Nonnull final Class<T> eventType) {

        Objects.requireNonNull(eventType, "EventType is required");

        return eventPublisher.ofType(eventType);
    }

    @Nonnull
    @Override
    public FluxClientReactive enableGzip() {
        this.gzipRequestInterceptor.enable();
        return this;
    }

    @Nonnull
    @Override
    public FluxClientReactive disableGzip() {
        this.gzipRequestInterceptor.disable();
        return this;
    }

    @Override
    public boolean isGzipEnabled() {
        return this.gzipRequestInterceptor.isEnabled();
    }

    @Override
    @Nonnull
    public Maybe<Boolean> ping() {

        return fluxService
                .ping()
                .map(Response::isSuccessful);
    }

    @Nonnull
    @Override
    public HttpLoggingInterceptor.Level getLogLevel() {
        return this.loggingInterceptor.getLevel();
    }

    @Nonnull
    @Override
    public FluxClientReactive setLogLevel(@Nonnull final HttpLoggingInterceptor.Level logLevel) {

        Objects.requireNonNull(logLevel, "Log level is required");

        this.loggingInterceptor.setLevel(logLevel);

        return this;
    }

    @Nonnull
    @Override
    public FluxClientReactive close() {

        LOG.log(Level.INFO, "Dispose all event listeners before shutdown.");

        eventPublisher.onComplete();

        return this;
    }

    @Override
    public boolean isClosed() {
        return eventPublisher.hasComplete();
    }

    @Nonnull
    private Observable<FluxResult> chunkReader(@Nonnull final String query,
                                               @Nonnull final FluxConnectionOptions options,
                                               @Nonnull final ResponseBody body,
                                               @Nonnull final FluxCsvParserOptions parserOptions) {

        Objects.requireNonNull(options, "FluxConnectionOptions are required");
        Preconditions.checkNonEmptyString(query, "Flux query");
        Objects.requireNonNull(body, "ResponseBody is required");
        Objects.requireNonNull(parserOptions, "FluxCsvParserOptions are required");

        return Observable.create(subscriber -> {

            boolean isCompleted = false;
            try {
                BufferedSource source = body.source();

                //
                // Subscriber is not disposed && source has data => parse
                //
                while (!subscriber.isDisposed() && !source.exhausted()) {

                    FluxResult fluxResult = mapper.toFluxResult(source, parserOptions);
                    if (fluxResult != null) {

                        subscriber.onNext(fluxResult);
                        publishEvent(new FluxSuccessEvent(options, query));
                    }
                }
            } catch (IOException e) {

                //
                // Socket close by remote server or end of data
                //
                if (e.getMessage().equals("Socket closed") || e instanceof EOFException) {
                    isCompleted = true;
                    subscriber.onComplete();
                } else {
                    throw new UncheckedIOException(e);
                }
            }

            //if response end we get here
            if (!isCompleted) {
                subscriber.onComplete();
            }

            body.close();
        });
    }

    private <T extends AbstractFluxEvent> void publishEvent(@Nonnull final T event) {

        Objects.requireNonNull(event, "Event is required");

        event.logEvent();
        eventPublisher.onNext(event);
    }
}
