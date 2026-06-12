# 📖 The Distributed Ledger Novel (Collaborative Book App)

A real-time, multi-user collaborative writing engine where an infinite book is composed word-by-word. This application bypasses traditional blob/text-storage paradigms by rendering a single continuous book using an un-fractured **Distributed Linear Linked List Map** across a Spring Boot microservice mesh and a reactive Vue 3 client.

---

## 🛠 Tech Stack

* **Frontend:** Vue 3 (Composition API), Pinia (State Management), Axios, `@stomp/stompjs` (WebSocket/STOMP Client).
* **Backend:** Java 17, Spring Boot, Spring WebSockets, Spring Data JPA, Java Scheduling.
* **Database:** PostgreSQL (Optimized for unique pointers and self-referential foreign relationships).

---

## 🧠 Core System Architecture & Mechanics

### 1. Data Structure: Word-Level Linked Splicing
Unlike a traditional document database that stores strings, markdown blocks, or array lists, every single word in this book is an independent record in a self-referential graph. 
* Words contain content restricted up to **30 characters**.
* Each word maintains absolute `nextWord` and `previousWord` self-referential database constraints.
* **Optimistic UI Splicing:** When a user inserts a word mid-sentence, the Vue layout uses a transient tracking identifier (`localId`) to splice the item into view instantly for zero latency. Concurrently, a message is routed down the WebSocket pipeline to let the transactional database handle atomic list-pointer re-routing.

### 2. The 10-Minute Consolidation Loop (Page Horizons)
Pages do not possess a hard character or layout ceiling while being actively modified.
* **The Uncapped State:** During active typing, pages expand dynamically in memory. Users view data broken down into page arrays dynamically computed by a head-anchor pointer.
* **The Consolidation Stop:** Every **10 minutes**, the Spring Boot server initiates an exclusive database consolidation process, temporarily halting incoming socket connections for a short few seconds. The consolidation engine finds the starting word for each page, steps through the text graph, calculates character weights (~2,000 characters per page boundaries), and anchors hard `startWord` and `endWord` foreign keys to compile a permanent Page Entity Linked List.

### 3. Credit-Based Self-Moderation Topology
The platform self-moderates through an automated economy balancing literary contribution against editorial control:
* **Earning Credits:** Authors accumulate **Delete Credits** dynamically when their contributed words receive community `Likes`. 
* **Volatile Deletion Costs:** The cost to delete an offensive or out-of-context word fluctuates using a statistical algorithmic distribution. Words with high `Dislike` ratios shift downward across a uniform distribution curve. As a word moves multiple standard deviations ($\sigma$) away from the mean ($\mu$) popularity score of the active page, its deletion cost drops exponentially, allowing verified high-tier contributors to clear out spam effortlessly.

---

## 📡 The Real-Time Network Pipeline

We configured a low-latency bidirectional pipeline that bypasses standard REST overhead for real-time writing synchronization:

* **Inbound Messaging Broker:** The Vue layer bypasses the `/api` handler route, establishing a native secure `wss://` handshake directly to the root socket broker endpoint `/gs-guide-websocket` to pass Spring Boot origin checks.
* **Dynamic Room Subscriptions:** Pinia monitors active routing states via `fetchPage(id)`. When a user changes pages, the store cleanly unsubscribes from the old page cluster and locks into a page-specific room: `/topic/page/{currentPageId}`.
* **Cross-Page Ripple Engine:** Because boundary elements have layout dependencies, editing the absolute head or tail of a page ripples across multiple browser rooms. The backend monitors edge insertions and instantly distributes state-patch packets across multiple channels to maintain pointer synchronization for concurrent users watching neighboring pages:
    * `NEXT_PAGE_HEAD_CHANGED`: Dispatched backwards to update the previous page's outbound tracking parameters (`nextPageFirstWordId`).
    * `PREVIOUS_PAGE_TAIL_CHANGED`: Dispatched forwards to update the next page's inbound fallback indicators (`lastWordIdOfPreviousPage`).

---

## 🗄 Database Model Blueprint

### Word Schema
```text
Table "public.word"
     Column      |         Type          | Modifiers
-----------------+-----------------------+-----------
 id              | bigint                | not null (Primary Key)
 local_id        | character varying(255)| not null
 content         | character varying(30) | not null
 next_word_id    | bigint                | (Foreign Key -> word.id, Unique)
 like_count      | integer               | default 0
 dislike_count   | integer               | default 0
 author_id       | bigint                | (Foreign Key -> author.id)
 ```

### Page Schema
```Plaintext
Table "public.page"
           Column             |  Type  | Modifiers
------------------------------+--------+-----------
 id                           | bigint | not null (Primary Key)
 first_word_id                | bigint | (Foreign Key -> word.id)
 last_word_id                 | bigint | (Foreign Key -> word.id)
 last_word_id_of_previous_page| bigint | (Cache / Fallback Pointer)
```

### Author Schema
```Plaintext
Table "public.author"
           Column             |  Type  | Modifiers
------------------------------+--------+-----------
 id                           | bigint | not null (Primary Key)
 name                         | varchar| not null
 total_likes                  | bigint | default 0
 total_dislikes               | bigint | default 0
 total_words_in_book          | bigint | default 0
```

## 💻 Frontend State Flow
User Action: User clicks a + boundary split slot, types a word, and hits enter.

1. Optimistic Projection: Plus.vue generates a tracking localId, emits the text, and the reactive template renders the word instantly.

2. Socket Dispatch: The payload travels via STOMP down /app/test-word, sending the whole layout parameter set: content, currentPageId, localId, previousWordId, nextWordId, and previousLocalId.

3. Backend Processing: WordWebSocketController unpacks the parameters, executes wordService.createWord(), commits the transactional row to PostgreSQL, and broadcasts the permanent, database-verified Word entity out to the active channel.

4. State Sync: Neighboring clients catch the entity, pass it through pageStore.insertWordIntoRecords(), modify the local Pinia memory linked-list tree, and a Vue deep watcher triggers a clean, instant UI re-render with zero slow-load animation flickers.