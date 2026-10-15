-- ====================================================================
-- DADOS FAKE INICIAIS DO BANCO RELACIONAL (H2) - VIAJA JUNTO API
-- ====================================================================

-- 1. USUÁRIOS (users)
-- Senha de todos os usuários de teste: "123456" (hash BCrypt abaixo)
INSERT INTO users (id, nome, email, senha, avatar_url, data_criacao) 
VALUES (1, 'Ana Silva', 'ana.silva@email.com', '$2a$10$dXJ3SW6G7P50lGmMkkmwe.20cQQubK3.85UXy3d0R1N0UvO8L0q4a', 'https://images.unsplash.com/photo-1494790108377-be9c29b29330', CURRENT_TIMESTAMP());

INSERT INTO users (id, nome, email, senha, avatar_url, data_criacao) 
VALUES (2, 'Carlos Eduardo', 'carlos.eduardo@email.com', '$2a$10$dXJ3SW6G7P50lGmMkkmwe.20cQQubK3.85UXy3d0R1N0UvO8L0q4a', 'https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d', CURRENT_TIMESTAMP());

INSERT INTO users (id, nome, email, senha, avatar_url, data_criacao) 
VALUES (3, 'Beatriz Souza', 'beatriz.souza@email.com', '$2a$10$dXJ3SW6G7P50lGmMkkmwe.20cQQubK3.85UXy3d0R1N0UvO8L0q4a', 'https://images.unsplash.com/photo-1438761681033-6461ffad8d80', CURRENT_TIMESTAMP());


-- 2. CATÁLOGO DE DESTINOS (destinos_catalogo)
INSERT INTO destinos_catalogo (id, nome, pais, codigo_pais_iso, cidade, estado, descricao, categoria, foto_url, avaliacao_media, total_avaliacoes) 
VALUES (1, 'Paris', 'França', 'FRA', 'Paris', 'Île-de-France', 'A incrível Cidade Luz com a Torre Eiffel e museu do Louvre.', 'cultural', 'https://images.unsplash.com/photo-1502602898657-3e91760cbb34', 4.8, 12);

INSERT INTO destinos_catalogo (id, nome, pais, codigo_pais_iso, cidade, estado, descricao, categoria, foto_url, avaliacao_media, total_avaliacoes) 
VALUES (2, 'Rio de Janeiro', 'Brasil', 'BRA', 'Rio de Janeiro', 'RJ', 'Cidade Maravilhosa repleta de praias icônicas e o Cristo Redentor.', 'praia', 'https://images.unsplash.com/photo-1483729558449-99ef09a8c325', 4.6, 8);

INSERT INTO destinos_catalogo (id, nome, pais, codigo_pais_iso, cidade, estado, descricao, categoria, foto_url, avaliacao_media, total_avaliacoes) 
VALUES (3, 'Tóquio', 'Japão', 'JPN', 'Tóquio', 'Kanto', 'Metrópole vibrante mesclando tecnologia futurista e tradição millenar.', 'cidade', 'https://images.unsplash.com/photo-1540959733332-eab4deabeeaf', 4.9, 15);

INSERT INTO destinos_catalogo (id, nome, pais, codigo_pais_iso, cidade, estado, descricao, categoria, foto_url, avaliacao_media, total_avaliacoes) 
VALUES (4, 'Bariloche', 'Argentina', 'ARG', 'San Carlos de Bariloche', 'Río Negro', 'Famosa por suas montanhas com neve, lagos cristalinos e chocolates.', 'natureza', 'https://images.unsplash.com/photo-1544644181-1484b3fdfc62', 4.5, 5);


-- 3. CATÁLOGO DE ATIVIDADES (catalogo_atividades)
INSERT INTO catalogo_atividades (id, nome, tipo, localizacao, cidade, pais, descricao, foto_url, preco_medio, avaliacao_media, total_avaliacoes)
VALUES (1, 'Visita à Torre Eiffel', 'Passeio', 'Champ de Mars', 'Paris', 'França', 'Ingresso com acesso ao topo da famosa estrutura de ferro.', 'https://images.unsplash.com/photo-1511739001486-6bfe10ce785f', 150.00, 4.9, 20);

INSERT INTO catalogo_atividades (id, nome, tipo, localizacao, cidade, pais, descricao, foto_url, preco_medio, avaliacao_media, total_avaliacoes)
VALUES (2, 'Passeio de Bondinho no Pão de Açúcar', 'Passeio', 'Urca', 'Rio de Janeiro', 'Brasil', 'Teleférico com vista panorâmica espetacular da baía de Guanabara.', 'https://images.unsplash.com/photo-1516306580123-e6e52b1b7b5f', 120.00, 4.7, 10);

INSERT INTO catalogo_atividades (id, nome, tipo, localizacao, cidade, pais, descricao, foto_url, preco_medio, avaliacao_media, total_avaliacoes)
VALUES (3, 'Jantar Tradicional de Lamen em Shibuya', 'Gastronomia', 'Shibuya Crossing', 'Tóquio', 'Japão', 'Experiência gastronômica autêntica nos becos de Tóquio.', 'https://images.unsplash.com/photo-1569718212165-3a8278d5f624', 60.00, 4.8, 14);

INSERT INTO catalogo_atividades (id, nome, tipo, localizacao, cidade, pais, descricao, foto_url, preco_medio, avaliacao_media, total_avaliacoes)
VALUES (4, 'Aula de Esqui Cerro Catedral', 'Passeio', 'Cerro Catedral', 'Bariloche', 'Argentina', 'Aula prática de esqui na neve com equipamento incluso.', 'https://images.unsplash.com/photo-1551698618-1dfe5d97d256', 300.00, 4.6, 6);


-- 4. VIAGENS (viagens)
INSERT INTO viagens (id, nome, descricao, data_inicio, data_fim, status, codigo_convite, criador_id, data_criacao, data_atualizacao)
VALUES (1, 'Férias na Europa 2026', 'Mochilão cultural pela Europa explorando Paris e arredores.', '2026-11-10', '2026-11-20', 'EM_PLANEJAMENTO', 'EUR2026CODE1', 1, CURRENT_TIMESTAMP(), CURRENT_TIMESTAMP());

INSERT INTO viagens (id, nome, descricao, data_inicio, data_fim, status, codigo_convite, criador_id, data_criacao, data_atualizacao)
VALUES (2, 'Ano Novo no Rio', 'Celebração de réveillon com amigos nas praias de Copacabana.', '2026-12-28', '2027-01-03', 'CONFIRMADA', 'RIO2027CODE2', 2, CURRENT_TIMESTAMP(), CURRENT_TIMESTAMP());


-- 5. MEMBROS DA VIAGEM (membros_viagem)
INSERT INTO membros_viagem (id, viagem_id, usuario_id, permissao, data_entrada)
VALUES (1, 1, 1, 'CRIADOR', CURRENT_TIMESTAMP());

INSERT INTO membros_viagem (id, viagem_id, usuario_id, permissao, data_entrada)
VALUES (2, 1, 3, 'EDITOR', CURRENT_TIMESTAMP());

INSERT INTO membros_viagem (id, viagem_id, usuario_id, permissao, data_entrada)
VALUES (3, 2, 2, 'CRIADOR', CURRENT_TIMESTAMP());

INSERT INTO membros_viagem (id, viagem_id, usuario_id, permissao, data_entrada)
VALUES (4, 2, 1, 'VISUALIZADOR', CURRENT_TIMESTAMP());


-- 6. DESTINOS DA VIAGEM (destinos_viagem)
INSERT INTO destinos_viagem (id, viagem_id, destino_catalogo_id, nome, pais, codigo_pais_iso, localizacao, foto_url, descricao, categoria, data_chegada, data_saida, ordem_visita)
VALUES (1, 1, 1, 'Paris', 'França', 'FRA', 'Centro de Paris', 'https://images.unsplash.com/photo-1502602898657-3e91760cbb34', 'Parada principal do itinerário.', 'cultural', '2026-11-10', '2026-11-16', 1);

INSERT INTO destinos_viagem (id, viagem_id, destino_catalogo_id, nome, pais, codigo_pais_iso, localizacao, foto_url, descricao, categoria, data_chegada, data_saida, ordem_visita)
VALUES (2, 2, 2, 'Rio de Janeiro', 'Brasil', 'BRA', 'Copacabana', 'https://images.unsplash.com/photo-1483729558449-99ef09a8c325', 'Estadia em hotel frente mar.', 'praia', '2026-12-28', '2027-01-03', 1);


-- 7. ATIVIDADES DA VIAGEM (atividades_viagem)
INSERT INTO atividades_viagem (id, destino_viagem_id, catalogo_atividade_id, nome, tipo, local, foto_url, descricao, data_horario, duracao_minutos, custo_previsto, status)
VALUES (1, 1, 1, 'Subir na Torre Eiffel', 'Passeio', 'Torre Eiffel', 'https://images.unsplash.com/photo-1511739001486-6bfe10ce785f', 'Visita no fim da tarde para ver o pôr do sol.', '2026-11-12T16:00:00', 120, 150.00, 'CONFIRMADA');

INSERT INTO atividades_viagem (id, destino_viagem_id, catalogo_atividade_id, nome, tipo, local, foto_url, descricao, data_horario, duracao_minutos, custo_previsto, status)
VALUES (2, 2, 2, 'Bondinho Pão de Açúcar', 'Passeio', 'Morro da Urca', 'https://images.unsplash.com/photo-1516306580123-e6e52b1b7b5f', 'Passeio de bondinho pela manhã.', '2026-12-29T10:00:00', 180, 120.00, 'PENDENTE');


-- 8. ORÇAMENTOS (orcamentos)
INSERT INTO orcamentos (id, viagem_id, orcamento_total)
VALUES (1, 1, 15000.00);

INSERT INTO orcamentos (id, viagem_id, orcamento_total)
VALUES (2, 2, 5000.00);


-- ====================================================================
-- ATUALIZAR RESTART DAS SEQUÊNCIAS AUTO_INCREMENT DO H2
-- Impede colisão de IDs quando novos registros são salvos via API/JPA
-- ====================================================================
ALTER TABLE users ALTER COLUMN id RESTART WITH 10;
ALTER TABLE destinos_catalogo ALTER COLUMN id RESTART WITH 10;
ALTER TABLE catalogo_atividades ALTER COLUMN id RESTART WITH 10;
ALTER TABLE viagens ALTER COLUMN id RESTART WITH 10;
ALTER TABLE membros_viagem ALTER COLUMN id RESTART WITH 10;
ALTER TABLE destinos_viagem ALTER COLUMN id RESTART WITH 10;
ALTER TABLE atividades_viagem ALTER COLUMN id RESTART WITH 10;
ALTER TABLE orcamentos ALTER COLUMN id RESTART WITH 10;
