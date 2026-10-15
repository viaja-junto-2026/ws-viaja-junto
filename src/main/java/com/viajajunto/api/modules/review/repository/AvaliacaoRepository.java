package com.viajajunto.api.modules.review.repository;

import com.viajajunto.api.modules.review.entity.AvaliacaoEntity;
import jakarta.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryConditional;
import software.amazon.awssdk.services.dynamodb.model.ResourceNotFoundException;

@Repository
public class AvaliacaoRepository {

    private final DynamoDbEnhancedClient enhancedClient;
    private final String tableName;
    private DynamoDbTable<AvaliacaoEntity> table;

    public AvaliacaoRepository(
            DynamoDbEnhancedClient enhancedClient,
            @Value("${aws.dynamodb.table-name:Avaliacoes}") String tableName
    ) {
        this.enhancedClient = enhancedClient;
        this.tableName = tableName;
    }

    @PostConstruct
    public void init() {
        this.table = enhancedClient.table(tableName, TableSchema.fromBean(AvaliacaoEntity.class));
        try {
            this.table.createTable();
        } catch (Exception ignored) {
            // Tabela já existe ou criação gerenciada externamente
        }
    }

    public AvaliacaoEntity save(AvaliacaoEntity avaliacao) {
        table.putItem(avaliacao);
        return avaliacao;
    }

    public List<AvaliacaoEntity> findAllByDestinoCatalogoId(Long destinoCatalogoId) {
        return queryByTargetKey("DESTINO#" + destinoCatalogoId);
    }

    public List<AvaliacaoEntity> findAllByCatalogoAtividadeId(Long catalogoAtividadeId) {
        return queryByTargetKey("ATIVIDADE#" + catalogoAtividadeId);
    }

    public Double calculateAverageRatingForDestino(Long destinoId) {
        List<AvaliacaoEntity> avaliacoes = findAllByDestinoCatalogoId(destinoId);
        if (avaliacoes.isEmpty()) {
            return null;
        }
        double sum = avaliacoes.stream()
                .mapToInt(a -> a.getNota() != null ? a.getNota() : 0)
                .sum();
        return sum / avaliacoes.size();
    }

    public Double calculateAverageRatingForAtividade(Long atividadeId) {
        List<AvaliacaoEntity> avaliacoes = findAllByCatalogoAtividadeId(atividadeId);
        if (avaliacoes.isEmpty()) {
            return null;
        }
        double sum = avaliacoes.stream()
                .mapToInt(a -> a.getNota() != null ? a.getNota() : 0)
                .sum();
        return sum / avaliacoes.size();
    }

    private List<AvaliacaoEntity> queryByTargetKey(String partitionKeyValue) {
        List<AvaliacaoEntity> result = new ArrayList<>();
        try {
            QueryConditional queryConditional = QueryConditional.keyEqualTo(
                    Key.builder().partitionValue(partitionKeyValue).build()
            );
            table.query(queryConditional).items().forEach(result::add);
        } catch (ResourceNotFoundException e) {
            return result;
        }
        return result;
    }
}
