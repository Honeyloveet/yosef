package net.sampro.yosef.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.AsyncListDiffer
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import net.sampro.yosef.databinding.FileTransferListLayoutBinding
import net.sampro.yosef.models.ReceivedFileGet
import java.text.SimpleDateFormat
import java.util.*

class FileTransferListAdapter : RecyclerView.Adapter<FileTransferListAdapter.FileTransferListViewHolder>() {

    private lateinit var clickListener : OnItemClickListener

    interface OnItemClickListener {
        fun onDownloadClick(position: Int)
        fun onApproveClick(position: Int)
    }

    fun setOnItemClickListener(listener: OnItemClickListener) {
        clickListener = listener
    }

    inner class FileTransferListViewHolder(val binding: FileTransferListLayoutBinding, listener: FileTransferListAdapter.OnItemClickListener) : RecyclerView.ViewHolder(binding.root) {
        init {
            binding.apply {
                btnDownload.setOnClickListener {
                    listener.onDownloadClick(adapterPosition)
                }
                btnApprove.setOnClickListener {
                    listener.onApproveClick(adapterPosition)
                }
            }
        }
    }

    private val differCallback = object : DiffUtil.ItemCallback<ReceivedFileGet>() {
        override fun areItemsTheSame(oldItem: ReceivedFileGet, newItem: ReceivedFileGet): Boolean {
            return oldItem.file_id == newItem.file_id
        }

        override fun areContentsTheSame(oldItem: ReceivedFileGet, newItem: ReceivedFileGet): Boolean {
            return oldItem == newItem
        }

    }

    val differ = AsyncListDiffer(this, differCallback)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FileTransferListViewHolder {
        return FileTransferListViewHolder(FileTransferListLayoutBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        ), clickListener)
    }

    override fun onBindViewHolder(holder: FileTransferListViewHolder, position: Int) {
        val file = differ.currentList[position]
        holder.binding.apply {
            tvFileName.text = file.file_name
            tvFrom.text = file.from
            tvTo.text = file.to
            tvApprover.text = file.approved_by
            tvStatus.text = file.approval_status
            tvDateSent.text = convertLongToTime(file.received_at!!)
        }
    }

    override fun getItemCount(): Int {
        return differ.currentList.size
    }

    private fun convertLongToTime(time: Long): String {
        val date = Date(time)
        val format = SimpleDateFormat("yyyy-dd-MM HH:mm", Locale.getDefault())
        return format.format(date)
    }

}