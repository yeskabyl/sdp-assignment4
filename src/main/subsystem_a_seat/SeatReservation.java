package main.subsystem_a_seat;

public class SeatReservation {
    private final String id;
    private final String flightNumber;
    private final String seatNumber;
    private final String passengerName;
    private final double fare;
    private SeatReservationStatus status;

    public SeatReservation(String id, String flightNumber, String seatNumber, String passengerName, double fare) {
        this(id, flightNumber, seatNumber, passengerName, fare, SeatReservationStatus.CREATED);
    }

    public SeatReservation(String id, String flightNumber, String seatNumber, String passengerName, double fare, SeatReservationStatus status) {
        this.id = id;
        this.flightNumber = flightNumber;
        this.seatNumber = seatNumber;
        this.passengerName = passengerName;
        this.fare = fare;
        this.status = status;
    }

    public void hold() {
        if (this.status != SeatReservationStatus.CREATED) {
            throw new IllegalStateException("Seat can only be held from CREATED status");
        }
        this.status = SeatReservationStatus.HELD;
    }

    public void release() {
        if (this.status != SeatReservationStatus.HELD) {
            throw new IllegalStateException("Only an actively HELD seat can be released");
        }
        this.status = SeatReservationStatus.RELEASED;
    }

    public String getId() { return id; }
    public String getFlightNumber() { return flightNumber; }
    public String getSeatNumber() { return seatNumber; }
    public String getPassengerName() { return passengerName; }
    public double getFare() { return fare; }
    public SeatReservationStatus getStatus() { return status; }
}
