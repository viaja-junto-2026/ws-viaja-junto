package com.viajajunto.api.modules.review.dto;

import com.viajajunto.api.modules.auth.dto.UserDTO;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AvaliacaoResponseDTO {

    private String id;
    private UserDTO usuario;
    private Long destinoCatalogoId;
    private Long catalogoAtividadeId;
    private Integer nota;
    private String comentario;
    private LocalDateTime dataCriacao;
}
