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
| **Status / Health** | `http://localhost:8080/actuator/health` | Health Check via Spring Boot Actuator |
| **H2 Console** | `http://localhost:8080/h2-console` | **JDBC URL:** `jdbc:h2:mem:viajajuntodb`<br>**User:** `sa` \| **Password:** *(vazio)* |
| **DynamoDB Admin** | `http://localhost:8002` | Interface web para visualização de tabelas e itens |
| **MiniStack (AWS Local)** | `http://localhost:4566` | Emulador local com suporte a DynamoDB |

> [!TIP]
> O banco de dados relacional (H2) é inicializado automaticamente com dados fictícios carregados a partir do arquivo [`src/main/resources/import.sql`](src/main/resources/import.sql) (usuários, destinos turísticos, atividades e viagens).

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

---

## 📌 Principais Rotas da API (Padrão OpenAPI)

As rotas e recursos da API seguem a especificação OpenAPI 3.0.3 v1.0.0 ([`docs/viajajunto-openapi.json`](docs/viajajunto-openapi.json)):

### Autenticação & Usuários (`/auth`)
| Método | Endpoint | Descrição | Autenticação |
| :--- | :--- | :--- | :---: |
| `POST` | `/auth/register` | Cadastro de novo usuário | Pública |
| `POST` | `/auth/login` | Login e emissão de token JWT | Pública |
| `GET` | `/auth/me` | Dados do perfil do usuário autenticado | Bearer JWT |
| `POST` | `/auth/forgot-password` | Solicitação de recuperação de senha | Pública |

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
| `POST` | `/places/{placeId}/reviews` | Criação de avaliação (persistência DynamoDB) | Bearer JWT |

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
│   └── security/                        # Segurança JWT (filtros, provedores, autenticação)
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
