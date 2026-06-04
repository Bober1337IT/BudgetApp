package pk.bp.pasir_pietras_bafrtlomiej.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UserDTO {
    @NotBlank
    private String username;

    @NotBlank
    @Email(message = "Nieprawidłowy format adresu email")
    private String email;

    @NotBlank
    private String password;
}
