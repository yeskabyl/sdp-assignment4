package facade;

import domain.ChainState;
import domain.exceptions.BaggageLimitExceededException;
import domain.exceptions.BookingException;
import domain.exceptions.MealUnavailableException;
import dsl.BookingScenarioBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import persistence.PostgresDatabaseManager;
import subsystem_a_seat.*;
import subsystem_b_ticket.*;
import subsystem_c_loyalty.*;

import static org.junit.jupiter.api.Assertions.*;

public class BookingOrchestratorPostgresTest {
    private static final String DB_NAME = System.getenv().getOrDefault("DB_NAME", "flight_reservation");

    private PostgresDatabaseManager db;
    private SeatReservationRepository seatRepo;
    private FareCalculator fareCalculator;
    private TicketRepository ticketRepo;
    private BaggageRulesEngine baggageEngine;
    private LoyaltyRepository loyaltyRepo;
    private MealPreferenceService mealService;
    private ChainJournalRepository journalRepo;
    private BookingOrchestratorFacade orchestrator;

    @BeforeEach
    void setUp() {
        db = new PostgresDatabaseManager(DB_NAME);
        db.cleanAllData(); // F.I.R.S.T.: очистка таблиц перед каждым тестом

        seatRepo = new SeatReservationRepository(db);
        fareCalculator = new FareCalculator();
        ticketRepo = new TicketRepository(db);
        baggageEngine = new BaggageRulesEngine();
        loyaltyRepo = new LoyaltyRepository(db);
        mealService = new MealPreferenceService();
        journalRepo = new ChainJournalRepository(db);

        orchestrator = new BookingOrchestratorFacade(
                seatRepo, fareCalculator, ticketRepo, baggageEngine, loyaltyRepo, mealService, journalRepo
        );
    }

    private BookingScenarioBuilder givenBooking(String bookingId) {
        return new BookingScenarioBuilder(orchestrator, bookingId);
    }

    @Test
    @DisplayName("Happy Path: Фиксация A -> B -> C в базе данных")
    void testHappyPath() {
        givenBooking("PG-001")
                .withBaggageWeight(20.0)
                .withMeal("VEGAN")
                .executesSuccessfully();

        SeatReservation seat = seatRepo.findById("RES-PG-001").orElseThrow();
        assertEquals(SeatReservationStatus.HELD, seat.getStatus());

        Ticket ticket = ticketRepo.findById("TKT-PG-001").orElseThrow();
        assertEquals(TicketStatus.ISSUED, ticket.getStatus());

        LoyaltyRecord loyalty = loyaltyRepo.findById("LOY-PG-001").orElseThrow();
        assertEquals(LoyaltyStatus.CONFIRMED, loyalty.getStatus());

        var journal = journalRepo.findByBookingId("PG-001").orElseThrow();
        assertEquals(ChainState.COMPLETED, journal.state());
    }

    @Test
    @DisplayName("Depth 1: Ошибка на шаге B -> откат только шага A (RELEASED)")
    void testCompensationDepthOne() {
        givenBooking("PG-002")
                .withBaggageWeight(45.0)
                .failsWith(BaggageLimitExceededException.class);

        SeatReservation seat = seatRepo.findById("RES-PG-002").orElseThrow();
        assertEquals(SeatReservationStatus.RELEASED, seat.getStatus());

        assertTrue(ticketRepo.findById("TKT-PG-002").isEmpty());

        var journal = journalRepo.findByBookingId("PG-002").orElseThrow();
        assertEquals(ChainState.COMPENSATED, journal.state());
    }

    @Test
    @DisplayName("Depth 2: Ошибка на шаге C -> двухшаговый откат: B (VOIDED), затем A (RELEASED)")
    void testCompensationDepthTwo() {
        givenBooking("PG-003")
                .withMeal("EXHAUSTED_KOSHER")
                .failsWith(MealUnavailableException.class);

        Ticket ticket = ticketRepo.findById("TKT-PG-003").orElseThrow();
        assertEquals(TicketStatus.VOIDED, ticket.getStatus());

        SeatReservation seat = seatRepo.findById("RES-PG-003").orElseThrow();
        assertEquals(SeatReservationStatus.RELEASED, seat.getStatus());

        var journal = journalRepo.findByBookingId("PG-003").orElseThrow();
        assertEquals(ChainState.COMPENSATED, journal.state());
    }

    @Test
    @DisplayName("Прерывание до шага A: пустой рейс прерывает цепочку без записи в БД")
    void testPreChainValidationFailure() {
        givenBooking("PG-004")
                .onFlight("   ")
                .failsWith(BookingException.class);

        assertTrue(seatRepo.findById("RES-PG-004").isEmpty());
        assertTrue(ticketRepo.findById("TKT-PG-004").isEmpty());
    }

    @Test
    @DisplayName("Persistence-Reload: Восстановление состояния после пересоздания репозиториев")
    void testPersistenceSurvivesRestart() {
        givenBooking("PG-RELOAD").executesSuccessfully();

        // Симуляция перезапуска приложения и считывания данных из PostgreSQL новым экземпляром
        PostgresDatabaseManager freshDb = new PostgresDatabaseManager(DB_NAME);
        SeatReservationRepository freshSeatRepo = new SeatReservationRepository(freshDb);
        TicketRepository freshTicketRepo = new TicketRepository(freshDb);

        assertEquals(SeatReservationStatus.HELD, freshSeatRepo.findById("RES-PG-RELOAD").orElseThrow().getStatus());
        assertEquals(TicketStatus.ISSUED, freshTicketRepo.findById("TKT-PG-RELOAD").orElseThrow().getStatus());
    }
}