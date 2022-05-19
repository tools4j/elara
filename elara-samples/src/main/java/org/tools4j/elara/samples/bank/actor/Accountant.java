/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2020-2022 tools4j.org (Marco Terzer, Anton Anufriev)
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
package org.tools4j.elara.samples.bank.actor;

import org.tools4j.elara.app.handler.EventApplier;
import org.tools4j.elara.event.Event;
import org.tools4j.elara.samples.bank.event.AccountCreatedEvent;
import org.tools4j.elara.samples.bank.event.AmountAddedOrRemovedEvent;
import org.tools4j.elara.samples.bank.event.EventType;
import org.tools4j.elara.samples.bank.state.Bank;

import static java.util.Objects.requireNonNull;

public class Accountant implements EventApplier {

    private final Bank.Mutable bank;

    public Accountant(final Bank.Mutable bank) {
        this.bank = requireNonNull(bank);
    }

    @Override
    public void onEvent(final Event event) {
        if (event.isApplication()) {
            final EventType type = EventType.byValue(event.type());
            switch (type) {
                case AccountCreated: {
                    final AccountCreatedEvent evt = AccountCreatedEvent.decode(event.payload());
                    bank.openAccount(evt.name);
                    break;
                }
                case AmountAddedOrRemoved: {
                    final AmountAddedOrRemovedEvent evt = AmountAddedOrRemovedEvent.decode(event.payload());
                    bank.account(evt.account).add(evt.change);
                    break;
                }
            }
        }
    }

}
