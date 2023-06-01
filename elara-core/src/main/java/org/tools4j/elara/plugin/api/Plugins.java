/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2020-2023 tools4j.org (Marco Terzer, Anton Anufriev)
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package org.tools4j.elara.plugin.api;

import org.tools4j.elara.plugin.activation.ActivationConfig;
import org.tools4j.elara.plugin.activation.ActivationPlugin;
import org.tools4j.elara.plugin.boot.BootPlugin;
import org.tools4j.elara.plugin.metrics.MetricsConfig;
import org.tools4j.elara.plugin.metrics.MetricsPlugin;
import org.tools4j.elara.plugin.repair.RepairPlugin;
import org.tools4j.elara.plugin.replication.ReplicationConfig;
import org.tools4j.elara.plugin.replication.ReplicationPlugin;
import org.tools4j.elara.plugin.timer.TimerPlugin;

public enum Plugins {
    ;
    public static RepairPlugin repairPlugin() {
        return RepairPlugin.INSTANCE;
    }

    public static TimerPlugin timerPlugin() {
        return new TimerPlugin();
    }
    public static TimerPlugin timerPlugin(final int sourceId) {
        return new TimerPlugin(sourceId);
    }
    public static TimerPlugin timerPlugin(final int sourceId, final boolean useInterceptor, final int signalInputSkip) {
        return new TimerPlugin(sourceId, useInterceptor, signalInputSkip);
    }

    public static BootPlugin bootPlugin() {
        return BootPlugin.DEFAULT;
    }
    public static BootPlugin bootPlugin(final int sourceId) {
        return new BootPlugin(sourceId);
    }

    public static MetricsPlugin metricsPlugin(final MetricsConfig config) {
        return new MetricsPlugin(config);
    }

    public static ActivationPlugin activationPlugin() {
        return activationPlugin(ActivationPlugin.configure());
    }
    public static ActivationPlugin activationPlugin(final ActivationConfig config) {
        return new ActivationPlugin(config);
    }

    public static ReplicationPlugin replicationPlugin(final ReplicationConfig config) {
        return new ReplicationPlugin(config);
    }
}
