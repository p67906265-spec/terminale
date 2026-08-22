package com.sshterm.app

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.sshterm.app.databinding.ActivityQuickCommandsBinding

class QuickCommandsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityQuickCommandsBinding
    private lateinit var commands: MutableList<QuickCommand>
    private lateinit var adapter: QuickCommandAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityQuickCommandsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        commands = AppStorage.loadQuickCommands(this)
        adapter = QuickCommandAdapter(commands) { position ->
            commands.removeAt(position)
            adapter.notifyItemRemoved(position)
            AppStorage.saveQuickCommands(this, commands)
        }

        binding.recyclerCommands.layoutManager = LinearLayoutManager(this)
        binding.recyclerCommands.adapter = adapter

        binding.buttonAdd.setOnClickListener {
            val label = binding.editLabel.text.toString().trim()
            val command = binding.editCommand.text.toString().trim()
            if (label.isNotEmpty() && command.isNotEmpty()) {
                commands.add(QuickCommand(label, command))
                adapter.notifyItemInserted(commands.size - 1)
                AppStorage.saveQuickCommands(this, commands)
                binding.editLabel.setText("")
                binding.editCommand.setText("")
            }
        }
    }
}
