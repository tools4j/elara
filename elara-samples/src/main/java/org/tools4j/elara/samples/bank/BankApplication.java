/**
 * The MIT License (MIT)
 *
 * Copyright (c) 2020 tools4j.org (Marco Terzer, Anton Anufriev)
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
import org.tools4j.elara.application.Application;
import org.tools4j.elara.application.CommandProcessor;
import org.tools4j.elara.application.DuplicateHandler;
import org.tools4j.elara.application.EventApplier;
import org.tools4j.elara.command.Command;
import org.tools4j.elara.command.CommandLoopback;
import org.tools4j.elara.flyweight.FlyweightCommand;
import org.tools4j.elara.event.Event;
import org.tools4j.elara.event.EventRouter;
import org.tools4j.elara.flyweight.FlyweightEvent;
import org.tools4j.elara.init.Context;
import org.tools4j.elara.init.Launcher;
import org.tools4j.elara.input.Input;
import org.tools4j.elara.log.InMemoryLog;
import org.tools4j.elara.log.MessageLog;
import org.tools4j.elara.log.PeekableMessageLog;
import org.tools4j.elara.samples.bank.actor.Accountant;
import org.tools4j.elara.samples.bank.actor.Teller;
import org.tools4j.elara.samples.bank.command.BankCommand;
import org.tools4j.elara.samples.bank.command.CommandType;
import org.tools4j.elara.samples.bank.event.EventType;
import org.tools4j.elara.samples.bank.state.Bank;

import java.util.Queue;

import static java.util.Objects.requireNonNull;

public class BankApplication implements Application {

    private final Bank.Mutable bank = new Bank.Default();
    private final Teller teller = new Teller(bank);
    private final Accountant accountant = new Accountant(bank);

    private final CommandProcessor commandProcessor = this::process;
    private final EventApplier eventApplier = this::apply;
    private final DuplicateHandler duplicateHandler = new DuplicateHandler() {
        @Override
        public void skipCommandProcessing(final Command command) {
            System.out.println("-----------------------------------------------------------");
            System.out.println("skipping: " + command + ", payload=" + payloadFor(command));
        }

        @Override
        public void skipEventApplying(final Event event) {
            //System.out.println("skipping: " + event + ", payload=" + payloadFor(event));
        }

        @Override
        public void dropCommandReceived(final Command command) {
            System.out.println("dropping: " + command + ", payload=" + payloadFor(command));
        }
    };

    @Override
    public CommandProcessor commandProcessor() {
        return commandProcessor;
    }

    @Override
    public EventApplier eventApplier() {
        return eventApplier;
    }

    public Launcher launch(final Queue<BankCommand> inputQueue) {
        return launch(inputQueue,
                new InMemoryLog<>(new FlyweightCommand()),
                new InMemoryLog<>(new FlyweightEvent()));
    }

    public Launcher launch(final Queue<BankCommand> inputQueue,
                           final PeekableMessageLog<Command> commandLog,
                           final MessageLog<Event> eventLog) {
        return launch(Input.create(666, new CommandPoller(inputQueue)),
                commandLog, eventLog);
    }

    public Launcher launch(final Input input,
                           final PeekableMessageLog<Command> commandLog,
                           final MessageLog<Event> eventLog) {
        return Launcher.launch(Context.create(this)
                .input(input)
                .output(this::publish)
                .commandLog(commandLog)
                .eventLog(eventLog)
                .duplicateHandler(duplicateHandler)
        );
    }

    public void printEnd() {
        System.out.println("===========================================================");
    }

    private void process(final Command command, final EventRouter router) {
        System.out.println("-----------------------------------------------------------");
        System.out.println("processing: " + command + ", payload=" + payloadFor(command));
        teller.onCommand(command, router);
    }

    private void apply(final Event event, final CommandLoopback commandLoopback) {
        System.out.println("applying: " + event + ", payload=" + payloadFor(event));
        accountant.onEvent(event, commandLoopback);
        if (event.isCommit()) {
            printBankAccounts(bank);
        }
    }

    private static void printBankAccounts(final Bank bank) {
        System.out.println("bank accounts:");
        for (final String account : bank.accounts()) {
            System.out.println("..." + account + ":\tbalance=" + bank.account(account).balance());

        }

    }

    private void publish(final Event event) {
        System.out.println("published: " + event + ", payload=" + payloadFor(event));
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

    private static class CommandPoller implements Input.Poller {
        final Queue<BankCommand> commands;
        long seq = 0;

        CommandPoller(final Queue<BankCommand> commands) {
            this.commands = requireNonNull(commands);
        }

        @Override
        public int poll(final Input.Handler handler) {
            final BankCommand cmd = commands.poll();
            if (cmd != null) {
                final int type = cmd.type().value;
                final DirectBuffer encoded = cmd.encode();
                handler.onMessage(++seq, type, encoded, 0, encoded.capacity());
                return 1;
            }
            return 0;
        }
    }
}