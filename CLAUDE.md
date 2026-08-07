# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build & Test Commands

```bash
# Build
./mvnw clean package

# Run locally (requires local postgres via docker-compose up)
./mvnw spring-boot:run

# Tests
./mvnw test
./mvnw test -Dtest=PostOutServiceTest                    # single class
./mvnw test -Dtest=PostOutServiceTest#testMethodName     # single method
```

## Working With Me

- I am learning backend development. When I'm implementing something
  for the first time, explain the approach and let me write it —
  don't generate the implementation unless I ask.
- Prefer reviewing my code over writing it. Tell me what's wrong and
  why, including things a senior dev would flag.
- When you do write code, explain the non-obvious parts. I should be
  able to explain every line in my repo.
- Ask before making multi-file changes.

## Local Dev Setup

- Java 21, Spring Boot 4.0.6, Maven wrapper included
- `docker-compose up` starts PostgreSQL 16 (localhost:5432/agentx)
- Required env vars: `AGENCY_BOT_DISCORD_TOKEN`, `WOW_UTILS_API_KEY`
- Production profile uses `application-production.properties` with Railway-style env vars (PGHOST, etc.)

## Architecture

WoW guild management Discord bot using **JDA 6.4.1** (Java Discord API) + Spring Boot.

**Core domain:** Members can manage "post-outs" (raid absence notifications). Officers receive scheduled Discord embed summaries.

**Key conventions:**
- Listeners handle Discord interactions only, delegate all logic to services
- Services never interact with JDA directly (except NotificationService for scheduled messages)
- EmbedUtility stays generic (confirm/error/info templates only), feature-specific embed fields added in listeners or NotificationService
- Records for API response mapping, entities for database
- Constructor injection preferred over field injection
- Empty string over null for optional values
- Button/menu component IDs prefixed by feature name (e.g. postout-create-confirm)
- Formatting utilities are static classes, not Spring beans (EmbedUtility, PostOutFormatter)
- Never call `ZonedDateTime.now()` directly — inject a `Clock` field and use `ZonedDateTime.now(clock)`. `PostOutService` holds a `Clock` built from `GuildConfig.getTimezone()` in its `@Autowired` constructor, plus a secondary constructor taking a `Clock` for tests

**Testing:**
- Unit tests with JUnit 5 and Mockito
- Mock repositories and NotificationService, never mock the class under test
- GuildConfig values set manually in @BeforeEach (tests don't load Spring context)
- No @SpringBootTest — tests are pure unit tests
- Time-dependent code is tested by constructing the service with a `Clock.fixed(...)` via its secondary constructor

**Package layout** (`com.ryanh.agent_discord_bot`):
- `listener/` — JDA `ListenerAdapter` subclasses handle slash commands, buttons, modals, select menus. Each listener is a Spring `@Component` auto-registered via `ListenerRegister`.
- `service/` — Business logic. `PostOutService` has complex date/week math around raid schedules. `MemberService` validates BattleTag against WoWUtils API. `NotificationService` sends embeds to officer channel.
- `entity/` — JPA entities: `Member`, `RaidCharacter`, `PostOut`. Member↔RaidCharacter is OneToMany. PostOut has unique constraint on (discordId, postDate).
- `repository/` — Spring Data JPA interfaces.
- `config/` — `JDAConfig` (bot setup), `GuildConfig` (timezone, raid days/times, officer channel), `ListenerRegister` (auto-wires all listeners to JDA).
- `model/` — Enums (`GuildRank`, `Role`) and API response models (`WowUtilsRoster`).
- `utility/` — `EmbedUtility` (Discord embed builders with color constants), `PostOutFormatter` (formats post-out data for display).

**Listener interaction flow:** Slash command → button/menu selection → optional modal → confirmation → service call → database + Discord response. `PostOutListener` uses in-memory HashMaps to track multi-step user selections.

**Scheduled jobs:** `PostOutService` runs a noon notification on raid days (TUE/WED/THU) and a midnight cleanup of expired post-outs.
