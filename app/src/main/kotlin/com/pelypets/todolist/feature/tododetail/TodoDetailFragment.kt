package com.pelypets.todolist.feature.tododetail

import android.os.Bundle
import android.text.format.DateFormat
import android.view.View
import androidx.activity.addCallback
import androidx.core.view.children
import androidx.core.view.isVisible
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.timepicker.MaterialTimePicker
import com.google.android.material.timepicker.TimeFormat
import com.pelypets.todolist.R
import com.pelypets.todolist.core.ui.applySystemBarsPadding
import com.pelypets.todolist.core.ui.collectWhenStarted
import com.pelypets.todolist.core.ui.formatted
import com.pelypets.todolist.core.ui.messageRes
import com.pelypets.todolist.core.ui.setLoading
import com.pelypets.todolist.core.ui.setTextIfChanged
import com.pelypets.todolist.core.ui.showError
import com.pelypets.todolist.core.ui.viewBinding
import com.pelypets.todolist.databinding.FragmentTodoDetailBinding
import com.pelypets.todolist.domain.model.TodoStatus
import dagger.hilt.android.AndroidEntryPoint
import java.time.Instant
import java.time.LocalTime
import java.time.ZoneOffset

@AndroidEntryPoint
class TodoDetailFragment : Fragment(R.layout.fragment_todo_detail) {

    private val viewModel: TodoDetailViewModel by viewModels()
    private val binding by viewBinding(FragmentTodoDetailBinding::bind)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setUpViews()
        reattachPickers()

        childFragmentManager.setFragmentResultListener(DeleteTodoDialogFragment.REQUEST_KEY, viewLifecycleOwner) { _, _ ->
            viewModel.onDeleteConfirmed()
        }

        collectWhenStarted(viewModel.uiState, ::render)
        collectWhenStarted(viewModel.events) { event ->
            when (event) {
                TodoDetailEvent.Close -> findNavController().navigateUp()
                is TodoDetailEvent.ShowError -> Snackbar.make(binding.root, event.error.messageRes, Snackbar.LENGTH_LONG).show()
            }
        }
    }

    private fun setUpViews() = with(binding) {
        form.applySystemBarsPadding(bottom = true, ime = true)
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner) { viewModel.onBackClicked() }
        toolbar.setNavigationOnClickListener { viewModel.onBackClicked() }
        toolbar.setOnMenuItemClickListener { item ->
            (item.itemId == R.id.action_delete).also { if (it) showDeleteConfirmation() }
        }

        titleInput.doAfterTextChanged { viewModel.onTitleChanged(it.toString()) }
        descriptionInput.doAfterTextChanged { viewModel.onDescriptionChanged(it.toString()) }
        dateInput.setOnClickListener { showDatePicker() }
        timeInput.setOnClickListener { showTimePicker() }
        statusGroup.addOnButtonCheckedListener { _, buttonId, isChecked ->
            if (isChecked) statusByButton[buttonId]?.let(viewModel::onStatusSelected)
        }
        saveButton.setOnClickListener { viewModel.onSaveClicked() }
        retryButton.setOnClickListener { viewModel.onRetry() }
    }

    private fun render(state: TodoDetailUiState) = with(binding) {
        toolbar.setTitle(if (state.isNew) R.string.new_todo_title else R.string.edit_todo_title)
        toolbar.menu.findItem(R.id.action_delete).apply {
            isVisible = !state.isNew
            isEnabled = state.canEdit
        }

        progress.isVisible = state.isLoading
        loadErrorGroup.isVisible = state.loadError != null
        loadErrorText.text = state.loadError?.let { getString(it.messageRes) }
        form.isVisible = !state.isLoading && state.loadError == null

        titleInput.setTextIfChanged(state.title)
        descriptionInput.setTextIfChanged(state.description)
        dateInput.setTextIfChanged(state.date.formatted())
        timeInput.setTextIfChanged(state.time.formatted())
        titleLayout.showError(state.titleError)
        descriptionLayout.showError(state.descriptionError)
        val checkedId = statusByButton.entries.first { it.value == state.status }.key
        if (statusGroup.checkedButtonId != checkedId) statusGroup.check(checkedId)

        val enabled = state.canEdit
        (listOf(titleLayout, descriptionLayout, dateLayout, timeLayout, dateInput, timeInput, saveButton) + statusGroup.children)
            .forEach { it.isEnabled = enabled }
        saveButton.setLoading(state.isSaving, saveProgress, getString(R.string.save))
    }

    private fun showDeleteConfirmation() {
        if (!viewModel.uiState.value.canEdit) return
        DeleteTodoDialogFragment().show(childFragmentManager, DeleteTodoDialogFragment.TAG)
    }

    private fun showDatePicker() {
        val state = viewModel.uiState.value
        if (!state.canEdit || childFragmentManager.findFragmentByTag(TAG_DATE_PICKER) != null) return
        // The Material date picker works with UTC midnight timestamps.
        val selection = state.date.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
        MaterialDatePicker.Builder.datePicker()
            .setTitleText(R.string.field_date)
            .setSelection(selection)
            .build()
            .also(::attachDateListener)
            .show(childFragmentManager, TAG_DATE_PICKER)
    }

    private fun showTimePicker() {
        val state = viewModel.uiState.value
        if (!state.canEdit || childFragmentManager.findFragmentByTag(TAG_TIME_PICKER) != null) return
        val format = if (DateFormat.is24HourFormat(requireContext())) TimeFormat.CLOCK_24H else TimeFormat.CLOCK_12H
        MaterialTimePicker.Builder()
            .setTimeFormat(format)
            .setHour(state.time.hour)
            .setMinute(state.time.minute)
            .setTitleText(R.string.field_time)
            .build()
            .also(::attachTimeListener)
            .show(childFragmentManager, TAG_TIME_PICKER)
    }

    /** Pickers are restored by the FragmentManager after rotation, but their listeners are not. */
    @Suppress("UNCHECKED_CAST")
    private fun reattachPickers() {
        (childFragmentManager.findFragmentByTag(TAG_DATE_PICKER) as? MaterialDatePicker<Long>)?.let(::attachDateListener)
        (childFragmentManager.findFragmentByTag(TAG_TIME_PICKER) as? MaterialTimePicker)?.let(::attachTimeListener)
    }

    private fun attachDateListener(picker: MaterialDatePicker<Long>) {
        picker.addOnPositiveButtonClickListener { millis ->
            viewModel.onDateSelected(Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate())
        }
    }

    private fun attachTimeListener(picker: MaterialTimePicker) {
        picker.addOnPositiveButtonClickListener { viewModel.onTimeSelected(LocalTime.of(picker.hour, picker.minute)) }
    }

    private companion object {
        const val TAG_DATE_PICKER = "date_picker"
        const val TAG_TIME_PICKER = "time_picker"

        val statusByButton = mapOf(
            R.id.statusTodo to TodoStatus.TODO,
            R.id.statusInProgress to TodoStatus.IN_PROGRESS,
            R.id.statusDone to TodoStatus.DONE,
        )
    }
}
