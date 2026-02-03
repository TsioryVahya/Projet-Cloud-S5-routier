package com.cloud.identity.events;

import com.cloud.identity.entities.Signalement;
import org.springframework.context.ApplicationEvent;

public class SignalementSavedEvent extends ApplicationEvent {

    private final Signalement signalement;

    public SignalementSavedEvent(Object source, Signalement signalement) {
        super(source);
        this.signalement = signalement;
    }

    public Signalement getSignalement() {
        return signalement;
    }
}
