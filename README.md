# ✈️ ViajaJunto API

API RESTful para planejamento colaborativo de viagens em grupo. A plataforma permite aos viajantes organizar itinerários, gerenciar atividades com prevenção de conflitos de agenda, controlar orçamentos coletivos, consultar destinos e registrar avaliações.

> 👉 **Quer rodar e testar agora?** Vá direto ao [passo a passo](#-como-rodar-e-testar-passo-a-passo): ele cobre desde a criação do projeto no Firebase até o roteiro de testes no Swagger.

---

## 🛠️ Tecnologias Utilizadas

- **Linguagem & Framework:** Java 17, Spring Boot 3.5.16
- **Persistência Relacional:** Spring Data JPA, Hibernate, H2 Database (em memória)
- **Persistência NoSQL:** AWS SDK v2 (DynamoDB Enhanced Client)
- **Segurança & Autenticação:** Spring Security como OAuth2 Resource Server, validando tokens do Firebase Authentication (a API não guarda senha nem emite token)
- **Documentação de API:** OpenAPI 3.0.3, Spectral Linter
- **Qualidade & Testes:** JUnit 5, Mockito, REST Assured, JaCoCo, Checkstyle, SonarQube
- **Infraestrutura Local:** Docker, Docker Compose, MiniStack (emulador AWS local), DynamoDB Admin

---

## 📋 Pré-requisitos

- **Java JDK 17** ou superior instalado (`java -version`)
- **Docker Desktop** (ou Docker + Docker Compose) instalado **e ligado**
- **Git**
- Uma **conta Google**, para criar o projeto no Firebase (gratuito, veja o [Passo 2](#passo-2-criar-o-projeto-no-firebase))
- Um terminal: PowerShell no Windows, ou bash no Linux e macOS

> [!NOTE]
> Não é necessário ter o Maven instalado globalmente na máquina; o projeto inclui o Maven Wrapper (`mvnw` / `mvnw.cmd`).

---

## 🚀 Como Rodar e Testar (Passo a Passo)

Este guia leva do zero até a primeira chamada autenticada.

1. [Clonar o repositório](#passo-1-clonar-o-repositório)
2. [Criar o projeto no Firebase](#passo-2-criar-o-projeto-no-firebase)
3. [Subir o banco local (MiniStack)](#passo-3-subir-o-banco-local-ministack)
4. [Subir a API](#passo-4-subir-a-api)
5. [Conferir que subiu](#passo-5-conferir-que-subiu)
6. [Gerar o token de acesso](#passo-6-gerar-o-token-de-acesso)
7. [Autorizar no Swagger](#passo-7-autorizar-no-swagger)
8. [Roteiro de teste](#passo-8-roteiro-de-teste)
9. [Rodar os testes automáticos](#passo-9-rodar-os-testes-automáticos)
10. [Problemas comuns](#passo-10-problemas-comuns)

> [!IMPORTANT]
> **A API não tem tela de login nem guarda senha.** Quem cadastra e autentica os usuários é o **Firebase Authentication**. A API só valida o **token** que o Firebase emite. Por isso, para usar as rotas protegidas, você precisa de um projeto Firebase (gratuito) e de um token. Rotas públicas (como `GET /places`) funcionam sem token.

### Passo 1: Clonar o repositório

```bash
git clone https://github.com/viaja-junto-2026/ws-viaja-junto.git
cd ws-viaja-junto
```

### Passo 2: Criar o projeto no Firebase

Faça isso uma vez só. Cada grupo usa o **próprio** projeto: a API só aceita tokens do projeto indicado em `FIREBASE_PROJECT_ID`.

1. Entre em <https://console.firebase.google.com> com uma conta Google e clique em **Criar um projeto** (*Add project*). Dê um nome e pode **desativar** o Google Analytics.
2. No menu da esquerda, abra **Authentication** → **Vamos começar** (*Get started*) → aba **Método de login** (*Sign-in method*) → **E-mail/senha** (*Email/Password*) → ative a primeira opção → **Salvar**.
3. Clique na **engrenagem** ao lado de "Visão geral do projeto" → **Configurações do projeto** (*Project settings*) → aba **Geral**.
4. Role até **Seus aplicativos** (*Your apps*) e clique no ícone **`</>`** (Web). Dê um apelido (por exemplo `swagger`) e clique em **Registrar app**. Não precisa configurar o *Hosting*.
5. Vai aparecer um bloco de código como este. **Anote dois valores**:
   ```js
   const firebaseConfig = {
     apiKey: "AIzaSy...",          // <- WEB_API_KEY (39 caracteres, começa com "AIza")
     authDomain: "...",
     projectId: "meu-projeto-a1b2c", // <- PROJECT_ID (minúsculas e hifens)
     appId: "1:123456789:web:..."    // <- NÃO é a chave, ignore
   };
   ```
   Para ver o bloco de novo depois: **Configurações do projeto → Geral → Seus aplicativos →** clique no app → **Configuração do SDK**.

| Valor | Como é | Para que serve |
| :--- | :--- | :--- |
| **`projectId`** | minúsculas e hifens, ex.: `meu-projeto-a1b2c` | vai na variável `FIREBASE_PROJECT_ID` da API |
| **`apiKey`** (Web API Key) | 39 caracteres, começa com `AIza` | usada nos comandos do Passo 6 para cadastrar e logar |

> [!NOTE]
> A Web API Key do Firebase é só um identificador do projeto (ela vai embutida em aplicativos publicados). Mesmo assim, **não a coloque no repositório**. O que vale como senha é o **token** e a **senha do usuário**.

### Passo 3: Subir o banco local (MiniStack)

As **avaliações** ficam no DynamoDB, emulado localmente pelo MiniStack. Abra o **Docker Desktop**, espere ele ficar ligado e rode:

```bash
docker compose up -d ministack dynamodb-admin
```

Confira que o `ministack` está `healthy`:
```bash
docker compose ps
```

### Passo 4: Subir a API

Escolha **uma** das duas formas. Em ambas, **informe o `projectId` do Passo 2** em `FIREBASE_PROJECT_ID`. Sem isso, a API usa o projeto de exemplo `viajajunto-dev` e **recusa (401) os tokens do seu projeto**.

**Opção A: pelo Maven (recomendada para desenvolver)**

- **Windows (PowerShell):**
  ```powershell
  $env:FIREBASE_PROJECT_ID="meu-projeto-a1b2c"; .\mvnw.cmd spring-boot:run "-Dspring-boot.run.profiles=dev"
  ```
- **Linux / macOS:**
  ```bash
  FIREBASE_PROJECT_ID="meu-projeto-a1b2c" ./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
  ```

**Opção B: tudo no Docker (API, MiniStack e DynamoDB Admin)**

1. Crie, na raiz do projeto, um arquivo chamado **`.env`** com o seu `projectId` (o arquivo é ignorado pelo Git):
   ```
   FIREBASE_PROJECT_ID=meu-projeto-a1b2c
   ```
2. Suba tudo:
   ```bash
   docker compose up --build
   ```

Espere aparecer `Started ViajaJuntoApiApplication` no log. O perfil `dev` já cuida do resto (dados de exemplo, H2 console, CORS local, DynamoDB local).

Para encerrar: `Ctrl+C` no terminal da API e, para parar o Docker, `docker compose down`.

### Passo 5: Conferir que subiu

> [!NOTE]
> **No PowerShell, use `curl.exe` no lugar de `curl`** (nele, `curl` é um apelido de outro comando e não aceita as mesmas opções). No Linux, macOS e Git Bash, use `curl` normalmente.

1. Saúde da API:
   ```bash
   curl http://localhost:8080/actuator/health
   ```
   O esperado é `{"status":"UP"}`.
2. Abra o **Swagger UI**: <http://localhost:8080/swagger-ui.html>.
3. Teste uma rota **pública**: `GET /places` → **Try it out → Execute**. Deve listar os 4 destinos de exemplo (Paris, Rio de Janeiro, Tóquio, Bariloche).
4. Confirme que as rotas protegidas exigem login (sem token deve dar **401**):
   ```bash
   curl -i http://localhost:8080/users/me
   ```

### Passo 6: Gerar o token de acesso

O token é o **ID token do Firebase**: um texto longo que começa com `eyJ`. Você o obtém fazendo login no Firebase com e-mail e senha. Use os comandos abaixo com a **Web API Key** do Passo 2.

> [!WARNING]
> - O **e-mail precisa ser real e verificado**: a API recusa (401) tokens de e-mail não verificado. O Firebase não verifica sozinho; você vai clicar em um link enviado para a sua caixa de entrada (olhe o spam).
> - **Não use** os e-mails de exemplo do `data.sql` (como `ana.silva@email.com`): eles têm um identificador fixo e a API recusa outra conta com o mesmo e-mail.
> - A senha precisa ter pelo menos **6 caracteres**. Não compartilhe o token nem a senha.

**Windows (PowerShell)**

1. Defina os dados (troque os três valores, mantendo as aspas simples):
   ```powershell
   $key = 'SUA_WEB_API_KEY'; $email = 'voce@email.com'; $senha = 'UmaSenha123'
   ```
2. **Cadastrar o usuário** (só na primeira vez):
   ```powershell
   $t = (Invoke-RestMethod -Method Post -Uri "https://identitytoolkit.googleapis.com/v1/accounts:signUp?key=$key" -ContentType "application/json" -Body (@{ email=$email; password=$senha; returnSecureToken=$true } | ConvertTo-Json)).idToken
   ```
3. **Pedir o e-mail de verificação** e **clicar no link** que chegar (só na primeira vez):
   ```powershell
   Invoke-RestMethod -Method Post -Uri "https://identitytoolkit.googleapis.com/v1/accounts:sendOobCode?key=$key" -ContentType "application/json" -Body (@{ requestType="VERIFY_EMAIL"; idToken=$t } | ConvertTo-Json)
   ```
4. **Fazer o login e copiar o token** (repita só este passo quando o token vencer):
   ```powershell
   $t = (Invoke-RestMethod -Method Post -Uri "https://identitytoolkit.googleapis.com/v1/accounts:signInWithPassword?key=$key" -ContentType "application/json" -Body (@{ email=$email; password=$senha; returnSecureToken=$true } | ConvertTo-Json)).idToken
   $t | Set-Clipboard
   ```
   O token já fica copiado. Para conferir sem mostrá-lo: `$t.Substring(0,3); $t.Length` deve imprimir `eyJ` e um número acima de 800.

**Linux / macOS / Git Bash**

1. Defina os dados (troque os três valores):
   ```bash
   KEY='SUA_WEB_API_KEY'; EMAIL='voce@email.com'; SENHA='UmaSenha123'
   ```
2. **Cadastrar o usuário** (só na primeira vez). A resposta traz o `idToken`; guarde-o:
   ```bash
   TOKEN=$(curl -s -X POST "https://identitytoolkit.googleapis.com/v1/accounts:signUp?key=$KEY" -H 'Content-Type: application/json' -d "{\"email\":\"$EMAIL\",\"password\":\"$SENHA\",\"returnSecureToken\":true}" | sed -n 's/.*"idToken": *"\([^"]*\)".*/\1/p')
   ```
3. **Pedir o e-mail de verificação** e **clicar no link** que chegar (só na primeira vez):
   ```bash
   curl -s -X POST "https://identitytoolkit.googleapis.com/v1/accounts:sendOobCode?key=$KEY" -H 'Content-Type: application/json' -d "{\"requestType\":\"VERIFY_EMAIL\",\"idToken\":\"$TOKEN\"}"
   ```
4. **Fazer o login e obter o token** (repita só este passo quando o token vencer):
   ```bash
   TOKEN=$(curl -s -X POST "https://identitytoolkit.googleapis.com/v1/accounts:signInWithPassword?key=$KEY" -H 'Content-Type: application/json' -d "{\"email\":\"$EMAIL\",\"password\":\"$SENHA\",\"returnSecureToken\":true}" | sed -n 's/.*"idToken": *"\([^"]*\)".*/\1/p')
   echo "$TOKEN"
   ```
   Copie o texto impresso (ou use `echo -n "$TOKEN" | pbcopy` no macOS, `xclip -selection clipboard` no Linux).

Se algum comando devolver erro, o campo `message` da resposta diz o motivo:

| Mensagem | Causa | O que fazer |
| :--- | :--- | :--- |
| `API key not valid` | a chave está errada ou é de outro projeto | copie de novo o `apiKey` do Passo 2 (39 caracteres, começa com `AIza`) |
| `EMAIL_EXISTS` | o e-mail já foi cadastrado | pule o cadastro e faça só o login (passo 4) |
| `INVALID_LOGIN_CREDENTIALS` | e-mail ou senha errados | confira os valores e a senha (mínimo 6 caracteres) |
| `OPERATION_NOT_ALLOWED` | o login por e-mail e senha está desligado | ative em *Authentication → Método de login → E-mail/senha* |
| `WEAK_PASSWORD` | senha com menos de 6 caracteres | use uma senha maior |

> [!TIP]
> O token **vale 1 hora**. Quando vencer, as rotas protegidas passam a responder **401**: basta repetir o **passo 4** do login e colar o token novo no Swagger. Não precisa reiniciar a API.

### Passo 7: Autorizar no Swagger

1. Abra <http://localhost:8080/swagger-ui.html> e clique em **Authorize** (cadeado, no topo).
2. No campo **Value**, cole **só o token** (o texto que começa com `eyJ`). **Não escreva `Bearer`**: o Swagger acrescenta sozinho. Escrever duplica e dá 401.
3. Clique em **Authorize** e depois em **Close**. Os cadeados das rotas protegidas ficam fechados.
4. Teste `GET /users/me` → **Try it out → Execute**. O esperado é **200**, com os seus dados (no primeiro acesso a API cria o usuário a partir do token).

Para trocar o token depois de vencido: **Authorize → Logout**, cole o novo e confirme.

Fora do Swagger, o token vai no cabeçalho de toda chamada. Em Linux, macOS e Git Bash:
```bash
curl -H "Authorization: Bearer $TOKEN" http://localhost:8080/users/me
```
No PowerShell (o token está em `$t`):
```powershell
curl.exe -H "Authorization: Bearer $t" http://localhost:8080/users/me
```

### Passo 8: Roteiro de teste

Siga na ordem, no Swagger (**Try it out → Execute**). Os números (`id`) que as respostas devolvem entram nos passos seguintes. Em todas as rotas de destino e atividade, o `{destinoId}` precisa ser de um destino **da própria viagem**.

| # | Rota | Corpo | Esperado |
| :-: | :--- | :--- | :--- |
| 1 | `GET /users/me` | (sem corpo) | **200** com os seus dados |
| 2 | `POST /trips` | ver abaixo | **201**. Anote o `id` (é o `{viagemId}`) |
| 3 | `GET /trips` | (sem corpo) | **200**, paginada `{data, meta}`, com a viagem criada |
| 4 | `POST /trips/{viagemId}/destinations` | ver abaixo | **201**. Anote o `id` (é o `{destinoId}`) |
| 5 | `POST /trips/{viagemId}/destinations/{destinoId}/activities` | ver abaixo (2 atividades) | **201** em cada uma |
| 6 | `GET /trips/{viagemId}/budget` | (sem corpo) | **200** com `totalPlanejado` 1000 |
| 7 | `PUT /trips/{viagemId}/budget` | `{ "orcamentoTotal": 8000 }` | **200** com `orcamentoTotal` 8000 |
| 8 | `GET /trips/{viagemId}/activities` | (sem corpo) | **200**, as 2 atividades em `{data, meta}` |

**Corpo do passo 2** (criar a viagem):
```json
{
  "nome": "Viagem de teste",
  "descricao": "Testando o Swagger",
  "dataInicio": "2026-12-01",
  "dataFim": "2026-12-10",
  "orcamentoTotal": 5000
}
```
**Corpo do passo 4** (criar o destino):
```json
{ "nome": "Lisboa", "pais": "Portugal" }
```
**Corpos do passo 5** (rode a rota duas vezes, uma com cada corpo):
```json
{
  "nome": "Passeio de barco",
  "tipo": "Passeio",
  "dataHorario": "2026-12-02T10:00:00",
  "duracaoMinutos": 60,
  "custoPrevisto": 600
}
```
```json
{
  "nome": "Jantar tradicional",
  "tipo": "Gastronomia",
  "dataHorario": "2026-12-02T20:00:00",
  "duracaoMinutos": 90,
  "custoPrevisto": 400
}
```
No passo 6 o resumo vem assim: `totalPlanejado` 1000, `saldoDisponivel` 4000, `percentualConsumidoTotal` 20.0 e os gastos por categoria (`Passeio` 600 e `Gastronomia` 400).

**Avaliações (usam o DynamoDB).** Os destinos de exemplo têm `id` 1 a 4 (Paris, Rio de Janeiro, Tóquio, Bariloche) e as atividades do catálogo também, de 1 a 4. Cada um já vem com 3 avaliações do seed:

| # | Rota | Corpo | Esperado |
| :-: | :--- | :--- | :--- |
| 9 | `GET /places/1/reviews` | (sem corpo, não precisa de token) | **200**, 3 avaliações de exemplo |
| 10 | `POST /places/2/reviews` | `{ "rating": 5, "comment": "Lugar incrível" }` | **201**; o `GET /places/2/reviews` agora tem 4 |
| 11 | `POST /places/2/reviews` (de novo, mesmo corpo) | o mesmo | **409** `REVIEW_EXISTS` (uma avaliação por destino) |
| 12 | `POST /places/1/activities/1/reviews` | `{ "rating": 4, "comment": "Passeio ótimo" }` | **201**; veja em `GET /places/1/activities/1/reviews` |

Você pode ver os itens gravados no **DynamoDB Admin**: <http://localhost:8002> → tabela `Avaliacoes`.

**Erros para demonstrar** (todos de propósito):

| Teste | Esperado |
| :--- | :--- |
| qualquer rota protegida sem **Authorize** | **401** `UNAUTHORIZED` |
| `GET /trips/99999/budget` | **404** `NOT_FOUND` |
| `GET /trips/1/budget` (viagem de outro usuário do seed) | **403** `FORBIDDEN` |
| `POST /trips` com `"nome": ""` | **422** `VALIDATION_ERROR` |
| `POST /places/2/reviews` com `{ "rating": 9 }` | **422** `VALIDATION_ERROR` |
| segunda atividade sobreposta, às `2026-12-02T10:30:00` | **409** `SCHEDULE_CONFLICT` |
| `POST /trips` com JSON quebrado (falta uma aspa) | **400** `MALFORMED_REQUEST` |

### Passo 9: Rodar os testes automáticos

```bash
./mvnw verify
```
No Windows: `.\mvnw.cmd verify`. Roda o lint, todos os testes e a verificação de cobertura mínima. Com o **Docker ligado**, os testes que usam DynamoDB de verdade também rodam. Os testes **não precisam** de Firebase nem de internet (usam um token simulado). Mais detalhes na seção [Como Rodar os Testes](#-como-rodar-os-testes).

### Passo 10: Problemas comuns

| Sintoma | Causa provável | O que fazer |
| :--- | :--- | :--- |
| A API não sobe e reclama de `FIREBASE_PROJECT_ID` ou de `AWS_...` | faltou o perfil `dev` | rode com `-Dspring-boot.run.profiles=dev` (Passo 4) |
| **401** em todas as rotas protegidas | token vencido (vale 1 hora) | repita o login (Passo 6, passo 4) e use o **Authorize** de novo |
| **401** só com o seu e-mail | e-mail ainda **não verificado**, ou a API subiu **sem** o `FIREBASE_PROJECT_ID` do seu projeto | clique no link do e-mail e gere um token novo; confira o Passo 4 |
| **401** logo depois de colar o token | você escreveu `Bearer` antes do token no Swagger | **Authorize → Logout** e cole só o token |
| **400** "O destino não pertence à viagem indicada" | `{destinoId}` de outra viagem (ou do seed) | liste `GET /trips/{viagemId}/destinations` e use um `id` de lá |
| **403** em uma viagem | a viagem não é sua | use o `id` da viagem que você criou |
| **409** `SCHEDULE_CONFLICT` | já existe atividade nesse horário na viagem | mude o `dataHorario` (atividades que terminam quando a outra começa não conflitam) |
| Rotas de avaliações dão erro | o MiniStack não está no ar | `docker compose up -d ministack dynamodb-admin` (Passo 3) |
| Erro de porta `8080` em uso | outra API (ou outro contêiner) já usa a porta | pare o outro processo, ou `docker compose down` |
| As viagens criadas sumiram | a API foi reiniciada: o H2 é em memória | crie de novo (o seed volta sozinho) |
| `docker` não funciona | o Docker Desktop não está ligado | abra o Docker Desktop e espere ficar pronto |

---

## 🌐 URLs e Acessos Úteis

| Serviço | URL | Credenciais / Observações |
| :--- | :--- | :--- |
| **API Base** | `http://localhost:8080` | Porta padrão da aplicação |
| **Status / Health** | `http://localhost:8080/actuator/health` | Health Check via Spring Boot Actuator |
| **Swagger UI** | `http://localhost:8080/swagger-ui.html` | Só no perfil `dev`. Veja o [Passo 7](#passo-7-autorizar-no-swagger) |
| **Spec OpenAPI gerada** | `http://localhost:8080/v3/api-docs` | Só no perfil `dev` |
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
| `FIREBASE_PROJECT_ID` | **Sim**, exceto no perfil `dev` | ID do projeto no Firebase Authentication. A API só aceita tokens emitidos para este projeto (emissor `https://securetoken.google.com/<id>` e audiência `<id>`). O perfil `dev` usa o padrão `viajajunto-dev`, que **só aceita tokens de um projeto com esse ID**: para testar com o token do seu projeto, defina a variável com o `projectId` dele ([Passo 4](#passo-4-subir-a-api)). |
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
O `./mvnw verify` roda os testes, gera o relatório e **reprova o build** se a cobertura do projeto cair abaixo de **70% das linhas** ou **60% dos branches** (regra no `pom.xml`, plugin JaCoCo).

No CI, o job **Tests and Coverage** roda `mvn verify` a cada PR, falha se algum teste quebrar ou ficar ignorado (no CI o Docker está disponível, então nenhum teste é pulado), mostra a cobertura no resumo do job e publica os artefatos `jacoco-report` e `surefire-reports`.

### Lint (Checkstyle)
As regras estão em [`config/checkstyle/checkstyle.xml`](config/checkstyle/checkstyle.xml) e todas têm severidade **error**: qualquer violação reprova o build. Elas cobrem imports não usados, chaves obrigatórias, comparações de `String`, `switch` sem `default`, nomes e espaços no fim da linha (formatação como indentação fica de fora). O código de testes também é verificado.
```bash
./mvnw checkstyle:check
```
O mesmo comando roda no CI, no job **Lint (Checkstyle)**, a cada PR. Se falhar, o relatório fica como artefato do job (`checkstyle-report`).

---

## 🛡️ Varredura de vulnerabilidades (Trivy)

O job **Build and Scan with Trivy** constrói a imagem Docker e a escaneia a cada PR. Ele **reprova o build** se encontrar vulnerabilidade **HIGH** ou **CRITICAL**.

- **Corrigir de verdade:** atualizar a dependência. Quando o Spring Boot ainda traz uma versão vulnerável, a versão corrigida é fixada por propriedade no `pom.xml` (hoje `tomcat.version` e `jackson-bom.version`).
- **Exceção justificada:** só quando não existe correção na versão usada e a falha não se aplica ao projeto. Vai no [`.trivyignore`](.trivyignore) com o motivo e uma data de validade (`exp:`); depois dela o Trivy volta a reprovar, o que força a revisão.
- **Imagem:** roda como usuário não root, tem `HEALTHCHECK` em `/actuator/health` e o `.dockerignore` mantém o contexto de build enxuto.

---

## ✅ Pipeline de CI/CD

A cada PR rodam, em jobs separados, o **lint** (Checkstyle), os **testes com cobertura** (JaCoCo), a **validação do contrato** (Spectral), a **análise do SonarQube** (com Quality Gate) e o **scan do Trivy**. Todos são obrigatórios para mergear na `main`, que só aceita alterações por Pull Request. No push na `main` e em tags `v*`, a imagem é publicada no Docker Hub com as tags `latest`, versão semântica e SHA.

A comprovação de cada requisito, com os links, está em [`docs/evidencias-ci.md`](docs/evidencias-ci.md).

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

### Formato de erros
Toda resposta de erro da API segue o mesmo formato, definido no contrato:
```json
{ "code": "VALIDATION_ERROR", "message": "Dados fornecidos são inválidos", "details": [{ "field": "email", "issue": "formato inválido" }] }
```

| Status | `code` | Quando acontece |
| :---: | :--- | :--- |
| 400 | `MALFORMED_REQUEST` | JSON malformado ou corpo ausente |
| 400 | `INVALID_PARAMETER` | parâmetro com tipo errado (`?page=abc`) ou obrigatório ausente; `details` indica o campo |
| 400 | `BUSINESS_RULE_VIOLATION` | regra de negócio violada |
| 401 | `UNAUTHORIZED` | token ausente, inválido ou expirado |
| 403 | `FORBIDDEN` | usuário sem permissão sobre o recurso |
| 404 | `NOT_FOUND` | recurso ou rota inexistente |
| 405 | `METHOD_NOT_ALLOWED` | método HTTP não suportado; o cabeçalho `Allow` lista os aceitos |
| 409 | `SCHEDULE_CONFLICT`, `REVIEW_EXISTS` | conflito de horário; avaliação já existente |
| 415 | `UNSUPPORTED_MEDIA_TYPE` | `Content-Type` diferente de `application/json` |
| 422 | `VALIDATION_ERROR` | dados inválidos; `details` lista campo e problema |
| 500 | `INTERNAL_SERVER_ERROR` | erro inesperado; a mensagem não expõe detalhes internos |

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

### Destinos da Viagem (`/trips/{tripId}/destinations`)
| Método | Endpoint | Descrição |
| :--- | :--- | :--- |
| `GET` | `/trips/{tripId}/destinations` | Lista os destinos da viagem, na ordem de visita |
| `POST` | `/trips/{tripId}/destinations` | Adiciona um destino à viagem |
| `DELETE` | `/trips/{tripId}/destinations/{destinationId}` | Remove o destino da viagem |
| `GET` | `/trips/{tripId}/map` | Marcadores de destinos e atividades para o mapa (usa o Google Places, com cache) |

### Itinerário & Atividades (`/trips/{tripId}/activities`)
| Método | Endpoint | Descrição |
| :--- | :--- | :--- |
| `GET` | `/trips/{tripId}/activities` | Listagem paginada de atividades agendadas |
| `GET` | `/trips/{tripId}/destinations/{destinationId}/activities` | Lista as atividades de um destino da viagem |
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
| `GET` | `/places/{placeId}/activities/{activityId}/reviews` | Avaliações de uma atividade do catálogo | Pública |
| `POST` | `/places/{placeId}/activities/{activityId}/reviews` | Avaliação de uma atividade do catálogo (DynamoDB) | Bearer (token Firebase) |

### Catálogo de Atividades (`/activities/catalog`)
| Método | Endpoint | Descrição | Autenticação |
| :--- | :--- | :--- | :---: |
| `GET` | `/activities/catalog` | Busca de atividades e passeios turísticos | Pública |
| `GET` | `/activities/catalog/{id}` | Consulta de atividade do catálogo por ID | Pública |
| `GET` | `/activities/catalog/featured` | Atividades em destaque | Pública |

---

## 🌐 Convenção de idioma

- **Rotas:** seguem o contrato OpenAPI, em inglês (`/trips`, `/places`, `/users/me`).
- **Código novo:** deve usar inglês, para ficar alinhado ao contrato.
- **Código existente:** classes, entidades e colunas do banco continuam em português e **não serão renomeadas em massa** (o diff seria enorme, mexeria no banco e não traria ganho funcional).
- **Campos do JSON:** em vários recursos ainda estão em português (`nome`, `dataInicio`...), divergindo do contrato. A divergência está registrada em [`docs/rotas-contrato-vs-implementacao.md`](docs/rotas-contrato-vs-implementacao.md) e será tratada na issue #39.

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
