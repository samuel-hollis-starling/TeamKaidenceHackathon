# Backend

## Stack

- Java 17
- Jersey 3.1.5 (JAX-RS) — REST framework, uses `jakarta.*` namespace (not `javax.*`)
- Guice 7.0.0 — dependency injection
- Grizzly — embedded HTTP server on `http://localhost:8080`
- Jackson — JSON serialisation (via `jersey-media-json-jackson`)

## Running

```sh
./gradlew run
```

## Project layout

```
src/main/java/com/starlingbank/
├── Main.java               — starts Grizzly, registers resources + HK2 bridge
├── AppModule.java          — Guice bindings
├── api/                    — Jersey resource classes (endpoints)
├── model/                  — POJO model classes
└── service/                — service interfaces + implementations
```

## Adding a new endpoint

1. Create a resource class in `com.starlingbank.api/`:
   ```java
   @Path("/api/your-path")
   public class YourResource {
       @Inject
       public YourResource(YourService service) { ... }

       @GET
       @Produces(MediaType.APPLICATION_JSON)
       public YourModel getThings() { ... }
   }
   ```

2. Register the resource in `Main.java`:
   ```java
   config.register(YourResource.class);
   ```

3. If the resource injects a service, add the binding to the `AbstractBinder` in `Main.java`:
   ```java
   bind(injector.getInstance(YourService.class)).to(YourService.class);
   ```

4. Add the class to `build.gradle.kts` so it appears in the TypeScript client:
   ```kotlin
   classes = mutableListOf(
       // existing entries...
       "com.starlingbank.api.YourResource"
   )
   ```

5. Run `./gradlew generateTypeScript` to regenerate `frontend/src/generated/api.ts`.

## Guice + HK2 bridge

Jersey uses HK2 for its own DI. Guice manages the service graph. The bridge in `Main.java` allows Jersey to resolve `@Inject` constructors in resource classes using Guice-managed instances:

```java
config.register(new AbstractBinder() {
    @Override
    protected void configure() {
        bind(injector.getInstance(SomeService.class)).to(SomeService.class);
    }
});
```

Every service that a resource class `@Inject`s must have a binding here.

## Service implementations

Services are singletons, bound in `AppModule.java`:
```java
bind(FloorMapService.class).to(FloorMapServiceImpl.class).in(Singleton.class);
```

Services load data from `input-data/` at construction time. If a file is missing, the service throws at startup (fail-fast).

## Models

All model classes need:
- A no-arg constructor (for Jackson deserialisation)
- Getters for all fields (for Jackson serialisation)
- No Lombok — write these manually

Use `@JsonInclude(JsonInclude.Include.NON_NULL)` on classes with nullable fields (e.g. `Desk.neighborhood`).

See [models.md](models.md) for the full model list.
