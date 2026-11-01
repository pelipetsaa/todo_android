package com.pelypets.todolist.feature.auth.register

import android.os.Bundle
import android.view.View
import android.view.inputmethod.EditorInfo
import androidx.activity.addCallback
import androidx.core.view.isVisible
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.pelypets.todolist.R
import com.pelypets.todolist.core.ui.applySystemBarsPadding
import com.pelypets.todolist.core.ui.collectWhenStarted
import com.pelypets.todolist.core.ui.messageRes
import com.pelypets.todolist.core.ui.setLoading
import com.pelypets.todolist.core.ui.setTextIfChanged
import com.pelypets.todolist.core.ui.showError
import com.pelypets.todolist.core.ui.viewBinding
import com.pelypets.todolist.databinding.FragmentRegisterBinding
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class RegisterFragment : Fragment(R.layout.fragment_register) {

    private val viewModel: RegisterViewModel by viewModels()
    private val binding by viewBinding(FragmentRegisterBinding::bind)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.scroll.applySystemBarsPadding(bottom = true, ime = true)
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner) { viewModel.onBackClicked() }

        with(binding) {
            toolbar.setNavigationOnClickListener { viewModel.onBackClicked() }
            emailInput.doAfterTextChanged { viewModel.onEmailChanged(it.toString()) }
            passwordInput.doAfterTextChanged { viewModel.onPasswordChanged(it.toString()) }
            confirmPasswordInput.doAfterTextChanged { viewModel.onConfirmPasswordChanged(it.toString()) }
            confirmPasswordInput.setOnEditorActionListener { _, actionId, _ ->
                (actionId == EditorInfo.IME_ACTION_DONE).also { if (it) viewModel.onSubmitClicked() }
            }
            createButton.setOnClickListener { viewModel.onSubmitClicked() }
        }

        collectWhenStarted(viewModel.uiState, ::render)
        collectWhenStarted(viewModel.events) { event ->
            when (event) {
                RegisterEvent.NavigateBack -> findNavController().navigateUp()
            }
        }
    }

    private fun render(state: RegisterUiState) = with(binding) {
        emailInput.setTextIfChanged(state.email)
        passwordInput.setTextIfChanged(state.password)
        confirmPasswordInput.setTextIfChanged(state.confirmPassword)
        emailLayout.showError(state.emailError)
        passwordLayout.showError(state.passwordError)
        confirmPasswordLayout.showError(state.confirmPasswordError)

        errorText.isVisible = state.error != null
        errorText.text = state.error?.let { getString(it.messageRes) }

        val enabled = !state.isSubmitting
        emailLayout.isEnabled = enabled
        passwordLayout.isEnabled = enabled
        confirmPasswordLayout.isEnabled = enabled
        createButton.isEnabled = enabled
        createButton.setLoading(state.isSubmitting, createProgress, getString(R.string.create_account))
    }

    private val RegisterError.messageRes: Int
        get() = when (this) {
            RegisterError.EmailTaken -> R.string.error_email_taken
            is RegisterError.Other -> error.messageRes
        }
}
