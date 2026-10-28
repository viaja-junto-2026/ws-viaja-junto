---
name: backend-api-builder
description: Orienta a implementação de endpoints, DTOs e regras de resposta HTTP no ViajaJunto seguindo a OpenAPI 3.0.3.
---

# Diretrizes para Criação de Endpoints e DTOs (ViajaJunto API)

Ao implementar ou alterar endpoints no backend, siga rigorosamente o contrato definido na OpenAPI (`viajajunto-openapi.json`):

## 1. Padrão de Respostas e Tratamento de Erros
- **Sucesso com Listas:** Toda resposta paginada DEVE seguir a estrutura `{ "data": [...], "meta": { "page", "limit", "totalItems", "totalPages" } }`.
- **Formato Padrão de Erro:** Em caso de exceção, a API DEVE retornar a estrutura `{ "code": string, "message": string, "details": [...] }`.
  - Exemplo de Validação (422): `code: "VALIDATION_ERROR"`.
  - Exemplo de Conflito de Agenda (409): `code: "SCHEDULE_CONFLICT"`.
  - Exemplo de Recurso Não Encontrado (404): `code: "NOT_FOUND"`.

## 2. Autenticação e Autorização
- Todos os endpoints protegidos exigem o cabeçalho `Authorization: Bearer <accessToken>` (Firebase JWT).
- Endpoints públicos (como `/places`, `/places/{placeId}/reviews` e `/places/{placeId}/topics`) não devem exigir o filtro de autenticação.

## 3. Padrão de Nomenclatura e Tipos
- Parâmetros da query de paginação: `page` (padrão 1) e `limit` (padrão 20, máx 100).
- Ordenação: prefixo `-` na query para indicar ordem decrescente (ex: `?sort=-createdAt`).