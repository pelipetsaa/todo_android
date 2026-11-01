package com.pelypets.todolist

import androidx.lifecycle.ViewModel
import com.pelypets.todolist.domain.model.SessionState
import com.pelypets.todolist.domain.usecase.ObserveSessionUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

/** Auth gate: decides between the sign-in flow and the todo list; the session ends on logout or a failed token refresh. */
@HiltViewModel
class MainViewModel @Inject constructor(observeSession: ObserveSessionUseCase) : ViewModel() {
    val session: StateFlow<SessionState> = observeSession()
}
