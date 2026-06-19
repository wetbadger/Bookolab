# 📖 The Distributed Ledger Novel (Collaborative Book App)

A real-time, multi-user collaborative writing engine where an infinite book is composed word-by-word. This application bypasses traditional blob/text-storage paradigms by rendering a single continuous book using an un-fractured **Distributed Linear Linked List Map** across a Spring Boot microservice mesh and a reactive Vue 3 client. Linked lists are used because insertion time is O(1) and there could be a lot of insertions happening simultaneously.

---

## 🛠 Tech Stack

* **Frontend:** Vue 3 (Composition API), Pinia (State Management), Axios, `@stomp/stompjs` (WebSocket/STOMP Client).
* **Backend:** Java 25, Spring Boot 4, Spring WebSockets, Spring Data JPA, Java Scheduling.
* **Database:** PostgreSQL (Optimized for unique pointers and self-referential foreign relationships).

---

## 🧠 Core System Architecture & Mechanics

### 1. Data Structure: Word-Level Linked Splicing
Unlike a traditional document database that stores strings, markdown blocks, or array lists, every single word in this book is an independent record in a self-referential graph. 
* Words contain content restricted up to **30 characters**.
* Each word maintains absolute `nextWord` self-referential database constraint.
* **Optimistic UI Splicing:** When a user inserts a word mid-sentence, the Vue layout uses a transient tracking identifier (`localId`) to splice the item into view instantly for zero latency. Concurrently, a message is routed down the WebSocket pipeline to let the transactional database handle atomic list-pointer re-routing.

### 2. The 10-Minute Consolidation Loop (Page Horizons)
Pages do not possess a hard character or layout ceiling while being actively modified.
* **The Uncapped State:** During active typing, pages expand dynamically in memory. Users view data broken down into page arrays dynamically computed by a head-anchor pointer.
* **The Consolidation Stop:** Every **10 minutes**, the Spring Boot server initiates an exclusive database consolidation process, temporarily halting incoming socket connections for a short few seconds. The consolidation engine finds the starting word for each page, steps through the text graph, calculates character weights (~2,000 characters per page boundaries), and anchors hard `startWord` and `endWord` foreign keys to compile a permanent Page Entity Linked List.

### 3. Credit-Based Self-Moderation Topology
The platform self-moderates through an automated economy balancing literary contribution against editorial control:
* **Earning Credits:** Authors accumulate **Delete Credits** dynamically when their contributed words receive community `Likes`.
* **Leaderboard:** A leaderboard displays the authors with the most likes.

---

## 📡 The Real-Time Network Pipeline

We use WebSockets for real-time writing synchronization:

* **Inbound Messaging Broker:** The Vue layer bypasses the `/api` handler route, establishing a native secure `wss://` handshake directly to the root socket broker endpoint `/gs-guide-websocket` to pass Spring Boot origin checks.
* **Dynamic Room Subscriptions:** Pinia monitors active routing states via `fetchPage(id)`. When a user changes pages, the store cleanly unsubscribes from the old page cluster and locks into a page-specific room: `/topic/page/{currentPageId}`.
* **Cross-Page Ripple Engine:** Because boundary elements have layout dependencies, editing the absolute head or tail of a page ripples across multiple browser rooms. The backend monitors edge insertions and instantly distributes state-patch packets across multiple channels to maintain pointer synchronization for concurrent users watching neighboring pages:
    * `NEXT_PAGE_HEAD_CHANGED`: Dispatched backwards to update the previous page's outbound tracking parameters (`nextPageFirstWordId`).
    * `PREVIOUS_PAGE_TAIL_CHANGED`: Dispatched forwards to update the next page's inbound fallback indicators (`lastWordIdOfPreviousPage`).

---

## 🔒 Authentication & Security Archetype

The system relies on a decoupled, stateless **JWT Architecture** matched with top-down path authorization rules to protect write operations while preserving open readability across public channels.

### 1. Unified Backend Security Filter Chain
Spring Security handles validation inside a custom stateless execution chain. The system explicitly isolates browser preflight mechanisms from functional endpoints to enforce authorization without breaking decoupled environments:

* **Preflight Protection Fallback:** To facilitate cross-origin browser validation, the `CorsConfigurationSource` deploys a universal fallback mapping (`/**`) configured with credential capabilities (`setAllowCredentials(true)`).
* **Asynchronous JWT Extraction:** A custom `JwtAuthenticationFilter` intercepts HTTP traffic right before the standard `UsernamePasswordAuthenticationFilter`. The filter explicitly bypasses HTTP `OPTIONS` methods via lookahead short-circuiting to let the native CORS layer safely append `Access-Control-Allow-Origin` headers. For standard methods, it extracts the `Bearer` token string, validates the expiration against `JwtService`, and populates the `SecurityContextHolder` with `ROLE_USER` permissions.
* **Granular Rule Ordering:** Access rules are applied sequentially in a top-down priority hierarchy:
  1. `/auth/**` and `/gs-guide-websocket/**` are completely open (`permitAll()`).
  2. Sub-paths requiring write access (e.g., `/api/pages/*/edit`, `/authors/me`) are strictly bounded to `.authenticated()`.
  3. Structural data indexing (`GET /api/pages/*`) is publicly whitelisted to permit read-only navigation.
  4. Any residual fallback request is locked down by default via `.anyRequest().authenticated()`.

### 2. Client-Side Navigation Guards & Parameter Persistence
The Vue Router utilizes modern return-based architecture to shield authenticated routes (like `/me` and `/pages/:id/edit`) from unauthenticated traffic while retaining contextual state history:

* **State Initialization Synchronization:** The global `beforeEach` navigation guard intercepts protected targets (`requiresAuth`). If the local store is unverified but retains a raw storage token, it calls `authStore.fetchCurrentUser()` to resolve backend identity state asynchronously before evaluating access.
* **Interception Query Forwarding:** If authentication fails, the guard returns a redirection payload holding a reactive dynamic query context: `{ path: '/login', query: { redirectFrom: to.fullPath } }`.
* **Link Propagation:** The application wraps all toggle buttons and cross-auth references inside bounded paths (`:to="{ path: '/signup', query: route.query }"`) ensuring the destination fallback is passed between auth views seamlessly if a user hops back and forth between screens.

### 3. WebSocket Handshaking
Because asynchronous routing triggers faster than storage persistence cycles can settle, mounting page views instantly can drop the WebSocket authorization layer during page redirects.
The application solves this by wrapping socket generation inside an atomic reactive safety watcher:
* Upon mounting, `initializeSocketSafely()` checks the store for active token state.
* If a token is absent due to an ongoing redirect write cycle, it deploys a transient micro-watcher to stall initialization. The split-second `authStore.token` registers the committed backend signature, the watcher fires the connection, hooks the header configuration, and instantly self-destructs to prevent leak vulnerabilities.

---

## 🗄 Database Model Blueprint

### Word Schema
```Plaintext
                          Table "public.word"
    Column    |          Type          | Collation | Nullable | Default
--------------+------------------------+-----------+----------+---------
 author_id    | bigint                 |           |          |
 id           | bigint                 |           | not null |
 next_word_id | bigint                 |           |          |
 content      | character varying(30)  |           | not null |
 local_id     | character varying(255) |           | not null |
 ```

### Page Schema
```Plaintext
                   Table "public.page"
    Column     |  Type  | Collation | Nullable | Default
---------------+--------+-----------+----------+---------
 first_word_id | bigint |           |          |
 id            | bigint |           | not null |
 last_word_id  | bigint |           |          |
```

### Author Schema
```Plaintext
                              Table "public.author"
    Column     |              Type              | Collation | Nullable | Default
---------------+--------------------------------+-----------+----------+---------
 enabled       | boolean                        |           | not null |
 banned_until  | timestamp(6) without time zone |           |          |
 credits_spent | bigint                         |           | not null | 0
 id            | bigint                         |           | not null |
 username      | character varying(30)          |           | not null |
 password      | character varying(255)         |           | not null |
 role          | character varying(255)         |           | not null |
```

### Reaction Schema
```Plaintext
                                         Table "public.reaction"
    Column     |              Type              | Collation | Nullable |             Default
---------------+--------------------------------+-----------+----------+----------------------------------
 author_id     | bigint                         |           | not null |
 created_at    | timestamp(6) without time zone |           | not null |
 id            | bigint                         |           | not null | generated by default as identity
 word_id       | bigint                         |           | not null |
 reaction_type | character varying(10)          |           | not null |
```

## 💻 Frontend State Flow
User Action: User clicks a + boundary split slot, types a word, and hits enter.

1. Optimistic Projection: Plus.vue generates a tracking localId, emits the text, and the reactive template renders the word instantly.

2. Socket Dispatch: The payload travels via STOMP down /app/send-word, sending the whole layout parameter set: content, currentPageId, localId, previousWordId, nextWordId, and previousLocalId.

3. Backend Processing: WordWebSocketController unpacks the parameters, executes wordService.createWord(), commits the transactional row to PostgreSQL, and broadcasts the permanent, database-verified Word entity out to the active channel.

4. State Sync: Neighboring clients catch the entity, pass it through pageStore.insertWordIntoRecords(), modify the local Pinia memory linked-list tree, and a Vue deep watcher triggers a clean, instant UI re-render with zero slow-load animation flickers.


---

## 🚀 Setup Steps

1. Make sure a postgres service is running.

See the [postgres docs](https://www.postgresql.org/docs/) on how to do this.

2. Create a .env file in the root folder that looks like this:

```Plaintext
SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/bookolab_db
SPRING_DATASOURCE_USERNAME=postgres
SPRING_DATASOURCE_PASSWORD=local_password
CORS_ALLOWED_ORIGIN=http://localhost:5173
JWT_SECRET_KEY=abcd1234
```

3. Create a .env file in the src/collabbook directory that looks like this:

```Plaintext
VITE_API_BASE_URL=http://localhost:8080
```

4. Navigate to src/main/resources  and generate a private and public key 

```Plaintext
openssl genpkey -algorithm EC -pkeyopt ec_paramgen_curve:P-256 -out private.key
openssl pkey -in private.key -pubout -out public.key
```

5. Install

Linux or mac:
```Plaintext
export $(cat .env | xargs) && mvn clean install
```
Windows:
```
Get-Content .env | Foreach-Object {
    if ($_ -match '^[a-zA-Z_][a-zA-Z0-9_]*=') {                                                                                                  
        $name, $value = $_ -split '=', 2                                                                                                         
        [System.Environment]::SetEnvironmentVariable($name, $value, "Process")                                                                   
    }                                                                                                                                            
}
```
```Plaintext
mvn clean install
```
6. Start redis

```Plaintext
sudo docker run --name bookolab-redis -p 6379:6379 -d redis
```

7. Run

```Plaintext
./mvnw spring-boot:run
```

8. Navigate to src/booklab and run 
```Plaintext
npm install && npm run dev
```
