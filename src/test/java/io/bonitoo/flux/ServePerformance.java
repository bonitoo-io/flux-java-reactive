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
import java.util.Objects;
import javax.annotation.Nonnull;

import org.influxdb.annotation.Column;
import org.influxdb.annotation.Measurement;

/**
 * @author Jakub Bednar (bednar@github) (27/06/2018 07:31)
 */
@Measurement(name = "server_performance")
public class ServePerformance {

    @Column(name = "location", tag = true)
    String location;

    @Column(name = "cpu_usage")
    Double cpuUsage;

    @Column(name = "server description")
    String description;

    @Column(name = "upTime")
    Long upTime;

    @Column(name = "rackNumber")
    Integer rackNumber;

    @Column(name = "production_usage", tag = true)
    boolean production;

    @Column(name = "time")
    private Instant time;

    public ServePerformance() {
    }

    @Nonnull
    static ServePerformance create(@Nonnull final Integer index) {
        Objects.requireNonNull(index, "Measurement index is required");

        ServePerformance servePerformance = new ServePerformance();
        servePerformance.time = Instant.ofEpochMilli(1_530_079_000_000L + index);
        servePerformance.production = ((index % 2) == 0);
        servePerformance.rackNumber = index;
        servePerformance.upTime = Long.valueOf(index) * 10_000;
        servePerformance.description = "Server no. " + index;
        servePerformance.cpuUsage = 50 * index.doubleValue();
        servePerformance.location = String.format("Area %s° %s' \"%s", index, index * 10, index * 20);

        return servePerformance;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ServePerformance)) return false;
        ServePerformance that = (ServePerformance) o;
        return production == that.production &&
                Objects.equals(location, that.location) &&
                Objects.equals(cpuUsage, that.cpuUsage) &&
                Objects.equals(description, that.description) &&
                Objects.equals(upTime, that.upTime) &&
                Objects.equals(rackNumber, that.rackNumber) &&
                Objects.equals(time, that.time);
    }

    @Override
    public int hashCode() {

        return Objects.hash(location, cpuUsage, description, upTime, rackNumber, production, time);
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("io.bonitoo.flux.ServePerformance{");
        sb.append("location='").append(location).append('\'');
        sb.append(", cpuUsage=").append(cpuUsage);
        sb.append(", description='").append(description).append('\'');
        sb.append(", upTime=").append(upTime);
        sb.append(", rackNumber=").append(rackNumber);
        sb.append(", production=").append(production);
        sb.append(", time=").append(time);
        sb.append('}');
        return sb.toString();
    }
}