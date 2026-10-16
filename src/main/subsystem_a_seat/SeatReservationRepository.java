package main.subsystem_a_seat;

import main.persistence.DatabaseManager;
import java.sql.*;
import java.util.Optional;

public class SeatReservationRepository {
    private final DatabaseManager db;

    public SeatReservationRepository(DatabaseManager db) {
        this.db = db;
    }

    public void save(SeatReservation res) {
        String sql = """
            INSERT INTO seat_reservations (id, flight_number, seat_number, passenger_name, calculated_fare, status)
            VALUES (?, ?, ?, ?, ?, ?)
            ON CONFLICT (id) DO UPDATE SET status = EXCLUDED.status;
        """;
        try (Connection conn = db.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, res.getId());
            ps.setString(2, res.getFlightNumber());
            ps.setString(3, res.getSeatNumber());
            ps.setString(4, res.getPassengerName());
            ps.setDouble(5, res.getFare());
            ps.setString(6, res.getStatus().name());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("PostgreSQL: failed to persist SeatReservation", e);
        }
    }

    public Optional<SeatReservation> findById(String id) {
        String sql = "SELECT * FROM seat_reservations WHERE id = ?";
        try (Connection conn = db.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(new SeatReservation(
                            rs.getString("id"),
                            rs.getString("flight_number"),
                            rs.getString("seat_number"),
                            rs.getString("passenger_name"),
                            rs.getDouble("calculated_fare"),
                            SeatReservationStatus.valueOf(rs.getString("status"))
                    ));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("PostgreSQL: failed to load SeatReservation", e);
        }
        return Optional.empty();
    }
}