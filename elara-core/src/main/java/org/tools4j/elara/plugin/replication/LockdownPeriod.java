package org.tools4j.elara.plugin.replication;

import org.tools4j.elara.command.Command;

enum LockdownPeriod {
    ;
    public static boolean isCommandExpired(final long time,
                                           final Command command,
                                           final Configuration configuration) {
        return isCommandExpired(time, command.time(), configuration);
    }

    public static boolean isCommandExpired(final long time,
                                           final long commandTime,
                                           final Configuration configuration) {
        return time - commandTime > configuration.leaderLockdownPeriodAfterChange();
    }

    public static boolean isLockdownPeriod(final long time,
                                           final ReplicationState replicationState,
                                           final Configuration configuration) {
        return time - replicationState.leaderElectionTime() <= configuration.leaderLockdownPeriodAfterChange();
    }

}
