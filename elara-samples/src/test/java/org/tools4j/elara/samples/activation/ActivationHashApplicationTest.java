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
package org.tools4j.elara.samples.activation;

import net.openhft.chronicle.queue.ChronicleQueue;
import net.openhft.chronicle.wire.WireType;
import org.junit.jupiter.api.Test;
import org.tools4j.elara.app.config.CommandPollingMode;
import org.tools4j.elara.chronicle.ChronicleMessageStore;
import org.tools4j.elara.plugin.activation.ActivationPlugin;
import org.tools4j.elara.plugin.activation.CommandCachingMode;
import org.tools4j.elara.plugin.api.Plugins;
import org.tools4j.elara.run.ElaraRunner;
import org.tools4j.elara.samples.hash.HashApplication;
import org.tools4j.elara.samples.hash.HashApplication.DefaultState;
import org.tools4j.elara.samples.hash.HashPassthroughApplication;
import org.tools4j.elara.store.InMemoryStore;

import java.util.Random;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.LockSupport;
import java.util.function.IntPredicate;

import static java.util.Objects.requireNonNull;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.tools4j.elara.samples.activation.ActivationHashApplicationTest.CommandStoreOption.PASSTHROUGH;
import static org.tools4j.elara.samples.hash.HashApplication.NULL_VALUE;

/**
 * Unit test running the {@link HashApplication} with the {@link ActivationPlugin}.
 */
public class ActivationHashApplicationTest {

    enum CommandStoreOption {
        NONE, MEMORY, PERSISTED, PASSTHROUGH
    }

    @FunctionalInterface
    interface InlineAsserts {
        void assertAt(int nextIndex, boolean replay);
    }

    @Test
    public void discardCommands_NoCommandStore() throws Exception {
        discardCommands(false, CommandStoreOption.NONE);
    }
    @Test
    public void discardCommands_InMemoryCommandStore() throws Exception {
        discardCommands(false, CommandStoreOption.MEMORY);
    }
    @Test
    public void discardCommands_PersistedCommandStore() throws Exception {
        discardCommands(false, CommandStoreOption.PERSISTED);

        //run again, this time commands will be skipped as they are duplicate
        discardCommands(true, CommandStoreOption.PERSISTED);
    }
    @Test
    public void discardCommands_PassthroughApp() throws Exception {
        //given
        final int n = 200;
        final int timeoutSeconds = 10;
        final long timeout = System.currentTimeMillis() + SECONDS.toMillis(timeoutSeconds);
        final HashApplication.ModifiableState state = new DefaultState();
        final IntPredicate active = index -> index < 50 || index >= n - 20;
        final long expectedCount = 70;
        final long expectedHash = 1282549506347406386L;
        final InlineAsserts inlineAsserts = (nextIndex, replay) -> {};

        //when
        runWithCommandReplayMode(n, false, CommandCachingMode.DISCARD, state, active, inlineAsserts, PASSTHROUGH);
        try (final ElaraRunner runner = HashPassthroughApplication.publisherWithState("activation-passthrough", state)) {
            while (state.count() < expectedCount) {
                if (System.currentTimeMillis() > timeout) {
                    throw new TimeoutException("Publisher did not terminate after " + timeoutSeconds + " seconds");
                }
                Thread.sleep(50);
            }
            runner.join(200);
        }

        //then
        assertEquals(expectedCount, state.count(), "state.count(" + n + ")");
        assertEquals(expectedHash, state.hash(), "state.hash(" + n + ")");
    }

    @Test
    public void replayCommands_InMemoryCommandStore() throws Exception {
        replayCommands(false, CommandStoreOption.MEMORY);
    }
    @Test
    public void replayCommands_PersistedCommandStore() throws Exception {
        replayCommands(false, CommandStoreOption.PERSISTED);

        //run again, this time commands will be skipped as they are duplicate
        replayCommands(true, CommandStoreOption.PERSISTED);
    }
    @Test
    public void replayCommands_NoCommandStore_fails() {
        assertThrows(IllegalArgumentException.class, () -> replayCommands(false, CommandStoreOption.NONE));
    }
    @Test
    public void replayCommands_PassthroughApp_fails() {
        assertThrows(IllegalArgumentException.class, () -> replayCommands(false, PASSTHROUGH));
    }

    private void discardCommands(final boolean isReplay, final CommandStoreOption queueMode) throws Exception {
        //given
        final int n = 200;
        final HashApplication.ModifiableState state = new DefaultState();
        final IntPredicate active = index -> index < 50 || index >= n - 20;
        final long expectedCount = 70;
        final long expectedHash = 1282549506347406386L;
        final InlineAsserts inlineAsserts = (nextIndex, replay) -> {
            if (nextIndex == 50 || nextIndex == n - 20) {
                sleep(100);
                final long expected = replay ? expectedCount : 50;
                assertEquals(expected, state.count(), "state.count(" + nextIndex + ")");
            }
        };

        //when
        runWithCommandReplayMode(n, isReplay, CommandCachingMode.DISCARD, state, active, inlineAsserts, queueMode);

        //then
        assertEquals(expectedCount, state.count(), "state.count(" + n + ")");
        assertEquals(expectedHash, state.hash(), "state.hash(" + n + ")");
    }

    private void replayCommands(final boolean isReplay, final CommandStoreOption queueMode) throws Exception {
        //given
        final int n = 200;
        final HashApplication.ModifiableState state = new DefaultState();
        final IntPredicate active = index -> index < 50 || index >= n - 20;
        final long expectedHash = 6244545253611137478L;
        final InlineAsserts inlineAsserts = (nextIndex, replay) -> {
            if (nextIndex == 50 || nextIndex == n - 20) {
                sleep(100);
                final long expected = replay ? n : nextIndex;
                assertEquals(expected, state.count(), "state.count(" + nextIndex + ")");
            }
        };

        //when
        runWithCommandReplayMode(n, isReplay, CommandCachingMode.REPLAY, state, active, inlineAsserts, queueMode);

        //then
        assertEquals(n, state.count(), "state.count(" + n + ")");
        assertEquals(expectedHash, state.hash(), "state.hash(" + n + ")");
    }

    private void runWithCommandReplayMode(final int n,
                                          final boolean isReplay,
                                          final CommandCachingMode replayMode,
                                          final HashApplication.ModifiableState state,
                                          final IntPredicate active,
                                          final InlineAsserts inlineAsserts,
                                          final CommandStoreOption queueMode) throws Exception {
        //given
        final AtomicLong input = new AtomicLong(NULL_VALUE);
        final Random random = new Random(123);
        final long shortSleepNanos = MILLISECONDS.toNanos(1);
        final long activationSleepNanos = MILLISECONDS.toNanos(500);
        final ActivationPlugin plugin = Plugins.activationPlugin(ActivationPlugin.configure()
                .commandCachingMode(replayMode)
        );

        //when
        try (final ElaraRunner runner = elaraRunner(queueMode, replayMode, state, input, plugin)) {
            assertTrue(plugin.activate());
            runHashApp(n, isReplay, random, shortSleepNanos, activationSleepNanos, input, active, inlineAsserts, plugin, runner);
        }
    }

    public static ElaraRunner elaraRunner(final CommandStoreOption queueMode,
                                          final CommandCachingMode replayMode,
                                          final HashApplication.ModifiableState state,
                                          final AtomicLong input,
                                          final ActivationPlugin plugin) {
        switch (queueMode) {
            case NONE:
                return noCommandStore(state, input, plugin);
            case MEMORY:
                return inMemory(state, input, plugin);
            case PERSISTED:
                return chronicleQueue(replayMode, state, input, plugin);
            case PASSTHROUGH:
                return passthrough(input, plugin);
            default:
                throw new IllegalArgumentException("Unsupported queue mode: " + queueMode);
        }
    }

    public static ElaraRunner noCommandStore(final HashApplication.ModifiableState state,
                                             final AtomicLong input,
                                             final ActivationPlugin plugin) {
        requireNonNull(plugin);
        return new HashApplication(state).launch(config -> config
                .input(HashApplication.DEFAULT_SOURCE_ID, HashApplication.inputPoller(input))
                .commandPollingMode(CommandPollingMode.NO_STORE)
                .eventStore(new InMemoryStore())
                .plugin(plugin)
        );
    }

    public static ElaraRunner inMemory(final HashApplication.ModifiableState state,
                                       final AtomicLong input,
                                       final ActivationPlugin plugin) {
        requireNonNull(plugin);
        return new HashApplication(state).launch(config -> config
                .input(HashApplication.DEFAULT_SOURCE_ID, HashApplication.inputPoller(input))
                .commandStore(new InMemoryStore())
                .eventStore(new InMemoryStore())
                .plugin(plugin)
        );
    }

    public static ElaraRunner chronicleQueue(final CommandCachingMode replayMode,
                                             final HashApplication.ModifiableState state,
                                             final AtomicLong input,
                                             final ActivationPlugin plugin) {
        requireNonNull(plugin);
        final String path = replayMode == CommandCachingMode.DISCARD ?
                "build/chronicle/activation/discard/" :
                "build/chronicle/activation/replay/";
        final ChronicleQueue cq = ChronicleQueue.singleBuilder()
                .path(path + "cmd.cq4")
                .wireType(WireType.BINARY_LIGHT)
                .build();
        final ChronicleQueue eq = ChronicleQueue.singleBuilder()
                .path(path + "evt.cq4")
                .wireType(WireType.BINARY_LIGHT)
                .build();
        return new HashApplication(state).launch(config -> config
                .input(HashApplication.DEFAULT_SOURCE_ID, HashApplication.inputPoller(input))
                .commandStore(new ChronicleMessageStore(cq))
                .eventStore(new ChronicleMessageStore(eq))
                .plugin(plugin)
        );
    }

    public static ElaraRunner passthrough(final AtomicLong input,
                                          final ActivationPlugin plugin) {
        requireNonNull(plugin);
        final ChronicleQueue eq = ChronicleQueue.singleBuilder()
                .path("build/chronicle/activation-passthrough/evt.cq4")
                .wireType(WireType.BINARY_LIGHT)
                .build();
        return new HashPassthroughApplication().launch(config -> config
                .input(HashApplication.DEFAULT_SOURCE_ID, HashApplication.inputPoller(input))
                .eventStore(new ChronicleMessageStore(eq))
                .plugin(plugin)
        );
    }

    private static void runHashApp(final int n,
                                   final boolean isReplay,
                                   final Random random,
                                   final long shortSleepNanos,
                                   final long activationSleepNanos,
                                   final AtomicLong input,
                                   final IntPredicate active,
                                   final InlineAsserts inlineAsserts,
                                   final ActivationPlugin plugin,
                                   final ElaraRunner runner) throws InterruptedException {
        long value = random.nextLong();
        boolean wasActive = true;
        for (int i = 0; i < n; ) {
            final boolean isActive = active.test(i);
            if (wasActive != isActive) {
                LockSupport.parkNanos(activationSleepNanos);
                if (isActive) plugin.activate();
                else plugin.deactivate();
                wasActive = isActive;
            }
            inlineAsserts.assertAt(i, isReplay);
            if (input.compareAndSet(NULL_VALUE, value)) {
                value = random.nextLong();
                i++;
            }
            if (shortSleepNanos > 0) {
                LockSupport.parkNanos(shortSleepNanos);
            }
        }
        while (input.get() != NULL_VALUE) {
            Thread.sleep(50);
        }
        runner.join(200);
    }

    private static void sleep(final int millis) {
        try {
            Thread.sleep(millis);
        } catch (final InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
}