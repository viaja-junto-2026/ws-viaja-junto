package com.viajajunto.api.modules.review.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.viajajunto.api.core.exception.BusinessRuleException;
import com.viajajunto.api.core.exception.ResourceNotFoundException;
import com.viajajunto.api.modules.activity.entity.CatalogoAtividadeEntity;
import com.viajajunto.api.modules.activity.repository.CatalogoAtividadeRepository;
import com.viajajunto.api.modules.auth.entity.UserEntity;
import com.viajajunto.api.modules.auth.repository.UserRepository;
import com.viajajunto.api.modules.destination.entity.DestinoCatalogoEntity;
import com.viajajunto.api.modules.destination.repository.DestinoCatalogoRepository;
import com.viajajunto.api.modules.review.dto.AvaliacaoResponseDTO;
import com.viajajunto.api.modules.review.dto.CreateAvaliacaoDTO;
import com.viajajunto.api.modules.review.entity.AvaliacaoEntity;
import com.viajajunto.api.modules.review.repository.AvaliacaoRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

@ExtendWith(MockitoExtension.class)
class AvaliacaoServiceTest {

    @Mock
    private AvaliacaoRepository avaliacaoRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private DestinoCatalogoRepository destinoCatalogoRepository;

    @Mock
    private CatalogoAtividadeRepository catalogoAtividadeRepository;

    @InjectMocks
    private AvaliacaoService avaliacaoService;

    private UserEntity sampleUser;
    private DestinoCatalogoEntity sampleDestino;
    private CatalogoAtividadeEntity sampleAtividade;

    @BeforeEach
    void setUp() {
        sampleUser = UserEntity.builder()
                .id(1L)
                .nome("Maria")
                .email("maria@email.com")
                .avatarUrl("http://avatar.com/maria")
                .build();

        sampleDestino = DestinoCatalogoEntity.builder()
                .id(10L)
                .nome("Paris")
                .totalAvaliacoes(0)
                .avaliacaoMedia(0.0)
                .build();

        sampleAtividade = CatalogoAtividadeEntity.builder()
                .id(20L)
                .nome("Torre Eiffel")
                .totalAvaliacoes(0)
                .avaliacaoMedia(0.0)
                .build();
    }

    @Test
    @DisplayName("Deve criar avaliação para Destino e atualizar média no catálogo")
    void createAvaliacao_Destino_Success() {
        CreateAvaliacaoDTO dto = CreateAvaliacaoDTO.builder()
                .destinoCatalogoId(10L)
                .nota(5)
                .comentario("Incrível!")
                .build();

        when(userRepository.findById(1L)).thenReturn(Optional.of(sampleUser));
        when(destinoCatalogoRepository.findById(10L)).thenReturn(Optional.of(sampleDestino));
        when(avaliacaoRepository.save(any(AvaliacaoEntity.class))).thenAnswer(i -> i.getArgument(0));
        when(avaliacaoRepository.calculateAverageRatingForDestino(10L)).thenReturn(5.0);

        AvaliacaoResponseDTO response = avaliacaoService.createAvaliacao(dto, 1L);

        assertNotNull(response);
        assertEquals(5, response.getNota());
        assertEquals("Incrível!", response.getComentario());
        assertEquals(10L, response.getDestinoCatalogoId());
        assertEquals(1L, response.getUsuario().getId());

        verify(destinoCatalogoRepository).save(sampleDestino);
        assertEquals(1, sampleDestino.getTotalAvaliacoes());
        assertEquals(5.0, sampleDestino.getAvaliacaoMedia());
    }

    @Test
    @DisplayName("Deve criar avaliação para Atividade e atualizar média no catálogo")
    void createAvaliacao_Atividade_Success() {
        CreateAvaliacaoDTO dto = CreateAvaliacaoDTO.builder()
                .catalogoAtividadeId(20L)
                .nota(4)
                .comentario("Muito legal")
                .build();

        when(userRepository.findById(1L)).thenReturn(Optional.of(sampleUser));
        when(catalogoAtividadeRepository.findById(20L)).thenReturn(Optional.of(sampleAtividade));
        when(avaliacaoRepository.save(any(AvaliacaoEntity.class))).thenAnswer(i -> i.getArgument(0));
        when(avaliacaoRepository.calculateAverageRatingForAtividade(20L)).thenReturn(4.0);

        AvaliacaoResponseDTO response = avaliacaoService.createAvaliacao(dto, 1L);

        assertNotNull(response);
        assertEquals(4, response.getNota());
        assertEquals("Muito legal", response.getComentario());
        assertEquals(20L, response.getCatalogoAtividadeId());

        verify(catalogoAtividadeRepository).save(sampleAtividade);
        assertEquals(1, sampleAtividade.getTotalAvaliacoes());
        assertEquals(4.0, sampleAtividade.getAvaliacaoMedia());
    }

    @Test
    @DisplayName("Deve lançar exceção se usuário não existir ao criar avaliação")
    void createAvaliacao_UserNotFound() {
        CreateAvaliacaoDTO dto = CreateAvaliacaoDTO.builder().destinoCatalogoId(10L).nota(5).build();
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () ->
                avaliacaoService.createAvaliacao(dto, 99L)
        );
    }

    @Test
    @DisplayName("Deve lançar exceção se nenhum destino ou atividade for informado")
    void createAvaliacao_NoTarget() {
        CreateAvaliacaoDTO dto = CreateAvaliacaoDTO.builder().nota(5).build();
        when(userRepository.findById(1L)).thenReturn(Optional.of(sampleUser));

        assertThrows(BusinessRuleException.class, () ->
                avaliacaoService.createAvaliacao(dto, 1L)
        );
    }

    @Test
    @DisplayName("Deve listar avaliações por destino")
    void listAvaliacoesByDestino() {
        AvaliacaoEntity a1 = AvaliacaoEntity.builder()
                .targetKey("DESTINO#10")
                .id("av1")
                .usuarioId(1L)
                .usuarioNome("Maria")
                .destinoCatalogoId(10L)
                .nota(5)
                .dataCriacao("2026-10-05T10:00:00Z")
                .build();

        when(avaliacaoRepository.findAllByDestinoCatalogoId(10L)).thenReturn(List.of(a1));

        Page<AvaliacaoResponseDTO> page = avaliacaoService.listAvaliacoesByDestino(10L, PageRequest.of(0, 10));

        assertNotNull(page);
        assertEquals(1, page.getTotalElements());
        assertEquals(5, page.getContent().get(0).getNota());
    }

    @Test
    @DisplayName("Deve listar avaliações por atividade")
    void listAvaliacoesByAtividade() {
        AvaliacaoEntity a1 = AvaliacaoEntity.builder()
                .targetKey("ATIVIDADE#20")
                .id("av2")
                .usuarioId(1L)
                .usuarioNome("Maria")
                .catalogoAtividadeId(20L)
                .nota(4)
                .dataCriacao("2026-10-05T10:00:00Z")
                .build();

        when(avaliacaoRepository.findAllByCatalogoAtividadeId(20L)).thenReturn(List.of(a1));

        Page<AvaliacaoResponseDTO> page = avaliacaoService.listAvaliacoesByAtividade(20L, PageRequest.of(0, 10));

        assertNotNull(page);
        assertEquals(1, page.getTotalElements());
        assertEquals(4, page.getContent().get(0).getNota());
    }
}
