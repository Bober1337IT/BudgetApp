package pk.bp.pasir_pietras_bafrtlomiej.model;

import com.fasterxml.jackson.annotation.JsonCreator;

public enum TransactionType {
    INCOME,
    EXPENSE;

    @JsonCreator
    public static TransactionType fromString(String value) {
        return TransactionType.valueOf(value.toUpperCase());
    }
}
