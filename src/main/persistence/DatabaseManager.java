package persistence;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public class DatabaseManager {
    private final String dbUrl;

    public DatabaseManager(String dbPath) {
        this.dbUrl = "jdbc:sqlite:" + dbPath;
        initTables();
    }

    public Connection getConnection() throws SQLException {
        return DriverManager.getConnection(dbUrl);
    }

    public void initTables() {
        try (Connection conn = getConnection(); Statement stmt = conn.createStatement()) {
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS seat_reservations (
                    id TEXT PRIMARY KEY,
                    flight_number TEXT,
                    seat_number TEXT,
                    passenger_name TEXT,
                    calculated_fare REAL,
                    status TEXT
                );
            """);
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS tickets (
                    id TEXT PRIMARY KEY,
                    reservation_id TEXT,
                    baggage_weight REAL,
                    status TEXT
                );
            """);
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS loyalty_records (
                    id TEXT PRIMARY KEY,
                    ticket_id TEXT,
                    meal_choice TEXT,
                    points_awarded INTEGER,
                    status TEXT
                );
            """);
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS booking_chain_journal (
                    booking_id TEXT PRIMARY KEY,
                    reservation_id TEXT,
                    ticket_id TEXT,
                    loyalty_id TEXT,
                    chain_state TEXT
                );
            """);
        } catch (SQLException e) {
            throw new RuntimeException("Database init failed", e);
        }
    }

    public void cleanAllData() {
        try (Connection conn = getConnection(); Statement stmt = conn.createStatement()) {
            stmt.execute("DELETE FROM seat_reservations;");
            stmt.execute("DELETE FROM tickets;");
            stmt.execute("DELETE FROM loyalty_records;");
            stmt.execute("DELETE FROM booking_chain_journal;");
        } catch (SQLException e) {
            throw new RuntimeException("DB clean failed", e);
        }
    }
}