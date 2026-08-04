# Project Instructions

## Shared database workflow

- The runtime database remains team2026.
- sql/teamobi2026.sql is the canonical fresh-install bundle shared by the game server and web2026.
- Every game, shared, or web database change must be added as a new timestamped migration under sql/migrations/ and the exact migration must also be appended to sql/teamobi2026.sql between named BEGIN MIGRATION and END MIGRATION markers.
- Prefer rerunnable migrations using guarded DDL and idempotent data changes.
- Never add new changes to sql/archive/nro1.sql.
- Apply only the new migration to an existing database. Never import the full bundle over a live database.
- Validate the full bundle with a fresh temporary MariaDB database after changing it. Validate the standalone migration separately when it changes existing data.
