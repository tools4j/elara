/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2020-2024 tools4j.org (Marco Terzer, Anton Anufriev)
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

import static java.util.Objects.requireNonNull;
import static org.tools4j.elara.logging.Logger.Level.INFO;

public interface Logger {
    enum Level {
        ERROR, WARN, INFO, DEBUG
    }

    void log(Level level, CharSequence message);

    void logStackTrace(Level level, Throwable t);

    default boolean isEnabled(final Level level) {
        return level.ordinal() <= INFO.ordinal();
    }

    @FunctionalInterface
    interface Factory {
        Logger create(Class<?> clazz);
    }

    static Logger systemLogger() {
        return OutputStreamLogger.SYSTEM;
    }

    static Logger.Factory systemLoggerFactory() {
        return OutputStreamLogger.SYSTEM_FACTORY;
    }

    static Logger threadSafe(final Logger logger) {
        requireNonNull(logger);
        return new Logger() {
            @Override
            public void log(final Level level, final CharSequence message) {
                synchronized (logger) {
                    logger.log(level, message);
                }
            }

            @Override
            public void logStackTrace(final Level level, final Throwable t) {
                synchronized (logger) {
                    logger.logStackTrace(level, t);
                }
            }
        };
    }
}
