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
package org.tools4j.elara.plugin.timer;

import org.agrona.collections.Hashing;
import org.agrona.collections.Int2ObjectHashMap;
import org.agrona.collections.Long2ObjectHashMap;
import org.agrona.collections.LongObjConsumer;
import org.tools4j.elara.app.message.Event;

import java.util.ArrayList;
import java.util.List;

import static java.util.Objects.requireNonNull;
import static org.tools4j.elara.plugin.timer.TimerEventHandler.NOOP;
import static org.tools4j.elara.plugin.timer.TimerIdGenerator.NIL_TIMER_ID;

final class DefaultTimerHandlerRegistry implements TimerHandlerRegistry {
    public static final int DEFAULT_INITIAL_HANDLERS = 64;
    public static final int DEFAULT_INITIAL_CONTEXTS = 256;

    private final int initialHandlers;
    private final Long2ObjectHashMap<TimerEventHandler> defaultHandlers;
    private final Int2ObjectHashMap<TimerEventHandler> typeHandlers;
    private final Long2ObjectHashMap<TimerEventHandler> contextDefaultHandlers;
    private final Long2ObjectHashMap<Int2ObjectHashMap<TimerEventHandler>> contextTypeHandlers;
    private final List<Int2ObjectHashMap<TimerEventHandler>> unusedContextHandlers;
    private final LongObjConsumer<Int2ObjectHashMap<TimerEventHandler>> contextTypeHandlerClearer = this::clearContextTypeHandler;
    DefaultTimerHandlerRegistry() {
        this(DEFAULT_INITIAL_HANDLERS, DEFAULT_INITIAL_CONTEXTS);
    }


    DefaultTimerHandlerRegistry(final int initialHandlers, final int initialContexts) {
        this.initialHandlers = initialHandlers;
        this.defaultHandlers = new Long2ObjectHashMap<>(initialHandlers, Hashing.DEFAULT_LOAD_FACTOR);
        this.typeHandlers = new Int2ObjectHashMap<>(initialHandlers, Hashing.DEFAULT_LOAD_FACTOR);
        this.contextDefaultHandlers = new Long2ObjectHashMap<>(initialContexts, Hashing.DEFAULT_LOAD_FACTOR);
        this.contextTypeHandlers = new Long2ObjectHashMap<>(initialContexts, Hashing.DEFAULT_LOAD_FACTOR);
        this.unusedContextHandlers = new ArrayList<>(initialContexts);
    }

    void onTimerEvent(final Event event, final Timer timer) {
        final long timerId = timer.timerId();
        final int timerType = timer.timerType();
        final long contextId = timer.contextId();

        //NOTE: more specific first, then default handlers

        //1) timer ID handler
        handle(defaultHandlers.getOrDefault(timerId, NOOP), event, timer);

        //2) context handlers
        final Int2ObjectHashMap<TimerEventHandler> contextHandlers = contextTypeHandlers.get(contextId);
        if (contextHandlers != null) {
            handle(contextHandlers.getOrDefault(timerType, NOOP), event, timer);
        }
        handle(contextDefaultHandlers.getOrDefault(contextId, NOOP), event, timer);

        //3) type ID handlers
        handle(typeHandlers.getOrDefault(timerType, NOOP), event, timer);

        //4) default handler
        handle(defaultHandlers.getOrDefault(NIL_TIMER_ID, NOOP), event, timer);
    }

    private void handle(final TimerEventHandler handler, final Event event, final Timer timer) {
        try {
            handler.onTimerEvent(event, timer);
        } catch (final Exception exception) {
            //FIXME handle exception here
            throw exception;
        }
    }

    @Override
    public void clear(final boolean dispose) {
        defaultHandlers.clear();
        typeHandlers.clear();
        contextDefaultHandlers.clear();
        if (dispose) {
            contextTypeHandlers.clear();
            unusedContextHandlers.clear();
        } else {
            contextTypeHandlers.forEachLong(contextTypeHandlerClearer);
            contextTypeHandlers.clear();
        }
    }

    private void clearContextTypeHandler(final long contextId,
                                         final Int2ObjectHashMap<TimerEventHandler> contextHandlers) {
        contextHandlers.clear();
        unusedContextHandlers.add(contextHandlers);
    }

    private static boolean hasExistingDifferentHandler(final TimerEventHandler existing,
                                                       final TimerEventHandler current) {
        return existing != null && existing != current;
    }

    private Int2ObjectHashMap<TimerEventHandler> reuseOrAllocateTypeHandlersMap() {
        if (unusedContextHandlers.isEmpty()) {
            return new Int2ObjectHashMap<>(initialHandlers, Hashing.DEFAULT_LOAD_FACTOR);
        }
        return unusedContextHandlers.remove(unusedContextHandlers.size() - 1);
    }

    @Override
    public void addTimerEventHandlerFor(final long timerId, final TimerEventHandler handler) {
        requireNonNull(handler);
        if (hasExistingDifferentHandler(defaultHandlers.putIfAbsent(timerId, handler), handler)) {
            throw new IllegalStateException("Handler has already been added for timer " + timerId);
        }
    }

    @Override
    public void addTimerEventHandler(final TimerEventHandler handler) {
        requireNonNull(handler);
        if (hasExistingDifferentHandler(defaultHandlers.putIfAbsent(NIL_TIMER_ID, handler), handler)) {
            throw new IllegalStateException("Default handler has already been added");
        }
    }

    @Override
    public void addTimerEventHandler(final int timerType, final TimerEventHandler handler) {
        requireNonNull(handler);
        if (hasExistingDifferentHandler(typeHandlers.putIfAbsent(timerType, handler), handler)) {
            throw new IllegalStateException("Default handler has already been added for timerType " + timerType);
        }
    }

    @Override
    public void addTimerEventHandler(final int timerType, final long contextId, final TimerEventHandler handler) {
        requireNonNull(handler);
        Int2ObjectHashMap<TimerEventHandler> contextHandlers = contextTypeHandlers.get(contextId);
        if (contextHandlers == null) {
            contextHandlers = reuseOrAllocateTypeHandlersMap();
            contextHandlers.put(timerType, handler);
            contextTypeHandlers.put(contextId, contextHandlers);
        } else {
            if (hasExistingDifferentHandler(contextHandlers.putIfAbsent(timerType, handler), handler)) {
                throw new IllegalStateException("Handler has already been added for timerType " + timerType +
                        " and contextId " + contextId);
            }
        }
    }

    @Override
    public void addTimerEventHandler(final long contextId, final TimerEventHandler handler) {
        requireNonNull(handler);
        if (hasExistingDifferentHandler(contextDefaultHandlers.putIfAbsent(contextId, handler), handler)) {
            throw new IllegalStateException("Default handler has already been added for contextId " + contextId);
        }
    }

    @Override
    public void addTimerSignalHandler(final TimerSignalHandler handler) {
        addTimerEventHandler(handler);
    }

    @Override
    public void addTimerSignalHandler(final int timerType, final TimerSignalHandler handler) {
        addTimerEventHandler(timerType, handler);
    }

    @Override
    public void addTimerSignalHandler(final int timerType, final long contextId, final TimerSignalHandler handler) {
        addTimerEventHandler(timerType, contextId, handler);
    }

    @Override
    public void addTimerSignalHandler(final long contextId, final TimerSignalHandler handler) {
        addTimerEventHandler(contextId, handler);
    }

    @Override
    public boolean removeTimerEventHandler(final TimerEventHandler handler) {
        return defaultHandlers.remove(NIL_TIMER_ID, handler);
    }

    @Override
    public boolean removeTimerEventHandlerFor(final long timerId, final TimerEventHandler handler) {
        return defaultHandlers.remove(timerId, handler);
    }

    @Override
    public boolean removeTimerEventHandler(final int timerType, final TimerEventHandler handler) {
        return typeHandlers.remove(timerType, handler);
    }

    @Override
    public boolean removeTimerEventHandler(final int timerType, final long contextId, final TimerEventHandler handler) {
        final Int2ObjectHashMap<TimerEventHandler> contextHandlers = contextTypeHandlers.get(contextId);
        if (contextHandlers == null || !contextHandlers.remove(timerType, handler)) {
            return false;
        }
        if (contextHandlers.isEmpty()) {
            contextTypeHandlers.remove(contextId);
            unusedContextHandlers.add(contextHandlers);
        }
        return true;
    }

    @Override
    public boolean removeTimerEventHandler(final long contextId, final TimerEventHandler handler) {
        return contextDefaultHandlers.remove(contextId, handler);
    }

    @Override
    public boolean removeTimerEventHandlers(final long contextId) {
        boolean removed = contextDefaultHandlers.remove(contextId) != null;
        final Int2ObjectHashMap<TimerEventHandler> contextHandlers = contextTypeHandlers.remove(contextId);
        if (contextHandlers != null) {
            removed = !contextHandlers.isEmpty();
            contextHandlers.clear();
            unusedContextHandlers.add(contextHandlers);
        }
        return removed;
    }
}
