package com.viajajunto.api.modules.review.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.Setter;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbSortKey;

@DynamoDbBean
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AvaliacaoEntity {

    private String targetKey;
    private String id;
    private Long usuarioId;
    private String usuarioNome;
    private String usuarioEmail;
    private String usuarioAvatarUrl;
    private Long destinoCatalogoId;
    private Long catalogoAtividadeId;
    private Integer nota;
    private String comentario;
    private String dataCriacao;

    @DynamoDbPartitionKey
    public String getTargetKey() {
        return targetKey;
    }

    @DynamoDbSortKey
    public String getId() {
        return id;
    }

    public Long getUsuarioId() {
        return usuarioId;
    }

    public String getUsuarioNome() {
        return usuarioNome;
    }

    public String getUsuarioEmail() {
        return usuarioEmail;
    }

    public String getUsuarioAvatarUrl() {
        return usuarioAvatarUrl;
    }

    public Long getDestinoCatalogoId() {
        return destinoCatalogoId;
    }

    public Long getCatalogoAtividadeId() {
        return catalogoAtividadeId;
    }

    public Integer getNota() {
        return nota;
    }

    public String getComentario() {
        return comentario;
    }

    public String getDataCriacao() {
        return dataCriacao;
    }
}
