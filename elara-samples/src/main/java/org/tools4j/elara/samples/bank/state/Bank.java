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

import org.agrona.collections.Object2ObjectHashMap;
import org.tools4j.elara.samples.bank.command.CreateAccountCommand;
import org.tools4j.elara.samples.bank.flyweight.AsciiString;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public interface Bank {
    /** @return all accounts, actually a set but for GC free iteration returns as a list */
    List<CharSequence> accounts();
    boolean hasAccount(CharSequence name);
    BankAccount account(CharSequence name);

    void createAccount(CreateAccountCommand command, BankEventRouter router);

    interface Mutable extends Bank {
        @Override
        BankAccount.Mutable account(CharSequence name);
        BankAccount.Mutable openAccount(CharSequence name);
        BankAccount.Mutable closeAccount(CharSequence name);
    }

    class Default implements Mutable {
        private final List<AsciiString> accounts = new ArrayList<>();
        private final List<CharSequence> unmodifiableAccounts = Collections.unmodifiableList(accounts);
        private final Map<AsciiString, BankAccount.Mutable> accountByName = new Object2ObjectHashMap<>();

        private final AsciiString tempString = new AsciiString();

        private AsciiString toAsciiString(final CharSequence value) {
            return value instanceof AsciiString ? (AsciiString)value : tempString.set(value);
        }

        @Override
        public List<CharSequence> accounts() {
            return unmodifiableAccounts;
        }

        @Override
        public boolean hasAccount(final CharSequence name) {
            return accountByName.containsKey(toAsciiString(name));
        }

        @Override
        public BankAccount.Mutable account(final CharSequence name) {
            final BankAccount.Mutable account = accountByName.get(toAsciiString(name));
            if (account != null) {
                return account;
            }
            throw new IllegalArgumentException("No such account: " + name);
        }

        @Override
        public BankAccount.Mutable openAccount(final CharSequence name) {
            if (accountByName.containsKey(toAsciiString(name))) {
                throw new IllegalArgumentException("Account already exists: " + name);
            }
            final AsciiString asciiName = new AsciiString(name);
            final BankAccount.Mutable account = new BankAccount.Default(this, name.toString());
            accountByName.put(asciiName, account);
            accounts.add(asciiName);
            return account;
        }

        @Override
        public BankAccount.Mutable closeAccount(final CharSequence name) {
            final BankAccount.Mutable account = accountByName.remove(toAsciiString(name));
            if (account != null) {
                return account;
            }
            throw new IllegalArgumentException("No such account: " + name);
        }

        @Override
        public void createAccount(final CreateAccountCommand command, final BankEventRouter router) {
            final CharSequence account = command.name();
            if (hasAccount(account)) {
                router.routeAccountCreationRejectedEvent(account, "Account cannot be created since another account with the same name already exists");
                return;
            }
            router.routeAccountCreatedEvent(account);
        }
    }
}
