package com.example.imposterparty.data.model

/**
 * Game settings configured during setup.
 */
data class GameSettings(
    val imposterMode: ImposterMode = ImposterMode.MANUAL,
    val manualImposterCount: Int = 1,
    val autoRange: ImposterRange = ImposterRange.ONE_TO_ONE,
    val revealImposterCountAtEnd: Boolean = false,
    val isTimerEnabled: Boolean = true,
    val timerDuration: TimerDuration = TimerDuration.FIVE_MIN,
    val customTimerSeconds: Int = 300,
    val selectedCategoryIds: Set<Long> = emptySet(),
)

enum class ImposterMode {
    MANUAL,
    AUTO_RANGE
}

enum class ImposterRange(val min: Int, val max: Int, val label: String) {
    ONE_TO_ONE(1, 1, "1-1"),
    ONE_TO_TWO(1, 2, "1-2"),
    ONE_TO_THREE(1, 3, "1-3"),
    ONE_TO_FOUR(1, 4, "1-4"),
    ONE_TO_FIVE(1, 5, "1-5"),
}

enum class TimerDuration(val seconds: Int, val label: String) {
    TWO_MIN(120, "2 min"),
    FIVE_MIN(300, "5 min"),
    TEN_MIN(600, "10 min"),
    CUSTOM(-1, "Custom"),
}

/**
 * Returns the maximum number of imposters allowed for the given player count.
 *
 * 5–6 players  → 1 Imposter
 * 7–10         → 2
 * 11–14        → 3
 * 15–18        → 4
 * 19–24        → 5
 */
fun maxImpostersForPlayerCount(playerCount: Int): Int = when {
    playerCount <= 6  -> 1
    playerCount <= 10 -> 2
    playerCount <= 14 -> 3
    playerCount <= 18 -> 4
    else              -> 5
}

/**
 * Returns the recommended [ImposterRange] for the given player count.
 * This is the "right" auto-range option based on the ratio table.
 */
fun recommendedAutoRange(playerCount: Int): ImposterRange = when {
    playerCount <= 6  -> ImposterRange.ONE_TO_ONE
    playerCount <= 10 -> ImposterRange.ONE_TO_TWO
    playerCount <= 14 -> ImposterRange.ONE_TO_THREE
    playerCount <= 18 -> ImposterRange.ONE_TO_FOUR
    else              -> ImposterRange.ONE_TO_FIVE
}

/**
 * Returns only the [ImposterRange] entries that are valid for the given player count.
 * i.e. ranges whose max does not exceed [maxImpostersForPlayerCount].
 */
fun validAutoRanges(playerCount: Int): List<ImposterRange> {
    val maxAllowed = maxImpostersForPlayerCount(playerCount)
    return ImposterRange.entries.filter { it.max <= maxAllowed }
}
