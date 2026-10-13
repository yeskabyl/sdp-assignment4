package subsystem_c_loyalty;

import domain.exceptions.MealUnavailableException;

public class MealPreferenceService {
    public void verifyMealAvailability(String mealType) throws MealUnavailableException {
        if ("EXHAUSTED_KOSHER".equalsIgnoreCase(mealType) || "OUT_OF_STOCK".equalsIgnoreCase(mealType)) {
            throw new MealUnavailableException(mealType);
        }
    }

    public int calculateLoyaltyMiles(int distanceMiles) {
        return (int) (distanceMiles * 1.5);
    }
}