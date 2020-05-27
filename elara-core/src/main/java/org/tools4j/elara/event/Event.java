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
package org.tools4j.elara.event;

import org.agrona.DirectBuffer;
import org.agrona.MutableDirectBuffer;
import org.tools4j.elara.command.Command;
import org.tools4j.elara.flyweight.Writable;

public interface Event extends Writable {
    interface Id {
        Command.Id commandId();
        int index();
    }

    interface Flags {
        /** @return true if this is the last event of the command and all events are hereby committed */
        boolean isCommit();
        /** @return true if this is the last event of the command and all events are hereby rolled back */
        boolean isRollback();
        /** @return true if commit or rollback is true */
        boolean isFinal();
        /** @return true if neither commit nor rollback nor undefined */
        boolean isNonFinal();
        /** @return flags as raw bits value */
        byte value();
    }

    Id id();

    int type();

    long time();

    Flags flags();

    default boolean isAdmin() {
        return EventType.isAdmin(type());
    }

    default boolean isApplication() {
        return EventType.isApplication(type());
    }

    DirectBuffer payload();

    @Override
    int writeTo(MutableDirectBuffer dst, int offset);
}
