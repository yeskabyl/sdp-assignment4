package main.subsystem_c_loyalty;

import main.persistence.DatabaseManager;
import java.sql.*;
import java.util.Optional;

public class LoyaltyRepository {
    private final DatabaseManager db;

    public LoyaltyRepository(DatabaseManager db) {
        this.db = db;
    }

    public void save(LoyaltyRecord record) {
        String sql = """
            INSERT INTO loyalty_records (id, ticket_id, meal_choice, points_awarded, status)
            VALUES (?, ?, ?, ?, ?)
            ON CONFLICT (id) DO UPDATE SET status = EXCLUDED.status;
        """;
        try (Connection conn = db.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, record.getId());
            ps.setString(2, record.getTicketId());
            ps.setString(3, record.getMealChoice());
            ps.setInt(4, record.getPointsAwarded());
            ps.setString(5, record.getStatus().name());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("PostgreSQL: failed to persist LoyaltyRecord", e);
        }
    }

    public Optional<LoyaltyRecord> findById(String id) {
        String sql = "SELECT * FROM loyalty_records WHERE id = ?";
        try (Connection conn = db.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(new LoyaltyRecord(
                            rs.getString("id"),
                            rs.getString("ticket_id"),
                            rs.getString("meal_choice"),
                            rs.getInt("points_awarded"),
                            LoyaltyStatus.valueOf(rs.getString("status"))
                    ));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("PostgreSQL: failed to load LoyaltyRecord", e);
        }
        return Optional.empty();
    }
}