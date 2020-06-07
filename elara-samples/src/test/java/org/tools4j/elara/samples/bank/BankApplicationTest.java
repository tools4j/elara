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

import net.openhft.chronicle.queue.ChronicleQueue;
import net.openhft.chronicle.wire.WireType;
import org.junit.jupiter.api.Test;
import org.tools4j.elara.chronicle.ChronicleMessageLog;
import org.tools4j.elara.run.ElaraRunner;
import org.tools4j.elara.samples.bank.command.BankCommand;
import org.tools4j.elara.samples.bank.command.CreateAccountCommand;
import org.tools4j.elara.samples.bank.command.DepositCommand;
import org.tools4j.elara.samples.bank.command.TransferCommand;
import org.tools4j.elara.samples.bank.command.WithdrawCommand;
import org.tools4j.elara.samples.bank.state.Bank;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class BankApplicationTest {

    @Test
    public void inMemory() {
        //given
        final BankApplication bankApplication = new BankApplication();
        final Queue<BankCommand> commands = initCommandQueue();

        //when
        try (final ElaraRunner runner = bankApplication.launch(commands)) {
            injectSomeCommands(commands);
            while (!commands.isEmpty()) {
                runner.join(20);
            }
            runner.join(200);
        }
        bankApplication.printEnd();

        //then
        assertBankAccounts(bankApplication.bank());
    }

    @Test
    public void chronicleQueue() {
        //given
        Queue<BankCommand> commands;

        //when
        commands = initCommandQueue();
        final BankApplication bankOne = new BankApplication();
        try (final ElaraRunner runner = bankOne.launch(
                commands,
                new ChronicleMessageLog(commandQueue()),
                new ChronicleMessageLog(eventQueue())
        )) {
            injectSomeCommands(commands);
            while (!commands.isEmpty()) {
                runner.join(20);
            }
            runner.join(200);
        }
        bankOne.printEnd();

        //then
        assertBankAccounts(bankOne.bank());

        //when
        commands = initCommandQueue();
        final BankApplication bankTwo = new BankApplication();
        try (final ElaraRunner runner = bankTwo.launch(
                commands,
                new ChronicleMessageLog(commandQueue()),
                new ChronicleMessageLog(eventQueue())
        )) {
            injectSomeCommands(commands);
            while (!commands.isEmpty()) {
                runner.join(20);
            }
            runner.join(200);
        }
        bankTwo.printEnd();

        //then
        assertBankAccounts(bankTwo.bank());
    }

    private ChronicleQueue commandQueue() {
        return ChronicleQueue.singleBuilder()
                .path("build/chronicle/bank/cmd.cq4")
                .wireType(WireType.BINARY_LIGHT)
                .build();
    }

    private ChronicleQueue eventQueue() {
        return ChronicleQueue.singleBuilder()
                .path("build/chronicle/bank/evt.cq4")
                .wireType(WireType.BINARY_LIGHT)
                .build();
    }

    private Queue<BankCommand> initCommandQueue() {
        final Queue<BankCommand> commands = new ConcurrentLinkedQueue<>();
        commands.add(new CreateAccountCommand("Marco"));
        commands.add(new CreateAccountCommand("Henry"));
        commands.add(new CreateAccountCommand("Frank"));
        return commands;
    }

    private void injectSomeCommands(final Queue<BankCommand> commands) {
        sleep(500);
        //WHEN: deposits
        commands.add(new DepositCommand("Marco", 1000.0));
        commands.add(new DepositCommand("Frank", 200.0));
        sleep(500);

        //WHEN: deposits + withdrawals
        commands.add(new DepositCommand("Marco", 200.0));
        commands.add(new WithdrawCommand("Frank", 50.0));
        sleep(500);

        //WHEN: illegal stuff
        commands.add(new WithdrawCommand("Henry", 1.0));
        commands.add(new DepositCommand("Lawry", 50.0));
        commands.add(new TransferCommand("Frank", "Marco", 200.0));

        //WHEN: other stuff that works
        commands.add(new CreateAccountCommand("Lawry"));
        commands.add(new DepositCommand("Lawry", 50.0));
        commands.add(new TransferCommand("Marco", "Frank", 100.0));
        commands.add(new WithdrawCommand("Frank", 200.0));
    }

    private void assertBankAccounts(final Bank bank) {
        assertEquals(4, bank.accounts().size(), "accounts.size");
        assertEquals(1100, bank.account("Marco").balance(),  "accounts('Marco').balance");
        assertEquals(0, bank.account("Henry").balance(),  "accounts('Henry').balance");
        assertEquals(50, bank.account("Frank").balance(),  "accounts('Frank').balance");
        assertEquals(50, bank.account("Lawry").balance(),  "accounts('Lawry').balance");
        System.out.println(bank.accounts().size() + " bank accounts asserted.");
    }

    private static void sleep(final long millis) {
        try {
            Thread.sleep(millis);
        } catch (final InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(e);
        }
    }
}