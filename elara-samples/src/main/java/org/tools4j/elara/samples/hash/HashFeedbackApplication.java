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
package org.tools4j.elara.samples.hash;

import net.openhft.chronicle.queue.ChronicleQueue;
import net.openhft.chronicle.wire.WireType;
import org.agrona.concurrent.BusySpinIdleStrategy;
import org.tools4j.elara.app.message.Event;
import org.tools4j.elara.app.type.FeedbackApp;
import org.tools4j.elara.chronicle.ChronicleMessageStore;
import org.tools4j.elara.input.CommandMessageInput;
import org.tools4j.elara.input.InputPoller;
import org.tools4j.elara.run.ElaraRunner;
import org.tools4j.elara.samples.hash.HashApplication.ModifiableState;
import org.tools4j.elara.samples.time.PseudoMicroClock;
import org.tools4j.elara.send.CommandContext;
import org.tools4j.elara.send.CommandSender;
import org.tools4j.elara.stream.MessageReceiver;
import org.tools4j.elara.stream.MessageSender;
import org.tools4j.elara.stream.ipc.AllocationStrategy;
import org.tools4j.elara.stream.ipc.Ipc;
import org.tools4j.elara.stream.ipc.IpcConfig;
import org.tools4j.elara.time.DefaultMutableTimeSource;
import org.tools4j.elara.time.TimeSource;

import java.io.File;
import java.util.concurrent.atomic.AtomicLong;

import static java.util.Objects.requireNonNull;
import static org.tools4j.elara.samples.hash.HashApplication.isEven;
import static org.tools4j.elara.stream.ipc.Cardinality.ONE;

public class HashFeedbackApplication implements FeedbackApp {
    public static final int INPUT_SOURCE_ID = 46;
    public static final int PROCESSOR_SOURCE_ID = 47;
    private static final int IPC_BUFFER_SIZE = 1<<20;
    private static final int IPC_SENDER_BUFFER_SIZE = 1024;

    private final ModifiableState state;
    private final InputPoller inputPoller;
    public HashFeedbackApplication(final ModifiableState state, final AtomicLong input) {
        this.state = requireNonNull(state);
        this.inputPoller = HashApplication.inputPoller(input);
    }

    @Override
    public void onEvent(final Event event, final CommandContext commandContext, final CommandSender sender) {
        final boolean even = isEven(state.hash());
        final long eventValue = event.payload().getLong(0);
        final long updateValue = even ? eventValue : ~eventValue;
        state.update(updateValue);
        if (even) {
            //send every second command from here, rest form input
            inputPoller.poll(commandContext, sender);
        }
    }

    public static InputPoller inflightAwareInputPoller(final AtomicLong input) {
        requireNonNull(input);
        final InputPoller inputPoller = HashApplication.inputPoller(input);
        return (commandContext, commandSender) -> {
            if (commandContext.hasInFlightCommand()) {
                return 0;
            }
            return inputPoller.poll(commandContext, commandSender);
        };
    }

    private static IpcConfig ipcConfig() {
        return IpcConfig.configure()
                .senderCardinality(ONE)
                .senderInitialBufferSize(IPC_SENDER_BUFFER_SIZE)
                .senderAllocationStrategy(AllocationStrategy.FIXED)
                .maxMessagesReceivedPerPoll(2)
                .newFileCreateParentDirs(true)
                .newFileDeleteIfPresent(true);
    }

    private static MessageSender ipcSender(final String folder) {
        final File file = new File("build/ipc/" + folder + "/ipc.map");
        return Ipc.newSender(file, IPC_BUFFER_SIZE, ipcConfig());
    }

    private static MessageReceiver ipcReceiver(final String folder) {
        final File file = new File("build/ipc/" + folder + "/ipc.map");
        return Ipc.retryOpenReceiver(file, ipcConfig());
    }

    public static ElaraRunner passthroughAppWithChronicleQueueAndMetrics(final String folder,
                                                                         final boolean timeMetrics) {
        final MessageReceiver messageReceiver = ipcReceiver(folder);
        return new HashPassthroughApplication().launch(
                config -> HashPassthroughApplication.configureChronicleQueueWithMetrics(config, folder, timeMetrics)
                        .input(new CommandMessageInput(messageReceiver, config.exceptionHandler()))
        );
    }

    public static ElaraRunner feedbackApp(final String folder,
                                          final AtomicLong input,
                                          final ModifiableState state) {
        final String path = "build/chronicle/" + folder;
        final TimeSource pseudoNanoClock = new PseudoMicroClock();
        final ChronicleQueue eq = ChronicleQueue.singleBuilder()
                .path(path + "/evt.cq4")
                .wireType(WireType.BINARY_LIGHT)
                .build();
        final MessageSender messageSender = ipcSender(folder);
        return new HashFeedbackApplication(state, input).launch(config -> config
                .processorSourceId(PROCESSOR_SOURCE_ID)
                .commandSender(messageSender)
                .eventStore(new ChronicleMessageStore(eq))
                .input(INPUT_SOURCE_ID, HashApplication.inputPoller(input))
                .timeSource(new DefaultMutableTimeSource())
                .idleStrategy(BusySpinIdleStrategy.INSTANCE)
        );
    }
}
