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
        System.out.println("=== Flight Reservation System (PostgreSQL) Live Demo ===");

        while (true) {
            System.out.println("\n--- Выберите действие ---");
            System.out.println("1. Успешный заказ (Happy Path A -> B -> C)");
            System.out.println("2. Спровоцировать сбой в B (Багаж > 32 кг) -> Откат A");
            System.out.println("3. Спровоцировать сбой в C (Недоступное питание) -> Откат B, затем A");
            System.out.println("4. Инспекция состояния брони в PostgreSQL");
            System.out.println("5. Сбросить (Truncate) таблицы базы данных");
            System.out.println("0. Выход");
            System.out.print("> ");

            String choice = scanner.nextLine().trim();
            if ("0".equals(choice)) break;

            try {
                switch (choice) {
                    case "1" -> {
                        String id = "BKG-" + UUID.randomUUID().toString().substring(0, 6);
                        BookingRequest req = new BookingRequest(id, "KC-951", "12A", "Merey", true, 20.0, "STANDARD", 1200);
                        orchestrator.processBooking(req);
                        System.out.println("[OK] Заказ успешно сохранен в PostgreSQL! ID: " + id);
                    }
                    case "2" -> {
                        String id = "BKG-" + UUID.randomUUID().toString().substring(0, 6);
                        BookingRequest req = new BookingRequest(id, "KC-951", "14B", "Merey", false, 55.0, "STANDARD", 1200);
                        try {
                            orchestrator.processBooking(req);
                        } catch (Exception ex) {
                            System.out.println("[СБОЙ В ШАГЕ B]: " + ex.getMessage());
                            System.out.println("[ROLLBACK]: Статус брони A переведен в RELEASED в PostgreSQL! ID: " + id);
                        }
                    }
                    case "3" -> {
                        String id = "BKG-" + UUID.randomUUID().toString().substring(0, 6);
                        BookingRequest req = new BookingRequest(id, "KC-951", "1A", "Merey", true, 18.0, "EXHAUSTED_KOSHER", 1200);
                        try {
                            orchestrator.processBooking(req);
                        } catch (Exception ex) {
                            System.out.println("[СБОЙ В ШАГЕ C]: " + ex.getMessage());
                            System.out.println("[2-STEP ROLLBACK]: Билет B аннулирован (VOIDED) -> Бронь A снята (RELEASED)! ID: " + id);
                        }
                    }
                    case "4" -> {
                        System.out.print("Введите ID бронирования (например, BKG-xxxxxx): ");
                        String bId = scanner.nextLine().trim();
                        System.out.println(adminFacade.inspectBooking(bId));
                    }
                    case "5" -> {
                        db.cleanAllData();
                        System.out.println("[CLEAN] Все таблицы PostgreSQL очищены (TRUNCATE).");
                    }
                    default -> System.out.println("Неверный пункт.");
                }
            } catch (Exception e) {
                System.out.println("[ERROR] " + e.getMessage());
            }
        }
    }
}