package com.vega.ui.activities

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.vega.ui.fragments.QuickAddBottomSheetFragment
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class QuickAddActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        overridePendingTransition(0, 0)
        super.onCreate(savedInstanceState)
        
        if (savedInstanceState == null) {
            val quickAddBottomSheet = QuickAddBottomSheetFragment()
            quickAddBottomSheet.show(supportFragmentManager, "QuickAddBottomSheetFragment")
        }
    }

    override fun finish() {
        super.finish()
        overridePendingTransition(0, 0)
    }

    @Suppress("DEPRECATION", "OVERRIDE_DEPRECATION")
    override fun onBackPressed() {
        super.onBackPressed()
        finish()
    }
}
