package persistence;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public class PostgresDatabaseManager {
    private final String url;
    private final String user;
    private final String password;

    public PostgresDatabaseManager(String host, int port, String dbName, String user, String password) {
        this.url = String.format("jdbc:postgresql://%s:%d/%s", host, port, dbName);
        this.user = user;
        this.password = password;
        initTables();
    }

    public PostgresDatabaseManager(String dbName) {
        this("localhost", 5432, dbName, user, password);
    }

    public Connection getConnection() throws SQLException {
        return DriverManager.getConnection(url, user, password);
    }

    public void initTables() {
        try (Connection conn = getConnection(); Statement stmt = conn.createStatement()) {
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS seat_reservations (
                    id VARCHAR(64) PRIMARY KEY,
                    flight_number VARCHAR(32) NOT NULL,
                    seat_number VARCHAR(16) NOT NULL,
                    passenger_name VARCHAR(128) NOT NULL,
                    calculated_fare DOUBLE PRECISION NOT NULL,
                    status VARCHAR(32) NOT NULL
                );
            """);

            stmt.execute("""
                CREATE TABLE IF NOT EXISTS tickets (
                    id VARCHAR(64) PRIMARY KEY,
                    reservation_id VARCHAR(64) NOT NULL,
                    baggage_weight DOUBLE PRECISION NOT NULL,
                    status VARCHAR(32) NOT NULL
                );
            """);

            stmt.execute("""
                CREATE TABLE IF NOT EXISTS loyalty_records (
                    id VARCHAR(64) PRIMARY KEY,
                    ticket_id VARCHAR(64) NOT NULL,
                    meal_choice VARCHAR(64) NOT NULL,
                    points_awarded INT NOT NULL,
                    status VARCHAR(32) NOT NULL
                );
            """);

            stmt.execute("""
                CREATE TABLE IF NOT EXISTS booking_chain_journal (
                    booking_id VARCHAR(64) PRIMARY KEY,
                    reservation_id VARCHAR(64),
                    ticket_id VARCHAR(64),
                    loyalty_id VARCHAR(64),
                    chain_state VARCHAR(32) NOT NULL
                );
            """);
        } catch (SQLException e) {
            throw new RuntimeException("PostgreSQL schema initialization failed", e);
        }
    }

    public void cleanAllData() {
        try (Connection conn = getConnection(); Statement stmt = conn.createStatement()) {
            stmt.execute("TRUNCATE TABLE booking_chain_journal, loyalty_records, tickets, seat_reservations;");
        } catch (SQLException e) {
            throw new RuntimeException("PostgreSQL cleanup failed", e);
        }
    }
}