## TFTP Server & Client (Java, TCP)

An end‑to‑end implementation of an extended TFTP (Trivial File Transfer Protocol) server and client over TCP. It includes a multi‑client server (Thread‑Per‑Client pattern) and an interactive client with a keyboard thread and a listening thread. The system supports login, upload, download, directory listing, delete, disconnect, and server‑initiated broadcasts when files are added or removed.

### Technical Significance
- Demonstrates design and implementation of a custom binary protocol over TCP with Big‑endian encoding.
- Showcases concurrent server architecture, safe I/O, and message framing/decoding.
- Provides a practical, production‑like client/server system that exercises networking, concurrency, and state management.

---

## Features
- **Protocol coverage**: `LOGRQ`, `RRQ`, `WRQ`, `DIRQ`, `DELRQ`, `DISC`, `DATA`, `ACK`, `ERROR`, `BCAST`.
- **Bi‑directional messaging** via `Connections` API to target specific clients and to broadcast to all logged‑in clients.
- **Thread‑Per‑Client (TPC) server** with a `BlockingConnectionHandler` per client.
- **Binary protocol** with explicit framing and Big‑endian numeric fields.
- **Client with two threads**: keyboard input and listener for server packets.
- **File system integration**: uses the server `Files/` directory as the authoritative store.

---

## Architecture

### Server (Java, Maven)
- `Server`/`BaseServer`: bootstraps the TPC server, accepts sockets, and wires a `BlockingConnectionHandler` per connection.
- `BlockingConnectionHandler`: per‑client runnable responsible for socket I/O and invoking protocol/encdec.
- `Connections`/`ConnectionsImpl`: registry that maps `connectionId → ConnectionHandler` for p2p sends and broadcasts.
- `BidiMessagingProtocol<byte[]>`: protocol SPI; implemented by `tftp.TftpProtocol`.
- `MessageEncoderDecoder<byte[]>`: SPI; implemented by `tftp.TftpEncoderDecoder`.
- `tftp.holder`: in‑memory state for logged‑in users and files.

### Client (Java, Maven)
- `tftp.TftpClient`: interactive client core with send/receive loops, flow control, and file I/O.
- `tftp.TftpClientEncDec`: client‑side codec for framing/deframing protocol packets.
- `tftp.TftpClientMain`: launches client and starts keyboard/listener threads.

### Data flow highlights
- **Upload (WRQ)**: client sends `WRQ`, waits for `ACK 0`, streams file in 512‑byte `DATA` packets; server ACKs each block; client prints completion.
- **Download (RRQ)**: client sends `RRQ`; server streams `DATA` packets; client ACKs each block; writes file to disk upon completion.
- **DIRQ**: server returns newline‑separated filenames via `DATA` packets; client prints them.
- **Delete (DELRQ)**: server deletes file, ACKs requester, and sends `BCAST del <filename>` to all logged‑in clients.
- **Login/Disconnect**: `LOGRQ` registers a unique username; `DISC` cleanly removes user and closes connection after `ACK 0`.

---

## Repository layout
```text
Skeleton/
  server/
    src/main/java/bgu/spl/net/api/               # Protocol/codec SPIs
    src/main/java/bgu/spl/net/srv/                # TPC server infra + Connections registry
    src/main/java/bgu/spl/net/impl/tftp/          # TFTP protocol + encoder/decoder + server main
    Files/                                        # Server data directory (seeded with sample files)
    pom.xml
  client/
    src/main/java/bgu/spl/net/impl/tftp/          # Client core, codec, and main
    pom.xml
```

---

## Build & Run

Prerequisites: Java 8+, Maven 3.8+

### Server
```bash
cd Skeleton/server
mvn -q compile
mvn -q exec:java -Dexec.mainClass="bgu.spl.net.impl.tftp.TftpServer" -Dexec.args="7777"
```
Notes:
- The server listens on port `7777` in the current implementation.
- The initial file set is loaded from `Skeleton/server/Files/` on startup.

### Client
```bash
cd Skeleton/client
mvn -q compile
mvn -q exec:java -Dexec.mainClass="bgu.spl.net.impl.tftp.TftpClientMain" -Dexec.args="<server-host> 7777"
```
Notes:
- The provided `TftpClientMain` defaults to host `cs302six5-4-lnx` and port `7777` if no args are supplied.
- Once running, type commands in the client terminal as shown below.

---

## Example session (client commands)
```text
LOGRQ alice
DIRQ
WRQ A.txt
RRQ "this is a file with spaces.txt"
DELRQ A.txt
DISC
```
Expected console prints include `ACK <n>`, `BCAST add|del <filename>`, and final transfer confirmations:
```text
WRQ A.txt complete
RRQ B.txt complete
```

---

## Implementation highlights
- **Message framing**: a stateful encoder/decoder collects bytes until a full packet is assembled; numerics are parsed in Big‑endian.
- **Backpressure/flow control**: client waits (`wait/notify`) between request and completion/next‑step acknowledgement to keep transfers ordered.
- **Broadcasts**: server emits `BCAST` to all logged‑in clients on file add/delete events.
- **Thread safety**: shared registries use `ConcurrentHashMap`; socket writes are synchronized per connection handler.
- **File I/O**: uses `Files.readAllBytes` for transfers and streams to write downloads to disk; server `Files/` is the canonical store.

---

## Skills and tools
- **Java 8**: concurrency, I/O streams, NIO files.
- **Maven**: per‑module build and execution.
- **Networking**: TCP sockets, Thread‑Per‑Client server.
- **Binary protocols**: Big‑endian encoding, UTF‑8 strings with zero terminators, message framing.
- **Software design**: clean interfaces (`Connections`, `BidiMessagingProtocol`, `MessageEncoderDecoder`), separation of concerns, state management.

---

## Credits
- Yuval Levy
- Tomer Faran

