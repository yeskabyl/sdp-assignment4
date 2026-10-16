package main.domain;

public record BookingRequest(
        String bookingId,
        String flightNumber,
        String seatNumber,
        String passengerName,
        boolean isWindowOrAisle,
        double baggageWeightKg,
        String mealPreference,
        int distanceMiles
) {}
