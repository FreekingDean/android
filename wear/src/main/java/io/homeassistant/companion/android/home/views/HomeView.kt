package io.homeassistant.companion.android.home.views

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material.Chip
import androidx.wear.compose.material.ExperimentalWearMaterialApi
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Text
import androidx.wear.compose.navigation.SwipeDismissableNavHost
import androidx.wear.compose.navigation.composable
import androidx.wear.compose.navigation.rememberSwipeDismissableNavController
import androidx.wear.tiles.TileService
import io.homeassistant.companion.android.R
import io.homeassistant.companion.android.home.HomePresenterImpl
import io.homeassistant.companion.android.home.MainViewModel
import io.homeassistant.companion.android.tiles.ShortcutsTile
import io.homeassistant.companion.android.util.LocalRotaryEventDispatcher
import io.homeassistant.companion.android.util.RotaryEventDispatcher
import io.homeassistant.companion.android.util.RotaryEventHandlerSetup
import io.homeassistant.companion.android.util.setChipDefaults

private const val SCREEN_LANDING = "landing"
private const val SCREEN_SETTINGS = "settings"
private const val SCREEN_SET_FAVORITES = "set_favorites"
private const val SCREEN_SET_TILE_SHORTCUTS = "set_tile_shortcuts"
private const val SCREEN_SELECT_TILE_SHORTCUT = "select_tile_shortcut"

@ExperimentalWearMaterialApi
@Composable
fun LoadHomePage(
    mainViewModel: MainViewModel
) {
    var shortcutEntitySelectionIndex: Int by remember { mutableStateOf(0) }
    val context = LocalContext.current

    val rotaryEventDispatcher = RotaryEventDispatcher()
    if (mainViewModel.entities.isNullOrEmpty() && mainViewModel.favoriteEntityIds.isNullOrEmpty()) {
        Column {
            ListHeader(id = R.string.loading)
            Chip(
                modifier = Modifier
                    .padding(top = 30.dp, start = 10.dp, end = 10.dp),
                label = {
                    Text(
                        text = stringResource(R.string.loading_entities),
                        textAlign = TextAlign.Center
                    )
                },
                onClick = { /* No op */ },
                colors = setChipDefaults()
            )
        }
    } else {
        val swipeDismissableNavController = rememberSwipeDismissableNavController()
        MaterialTheme {
            CompositionLocalProvider(
                LocalRotaryEventDispatcher provides rotaryEventDispatcher
            ) {
                RotaryEventHandlerSetup(rotaryEventDispatcher)
                SwipeDismissableNavHost(
                    navController = swipeDismissableNavController,
                    startDestination = SCREEN_LANDING
                ) {
                    composable(SCREEN_LANDING) {
                        MainView(
                            mainViewModel.entities,
                            mainViewModel.favoriteEntityIds,
                            { mainViewModel.toggleEntity(it) },
                            { swipeDismissableNavController.navigate(SCREEN_SETTINGS) },
                            { mainViewModel.logout() },
                            mainViewModel.isHapticEnabled.value,
                            mainViewModel.isToastEnabled.value
                        )
                    }
                    composable(SCREEN_SETTINGS) {
                        SettingsView(
                            mainViewModel.favoriteEntityIds,
                            { swipeDismissableNavController.navigate(SCREEN_SET_FAVORITES) },
                            { mainViewModel.clearFavorites() },
                            { swipeDismissableNavController.navigate(SCREEN_SET_TILE_SHORTCUTS) },
                            mainViewModel.isHapticEnabled.value,
                            mainViewModel.isToastEnabled.value,
                            { mainViewModel.setHapticEnabled(it) },
                            { mainViewModel.setToastEnabled(it) }
                        )
                    }
                    composable(SCREEN_SET_FAVORITES) {
                        val validEntities = mainViewModel.entities
                            .filter { it.key.split(".")[0] in HomePresenterImpl.supportedDomains }
                        SetFavoritesView(
                            validEntities,
                            mainViewModel.favoriteEntityIds
                        ) { entityId, isSelected ->
                            if (isSelected) {
                                mainViewModel.addFavorite(entityId)
                            } else {
                                mainViewModel.removeFavorite(entityId)
                            }
                        }
                    }
                    composable(SCREEN_SET_TILE_SHORTCUTS) {
                        SetTileShortcutsView(
                            mainViewModel.shortcutEntities
                        ) {
                            shortcutEntitySelectionIndex = it
                            swipeDismissableNavController.navigate(SCREEN_SELECT_TILE_SHORTCUT)
                        }
                    }
                    composable(SCREEN_SELECT_TILE_SHORTCUT) {
                        val validEntities = mainViewModel.entities
                            .filter { it.key.split(".")[0] in HomePresenterImpl.supportedDomains }
                        ChooseEntityView(
                            validEntities,
                            {
                                mainViewModel.clearTileShortcut(shortcutEntitySelectionIndex)
                                TileService.getUpdater(context).requestUpdate(ShortcutsTile::class.java)
                                swipeDismissableNavController.navigateUp()
                            },
                            { entity ->
                                mainViewModel.setTileShortcut(shortcutEntitySelectionIndex, entity)
                                TileService.getUpdater(context).requestUpdate(ShortcutsTile::class.java)
                                swipeDismissableNavController.navigateUp()
                            }
                        )
                    }
                }
            }
        }
    }
}

@ExperimentalWearMaterialApi
@Preview
@Composable
private fun PreviewHomeView() {
    LoadHomePage(mainViewModel = MainViewModel())
}
