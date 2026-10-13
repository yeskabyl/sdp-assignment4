package subsystem_b_ticket;

import persistence.PostgresDatabaseManager;
import java.sql.*;
import java.util.Optional;

public class TicketRepository {
    private final PostgresDatabaseManager db;

    public TicketRepository(PostgresDatabaseManager db) {
        this.db = db;
    }

    public void save(Ticket ticket) {
        String sql = """
            INSERT INTO tickets (id, reservation_id, baggage_weight, status)
            VALUES (?, ?, ?, ?)
            ON CONFLICT (id) DO UPDATE SET status = EXCLUDED.status;
        """;
        try (Connection conn = db.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, ticket.getId());
            ps.setString(2, ticket.getReservationId());
            ps.setDouble(3, ticket.getBaggageWeight());
            ps.setString(4, ticket.getStatus().name());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("PostgreSQL: failed to persist Ticket", e);
        }
    }

    public Optional<Ticket> findById(String id) {
        String sql = "SELECT * FROM tickets WHERE id = ?";
        try (Connection conn = db.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(new Ticket(
                            rs.getString("id"),
                            rs.getString("reservation_id"),
                            rs.getDouble("baggage_weight"),
                            TicketStatus.valueOf(rs.getString("status"))
                    ));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("PostgreSQL: failed to load Ticket", e);
        }
        return Optional.empty();
    }
}