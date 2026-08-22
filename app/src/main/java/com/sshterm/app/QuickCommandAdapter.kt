package com.sshterm.app

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.sshterm.app.databinding.ItemQuickCommandBinding

class QuickCommandAdapter(
    private val items: MutableList<QuickCommand>,
    private val onEdit: (Int) -> Unit,
    private val onDelete: (Int) -> Unit
) : RecyclerView.Adapter<QuickCommandAdapter.ViewHolder>() {

    inner class ViewHolder(val binding: ItemQuickCommandBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemQuickCommandBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        holder.binding.textLabel.text = item.label
        holder.binding.textCommand.text = item.command

        holder.binding.buttonEdit.setOnClickListener {
            val pos = holder.bindingAdapterPosition
            if (pos != RecyclerView.NO_POSITION) onEdit(pos)
        }
        holder.binding.buttonDelete.setOnClickListener {
            val pos = holder.bindingAdapterPosition
            if (pos != RecyclerView.NO_POSITION) onDelete(pos)
        }
    }

    override fun getItemCount() = items.size
}
