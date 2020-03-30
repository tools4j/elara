/**
 * The MIT License (MIT)
 *
 * Copyright (c) 2020 tools4j.org (Marco Terzer, Anton Anufriev)
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
package org.tools4j.elara.log;

import org.tools4j.elara.flyweight.Frame;
import org.tools4j.elara.flyweight.Header;

import java.io.PrintWriter;

public interface FrameFormatter extends MessageFormatter<Frame> {

    void format(long line, int input, int type, long sequence, long time, int version, short index, int payloadSize, PrintWriter writer);

    @Override
    default void format(final long line, final Frame frame, final PrintWriter writer) {
        format(line, frame.header(), writer);
    }

    default void format(final long line, final Header header, final PrintWriter writer) {
        format(line, header.input(), header.type(), header.sequence(), header.time(), header.version(), header.index(),
                header.payloadSize(), writer);
    }

    interface ValueFormatter {
        ValueFormatter DEFAULT = new ValueFormatter() {};
        default Object line(long line) {return line;}
        default Object input(int input) {return input;}
        default Object type(int type) {return type;}
        default Object sequence(long sequence) {return sequence;}
        default Object time(long time) {return time;}
        default Object version(int version) {return version;}
        default Object index(short index) {return index;}
        default Object payloadSize(int payloadSize) {return payloadSize;}

        default Object versionNewLine(long line, int version) {
            return line > 0 ? NEW_LINE : " (V " + version + ")" + NEW_LINE;
        }
    }

    String PIPE_FORMAT_STRING = "%s: in=%s|tp=%s|sq=%s|tm=%s|vs=%s|ix=%s|sz=%s%s";
    String SHORT_FORMAT_STRING = "%s: in=%s, tp=%s, sq=%s, tm=%s, vs=%s, ix=%s, sz=%s%s";
    String LONG_FORMAT_STRING = "%s: input=%s, type=%s, sequence=%s, time=%s, version=%s, index=%s, size=%s%s";
    String COMMAND_FORMAT_STRING = "%s: %s:%s | type=%s, size=%s at %s%s";
    String EVENT_FORMAT_STRING_0 = "%s: %s:%s.%s | type=%s, size=%s at %s%s";
    String EVENT_FORMAT_STRING_N = "%s: ...%s.%s | type=%s, size=%s%s";

    FrameFormatter PIPE = formatter(PIPE_FORMAT_STRING);
    FrameFormatter SHORT = formatter(SHORT_FORMAT_STRING);
    FrameFormatter LONG = formatter(LONG_FORMAT_STRING);

    FrameFormatter COMMAND = (line, input, type, sequence, time, version, index, payloadSize, writer) ->
        writer.printf(COMMAND_FORMAT_STRING, line, input, sequence, type, payloadSize, time, ValueFormatter.DEFAULT.versionNewLine(line, version));

    FrameFormatter EVENT = (line, input, type, sequence, time, version, index, payloadSize, writer) -> {
        if (index == 0) {
            writer.printf(EVENT_FORMAT_STRING_0, line, input, sequence, index, type, payloadSize, time, ValueFormatter.DEFAULT.versionNewLine(line, version));
        } else {
            final String dots = String.format("%s.%s", input, sequence).replaceAll(".", ".").substring(3);
            writer.printf(EVENT_FORMAT_STRING_N, line, dots, index, type, payloadSize, NEW_LINE);
        }
    };

    FrameFormatter DEFAULT =  (line, input, type, sequence, time, version, index, payloadSize, writer) ->
        (index < 0 ? COMMAND : EVENT)
            .format(line, input, type, sequence, time, version, index, payloadSize, writer);

    static FrameFormatter command(final ValueFormatter vf) {
        return (line, input, type, sequence, time, version, index, payloadSize, writer) ->
            writer.printf(COMMAND_FORMAT_STRING, vf.line(line), vf.input(input), vf.sequence(sequence), vf.type(type),
                    vf.payloadSize(payloadSize), vf.time(time), vf.versionNewLine(line, version));
    }

    static FrameFormatter event(final ValueFormatter vf) {
        return (line, input, type, sequence, time, version, index, payloadSize, writer) -> {
            if (index == 0) {
                writer.printf(EVENT_FORMAT_STRING_0, vf.line(line), vf.input(input), vf.sequence(sequence),
                        vf.index(index), vf.type(type), vf.payloadSize(payloadSize), vf.time(time), vf.versionNewLine(line, version));
            } else {
                final String dots = String.format("%s.%s", vf.input(input), vf.sequence(sequence)).replaceAll(".", ".");
                final String dots3 = dots.substring(Math.min(3, dots.length()));
                writer.printf(EVENT_FORMAT_STRING_N, vf.line(line), dots3, vf.index(index), vf.type(type),
                        vf.payloadSize(payloadSize), NEW_LINE);
            }
        };
    }

    static FrameFormatter getDefault(final ValueFormatter valueFormatter) {
        final FrameFormatter cmd = command(valueFormatter);
        final FrameFormatter evt = event(valueFormatter);
        return (line, input, type, sequence, time, version, index, payloadSize, writer) ->
            (index < 0 ? cmd : evt).format(
                    line, input, type, sequence, time, version, index, payloadSize, writer
            );
    }

    static FrameFormatter formatter(final String formatString) {
        return (line, input, type, sequence, time, version, index, payloadSize, writer) ->
                writer.printf(formatString, line, input, type, sequence, time, version, index, payloadSize, NEW_LINE);
    }

    static FrameFormatter formatter(final String formatString, final ValueFormatter vf) {
        return (line, input, type, sequence, time, version, index, payloadSize, writer) ->
                writer.printf(formatString, vf.line(line), vf.input(input), vf.type(type), vf.sequence(sequence),
                        vf.time(time), vf.version(version), vf.index(index), vf.payloadSize(payloadSize), NEW_LINE);
    }
}
