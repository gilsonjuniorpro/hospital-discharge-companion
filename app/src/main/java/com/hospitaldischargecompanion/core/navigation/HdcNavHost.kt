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
import com.hospitaldischargecompanion.feature.alerts.AlertsScreen
import com.hospitaldischargecompanion.feature.appointments.AppointmentsScreen
import com.hospitaldischargecompanion.feature.careplan.CarePlanScreen
import com.hospitaldischargecompanion.feature.demo.DemoControlsScreen
import com.hospitaldischargecompanion.feature.education.EducationScreen
import com.hospitaldischargecompanion.feature.ehr.EhrSyncScreen
import com.hospitaldischargecompanion.feature.family.FamilyHomeScreen
import com.hospitaldischargecompanion.feature.medications.MedicationScreen
import com.hospitaldischargecompanion.feature.messaging.MessagingScreen
import com.hospitaldischargecompanion.feature.nurse.NurseWorkstationScreen
import com.hospitaldischargecompanion.feature.patient.PatientHomeScreen
import com.hospitaldischargecompanion.feature.physician.PhysicianWorkstationScreen
import com.hospitaldischargecompanion.feature.profile.ProfileSelectionScreen
import com.hospitaldischargecompanion.feature.telemonitor.TelemonitoringScreen

@Composable
fun HdcNavHost(
    navController: NavHostController = rememberNavController()
) {
    val navigateToProfile: () -> Unit = {
        navController.navigate(HdcRoute.ProfileSelection) {
            popUpTo(0) { inclusive = true }
        }
    }

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
            PatientHomeScreen(
                onSwitchRole = navigateToProfile,
                onOpenDemoControls = { navController.navigate(HdcRoute.DemoControls) },
                onOpenCarePlan = { epId -> navController.navigate(HdcRoute.CarePlan(epId)) },
                onOpenMedications = { epId -> navController.navigate(HdcRoute.Medications(epId)) },
                onOpenTelemonitoring = { epId -> navController.navigate(HdcRoute.Telemonitoring(epId)) },
                onOpenAlerts = { epId -> navController.navigate(HdcRoute.Alerts(epId)) },
                onOpenMessaging = { epId -> navController.navigate(HdcRoute.Messaging(epId)) },
                onOpenAppointments = { epId -> navController.navigate(HdcRoute.Appointments(epId)) },
                onOpenEducation = { epId -> navController.navigate(HdcRoute.Education(epId)) },
                onOpenEhrSync = { epId -> navController.navigate(HdcRoute.EhrSync(epId)) }
            )
        }

        composable<HdcRoute.FamilyHome> {
            FamilyHomeScreen(
                onSwitchRole = navigateToProfile,
                onOpenDemoControls = { navController.navigate(HdcRoute.DemoControls) },
                onOpenMedications = { epId -> navController.navigate(HdcRoute.Medications(epId)) },
                onOpenTelemonitoring = { epId -> navController.navigate(HdcRoute.Telemonitoring(epId)) },
                onOpenAlerts = { epId -> navController.navigate(HdcRoute.Alerts(epId)) },
                onOpenMessaging = { epId -> navController.navigate(HdcRoute.Messaging(epId)) }
            )
        }
        composable<HdcRoute.NurseWorkstation> {
            NurseWorkstationScreen(
                onSwitchRole = navigateToProfile,
                onOpenDemoControls = { navController.navigate(HdcRoute.DemoControls) },
                onOpenCarePlan = { epId -> navController.navigate(HdcRoute.CarePlan(epId)) },
                onOpenMessaging = { epId -> navController.navigate(HdcRoute.Messaging(epId)) },
                onOpenTelemonitoring = { epId -> navController.navigate(HdcRoute.Telemonitoring(epId)) },
                onOpenEhrSync = { epId -> navController.navigate(HdcRoute.EhrSync(epId)) }
            )
        }
        composable<HdcRoute.PhysicianWorkstation> {
            PhysicianWorkstationScreen(
                onSwitchRole = navigateToProfile,
                onOpenDemoControls = { navController.navigate(HdcRoute.DemoControls) },
                onOpenCarePlan = { epId -> navController.navigate(HdcRoute.CarePlan(epId)) },
                onOpenEhrSync = { epId -> navController.navigate(HdcRoute.EhrSync(epId)) }
            )
        }
        composable<HdcRoute.CarePlan> {
            CarePlanScreen(onBack = { navController.popBackStack() })
        }
        composable<HdcRoute.Medications> {
            MedicationScreen(onBack = { navController.popBackStack() })
        }
        composable<HdcRoute.Telemonitoring> {
            TelemonitoringScreen(
                onBack = { navController.popBackStack() },
                onOpenAlerts = { epId -> navController.navigate(HdcRoute.Alerts(epId)) }
            )
        }
        composable<HdcRoute.Alerts> {
            AlertsScreen(onBack = { navController.popBackStack() })
        }
        composable<HdcRoute.Messaging> {
            MessagingScreen(onBack = { navController.popBackStack() })
        }
        composable<HdcRoute.Appointments> {
            AppointmentsScreen(onBack = { navController.popBackStack() })
        }
        composable<HdcRoute.Education> {
            EducationScreen(onBack = { navController.popBackStack() })
        }
        composable<HdcRoute.EhrSync> {
            EhrSyncScreen(onBack = { navController.popBackStack() })
        }
        composable<HdcRoute.DemoControls> {
            DemoControlsScreen(
                onBack = { navController.popBackStack() },
                onOpenDeepLink = { uri ->
                    when {
                        uri.startsWith("hdc://dose/") -> {
                            val medId = uri.removePrefix("hdc://dose/")
                            navController.navigate(HdcRoute.Medications("ep-hf-001", medId))
                        }
                        uri.startsWith("hdc://alert/") -> {
                            val alertId = uri.removePrefix("hdc://alert/")
                            navController.navigate(HdcRoute.Alerts("ep-hf-001", alertId))
                        }
                        uri.startsWith("hdc://episode/") -> {
                            val epId = uri.removePrefix("hdc://episode/")
                            navController.navigate(HdcRoute.PatientHome(epId))
                        }
                    }
                }
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
                Text(text = stringResource(R.string.action_back))
            }
        }
    }
}
