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
package org.tools4j.elara.samples.bank;

import org.agrona.DirectBuffer;
import org.tools4j.elara.app.type.AllInOneApp;
import org.tools4j.elara.command.Command;
import org.tools4j.elara.event.Event;
import org.tools4j.elara.exception.DuplicateHandler;
import org.tools4j.elara.input.Input;
import org.tools4j.elara.plugin.api.Plugins;
import org.tools4j.elara.route.EventRouter;
import org.tools4j.elara.run.ElaraRunner;
import org.tools4j.elara.samples.bank.actor.Accountant;
import org.tools4j.elara.samples.bank.actor.Teller;
import org.tools4j.elara.samples.bank.command.BankCommand;
import org.tools4j.elara.samples.bank.command.CommandType;
import org.tools4j.elara.samples.bank.event.EventType;
import org.tools4j.elara.samples.bank.state.Bank;
import org.tools4j.elara.send.CommandSender;
import org.tools4j.elara.send.SenderSupplier;
import org.tools4j.elara.store.InMemoryStore;
import org.tools4j.elara.store.MessageStore;

import java.util.Queue;

import static java.util.Objects.requireNonNull;

public class BankApplication implements AllInOneApp {

    private static final int SOURCE = 666;
    private final Bank.Mutable bank = new Bank.Default();
    private final Teller teller = new Teller(bank);
    private final Accountant accountant = new Accountant(bank);

    private final DuplicateHandler duplicateHandler = command -> {
        System.out.println("-----------------------------------------------------------");
        System.out.println("skipping: " + command + ", payload=" + payloadFor(command));
    };

    public Bank bank() {
        return bank;
    }

    public ElaraRunner launch(final Queue<BankCommand> inputQueue) {
        return launch(inputQueue, new InMemoryStore(), new InMemoryStore());
    }

    public ElaraRunner launch(final Queue<BankCommand> inputQueue,
                              final MessageStore commandStore,
                              final MessageStore eventStore) {
        return launch(new CommandInput(inputQueue), commandStore, eventStore);
    }

    public ElaraRunner launch(final Input input,
                              final MessageStore commandStore,
                              final MessageStore eventStore) {
        return launch(config -> config
                .input(input)
                .commandStore(commandStore)
                .eventStore(eventStore)
                .duplicateHandler(duplicateHandler)
                .plugin(Plugins.bootPlugin())
        );
    }

    public void printEnd() {
        System.out.println("===========================================================");
    }

    @Override
    public void onCommand(final Command command, final EventRouter router) {
        System.out.println("-----------------------------------------------------------");
        System.out.println("processing: " + command + ", payload=" + payloadFor(command));
        teller.onCommand(command, router);
    }

    @Override
    public void onEvent(final Event event) {
        System.out.println("applying: " + event + ", payload=" + payloadFor(event));
        accountant.onEvent(event);
        printBankAccounts(bank);
    }

    private static void printBankAccounts(final Bank bank) {
        System.out.println("bank accounts:");
        for (final String account : bank.accounts()) {
            System.out.println("..." + account + ":\tbalance=" + bank.account(account).balance());
        }
    }

    @Override
    public Ack publish(final Event event, final boolean replay, final int retry, final CommandSender loopback) {
        System.out.println("published: " + event + ", replay=" + replay + ", retry=" + retry + ", payload=" + payloadFor(event));
        return Ack.COMMIT;
    }

    private String payloadFor(final Command command) {
        if (command.isApplication()) {
            return CommandType.toString(command);
        }
        return "(unknown)";
    }

    private String payloadFor(final Event event) {
        if (event.isApplication()) {
            return EventType.toString(event);
        }
        return "(unknown)";
    }

    private static class CommandInput implements Input {
        final Queue<BankCommand> commands;

        CommandInput(final Queue<BankCommand> commands) {
            this.commands = requireNonNull(commands);
        }

        @Override
        public int poll(final SenderSupplier senderSupplier) {
            final BankCommand cmd = commands.poll();
            if (cmd != null) {
                final int type = cmd.type().value;
                final DirectBuffer encoded = cmd.encode();
                senderSupplier.senderFor(SOURCE).sendCommand(type, encoded, 0, encoded.capacity());
                return 1;
            }
            return 0;
        }
    }
}