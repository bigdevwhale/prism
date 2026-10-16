package app.prism.ui.today

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import app.prism.domain.Practice
import app.prism.ui.Go
import app.prism.ui.Tab
import app.prism.ui.WData

// Placeholders until the screens are written; delete this file when they are.

@Suppress("UNUSED_PARAMETER")
@Composable
fun PrismTab(data: WData, go: Go, onTab: (Tab) -> Unit) { Text("PrismTab") }

@Suppress("UNUSED_PARAMETER")
@Composable
fun MorningScreen(data: WData, go: Go) { Text("MorningScreen") }

@Suppress("UNUSED_PARAMETER")
@Composable
fun ReviewScreen(data: WData, go: Go) { Text("ReviewScreen") }

@Suppress("UNUSED_PARAMETER")
@Composable
fun QuickSceneScreen(roleId: Long, data: WData, go: Go) { Text("QuickSceneScreen") }
