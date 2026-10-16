package main.domain.exceptions;

public class MealUnavailableException extends BookingException {
    public MealUnavailableException(String meal) {
        super("Requested meal type '" + meal + "' is completely exhausted for this flight");
    }
}
