import domain.BookingRequest;
import facade.*;
import persistence.PostgresDatabaseManager;
import subsystem_a_seat.*;
import subsystem_b_ticket.*;
import subsystem_c_loyalty.*;

import java.util.Scanner;
import java.util.UUID;

public class Main {
    public static void main(String[] args) {
        String dbName = System.getenv().getOrDefault("DB_NAME", "flight_reservation");
        String dbUser = System.getenv().getOrDefault("DB_USER", "postgres");
        String dbPassword = System.getenv().getOrDefault("DB_PASS", "postgres");

        PostgresDatabaseManager db = new PostgresDatabaseManager(dbName, dbUser, dbPassword);

        SeatReservationRepository seatRepo = new SeatReservationRepository(db);
        FareCalculator fareCalculator = new FareCalculator();

        TicketRepository ticketRepo = new TicketRepository(db);
        BaggageRulesEngine baggageEngine = new BaggageRulesEngine();

        LoyaltyRepository loyaltyRepo = new LoyaltyRepository(db);
        MealPreferenceService mealService = new MealPreferenceService();

        ChainJournalRepository journalRepo = new ChainJournalRepository(db);

        BookingOrchestratorFacade orchestrator = new BookingOrchestratorFacade(
                seatRepo, fareCalculator, ticketRepo, baggageEngine, loyaltyRepo, mealService, journalRepo
        );
        AdminBookingFacade adminFacade = new AdminBookingFacade(journalRepo, seatRepo, ticketRepo, loyaltyRepo);

        Scanner scanner = new Scanner(System.in);
        System.out.println("============================================================");

        while (true) {
            System.out.println("""
                
                --- Select an Action ---
                1. Happy Path: Execute Full Chain (A -> B -> C)
                2. Force Step B Failure: Heavy Baggage (> 32kg) -> Unwind A
                3. Force Step C Failure: Meal Unavailable -> Unwind B, then A
                4. Inspect Booking by ID (Check Persisted Database State)
                5. Reset Database to Clean State (TRUNCATE)
                0. Exit
                """);
            System.out.print("> ");

            String choice = scanner.nextLine().trim();
            if ("0".equals(choice)) {
                System.out.println("Exiting application. Goodbye!");
                break;
            }

            try {
                switch (choice) {
                    case "1" -> {
                        String id = "BKG-" + UUID.randomUUID().toString().substring(0, 6);
                        BookingRequest req = new BookingRequest(id, "KC-951", "12A", "Merey", true, 20.0, "STANDARD", 1200);
                        orchestrator.processBooking(req);
                        System.out.println("\n[SUCCESS] Booking completed across all subsystems! Booking ID: " + id);
                    }
                    case "2" -> {
                        String id = "BKG-" + UUID.randomUUID().toString().substring(0, 6);
                        System.out.println("\n[DEMO] Attempting booking with 55.0kg baggage (limit: 32.0kg)...");
                        BookingRequest req = new BookingRequest(id, "KC-951", "14B", "Merey", false, 55.0, "STANDARD", 1200);
                        try {
                            orchestrator.processBooking(req);
                        } catch (Exception ex) {
                            System.out.println("[HANDLED EXCEPTION]: " + ex.getMessage());
                            System.out.println("[ROLLBACK COMPLETED]: Seat reservation released (RELEASED). Ticket was never issued.");
                            System.out.println("Inspect persisted state using ID: " + id);
                        }
                    }
                    case "3" -> {
                        String id = "BKG-" + UUID.randomUUID().toString().substring(0, 6);
                        System.out.println("\n[DEMO] Attempting booking with EXHAUSTED_KOSHER meal option...");
                        BookingRequest req = new BookingRequest(id, "KC-951", "1A", "Merey", true, 18.0, "EXHAUSTED_KOSHER", 1200);
                        try {
                            orchestrator.processBooking(req);
                        } catch (Exception ex) {
                            System.out.println("[HANDLED EXCEPTION]: " + ex.getMessage());
                            System.out.println("[TWO-STEP ROLLBACK COMPLETED]: Ticket voided (VOIDED) -> Seat released (RELEASED)!");
                            System.out.println("Inspect persisted state using ID: " + id);
                        }
                    }
                    case "4" -> {
                        System.out.print("\nEnter Booking ID (e.g., BKG-xxxxxx): ");
                        String bId = scanner.nextLine().trim();
                        System.out.println(adminFacade.inspectBooking(bId));
                    }
                    case "5" -> {
                        db.cleanAllData();
                        System.out.println("\n[CLEAN] All database tables truncated successfully.");
                    }
                    default -> System.out.println("Invalid selection. Please choose an option between 0 and 5.");
                }
            } catch (Exception e) {
                System.out.println("[SYSTEM ERROR] " + e.getMessage());
            }
        }
    }
}