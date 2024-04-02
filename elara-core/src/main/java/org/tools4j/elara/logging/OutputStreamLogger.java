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

import java.io.OutputStream;
import java.io.PrintWriter;
import java.time.LocalDate;

import static java.time.temporal.ChronoField.YEAR;
import static java.util.Objects.requireNonNull;

public class OutputStreamLogger implements Logger {

    public static final OutputStreamLogger SYSTEM = new OutputStreamLogger();
    public static final Factory SYSTEM_FACTORY = clazz -> SYSTEM;

    private static final int DAYS_PER_CYCLE = 146097;
    /**
     * The number of days from year zero to year 1970.
     * There are five 400 year cycles from year zero to 2000.
     * There are 7 leap years from 1970 to 2000.
     */
    private static final long DAYS_0000_TO_1970 = (DAYS_PER_CYCLE * 5L) - (30L * 365L + 7L);
    private static final long MILLIS_PER_DAY = 24 * 60 * 60 * 1000L;

    private final Level level;
    private final PrintWriter out;
    private final PrintWriter err;

    private final char[] charBuffer = new char[1024];

    public OutputStreamLogger() {
        this(Level.INFO);
    }

    public OutputStreamLogger(final Level level) {
        this(level, System.out, System.err);
    }

    public OutputStreamLogger(final Level level, final OutputStream out, final OutputStream err) {
        this(level, new PrintWriter(out), new PrintWriter(err));
    }

    public OutputStreamLogger(final Level level, final PrintWriter out, final PrintWriter err) {
        this.level = requireNonNull(level);
        this.out = requireNonNull(out);
        this.err = requireNonNull(err);
    }

    @Override
    public boolean isEnabled(final Level level) {
        return level.ordinal() <= this.level.ordinal();
    }

    @Override
    public void log(final Level level, final CharSequence message) {
        log(level == Level.ERROR || level == Level.WARN ? err : out, level, message);
    }

    @Override
    public void logStackTrace(final Level level, final Throwable t) {
        t.printStackTrace(level == Level.ERROR || level == Level.WARN ? err : out);
    }

    private void log(final PrintWriter writer, final Level level, final CharSequence message) {
        final long time = System.currentTimeMillis();
        printDate(writer, time);
        writer.print('T');
        printTime(writer, time);
        writer.print('Z');
        writer.print(' ');
        writer.print(levelString(level));
        writer.print(' ');
        printMessage(writer, message);
        writer.println();
        writer.flush();
    }

    private void printMessage(final PrintWriter writer, final CharSequence message) {
        if (message instanceof String) {
            writer.print((String)message);
        } else if (message instanceof StringBuilder || message instanceof StringBuffer) {
            final StringBuilder builder = message instanceof StringBuilder ? (StringBuilder)message : null;
            final StringBuffer buffer = message instanceof StringBuffer ? (StringBuffer)message : null;
            int off = 0;
            int rem = message.length();
            while (rem > 0) {
                final int len = Math.min(rem,  charBuffer.length);
                if (builder != null) {
                    builder.getChars(off, off + len, charBuffer, 0);
                } else {
                    buffer.getChars(off, off + len, charBuffer, 0);
                }
                writer.write(charBuffer, 0, len);
                off += len;
                rem -= len;
            }
        } else {
            final int len = message.length();
            for (int i = 0; i < len; i++) {
                writer.write(message.charAt(i));
            }
        }
    }

    /** @see LocalDate#ofEpochDay(long) */
    private static void printDate(final PrintWriter writer, final long epochMillis) {
        final long epochDay = Math.floorDiv(epochMillis, MILLIS_PER_DAY);
        long zeroDay = epochDay + DAYS_0000_TO_1970;
        // find the march-based year
        zeroDay -= 60;  // adjust to 0000-03-01 so leap day is at end of four year cycle
        long adjust = 0;
        if (zeroDay < 0) {
            // adjust negative years to positive for calculation
            long adjustCycles = (zeroDay + 1) / DAYS_PER_CYCLE - 1;
            adjust = adjustCycles * 400;
            zeroDay += -adjustCycles * DAYS_PER_CYCLE;
        }
        long yearEst = (400 * zeroDay + 591) / DAYS_PER_CYCLE;
        long doyEst = zeroDay - (365 * yearEst + yearEst / 4 - yearEst / 100 + yearEst / 400);
        if (doyEst < 0) {
            // fix estimate
            yearEst--;
            doyEst = zeroDay - (365 * yearEst + yearEst / 4 - yearEst / 100 + yearEst / 400);
        }
        yearEst += adjust;  // reset any negative year
        int marchDoy0 = (int) doyEst;

        // convert march-based values back to january-based
        final int marchMonth0 = (marchDoy0 * 5 + 2) / 153;
        final int month = (marchMonth0 + 2) % 12 + 1;
        final int dom = marchDoy0 - (marchMonth0 * 306 + 5) / 10 + 1;
        yearEst += marchMonth0 / 10;

        // check year now we are certain it is correct
        final int year = YEAR.checkValidIntValue(yearEst);
        printDigits(writer, year, 4);
        writer.print('-');
        printDigits(writer, month, 2);
        writer.print('-');
        printDigits(writer, dom, 2);
    }
    private static void printTime(final PrintWriter writer, final long epochMillis) {
        final int millisOfDay = (int)Math.floorMod(epochMillis, MILLIS_PER_DAY);
        final int millis = millisOfDay % 1000;
        final int seconds = (millisOfDay / 1000) % 60;
        final int minutes = (millisOfDay / (1000 * 60)) % 60;
        final int hours = millisOfDay / (1000 * 60 * 60);
        printDigits(writer, hours, 2);
        writer.print(':');
        printDigits(writer, minutes, 2);
        writer.print(':');
        printDigits(writer, seconds, 2);
        writer.print('.');
        printDigits(writer, millis, 3);
    }

    private static void printDigits(final PrintWriter writer, final int value, final int digits) {
        if (value > 0) {
            switch (digits) {
                case 4:
                    if (value < 1000) writer.print('0');
                case 3:
                    if (value < 100) writer.print('0');
                case 2:
                    if (value < 10) writer.print('0');
            }
        }
        writer.print(value);
    }
    private static String levelString(final Level level) {
        switch (level) {
            case ERROR: return "ERROR";
            case WARN:  return " WARN";
            case INFO:  return " INFO";
            case DEBUG: return "DEBUG";
            default: return level.name();
        }
    }
}
