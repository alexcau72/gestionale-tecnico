package com.gestionale.tecnico.ui.screens.clients

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.gestionale.tecnico.ui.components.AddClientDialog
import com.gestionale.tecnico.ui.viewmodel.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClientsScreen(vm: MainViewModel, nav: NavController) {
    val clients by vm.clients.collectAsState()
    var showAdd by remember { mutableStateOf(false) }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Clienti") }) },
        floatingActionButton = { FloatingActionButton(onClick = { showAdd = true }) { Icon(Icons.Filled.Add, "Nuovo cliente") } }
    ) { pad ->
        if (clients.isEmpty()) {
            Box(Modifier.padding(pad).fillMaxSize(), contentAlignment = androidx.compose.ui.Alignment.Center) {
                Text("Nessun cliente. Aggiungine uno con +")
            }
        } else {
            LazyColumn(Modifier.padding(pad).fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(clients, key = { it.id }) { c ->
                    Card(Modifier.fillMaxWidth().clickable { nav.navigate("client/${c.id}") }) {
                        ListItem(
                            leadingContent = { Icon(Icons.Filled.Person, null) },
                            headlineContent = { Text(c.nome, fontWeight = FontWeight.SemiBold) },
                            supportingContent = { if (c.telefono.isNotBlank()) Text(c.telefono) else null }
                        )
                    }
                }
            }
        }
    }
    if (showAdd) AddClientDialog(onDismiss = { showAdd = false }, onSave = { vm.saveClient(it); showAdd = false })
}
