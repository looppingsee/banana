package com.github.shadowsocks

import android.app.Activity
import android.app.backup.BackupManager
import android.content.Intent
import android.database.DataSetObserver
import android.graphics.Color
import android.net.VpnService
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.Settings
import android.text.TextUtils
import android.util.Log
import android.view.*
import android.widget.*
import androidx.preference.PreferenceDataStore
import com.crashlytics.android.Crashlytics
import com.github.shadowsocks.aidl.IShadowsocksService
import com.github.shadowsocks.aidl.IShadowsocksServiceCallback
import com.github.shadowsocks.bg.BaseService
import com.github.shadowsocks.bg.Executable
import com.github.shadowsocks.database.Profile
import com.github.shadowsocks.database.ProfileManager
import com.github.shadowsocks.preference.DataStore
import com.github.shadowsocks.preference.OnPreferenceDataStoreChangeListener
import com.github.shadowsocks.utils.Key
import com.github.shadowsocks.utils.SharedPreferencesHelper
import com.github.shadowsocks.widget.BottomSheetDialog
import com.github.shadowsocks.widget.BottomSheetDialogListView
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*

class HomeActivity : AppCompatActivity(), ShadowsocksConnection.Interface, OnPreferenceDataStoreChangeListener {
    companion object {
        private const val TAG = "ShadowsocksMainActivity"
        private const val REQUEST_CONNECT = 1
        var stateListener: ((Int) -> Unit)? = null
    }

    private lateinit var btnStart: Button
    private lateinit var spHelper:SharedPreferencesHelper
    private var dialog:BottomSheetDialog? = null

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

    fun queryAccoutConfig() {
        var okClient = OkHttpClient()
        var request = Request.Builder().url("https://raw.githubusercontent.com/aishuidedabai/BanananNetConfig/master/Username.txt").build()
        try {
            var response = okClient.newCall(request).execute()
            if (response.isSuccessful()) {
                var msg = response.body()?.string() ?: ""
                if(!TextUtils.isEmpty(msg)) {
                    spHelper.put("account",msg)
                }
            } else {
                System.out.println("连接服务器失败返回错误代码" + response.code())
            }

        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    fun queryVPNConfig() {
        var okClient = OkHttpClient()
        var request = Request.Builder().url("https://raw.githubusercontent.com/aishuidedabai/BanananNetConfig/master/NEW_GCM.txt").build()
        try {
            var response = okClient.newCall(request).execute()
            if (response.isSuccessful()) {
                var msg = response.body()?.string() ?: ""
                if(!TextUtils.isEmpty(msg)) {
                    spHelper.put("vconfig",msg)
                }
            } else {
                System.out.println("连接服务器失败返回错误代码" + response.code())
            }

        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    fun checkIsExpire():Boolean{
        var curName = generateUserName()
        val date = Calendar.getInstance().time
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
        var curTime = dateFormat.format(date)
        var accountStr = spHelper.getSharedPreference("account")
        var accounts = accountStr.split("\n")
        for (account in accounts) {
            var name = account.split("/")[0]
            var time = account.split("/")[1]
            if(name.equals(curName) && time.compareTo(curName) < 0) {
                return true
            }
        }
        return false
    }

    fun isPaidUser():Boolean {
        var curName = generateUserName()
        var accountStr = spHelper.getSharedPreference("account")
        var accounts = accountStr.split("\n")
        for (account in accounts) {
            var name = account.split("/")[0]
            if(name.equals(curName)) {
                return true
            }
        }
        return false
    }

    fun generateUserName():String{
       return Settings.Secure.getString(getContentResolver(), Settings.Secure.ANDROID_ID)
    }

    fun show(view: View) {
        dialog = BottomSheetDialog(this)
        val v = LayoutInflater.from(this).inflate(R.layout.list, null, false)

        val l = v.findViewById<BottomSheetDialogListView>(R.id.listview)
        dialog?.setContentView(v)

        initListView(l)
        l.bindBottomSheetDialog(v)
        dialog?.addSpringBackDisLimit(-1)
        dialog?.show()
    }

    fun setConfig(configStr:String) {
        var config = configStr.split(":")
        DataStore.privateStore.putString(Key.host,config[0])
        DataStore.privateStore.putInt(Key.remotePort,config[1].toInt())
    }


    private fun initListView(l: BottomSheetDialogListView) {
        var configsStr = spHelper.getSharedPreference("vconfig")
        var datas = configsStr.split("\n")
        setConfig(datas[0])
        l.adapter = object : ListAdapter {
            override fun areAllItemsEnabled(): Boolean {
                return false
            }

            override fun isEnabled(position: Int): Boolean {
                return false
            }

            override fun registerDataSetObserver(observer: DataSetObserver) {

            }

            override fun unregisterDataSetObserver(observer: DataSetObserver) {

            }

            override fun getCount(): Int {
                return datas.size
            }

            override fun getItem(position: Int): Any {
                return datas[position]
            }

            override fun getItemId(position: Int): Long {
                return position.toLong()
            }

            override fun hasStableIds(): Boolean {
                return false
            }

            override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                var convertView = convertView
                if (convertView == null) {
                    convertView = TextView(parent.context)
                    convertView.layoutParams = AbsListView.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            40 * 3 // 40dp
                    )
                }
                val t = convertView as TextView?
                t!!.setTextColor(Color.BLACK)
                t.gravity = Gravity.CENTER
                t.text = datas[position]
                t.textSize = 17f
                t.setOnClickListener {
                    setConfig(datas[position])
                    dialog?.dismiss()
                    Core.stopService()
                }
                t.setOnTouchListener { v, event ->
                    if (event.action == MotionEvent.ACTION_DOWN) {
                        l.setCoordinatorDisallow()
                    }
                    false
                }
                return t
            }

            override fun getItemViewType(position: Int): Int {
                return 0
            }

            override fun getViewTypeCount(): Int {
                return 1
            }

            override fun isEmpty(): Boolean {
                return false
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
        spHelper = SharedPreferencesHelper(this)

        Thread(Runnable {
            queryAccoutConfig()
            queryVPNConfig()
        }).start()
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
