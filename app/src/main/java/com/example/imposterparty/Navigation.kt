package com.example.imposterparty

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import com.example.imposterparty.data.model.GamePhase
import com.example.imposterparty.ui.screens.*
import com.example.imposterparty.viewmodel.GameViewModel

@Composable
fun MainNavigation() {
    val backStack = rememberNavBackStack(SplashRoute)
    val gameViewModel: GameViewModel = viewModel()

    val baseMod = Modifier
        .fillMaxSize()
        .safeDrawingPadding()

    NavDisplay(
        backStack = backStack,
        onBack = { backStack.removeLastOrNull() },
        entryProvider = entryProvider {
            entry<SplashRoute> {
                SplashScreen(
                    onSplashFinished = {
                        while (backStack.removeLastOrNull() != null) { /* clear */ }
                        backStack.add(HomeRoute)
                    },
                    modifier = baseMod,
                )
            }

            entry<HomeRoute> {
                HomeScreen(
                    gameViewModel = gameViewModel,
                    onResumeInProgressGame = { phase ->
                        when (phase) {
                            GamePhase.REVEALING -> backStack.add(CardRevealRoute)
                            GamePhase.DISCUSSION -> backStack.add(DiscussionRoute)
                            GamePhase.VOTING -> backStack.add(VotingRoute)
                            else -> backStack.add(GameSetupRoute)
                        }
                    },
                    onNewGame = {
                        gameViewModel.startNewGame()
                        backStack.add(GameSetupRoute)
                    },
                    onWordPacks = { backStack.add(WordPackListRoute) },
                    onScoreboard = { backStack.add(ScoreboardRoute) },
                    modifier = baseMod,
                )
            }

            entry<GameSetupRoute> {
                GameSetupScreen(
                    gameViewModel = gameViewModel,
                    onStartGame = {
                        gameViewModel.startGame()
                        backStack.add(CardRevealRoute)
                    },
                    onBack = { backStack.removeLastOrNull() },
                    modifier = baseMod,
                )
            }

            entry<CardRevealRoute> {
                CardRevealScreen(
                    gameViewModel = gameViewModel,
                    onAllRevealed = {
                        backStack.add(DiscussionRoute)
                    },
                    modifier = baseMod,
                )
            }

            entry<DiscussionRoute> {
                DiscussionScreen(
                    gameViewModel = gameViewModel,
                    onEndDiscussion = {
                        gameViewModel.endDiscussion()
                        backStack.add(VotingRoute)
                    },
                    modifier = baseMod,
                )
            }

            entry<VotingRoute> {
                VotingScreen(
                    gameViewModel = gameViewModel,
                    onVotingComplete = {
                        backStack.add(ResultRoute)
                    },
                    modifier = baseMod,
                )
            }

            entry<ResultRoute> {
                ResultScreen(
                    gameViewModel = gameViewModel,
                    onNextRound = {
                        gameViewModel.nextRound()
                        // Clear back to setup
                        while (backStack.removeLastOrNull() != null) { /* clear */ }
                        backStack.add(HomeRoute)
                        backStack.add(GameSetupRoute)
                    },
                    onHome = {
                        gameViewModel.resetGame()
                        while (backStack.removeLastOrNull() != null) { /* clear */ }
                        backStack.add(HomeRoute)
                    },
                    modifier = baseMod,
                )
            }

            entry<WordPackListRoute> {
                WordPackListScreen(
                    gameViewModel = gameViewModel,
                    onCreateNew = { backStack.add(WordPackEditRoute()) },
                    onEdit = { packId -> backStack.add(WordPackEditRoute(packId)) },
                    onBack = { backStack.removeLastOrNull() },
                    modifier = baseMod,
                )
            }

            entry<WordPackEditRoute> { key ->
                WordPackEditScreen(
                    gameViewModel = gameViewModel,
                    packId = key.packId,
                    onSaved = { backStack.removeLastOrNull() },
                    onBack = { backStack.removeLastOrNull() },
                    modifier = baseMod,
                )
            }

            entry<ScoreboardRoute> {
                ScoreboardScreen(
                    gameViewModel = gameViewModel,
                    onBack = { backStack.removeLastOrNull() },
                    onResumeMatch = {
                        while (backStack.removeLastOrNull() != null) { /* clear */ }
                        backStack.add(HomeRoute)
                        backStack.add(GameSetupRoute)
                    },
                    modifier = baseMod,
                )
            }
        },
    )
}
