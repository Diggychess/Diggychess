package com.example.chess

enum class ChessPlatform(val id: String, val label: String, val placeholder: String) {
    CHESS_COM("chesscom", "Chess.com", "e.g. Hikaru or MagnusCarlsen"),
    LICHESS("lichess", "Lichess", "e.g. DrNykterstein or EricRosen")
}

data class PlatformRatingStat(
    val mode: String,
    val rating: Int,
    val bestRating: Int? = null,
    val wins: Int = 0,
    val losses: Int = 0,
    val draws: Int = 0
)

data class PlatformPlayerProfile(
    val platform: ChessPlatform,
    val username: String,
    val title: String? = null,
    val subtitle: String? = null,
    val stats: List<PlatformRatingStat> = emptyList()
)

data class ImportedGameSummary(
    val id: String,
    val platform: ChessPlatform,
    val whiteName: String,
    val blackName: String,
    val whiteRating: Int? = null,
    val blackRating: Int? = null,
    val timeClass: String, // bullet, blitz, rapid, classical, daily
    val timeControl: String,
    val result: String, // 1-0, 0-1, 1/2-1/2
    val userOutcome: String, // win, loss, draw
    val userColor: String, // white, black
    val openingName: String,
    val eco: String? = null,
    val moveCount: Int,
    val playedAtMillis: Long,
    val pgn: String
)

data class PlatformFetchResult(
    val profile: PlatformPlayerProfile,
    val games: List<ImportedGameSummary>
)

enum class ImportSpeedFilter(val id: String, val label: String) {
    ALL("all", "All speeds"),
    BULLET("bullet", "Bullet"),
    BLITZ("blitz", "Blitz"),
    RAPID("rapid", "Rapid"),
    CLASSICAL("classical", "Daily / Classical")
}

enum class ImportResultFilter(val id: String, val label: String) {
    ALL("all", "All"),
    WIN("win", "Wins"),
    LOSS("loss", "Losses"),
    DRAW("draw", "Draws")
}

enum class ImportColorFilter(val id: String, val label: String) {
    BOTH("both", "Both"),
    WHITE("white", "White"),
    BLACK("black", "Black")
}
