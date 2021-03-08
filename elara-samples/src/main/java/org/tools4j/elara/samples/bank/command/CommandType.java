/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2021 tools4j.org (Marco Terzer, Anton Anufriev)
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
package org.tools4j.elara.samples.bank.command;

import org.tools4j.elara.command.Command;

public enum CommandType {
    CreateAccount(1),
    Deposit(2),
    Withdraw(3),
    Transfer(4);
    public final int value;
    CommandType(final int value) {
        this.value = value;
    }

    public static CommandType byValue(final int value) {
        for (final CommandType type : VALUES) {
            if (type.value == value) {
                return type;
            }
        }
        throw new IllegalArgumentException("Invalid command type value: " + value);
    }

    public static String toString(final Command command) {
        if (command.isApplication()) {
            switch (CommandType.byValue(command.type())) {
                case CreateAccount:
                    return CreateAccountCommand.toString(command.payload());
                case Deposit:
                    return DepositCommand.toString(command.payload());
                case Withdraw:
                    return WithdrawCommand.toString(command.payload());
                case Transfer:
                    return TransferCommand.toString(command.payload());
                default:
                    //fallthrough
            }
        }
        return "(unknown)";
    }

    private static CommandType[] VALUES = values();
}
