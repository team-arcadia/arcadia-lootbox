# Contributing to Arcadia LootBox / Contribuer

Thank you for your interest in contributing! | Merci de votre interet !

## Getting Started / Pour commencer

### Prerequisites / Prerequis
- Java 21 (Temurin recommended)
- Gradle 8.7+
- NeoForge MDK knowledge
- [Arcadia Lib](https://github.com/Team-Arcadia) source (for API reference)

### Setup / Installation
```bash
git clone https://github.com/Team-Arcadia/Arcadia-LootBox.git
cd Arcadia-LootBox
./gradlew build
```

Place `arcadia-lib-1.2.0.jar` in the `libs/` folder before building.

## Code Conventions / Conventions de code

### Language
- **Code, variables, logs**: English only
- **Comments**: English, minimal
- **UI text**: Must support both EN and FR translations

### Style
- **Naming**: `PascalCase` for classes, `camelCase` for methods/fields
- **Indentation**: 4 spaces
- **Max line length**: 120 characters (soft limit)

### Architecture Rules
- Use `ItemBuilder` from Arcadia Lib for all `ItemStack` creation
- Use `ArcadiaMessages` for chat messages (consistent prefix/styling)
- Use `PermissionHelper` for permission checks (soft LuckPerms)
- Use `SchedulerService` for delayed/repeating tasks
- Thread-safe collections (`ConcurrentHashMap`) for shared state
- Never block the server thread

### Commit Messages
Follow [Conventional Commits](https://www.conventionalcommits.org/):
```
feat: add new feature
fix: resolve bug
refactor: restructure code
docs: update documentation
perf: improve performance
```

## Branch Strategy / Strategie de branches

```
main          Production-ready releases (protected)
  |
develop       Active development, feature integration
  |
feat/*        Feature branches (from develop)
fix/*         Bug fix branches (from develop)
hotfix        Critical production fixes (from main)
```

| Branch | Purpose | Merges into |
|--------|---------|-------------|
| `main` | Stable releases, tagged versions | - |
| `develop` | Feature integration, daily work | `main` |
| `feat/*` | New features | `develop` |
| `fix/*` | Bug fixes | `develop` |
| `hotfix` | Critical production patches | `main` + `develop` |

## Pull Request Process / Processus de PR

1. Fork the repository / Forkez le repo
2. Create a feature branch from `develop`: `git checkout -b feat/my-feature develop`
3. Make your changes
4. Ensure `./gradlew build` passes
5. Submit a PR against `develop`

## Reporting Issues / Signaler des problemes

Use the [issue templates](https://github.com/Team-Arcadia/Arcadia-LootBox/issues/new/choose) for bug reports and feature requests.

## Community / Communaute

- [Discord](https://discord.gg/xjF8Rtzyd4) - Best place for questions
- [Website](https://arcadia-echoes-of-power.fr/)
