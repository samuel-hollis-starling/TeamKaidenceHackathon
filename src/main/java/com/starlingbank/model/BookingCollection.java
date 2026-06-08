package com.starlingbank.model;

import java.util.List;

public class BookingCollection {
    private List<BookingRequest> bookings;
    private int totalCapacity;
    private int remaining;

    public BookingCollection() {}

    public BookingCollection(List<BookingRequest> bookings, int totalCapacity, int remaining) {
        this.bookings = bookings;
        this.totalCapacity = totalCapacity;
        this.remaining = remaining;
    }

    public List<BookingRequest> getBookings() { return bookings; }
    public int getTotalCapacity() { return totalCapacity; }
    public int getRemaining() { return remaining; }
}
