package main.subsystem_a_seat;

public class FareCalculator {
    private static final double BASE_FARE = 120.0;
    private static final double EXTRA_SEAT_FEE = 35.0;

    public double calculateFare(boolean preferredSeat) {
        return preferredSeat ? (BASE_FARE + EXTRA_SEAT_FEE) : BASE_FARE;
    }
}
