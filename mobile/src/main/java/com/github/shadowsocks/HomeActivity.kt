package com.github.shadowsocks

import android.app.Activity
import android.app.backup.BackupManager
import android.content.Intent
import android.net.VpnService
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Button
import androidx.preference.PreferenceDataStore
import com.crashlytics.android.Crashlytics
import com.github.shadowsocks.aidl.IShadowsocksService
import com.github.shadowsocks.aidl.IShadowsocksServiceCallback
import com.github.shadowsocks.bg.BaseService
import com.github.shadowsocks.bg.Executable
import com.github.shadowsocks.preference.DataStore
import com.github.shadowsocks.preference.OnPreferenceDataStoreChangeListener
import com.github.shadowsocks.utils.Key

class HomeActivity : AppCompatActivity(),ShadowsocksConnection.Interface, OnPreferenceDataStoreChangeListener {
    companion object {
        private const val TAG = "ShadowsocksMainActivity"
        private const val REQUEST_CONNECT = 1

        var stateListener: ((Int) -> Unit)? = null
    }

    private lateinit var btnStart: Button

    // service
    var state = BaseService.IDLE
    override val serviceCallback: IShadowsocksServiceCallback.Stub by lazy {
        object : IShadowsocksServiceCallback.Stub() {
            override fun stateChanged(state: Int, profileName: String?, msg: String?) {
                Core.handler.post { changeState(state, msg, true) }
            }
            override fun trafficUpdated(profileId: Long, txRate: Long, rxRate: Long, txTotal: Long, rxTotal: Long) {
                Core.handler.post {
//                    stats.updateTraffic(txRate, rxRate, txTotal, rxTotal)
                    val child = supportFragmentManager.findFragmentById(R.id.fragment_holder) as ToolbarFragment?
                    if (state != BaseService.STOPPING)
                        child?.onTrafficUpdated(profileId, txRate, rxRate, txTotal, rxTotal)
                }
            }
            override fun trafficPersisted(profileId: Long) {
                Core.handler.post { ProfilesFragment.instance?.onTrafficPersisted(profileId) }
            }
        }
    }

    private fun changeState(state: Int, msg: String? = null, animate: Boolean = false) {
    }

    override val listenForDeath: Boolean get() = true
    override fun onServiceConnected(service: IShadowsocksService) = changeState(service.state)
    override fun onServiceDisconnected() = changeState(BaseService.IDLE)
    override fun binderDied() {
        super.binderDied()
        Core.handler.post {
            connection.disconnect()
            Executable.killAll()
            connection.connect()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        when {
            requestCode != HomeActivity.REQUEST_CONNECT -> super.onActivityResult(requestCode, resultCode, data)
            resultCode == Activity.RESULT_OK -> Core.startService()
            else -> {
                Crashlytics.log(Log.ERROR, HomeActivity.TAG, "Failed to start VpnService from onActivityResult: $data")
            }
        }
    }

    override fun onPreferenceDataStoreChanged(store: PreferenceDataStore, key: String?) {
        when (key) {
            Key.serviceMode -> Core.handler.post {
                connection.disconnect()
                connection.connect()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        btnStart = findViewById(R.id.btn_start)
        btnStart.setOnClickListener {
            when {
                state == BaseService.CONNECTED -> Core.stopService()
                BaseService.usingVpnMode -> {
                    val intent = VpnService.prepare(this)
                    if (intent != null) startActivityForResult(intent, HomeActivity.REQUEST_CONNECT)
                    else onActivityResult(HomeActivity.REQUEST_CONNECT, Activity.RESULT_OK, null)
                }
                else -> Core.startService()
            }
        }

        changeState(BaseService.IDLE)   // reset everything to init state
        Core.handler.post { connection.connect() }
        DataStore.publicStore.registerChangeListener(this)
    }

    override fun onResume() {
        super.onResume()
        Core.remoteConfig.fetch()
    }

    override fun onStart() {
        super.onStart()
        connection.listeningForBandwidth = true
    }

    override fun onStop() {
        connection.listeningForBandwidth = false
        super.onStop()
    }

    override fun onDestroy() {
        super.onDestroy()
        DataStore.publicStore.unregisterChangeListener(this)
        connection.disconnect()
        BackupManager(this).dataChanged()
        Core.handler.removeCallbacksAndMessages(null)
    }
}
