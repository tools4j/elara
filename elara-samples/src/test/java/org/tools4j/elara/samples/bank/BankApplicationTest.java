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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.tools4j.elara.chronicle.ChronicleMessageLog;
import org.tools4j.elara.flyweight.FlyweightCommand;
import org.tools4j.elara.flyweight.FlyweightEvent;
import org.tools4j.elara.run.ElaraRunner;
import org.tools4j.elara.samples.bank.command.BankCommand;
import org.tools4j.elara.samples.bank.command.CreateAccountCommand;
import org.tools4j.elara.samples.bank.command.DepositCommand;
import org.tools4j.elara.samples.bank.command.TransferCommand;
import org.tools4j.elara.samples.bank.command.WithdrawCommand;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

public class BankApplicationTest {

    //under test
    private BankApplication bankApplication;

    @BeforeEach
    public void initApplication() {
        bankApplication = new BankApplication();
    }

    @Test
    public void inMemory() {
        final Queue<BankCommand> commands = initCommandQueue();
        try (final ElaraRunner runner = bankApplication.launch(commands)) {
            //when
            injectSomeCommands(commands);
            //then: await termination
            while (!commands.isEmpty()) {
                runner.join(20);
            }
            runner.join(200);
        }
        bankApplication.printEnd();
    }

    @Test
    public void chronicleQueue() throws Exception {
        final Queue<BankCommand> commands = initCommandQueue();
        final ChronicleQueue cq = ChronicleQueue.singleBuilder()
                .path("build/chronicle/bank/cmd.cq4")
                .wireType(WireType.BINARY_LIGHT)
                .build();
        final ChronicleQueue eq = ChronicleQueue.singleBuilder()
                .path("build/chronicle/bank/evt.cq4")
                .wireType(WireType.BINARY_LIGHT)
                .build();
        try (final ElaraRunner runner = bankApplication.launch(
                commands,
                new ChronicleMessageLog<>(cq, new FlyweightCommand()),
                new ChronicleMessageLog<>(eq, new FlyweightEvent())
        )) {
            //when
            injectSomeCommands(commands);
            //then: await termination
            while (!commands.isEmpty()) {
                runner.join(20);
            }
            runner.join(200);
        }
        bankApplication.printEnd();
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

    private static void sleep(final long millis) {
        try {
            Thread.sleep(millis);
        } catch (final InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(e);
        }
    }
}