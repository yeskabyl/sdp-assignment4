package domain;

public enum ChainState {
    STARTED,
    STEP_A_COMMITTED,
    STEP_B_COMMITTED,
    COMPLETED,
    FAILED_AT_B,
    FAILED_AT_C,
    COMPENSATED
}
