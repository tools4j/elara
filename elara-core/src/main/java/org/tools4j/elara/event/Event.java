/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2020-2023 tools4j.org (Marco Terzer, Anton Anufriev)
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
import org.tools4j.elara.flyweight.PayloadType;
import org.tools4j.elara.flyweight.Writable;
import org.tools4j.elara.logging.Printable;

public interface Event extends Writable, Printable {
    int sourceId();
    long sourceSequence();
    long eventSequence();
    int index();

    int payloadType();

    long eventTime();

    Flags flags();

    default boolean isSystem() {
        return PayloadType.isSystem(payloadType());
    }

    default boolean isApplication() {
        return PayloadType.isApplication(payloadType());
    }

    DirectBuffer payload();

    @Override
    int writeTo(MutableDirectBuffer dst, int offset);

    interface Flags {
        /** Constant for no flags */
        char NONE = '0';
        /** Constant for application commit flag indicating last event for the command */
        char COMMIT = 'C';
        /** Constant for nil (aka auto-commit) flag, same function as {@link #COMMIT} but set by system */
        char NIL ='N';
        /** Constant for rollback flag indicating that all events of the last command should be ignored */
        char ROLLBACK = 'R';

        /** @return the raw flags value */
        char value();

        /** @return true if this is the last event of the command and all events are hereby committed */
        default boolean isCommit() {return isCommit(value());}
        /** @return true if this is the last event for a command, and it was explicitly routed by the application */
        default boolean isAppCommit() {return isAppCommit(value());}
        /** @return true if this is an implicitly added commit event, the application did not route any events */
        default boolean isAutoCommit() {return isAutoCommit(value());}
        /** @return true if this is the last event of the command and all events are hereby rolled back */
        default boolean isRollback() {return isRollback(value());}
        /** @return true if commit or rollback is true */
        default boolean isLast() {return isLast(value());}

        /** @return true if value is {@link #COMMIT} or {@link #NIL} */
        static boolean isCommit(final char value) {
            return value == COMMIT || value == NIL;
        }
        /** @return true if value is {@link #COMMIT} */
        static boolean isAppCommit(final char value) {
            return value == COMMIT;
        }
        /** @return true if value is {@link #NIL} */
        static boolean isAutoCommit(final char value) {
            return value == NIL;
        }
        /** @return true if value is {@link #ROLLBACK} */
        static boolean isRollback(final char value) {
            return value == ROLLBACK;
        }
        /** @return true if value is{@link #COMMIT}, {@link #NIL} or {@link #ROLLBACK} */
        static boolean isLast(final char value) {
            return value == COMMIT || value == NIL || value == ROLLBACK;
        }
    }
}
