package net.sampro.yosef.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.AsyncListDiffer
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import net.sampro.yosef.databinding.DownloadFilesListLayoutBinding
import net.sampro.yosef.models.FileModel

class FilesDownloadsAdapter : RecyclerView.Adapter<FilesDownloadsAdapter.FilesDownloadsViewHolder>() {

    private lateinit var clickListener : OnItemClickListener

    interface OnItemClickListener {
        fun onOpenClick(position: Int)
        fun onDeleteClick(position: Int)
    }

    fun setOnItemClickListener(listener: OnItemClickListener) {
        clickListener = listener
    }

    inner class FilesDownloadsViewHolder(val binding: DownloadFilesListLayoutBinding, listener: OnItemClickListener) : RecyclerView.ViewHolder(binding.root) {
        init {
            binding.apply {
                btnOpen.setOnClickListener {
                    listener.onOpenClick(adapterPosition)
                }
                btnDelete.setOnClickListener {
                    listener.onDeleteClick(adapterPosition)
                }
            }
        }
    }

    private val differCallback = object : DiffUtil.ItemCallback<FileModel>() {
        override fun areItemsTheSame(oldItem: FileModel, newItem: FileModel): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: FileModel, newItem: FileModel): Boolean {
            return oldItem == newItem
        }

    }

    val differ = AsyncListDiffer(this, differCallback)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FilesDownloadsViewHolder {
        return FilesDownloadsViewHolder(DownloadFilesListLayoutBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        ), clickListener)
    }

    override fun onBindViewHolder(holder: FilesDownloadsViewHolder, position: Int) {
        val file = differ.currentList[position]
        holder.binding.apply {
            tvFileNo.text = file.id.toString()
            tvFileName.text = file.name
        }
    }

    override fun getItemCount(): Int {
        return differ.currentList.size
    }

}