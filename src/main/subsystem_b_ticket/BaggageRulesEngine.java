package subsystem_b_ticket;

import domain.exceptions.BaggageLimitExceededException;

public class BaggageRulesEngine {
    public static final double MAX_ALLOWED_WEIGHT = 32.0;

    public void validateBaggageWeight(double weight) throws BaggageLimitExceededException {
        if (weight < 0) {
            throw new IllegalArgumentException("Weight cannot be negative");
        }
        if (weight > MAX_ALLOWED_WEIGHT) {
            throw new BaggageLimitExceededException(weight);
        }
    }
}