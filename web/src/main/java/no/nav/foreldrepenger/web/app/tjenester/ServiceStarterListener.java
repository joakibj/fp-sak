package no.nav.foreldrepenger.web.app.tjenester;

import javax.inject.Inject;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.annotation.WebListener;

@WebListener
public class ServiceStarterListener implements ServletContextListener {

    @Inject
    private ApplicationServiceStarter applicationServiceStarter;  // vil ikke fungere med constructor innjection

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        applicationServiceStarter.startServices();
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        applicationServiceStarter.stopServices();
    }

}