## OLMS Java Version

This directory contains a Java re-implementation of the original C socket-based Online Library Management System. The Java version preserves the same user/customer flows while providing a safer storage layer, richer protocol, and cross-platform build experience.

### Project layout

- `src/main/java/com/olms/model`: data classes (`Book`, `User`, `Rental`)
- `src/main/java/com/olms/storage`: `LibraryStore` file-backed repository (`data/*.db`)
- `src/main/java/com/olms/server`: multithreaded TCP server (`LibraryServer`)
- `src/main/java/com/olms/client`: console client (`LibraryClient`)
- `data`: human-readable database files created on first run

### Building

Use any JDK 8+:

```powershell
cd "Java Version"
javac -d out src/main/java/com/olms/model/*.java `
    src/main/java/com/olms/storage/*.java `
    src/main/java/com/olms/server/*.java `
    src/main/java/com/olms/client/*.java
```

This compiles classes into `Java Version/out`.

### Running

Start the server (uses port 8080 and `Java Version/data` by default):

```powershell
cd "Java Version"
java -cp out com.olms.server.LibraryServer
```

Then launch any number of interactive clients from separate terminals:

```powershell
cd "Java Version"
java -cp out com.olms.client.LibraryClient
```

Both commands accept optional arguments:

- Server: `java -cp out com.olms.server.LibraryServer <port> <dataDir>`
- Client: `java -cp out com.olms.client.LibraryClient <host> <port>`

On first launch a default admin account (`admin` / `admin`) is created automatically.

### Symbolic Execution (SPF)

To explore the multithreaded `RETURN_BOOK` flow under Symbolic Pathfinder:

1. Compile the example sources so the driver lands in `build/examples`:
   ```powershell
   cd jpf-symbc
   .\gradlew.bat compileExamplesJava
   ```
2. Run SPF with the provided configuration:
   ```powershell
   .\bin\jpf.bat src/examples/javaLib/ReturnBookSymbolic.jpf
   ```

The configuration targets `com.olms.symbolic.ReturnBookSymbolicDriver`. The driver keeps the
real `LibraryStore` API concrete, while symbolic booleans/integers control:

- which customer ID each concurrent thread uses (valid renter, other customer, or fresh account)
- which book identifier the thread attempts to return (the rented book, an unrented catalog entry, or a missing id)
- whether the catalog entry disappears before the return (simulating data corruption)

SPF explores the different schedules of the two return threads, covering outcomes such as
successful returns, duplicate return attempts, invalid customer/book combinations, and the
“Book deleted from catalog” edge case.

