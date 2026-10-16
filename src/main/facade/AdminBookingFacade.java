package main.facade;

import main.domain.ChainState;
import main.subsystem_a_seat.*;
import main.subsystem_b_ticket.*;
import main.subsystem_c_loyalty.*;

public class AdminBookingFacade {
    private final ChainJournalRepository journalRepo;
    private final SeatReservationRepository seatRepo;
    private final TicketRepository ticketRepo;
    private final LoyaltyRepository loyaltyRepo;

    public AdminBookingFacade(
            ChainJournalRepository journalRepo,
            SeatReservationRepository seatRepo,
            TicketRepository ticketRepo,
            LoyaltyRepository loyaltyRepo
    ) {
        this.journalRepo = journalRepo;
        this.seatRepo = seatRepo;
        this.ticketRepo = ticketRepo;
        this.loyaltyRepo = loyaltyRepo;
    }

    public String inspectBooking(String bookingId) {
        var entryOpt = journalRepo.findByBookingId(bookingId);
        if (entryOpt.isEmpty()) return "Бронирование с ID " + bookingId + " не найдено в БД.";

        var entry = entryOpt.get();
        StringBuilder sb = new StringBuilder();
        sb.append("\n================ AUDIT REPORT (PostgreSQL) ================\n");
        sb.append("Booking ID:  ").append(entry.bookingId()).append("\n");
        sb.append("Chain State: ").append(entry.state()).append("\n");
        sb.append("-----------------------------------------------------------\n");

        if (entry.resId() != null) {
            seatRepo.findById(entry.resId()).ifPresent(s ->
                    sb.append(String.format("Step A [Seat]:   Место %s, Рейс %s, Статус: %s\n",
                            s.getSeatNumber(), s.getFlightNumber(), s.getStatus()))
            );
        }
        if (entry.ticketId() != null) {
            ticketRepo.findById(entry.ticketId()).ifPresent(t ->
                    sb.append(String.format("Step B [Ticket]: Билет %s, Вес: %.1f кг, Статус: %s\n",
                            t.getId(), t.getBaggageWeight(), t.getStatus()))
            );
        }
        if (entry.loyaltyId() != null) {
            loyaltyRepo.findById(entry.loyaltyId()).ifPresent(l ->
                    sb.append(String.format("Step C [Loyalty]: Питание: %s, Баллы: %d, Статус: %s\n",
                            l.getMealChoice(), l.getPointsAwarded(), l.getStatus()))
            );
        }
        sb.append("===========================================================");
        return sb.toString();
    }

    public void manualFullRollback(String bookingId) {
        var entryOpt = journalRepo.findByBookingId(bookingId);
        if (entryOpt.isEmpty()) return;
        var entry = entryOpt.get();

        if (entry.loyaltyId() != null) {
            loyaltyRepo.findById(entry.loyaltyId()).ifPresent(l -> {
                if (l.getStatus() == LoyaltyStatus.CONFIRMED) {
                    l.revoke();
                    loyaltyRepo.save(l);
                }
            });
        }
        if (entry.ticketId() != null) {
            ticketRepo.findById(entry.ticketId()).ifPresent(t -> {
                if (t.getStatus() == TicketStatus.ISSUED) {
                    t.voidTicket();
                    ticketRepo.save(t);
                }
            });
        }
        if (entry.resId() != null) {
            seatRepo.findById(entry.resId()).ifPresent(s -> {
                if (s.getStatus() == SeatReservationStatus.HELD) {
                    s.release();
                    seatRepo.save(s);
                }
            });
        }
        journalRepo.updateJournal(bookingId, entry.resId(), entry.ticketId(), entry.loyaltyId(), ChainState.COMPENSATED);
    }
}