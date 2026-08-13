package com.example.imposterparty.data.model

/**
 * Game settings configured during setup.
 */
data class GameSettings(
    val imposterMode: ImposterMode = ImposterMode.MANUAL,
    val manualImposterCount: Int = 1,
    val autoRange: ImposterRange = ImposterRange.ONE_TO_ONE,
    val revealImposterCountAtEnd: Boolean = false,
    val timerDuration: TimerDuration = TimerDuration.FIVE_MIN,
    val customTimerSeconds: Int = 300,
    val selectedCategoryId: Long = -1,
)

enum class ImposterMode {
    MANUAL,
    AUTO_RANGE
}

enum class ImposterRange(val min: Int, val max: Int, val label: String) {
    ONE_TO_ONE(1, 1, "1-1"),
    ONE_TO_TWO(1, 2, "1-2"),
    ONE_TO_THREE(1, 3, "1-3"),
}

enum class TimerDuration(val seconds: Int, val label: String) {
    TWO_MIN(120, "2 min"),
    FIVE_MIN(300, "5 min"),
    TEN_MIN(600, "10 min"),
    CUSTOM(-1, "Custom"),
}
