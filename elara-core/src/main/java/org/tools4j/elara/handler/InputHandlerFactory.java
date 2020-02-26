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
package org.tools4j.elara.handler;

import org.agrona.ExpandableDirectByteBuffer;
import org.agrona.MutableDirectBuffer;
import org.tools4j.elara.command.Command;
import org.tools4j.elara.command.FlyweightCommand;
import org.tools4j.elara.input.Input;
import org.tools4j.elara.log.MessageLog;
import org.tools4j.elara.time.TimeSource;

import java.util.function.Function;

import static java.util.Objects.requireNonNull;

public final class InputHandlerFactory implements Function<Input, Input.Handler> {

    private final MessageLog.Appender<? super Command> commandLogAppender;
    private final TimeSource timeSource;
    private final MutableDirectBuffer headerBuffer = new ExpandableDirectByteBuffer(FlyweightCommand.HEADER_LENGTH);
    private final FlyweightCommand flyweightCommand = new FlyweightCommand();

    public InputHandlerFactory(final MessageLog.Appender<? super Command> commandLogAppender,
                               final TimeSource timeSource) {
        this.commandLogAppender = requireNonNull(commandLogAppender);
        this.timeSource = requireNonNull(timeSource);
    }

    @Override
    public Input.Handler apply(final Input input) {
        return new InputHandler(
                commandLogAppender,
                timeSource,
                input,
                headerBuffer,
                flyweightCommand
        );
    }
}
