package unit.subsystem_b;

import domain.exceptions.BaggageLimitExceededException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import subsystem_b_ticket.BaggageRulesEngine;
import subsystem_b_ticket.Ticket;
import subsystem_b_ticket.TicketStatus;

import static org.junit.jupiter.api.Assertions.*;

public class TicketSubsystemTest {

    @Test
    @DisplayName("Subsystem B: Корректный переход состояний PENDING -> ISSUED -> VOIDED")
    void testTicketStateTransitions() {
        Ticket ticket = new Ticket("TKT-101", "RES-101", 20.0);
        assertEquals(TicketStatus.PENDING, ticket.getStatus());

        // Нельзя аннулировать билет в статусе PENDING
        assertThrows(IllegalStateException.class, ticket::voidTicket);

        ticket.issue();
        assertEquals(TicketStatus.ISSUED, ticket.getStatus());

        ticket.voidTicket();
        assertEquals(TicketStatus.VOIDED, ticket.getStatus());
    }

    @Test
    @DisplayName("Subsystem B: Проверка ограничений веса багажа")
    void testBaggageEngineLimits() {
        BaggageRulesEngine engine = new BaggageRulesEngine();

        assertDoesNotThrow(() -> engine.validateBaggageWeight(25.0));
        assertThrows(BaggageLimitExceededException.class, () -> engine.validateBaggageWeight(35.0));
        assertThrows(IllegalArgumentException.class, () -> engine.validateBaggageWeight(-5.0));
    }
}