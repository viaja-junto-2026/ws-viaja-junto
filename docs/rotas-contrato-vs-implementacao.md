# Rotas do contrato × rotas implementadas

Comparação entre o contrato alvo ([`viajajunto-openapi.json`](viajajunto-openapi.json), 43 operações) e o que a API implementa hoje (visível no Swagger UI do perfil `dev`).

Legenda: ✅ implementado como no contrato · ⚠️ existe, mas difere do contrato · ❌ não existe.

**Resumo:** 15 ✅ · 7 ⚠️ · 21 ❌

## Usuários
| Contrato | Situação | Observação |
| :--- | :---: | :--- |
| `GET /users/me` | ✅ | |
| `PATCH /users/me` | ✅ | |

## Viagens
| Contrato | Situação | Observação |
| :--- | :---: | :--- |
| `GET /trips` | ✅ | paginada `{data, meta}` |
| `POST /trips` | ✅ | |
| `GET /trips/{tripId}` | ✅ | aceita também o prefixo `trp_` no ID |
| `PATCH /trips/{tripId}` | ⚠️ | hoje é `PUT /trips/{id}` |
| `DELETE /trips/{tripId}` | ✅ | |

## Membros
| Contrato | Situação | Observação |
| :--- | :---: | :--- |
| `GET /trips/{tripId}/members` | ✅ | paginada |
| `POST /trips/{tripId}/members` | ⚠️ | hoje é `POST /trips/{id}/members/join` com **código de convite**; o contrato pede convite por e-mail (`MemberInvite`) |
| `PATCH /trips/{tripId}/members/{userId}` | ⚠️ | hoje o identificador é o `membroId` interno, não o `userId` |
| `DELETE /trips/{tripId}/members/{userId}` | ⚠️ | idem |

## Destinos da viagem
| Contrato | Situação | Observação |
| :--- | :---: | :--- |
| `GET /trips/{tripId}/destinations` | ✅ | |
| `POST /trips/{tripId}/destinations` | ✅ | |
| `PATCH /trips/{tripId}/destinations/{destinationId}` | ❌ | |
| `DELETE /trips/{tripId}/destinations/{destinationId}` | ✅ | |

## Atividades
| Contrato | Situação | Observação |
| :--- | :---: | :--- |
| `GET /trips/{tripId}/activities` | ✅ | paginada |
| `POST /trips/{tripId}/activities` | ⚠️ | hoje só existe `POST /trips/{id}/destinations/{destinoId}/activities` (atividade criada dentro de um destino) |
| `PATCH /trips/{tripId}/activities/{activityId}` | ❌ | |
| `DELETE /trips/{tripId}/activities/{activityId}` | ✅ | |

## Mapa e rota
| Contrato | Situação | Observação |
| :--- | :---: | :--- |
| `GET /trips/{tripId}/map` | ✅ | marcadores via Google Places, com cache |
| `GET /trips/{tripId}/route` | ❌ | exige a Directions API |

## Despesas
| Contrato | Situação | Observação |
| :--- | :---: | :--- |
| `GET /trips/{tripId}/expenses` | ❌ | o domínio e o cálculo existem (`modules/expense/domain`), falta a camada web |
| `POST /trips/{tripId}/expenses` | ❌ | idem |
| `PATCH /trips/{tripId}/expenses/{expenseId}` | ❌ | idem |
| `DELETE /trips/{tripId}/expenses/{expenseId}` | ❌ | idem |
| `GET /trips/{tripId}/balances` | ❌ | idem |
| `POST /trips/{tripId}/settlements` | ❌ | idem |

## Lugares e avaliações
| Contrato | Situação | Observação |
| :--- | :---: | :--- |
| `GET /places` | ⚠️ | existe como busca no catálogo, mas devolve o `Page` do Spring (sem `{data, meta}`) e usa filtros `categoria`/`notaMinima` em vez de `country`/`sort` |
| `GET /places/{placeId}` | ⚠️ | existe, mas o corpo (`DestinoCatalogoDTO`) difere do schema `Place` |
| `GET /places/{placeId}/reviews` | ✅ | paginada, pública |
| `POST /places/{placeId}/reviews` | ✅ | |
| `PATCH /reviews/{reviewId}` | ❌ | |
| `DELETE /reviews/{reviewId}` | ❌ | |
| `GET /places/{placeId}/topics` | ❌ | |
| `POST /places/{placeId}/topics` | ❌ | |
| `GET /topics/{topicId}/comments` | ❌ | |
| `POST /topics/{topicId}/comments` | ❌ | |

## Denúncias e notificações
| Contrato | Situação | Observação |
| :--- | :---: | :--- |
| `POST /reports` | ❌ | |
| `GET /notifications` | ❌ | |
| `PATCH /notifications/{notificationId}/read` | ❌ | |
| `GET /notifications/preferences` | ❌ | |
| `PUT /notifications/preferences` | ❌ | |
| `POST /notifications/devices` | ❌ | |

## Rotas que existem no código e não estão no contrato
| Rota | Observação |
| :--- | :--- |
| `/trips/{id}/budget` (`GET`, `PUT`) | orçamento planejado da viagem |
| `/activities/catalog` (`GET`, `GET /{id}`, `GET /featured`) | catálogo de atividades |
| `/places/featured`, `/places/visited-countries` | extras do catálogo de destinos |
| `GET /trips/{id}/destinations/{destinoId}/activities` | atividades de um destino |
| `GET /places/{placeId}/activities/{atividadeId}/reviews` | avaliações de uma atividade (hoje sem envelope) |
| `/api/...` (rotas antigas em português) | mantidas por compatibilidade, fora do Swagger |

## Sugestão de issues futuras
Cada grupo abaixo pode virar uma issue pequena:

1. **Alinhar rotas parciais:** `PATCH /trips/{id}`, membros por `userId` com convite por e-mail, `POST /trips/{id}/activities`, envelope e filtros de `GET /places`, schema `Place`.
2. **Despesas (camada web):** `/expenses`, `/balances` e `/settlements` sobre o domínio já pronto.
3. **Edição de destinos e atividades:** `PATCH` de ambos.
4. **Rota entre destinos:** `GET /trips/{id}/route` (Directions API).
5. **Comunidade:** `PATCH`/`DELETE` de reviews, topics, comments e reports.
6. **Notificações:** lista, leitura, preferências e dispositivos.
