package unit.subsystem_a;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import subsystem_a_seat.FareCalculator;
import subsystem_a_seat.SeatReservation;
import subsystem_a_seat.SeatReservationStatus;

import static org.junit.jupiter.api.Assertions.*;

public class SeatReservationTest {

    @Test
    @DisplayName("Subsystem A: Корректный переход состояний CREATED -> HELD -> RELEASED")
    void testStateTransitions() {
        SeatReservation res = new SeatReservation("RES-101", "KC-951", "12A", "Merey", 155.0);
        assertEquals(SeatReservationStatus.CREATED, res.getStatus());

        // Переход CREATED -> RELEASED недопустим
        assertThrows(IllegalStateException.class, res::release);

        res.hold();
        assertEquals(SeatReservationStatus.HELD, res.getStatus());

        // Повторный hold из HELD недопустим
        assertThrows(IllegalStateException.class, res::hold);

        res.release();
        assertEquals(SeatReservationStatus.RELEASED, res.getStatus());

        // Повторный release недопустим
        assertThrows(IllegalStateException.class, res::release);
    }

    @Test
    @DisplayName("Subsystem A: Расчет тарифа в FareCalculator")
    void testFareCalculator() {
        FareCalculator calc = new FareCalculator();
        assertEquals(120.0, calc.calculateFare(false));
        assertEquals(155.0, calc.calculateFare(true));
    }
}