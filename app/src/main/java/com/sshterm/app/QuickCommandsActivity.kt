package com.sshterm.app

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.sshterm.app.databinding.ActivityQuickCommandsBinding

class QuickCommandsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityQuickCommandsBinding
    private lateinit var commands: MutableList<QuickCommand>
    private lateinit var adapter: QuickCommandAdapter
    private var editingPosition: Int? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityQuickCommandsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        commands = AppStorage.loadQuickCommands(this)
        adapter = QuickCommandAdapter(
            commands,
            onEdit = { position -> beginEdit(position) },
            onDelete = { position -> deleteCommand(position) }
        )

        binding.recyclerCommands.layoutManager = LinearLayoutManager(this)
        binding.recyclerCommands.adapter = adapter

        binding.buttonAdd.setOnClickListener { saveCommand() }
        binding.buttonCancelEdit.setOnClickListener { cancelEdit() }
    }

    private fun beginEdit(position: Int) {
        editingPosition = position
        val item = commands[position]
        binding.editLabel.setText(item.label)
        binding.editCommand.setText(item.command)
        binding.buttonAdd.text = "Salva modifica"
        binding.buttonCancelEdit.visibility = View.VISIBLE
        binding.editLabel.requestFocus()
    }

    private fun cancelEdit() {
        editingPosition = null
        binding.editLabel.setText("")
        binding.editCommand.setText("")
        binding.buttonAdd.text = "Aggiungi comando rapido"
        binding.buttonCancelEdit.visibility = View.GONE
    }

    private fun saveCommand() {
        val label = binding.editLabel.text.toString().trim()
        val command = binding.editCommand.text.toString().trim()
        if (label.isEmpty() || command.isEmpty()) return

        val pos = editingPosition
        if (pos == null) {
            commands.add(QuickCommand(label, command))
            adapter.notifyItemInserted(commands.lastIndex)
        } else {
            commands[pos] = QuickCommand(label, command)
            adapter.notifyItemChanged(pos)
        }
        AppStorage.saveQuickCommands(this, commands)
        cancelEdit()
    }

    private fun deleteCommand(position: Int) {
        commands.removeAt(position)
        adapter.notifyItemRemoved(position)
        AppStorage.saveQuickCommands(this, commands)
        if (editingPosition == position) cancelEdit()
    }
}
