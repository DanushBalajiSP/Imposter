package com.example.imposterparty

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

@Serializable data object Main : NavKey

@Serializable data object HomeRoute : NavKey
@Serializable data object GameSetupRoute : NavKey
@Serializable data object CardRevealRoute : NavKey
@Serializable data object DiscussionRoute : NavKey
@Serializable data object VotingRoute : NavKey
@Serializable data object ResultRoute : NavKey
@Serializable data object WordPackListRoute : NavKey
@Serializable data class WordPackEditRoute(val packId: Long = -1) : NavKey
@Serializable data object ScoreboardRoute : NavKey
