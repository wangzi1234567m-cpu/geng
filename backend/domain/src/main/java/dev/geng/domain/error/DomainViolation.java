package dev.geng.domain.error;

import java.util.Objects;

public final class DomainViolation extends RuntimeException {
    private final FailureCode code;

    public DomainViolation(FailureCode code) {
        super(Objects.requireNonNull(code).name());
        this.code = code;
    }

    public FailureCode code() {
        return code;
    }
}
