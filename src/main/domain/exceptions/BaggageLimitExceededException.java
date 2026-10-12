package domain.exceptions;

public class BaggageLimitExceededException extends BookingException {
    public BaggageLimitExceededException(double weight) {
        super("Baggage weight " + weight + " kg exceeds maximum allowable limit of 32.0 kg");
    }
}
