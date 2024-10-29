package de.unimarburg.diz.termmapper;

import org.springframework.context.ApplicationEvent;

public class UpdateCompleted extends ApplicationEvent {

    public UpdateCompleted(Object source) {
        super(source);
    }
}
