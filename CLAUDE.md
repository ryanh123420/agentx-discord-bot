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

**Core domain:** Members can manage "post-outs" (raid absence notifications). Officers receive scheduled Discord embed summaries. Members can also upload droptimizer reports (wishlists) to the WoWUtils API.

**Key conventions:**
- Listeners handle Discord interactions only, delegate all logic to services
- Services never interact with JDA directly (except NotificationService for scheduled messages)
- Notifications for user-initiated events are sent from the listener; services only send scheduled notifications
- EmbedUtility stays generic (confirm/error/info templates only), feature-specific embed fields added in listeners or NotificationService
- Records for API response mapping, entities for database
- Constructor injection preferred over field injection
- Empty string over null for optional values
- Button/menu component IDs prefixed by feature name (e.g. postout-create-confirm)
- Formatting utilities are static classes, not Spring beans (EmbedUtility, PostOutFormatter, WishlistFormatter)
- Never call `ZonedDateTime.now()` directly — inject the `Clock` bean and use `ZonedDateTime.now(clock)`. `TimeConfig` exposes a single `Clock` built from `GuildConfig.getTimezone()`; classes that need "now" take it as a normal constructor parameter
- Raid week math works in 1-based raid-week positions (reset day = 1), never raw `DayOfWeek.getValue()`. Convert with `RaidCalendar.raidWeekPosition` at the boundary and get back to a date with `weekStart.plusDays(position - 1)`. Mixing the two numbering systems is what caused a real ordering bug

**Testing:**
- Unit tests with JUnit 5 and Mockito
- Mock repositories and NotificationService, never mock the class under test
- GuildConfig values set manually in @BeforeEach (tests don't load Spring context)
- No @SpringBootTest — tests are pure unit tests
- Time-dependent code is tested by passing a `Clock.fixed(...)` to the normal constructor
- `RaidCalendar` is a pure function of `GuildConfig` + `Clock`, so tests construct it directly rather than mocking it

**Package layout** (`com.ryanh.agent_discord_bot`):
- `listener/` — JDA `ListenerAdapter` subclasses handle slash commands, buttons, modals, select menus. Each listener is a Spring `@Component` auto-registered via `ListenerRegister`. Currently `AdminListener`, `PostOutListener`, `ThreadsListener`, `WishlistListener`.
- `client/` — HTTP wrappers around external APIs. `WowUtilsClient` (droptimizer upload) builds its `RestClient` from an injected `RestClient.Builder` so tests can bind `MockRestServiceServer` to it.
- `service/` — Business logic. `PostOutService` handles post-out persistence and orchestration. `RaidCalendar` owns all raid week date math (week start, valid menu days, date parsing) and touches no repository. `NotificationService` sends embeds to officer channel. `ConfigService` reads/writes runtime settings with a caller-supplied default.
- `entity/` — JPA entities. `PostOut` (unique constraint on (discordId, postDate)) and `Config` (runtime bot settings, keyed by a `ConfigKey` enum used directly as the primary key).
- `repository/` — Spring Data JPA interfaces. `PostOutRepository`, `ConfigRepository`.
- `exception/` — `WowUtilsException`, carrying a `ErrorCodes` enum mapped from the API's wire values via `fromWireValue`.
- `config/` — `JDAConfig` (bot setup), `GuildConfig` (timezone, raid days/times, officer channel), `TimeConfig` (the application's `Clock` bean), `ListenerRegister` (auto-wires all listeners to JDA).
- `model/` — Enums (`GuildRank`, `Role`, `ConfigKey`) and API response records (`DroptimizerResponse`).
- `utility/` — `EmbedUtility` (Discord embed builders with color constants), `PostOutFormatter`, `WishlistFormatter` (format data for display).

**Listener interaction flow:** Slash command → button/menu selection → optional modal → confirmation → service call → database + Discord response. `PostOutListener` tracks multi-step selections in Caffeine-backed maps keyed by the flow's message ID (not the user ID, so two open flows can't mix), with entries expiring 10 minutes after they are written.

**Scheduled jobs:** `PostOutService` sends the weekly post-out report on `guild.notification-schedule` (currently noon on Tuesday, the reset day). The method also returns early unless today is `guild.reset-day`, as a guard if the schedule is changed. A midnight job cleans up expired post-outs.
