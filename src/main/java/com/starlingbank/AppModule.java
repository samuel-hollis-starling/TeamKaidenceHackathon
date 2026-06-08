package com.starlingbank;

import com.google.inject.AbstractModule;
import com.starlingbank.service.AssignmentService;
import com.starlingbank.service.SimulatedAnnealingAssignmentService;
import com.starlingbank.service.BookingService;
import com.starlingbank.service.BookingServiceImpl;
import com.starlingbank.service.FloorMapService;
import com.starlingbank.service.FloorMapServiceImpl;
import com.starlingbank.service.OrgChartService;
import com.starlingbank.service.OrgChartServiceImpl;
import com.starlingbank.service.ScoringService;
import com.starlingbank.service.ScoringServiceImpl;

public class AppModule extends AbstractModule {
    @Override
    protected void configure() {
        bind(HelloService.class).to(HelloServiceImpl.class);
        bind(FloorMapService.class).to(FloorMapServiceImpl.class);
        bind(OrgChartService.class).to(OrgChartServiceImpl.class);
        bind(AssignmentService.class).to(SimulatedAnnealingAssignmentService.class);
        bind(ScoringService.class).to(ScoringServiceImpl.class);
        bind(BookingService.class).to(BookingServiceImpl.class);
    }
}