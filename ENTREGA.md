# Entrega — Tuckersoft Branch Engine

## Estrellas obtenidas
<pegar aquí la salida de `cd autotests && ./mvnw test`>

## Flujo asíncrono implementado
<explicación breve: DecisionService publica DecisionCommittedEvent → 201 inmediato →
AFTER_COMMIT → BranchNotificationListener (@Async branchExecutor, REQUIRES_NEW) →
email + RealityLog>

## Lo que no se completó
<si aplica>
