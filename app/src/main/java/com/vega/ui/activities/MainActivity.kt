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
import androidx.navigation.NavController
import androidx.navigation.NavOptions
import androidx.navigation.fragment.NavHostFragment
import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.view.View
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

        // Setup floating navigation bar click listeners
        binding.navBtnToday.setOnClickListener {
            navigateToTab(navController, R.id.todayFragment)
        }
        binding.navBtnUpcoming.setOnClickListener {
            navigateToTab(navController, R.id.upcomingFragment)
        }
        binding.navBtnInbox.setOnClickListener {
            navigateToTab(navController, R.id.inboxFragment)
        }
        binding.navBtnSettings.setOnClickListener {
            navigateToTab(navController, R.id.settingsFragment)
        }

        // Toggle floating bar visibility & active icon tint based on destination
        navController.addOnDestinationChangedListener { _, destination, _ ->
            updateNavActiveState(destination.id)
            if (destination.id == R.id.searchFragment) {
                binding.layoutFloatingBottomBar.visibility = View.GONE
            } else {
                binding.layoutFloatingBottomBar.visibility = View.VISIBLE
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

    private fun navigateToTab(navController: NavController, destinationId: Int) {
        if (navController.currentDestination?.id == destinationId) return
        val navOptions = NavOptions.Builder()
            .setLaunchSingleTop(true)
            .setRestoreState(true)
            .setPopUpTo(R.id.todayFragment, false, true)
            .build()
        navController.navigate(destinationId, null, navOptions)
    }

    private fun updateNavActiveState(destinationId: Int) {
        val activeColor = ContextCompat.getColor(this, R.color.vega_nav_icon_active)
        val inactiveColor = ContextCompat.getColor(this, R.color.vega_nav_icon_inactive)

        binding.navBtnToday.setColorFilter(if (destinationId == R.id.todayFragment) activeColor else inactiveColor)
        binding.navBtnUpcoming.setColorFilter(if (destinationId == R.id.upcomingFragment) activeColor else inactiveColor)
        binding.navBtnInbox.setColorFilter(if (destinationId == R.id.inboxFragment) activeColor else inactiveColor)
        binding.navBtnSettings.setColorFilter(if (destinationId == R.id.settingsFragment) activeColor else inactiveColor)
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
