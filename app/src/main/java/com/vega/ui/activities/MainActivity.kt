package com.vega.ui.activities

import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.NavigationUI
import com.vega.R
import com.vega.databinding.ActivityMainBinding
import com.vega.ui.fragments.QuickAddBottomSheetFragment
import com.vega.ui.fragments.SnoozeBottomSheetFragment
import com.vega.ui.viewmodels.SnoozeViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val snoozeViewModel: SnoozeViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Setup navigation
        val navHostFragment = supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        val navController = navHostFragment.navController
        
        // Link toolbar with NavController
        NavigationUI.setupWithNavController(binding.toolbar, navController)
        NavigationUI.setupWithNavController(binding.bottomNavView, navController)

        // Inflate toolbar search menu
        binding.toolbar.inflateMenu(R.menu.toolbar_menu)
        binding.toolbar.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.action_search -> {
                    navController.navigate(R.id.searchFragment)
                    true
                }
                R.id.settingsFragment -> {
                    navController.navigate(R.id.settingsFragment)
                    true
                }
                else -> false
            }
        }

        // Toggle elements based on destination
        navController.addOnDestinationChangedListener { _, destination, _ ->
            if (destination.id == R.id.searchFragment || destination.id == R.id.settingsFragment) {
                binding.toolbar.menu.findItem(R.id.action_search)?.isVisible = false
                binding.toolbar.menu.findItem(R.id.settingsFragment)?.isVisible = false
                binding.fabQuickAdd.hide()
                binding.bottomNavView.visibility = android.view.View.GONE
            } else {
                binding.toolbar.menu.findItem(R.id.action_search)?.isVisible = true
                binding.toolbar.menu.findItem(R.id.settingsFragment)?.isVisible = true
                binding.fabQuickAdd.show()
                binding.bottomNavView.visibility = android.view.View.VISIBLE
            }
        }

        // Setup FAB for Quick Add
        binding.fabQuickAdd.setOnClickListener {
            val quickAddBottomSheet = QuickAddBottomSheetFragment()
            quickAddBottomSheet.show(supportFragmentManager, "QuickAddBottomSheetFragment")
        }

        observeSnoozeFlow()
        handleSnoozeIntent(intent)
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleSnoozeIntent(intent)
    }

    private fun handleSnoozeIntent(intent: Intent?) {
        if (intent?.getBooleanExtra("EXTRA_START_SNOOZE", false) == true) {
            // Reset intent extra to avoid re-triggering
            intent.putExtra("EXTRA_START_SNOOZE", false)
            snoozeViewModel.loadTodayTasksForSnooze()
        }
    }

    private fun observeSnoozeFlow() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                snoozeViewModel.currentTask.collect { task ->
                    val fragment = supportFragmentManager.findFragmentByTag("SnoozeBottomSheetFragment")
                    if (task != null) {
                        if (fragment == null) {
                            val snoozeBottomSheet = SnoozeBottomSheetFragment.newInstance()
                            snoozeBottomSheet.show(supportFragmentManager, "SnoozeBottomSheetFragment")
                        }
                    } else {
                        if (fragment != null) {
                            (fragment as? SnoozeBottomSheetFragment)?.dismiss()
                        }
                        // Clear notifications once the ritual is complete or empty
                        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                        notificationManager.cancelAll()
                    }
                }
            }
        }
    }
}
