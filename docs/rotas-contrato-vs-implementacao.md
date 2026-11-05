# Rotas do contrato × rotas implementadas

Comparação entre o contrato alvo ([`viajajunto-openapi.json`](viajajunto-openapi.json), 43 operações) e o que a API implementa hoje (visível no Swagger UI do perfil `dev`).

Legenda: ✅ a rota e o verbo existem como no contrato · ⚠️ existe, mas difere do contrato · ❌ não existe.

> **Importante:** esta tabela compara **caminho e verbo**. O **formato dos dados** (nomes dos campos do JSON) ainda diverge do contrato na maioria dos recursos; veja a seção "Formato dos dados" mais abaixo.

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
| `GET` e `POST /places/{placeId}/activities/{atividadeId}/reviews` | avaliações de uma atividade (a listagem ainda não tem o envelope `{data, meta}`) |

## Formato dos dados
Os nomes dos campos do JSON usam português em vários recursos, enquanto o contrato usa inglês. Os nomes em Java podem continuar em português; o que precisa mudar é o JSON. O plano está na issue #39.

| Recurso | Contrato | API hoje |
| :--- | :--- | :--- |
| Usuário | `name`, `createdAt` | `nome`, `dataCriacao` |
| Viagem | `title`, `startDate`, `endDate`, `baseCurrency`, `ownerId` | `nome`, `dataInicio`, `dataFim`, (sem moeda base), `criador` |
| Membro | `userId`, `name`, `email`, `role`, `status` | `id`, `usuario`, `permissao` |
| Destino | `name`, `arrivalDate`, `departureDate`, `order`, `latitude`, `longitude` | `nome`, `dataChegada`, `dataSaida`, `ordemVisita` (sem coordenadas) |
| Atividade | `title`, `date`, `startTime`, `endTime`, `estimatedCost`, `currency` | `nome`, `dataHorario`, `duracaoMinutos`, `custoPrevisto` (sem moeda) |
| Avaliação | `placeId`, `rating`, `comment`, `author` | `destinoCatalogoId`, `nota`, `comentario`, `usuario` |
| Lugar | `name`, `country`, `averageRating`, `reviewCount` | `nome`, `pais`, `avaliacaoMedia`, `totalAvaliacoes` |

## Sugestão de issues futuras
Cada grupo abaixo pode virar uma issue pequena:

1. **Alinhar rotas parciais:** `PATCH /trips/{id}`, membros por `userId` com convite por e-mail, `POST /trips/{id}/activities`, envelope e filtros de `GET /places`, schema `Place`.
2. **Despesas (camada web):** `/expenses`, `/balances` e `/settlements` sobre o domínio já pronto.
3. **Edição de destinos e atividades:** `PATCH` de ambos.
4. **Rota entre destinos:** `GET /trips/{id}/route` (Directions API).
5. **Comunidade:** `PATCH`/`DELETE` de reviews, topics, comments e reports.
6. **Notificações:** lista, leitura, preferências e dispositivos.
