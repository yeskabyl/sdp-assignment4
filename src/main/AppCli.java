package main;

import main.domain.BookingRequest;
import main.facade.*;
import main.persistence.DatabaseManager;
import main.subsystem_a_seat.*;
import main.subsystem_b_ticket.*;
import main.subsystem_c_loyalty.*;

import java.util.Scanner;
import java.util.UUID;

public class AppCli {
    public static void main(String[] args) {
        DatabaseManager db = new DatabaseManager("flight_booking_demo.db");
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
        System.out.println("=== Flight Reservation Facade CLI Started ===");

        while (true) {
            System.out.println("\nSelect an action:");
            System.out.println("1. Happy Path: Complete Successful Booking");
            System.out.println("2. Force Step B Failure: Heavy Baggage (Over 32kg) -> Unwind A");
            System.out.println("3. Force Step C Failure: Unavailable Meal -> Unwind B, then A");
            System.out.println("4. Inspect Booking by ID (Check Persisted State on Disk)");
            System.out.println("5. Reset Database to Clean State");
            System.out.println("0. Exit");
            System.out.print("> ");

            String choice = scanner.nextLine().trim();
            if ("0".equals(choice)) break;

            try {
                switch (choice) {
                    case "1" -> {
                        String id = UUID.randomUUID().toString().substring(0, 8);
                        BookingRequest req = new BookingRequest(id, "KC-951", "12A", "Merey", true, 20.0, "STANDARD", 1200);
                        orchestrator.processBooking(req);
                        System.out.println("[SUCCESS] Booking completed! Booking ID: " + id);
                    }
                    case "2" -> {
                        String id = UUID.randomUUID().toString().substring(0, 8);
                        System.out.println("[DEMO] Attempting booking with 50kg luggage...");
                        BookingRequest req = new BookingRequest(id, "KC-951", "14B", "Merey", false, 50.0, "STANDARD", 1200);
                        try {
                            orchestrator.processBooking(req);
                        } catch (Exception ex) {
                            System.out.println("[HANDLED EXCEPTION] " + ex.getMessage());
                            System.out.println("[INFO] Rollback triggered! Check persisted record for ID: " + id);
                        }
                    }
                    case "3" -> {
                        String id = UUID.randomUUID().toString().substring(0, 8);
                        System.out.println("[DEMO] Attempting booking with EXHAUSTED_KOSHER meal...");
                        BookingRequest req = new BookingRequest(id, "KC-951", "1A", "Merey", true, 18.0, "EXHAUSTED_KOSHER", 1200);
                        try {
                            orchestrator.processBooking(req);
                        } catch (Exception ex) {
                            System.out.println("[HANDLED EXCEPTION] " + ex.getMessage());
                            System.out.println("[INFO] Two-step rollback executed (B then A)! Inspect ID: " + id);
                        }
                    }
                    case "4" -> {
                        System.out.print("Enter booking ID: ");
                        String bId = scanner.nextLine().trim();
                        System.out.println(adminFacade.inspectBooking(bId));
                    }
                    case "5" -> {
                        db.cleanAllData();
                        System.out.println("[CLEAN] Database has been wiped clean.");
                    }
                    default -> System.out.println("Invalid option.");
                }
            } catch (Exception e) {
                System.out.println("[ERROR] " + e.getMessage());
            }
        }
    }
}