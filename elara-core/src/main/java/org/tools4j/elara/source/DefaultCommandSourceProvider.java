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
package org.tools4j.elara.source;

import org.agrona.collections.Hashing;
import org.agrona.collections.Int2IntHashMap;
import org.tools4j.elara.app.state.BaseEngineState;
import org.tools4j.elara.app.state.BaseEventState;
import org.tools4j.elara.app.state.BaseState;
import org.tools4j.elara.app.state.EventState;
import org.tools4j.elara.app.state.MutableEventProcessingState;
import org.tools4j.elara.app.state.MutableInFlightState;
import org.tools4j.elara.app.state.TransientEngineState;
import org.tools4j.elara.send.SenderSupplier;
import org.tools4j.elara.sequence.SequenceGenerator;

import java.util.ArrayList;
import java.util.List;
import java.util.function.IntFunction;

import static java.util.Objects.requireNonNull;

public class DefaultCommandSourceProvider implements CommandSourceProvider {

    private static final int DEFAULT_INITIAL_CAPACITY = 64;
    private static final int INDEX_NOT_AVAILABLE = -1;

    private final List<CommandSource> sources;
    private final Int2IntHashMap sourceIdToIndex;
    private final MutableInFlightState inFlightState;
    private final TransientEngineState transientEngineState;
    private final SenderSupplier senderSupplier;
    private final IntFunction<? extends SequenceGenerator> sourceSequenceGeneratorFactory;
    private final IntFunction<EventState> eventStateFactory;

    public DefaultCommandSourceProvider(final BaseState baseState,
                                        final MutableInFlightState inFlightState,
                                        final SenderSupplier senderSupplier) {
        this(DEFAULT_INITIAL_CAPACITY, baseState, inFlightState, senderSupplier, sourceId -> SequenceGenerator.create());
    }

    public DefaultCommandSourceProvider(final int initialCapacity,
                                        final BaseState baseState,
                                        final MutableInFlightState inFlightState,
                                        final SenderSupplier senderSupplier,
                                        final IntFunction<? extends SequenceGenerator> sourceSequenceGeneratorFactory) {
        this.sources = new ArrayList<>(initialCapacity);
        this.sourceIdToIndex = new Int2IntHashMap(initialCapacity, Hashing.DEFAULT_LOAD_FACTOR, INDEX_NOT_AVAILABLE);
        this.senderSupplier = requireNonNull(senderSupplier);
        this.inFlightState = requireNonNull(inFlightState);
        this.sourceSequenceGeneratorFactory = requireNonNull(sourceSequenceGeneratorFactory);
        if (baseState instanceof MutableEventProcessingState) {
            final MutableEventProcessingState eventProcessingState = (MutableEventProcessingState)baseState;
            this.transientEngineState = eventProcessingState.transientEngineState();
            this.eventStateFactory = eventProcessingState::lastProcessedEventCreateIfAbsent;
        } else {
            this.transientEngineState = new BaseEngineState(baseState);
            this.eventStateFactory = sourceId -> new BaseEventState(sourceId, baseState);
        }
    }

    @Override
    public int sources() {
        return sources.size();
    }

    @Override
    public CommandSource sourceByIndex(final int index) {
        return sources.get(index);
    }

    @Override
    public CommandSource sourceById(final int sourceId) {
        final int index = sourceIdToIndex.getOrDefault(sourceId, INDEX_NOT_AVAILABLE);
        if (index != INDEX_NOT_AVAILABLE) {
            return sources.get(index);
        }
        final SequenceGenerator seqGenerator = sourceSequenceGeneratorFactory.apply(sourceId);
        final EventState eventState = eventStateFactory.apply(sourceId);
        final CommandSource commandSource = new DefaultCommandSource(
                sourceId, senderSupplier, seqGenerator, eventState, inFlightState, transientEngineState
        );
        final int newIndex = sources.size();
        sourceIdToIndex.put(sourceId, newIndex);
        sources.add(commandSource);
        return commandSource;
    }

    @Override
    public String toString() {
        return "DefaultCommandSourceProvider:sources=" + sources;
    }
}
