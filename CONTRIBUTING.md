# Contributing to Opportunity

Thank you for your interest in contributing to **Opportunity**!

## Development Principles

1. **Offline-First Core**: Any new feature in `:app` must function cleanly without network access, an Anthropic API key, or a running sync backend.
2. **Database Schema Discipline**: Changes to Room entities in `app/src/main/java/com/opportunity/app/data/local/entity/` must include an explicit `Migration` in `OpportunityDatabase.kt` and be registered in `DatabaseModule.kt`.
3. **Calm Visual Language**: UI additions should follow the warm neutral palette (`Color.kt`), `AccentPalette.kt` single-accent philosophy, and `GlassCard` surfaces.
4. **R8 / ProGuard Compatibility**: New Retrofit interfaces, `@Serializable` models, or Glance receivers should be verified with `./gradlew :app:assembleRelease`.

## Pull Request Checklist

Before opening a pull request, please verify:

- [ ] Android debug builds succeed: `./gradlew :app:assembleDebug :wear:assembleDebug`
- [ ] Unit tests pass: `./gradlew testDebugUnitTest`
- [ ] Release R8 build succeeds if modifying models/APIs/widgets: `./gradlew :app:assembleRelease`
- [ ] Backend TypeScript compiles cleanly if touching `backend/`: `cd backend && npm run build`
- [ ] No secrets (`local.properties`, `.env`, API keys) are committed.
