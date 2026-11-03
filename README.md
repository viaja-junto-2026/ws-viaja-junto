# ✈️ ViajaJunto API

API RESTful para planejamento colaborativo de viagens em grupo. A plataforma permite aos viajantes organizar itinerários, gerenciar atividades com prevenção de conflitos de agenda, controlar orçamentos coletivos, consultar destinos e registrar avaliações.

---

## 🛠️ Tecnologias Utilizadas

- **Linguagem & Framework:** Java 17, Spring Boot 3.4.3
- **Persistência Relacional:** Spring Data JPA, Hibernate, H2 Database (em memória)
- **Persistência NoSQL:** AWS SDK v2 (DynamoDB Enhanced Client)
- **Segurança & Autenticação:** Spring Security, JWT (jjwt 0.12.6), BCrypt
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

2. **Execute a aplicação com o Maven Wrapper:**
   - **Linux / macOS:**
     ```bash
     ./mvnw spring-boot:run
     ```
   - **Windows (PowerShell ou Prompt de Comando):**
     ```powershell
     .\mvnw.cmd spring-boot:run
     ```

---

## 🌐 URLs e Acessos Úteis

| Serviço | URL | Credenciais / Observações |
| :--- | :--- | :--- |
| **API Base** | `http://localhost:8080` | Porta padrão da aplicação |
| **Status / Health** | `http://localhost:8080/api/v1/status` | Verificação de disponibilidade |
| **H2 Console** | `http://localhost:8080/h2-console` | **JDBC URL:** `jdbc:h2:mem:viajajuntodb`<br>**User:** `sa` \| **Password:** *(vazio)* |
| **DynamoDB Admin** | `http://localhost:8002` | Interface web para gerenciar tabelas do DynamoDB |
| **MiniStack (AWS)** | `http://localhost:4566` | Endpoint local para serviços AWS |

> [!TIP]
> O banco de dados relacional (H2) é inicializado automaticamente com dados fictícios de teste carregados a partir do arquivo [`src/main/resources/import.sql`](src/main/resources/import.sql) (usuários, destinos turísticos, atividades e viagens).

---

## ⚙️ Perfis de Execução (Spring Profiles)

A aplicação suporta múltiplos perfis de configuração:

- **`default` (Padrão):**
  - Utilizado no desenvolvimento local.
  - Carrega o banco H2 em memória com recriação de schema (`hibernate.ddl-auto: update`).
  - Executa a carga inicial do `import.sql`.
  - Aponta para o DynamoDB local no MiniStack (`http://localhost:4566`).

- **`test` (Testes Automatizados):**
  - Ativado automaticamente nas suítes de testes (`@ActiveProfiles("test")`).
  - Configura portas dinâmicas para testes de integração com REST Assured.
  - Habilita atalhos seguros de autenticação controlada para testes de contrato no `JwtAuthFilter`.

### Como alternar perfis manualmente

Via argumento de linha de comando:
```bash
./mvnw spring-boot:run -Dspring-boot.run.profiles=test
```

Ou definindo a variável de ambiente:
```bash
export SPRING_PROFILES_ACTIVE=test
```

---

## 🧪 Como Rodar os Testes

O projeto conta com suítes de testes unitários isolados com Mockito e testes de contrato ponta a ponta com REST Assured.

### Executar todos os testes
- **Linux / macOS:**
  ```bash
  ./mvnw test
  ```
- **Windows:**
  ```powershell
  .\mvnw.cmd test
  ```

### Executar uma classe de teste específica
```bash
./mvnw test -Dtest=ViagemServiceTest
```

### Executar um método de teste específico
```bash
./mvnw test -Dtest=ViagemServiceTest#deveCriarViagemComSucesso
```

### Gerar Relatório de Cobertura de Código (JaCoCo)
```bash
./mvnw clean test jacoco:report
```
O relatório HTML completo será gerado em:
`target/site/jacoco/index.html`

---

## 📁 Estrutura de Pacotes

O código está estruturado em uma arquitetura modular orientada a domínios (Package-by-Feature), separando as configurações transversais das regras de negócio:

```text
src/main/java/com/viajajunto/api/
├── core/                                # Componentes transversais e infraestrutura
│   ├── config/                          # Configurações Spring (Security, DynamoDB, CORS, Beans)
│   ├── dto/                             # DTOs globais e envoltórios (ex: paginação e erros)
│   ├── exception/                       # Tratamento centralizado de exceções (@ControllerAdvice)
│   └── security/                        # Implementação de segurança JWT (filtros, provedores, tokens)
│
├── integrations/                        # Clientes para serviços externos
│   └── google/                          # Integração com APIs externas (Google Places)
│
└── modules/                             # Módulos de domínio desacoplados
    ├── activity/                        # Gestão de atividades e passeios do itinerário
    │   ├── controller/
    │   ├── dto/
    │   ├── entity/
    │   ├── repository/
    │   └── service/
    │
    ├── auth/                            # Autenticação, login e perfil de usuários
    │   ├── controller/
    │   ├── dto/
    │   ├── entity/
    │   ├── repository/
    │   └── service/
    │
    ├── budget/                          # Orçamentos e planejamento financeiro da viagem
    │   ├── controller/
    │   ├── dto/
    │   ├── entity/
    │   ├── repository/
    │   └── service/
    │
    ├── destination/                     # Catálogo e pesquisa de destinos turísticos
    │   ├── controller/
    │   ├── dto/
    │   ├── entity/
    │   ├── repository/
    │   └── service/
    │
    ├── review/                          # Avaliações de destinos e viagens (DynamoDB NoSQL)
    │   ├── controller/
    │   ├── dto/
    │   ├── entity/
    │   ├── repository/
    │   └── service/
    │
    └── trip/                            # Gestão central de viagens, participantes e convites
        ├── controller/
        ├── dto/
        ├── entity/
        ├── repository/
        └── service/
```

### Responsabilidades de cada camada interna:
- **`controller`:** Endpoints HTTP REST, validação de requisições (`@Valid`) e mapeamento de respostas.
- **`dto`:** Objetos de transferência de dados (Request / Response) isolando a camada externa do modelo de domínio.
- **`entity`:** Entidades persistentes mapeadas via JPA (banco relacional) ou DynamoDB Enhanced (NoSQL).
- **`repository`:** Interfaces de acesso a dados (Spring Data JPA e DAOs DynamoDB).
- **`service`:** Regras de negócio, cálculos, validações lógicas e orquestração transacional.

---

## 🌐 Convenção de Idioma e Contrato OpenAPI

Para garantir a consistência arquitetural e a conformidade com o contrato de API ([`docs/viajajunto-openapi.json`](docs/viajajunto-openapi.json)), estabeleceu-se a seguinte diretriz de idioma para o projeto:

### ⚠️ Regra para Novo Código (New Features & Refactorings)

> [!IMPORTANT]
> **Todo código novo, rota HTTP e DTO deve ser implementado obrigatoriamente em inglês**, em total conformidade com o OpenAPI 3.0.3 v1.0.0.

- **Rotas e Endpoints:** Devem ser nomeados em inglês e seguir o padrão RESTful estabelecido no contrato OpenAPI:
  - ✅ **Correto:** `GET /trips`, `POST /trips/{tripId}/activities`, `GET /places/{placeId}/reviews`
  - ❌ **Evitar:** `GET /api/viagens`, `POST /atividades`, `GET /destinos/{id}/avaliacoes`
- **DTOs e Payloads JSON:**
  - Nomes das classes de request/response e seus campos serializados devem ser em inglês:
  - ✅ **Correto:** `CreateTripRequest`, `TripResponseDTO`, com campos JSON como `{ "title": "...", "startDate": "...", "budget": 1000 }`
  - ❌ **Evitar:** `ViagemRequestDTO`, com campos `{ "nome": "...", "dataInicio": "..." }`
- **Respostas Paginadas:**
  - Listagens novas devem utilizar o padrão de envelopamento `{ "data": [...], "meta": { "page": 1, "limit": 20, "totalItems": 100, "totalPages": 5 } }`.
- **Tratamento de Erros:**
  - As mensagens e schemas de erro devem respeitar o formato padronizado da OpenAPI (`{ "timestamp": "...", "status": 404, "error": "Not Found", "message": "...", "path": "..." }`).

### 🔄 Convivência com Código Legado
O projeto ainda possui classes e entidades legadas em português (ex.: `ViagemEntity`, `OrcamentoService`, `AvaliacaoController`). Essas estruturas estão em processo de transição; **ao criar novos módulos, novas rotas ou refatorar componentes existentes, utilize exclusivamente o inglês**.

