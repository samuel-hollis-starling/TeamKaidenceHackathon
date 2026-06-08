package com.starlingbank;

import com.google.inject.AbstractModule;
import com.starlingbank.service.FloorMapService;
import com.starlingbank.service.FloorMapServiceImpl;

public class AppModule extends AbstractModule {
    @Override
    protected void configure() {
        bind(HelloService.class).to(HelloServiceImpl.class);
        bind(FloorMapService.class).to(FloorMapServiceImpl.class);
    }
}