package app.prism.ui.practices

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import app.prism.domain.Practice
import app.prism.ui.Go
import app.prism.ui.Tab
import app.prism.ui.WData

// Placeholders until the screens are written; delete this file when they are.

@Suppress("UNUSED_PARAMETER")
@Composable
fun PracticesTab(data: WData, go: Go) { Text("PracticesTab") }

@Suppress("UNUSED_PARAMETER")
@Composable
fun ThresholdScreen(roleId: Long, data: WData, go: Go) { Text("ThresholdScreen") }

@Suppress("UNUSED_PARAMETER")
@Composable
fun ThresholdsScreen(data: WData, go: Go) { Text("ThresholdsScreen") }

@Suppress("UNUSED_PARAMETER")
@Composable
fun SosScreen(data: WData, go: Go) { Text("SosScreen") }

@Suppress("UNUSED_PARAMETER")
@Composable
fun RehearsalScreen(id: Long, roleId: Long, data: WData, go: Go) { Text("RehearsalScreen") }

@Suppress("UNUSED_PARAMETER")
@Composable
fun CouncilScreen(id: Long, data: WData, go: Go) { Text("CouncilScreen") }

@Suppress("UNUSED_PARAMETER")
@Composable
fun NoteScreen(kind: Practice, id: Long, roleId: Long, data: WData, go: Go) { Text("NoteScreen") }

@Suppress("UNUSED_PARAMETER")
@Composable
fun AnchorScreen(roleId: Long, data: WData, go: Go) { Text("AnchorScreen") }

@Suppress("UNUSED_PARAMETER")
@Composable
fun ActScreen(id: Long, roleId: Long, data: WData, go: Go) { Text("ActScreen") }
