package nl.hu.luchtkwaliteit.setup;

import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.server.filter.RolesAllowedDynamicFeature;

import javax.ws.rs.ApplicationPath;

@ApplicationPath("restservices")
public class JerseyConfig extends ResourceConfig {
    public JerseyConfig() {
        packages("nl.hu.luchtkwaliteit", "nl.hu.luchtkwaliteit.application");
        register(RolesAllowedDynamicFeature.class);
    }
}
