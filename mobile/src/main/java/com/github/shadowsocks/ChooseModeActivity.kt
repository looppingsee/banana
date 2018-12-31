package com.github.shadowsocks

import android.app.Activity
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.github.shadowsocks.acl.Acl
import com.github.shadowsocks.bean.NetModeBean
import com.github.shadowsocks.preference.DataStore
import com.github.shadowsocks.utils.Key

class ChooseModeActivity : Activity() {
    private var modes: List<NetModeBean>? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_choose_mode)
        initDate()
        var recyclerView = findViewById<RecyclerView>(R.id.recycler_view)
        recyclerView.layoutManager = LinearLayoutManager(this)
        var adapter = MAdapter(this, modes)
        recyclerView.adapter = adapter

        findViewById<View>(R.id.iv_back).setOnClickListener() {
            finish()
        }
    }

    private fun initDate() {
        modes = mutableListOf(NetModeBean(Acl.ALL, "全局模式", "全部流量走服务器(不推荐)", false),
                NetModeBean(Acl.BYPASS_CHN, "国内分流", "国内国外互不干扰(推荐)", true))
    }

    private class MAdapter(var mContext: Context, var datas: List<NetModeBean>?) : RecyclerView.Adapter<MAdapter.MViewHodler>() {
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MViewHodler {
            var view = LayoutInflater.from(mContext).inflate(R.layout.item_mode, parent,false)
            return MViewHodler(view)
        }

        override fun getItemCount(): Int {
            return datas!!.size
        }

        override fun onBindViewHolder(holder: MViewHodler, position: Int) {
            var modeBean = datas!![position]
            holder.mode.text = modeBean.name
            holder.desc.text = modeBean.desc
            holder.check.visibility = if (modeBean.isCheck) View.VISIBLE else View.GONE

            holder.itemView.setOnClickListener {
                for (data in datas!!) {
                    data.isCheck = false
                }
                datas!![position].isCheck = true
                DataStore.privateStore.putString(Key.route,modeBean.mode)
                notifyDataSetChanged()
            }
        }

        inner class MViewHodler(itemView: View) : RecyclerView.ViewHolder(itemView) {
            var mode = itemView.findViewById<TextView>(R.id.tv_mode)
            var desc = itemView.findViewById<TextView>(R.id.tv_mode_desc)
            var check = itemView.findViewById<ImageView>(R.id.iv_check)
        }
    }
}
