# BudgetApp

Aplikacja do zarządzania osobistym budżetem oraz wspólnymi wydatkami w grupach. Backend obsługuje rejestrację użytkowników, transakcje indywidualne, grupy z członkami, rozliczanie długów między uczestnikami oraz powiadomienia w czasie rzeczywistym.

## Stack (backend)

- Java 25, Spring Boot 4
- Spring Security + JWT
- Spring Data JPA (Hibernate)
- MySQL 8
- GraphQL (`/graphql`)
- WebSocket (`/ws/group-notifications`)
- Maven, Lombok

## Funkcjonalności

### Osobisty budżet

- CRUD transakcji (przychód / wydatek) przypisanych do zalogowanego użytkownika
- Pola: kwota, typ, tagi, notatki, znacznik czasu
- Obliczanie salda (przychody, wydatki, bilans) z opcjonalnym filtrem po liczbie dni

### Grupy i członkostwo

- Tworzenie grup z właścicielem
- Dodawanie członków po adresie e-mail (tylko właściciel)
- Usuwanie członków (właściciel; właściciela nie można usunąć)
- Usuwanie grupy (tylko właściciel; kaskadowe czyszczenie członkostw i długów)

### Długi w grupach

- Ręczne tworzenie długu między dwoma członkami grupy
- Automatyczne generowanie długów przy transakcji grupowej — kwota dzielona równo między wybranych uczestników
- Dla wydatku (`EXPENSE`): wierzyciel = płacący, dłużnik = pozostali uczestnicy
- Dla przychodu (`INCOME`): odwrotnie
- Proces spłaty: dłużnik oznacza jako opłacone → wierzyciel potwierdza
- Operacje na długach dostępne dla właściciela grupy lub uczestników danego długu

### Powiadomienia WebSocket

- Po dodaniu wspólnego wydatku grupa otrzymuje powiadomienie z informacją o swojej części kwoty
- Połączenie wymaga tokena JWT przekazanego przy handshake

## API

### REST

| Endpoint | Opis |
|---|---|
| `POST /api/auth/register` | Rejestracja użytkownika |
| `POST /api/auth/login` | Logowanie, zwraca JWT |
| `GET /api/info` | Informacje o aplikacji |
| `GET/POST/PUT/DELETE /api/transactions` | CRUD transakcji osobistych (wymaga JWT) |

### GraphQL

Endpoint: `POST /graphql` (nagłówek `Authorization: Bearer <token>` dla operacji chronionych)

**Query:** `transactions`, `userBalance`, `groups`, `myGroups`, `groupMembers`, `groupDebts`

**Mutation:** `addTransaction`, `updateTransaction`, `deleteTransaction`, `createGroup`, `deleteGroup`, `addMember`, `removeMember`, `createDebt`, `deleteDebt`, `markDebtAsPaid`, `confirmDebtPayment`, `addGroupTransaction`

Schemat: `server/src/main/resources/graphql/schema.graphql`

### WebSocket

- `ws://localhost:8080/ws/group-notifications?token=<JWT>`

## Model danych

```
User ──< Transaction
User ──< Membership >── Group
User ──< Debt (jako debtor / creditor) >── Group
```

Encje: `User`, `Transaction`, `Group`, `Membership`, `Debt`

## Struktura backendu

```
server/src/main/java/pk/bp/pasir_pietras_bafrtlomiej/
├── config/          # Security, CORS, WebSocket, GraphQL
├── controller/      # REST (auth, transactions)
├── controller/graphql_controllers/
├── service/         # Logika biznesowa
├── repository/      # Spring Data JPA
├── model/           # Encje JPA
├── dto/             # Obiekty transferu
├── security/        # JWT filter, JwtUtil
├── websocket/       # Handler powiadomień
└── exception/       # Obsługa błędów
```

Warstwa serwisów weryfikuje uprawnienia (właściciel grupy, członek grupy, właściciel transakcji) i rzuca `AccessDeniedException` / `EntityNotFoundException` przy naruszeniu reguł.

## Uruchomienie

### Wymagania

- JDK 25
- Maven
- Docker (opcjonalnie, do bazy MySQL)

### Konfiguracja `.env`

W katalogu `server/` utwórz plik `.env` (format `klucz=wartość`, bez cudzysłowów). Plik jest wczytywany przez:

- **Spring Boot** — `application.properties` importuje go przez `spring.config.import=optional:file:.env[.properties]`
- **Docker Compose** — `./server/.env` (root) lub `server/docker/docker-compose.yml` (`../.env`)

Przykładowa zawartość (wartości deweloperskie):

```env
MYSQL_DATABASE=budgetapp
MYSQL_USER=budgetapp
MYSQL_PASSWORD=budgetapp_dev
MYSQL_ROOT_PASSWORD=root_dev_password
MYSQL_CONTAINER_NAME=budgetapp-mysql
TZ=Europe/Warsaw
JWT_SECRET=pasir_jwt_secret_key_for_development_only_must_be_at_least_64_bytes_long_12345
```

| Zmienna | Opis | Wymagana |
|---|---|---|
| `MYSQL_DATABASE` | Nazwa bazy danych tworzonej w MySQL | tak |
| `MYSQL_USER` | Użytkownik MySQL z dostępem do `MYSQL_DATABASE` | tak |
| `MYSQL_PASSWORD` | Hasło użytkownika `MYSQL_USER` | tak |
| `MYSQL_ROOT_PASSWORD` | Hasło roota MySQL (healthcheck Dockera) | tak |
| `MYSQL_CONTAINER_NAME` | Nazwa kontenera MySQL (`server/docker/docker-compose.yml`) | tak (Docker) |
| `TZ` | Strefa czasowa kontenera MySQL, np. `Europe/Warsaw` | nie |
| `JWT_SECRET` | Sekret do podpisywania tokenów JWT (algorytm HS512) | tak |

**Uwagi:**

- `JWT_SECRET` musi mieć **co najmniej 64 bajty** — inaczej backend nie wystartuje (`JwtUtil`).
- W produkcji ustaw silne, unikalne hasła i losowy sekret JWT (np. `openssl rand -base64 64`).
- Plik `.env` jest w `.gitignore` — nie commituj go do repozytorium.
- Przy uruchamianiu backendu lokalnie (`mvn spring-boot:run`) uruchamiaj polecenie z katalogu `server/`, żeby Spring znalazł `.env`.

### Baza danych

Uruchom MySQL:

```bash
cd server/docker
docker compose --env-file ../.env up -d
```

Cały stack (MySQL + backend + frontend) z katalogu głównego:

```bash
docker compose up -d
```

### Backend

```bash
cd server
mvn spring-boot:run
```

Aplikacja startuje domyślnie na `http://localhost:8080`. Hibernate aktualizuje schemat bazy (`ddl-auto=update`).

### Testy

```bash
cd server
mvn test
```

Testy integracyjne używają profilu `test` z bazą H2 w pamięci. Pokrywają m.in. autentykację REST i operacje GraphQL na grupach.

## Frontend

Klient w React (Vite) służy jako interfejs do backendu — nie jest głównym zakresem projektu. Dostępne ekrany:

- logowanie i rejestracja
- strona główna
- dodawanie transakcji, lista transakcji, pasek salda
- grupy (tworzenie, członkowie, transakcje grupowe)
- długi w grupie
- powiadomienia WebSocket (toast)

Uruchomienie: `cd client && npm install && npm run dev` (port `5174`).
