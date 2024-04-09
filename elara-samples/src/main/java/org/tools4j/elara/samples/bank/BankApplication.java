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
package org.tools4j.elara.samples.bank;

import org.tools4j.elara.app.message.Command;
import org.tools4j.elara.app.message.Event;
import org.tools4j.elara.app.type.AllInOneApp;
import org.tools4j.elara.exception.DuplicateHandler;
import org.tools4j.elara.input.InputPoller;
import org.tools4j.elara.output.Output;
import org.tools4j.elara.plugin.api.Plugins;
import org.tools4j.elara.route.EventRouter;
import org.tools4j.elara.run.ElaraRunner;
import org.tools4j.elara.samples.bank.actor.Accountant;
import org.tools4j.elara.samples.bank.actor.Teller;
import org.tools4j.elara.samples.bank.command.BankCommand;
import org.tools4j.elara.samples.bank.state.Bank;
import org.tools4j.elara.send.CommandContext;
import org.tools4j.elara.send.CommandSender;
import org.tools4j.elara.send.CommandSender.SendingContext;
import org.tools4j.elara.store.InMemoryStore;
import org.tools4j.elara.store.MessageStore;

import java.util.Queue;

import static java.util.Objects.requireNonNull;

public class BankApplication implements AllInOneApp, Output {

    private static final int SOURCE_ID = 666;
    private final Bank.Mutable bank = new Bank.Default();
    private final Teller teller = new Teller(bank);
    private final Accountant accountant = new Accountant(bank);
    private final PayloadPrinter flyweightString = new PayloadPrinter();

    private final DuplicateHandler duplicateHandler = command -> {
        System.out.println("-----------------------------------------------------------");
        flyweightString.reset().append("skipping: ").append(command).println();
    };

    public Bank bank() {
        return bank;
    }

    public ElaraRunner launch(final Queue<? extends BankCommand> inputQueue) {
        return launch(inputQueue, new InMemoryStore(), new InMemoryStore());
    }

    public ElaraRunner launch(final Queue<? extends BankCommand> inputQueue,
                              final MessageStore commandStore,
                              final MessageStore eventStore) {
        return launch(new CommandInputPoller(inputQueue), commandStore, eventStore);
    }

    public ElaraRunner launch(final InputPoller inputPoller,
                              final MessageStore commandStore,
                              final MessageStore eventStore) {
        return launch(config -> config
                .input(SOURCE_ID, inputPoller)
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
        flyweightString.reset().append("processing: ").append(command).println();
        teller.onCommand(command, router);
    }

    @Override
    public void onEvent(final Event event) {
        flyweightString.reset().append("applying: ").append(event).println();
        accountant.onEvent(event);
        printBankAccounts(bank);
    }

    private void printBankAccounts(final Bank bank) {
        System.out.println("bank accounts:");
        for (int i = 0; i < bank.accounts().size(); i++) {
            final CharSequence account = bank.accounts().get(i);
            flyweightString.reset()
                    .append("...").append(account)
                    .append(":\tbalance=").append(bank.account(account).balance())
                    .println();
        }
    }

    @Override
    public Ack publish(final Event event, final boolean replay, final int retry) {
        flyweightString.reset().append("published: ").append(event).append(", replay=").append(replay).append(", retry=").append(retry).println();
        return Ack.COMMIT;
    }

    private static class CommandInputPoller implements InputPoller {
        final Queue<? extends BankCommand> commands;

        CommandInputPoller(final Queue<? extends BankCommand> commands) {
            this.commands = requireNonNull(commands);
        }

        @Override
        public int poll(final CommandContext commandContext, final CommandSender commandSender) {
            final BankCommand cmd = commands.poll();
            if (cmd != null) {
                final int type = cmd.type().value;
                try (final SendingContext context = commandSender.sendingCommand(type)) {
                    final int length = cmd.encodeTo(context.buffer(), 0);
                    context.send(length);
                }
                return 1;
            }
            return 0;
        }
    }
}