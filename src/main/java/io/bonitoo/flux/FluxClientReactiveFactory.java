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
import javax.annotation.Nonnull;

import io.bonitoo.flux.impl.FluxClientReactiveImpl;
import io.bonitoo.flux.option.FluxConnectionOptions;

/**
 * The Factory that create a reactive instance of a Flux client.
 *
 * @author Jakub Bednar (bednar@github) (26/06/2018 11:13)
 * @since 1.0.0
 */
public final class FluxClientReactiveFactory {

    private FluxClientReactiveFactory() {
    }

    /**
     * Create a instance of the Flux reactive client.
     *
     * @param options the connection configuration
     * @return 3.0.0
     */
    @Nonnull
    public static FluxClientReactive connect(@Nonnull final FluxConnectionOptions options) {

        Objects.requireNonNull(options, "FluxConnectionOptions are required");

        return new FluxClientReactiveImpl(options);
    }
}
