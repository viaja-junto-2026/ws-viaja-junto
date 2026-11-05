package com.viajajunto.api.modules.review.seed;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.viajajunto.api.modules.activity.entity.CatalogoAtividadeEntity;
import com.viajajunto.api.modules.activity.repository.CatalogoAtividadeRepository;
import com.viajajunto.api.modules.auth.entity.UserEntity;
import com.viajajunto.api.modules.auth.repository.UserRepository;
import com.viajajunto.api.modules.destination.entity.DestinoCatalogoEntity;
import com.viajajunto.api.modules.destination.repository.DestinoCatalogoRepository;
import com.viajajunto.api.modules.review.entity.AvaliacaoEntity;
import com.viajajunto.api.modules.review.repository.AvaliacaoRepository;
import com.viajajunto.api.modules.review.repository.AvaliacaoRepository.ResumoNotas;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.data.domain.Sort;

@ExtendWith(MockitoExtension.class)
class AvaliacaoSeedRunnerTest {

    @Mock
    private AvaliacaoRepository avaliacaoRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private DestinoCatalogoRepository destinoCatalogoRepository;

    @Mock
    private CatalogoAtividadeRepository catalogoAtividadeRepository;

    @InjectMocks
    private AvaliacaoSeedRunner seedRunner;

    private DestinoCatalogoEntity paris;
    private CatalogoAtividadeEntity torre;

    @BeforeEach
    void setUp() {
        paris = DestinoCatalogoEntity.builder().id(1L).nome("Paris").build();
        torre = CatalogoAtividadeEntity.builder().id(1L).nome("Torre Eiffel").build();
    }

    private List<UserEntity> usuarios(int quantidade) {
        return java.util.stream.LongStream.rangeClosed(1, quantidade)
                .mapToObj(id -> UserEntity.builder().id(id).nome("Usuário " + id).email("u" + id + "@email.com").build())
                .toList();
    }

    @Test
    @DisplayName("Cria no máximo uma avaliação por usuário em cada destino e atividade, com ids fixos")
    void seedsOneReviewPerUserWithFixedIds() {
        when(userRepository.findAll(any(Sort.class))).thenReturn(usuarios(3));
        when(destinoCatalogoRepository.findAll()).thenReturn(List.of(paris));
        when(catalogoAtividadeRepository.findAll()).thenReturn(List.of(torre));
        when(avaliacaoRepository.resumirDestino(1L)).thenReturn(new ResumoNotas(3, 4.6667));
        when(avaliacaoRepository.resumirAtividade(1L)).thenReturn(new ResumoNotas(3, 4.0));

        seedRunner.run(new DefaultApplicationArguments());

        ArgumentCaptor<AvaliacaoEntity> captor = ArgumentCaptor.forClass(AvaliacaoEntity.class);
        verify(avaliacaoRepository, times(6)).save(captor.capture());
        Set<String> ids = captor.getAllValues().stream().map(AvaliacaoEntity::getId).collect(Collectors.toSet());
        assertEquals(
                Set.of("seed-destino-1-1", "seed-destino-1-2", "seed-destino-1-3",
                        "seed-atividade-1-1", "seed-atividade-1-2", "seed-atividade-1-3"),
                ids);
        captor.getAllValues().forEach(a -> assertTrue(a.getNota() >= 1 && a.getNota() <= 5));
        assertTrue(captor.getAllValues().stream()
                .filter(a -> a.getTargetKey().startsWith("DESTINO#"))
                .allMatch(a -> Long.valueOf(1L).equals(a.getDestinoCatalogoId()) && a.getCatalogoAtividadeId() == null));
        assertTrue(captor.getAllValues().stream()
                .filter(a -> a.getTargetKey().startsWith("ATIVIDADE#"))
                .allMatch(a -> Long.valueOf(1L).equals(a.getCatalogoAtividadeId()) && a.getDestinoCatalogoId() == null));
    }

    @Test
    @DisplayName("Rodar duas vezes grava exatamente os mesmos itens (mesmos ids e mesmo conteúdo)")
    void runningTwiceWritesTheSameItems() {
        when(userRepository.findAll(any(Sort.class))).thenReturn(usuarios(3));
        when(destinoCatalogoRepository.findAll()).thenReturn(List.of(paris));
        when(catalogoAtividadeRepository.findAll()).thenReturn(List.of(torre));
        when(avaliacaoRepository.resumirDestino(1L)).thenReturn(new ResumoNotas(3, 4.0));
        when(avaliacaoRepository.resumirAtividade(1L)).thenReturn(new ResumoNotas(3, 4.0));

        seedRunner.run(new DefaultApplicationArguments());
        seedRunner.run(new DefaultApplicationArguments());

        ArgumentCaptor<AvaliacaoEntity> captor = ArgumentCaptor.forClass(AvaliacaoEntity.class);
        verify(avaliacaoRepository, times(12)).save(captor.capture());
        List<AvaliacaoEntity> gravados = captor.getAllValues();
        // Mesmo id => o DynamoDB sobrescreve o item; mesmo conteúdo => nada muda entre execuções.
        for (int i = 0; i < 6; i++) {
            AvaliacaoEntity primeira = gravados.get(i);
            AvaliacaoEntity segunda = gravados.get(i + 6);
            assertEquals(primeira.getId(), segunda.getId());
            assertEquals(primeira.getNota(), segunda.getNota());
            assertEquals(primeira.getComentario(), segunda.getComentario());
            assertEquals(primeira.getDataCriacao(), segunda.getDataCriacao());
        }
    }

    @Test
    @DisplayName("Os contadores do catálogo vêm do resumo do DynamoDB, não de um número fixo")
    void countersComeFromDynamoSummary() {
        when(userRepository.findAll(any(Sort.class))).thenReturn(usuarios(3));
        when(destinoCatalogoRepository.findAll()).thenReturn(List.of(paris));
        when(catalogoAtividadeRepository.findAll()).thenReturn(List.of(torre));
        // Já havia 4 avaliações (3 de exemplo + 1 real): o contador reflete as 4.
        when(avaliacaoRepository.resumirDestino(1L)).thenReturn(new ResumoNotas(4, 4.25));
        when(avaliacaoRepository.resumirAtividade(1L)).thenReturn(new ResumoNotas(3, 4.0));

        seedRunner.run(new DefaultApplicationArguments());

        assertEquals(4, paris.getTotalAvaliacoes());
        assertEquals(4.3, paris.getAvaliacaoMedia());
        assertEquals(3, torre.getTotalAvaliacoes());
        assertEquals(4.0, torre.getAvaliacaoMedia());
        verify(destinoCatalogoRepository).save(paris);
        verify(catalogoAtividadeRepository).save(torre);
    }

    @Test
    @DisplayName("Com menos de 3 usuários, cria uma avaliação por usuário")
    void fewerUsersThanTheLimit() {
        when(userRepository.findAll(any(Sort.class))).thenReturn(usuarios(2));
        when(destinoCatalogoRepository.findAll()).thenReturn(List.of(paris));
        when(catalogoAtividadeRepository.findAll()).thenReturn(List.of());
        when(avaliacaoRepository.resumirDestino(1L)).thenReturn(new ResumoNotas(2, 4.5));

        seedRunner.run(new DefaultApplicationArguments());

        verify(avaliacaoRepository, times(2)).save(any(AvaliacaoEntity.class));
    }

    @Test
    @DisplayName("Sem usuários não grava nada")
    void noUsersDoesNothing() {
        when(userRepository.findAll(any(Sort.class))).thenReturn(List.of());

        seedRunner.run(new DefaultApplicationArguments());

        verify(avaliacaoRepository, never()).save(any(AvaliacaoEntity.class));
        verify(destinoCatalogoRepository, never()).save(any(DestinoCatalogoEntity.class));
    }

    @Test
    @DisplayName("Se o DynamoDB estiver fora do ar, a API sobe mesmo assim (o erro é logado)")
    void dynamoDownDoesNotBreakStartup() {
        when(userRepository.findAll(any(Sort.class))).thenReturn(usuarios(3));
        when(destinoCatalogoRepository.findAll()).thenReturn(List.of(paris));
        when(avaliacaoRepository.save(any(AvaliacaoEntity.class)))
                .thenThrow(new IllegalStateException("DynamoDB indisponível"));

        assertDoesNotThrow(() -> seedRunner.run(new DefaultApplicationArguments()));

        verify(destinoCatalogoRepository, never()).save(any(DestinoCatalogoEntity.class));
    }
}
