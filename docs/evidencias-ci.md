# Evidências do pipeline de CI/CD

Este documento mostra, requisito por requisito, como o repositório atende ao pipeline pedido e onde ver a prova de cada item.

**PR de evidência (fechado, com todos os checks passando):** [PR #36](https://github.com/viaja-junto-2026/ws-viaja-junto/pull/36), mergeado em 05/11/2026 (commit `c303c55`).

| Check | Resultado | Link |
| :--- | :---: | :--- |
| Lint (Checkstyle) | ✅ | [execução](https://github.com/viaja-junto-2026/ws-viaja-junto/actions/runs/42061372321/job/127363698253) |
| Tests and Coverage | ✅ | [execução](https://github.com/viaja-junto-2026/ws-viaja-junto/actions/runs/42061371317/job/127363695024) |
| Run Spectral Linter | ✅ | [execução](https://github.com/viaja-junto-2026/ws-viaja-junto/actions/runs/42061370986/job/127363692702) |
| SonarQube Scan (com Quality Gate) | ✅ | [execução](https://github.com/viaja-junto-2026/ws-viaja-junto/actions/runs/42061370973/job/127363705220) |
| SonarCloud Code Analysis | ✅ | [painel do PR](https://sonarcloud.io/dashboard?id=viaja-junto-2026_ws-viaja-junto&pullRequest=36) |
| Build and Scan with Trivy | ✅ | [execução](https://github.com/viaja-junto-2026/ws-viaja-junto/actions/runs/42061371371/job/127363693181) |

---

## 1. Proteção da `main`
Configuração em *Settings → Branches → regra da `main`*:
- merge **somente por Pull Request**;
- **checks obrigatórios:** `Build and Scan with Trivy`, `SonarQube Scan`, `Run Spectral Linter`, `Lint (Checkstyle)` e `Tests and Coverage`;
- a branch precisa estar **atualizada com a `main`** antes do merge;
- a regra **vale também para administradores** e **force push é bloqueado**.

> Observação: a aprovação obrigatória de um revisor está em 0 por enquanto (o projeto está sendo desenvolvido por uma pessoa). Pode ser ativada na mesma tela, em *Require approvals*, quando a equipe estiver trabalhando em conjunto.

## 2. Lint
- Ferramenta: **Checkstyle**, com regras próprias em [`config/checkstyle/checkstyle.xml`](../config/checkstyle/checkstyle.xml), todas com severidade **error**. Qualquer violação falha o build.
- CI: job **Lint (Checkstyle)** ([`.github/workflows/lint.yml`](../.github/workflows/lint.yml)), que roda `mvn -B checkstyle:check`. Se falhar, o relatório fica como artefato.
- Local: `./mvnw checkstyle:check`.

## 3. Testes e cobertura
- CI: job **Tests and Coverage** ([`.github/workflows/tests.yml`](../.github/workflows/tests.yml)), que roda `mvn verify`.
- O job **falha se qualquer teste quebrar** e também se algum teste for **ignorado** (no CI o Docker está disponível, então os testes com Testcontainers rodam).
- O JaCoCo gera o relatório e **reprova o build** se a cobertura ficar abaixo de **70% das linhas** ou **60% dos branches** (regra no [`pom.xml`](../pom.xml)).
- Artefatos de cada execução: `jacoco-report` (HTML) e `surefire-reports`, mais o resumo da cobertura no *job summary*.

## 4. Validação do contrato (Spectral)
- Ação oficial `stoplightio/spectral-action` ([`.github/workflows/spectral.yml`](../.github/workflows/spectral.yml)), fixada por commit.
- Ruleset [`.spectral.yaml`](../.spectral.yaml) com `extends: ["spectral:oas"]`.
- Valida o contrato real, [`docs/viajajunto-openapi.json`](viajajunto-openapi.json), sem erros.

## 5. SonarQube
- Ação oficial `SonarSource/sonarqube-scan-action` ([`.github/workflows/sonarqube.yml`](../.github/workflows/sonarqube.yml)), enviando o relatório de cobertura do JaCoCo (`sonar.coverage.jacoco.xmlReportPaths`).
- `sonar.qualitygate.wait=true`: **o job falha se o Quality Gate reprovar**, e como é um check obrigatório, o PR não pode ser mergeado.
- Painel público do projeto: <https://sonarcloud.io/dashboard?id=viaja-junto-2026_ws-viaja-junto>.

## 6. Trivy
- Ação oficial `aquasecurity/trivy-action` ([`.github/workflows/trivy.yml`](../.github/workflows/trivy.yml)), fixada por commit, que constrói a imagem Docker e a escaneia.
- `severity: HIGH,CRITICAL` e **`exit-code: 1`**: a vulnerabilidade reprova o build.
- Resultado atual na imagem: **0 vulnerabilidades no sistema operacional (Alpine) e 0 nas bibliotecas Java** (`app.jar`).
- Exceções: duas falhas CRÍTICAS do Spring Framework 6.2 que só têm correção no Spring 7. Estão registradas em [`.trivyignore`](../.trivyignore) com justificativa (a API não usa `XsltView` nem SSE) e **validade até 28/02/2027**, depois da qual o Trivy volta a reprovar.
- Para chegar a esse resultado foram atualizados o Spring Boot (3.4.3 → 3.5.16), o AWS SDK, o Tomcat e o Jackson, e removido o cliente HTTP assíncrono (Netty) que o SDK trazia sem necessidade.

## 7. Publicação no Docker Hub
- Workflow [`.github/workflows/docker-publish.yml`](../.github/workflows/docker-publish.yml), executado em **push na `main`** e em **tags `v*`**.
- Tags geradas: **`latest`**, **versão semântica** (ex.: `1.0.0`) e **SHA curto** (ex.: `sha-c303c55`).
- Credenciais: `DOCKER_USERNAME` e **`DOCKER_ACCESS_TOKEN`** (Access Token do Docker Hub, permissão *Read & Write*), guardados como **segredos do GitHub**, nunca no código.
- Imagem pública: <https://hub.docker.com/r/juliatietbohl/viajajunto-api>
- Prova: a tag [`v1.0.0`](https://github.com/viaja-junto-2026/ws-viaja-junto/releases/tag/v1.0.0) disparou a [publicação](https://github.com/viaja-junto-2026/ws-viaja-junto/actions/runs/42064659937), que gerou as imagens `1.0.0`, `latest` e `sha-c303c55`.

Para rodar a imagem publicada:
```bash
docker pull juliatietbohl/viajajunto-api:1.0.0
```

## 8. PR fechado com os checks passando
[PR #36](https://github.com/viaja-junto-2026/ws-viaja-junto/pull/36): todos os 6 checks da tabela do topo passaram antes do merge.
