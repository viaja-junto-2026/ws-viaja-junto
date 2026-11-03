package com.viajajunto.api.modules.review.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateAvaliacaoDTO {

    @JsonAlias({"placeId", "destinoCatalogoId"})
    private Long destinoCatalogoId;

    @JsonAlias({"activityId", "catalogoAtividadeId"})
    private Long catalogoAtividadeId;

    @NotNull(message = "A nota é obrigatória")
    @Min(value = 1, message = "A nota mínima é 1 estrela")
    @Max(value = 5, message = "A nota máxima é 5 estrelas")
    @JsonAlias({"rating", "nota"})
    private Integer nota;

    @Size(max = 1500, message = "O comentário pode ter no máximo 1500 caracteres")
    @JsonAlias({"comment", "comentario"})
    private String comentario;
}
