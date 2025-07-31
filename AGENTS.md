# Development Guidelines

- Follow a simple MVI architecture using `Flow<PluginState>` from the logic module.
- The logic module must not depend on IntelliJ SDK; only the UI module may use it.
- Use Compose for all UI components.
- Settings are stored using a repository interface in the logic module with an IntelliJ implementation in the UI module.
- Run `./gradlew build` before committing any changes.
