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
package org.tools4j.elara.plugin.timer;

import org.agrona.BitUtil;
import org.tools4j.elara.app.config.AppConfig;
import org.tools4j.elara.app.factory.Interceptor;
import org.tools4j.elara.app.factory.StateFactory;
import org.tools4j.elara.app.handler.CommandProcessor;
import org.tools4j.elara.app.handler.EventApplier;
import org.tools4j.elara.app.state.BaseState;
import org.tools4j.elara.app.state.MutableBaseState;
import org.tools4j.elara.event.Event;
import org.tools4j.elara.input.Input;
import org.tools4j.elara.plugin.api.PluginStateProvider;
import org.tools4j.elara.plugin.api.ReservedPayloadType;
import org.tools4j.elara.plugin.api.SystemPlugin;
import org.tools4j.elara.plugin.timer.TimerController.ControlContext;
import org.tools4j.elara.plugin.timer.TimerState.Mutable;
import org.tools4j.elara.route.EventRouter;
import org.tools4j.elara.send.CommandSender;
import org.tools4j.elara.sequence.SequenceGenerator;
import org.tools4j.elara.source.SourceContext;

import static java.util.Objects.requireNonNull;

/**
 * Timer plugin to support timers; timers can be started and cancelled through the timer {@link #controller()}.
 */
public class TimerPlugin implements SystemPlugin<TimerState.Mutable> {
    public static final int DEFAULT_SOURCE_ID = -10;
    public static final boolean DEFAULT_USE_INTERCEPTOR = true;
    public static final int DEFAULT_SIGNAL_INPUT_SKIP = 16;

    private final int sourceId;
    private final boolean useInterceptor;
    private final int signalInputSkip;
    private final Specification specification = new Specification();
    private final FlyweightTimerController controller = new FlyweightTimerController();
    private TimerEventHandler timerEventHandler = TimerEventHandler.NOOP;

    public TimerPlugin() {
        this(DEFAULT_SOURCE_ID);
    }
    public TimerPlugin(final int sourceId) {
        this(sourceId, DEFAULT_USE_INTERCEPTOR, DEFAULT_SIGNAL_INPUT_SKIP);
    }
    public TimerPlugin(final int sourceId, final boolean useInterceptor, final int signalInputSkip) {
        if (!BitUtil.isPowerOfTwo(signalInputSkip)) {
            throw new IllegalArgumentException("Invalid signalInputSkip value, must be a power of two: " +
                    signalInputSkip);
        }
        this.sourceId = sourceId;
        this.useInterceptor = useInterceptor;
        this.signalInputSkip = signalInputSkip;
    }

    public int sourceId() {
        return sourceId;
    }

    /**
     * True if interceptor mode is enabled (enabled by default).  Disabling this mode can be done to optimize
     * performance by removing extra handlers for the interception.  See {@link #controller()} for a description of the
     * interception benefits.
     *
     * @return true if interceptor mode is enabled (enabled by default).
     */
    public boolean useInterceptor() {
        return useInterceptor;
    }

    /**
     * Returns how many poll invocations are skipped before re-checking timeouts of timers in the
     * {@link TimerSignalInput}.
     *
     * @return  the polling interval after which timers are checked for expiry, one if every cycle should check, or a
     *          positive power of two to check less frequently
     */
    public int signalInputSkip() {
        return signalInputSkip;
    }

    /**
     * Returns a controller to start and cancel timers. Depending on the scope from which the controller is accessed,
     * this will be done either by sending commands or by routing events.
     * <p></p>
     * To auto-reset the controller after use, it is recommended to use it inside a try-resource loop:
     * <pre>
     *     try (final ControlContext timerControl = timerPlugin.controller()) {
     *         timerControl.startTimer(..)
     *     }
     * </pre>
     * Note that using this method requires {@link #useInterceptor() interceptor} mode to be enabled so that Elara
     * automatically initializes the controller appropriately.  The controller methods will throw an
     * {@link IllegalStateException} if interceptor mode is not enabled or if the invoked from an invalid scope (other
     * than command or event processing).  Use one of the other {@code controller(..)} methods for timer control from an
     * {@link Input} or if interceptor mode is disabled.
     *
     * @return the controller to start or cancel timers
     */
    public ControlContext controller() {
        return controller;
    }

    /**
     * Returns a controller to start and cancel timers. Timer control operations will result in timer commands sent
     * under the provided source context.
     * <p></p>
     * To auto-reset the controller after use, it is recommended to use it inside a try-resource loop:
     * <pre>
     *     try (final ControlContext timerControl = timerPlugin.controller(sourceContext)) {
     *         timerControl.startTimer(..)
     *     }
     * </pre>
     *
     * @param sourceContext the source context to initialize the controller for sending timer commands
     * @return the controller to start or cancel cancel timers via commands
     */
    public ControlContext controller(final SourceContext sourceContext) {
        return controller.init(null, requireNonNull(sourceContext.commandSender()));
    }

    /**
     * Returns a controller to start and cancel timers. Timer control operations will result in timer commands sent
     * under the provided source context.
     * <p></p>
     * To auto-reset the controller after use, it is recommended to use it inside a try-resource loop:
     * <pre>
     *     try (final ControlContext timerControl = timerPlugin.controller(input, commandSender)) {
     *         timerControl.startTimer(..)
     *     }
     * </pre>
     *
     * @param sourceContext the source context to initialize the controller for sending timer commands
     * @return the controller to start or cancel cancel timers via commands
     */
    public ControlContext controller(final CommandSender sourceContext) {
        return controller.init(null, requireNonNull(sourceContext));
    }

    /**
     * Returns a controller to start and cancel timers. Timer control operations will result in timer commands sent
     * using the provided command sender.  The controller's {@link TimerController#currentTime() currentTime()} method
     * will return the event time of the provided event.
     * <p></p>
     * To auto-reset the controller after use, it is recommended to use it inside a try-resource loop:
     * <pre>
     *     try (final ControlContext timerControl = timerPlugin.controller(event, commandSender)) {
     *         timerControl.startTimer(..)
     *     }
     * </pre>
     *
     * @param event the event whose time is used by the controller to give deterministically save current time values
     * @param commandSender the command sender to initialize the controller for sending timer commands
     * @return the controller to start or cancel timers via commands
     */
    public ControlContext controller(final Event event, final CommandSender commandSender) {
        return controller.init(requireNonNull(event), requireNonNull(commandSender));
    }

    /**
     * Returns a controller to start and cancel timers. Timer control operations will result in timer events routed
     * using the provided event router.
     * <p></p>
     * To auto-reset the controller after use, it is recommended to use it inside a try-resource loop:
     * <pre>
     *     try (final ControlContext timerControl = timerPlugin.controller(eventRouter)) {
     *         timerControl.startTimer(..)
     *     }
     * </pre>
     *
     * @param eventRouter the event router to initialize the controller for sending timer events
     * @return the controller to start or cancel cancel timers via events
     */
    public ControlContext controller(final EventRouter eventRouter) {
        return controller.init(eventRouter);
    }

    public void timerEventHandler(final TimerEventHandler timerEventHandler) {
        this.timerEventHandler = requireNonNull(timerEventHandler);
    }

    public TimerEventHandler timerEventHandler() {
        return timerEventHandler;
    }

    @Override
    public SystemPluginSpecification<TimerState.Mutable> specification() {
        return specification;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        final TimerPlugin that = (TimerPlugin) o;
        return sourceId == that.sourceId;
    }

    @Override
    public int hashCode() {
        return 31 * getClass().hashCode() + sourceId;
    }

    private final class Specification implements SystemPluginSpecification<TimerState.Mutable> {
        @Override
        public PluginStateProvider<Mutable> defaultPluginStateProvider() {
            return appConfig -> new SimpleTimerState();
        }

        @Override
        public ReservedPayloadType reservedPayloadType() {
            return ReservedPayloadType.TIMER;
        }

        @Override
        public Installer installer(final AppConfig appConfig, final TimerState.Mutable timerState) {
            requireNonNull(appConfig);
            requireNonNull(timerState);
            final SequenceGenerator timerIdGenerator = SequenceGenerator.create(1);
            controller.init(appConfig.timeSource(), timerIdGenerator, timerState);
            return new Installer.Default() {

                @Override
                public Input input(final BaseState baseState) {
                    final long sourceSeq = 1 + baseState.lastAppliedCommandSequence(sourceId);
                    return Input.single(sourceId, sourceSeq, new TimerSignalInput(appConfig.timeSource(), timerState,
                            TimerPlugin.this.signalInputSkip));
                }

                @Override
                public CommandProcessor commandProcessor(final BaseState baseState) {
                    return new TimerCommandProcessor(timerState);
                }

                @Override
                public EventApplier eventApplier(final MutableBaseState baseState) {
                    return new TimerEventApplier(TimerPlugin.this, timerState, timerIdGenerator);
                }

                @Override
                public Interceptor interceptor(final StateFactory stateFactory) {
                    return useInterceptor ? new TimerPluginInterceptor(TimerPlugin.this) : Interceptor.NOOP;
                }
            };
        }
    }
}