package com.starlingbank;

import com.google.inject.AbstractModule;

public class AppModule extends AbstractModule {
    @Override
    protected void configure() {
        bind(HelloService.class).to(HelloServiceImpl.class);
    }
}