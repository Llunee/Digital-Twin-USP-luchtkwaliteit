package nl.hu.luchtkwaliteit.setup;

import nl.hu.luchtkwaliteit.GeoJSONGenerator;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.annotation.WebListener;
import java.util.Timer;
import java.util.TimerTask;

@WebListener
public class MyServletContextListener implements ServletContextListener {
    @Override
    public void contextInitialized(ServletContextEvent sce) {
        this.hourlyTask();
    }

    public void hourlyTask() {
        GeoJSONGenerator generator = new GeoJSONGenerator();

        Timer timer = new Timer();
        TimerTask hourlyTask = new TimerTask() {
            @Override
            public void run() {
                generator.generateGeoJSONFile();
            }
        };

        // schedule the task to run starting now and then every hour...
        timer.schedule(hourlyTask, 0L, 1000 * 60 * 60);
    }
}
