package com.viajajunto.api.modules.auth.dto;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** PATCH /users/me: campos ausentes (null) não são alterados. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateUserDTO {

    @Pattern(regexp = ".*\\S.*", message = "O nome não pode ficar em branco.")
    @Size(max = 255, message = "O nome deve ter no máximo 255 caracteres.")
    private String nome;

    @Size(max = 255, message = "A URL do avatar deve ter no máximo 255 caracteres.")
    private String avatarUrl;
}
