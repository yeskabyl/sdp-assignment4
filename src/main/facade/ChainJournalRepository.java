package facade;

import domain.ChainState;
import persistence.DatabaseManager;
import java.sql.*;
import java.util.Optional;

public class ChainJournalRepository {
    private final DatabaseManager db;

    public ChainJournalRepository(DatabaseManager db) {
        this.db = db;
    }

    public void updateJournal(String bookingId, String resId, String ticketId, String loyaltyId, ChainState state) {
        String sql = """
            INSERT INTO booking_chain_journal (booking_id, reservation_id, ticket_id, loyalty_id, chain_state)
            VALUES (?, ?, ?, ?, ?)
            ON CONFLICT (booking_id) DO UPDATE SET
                reservation_id = COALESCE(EXCLUDED.reservation_id, booking_chain_journal.reservation_id),
                ticket_id = COALESCE(EXCLUDED.ticket_id, booking_chain_journal.ticket_id),
                loyalty_id = COALESCE(EXCLUDED.loyalty_id, booking_chain_journal.loyalty_id),
                chain_state = EXCLUDED.chain_state;
        """;
        try (Connection conn = db.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, bookingId);
            ps.setString(2, resId);
            ps.setString(3, ticketId);
            ps.setString(4, loyaltyId);
            ps.setString(5, state.name());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to update journal in PostgreSQL", e);
        }
    }

    public Optional<JournalEntry> findByBookingId(String bookingId) {
        String sql = "SELECT * FROM booking_chain_journal WHERE booking_id = ?";
        try (Connection conn = db.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, bookingId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(new JournalEntry(
                            rs.getString("booking_id"),
                            rs.getString("reservation_id"),
                            rs.getString("ticket_id"),
                            rs.getString("loyalty_id"),
                            ChainState.valueOf(rs.getString("chain_state"))
                    ));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to read journal from PostgreSQL", e);
        }
        return Optional.empty();
    }

    public record JournalEntry(String bookingId, String resId, String ticketId, String loyaltyId, ChainState state) {}
}