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

import org.agrona.BitUtil;
import org.tools4j.elara.app.message.Event;
import org.tools4j.elara.input.Input;
import org.tools4j.elara.plugin.api.SystemPlugin;
import org.tools4j.elara.plugin.timer.TimerController.ControlContext;
import org.tools4j.elara.route.EventRouter;
import org.tools4j.elara.send.CommandSender;
import org.tools4j.elara.source.CommandSource;
import org.tools4j.elara.time.TimeSource;

import static java.util.Objects.requireNonNull;

/**
 * Timer plugin to support timers; timers can be started and cancelled through the timer {@link #controller()}.
 */
public class TimerPlugin implements SystemPlugin<MutableTimerState> {
    public static final int DEFAULT_SOURCE_ID = -10;
    public static final boolean DEFAULT_USE_INTERCEPTOR = true;
    public static final int DEFAULT_SIGNAL_INPUT_SKIP = 16;

    private final int sourceId;
    private final boolean useInterceptor;
    private final int signalInputSkip;
    private final TimerPluginSpecification specification = new TimerPluginSpecification(this);
    private final FlyweightTimerController controller = new FlyweightTimerController();
    private final DefaultTimerHandlerRegistry registry = new DefaultTimerHandlerRegistry();
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

    void init(final TimeSource timeSource, final TimerIdGenerator timerIdGenerator, final MutableTimerState timerState) {
        controller.init(timeSource, timerIdGenerator, timerState);
    }

    void onTimerEvent(final Event event, final Timer timer) {
        registry.onTimerEvent(event, timer);
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
     * {@link TimerSignalPoller}.
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
     * <p>
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
     * <p>
     * To auto-reset the controller after use, it is recommended to use it inside a try-resource loop:
     * <pre>
     *     try (final ControlContext timerControl = timerPlugin.controller(sourceContext)) {
     *         timerControl.startTimer(..)
     *     }
     * </pre>
     *
     * @param commandSource the command source to initialize the controller for sending timer commands
     * @return the controller to start or cancel cancel timers via commands
     */
    public ControlContext controller(final CommandSource commandSource) {
        return controller.init(null, requireNonNull(commandSource.commandSender()));
    }

    /**
     * Returns a controller to start and cancel timers. Timer control operations will result in timer commands sent
     * under the provided source context.
     * <p>
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
     * <p>
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
     * <p>
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

    /**
     * Returns a {@link TimerHandlerRegistry registry} to add and remove timer event handlers.
     *
     * @return the registry to add and remove handlers for timer events
     * @see TimerHandlerRegistry
     */
    public TimerHandlerRegistry registry() {
        return registry;
    }

    @Override
    public SystemPluginSpecification<MutableTimerState> specification() {
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

    @Override
    public String toString() {
        final TimerState timerState = controller.timerState();;
        return "TimerPlugin" +
                ":source-id=" + sourceId +
                "|use-interceptor=" + useInterceptor +
                "|signal-input-skip=" + signalInputSkip +
                "|timer-state=" + (timerState != null ? timerState : "(not initialized)");
    }
}