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
import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
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

    private val requestNotificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { _ -> }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        checkAndRequestNotificationPermission()

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
        binding.fabQuickAdd.setOnTouchListener { v, event ->
            when (event.action) {
                android.view.MotionEvent.ACTION_DOWN -> {
                    v.animate().scaleX(0.94f).scaleY(0.94f).setDuration(100).start()
                }
                android.view.MotionEvent.ACTION_UP, android.view.MotionEvent.ACTION_CANCEL -> {
                    v.animate().scaleX(1.0f).scaleY(1.0f).setDuration(150).start()
                }
            }
            false
        }

        binding.fabQuickAdd.setOnClickListener {
            val quickAddBottomSheet = QuickAddBottomSheetFragment()
            quickAddBottomSheet.show(supportFragmentManager, "QuickAddBottomSheetFragment")
        }

        observeSnoozeFlow()
        handleSnoozeIntent(intent)
        handleWidgetIntent(intent)
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleSnoozeIntent(intent)
        handleWidgetIntent(intent)
    }

    private fun handleSnoozeIntent(intent: Intent?) {
        if (intent?.getBooleanExtra("EXTRA_START_SNOOZE", false) == true) {
            // Reset intent extra to avoid re-triggering
            intent.putExtra("EXTRA_START_SNOOZE", false)
            snoozeViewModel.loadTodayTasksForSnooze()
        }
    }

    private fun handleWidgetIntent(intent: Intent?) {
        if (intent?.getBooleanExtra("EXTRA_START_QUICK_ADD", false) == true) {
            // Reset intent extra to avoid re-triggering
            intent.putExtra("EXTRA_START_QUICK_ADD", false)
            val quickAddBottomSheet = QuickAddBottomSheetFragment()
            quickAddBottomSheet.show(supportFragmentManager, "QuickAddBottomSheetFragment")
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

    private fun checkAndRequestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                requestNotificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }
}
