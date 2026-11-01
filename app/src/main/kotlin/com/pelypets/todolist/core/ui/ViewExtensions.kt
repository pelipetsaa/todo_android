package com.pelypets.todolist.core.ui

import android.view.View
import android.widget.EditText
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
import androidx.core.view.updatePadding
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.google.android.material.button.MaterialButton
import com.google.android.material.progressindicator.CircularProgressIndicator
import com.google.android.material.textfield.TextInputLayout
import com.pelypets.todolist.domain.error.ValidationError
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

/** Collects [flow] while the fragment's view is at least STARTED. */
fun <T> Fragment.collectWhenStarted(flow: Flow<T>, action: suspend (T) -> Unit) {
    viewLifecycleOwner.lifecycleScope.launch {
        viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
            flow.collect(action)
        }
    }
}

/** Keeps the cursor in place: state is rendered back into the field the user is typing in. */
fun EditText.setTextIfChanged(value: String) {
    if (text.toString() != value) setText(value)
}

fun TextInputLayout.showError(error: ValidationError?) {
    this.error = error?.let { context.getString(it.messageRes) }
}

/** A button with a spinner on top of it (see `view_progress_button`); the label is hidden while loading. */
fun MaterialButton.setLoading(loading: Boolean, progress: CircularProgressIndicator, label: CharSequence) {
    text = if (loading) "" else label
    progress.isVisible = loading
}

/** Adds the system bar (and optionally the keyboard) insets to the view's own padding. */
fun View.applySystemBarsPadding(top: Boolean = false, bottom: Boolean = true, ime: Boolean = false) {
    val initialTop = paddingTop
    val initialBottom = paddingBottom
    ViewCompat.setOnApplyWindowInsetsListener(this) { view, windowInsets ->
        val types = WindowInsetsCompat.Type.systemBars() or (if (ime) WindowInsetsCompat.Type.ime() else 0)
        val insets = windowInsets.getInsets(types)
        view.updatePadding(
            left = insets.left,
            right = insets.right,
            top = if (top) initialTop + insets.top else initialTop,
            bottom = if (bottom) initialBottom + insets.bottom else initialBottom,
        )
        windowInsets
    }
}

/** Lifts a floating view (FAB) above the navigation bar. */
fun View.applySystemBarsMargin() {
    val params = layoutParams as android.view.ViewGroup.MarginLayoutParams
    val initialBottom = params.bottomMargin
    val initialEnd = params.marginEnd
    ViewCompat.setOnApplyWindowInsetsListener(this) { view, windowInsets ->
        val insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())
        view.updateLayoutParams<android.view.ViewGroup.MarginLayoutParams> {
            bottomMargin = initialBottom + insets.bottom
            marginEnd = initialEnd + insets.right
        }
        windowInsets
    }
}
