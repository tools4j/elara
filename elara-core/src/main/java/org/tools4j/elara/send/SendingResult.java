package org.tools4j.elara.send;

public interface SendingResult {
    Status status();
    boolean isSent();
    boolean isPending();
    boolean isUnsuccessful();
    Exception exception();
    boolean trySendingAgain();
    SendingResult sendAgainAfter(long time);

    enum Status {
        PENDING,
        SENT,
        DISCONNECTED,
        BACK_PRESSURED,
        FAILED
    }

    interface Default extends SendingResult {
        @Override
        default boolean isSent() {
            return status() == Status.SENT;
        }

        @Override
        default boolean isPending() {
            return status() == Status.PENDING;
        }

        @Override
        default boolean isUnsuccessful() {
            final Status status = status();
            return status != Status.SENT && status != Status.PENDING;
        }
    }
}
