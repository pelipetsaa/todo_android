package com.pelypets.todolist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pelypets.todolist.domain.model.SessionState
import com.pelypets.todolist.domain.usecase.ObserveSessionUseCase
import com.pelypets.todolist.domain.usecase.RestoreSessionUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import javax.inject.Inject

/** Auth gate: decides between the sign-in flow and the todo list; the session ends on logout or a failed token refresh. */
@HiltViewModel
class MainViewModel @Inject constructor(
    observeSession: ObserveSessionUseCase,
    private val restoreSession: RestoreSessionUseCase,
) : ViewModel() {

    val session: StateFlow<SessionState> = observeSession()

    init {
        viewModelScope.launch { restoreSession() }
    }

    /** Blocks the calling thread; only for a restored back stack, which needs its graph in `onCreate`. */
    fun restoreSessionNow() = runBlocking { restoreSession() }
}
