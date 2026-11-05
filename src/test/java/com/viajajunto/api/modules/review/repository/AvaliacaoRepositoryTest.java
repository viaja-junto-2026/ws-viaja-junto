package com.viajajunto.api.modules.review.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.viajajunto.api.modules.activity.entity.CatalogoAtividadeEntity;
import com.viajajunto.api.modules.activity.repository.CatalogoAtividadeRepository;
import com.viajajunto.api.modules.auth.entity.UserEntity;
import com.viajajunto.api.modules.auth.repository.UserRepository;
import com.viajajunto.api.modules.destination.entity.DestinoCatalogoEntity;
import com.viajajunto.api.modules.destination.repository.DestinoCatalogoRepository;
import com.viajajunto.api.modules.review.entity.AvaliacaoEntity;
import com.viajajunto.api.modules.review.repository.AvaliacaoRepository.ResumoNotas;
import com.viajajunto.api.modules.review.seed.AvaliacaoSeedRunner;
import java.net.URI;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.data.domain.Sort;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;

/**
 * Teste do repositório contra um DynamoDB de verdade (MiniStack, o mesmo emulador do
 * docker-compose). Precisa do Docker no ar; sem ele, o teste é ignorado.
 */
@Testcontainers(disabledWithoutDocker = true)
class AvaliacaoRepositoryTest {

    @Container
    private static final GenericContainer<?> MINISTACK = new GenericContainer<>("ministackorg/ministack:latest")
            .withExposedPorts(4566)
            .waitingFor(Wait.forListeningPort());

    private static DynamoDbClient dynamoDbClient;

    private String tableName;
    private AvaliacaoRepository repository;

    @BeforeAll
    static void connect() throws InterruptedException {
        dynamoDbClient = DynamoDbClient.builder()
                .region(Region.US_EAST_1)
                .endpointOverride(URI.create("http://" + MINISTACK.getHost() + ":" + MINISTACK.getMappedPort(4566)))
                .credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create("test", "test")))
                .build();

        // A porta abre antes do emulador responder: espera o DynamoDB atender de fato.
        RuntimeException last = null;
        for (int tentativa = 0; tentativa < 30; tentativa++) {
            try {
                dynamoDbClient.listTables();
                return;
            } catch (RuntimeException e) {
                last = e;
                Thread.sleep(1000);
            }
        }
        throw new IllegalStateException("MiniStack não respondeu a tempo", last);
    }

    @BeforeEach
    void setUp() {
        // Uma tabela por teste: nenhum teste enxerga os dados de outro.
        tableName = "Avaliacoes-" + UUID.randomUUID();
        repository = novoRepositorio(tableName);
    }

    @AfterEach
    void dropTable() {
        dynamoDbClient.deleteTable(builder -> builder.tableName(tableName));
    }

    private AvaliacaoRepository novoRepositorio(String table) {
        DynamoDbEnhancedClient enhanced = DynamoDbEnhancedClient.builder().dynamoDbClient(dynamoDbClient).build();
        return new AvaliacaoRepository(enhanced, table);
    }

    private AvaliacaoEntity avaliacaoDeDestino(Long destinoId, String id, long usuarioId, int nota) {
        return AvaliacaoEntity.builder()
                .targetKey("DESTINO#" + destinoId)
                .id(id)
                .usuarioId(usuarioId)
                .usuarioNome("Usuário " + usuarioId)
                .destinoCatalogoId(destinoId)
                .nota(nota)
                .comentario("Comentário " + id)
                .dataCriacao("2026-01-10T12:00:00Z")
                .build();
    }

    private AvaliacaoEntity avaliacaoDeAtividade(Long atividadeId, String id, long usuarioId, int nota) {
        return AvaliacaoEntity.builder()
                .targetKey("ATIVIDADE#" + atividadeId)
                .id(id)
                .usuarioId(usuarioId)
                .usuarioNome("Usuário " + usuarioId)
                .catalogoAtividadeId(atividadeId)
                .nota(nota)
                .comentario("Comentário " + id)
                .dataCriacao("2026-01-10T12:00:00Z")
                .build();
    }

    @Test
    @DisplayName("Salva avaliações de um destino e consulta só as dele")
    void saveAndFindByDestino() {
        repository.save(avaliacaoDeDestino(1L, "a1", 1, 5));
        repository.save(avaliacaoDeDestino(1L, "a2", 2, 4));
        repository.save(avaliacaoDeDestino(2L, "b1", 1, 3));

        List<AvaliacaoEntity> doDestino1 = repository.findAllByDestinoCatalogoId(1L);

        assertEquals(2, doDestino1.size());
        assertTrue(doDestino1.stream().allMatch(a -> a.getDestinoCatalogoId().equals(1L)));
        assertEquals(List.of("a1", "a2"), doDestino1.stream().map(AvaliacaoEntity::getId).sorted().toList());
        assertEquals(1, repository.findAllByDestinoCatalogoId(2L).size());
    }

    @Test
    @DisplayName("Salva avaliações de uma atividade sem misturar com as de destino de mesmo número")
    void saveAndFindByAtividade() {
        repository.save(avaliacaoDeAtividade(1L, "a1", 1, 5));
        repository.save(avaliacaoDeDestino(1L, "d1", 1, 2));

        List<AvaliacaoEntity> daAtividade = repository.findAllByCatalogoAtividadeId(1L);

        assertEquals(1, daAtividade.size());
        assertEquals("a1", daAtividade.get(0).getId());
        assertEquals(1L, daAtividade.get(0).getCatalogoAtividadeId());
        assertEquals("Comentário a1", daAtividade.get(0).getComentario());
    }

    @Test
    @DisplayName("Alvo sem avaliações devolve lista vazia e resumo zerado")
    void emptyTarget() {
        assertTrue(repository.findAllByDestinoCatalogoId(99L).isEmpty());
        assertEquals(new ResumoNotas(0, 0.0), repository.resumirDestino(99L));
    }

    @Test
    @DisplayName("Resumo traz total e média da mesma leitura")
    void summary() {
        repository.save(avaliacaoDeDestino(1L, "a1", 1, 5));
        repository.save(avaliacaoDeDestino(1L, "a2", 2, 4));
        repository.save(avaliacaoDeDestino(1L, "a3", 3, 4));

        ResumoNotas resumo = repository.resumirDestino(1L);

        assertEquals(3, resumo.total());
        assertEquals(4.3, resumo.mediaArredondada());
    }

    @Test
    @DisplayName("Salvar o mesmo item duas vezes não duplica")
    void saveSameItemTwice() {
        repository.save(avaliacaoDeDestino(1L, "a1", 1, 5));
        repository.save(avaliacaoDeDestino(1L, "a1", 1, 4));

        List<AvaliacaoEntity> avaliacoes = repository.findAllByDestinoCatalogoId(1L);

        assertEquals(1, avaliacoes.size());
        assertEquals(4, avaliacoes.get(0).getNota());
    }

    @Test
    @DisplayName("Outro repositório sobre a mesma tabela já existente funciona")
    void tableAlreadyExists() {
        repository.save(avaliacaoDeDestino(1L, "a1", 1, 5));

        AvaliacaoRepository outro = novoRepositorio(tableName);

        assertEquals(1, outro.findAllByDestinoCatalogoId(1L).size());
    }

    @Test
    @DisplayName("Seed de dev é idempotente: rodar duas vezes não duplica e os contadores batem")
    void seedRunsTwiceWithoutDuplicating() {
        UserRepository userRepository = mock(UserRepository.class);
        DestinoCatalogoRepository destinoRepository = mock(DestinoCatalogoRepository.class);
        CatalogoAtividadeRepository atividadeRepository = mock(CatalogoAtividadeRepository.class);
        List<UserEntity> usuarios = List.of(
                UserEntity.builder().id(1L).nome("Ana").email("ana@email.com").build(),
                UserEntity.builder().id(2L).nome("Carlos").email("carlos@email.com").build(),
                UserEntity.builder().id(3L).nome("Beatriz").email("beatriz@email.com").build());
        DestinoCatalogoEntity paris = DestinoCatalogoEntity.builder().id(1L).nome("Paris").build();
        CatalogoAtividadeEntity torre = CatalogoAtividadeEntity.builder().id(1L).nome("Torre Eiffel").build();
        when(userRepository.findAll(any(Sort.class))).thenReturn(usuarios);
        when(destinoRepository.findAll()).thenReturn(List.of(paris));
        when(atividadeRepository.findAll()).thenReturn(List.of(torre));
        AvaliacaoSeedRunner seed = new AvaliacaoSeedRunner(repository, userRepository, destinoRepository, atividadeRepository);

        seed.run(new DefaultApplicationArguments());
        int destinoDepoisDaPrimeira = repository.findAllByDestinoCatalogoId(1L).size();
        int atividadeDepoisDaPrimeira = repository.findAllByCatalogoAtividadeId(1L).size();
        seed.run(new DefaultApplicationArguments());

        assertEquals(3, destinoDepoisDaPrimeira);
        assertEquals(3, atividadeDepoisDaPrimeira);
        assertEquals(destinoDepoisDaPrimeira, repository.findAllByDestinoCatalogoId(1L).size());
        assertEquals(atividadeDepoisDaPrimeira, repository.findAllByCatalogoAtividadeId(1L).size());

        // Os contadores do catálogo batem com as avaliações que existem no DynamoDB.
        ResumoNotas resumoDestino = repository.resumirDestino(1L);
        assertEquals(3, paris.getTotalAvaliacoes());
        assertEquals(resumoDestino.mediaArredondada(), paris.getAvaliacaoMedia());
        ResumoNotas resumoAtividade = repository.resumirAtividade(1L);
        assertEquals(3, torre.getTotalAvaliacoes());
        assertEquals(resumoAtividade.mediaArredondada(), torre.getAvaliacaoMedia());
    }
}
