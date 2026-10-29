---
name: expense-module-decoupling
description: Regras de arquitetura e desacoplamento para o Módulo de Divisão de Despesas em Grupo do ViajaJunto.
---

# Diretrizes de Arquitetura do Módulo de Despesas

Conforme o Plano de Reuso do ViajaJunto, o módulo de **Divisão de Despesas** deve ser isolado do domínio de viagens para permitir futuro reuso como microsserviço ou biblioteca open-source.

## 1. Regra do Domínio Limpo (Clean Domain)
- **NUNCA** importe entidades ou conceitos de viagens (`Trip`, `Destination`, `Activity`) dentro do núcleo de cálculo do módulo de despesas.
- Use estritamente conceitos genéricos no núcleo: `Group`, `Participant`, `Expense`, `SplitType` (`equal`, `percentage`, `fixed`), `Balance` e `Transfer`.
- A associação entre despesas e viagens (`tripId`, `activityId`) deve existir apenas na camada externa de integração (DTOs / Controllers do ViajaJunto).

## 2. Regras de Negócio e Validação de Divisão
- **equal:** O campo `share` dos participantes deve ser `null`.
- **percentage:** A soma de `share` de todos os participantes no payload DEVE ser exatamente 100.
- **fixed:** A soma de `share` de todos os participantes DEVE ser exatamente igual ao `amount` total da despesa.

## 3. Algoritmo de Quitação / Simplificação de Dívidas
- O cálculo de `/trips/{tripId}/balances` deve gerar o saldo líquido por participante (`net`) e retornar a lista mínima de transferências necessárias para quitar as dívidas do grupo.