# Cocos Challenge

API de una cuenta de inversión. Informa el portfolio, busca instrumentos y recibe órdenes de compra y venta. No simula el mercado y no autentica usuarios.

## Arquitectura

Hexagonal, en cuatro paquetes:

- `domain` guarda el modelo y las reglas. No usa Spring ni JDBC.
- `application` son los casos de uso. Ahí se abre la transacción.
- `adapter.web` es la entrada HTTP.
- `adapter.persistence` es Postgres.

## Día de mercado

`marketdata` tiene precios del 13 y del 14 de julio de 2023. El día no sale del reloj del servidor: el cliente lo envía como `datetime` para poder elegir uno de esos dos días.

El día de mercado es la fecha de calendario de ese `datetime` en `America/Argentina/Buenos_Aires`. El `close` y el `previousClose` se leen de la fila de `marketdata` con esa `date`.

El rendimiento diario es `(close - previousClose) / previousClose`, expresado en porcentaje, igual que el rendimiento total de la posición. Si no hay fila para ese día, o `previousClose` es 0, el dato va vacío.

## Cuenta

`orders` es el libro. La foto de la cuenta evita recorrerlo en cada pedido:

- `account_cash` guarda `settled_cash` y `reserved_cash` de un usuario.
- `positions` guarda, por usuario e instrumento, `quantity`, `cost_basis` y `reserved_quantity`.

`cost_basis` es el costo total en pesos de las acciones que siguen abiertas. El promedio es `cost_basis / quantity` y no se persiste. El valor de mercado y los rendimientos se calculan al leer, con el `close` del día pedido. Un cambio de precio no actualiza `positions`.

Los pesos disponibles son `settled_cash - reserved_cash`. Las acciones disponibles son `quantity - reserved_quantity`.

| Orden | Efecto en la foto |
|---|---|
| `CASH_IN` `FILLED` | suma `size` a `settled_cash` |
| `CASH_OUT` `FILLED` | resta `size` de `settled_cash` |
| `BUY` `MARKET` `FILLED` | resta el nocional de `settled_cash`; `quantity` suma `size` y `cost_basis` suma el nocional |
| `SELL` `MARKET` `FILLED` | suma el nocional a `settled_cash`; `quantity` resta `size` y `cost_basis` descuenta la parte proporcional |
| `BUY` `LIMIT` `NEW` | suma el nocional a `reserved_cash` |
| `SELL` `LIMIT` `NEW` | suma `size` a `reserved_quantity` |
| `REJECTED` | no toca la foto |
| `BUY` `NEW` cancelada | resta el nocional de `reserved_cash` |
| `SELL` `NEW` cancelada | resta `size` de `reserved_quantity` |

El nocional es `size × price`. En una orden a mercado, `price` es el `close` del día pedido. En una límite, es el precio enviado. `size` es la cantidad de esa orden. `quantity` es la tenencia acumulada. Si el cliente envía un monto en pesos, `size` es `floor(monto / price)`.

Una `MARKET` se inserta `FILLED` o `REJECTED`. Una `LIMIT` se inserta `NEW` o `REJECTED`. Solo se cancela una orden `NEW`, y pasa a `CANCELLED`. Las transferencias no tienen endpoint: ya están en `orders`. Si `account_cash` está vacía al arrancar, la aplicación recorre las órdenes por fecha y aplica el estado guardado. Una `FILLED` mueve la caja y la posición, también un límite que ya quedó ejecutado y una venta que deja la posición en corto. Una `NEW` reserva. `REJECTED` y `CANCELLED` no cambian la foto, porque el alta y la cancelación ya se compensaron en esa fila.

Una orden de negocio que no se puede ejecutar se guarda `REJECTED` y responde 201. Si el `userId` o el `instrumentId` no existen, o el instrumento no es una acción, la respuesta es 404 y no se inserta nada.

## Concurrencia

Al crear o cancelar una orden, la transacción bloquea la fila de `account_cash` de ese usuario con `SELECT … FOR UPDATE`. Otra orden de la misma cuenta espera y lee los saldos ya actualizados. Usuarios distintos no se bloquean. Leer el portfolio no toma ese lock.

## Base

El esquema se versiona con Flyway. La base provista ya tiene `users`, `instruments`, `orders` y `marketdata`: esa foto es el baseline y no se vuelve a crear en Neon. Las migraciones siguientes agregan `account_cash`, `positions` y los índices. Una base vacía, como la de los tests, corre también el baseline.

## Arranque

Requiere JDK 21. La conexión no va en el repo. Para apuntar a Neon usá el host directo, sin `-pooler`, porque Flyway toma un lock de sesión.

```bash
export DB_URL='jdbc:postgresql://HOST:5432/neondb?sslmode=require'
export DB_USERNAME='neondb_owner'
export DB_PASSWORD='***'
./mvnw test
./mvnw spring-boot:run
```

`./mvnw test` no usa esa base: levanta un Postgres vacío y corre las dos migraciones.
