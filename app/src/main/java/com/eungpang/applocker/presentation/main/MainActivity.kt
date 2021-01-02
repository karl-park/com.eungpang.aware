package com.eungpang.applocker.presentation.main

import android.app.AppOpsManager
import android.app.AppOpsManager.OPSTR_GET_USAGE_STATS
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Process
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.AppOpsManagerCompat.MODE_ALLOWED
import androidx.lifecycle.ViewModelProvider
import com.eungpang.snstimechecker.databinding.ActivityMainBinding
import com.eungpang.applocker.domain.item.Item
import com.eungpang.applocker.domain.item.Item.Companion.KEY_SERIALIZABLE
import com.eungpang.applocker.presentation.service.TimeCheckService
import com.eungpang.applocker.presentation.service.TimeCheckServiceActions


class MainActivity : AppCompatActivity() {

    private lateinit var _viewBinding: ActivityMainBinding
    private lateinit var _viewModel: MainViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        _viewModel = ViewModelProvider(this, MainViewModel.ViewModelFactory(application)).get(MainViewModel::class.java)
        _viewBinding = ActivityMainBinding.inflate(layoutInflater)
        val view = _viewBinding.root
        setContentView(view)

        initViews()
        initViewModel()
    }

    override fun onResume() {
        super.onResume()

//        if (!checkPermission()) {
//            AlertDialog.Builder(this)
//                    .setCancelable(false)
//                    .setTitle("Permission Required")
//                    .setMessage("Needs permission to access the system to get app usages.\n" +
//                            "You need to enable the access for this app through\n" +
//                            " Settings > Security > Apps with usage access")
//                    .setPositiveButton("Go Settings") { _, _ ->
//                        startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
//                    }
//                    .setNegativeButton("Cancel") { _, _ ->
//                        Toast.makeText(this, "Can't use the app. Terminate.", Toast.LENGTH_LONG).show()
//                        finish()
//                    }
//                    .show()
//
//            return
//        }
    }

    private fun initViews() {
        val instagram = object: Item {
            override val name = "Instagram"
            override val recentLaunchDateTime = 0L
            override val totalLaunchTimeInSeconds = 0L
            override val packageName = "com.instagram.android"
            override val logoUrl = ""
        }

        val facebook = object: Item {
            override val name = "Facebook"
            override val recentLaunchDateTime = 0L
            override val totalLaunchTimeInSeconds = 0L
            override val packageName = "com.facebook.katana"
            override val logoUrl = ""
        }

        val facebookLight = object: Item {
            override val name = "Facebook Lite"
            override val recentLaunchDateTime = 0L
            override val totalLaunchTimeInSeconds = 0L
            override val packageName = "com.facebook.lite"
            override val logoUrl = ""
        }

        Log.e("Karl", "instagram: ${instagram.name.hashCode()}")
        Log.e("Karl", "facebook: ${facebook.name.hashCode()}")
        Log.e("Karl", "facebook lite: ${facebookLight.name.hashCode()}")

        val sns = listOf(instagram, facebook, facebookLight)

        _viewBinding.rvItems.run {
            adapter = MainRecyclerAdapter(sns, _viewModel).apply {
                setHasStableIds(true)
            }
            setHasFixedSize(true)
        }
    }

    private fun checkPermission() : Boolean {
        val appOps = getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        val mode = appOps.checkOpNoThrow(OPSTR_GET_USAGE_STATS, Process.myUid(), packageName)
        return mode == MODE_ALLOWED
    }

    private fun initViewModel() {
        _viewModel.actionState.observe(this, {
            when (it) {
                is MainViewModel.ActionState.LaunchSns -> {
                    val item = it.item
                    val intent = packageManager.getLaunchIntentForPackage(item.packageName)

                    if (intent == null) {
                        Toast.makeText(this, "${item.name} is not installed.", Toast.LENGTH_SHORT).show()
                        startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=${item.packageName}")))
                        return@observe
                    }

                    startActivity(intent)

                    // TODO: start service now
                    val serviceIntent = Intent(this, TimeCheckService::class.java).apply {
                        action = TimeCheckServiceActions.START_SERVICE
                        putExtra(KEY_SERIALIZABLE, item)
                    }
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        startForegroundService(serviceIntent)
                    } else {
                        startService(intent)
                    }
                }
            }
        })
    }
}