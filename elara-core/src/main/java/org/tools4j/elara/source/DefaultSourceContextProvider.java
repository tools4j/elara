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
package org.tools4j.elara.source;

import org.agrona.collections.Hashing;
import org.agrona.collections.Int2ObjectHashMap;
import org.tools4j.elara.app.state.BaseState;
import org.tools4j.elara.send.SenderSupplier;
import org.tools4j.elara.sequence.SequenceGenerator;

import java.util.function.IntFunction;

import static java.util.Objects.requireNonNull;

public class DefaultSourceContextProvider implements SourceContextProvider {

    private static final int DEFAULT_INITIAL_CAPACITY = 64;

    private final Int2ObjectHashMap<DefaultSourceContext> contextBySourceId;
    private final IntFunction<DefaultSourceContext> sourceContextFactory;

    public DefaultSourceContextProvider(final BaseState baseState,
                                        final SenderSupplier senderSupplier) {
        this(DEFAULT_INITIAL_CAPACITY, baseState, senderSupplier, sourceId -> SequenceGenerator.create());
    }

    public DefaultSourceContextProvider(final int initialCapacity,
                                        final BaseState baseState,
                                        final SenderSupplier senderSupplier,
                                        final IntFunction<? extends SequenceGenerator> sourceSequenceGeneratorFactory) {
        requireNonNull(baseState);
        requireNonNull(senderSupplier);
        this.contextBySourceId = new Int2ObjectHashMap<>(initialCapacity, Hashing.DEFAULT_LOAD_FACTOR);
        this.sourceContextFactory = sourceId -> new DefaultSourceContext(sourceId, baseState, senderSupplier,
                sourceSequenceGeneratorFactory.apply(sourceId));
    }

    @Override
    public SourceContext sourceContext() {
        int sourceId = contextBySourceId.size() + 1;
        while (contextBySourceId.containsKey(sourceId)) {
            sourceId++;
        }
        return sourceContext(sourceId);
    }

    @Override
    public SourceContext sourceContext(final int sourceId) {
        return contextBySourceId.computeIfAbsent(sourceId, sourceContextFactory);
    }

    @Override
    public SourceContext sourceContext(final int sourceId, final long minSourceSeq) {
        final SourceContext context = sourceContext(sourceId);
        context.commandTracker().transientCommandState().sourceSequenceGenerator().nextSequence(minSourceSeq);
        return context;
    }
}
