package de.unimarburg.diz.termmapper.configuration;

import org.apache.kafka.clients.admin.Admin;

@FunctionalInterface
public interface AdminClientProvider {

    Admin createClient();
}
