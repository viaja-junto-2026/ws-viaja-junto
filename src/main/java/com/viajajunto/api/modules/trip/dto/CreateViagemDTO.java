package com.viajajunto.api.modules.trip.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.viajajunto.api.modules.trip.entity.StatusViagem;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateViagemDTO {

    @NotBlank(message = "O nome da viagem é obrigatório")
    @Size(min = 2, max = 150, message = "O nome da viagem deve ter entre 2 e 150 caracteres")
    @JsonAlias({"title", "nome"})
    private String nome;

    @Size(max = 1000, message = "A descrição pode ter no máximo 1000 caracteres")
    @JsonAlias({"description", "descricao"})
    private String descricao;

    @JsonAlias({"startDate", "dataInicio"})
    private LocalDate dataInicio;

    @JsonAlias({"endDate", "dataFim"})
    private LocalDate dataFim;

    private StatusViagem status;

    @JsonAlias({"budget", "orcamentoTotal"})
    private BigDecimal orcamentoTotal;
}
