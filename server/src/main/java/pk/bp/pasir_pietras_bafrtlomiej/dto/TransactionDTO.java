package pk.bp.pasir_pietras_bafrtlomiej.dto;

import lombok.Getter;
import lombok.Setter;
import jakarta.validation.constraints.*;

@Setter
@Getter
public class TransactionDTO {

    @NotNull(message = "Kwota nie może być pusta")
    @DecimalMin(value = "0.01", message = "Kwota musi być większa niż 0")
    private Double amount;

    @Pattern(regexp = "(INCOME|EXPENSE)$", message = "Typ transakcji musi być INCOME lub EXPENSE")
    private String type;

    @Size(max = 50, message = "Tagi nie mogą przekraczać 50 znaków")
    private String tags;

    @Size(max = 255, message = "Notatka może mieć maksymalnie 255 znaków")
    private String notes;
}
