package com.viajajunto.api.modules.review.seed;

import com.viajajunto.api.modules.activity.entity.CatalogoAtividadeEntity;
import com.viajajunto.api.modules.activity.repository.CatalogoAtividadeRepository;
import com.viajajunto.api.modules.auth.entity.UserEntity;
import com.viajajunto.api.modules.auth.repository.UserRepository;
import com.viajajunto.api.modules.destination.entity.DestinoCatalogoEntity;
import com.viajajunto.api.modules.destination.repository.DestinoCatalogoRepository;
import com.viajajunto.api.modules.review.entity.AvaliacaoEntity;
import com.viajajunto.api.modules.review.repository.AvaliacaoRepository;
import com.viajajunto.api.modules.review.repository.AvaliacaoRepository.ResumoNotas;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Cria avaliações de exemplo no DynamoDB para os destinos e atividades do catálogo, somente no
 * perfil "dev".
 *
 * <p>Idempotente: cada avaliação tem um id fixo ({@code seed-<alvo>-<id>-<usuário>}) e uma data
 * fixa, então rodar de novo regrava os mesmos itens em vez de duplicá-los. Ao final, o total e a
 * média do catálogo (H2) são recalculados a partir do DynamoDB, que é a fonte das avaliações;
 * assim os contadores sempre batem com as avaliações existentes, mesmo quando o DynamoDB guarda
 * dados de uma execução anterior e o H2 (em memória) recomeçou do zero.
 */
@Slf4j
@Component
@Profile("dev")
@RequiredArgsConstructor
public class AvaliacaoSeedRunner implements ApplicationRunner {

    private record ModeloAvaliacao(int nota, String comentario) {
    }

    private static final List<ModeloAvaliacao> MODELOS_DESTINO = List.of(
            new ModeloAvaliacao(5, "Viagem dos sonhos! Superou todas as expectativas."),
            new ModeloAvaliacao(4, "Lugar incrível, só achei um pouco cheio na alta temporada."),
            new ModeloAvaliacao(5, "Voltaria amanhã mesmo: comida, passeios e gente maravilhosos."),
            new ModeloAvaliacao(3, "Bonito, mas precisa de mais dias do que eu tinha planejado."),
            new ModeloAvaliacao(4, "Ótima experiência e boa estrutura para quem viaja em grupo."));

    private static final List<ModeloAvaliacao> MODELOS_ATIVIDADE = List.of(
            new ModeloAvaliacao(5, "Vale cada centavo, uma das melhores partes da viagem."),
            new ModeloAvaliacao(4, "Muito boa, recomendo chegar cedo para evitar fila."),
            new ModeloAvaliacao(5, "Organização perfeita e guias atenciosos."),
            new ModeloAvaliacao(3, "Legal, mas achei o preço um pouco acima do esperado."),
            new ModeloAvaliacao(4, "Divertida e bem conduzida, boa para o grupo todo."));

    /** Avaliações de exemplo por alvo: no máximo uma por usuário, como na regra do serviço. */
    private static final int MAX_AVALIACOES_POR_ALVO = 3;

    private static final Instant DATA_BASE = Instant.parse("2026-01-10T12:00:00Z");

    private final AvaliacaoRepository avaliacaoRepository;
    private final UserRepository userRepository;
    private final DestinoCatalogoRepository destinoCatalogoRepository;
    private final CatalogoAtividadeRepository catalogoAtividadeRepository;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        List<UserEntity> usuarios = userRepository.findAll(Sort.by("id"));
        if (usuarios.isEmpty()) {
            log.warn("Seed de avaliações ignorado: não há usuários cadastrados.");
            return;
        }

        try {
            destinoCatalogoRepository.findAll().forEach(destino -> semearDestino(destino, usuarios));
            catalogoAtividadeRepository.findAll().forEach(atividade -> semearAtividade(atividade, usuarios));
            log.info("Seed de avaliações do DynamoDB concluído.");
        } catch (RuntimeException e) {
            // Em dev a API deve subir mesmo sem o DynamoDB (MiniStack) no ar; o erro já foi registrado.
            log.error("Seed de avaliações não executado: {}", e.getMessage());
        }
    }

    private void semearDestino(DestinoCatalogoEntity destino, List<UserEntity> usuarios) {
        String targetKey = "DESTINO#" + destino.getId();
        String prefixo = "seed-destino-" + destino.getId();
        int total = Math.min(usuarios.size(), MAX_AVALIACOES_POR_ALVO);
        for (int i = 0; i < total; i++) {
            AvaliacaoEntity avaliacao = montar(targetKey, prefixo, destino.getId(), i, usuarios.get(i), MODELOS_DESTINO);
            avaliacao.setDestinoCatalogoId(destino.getId());
            avaliacaoRepository.save(avaliacao);
        }

        ResumoNotas resumo = avaliacaoRepository.resumirDestino(destino.getId());
        destino.setAvaliacaoMedia(resumo.mediaArredondada());
        destino.setTotalAvaliacoes(resumo.total());
        destinoCatalogoRepository.save(destino);
    }

    private void semearAtividade(CatalogoAtividadeEntity atividade, List<UserEntity> usuarios) {
        String targetKey = "ATIVIDADE#" + atividade.getId();
        String prefixo = "seed-atividade-" + atividade.getId();
        int total = Math.min(usuarios.size(), MAX_AVALIACOES_POR_ALVO);
        for (int i = 0; i < total; i++) {
            AvaliacaoEntity avaliacao = montar(targetKey, prefixo, atividade.getId(), i, usuarios.get(i), MODELOS_ATIVIDADE);
            avaliacao.setCatalogoAtividadeId(atividade.getId());
            avaliacaoRepository.save(avaliacao);
        }

        ResumoNotas resumo = avaliacaoRepository.resumirAtividade(atividade.getId());
        atividade.setAvaliacaoMedia(resumo.mediaArredondada());
        atividade.setTotalAvaliacoes(resumo.total());
        catalogoAtividadeRepository.save(atividade);
    }

    private AvaliacaoEntity montar(
            String targetKey,
            String prefixoId,
            Long alvoId,
            int indice,
            UserEntity usuario,
            List<ModeloAvaliacao> modelos
    ) {
        // O modelo varia por alvo e por usuário, mas é sempre o mesmo para o mesmo par (idempotência).
        ModeloAvaliacao modelo = modelos.get((int) ((alvoId + indice) % modelos.size()));
        return AvaliacaoEntity.builder()
                .targetKey(targetKey)
                .id(prefixoId + "-" + usuario.getId())
                .usuarioId(usuario.getId())
                .usuarioNome(usuario.getNome())
                .usuarioEmail(usuario.getEmail())
                .usuarioAvatarUrl(usuario.getAvatarUrl())
                .nota(modelo.nota())
                .comentario(modelo.comentario())
                .dataCriacao(DATA_BASE.plus(Duration.ofDays(alvoId * 3 + indice)).toString())
                .build();
    }
}
