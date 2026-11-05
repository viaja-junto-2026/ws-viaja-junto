package com.viajajunto.api.modules.review.repository;

import com.viajajunto.api.modules.review.entity.AvaliacaoEntity;
import java.util.ArrayList;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryConditional;
import software.amazon.awssdk.services.dynamodb.model.ResourceInUseException;
import software.amazon.awssdk.services.dynamodb.model.ResourceNotFoundException;

/**
 * Acesso às avaliações no DynamoDB (único uso do DynamoDB; o restante é relacional).
 *
 * <p>A tabela é criada no primeiro uso, e não na subida da aplicação: assim o contexto sobe sem o
 * DynamoDB no ar e qualquer falha de acesso aparece (com log) na operação que a provocou.
 */
@Slf4j
@Repository
public class AvaliacaoRepository {

    /** Total e média das notas de um destino ou atividade, calculados sobre a mesma leitura. */
    public record ResumoNotas(int total, double media) {

        /** Média com uma casa decimal; zero quando não há avaliações. */
        public double mediaArredondada() {
            return Math.round(media * 10.0) / 10.0;
        }
    }

    private final String tableName;
    private final DynamoDbTable<AvaliacaoEntity> table;
    private volatile boolean tableReady;

    public AvaliacaoRepository(
            DynamoDbEnhancedClient enhancedClient,
            @Value("${aws.dynamodb.table-name:Avaliacoes}") String tableName
    ) {
        this.tableName = tableName;
        this.table = enhancedClient.table(tableName, TableSchema.fromBean(AvaliacaoEntity.class));
    }

    public AvaliacaoEntity save(AvaliacaoEntity avaliacao) {
        ensureTable();
        try {
            table.putItem(avaliacao);
        } catch (RuntimeException e) {
            log.error("Falha ao salvar a avaliação {} na tabela {}: {}",
                    avaliacao.getId(), tableName, e.getMessage());
            throw e;
        }
        return avaliacao;
    }

    public List<AvaliacaoEntity> findAllByDestinoCatalogoId(Long destinoCatalogoId) {
        return queryByTargetKey("DESTINO#" + destinoCatalogoId);
    }

    public List<AvaliacaoEntity> findAllByCatalogoAtividadeId(Long catalogoAtividadeId) {
        return queryByTargetKey("ATIVIDADE#" + catalogoAtividadeId);
    }

    public ResumoNotas resumirDestino(Long destinoId) {
        return resumir(findAllByDestinoCatalogoId(destinoId));
    }

    public ResumoNotas resumirAtividade(Long atividadeId) {
        return resumir(findAllByCatalogoAtividadeId(atividadeId));
    }

    private ResumoNotas resumir(List<AvaliacaoEntity> avaliacoes) {
        if (avaliacoes.isEmpty()) {
            return new ResumoNotas(0, 0.0);
        }
        double soma = avaliacoes.stream()
                .mapToInt(a -> a.getNota() != null ? a.getNota() : 0)
                .sum();
        return new ResumoNotas(avaliacoes.size(), soma / avaliacoes.size());
    }

    /**
     * Lê todas as avaliações do alvo. O DynamoDB não tem "página N" (só cursor), então a paginação
     * da API é feita em memória pelo service: serve ao volume atual, mas não escala para milhares
     * de avaliações por destino.
     */
    private List<AvaliacaoEntity> queryByTargetKey(String partitionKeyValue) {
        ensureTable();
        List<AvaliacaoEntity> result = new ArrayList<>();
        try {
            QueryConditional queryConditional = QueryConditional.keyEqualTo(
                    Key.builder().partitionValue(partitionKeyValue).build()
            );
            table.query(queryConditional).items().forEach(result::add);
        } catch (ResourceNotFoundException e) {
            log.warn("Tabela {} não existe ao consultar {}: devolvendo lista vazia.",
                    tableName, partitionKeyValue);
            return result;
        } catch (RuntimeException e) {
            log.error("Falha ao consultar {} na tabela {}: {}", partitionKeyValue, tableName, e.getMessage());
            throw e;
        }
        return result;
    }

    private void ensureTable() {
        if (tableReady) {
            return;
        }
        synchronized (this) {
            if (tableReady) {
                return;
            }
            try {
                table.createTable();
                log.info("Tabela DynamoDB {} criada.", tableName);
            } catch (ResourceInUseException e) {
                log.debug("Tabela DynamoDB {} já existe.", tableName);
            } catch (RuntimeException e) {
                log.error("Falha ao criar a tabela DynamoDB {}: {}", tableName, e.getMessage());
                throw e;
            }
            tableReady = true;
        }
    }
}
