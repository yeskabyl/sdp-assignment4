package facade;

import domain.BookingRequest;
import domain.ChainState;
import domain.exceptions.BookingException;
import subsystem_a_seat.*;
import subsystem_b_ticket.*;
import subsystem_c_loyalty.*;

public class BookingOrchestratorFacade {
    private final SeatReservationRepository seatRepo;
    private final FareCalculator fareCalculator;
    private final TicketRepository ticketRepo;
    private final BaggageRulesEngine baggageEngine;
    private final LoyaltyRepository loyaltyRepo;
    private final MealPreferenceService mealService;
    private final ChainJournalRepository journalRepo;

    public BookingOrchestratorFacade(
            SeatReservationRepository seatRepo,
            FareCalculator fareCalculator,
            TicketRepository ticketRepo,
            BaggageRulesEngine baggageEngine,
            LoyaltyRepository loyaltyRepo,
            MealPreferenceService mealService,
            ChainJournalRepository journalRepo
    ) {
        this.seatRepo = seatRepo;
        this.fareCalculator = fareCalculator;
        this.ticketRepo = ticketRepo;
        this.baggageEngine = baggageEngine;
        this.loyaltyRepo = loyaltyRepo;
        this.mealService = mealService;
        this.journalRepo = journalRepo;
    }

    public void processBooking(BookingRequest req) throws BookingException {
        if (req.flightNumber() == null || req.flightNumber().isBlank()) {
            throw new BookingException("Validation failed before chain execution: Flight number is required");
        }

        String resId = "RES-" + req.bookingId();
        String ticketId = "TKT-" + req.bookingId();
        String loyaltyId = "LOY-" + req.bookingId();

        journalRepo.updateJournal(req.bookingId(), null, null, null, ChainState.STARTED);

        // Step A: Intake / Reservation
        double fare = fareCalculator.calculateFare(req.isWindowOrAisle());
        SeatReservation seat = new SeatReservation(resId, req.flightNumber(), req.seatNumber(), req.passengerName(), fare);
        seat.hold();
        seatRepo.save(seat);
        journalRepo.updateJournal(req.bookingId(), resId, null, null, ChainState.STEP_A_COMMITTED);

        // Step B: Processing / Execution
        Ticket ticket;
        try {
            baggageEngine.validateBaggageWeight(req.baggageWeightKg());
            ticket = new Ticket(ticketId, resId, req.baggageWeightKg());
            ticket.issue();
            ticketRepo.save(ticket);
            journalRepo.updateJournal(req.bookingId(), resId, ticketId, null, ChainState.STEP_B_COMMITTED);
        } catch (Exception ex) {
            journalRepo.updateJournal(req.bookingId(), resId, null, null, ChainState.FAILED_AT_B);
            compensateStepA(resId);
            journalRepo.updateJournal(req.bookingId(), resId, null, null, ChainState.COMPENSATED);
            throw ex;
        }

        // Step C: Finalization / Confirmation
        try {
            mealService.verifyMealAvailability(req.mealPreference());
            int miles = mealService.calculateLoyaltyMiles(req.distanceMiles());
            LoyaltyRecord loyalty = new LoyaltyRecord(loyaltyId, ticketId, req.mealPreference(), miles);
            loyalty.confirm();
            loyaltyRepo.save(loyalty);
            journalRepo.updateJournal(req.bookingId(), resId, ticketId, loyaltyId, ChainState.COMPLETED);
        } catch (Exception ex) {
            journalRepo.updateJournal(req.bookingId(), resId, ticketId, null, ChainState.FAILED_AT_C);
            compensateStepB(ticketId);
            compensateStepA(resId);
            journalRepo.updateJournal(req.bookingId(), resId, ticketId, null, ChainState.COMPENSATED);
            throw ex;
        }
    }

    private void compensateStepA(String resId) {
        seatRepo.findById(resId).ifPresent(seat -> {
            seat.release();
            seatRepo.save(seat);
        });
    }

    private void compensateStepB(String ticketId) {
        ticketRepo.findById(ticketId).ifPresent(ticket -> {
            ticket.voidTicket();
            ticketRepo.save(ticket);
        });
    }
}