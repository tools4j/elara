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

import org.tools4j.elara.plugin.base.BasePlugin;
import org.tools4j.elara.plugin.boot.BootPlugin;
import org.tools4j.elara.plugin.metrics.MetricsConfig;
import org.tools4j.elara.plugin.metrics.MetricsPlugin;
import org.tools4j.elara.plugin.replication.ReplicationPlugin;
import org.tools4j.elara.plugin.timer.TimerPlugin;

public enum Plugins {
    ;
    public static BasePlugin basePlugin() {
        return BasePlugin.INSTANCE;
    }

    public static TimerPlugin timerPlugin() {
        return TimerPlugin.DEFAULT;
    }
    public static TimerPlugin timerPlugin(final int sourceId) {
        return new TimerPlugin(sourceId);
    }

    public static BootPlugin bootPlugin() {
        return BootPlugin.DEFAULT;
    }
    public static BootPlugin bootPlugin(final int commandSource) {
        return new BootPlugin(commandSource);
    }

    public static MetricsPlugin metricsPlugin(final MetricsConfig configuration) {
        return new MetricsPlugin(configuration);
    }

    public static ReplicationPlugin replicationPlugin(final org.tools4j.elara.plugin.replication.Configuration configuration) {
        return new ReplicationPlugin(configuration);
    }
}
