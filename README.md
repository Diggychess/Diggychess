# KnightScope (Android)

Native Android (Kotlin + Jetpack Compose) rewrite of **KnightScope**, an offline-first chess analysis, tactical puzzle trainer, and friendly match application.

## Features

- **Analyse**:
  - Paste PGN games from Chess.com or Lichess (or try the built-in sample) with full PGN header, comment, and `[%eval]` support.
  - Evaluates positions using the **Lichess Cloud Eval API** when online, with an **on-device alpha-beta chess engine** (tapered piece-square tables, quiescence search, transposition table) as automatic fallback.
  - Plain-English explanations for every move and alternative best move, win-probability accuracy breakdown, opening/middlegame/endgame summary, ECO opening recognition (80+ transposition-safe openings), interactive evaluation bar/graph, best-move/played-move board arrows, and shareable text reports.
  - Configurable analysis depth (4–10) and inaccuracy/mistake/blunder win-probability thresholds.

- **Puzzles**:
  - **5,058 bundled offline puzzles** (5,047 Lichess CC0 puzzles + 11 KnightScope starter checkmates).
  - Rated mode with ELO tracking and rating history sparkline, 7 unlockable difficulty levels (Rookie to Grandmaster), category/theme filters, Daily Puzzle, Random, Timed 3:00 rush, Review Mistakes, **My Game Mistakes** (puzzles extracted directly from your analysed games), 14-day progress bar chart, and Lichess CSV puzzle import.

- **Friends**:
  - **Pass & Play** side-by-side mode on one device with automatic board orientation option and rematch.
  - **Live Wi-Fi Match** over the same local Wi-Fi network (host or join by IP:port) supporting moves, draw offers, resignations, and rematches without requiring any account or login.

- **History & Backup**:
  - Local persistence via DataStore Preferences with per-side mistake reports (White / Black / Both) and JSON backup export/restore via the About dialog, plus a built-in diagnostic self-test.

## Tech Stack

- Kotlin, Jetpack Compose, Material 3, Navigation Compose
- StateFlow + ViewModel architecture
- DataStore Preferences, OkHttp, Coroutines
- Custom Compose Canvas Staunton pieces, board themes (Classic wood, Tournament green, Slate blue), and evaluation charts
