package net.sampro.yosef.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.AsyncListDiffer
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import net.sampro.yosef.databinding.UsersListLayoutBinding
import net.sampro.yosef.models.UserGet

class UsersAdapter : RecyclerView.Adapter<UsersAdapter.UsersViewHolder>() {

    private lateinit var clickListener : OnItemClickListener

    interface OnItemClickListener {
        fun onChangePasswordClick(position: Int)
        fun onDeleteUserClick(position: Int)
    }

    fun setOnItemClickListener(listener: OnItemClickListener) {
        clickListener = listener
    }

    inner class UsersViewHolder(val binding: UsersListLayoutBinding, listener: OnItemClickListener) : RecyclerView.ViewHolder(binding.root) {
        init {
            binding.apply {
                btnChangePassword.setOnClickListener {
                    listener.onChangePasswordClick(adapterPosition)
                }
                btnDeleteUser.setOnClickListener {
                    listener.onDeleteUserClick(adapterPosition)
                }
            }
        }
    }

    private val differCallback = object : DiffUtil.ItemCallback<UserGet>() {
        override fun areItemsTheSame(oldItem: UserGet, newItem: UserGet): Boolean {
            return oldItem.uid == newItem.uid
        }

        override fun areContentsTheSame(oldItem: UserGet, newItem: UserGet): Boolean {
            return oldItem == newItem
        }

    }

    val differ = AsyncListDiffer(this, differCallback)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UsersViewHolder {
        return UsersViewHolder(UsersListLayoutBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        ), clickListener)
    }

    override fun onBindViewHolder(holder: UsersViewHolder, position: Int) {
        val user = differ.currentList[position]
        holder.binding.apply {
            tvUserName.text = user.name
            tvPassword.text = user.password
            tvUserType.text = user.type
        }
    }

    override fun getItemCount(): Int {
        return differ.currentList.size
    }

}