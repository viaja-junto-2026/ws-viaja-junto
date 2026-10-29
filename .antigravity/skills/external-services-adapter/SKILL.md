---
name: external-services-adapter
description: Padrões de integração com serviços externos reutilizados e adaptados (Google Maps, SendGrid, FCM, Firebase Auth).
---

# Padrões para Integração de Serviços de Terceiros

Para garantir manutenibilidade e evitar lock-in direto, siga o padrão Adapter ao integrar bibliotecas/APIs externas.

## 1. Mapa Interativo (Google Maps / Places / Directions API)
- Utilize cache de resultados nas chamadas do Google Maps/Directions API para minimizar chamadas desnecessárias e otimizar custos.
- Sempre desacople o `placeId` de lugares do Google Maps ao vincular com os objetos internos de `Destination` e `Activity`.

## 2. Notificações (SendGrid & Firebase Cloud Messaging)
- Convites de membros e alertas de novas despesas devem ser despachados de forma assíncrona para não bloquear as requisições HTTP do usuário.
- Registre os tokens de dispositivos push na rota `/notifications/devices` usando FCM.

## 3. Autenticação (Firebase Auth)
- O backend deve apenas validar o Bearer Token recebido e mapear os dados do usuário. O fluxo de criação de senha/token é de responsabilidade do provedor de auth.