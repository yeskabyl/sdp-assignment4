package unit.subsystem_c;

import domain.exceptions.MealUnavailableException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import subsystem_c_loyalty.LoyaltyRecord;
import subsystem_c_loyalty.LoyaltyStatus;
import subsystem_c_loyalty.MealPreferenceService;

import static org.junit.jupiter.api.Assertions.*;

public class LoyaltySubsystemTest {

    @Test
    @DisplayName("Subsystem C: Переход статусов INITIAL -> CONFIRMED -> REVOKED")
    void testLoyaltyStateTransitions() {
        LoyaltyRecord record = new LoyaltyRecord("LOY-101", "TKT-101", "VEGAN", 1800);
        assertEquals(LoyaltyStatus.INITIAL, record.getStatus());

        // Нельзя отозвать запись до подтверждения
        assertThrows(IllegalStateException.class, record::revoke);

        record.confirm();
        assertEquals(LoyaltyStatus.CONFIRMED, record.getStatus());

        record.revoke();
        assertEquals(LoyaltyStatus.REVOKED, record.getStatus());
    }

    @Test
    @DisplayName("Subsystem C: Проверка доступности питания и расчет миль")
    void testMealService() {
        MealPreferenceService service = new MealPreferenceService();

        assertDoesNotThrow(() -> service.verifyMealAvailability("STANDARD"));
        assertThrows(MealUnavailableException.class, () -> service.verifyMealAvailability("EXHAUSTED_KOSHER"));
        assertThrows(MealUnavailableException.class, () -> service.verifyMealAvailability("OUT_OF_STOCK"));

        assertEquals(1500, service.calculateLoyaltyMiles(1000));
    }
}