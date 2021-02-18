/**
 * The MIT License (MIT)
 *
 * Copyright (c) 2021 tools4j.org (Marco Terzer, Anton Anufriev)
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
package org.tools4j.elara.logging;

import org.tools4j.elara.logging.Logger.Level;

import java.util.EnumMap;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Supplier;

import static java.util.Objects.requireNonNull;

public class DefaultElaraLogger implements ElaraLogger {

    private static final int DEFAULT_INITIAL_MESSAGE_CAPACITY = 128;
    private static final Level[] LEVELS = Level.values();

    private final Logger logger;
    private final Map<Level, Consumer<CharSequence>> levelLogger;
    private final Supplier<? extends FlyweightPlaceholderReplacer> placeholderReplacerSupplier;

    public DefaultElaraLogger(final Logger logger,
                              final Supplier<? extends FlyweightPlaceholderReplacer> placeholderReplacerSupplier) {
        this.logger = requireNonNull(logger);
        this.placeholderReplacerSupplier = requireNonNull(placeholderReplacerSupplier);
        this.levelLogger = new EnumMap<>(Level.class);
        for (final Level level : LEVELS) {
            levelLogger.put(level, s -> logger.log(level, s));
        }
    }

    public static DefaultElaraLogger create(final Logger logger) {
        return new DefaultElaraLogger(logger, () -> new FlyweightPlaceholderReplacer(DEFAULT_INITIAL_MESSAGE_CAPACITY));
    }

    public static DefaultElaraLogger threadLocal(final Logger logger) {
        return new DefaultElaraLogger(logger, ThreadLocal.withInitial(() -> new FlyweightPlaceholderReplacer(DEFAULT_INITIAL_MESSAGE_CAPACITY))::get);
    }

    @Override
    public PlaceholderReplacer log(final Level level, final String message) {
        return placeholderReplacerSupplier.get().init(levelLogger.get(level), message);
    }

    @Override
    public boolean isEnabled(final Level level) {
        return logger.isEnabled(level);
    }
}
