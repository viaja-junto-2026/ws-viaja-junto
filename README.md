# ✈️ ViajaJunto API

API RESTful para planejamento colaborativo de viagens em grupo. A plataforma permite aos viajantes organizar itinerários, gerenciar atividades com prevenção de conflitos de agenda, controlar orçamentos coletivos, consultar destinos e registrar avaliações.

---

## 🛠️ Tecnologias Utilizadas

- **Linguagem & Framework:** Java 17, Spring Boot 3.4.3
- **Persistência Relacional:** Spring Data JPA, Hibernate, H2 Database (em memória)
- **Persistência NoSQL:** AWS SDK v2 (DynamoDB Enhanced Client)
- **Segurança & Autenticação:** Spring Security como OAuth2 Resource Server, validando tokens do Firebase Authentication (a API não guarda senha nem emite token)
- **Documentação de API:** OpenAPI 3.0.3, Spectral Linter
- **Qualidade & Testes:** JUnit 5, Mockito, REST Assured, JaCoCo, Checkstyle, SonarQube
- **Infraestrutura Local:** Docker, Docker Compose, MiniStack (emulador AWS local), DynamoDB Admin

---

## 📋 Pré-requisitos

- **Java JDK 17** ou superior instalado
- **Docker** e **Docker Compose** instalados e em execução
- Git

> [!NOTE]
> Não é necessário ter o Maven instalado globalmente na máquina; o projeto inclui o Maven Wrapper (`mvnw` / `mvnw.cmd`).

---

## 🚀 Como Rodar a Aplicação

### Opção 1: Ambiente Completo via Docker Compose

Esta opção constrói a imagem da aplicação e sobe todos os serviços (MiniStack, DynamoDB Admin e API) em contêineres:

```bash
docker compose up --build
```

Para encerrar os serviços:
```bash
docker compose down
```

---

### Opção 2: Desenvolvimento Local (Spring Boot + Serviços Auxiliares)

Recomendada durante o desenvolvimento para recarregamento rápido e depuração:

1. **Suba apenas os serviços de infraestrutura local (MiniStack e DynamoDB Admin):**
   ```bash
   docker compose up -d ministack dynamodb-admin
   ```

2. **Execute a aplicação com o Maven Wrapper, usando o perfil `dev`** (ele traz um ID de projeto Firebase de desenvolvimento, as origens do CORS, o H2 console e as credenciais do emulador AWS para uso local; sem ele a aplicação não sobe, veja [Variáveis de Ambiente](#-variáveis-de-ambiente)):
   - **Linux / macOS:**
     ```bash
     ./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
     ```
   - **Windows (PowerShell ou Prompt de Comando):**
     ```powershell
     .\mvnw.cmd spring-boot:run "-Dspring-boot.run.profiles=dev"
     ```

---

## 🌐 URLs e Acessos Úteis

| Serviço | URL | Credenciais / Observações |
| :--- | :--- | :--- |
| **API Base** | `http://localhost:8080` | Porta padrão da aplicação |
| **Status / Health** | `http://localhost:8080/actuator/health` | Health Check via Spring Boot Actuator |
| **H2 Console** | `http://localhost:8080/h2-console` | Só no perfil `dev`.<br>**JDBC URL:** `jdbc:h2:mem:viajajuntodb`<br>**User:** `sa` \| **Password:** *(vazio)* |
| **DynamoDB Admin** | `http://localhost:8002` | Interface web para visualização de tabelas e itens |
| **MiniStack (AWS Local)** | `http://localhost:4566` | Emulador local com suporte a DynamoDB |

> [!TIP]
> No perfil `dev`, o banco relacional (H2) é inicializado com dados fictícios carregados do arquivo [`src/main/resources/data.sql`](src/main/resources/data.sql) (usuários, destinos turísticos, atividades e viagens). As **avaliações** ficam no DynamoDB: ao subir, o `AvaliacaoSeedRunner` cria avaliações de exemplo (sem duplicar se rodar de novo) e recalcula média e total de cada destino e atividade a partir delas. Sem o MiniStack no ar, a API sobe normalmente, mas sem esses dados de avaliação.

---

## ⚙️ Perfis de Execução (Spring Profiles)

A aplicação suporta múltiplos perfis de configuração:

- **`default` (Padrão, sem perfil ativo):**
  - Configuração segura, sem valores de desenvolvimento: exige as variáveis de ambiente da seção [Variáveis de Ambiente](#-variáveis-de-ambiente) e não sobe sem `FIREBASE_PROJECT_ID`.
  - Mantém o H2 console desligado e não permite nenhuma origem externa no CORS.
  - Usa o banco H2 em memória, recriado a cada execução (`hibernate.ddl-auto: create-drop`), **sem** dados de exemplo (`spring.sql.init.mode: never`).

- **`dev` (Desenvolvimento local):**
  - Ative com `SPRING_PROFILES_ACTIVE=dev`. **Nunca use em produção.**
  - Liga o H2 console em `/h2-console` (somente neste perfil).
  - Carrega o `data.sql` (`spring.sql.init.mode: always`) e semeia as avaliações de exemplo no DynamoDB.
  - Usa o ID de projeto Firebase `viajajunto-dev` (troque com `FIREBASE_PROJECT_ID`), libera o CORS para `localhost:3000`, `5173` e `4200` e aponta para o DynamoDB local no MiniStack (`http://localhost:4566`) com credenciais de mentira.
  - Os 3 usuários de exemplo têm `firebase_uid` fixo (`dev-firebase-uid-ana`, `dev-firebase-uid-carlos` e `dev-firebase-uid-beatriz`), usado pelos testes para gerar tokens.
  - É o perfil usado pelo `docker-compose.yml`.

- **Testes automatizados:**
  - Usam portas dinâmicas e REST Assured nos testes de integração, e um token Firebase simulado, assinado com uma chave de teste (nada de internet nem de atalhos de autenticação). O decodificador de teste aplica as mesmas regras da produção.
  - O `SecurityDevProfileTest` roda no perfil `dev`, com um banco H2 só dele.

### Como alternar perfis manualmente

Via argumento de linha de comando:
```bash
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
```

Ou definindo a variável de ambiente:
```bash
export SPRING_PROFILES_ACTIVE=dev
```

---

## 🔐 Variáveis de Ambiente

Nenhum segredo fica no repositório (ele é público): chaves e credenciais entram só por variável de ambiente. Fora do perfil `dev`, as obrigatórias precisam estar definidas, ou a aplicação não sobe.

| Variável | Obrigatória? | Para que serve |
| :--- | :--- | :--- |
| `FIREBASE_PROJECT_ID` | **Sim**, exceto no perfil `dev` | ID do projeto no Firebase Authentication. A API só aceita tokens emitidos para este projeto (emissor `https://securetoken.google.com/<id>` e audiência `<id>`). O perfil `dev` usa `viajajunto-dev`. |
| `AWS_ACCESS_KEY_ID` | **Sim**, exceto no perfil `dev` | Credencial de acesso ao DynamoDB. |
| `AWS_SECRET_ACCESS_KEY` | **Sim**, exceto no perfil `dev` | Credencial secreta do DynamoDB. |
| `AWS_REGION` | Não (padrão `us-east-1`) | Região do DynamoDB. |
| `AWS_DYNAMODB_ENDPOINT` | Não | Endpoint do DynamoDB. Vazio usa o DynamoDB real da AWS; o perfil `dev` aponta para o MiniStack em `http://localhost:4566`. |
| `CORS_ALLOWED_ORIGINS` | Não | Origens permitidas, separadas por vírgula. Vazio = nenhuma origem externa. O perfil `dev` libera `localhost:3000`, `5173` e `4200`. |
| `GOOGLE_PLACES_API_KEY` | Não | Chave da API do Google Places. Vazia, a busca de lugares devolve resposta vazia. |
| `SPRING_PROFILES_ACTIVE` | Não | Perfil ativo (`dev`). Sem ele, vale o perfil `default`. |

Exemplo para rodar fora do perfil `dev`:
```bash
export FIREBASE_PROJECT_ID="<id-do-projeto-firebase>"
export AWS_ACCESS_KEY_ID="<sua-chave>"
export AWS_SECRET_ACCESS_KEY="<seu-segredo>"
export CORS_ALLOWED_ORIGINS="https://app.exemplo.com"
./mvnw spring-boot:run
```

---

## 🧪 Como Rodar os Testes

O projeto conta com testes unitários isolados com Mockito e testes de contrato ponta a ponta com REST Assured.

### Executar todos os testes
- **Linux / macOS:**
  ```bash
  ./mvnw test
  ```
- **Windows:**
  ```powershell
  .\mvnw.cmd test
  ```

> [!NOTE]
> O `AvaliacaoRepositoryTest` usa um DynamoDB de verdade (MiniStack em contêiner, via Testcontainers) e **precisa do Docker no ar**. Sem o Docker, esses testes são ignorados (aparecem como *Skipped*) e o restante roda normalmente.

### Executar uma classe de teste específica
```bash
./mvnw test -Dtest=ViagemServiceTest
```

### Gerar Relatório de Cobertura de Código (JaCoCo)
```bash
./mvnw clean test jacoco:report
```
O relatório HTML completo será gerado em:
`target/site/jacoco/index.html`

### Cobertura mínima (`mvn verify`)
O `./mvnw verify` roda os testes, gera o relatório e **reprova o build** se a cobertura do projeto cair abaixo de **85% das linhas** ou **60% dos branches** (regra no `pom.xml`, plugin JaCoCo).

No CI, o job **Tests and Coverage** roda `mvn verify` a cada PR, falha se algum teste quebrar ou ficar ignorado (no CI o Docker está disponível, então nenhum teste é pulado), mostra a cobertura no resumo do job e publica os artefatos `jacoco-report` e `surefire-reports`.

### Lint (Checkstyle)
As regras estão em [`config/checkstyle/checkstyle.xml`](config/checkstyle/checkstyle.xml) e todas têm severidade **error**: qualquer violação reprova o build. Elas cobrem imports não usados, chaves obrigatórias, comparações de `String`, `switch` sem `default`, nomes e espaços no fim da linha (formatação como indentação fica de fora). O código de testes também é verificado.
```bash
./mvnw checkstyle:check
```
O mesmo comando roda no CI, no job **Lint (Checkstyle)**, a cada PR. Se falhar, o relatório fica como artefato do job (`checkstyle-report`).

---

## 📖 Documentação da API (OpenAPI e Swagger)

Há dois documentos, com papéis diferentes:

| Documento | Papel | Onde |
| :--- | :--- | :--- |
| **Contrato** (escrito à mão) | **Fonte da verdade**: o que a API deve ter | [`docs/viajajunto-openapi.json`](docs/viajajunto-openapi.json) |
| **Spec gerada** (springdoc) | **Reflexo do código**: o que a API tem hoje | `/v3/api-docs` e Swagger UI |

- **Validação do contrato:** o workflow *Lint OpenAPI Contract* roda o Spectral sobre `docs/viajajunto-openapi.json` em todo push e PR. Localmente: `npx @stoplight/spectral-cli lint docs/viajajunto-openapi.json`.
- **Swagger UI (somente perfil `dev`):** com `SPRING_PROFILES_ACTIVE=dev` (ou `docker compose up`), abra <http://localhost:8080/swagger-ui.html>. Fora do `dev` o Swagger e `/v3/api-docs` ficam desligados. Para testar rotas protegidas use o botão **Authorize** com um ID token do Firebase.
- **Diferenças entre contrato e código:** veja [`docs/rotas-contrato-vs-implementacao.md`](docs/rotas-contrato-vs-implementacao.md), com o que falta ou difere em cada rota.

---

## 📌 Principais Rotas da API (Padrão OpenAPI)

As rotas e recursos da API seguem a especificação OpenAPI 3.0.3 v1.0.0 ([`docs/viajajunto-openapi.json`](docs/viajajunto-openapi.json)):

### Usuários (`/users`)
Cadastro, login, social, renovação de sessão e recuperação de senha são do **Firebase Authentication**, feitos direto pelo app cliente: a API não tem essas rotas. Ela recebe o token do Firebase no cabeçalho `Authorization: Bearer <token>` e, no primeiro acesso de um usuário, cria a conta dele a partir do token (ou vincula a um usuário existente com o mesmo e-mail verificado). O e-mail do token precisa estar verificado.

| Método | Endpoint | Descrição | Autenticação |
| :--- | :--- | :--- | :---: |
| `GET` | `/users/me` | Dados do perfil do usuário autenticado | Bearer (token Firebase) |
| `PATCH` | `/users/me` | Altera nome e/ou avatar (só o que for enviado) | Bearer (token Firebase) |

### Viagens & Roteiros (`/trips`)
| Método | Endpoint | Descrição | Resposta |
| :--- | :--- | :--- | :--- |
| `GET` | `/trips` | Listagem de viagens do usuário logado | Paginada (`{data, meta}`) |
| `POST` | `/trips` | Criação de novo roteiro | Detalhes da viagem |
| `GET` | `/trips/{id}` | Consulta de viagem por ID | Detalhes da viagem |
| `PUT` | `/trips/{id}` | Atualização de dados da viagem | Detalhes atualizados |
| `DELETE` | `/trips/{id}` | Exclusão de viagem | `204 No Content` |

### Membros da Viagem (`/trips/{tripId}/members`)
| Método | Endpoint | Descrição |
| :--- | :--- | :--- |
| `GET` | `/trips/{tripId}/members` | Listagem paginada de membros participantes |
| `POST` | `/trips/{tripId}/members/join` | Ingresso na viagem através de código de convite |
| `PATCH` | `/trips/{tripId}/members/{memberId}` | Atualização do nível de permissão do membro |
| `DELETE` | `/trips/{tripId}/members/{memberId}` | Remoção de membro da viagem |

### Itinerário & Atividades (`/trips/{tripId}/activities`)
| Método | Endpoint | Descrição |
| :--- | :--- | :--- |
| `GET` | `/trips/{tripId}/activities` | Listagem paginada de atividades agendadas |
| `POST` | `/trips/{tripId}/destinations/{destinationId}/activities` | Adiciona atividade prevenindo conflitos de horário |
| `DELETE` | `/trips/{tripId}/activities/{activityId}` | Remove atividade do itinerário |

### Orçamento & Finanças (`/trips/{tripId}/budget`)
| Método | Endpoint | Descrição |
| :--- | :--- | :--- |
| `GET` | `/trips/{tripId}/budget` | Resumo financeiro consolidado da viagem |
| `PUT` | `/trips/{tripId}/budget` | Atualização do teto orçamentário total |

### Lugares & Avaliações da Comunidade (`/places`)
| Método | Endpoint | Descrição | Autenticação |
| :--- | :--- | :--- | :---: |
| `GET` | `/places` | Catálogo paginado com busca e filtros | Pública |
| `GET` | `/places/{id}` | Detalhes do destino selecionado | Pública |
| `GET` | `/places/featured` | Destinos com melhores avaliações | Pública |
| `GET` | `/places/{placeId}/reviews` | Lista paginada de avaliações do destino | Pública |
| `POST` | `/places/{placeId}/reviews` | Criação de avaliação (persistência DynamoDB) | Bearer (token Firebase) |

### Catálogo de Atividades (`/activities/catalog`)
| Método | Endpoint | Descrição | Autenticação |
| :--- | :--- | :--- | :---: |
| `GET` | `/activities/catalog` | Busca de atividades e passeios turísticos | Pública |
| `GET` | `/activities/catalog/{id}` | Consulta de atividade do catálogo por ID | Pública |
| `GET` | `/activities/catalog/featured` | Atividades em destaque | Pública |

---

## 📁 Estrutura de Pacotes

O projeto segue arquitetura modular por funcionalidades (Package-by-Feature):

```text
src/main/java/com/viajajunto/api/
├── core/                                # Componentes transversais e infraestrutura
│   ├── config/                          # Configurações Spring (Security, DynamoDB, CORS, Beans)
│   ├── dto/                             # DTOs globais e envoltórios (ex: paginação PagedResponse e PageMeta)
│   ├── exception/                       # Tratamento centralizado de erros (@RestControllerAdvice)
│   └── security/                        # Segurança (validação do token Firebase, usuário autenticado)
│
├── integrations/                        # Clientes de APIs externas
│   └── google/                          # Integração com Google Places API
│
└── modules/                             # Módulos de domínio de negócio
    ├── activity/                        # Atividades e passeios do itinerário
    ├── auth/                            # Autenticação, usuários e perfil
    ├── budget/                          # Orçamentos e finanças da viagem
    ├── destination/                     # Destinos e catálogo de lugares
    ├── review/                          # Avaliações (DynamoDB NoSQL)
    └── trip/                            # Gestão central de viagens e membros
```
