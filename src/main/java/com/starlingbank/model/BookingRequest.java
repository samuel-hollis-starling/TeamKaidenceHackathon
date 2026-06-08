package com.starlingbank.model;

public class BookingRequest {
    private String employeeId;
    private SocialPreference socialPreference;
    private boolean windowSeat;
    private boolean feelingLucky;

    public BookingRequest() {}

    public BookingRequest(String employeeId, SocialPreference socialPreference, boolean windowSeat, boolean feelingLucky) {
        this.employeeId = employeeId;
        this.socialPreference = socialPreference;
        this.windowSeat = windowSeat;
        this.feelingLucky = feelingLucky;
    }

    public String getEmployeeId() { return employeeId; }
    public SocialPreference getSocialPreference() { return socialPreference; }
    public boolean isWindowSeat() { return windowSeat; }
    public boolean isFeelingLucky() { return feelingLucky; }
}
