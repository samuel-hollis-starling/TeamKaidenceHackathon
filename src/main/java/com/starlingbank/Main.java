package com.starlingbank;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.starlingbank.service.AssignmentService;
import com.starlingbank.service.AssignmentState;
import com.starlingbank.service.BookingService;
import com.starlingbank.service.FloorMapService;
import com.starlingbank.service.OrgChartService;
import com.starlingbank.service.ScoringService;
import org.glassfish.grizzly.http.server.HttpServer;
import org.glassfish.hk2.utilities.binding.AbstractBinder;
import org.glassfish.jersey.grizzly2.httpserver.GrizzlyHttpServerFactory;
import org.glassfish.jersey.jackson.JacksonFeature;
import org.glassfish.jersey.server.ResourceConfig;

import java.net.URI;

public class Main {
    public static void main(String[] args) throws Exception {
        Injector injector = Guice.createInjector(new AppModule());

        ResourceConfig config = new ResourceConfig();
        config.register(HelloResource.class);
        config.register(JacksonFeature.class);
        config.register(com.starlingbank.api.CorsFilter.class);
        config.register(com.starlingbank.api.DeskResource.class);
        config.register(com.starlingbank.api.EmployeeResource.class);
        config.register(com.starlingbank.api.BookingResource.class);
        config.register(com.starlingbank.api.AssignmentResource.class);
        config.register(com.starlingbank.api.OrgNodeResource.class);
        config.register(com.starlingbank.api.FloorMapResource.class);
        // Bridge Guice-managed services into HK2 (Jersey's DI)
        config.register(new AbstractBinder() {
            @Override
            protected void configure() {
                bind(injector.getInstance(HelloService.class)).to(HelloService.class);
                bind(injector.getInstance(FloorMapService.class)).to(FloorMapService.class);
                bind(injector.getInstance(OrgChartService.class)).to(OrgChartService.class);
                bind(injector.getInstance(BookingService.class)).to(BookingService.class);
                bind(injector.getInstance(AssignmentService.class)).to(AssignmentService.class);
                bind(injector.getInstance(AssignmentState.class)).to(AssignmentState.class);
                bind(injector.getInstance(ScoringService.class)).to(ScoringService.class);
            }
        });

        HttpServer server = GrizzlyHttpServerFactory.createHttpServer(
            URI.create("http://0.0.0.0:8080/"),
            config
        );

        System.out.println("Server running at http://localhost:8080/hello");
        System.out.println("Try: curl http://localhost:8080/hello?name=Starling");
        System.out.println("Press Enter to stop...");
        System.in.read();
    }
}