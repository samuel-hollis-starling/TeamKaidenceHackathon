package com.starlingbank;

import com.starlingbank.model.HelloResponse;
import jakarta.inject.Inject;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;

@Path("/hello")
public class HelloResource {
    private final HelloService helloService;

    @Inject
    public HelloResource(HelloService helloService) {
        this.helloService = helloService;
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public HelloResponse greet(@QueryParam("name") @DefaultValue("World") String name) {
        return helloService.greet(name);
    }
}