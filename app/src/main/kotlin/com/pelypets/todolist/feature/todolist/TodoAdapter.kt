package com.pelypets.todolist.feature.todolist

import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.color.MaterialColors
import com.pelypets.todolist.core.ui.formatted
import com.pelypets.todolist.core.ui.labelRes
import com.pelypets.todolist.databinding.ItemTodoBinding
import com.pelypets.todolist.domain.model.Todo
import com.pelypets.todolist.domain.model.TodoStatus
import java.time.ZoneId
import com.google.android.material.R as MaterialR

class TodoAdapter(private val onClick: (Todo) -> Unit) : ListAdapter<Todo, TodoAdapter.ViewHolder>(DiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder =
        ViewHolder(ItemTodoBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: ViewHolder, position: Int) = holder.bind(getItem(position))

    inner class ViewHolder(private val binding: ItemTodoBinding) : RecyclerView.ViewHolder(binding.root) {

        fun bind(todo: Todo) = with(binding) {
            val dueAt = todo.dueAt.atZone(ZoneId.systemDefault())
            timeText.text = dueAt.toLocalTime().formatted()
            titleText.text = todo.title
            dateText.text = dueAt.toLocalDate().formatted()
            statusText.bindStatus(todo.status)
            root.setOnClickListener { onClick(todo) }
        }
    }

    private object DiffCallback : DiffUtil.ItemCallback<Todo>() {
        override fun areItemsTheSame(oldItem: Todo, newItem: Todo) = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: Todo, newItem: Todo) = oldItem == newItem
    }
}

private fun TextView.bindStatus(status: TodoStatus) {
    val (container, content) = when (status) {
        TodoStatus.TODO -> MaterialR.attr.colorSurfaceVariant to MaterialR.attr.colorOnSurfaceVariant
        TodoStatus.IN_PROGRESS -> MaterialR.attr.colorPrimaryContainer to MaterialR.attr.colorOnPrimaryContainer
        TodoStatus.DONE -> MaterialR.attr.colorTertiaryContainer to MaterialR.attr.colorOnTertiaryContainer
    }
    backgroundTintList = ColorStateList.valueOf(MaterialColors.getColor(this, container))
    setTextColor(MaterialColors.getColor(this, content))
    setText(status.labelRes)
}
