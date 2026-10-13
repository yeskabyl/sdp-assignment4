package dsl;

import domain.BookingRequest;
import facade.BookingOrchestratorFacade;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class BookingScenarioBuilder {
    private final BookingOrchestratorFacade orchestrator;
    private final String bookingId;
    private String flight = "KC-951";
    private String seat = "12A";
    private String passenger = "Merey";
    private boolean isPreferred = true;
    private double baggage = 20.0;
    private String meal = "STANDARD";
    private int distance = 1200;

    public BookingScenarioBuilder(BookingOrchestratorFacade orchestrator, String bookingId) {
        this.orchestrator = orchestrator;
        this.bookingId = bookingId;
    }

    public BookingScenarioBuilder onFlight(String flight) {
        this.flight = flight;
        return this;
    }

    public BookingScenarioBuilder withSeat(String seat) {
        this.seat = seat;
        return this;
    }

    public BookingScenarioBuilder withBaggageWeight(double weight) {
        this.baggage = weight;
        return this;
    }

    public BookingScenarioBuilder withMeal(String meal) {
        this.meal = meal;
        return this;
    }

    public void executesSuccessfully() {
        BookingRequest req = new BookingRequest(
                bookingId, flight, seat, passenger, isPreferred, baggage, meal, distance
        );
        assertDoesNotThrow(() -> orchestrator.processBooking(req));
    }

    public void failsWith(Class<? extends Throwable> expectedEx) {
        BookingRequest req = new BookingRequest(
                bookingId, flight, seat, passenger, isPreferred, baggage, meal, distance
        );
        assertThrows(expectedEx, () -> orchestrator.processBooking(req));
    }
}