package org.tools4j.elara.plugin.base;

import org.tools4j.elara.app.config.AppConfig;
import org.tools4j.elara.app.config.ApplierConfig;
import org.tools4j.elara.app.type.PassthroughAppConfig;
import org.tools4j.elara.event.Event;

public interface MutableBaseState extends BaseState {
    default MutableBaseState applyEvent(final Event event) {
        return applyEvent(event.sourceId(), event.sourceSequence(), event.eventSequence(), event.eventIndex());
    }

    MutableBaseState applyEvent(int sourceId, long sourceSeq, long eventSeq, int index);

    static MutableBaseState createDefault(final AppConfig config) {
        if (config instanceof PassthroughAppConfig && !(config instanceof ApplierConfig)) {
            return new SingleEventBaseState();
        }
        return new DefaultBaseState();
    }
}
