package nl.hu.luchtkwaliteit.setup;

import nl.hu.luchtkwaliteit.GeoJSONGenerator;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.annotation.WebListener;

@WebListener
public class MyServletContextListener implements ServletContextListener {
    @Override
    public void contextInitialized(ServletContextEvent sce) {
        GeoJSONGenerator generator = new GeoJSONGenerator();
        generator.generateGeoJSONFile();
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
    }
}
