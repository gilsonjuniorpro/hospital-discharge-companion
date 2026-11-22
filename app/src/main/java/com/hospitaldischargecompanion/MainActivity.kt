package com.hospitaldischargecompanion

import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hospitaldischargecompanion.core.demo.DemoSessionRepository
import com.hospitaldischargecompanion.core.i18n.LocaleManager
import com.hospitaldischargecompanion.core.navigation.DeepLinkParser
import com.hospitaldischargecompanion.core.navigation.HdcNavHost
import com.hospitaldischargecompanion.core.navigation.HdcRoute
import com.hospitaldischargecompanion.core.ui.theme.HdcTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    @Inject
    lateinit var sessionRepository: DemoSessionRepository

    private val _pendingDeepLink = MutableStateFlow<HdcRoute?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        handleIntent(intent)
        enableEdgeToEdge()
        setContent {
            val session by sessionRepository.sessionState.collectAsStateWithLifecycle(initialValue = null)
            val pendingDeepLink by _pendingDeepLink.asStateFlow().collectAsStateWithLifecycle()

            LaunchedEffect(session?.locale) {
                session?.locale?.let { LocaleManager.applyLocale(it) }
            }

            HdcTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    HdcNavHost(
                        pendingDeepLink = pendingDeepLink,
                        onDeepLinkConsumed = { _pendingDeepLink.value = null }
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        val route = DeepLinkParser.parse(intent?.dataString)
        if (route != null) {
            _pendingDeepLink.value = route
        }
    }
}
