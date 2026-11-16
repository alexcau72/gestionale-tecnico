package com.gestionale.tecnico

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.padding
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.*
import com.gestionale.tecnico.ui.screens.analysis.AnalysisScreen
import com.gestionale.tecnico.ui.screens.clients.ClientDetailScreen
import com.gestionale.tecnico.ui.screens.clients.ClientsScreen
import com.gestionale.tecnico.ui.screens.dashboard.DashboardScreen
import com.gestionale.tecnico.ui.screens.expiries.ExpiriesScreen
import com.gestionale.tecnico.ui.screens.subscriptions.SubscriptionsScreen
import com.gestionale.tecnico.ui.theme.GestionaleTheme
import com.gestionale.tecnico.ui.viewmodel.MainViewModel
import com.gestionale.tecnico.ui.viewmodel.MainVmFactory

sealed class Dest(val route: String, val label: String, val icon: androidx.compose.ui.graphics.vector.ImageVector) {
    data object Dashboard : Dest("dashboard", "Home", Icons.Filled.Dashboard)
    data object Clients : Dest("clients", "Clienti", Icons.Filled.People)
    data object Subscriptions : Dest("subscriptions", "Abbonamenti", Icons.Filled.Autorenew)
    data object Expiries : Dest("expiries", "Scadenze", Icons.Filled.Event)
    data object Analysis : Dest("analysis", "Analisi", Icons.Filled.BarChart)
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val app = application as GestionaleApp
        setContent {
            GestionaleTheme {
                val vm: MainViewModel = viewModel(factory = MainVmFactory(app.repository))
                val nav = rememberNavController()
                val items = listOf(Dest.Dashboard, Dest.Clients, Dest.Subscriptions, Dest.Expiries, Dest.Analysis)
                val backStack by nav.currentBackStackEntryAsState()
                val currentRoute = backStack?.destination?.route
                val showBottomBar = items.any { it.route == currentRoute }
                Scaffold(
                    bottomBar = {
                        if (showBottomBar) {
                            NavigationBar {
                                items.forEach { dest ->
                                    NavigationBarItem(
                                        selected = currentRoute == dest.route,
                                        onClick = {
                                            nav.navigate(dest.route) {
                                                popUpTo(nav.graph.findStartDestination().id) { saveState = true }
                                                launchSingleTop = true
                                                restoreState = true
                                            }
                                        },
                                        icon = { Icon(dest.icon, contentDescription = dest.label) },
                                        label = { Text(dest.label) }
                                    )
                                }
                            }
                        }
                    }
                ) { padding ->
                    NavHost(navController = nav, startDestination = Dest.Dashboard.route, modifier = Modifier.padding(padding)) {
                        composable(Dest.Dashboard.route) { DashboardScreen(vm, nav) }
                        composable(Dest.Clients.route) { ClientsScreen(vm, nav) }
                        composable(Dest.Subscriptions.route) { SubscriptionsScreen(vm) }
                        composable(Dest.Expiries.route) { ExpiriesScreen(vm) }
                        composable(Dest.Analysis.route) { AnalysisScreen(vm) }
                        composable("client/{id}") { entry ->
                            val id = entry.arguments?.getString("id")?.toLongOrNull() ?: return@composable
                            ClientDetailScreen(vm, clientId = id, onBack = { nav.popBackStack() })
                        }
                    }
                }
            }
        }
    }
}
