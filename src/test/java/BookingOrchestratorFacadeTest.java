import domain.BookingRequest;
import domain.ChainState;
import domain.exceptions.BaggageLimitExceededException;
import domain.exceptions.MealUnavailableException;
import facade.BookingOrchestratorFacade;
import facade.ChainJournalRepository;
import persistence.DatabaseManager;
import subsystem_a_seat.*;
import subsystem_b_ticket.*;
import subsystem_c_loyalty.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.File;

import static org.junit.jupiter.api.Assertions.*;

public class BookingOrchestratorFacadeTest {
    private static final String TEST_DB_PATH = "target/test_flights.db";

    private SeatReservationRepository seatRepo;
    private TicketRepository ticketRepo;
    private LoyaltyRepository loyaltyRepo;
    private ChainJournalRepository journalRepo;
    private BookingOrchestratorFacade facade;

    @BeforeEach
    void setUp() {
        new File("target").mkdirs();
        DatabaseManager db = new DatabaseManager(TEST_DB_PATH);
        db.cleanAllData();

        seatRepo = new SeatReservationRepository(db);
        FareCalculator fareCalculator = new FareCalculator();
        ticketRepo = new TicketRepository(db);
        BaggageRulesEngine baggageEngine = new BaggageRulesEngine();
        loyaltyRepo = new LoyaltyRepository(db);
        MealPreferenceService mealService = new MealPreferenceService();
        journalRepo = new ChainJournalRepository(db);

        facade = new BookingOrchestratorFacade(
                seatRepo, fareCalculator, ticketRepo, baggageEngine, loyaltyRepo, mealService, journalRepo
        );
    }

    private BookingScenarioBuilder givenBooking(String bookingId) {
        return new BookingScenarioBuilder(bookingId);
    }

    private class BookingScenarioBuilder {
        private final String bookingId;
        private final String flight = "KC-951";
        private final String seat = "10A";
        private final String passenger = "John Doe";
        private final boolean preferredSeat = false;
        private double luggageWeight = 20.0;
        private String meal = "STANDARD";
        private final int distance = 1000;

        public BookingScenarioBuilder(String id) { this.bookingId = id; }
        public BookingScenarioBuilder withBaggageWeight(double weight) { this.luggageWeight = weight; return this; }
        public BookingScenarioBuilder withMeal(String mealChoice) { this.meal = mealChoice; return this; }

        public void executeSuccessfully() {
            BookingRequest req = new BookingRequest(bookingId, flight, seat, passenger, preferredSeat, luggageWeight, meal, distance);
            assertDoesNotThrow(() -> facade.processBooking(req));
        }

        public void executeExpectingFailure(Class<? extends Throwable> expectedEx) {
            BookingRequest req = new BookingRequest(bookingId, flight, seat, passenger, preferredSeat, luggageWeight, meal, distance);
            assertThrows(expectedEx, () -> facade.processBooking(req));
        }
    }

    @Test
    @DisplayName("Happy Path: Full chain executes and persists all entities as active")
    void testHappyPathPersistence() {
        givenBooking("BKG-100")
                .withBaggageWeight(23.0)
                .withMeal("VEGAN")
                .executeSuccessfully();

        SeatReservation seat = seatRepo.findById("RES-BKG-100").orElseThrow();
        assertEquals(SeatReservationStatus.HELD, seat.getStatus());

        Ticket ticket = ticketRepo.findById("TKT-BKG-100").orElseThrow();
        assertEquals(TicketStatus.ISSUED, ticket.getStatus());

        LoyaltyRecord loyalty = loyaltyRepo.findById("LOY-BKG-100").orElseThrow();
        assertEquals(LoyaltyStatus.CONFIRMED, loyalty.getStatus());

        ChainJournalRepository.JournalEntry journal = journalRepo.findByBookingId("BKG-100").orElseThrow();
        assertEquals(ChainState.COMPLETED, journal.state());
    }

    @Test
    @DisplayName("Depth 1 Compensation: B fails (baggage exceeded) -> Unwinds A alone")
    void testFailureAtStepBCompensatesStepA() {
        givenBooking("BKG-200")
                .withBaggageWeight(55.0) // Лимит 32 кг
                .executeExpectingFailure(BaggageLimitExceededException.class);

        // Subsystem A должно быть сохранено и переведено в статус RELEASED
        SeatReservation seat = seatRepo.findById("RES-BKG-200").orElseThrow();
        assertEquals(SeatReservationStatus.RELEASED, seat.getStatus());

        // Subsystem B не должно иметь зафиксированного билета
        assertTrue(ticketRepo.findById("TKT-BKG-200").isEmpty());

        // Статус в журнале оркестратора
        ChainJournalRepository.JournalEntry journal = journalRepo.findByBookingId("BKG-200").orElseThrow();
        assertEquals(ChainState.COMPENSATED, journal.state());
    }

    @Test
    @DisplayName("Depth 2 Compensation: C fails (meal unavailable) -> Unwinds B, then A")
    void testFailureAtStepCCompensatesStepBandStepA() {
        givenBooking("BKG-300")
                .withMeal("EXHAUSTED_KOSHER")
                .executeExpectingFailure(MealUnavailableException.class);

        // Subsystem B было выпущено, но откачено в VOIDED
        Ticket ticket = ticketRepo.findById("TKT-BKG-300").orElseThrow();
        assertEquals(TicketStatus.VOIDED, ticket.getStatus());

        // Subsystem A откачено в RELEASED
        SeatReservation seat = seatRepo.findById("RES-BKG-300").orElseThrow();
        assertEquals(SeatReservationStatus.RELEASED, seat.getStatus());

        // Журнал зафиксировал полный откат
        ChainJournalRepository.JournalEntry journal = journalRepo.findByBookingId("BKG-300").orElseThrow();
        assertEquals(ChainState.COMPENSATED, journal.state());
    }

    @Test
    @DisplayName("Persistence-Reload: System survives restart and verifies persisted state on disk")
    void testPersistenceSurvivesRestart() {
        givenBooking("BKG-400").executeSuccessfully();

        // Симулируем перезапуск приложения: создаем новый DatabaseManager и новые репозитории поверх того же файла
        DatabaseManager restartedDb = new DatabaseManager(TEST_DB_PATH);
        SeatReservationRepository reloadedSeatRepo = new SeatReservationRepository(restartedDb);
        TicketRepository reloadedTicketRepo = new TicketRepository(restartedDb);
        ChainJournalRepository reloadedJournalRepo = new ChainJournalRepository(restartedDb);

        assertTrue(reloadedSeatRepo.findById("RES-BKG-400").isPresent());
        assertEquals(SeatReservationStatus.HELD, reloadedSeatRepo.findById("RES-BKG-400").get().getStatus());
        assertEquals(TicketStatus.ISSUED, reloadedTicketRepo.findById("TKT-BKG-400").get().getStatus());
        assertEquals(ChainState.COMPLETED, reloadedJournalRepo.findByBookingId("BKG-400").get().state());
    }
}