package com.pelypets.todolist.feature.tododetail

import android.app.Dialog
import android.os.Bundle
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.setFragmentResult
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.pelypets.todolist.R

/** A DialogFragment rather than a plain dialog, so the confirmation survives rotation. */
class DeleteTodoDialogFragment : DialogFragment() {

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog = MaterialAlertDialogBuilder(requireContext())
        .setTitle(R.string.delete_confirm_title)
        .setMessage(R.string.delete_confirm_message)
        .setPositiveButton(R.string.delete) { _, _ -> setFragmentResult(REQUEST_KEY, Bundle.EMPTY) }
        .setNegativeButton(R.string.cancel, null)
        .create()

    companion object {
        const val TAG = "delete_todo"
        const val REQUEST_KEY = "delete_todo_confirmed"
    }
}
