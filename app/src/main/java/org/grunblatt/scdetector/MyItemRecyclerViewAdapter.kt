package org.grunblatt.scdetector


import android.animation.ArgbEvaluator
import android.bluetooth.le.ScanResult
import android.os.ParcelUuid
import android.os.SystemClock
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.fragment_item.view.*
import java.text.DateFormat.getTimeInstance
import java.util.*

fun printB(bytes: ByteArray): String? {
    val sb = StringBuilder()
    sb.append("0x")
    for (b in bytes) {
        sb.append(String.format("%02X", b))
    }
    return sb.toString()
}

class MyItemRecyclerViewAdapter(
    private val mValues: List<ScanResult>
) : RecyclerView.Adapter<MyItemRecyclerViewAdapter.ViewHolder>() {

    private val mOnClickListener: View.OnClickListener

    private val serviceUuids =
        BuildConfig.SERVICE_UUIDS.map { value -> ParcelUuid(UUID.fromString(value)) }

    init {
        mOnClickListener = View.OnClickListener { v ->
            // Notify the active callbacks interface (the activity, if the fragment is attached to
            // one) that an item has been selected.
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.fragment_item, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = mValues[position]

        holder.mMacView.text = item.device.toString()

        var serviceData: ByteArray? = null

        for (serviceUuid in serviceUuids) {
            if (item.scanRecord?.getServiceData(serviceUuid) != null) {
                serviceData = item.scanRecord?.getServiceData(serviceUuid)
            }
        }

        if (serviceData != null) {
            holder.mDataView.text = printB(serviceData)
        }

        val dBmText: String = holder.itemView.getContext().getString(R.string.dBm, item.rssi)
        holder.mRssiView.setText(dBmText)

        val rxTimestampMillis: Long =
            System.currentTimeMillis() - SystemClock.elapsedRealtime() + item.getTimestampNanos() / 1000000
        val rxDate = Date(rxTimestampMillis)
        val sDate: String = getTimeInstance().format(rxDate)
        holder.mDateView.setText(sDate)

        val percent: Float = 1 + (item.rssi.toFloat() + 30) / (121 - 40)
        val color: Int =
            ArgbEvaluator().evaluate(percent, 0xFFFF0000.toInt(), 0xFF00FF00.toInt()) as Int
        holder.mRssiView.setTextColor(color)

        val name: String? = item.device.name
        if (name != null) {
            holder.mNameView.setText(name)
        }

        with(holder.mView) {
            tag = item
            setOnClickListener(mOnClickListener)
        }
    }

    override fun getItemCount(): Int = mValues.size

    inner class ViewHolder(val mView: View) : RecyclerView.ViewHolder(mView) {
        val mMacView: TextView = mView.mac
        val mRssiView: TextView = mView.rssi
        val mDateView: TextView = mView.date
        val mDataView: TextView = mView.data
        val mNameView: TextView = mView.name
    }
}
