package app.prism.ui.roles

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import app.prism.domain.Practice
import app.prism.ui.Go
import app.prism.ui.Tab
import app.prism.ui.WData

// Placeholders until the screens are written; delete this file when they are.

@Suppress("UNUSED_PARAMETER")
@Composable
fun RolesTab(data: WData, go: Go) { Text("RolesTab") }

@Suppress("UNUSED_PARAMETER")
@Composable
fun SphereScreen(id: Long, data: WData, go: Go) { Text("SphereScreen") }

@Suppress("UNUSED_PARAMETER")
@Composable
fun SphereFormScreen(id: Long, data: WData, go: Go) { Text("SphereFormScreen") }

@Suppress("UNUSED_PARAMETER")
@Composable
fun RoleScreen(id: Long, data: WData, go: Go) { Text("RoleScreen") }

@Suppress("UNUSED_PARAMETER")
@Composable
fun RoleFormScreen(id: Long, sphereId: Long, shadow: Boolean, data: WData, go: Go) { Text("RoleFormScreen") }

@Suppress("UNUSED_PARAMETER")
@Composable
fun LibraryScreen(sphereKey: String, shadows: Boolean, data: WData, go: Go) { Text("LibraryScreen") }

@Suppress("UNUSED_PARAMETER")
@Composable
fun PresetScreen(key: String, data: WData, go: Go) { Text("PresetScreen") }
