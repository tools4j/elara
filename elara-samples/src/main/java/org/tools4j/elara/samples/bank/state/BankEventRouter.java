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
package org.tools4j.elara.samples.bank.state;

import org.tools4j.elara.route.EventRouter;
import org.tools4j.elara.route.EventRouter.RoutingContext;
import org.tools4j.elara.samples.bank.flyweight.FlyweightAccountCreatedEvent;
import org.tools4j.elara.samples.bank.flyweight.FlyweightAccountCreationRejectedEvent;
import org.tools4j.elara.samples.bank.flyweight.FlyweightAmountAddedOrRemovedEvent;
import org.tools4j.elara.samples.bank.flyweight.FlyweightTransactionRejectedEvent;

import static java.util.Objects.requireNonNull;

/**
 * An application specific router that wraps around the Elara {@link EventRouter} with methods to route all the events
 * supported by the bank app.
 */
public class BankEventRouter {

    private final FlyweightAccountCreatedEvent accountCreatedEvent = new FlyweightAccountCreatedEvent();
    private final FlyweightAccountCreationRejectedEvent accountCreationRejectedEvent = new FlyweightAccountCreationRejectedEvent();
    private final FlyweightAmountAddedOrRemovedEvent amountAddedOrRemovedEvent = new FlyweightAmountAddedOrRemovedEvent();
    private final FlyweightTransactionRejectedEvent transactionRejectedEvent = new FlyweightTransactionRejectedEvent();

    private EventRouter router;

    public void init(final EventRouter router) {
        this.router = requireNonNull(router);
    }

    public void reset() {
        this.router = null;
    }

    public void routeAccountCreatedEvent(final CharSequence name) {
        try (final RoutingContext context = router.routingEvent(accountCreatedEvent.type().value)) {
            context.route(accountCreatedEvent.wrap(context.buffer(), 0)
                    .name(name)
                    .encodingLength()
            );
        }
    }

    public void routeAccountCreationRejectedEvent(final CharSequence account, final CharSequence reason) {
        try (final RoutingContext context = router.routingEvent(accountCreationRejectedEvent.type().value)) {
            context.route(accountCreationRejectedEvent.wrap(context.buffer(), 0)
                    .account(account)
                    .reason(reason)
                    .encodingLength()
            );
        }
    }

    public void routeAmountAddedOrRemovedEvent(final CharSequence account, final double change) {
        try (final RoutingContext context = router.routingEvent(amountAddedOrRemovedEvent.type().value)) {
            context.route(amountAddedOrRemovedEvent.wrap(context.buffer(), 0)
                    .account(account)
                    .change(change)
                    .encodingLength());
        }
    }

    public void routeTransactionRejectedEvent(final CharSequence account, final CharSequence reason) {
        try (final RoutingContext context = router.routingEvent(transactionRejectedEvent.type().value)) {
            context.route(transactionRejectedEvent.wrap(context.buffer(), 0)
                    .account(account)
                    .reason(reason)
                    .encodingLength());
        }
    }

}
