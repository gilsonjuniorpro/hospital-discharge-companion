package com.hospitaldischargecompanion.core.navigation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.hospitaldischargecompanion.R
import com.hospitaldischargecompanion.core.demo.UserRole
import com.hospitaldischargecompanion.feature.profile.ProfileSelectionScreen

@Composable
fun HdcNavHost(
    navController: NavHostController = rememberNavController()
) {
    NavHost(
        navController = navController,
        startDestination = HdcRoute.ProfileSelection
    ) {
        composable<HdcRoute.ProfileSelection> {
            ProfileSelectionScreen(
                onRoleSelected = { role, episodeId ->
                    when (role) {
                        UserRole.PATIENT -> navController.navigate(HdcRoute.PatientHome(episodeId))
                        UserRole.FAMILY -> navController.navigate(HdcRoute.FamilyHome(episodeId))
                        UserRole.NURSE -> navController.navigate(HdcRoute.NurseWorkstation)
                        UserRole.PHYSICIAN -> navController.navigate(HdcRoute.PhysicianWorkstation)
                    }
                },
                onOpenDemoControls = { navController.navigate(HdcRoute.DemoControls) }
            )
        }

        composable<HdcRoute.PatientHome> {
            RolePlaceholderScreen(
                title = stringResource(R.string.role_patient_title),
                onBack = { navController.popBackStack() }
            )
        }

        composable<HdcRoute.FamilyHome> {
            RolePlaceholderScreen(
                title = stringResource(R.string.role_family_title),
                onBack = { navController.popBackStack() }
            )
        }

        composable<HdcRoute.NurseWorkstation> {
            RolePlaceholderScreen(
                title = stringResource(R.string.role_nurse_title),
                onBack = { navController.popBackStack() }
            )
        }

        composable<HdcRoute.PhysicianWorkstation> {
            RolePlaceholderScreen(
                title = stringResource(R.string.role_physician_title),
                onBack = { navController.popBackStack() }
            )
        }

        composable<HdcRoute.DemoControls> {
            RolePlaceholderScreen(
                title = stringResource(R.string.action_demo_controls),
                onBack = { navController.popBackStack() }
            )
        }
    }
}

@Composable
private fun RolePlaceholderScreen(
    title: String,
    onBack: () -> Unit
) {
    Scaffold { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = title, style = MaterialTheme.typography.headlineMedium)
            Button(onClick = onBack) {
                Text(text = stringResource(R.string.action_switch_role))
            }
        }
    }
}
