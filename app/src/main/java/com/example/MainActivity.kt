package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.ui.dashboard.AddTransactionScreen
import com.example.ui.dashboard.CashViewModel
import com.example.ui.dashboard.CashViewModelFactory
import com.example.ui.dashboard.DashboardScreen
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    setContent {
      MyApplicationTheme {
        Surface(
          modifier = Modifier.fillMaxSize(),
          color = MaterialTheme.colorScheme.background
        ) {
          val app = application as LAMEMCashApp
          val viewModel: CashViewModel = viewModel(
            factory = CashViewModelFactory(app.repository)
          )

          val navController = rememberNavController()

          NavHost(navController = navController, startDestination = "person_selection") {
            composable("person_selection") {
                com.example.ui.person.PersonSelectionScreen(
                    viewModel = viewModel,
                    onNavigateToDashboard = {
                        navController.navigate("dashboard") {
                            popUpTo("person_selection") { inclusive = true }
                        }
                    }
                )
            }
            composable("dashboard") {
              DashboardScreen(
                viewModel = viewModel,
                onAddTransactionClick = { navController.navigate("add_transaction") },
                onEditTransactionClick = { id -> navController.navigate("add_transaction?id=$id") },
                onChangePersonClick = {
                    navController.navigate("person_selection") {
                        popUpTo("dashboard") { inclusive = true }
                    }
                }
              )
            }
            composable(
              route = "add_transaction?id={id}",
              arguments = listOf(androidx.navigation.navArgument("id") {
                type = androidx.navigation.NavType.IntType
                defaultValue = -1
              })
            ) { backStackEntry ->
              val id = backStackEntry.arguments?.getInt("id") ?: -1
              AddTransactionScreen(
                viewModel = viewModel,
                transactionId = if (id != -1) id else null,
                onNavigateBack = { navController.popBackStack() }
              )
            }
          }
        }
      }
    }
  }
}
