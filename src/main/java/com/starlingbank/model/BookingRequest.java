package com.starlingbank.model;

public class BookingRequest {
    private String employeeId;
    private SocialPreference socialPreference;
    private boolean feelingLucky;

    public BookingRequest() {}

    public BookingRequest(String employeeId, SocialPreference socialPreference, boolean feelingLucky) {
        this.employeeId = employeeId;
        this.socialPreference = socialPreference;
        this.feelingLucky = feelingLucky;
    }

    public String getEmployeeId() { return employeeId; }
    public SocialPreference getSocialPreference() { return socialPreference; }
    public boolean isFeelingLucky() { return feelingLucky; }
}
