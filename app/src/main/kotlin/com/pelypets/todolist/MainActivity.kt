package com.pelypets.todolist

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import com.pelypets.todolist.databinding.ActivityMainBinding
import com.pelypets.todolist.domain.model.SessionState
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    private val viewModel: MainViewModel by viewModels()
    private lateinit var navController: NavController
    private var hasGraph = false

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen().setKeepOnScreenCondition { viewModel.session.value == SessionState.UNKNOWN }
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContentView(ActivityMainBinding.inflate(layoutInflater).root)

        navController = (supportFragmentManager.findFragmentById(R.id.navHost) as NavHostFragment).navController
        // After a process restart the back stack is restored on top of the graph, which has to be set right here.
        // A configuration change keeps the view model, and with it the session that has already been read.
        if (savedInstanceState != null && viewModel.session.value == SessionState.UNKNOWN) viewModel.restoreSessionNow()
        viewModel.session.value.takeIf { it != SessionState.UNKNOWN }?.let(::setGraph)

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.session.collect(::onSessionChanged)
            }
        }
    }

    private fun setGraph(session: SessionState) {
        navController.graph = navController.navInflater.inflate(R.navigation.nav_graph).apply {
            setStartDestination(if (session == SessionState.LOGGED_IN) R.id.todoListFragment else R.id.loginFragment)
        }
        hasGraph = true
    }

    /** Signing in, logging out and a rejected refresh token all end up here. */
    private fun onSessionChanged(session: SessionState) {
        if (session == SessionState.UNKNOWN) return
        if (!hasGraph) {
            setGraph(session)
            return
        }
        val inAuthFlow = navController.currentDestination?.id in AUTH_DESTINATIONS
        when {
            session == SessionState.LOGGED_OUT && !inAuthFlow -> navController.navigate(R.id.action_global_login)
            session == SessionState.LOGGED_IN && inAuthFlow -> navController.navigate(R.id.action_global_todoList)
        }
    }

    private companion object {
        val AUTH_DESTINATIONS = setOf(R.id.loginFragment, R.id.registerFragment)
    }
}
