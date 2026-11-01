package com.pelypets.todolist.feature.auth.login

import android.os.Bundle
import android.view.View
import android.view.inputmethod.EditorInfo
import androidx.core.view.isVisible
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.pelypets.todolist.BuildConfig
import com.pelypets.todolist.R
import com.pelypets.todolist.core.ui.applySystemBarsPadding
import com.pelypets.todolist.core.ui.collectWhenStarted
import com.pelypets.todolist.core.ui.messageRes
import com.pelypets.todolist.core.ui.setLoading
import com.pelypets.todolist.core.ui.setTextIfChanged
import com.pelypets.todolist.core.ui.showError
import com.pelypets.todolist.core.ui.viewBinding
import com.pelypets.todolist.databinding.FragmentLoginBinding
import com.pelypets.todolist.feature.auth.google.GoogleSignInClient
import com.pelypets.todolist.feature.auth.google.GoogleSignInResult
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

@AndroidEntryPoint
class LoginFragment : Fragment(R.layout.fragment_login) {

    private val viewModel: LoginViewModel by viewModels()
    private val binding by viewBinding(FragmentLoginBinding::bind)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.scroll.applySystemBarsPadding(top = true, bottom = true, ime = true)

        with(binding) {
            emailInput.doAfterTextChanged { viewModel.onEmailChanged(it.toString()) }
            passwordInput.doAfterTextChanged { viewModel.onPasswordChanged(it.toString()) }
            passwordInput.setOnEditorActionListener { _, actionId, _ ->
                (actionId == EditorInfo.IME_ACTION_DONE).also { if (it) viewModel.onSubmitClicked() }
            }
            signInButton.setOnClickListener { viewModel.onSubmitClicked() }
            googleButton.setOnClickListener { viewModel.onGoogleClicked() }
            registerButton.setOnClickListener { viewModel.onRegisterClicked() }
        }

        collectWhenStarted(viewModel.uiState, ::render)
        collectWhenStarted(viewModel.events) { event ->
            when (event) {
                LoginEvent.LaunchGoogleSignIn -> launchGoogleSignIn()
                LoginEvent.NavigateToRegister -> findNavController().navigate(R.id.action_login_to_register)
            }
        }
    }

    private fun render(state: LoginUiState) = with(binding) {
        emailInput.setTextIfChanged(state.email)
        passwordInput.setTextIfChanged(state.password)
        emailLayout.showError(state.emailError)
        passwordLayout.showError(state.passwordError)

        errorText.isVisible = state.error != null
        errorText.text = state.error?.let { getString(it.messageRes) }

        emailLayout.isEnabled = !state.isBusy
        passwordLayout.isEnabled = !state.isBusy
        signInButton.isEnabled = !state.isBusy
        googleButton.isEnabled = !state.isBusy
        registerButton.isEnabled = !state.isBusy
        signInButton.setLoading(state.isSubmitting, signInProgress, getString(R.string.sign_in))
        googleButton.setLoading(state.isGoogleInProgress, googleProgress, getString(R.string.sign_in_with_google))
    }

    private fun launchGoogleSignIn() {
        val client = GoogleSignInClient(requireActivity(), BuildConfig.GOOGLE_WEB_CLIENT_ID)
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                viewModel.onGoogleResult(client.signIn())
            } catch (e: CancellationException) {
                // The screen went away (e.g. rotation) with the sheet open; unblock the form.
                viewModel.onGoogleResult(GoogleSignInResult.Cancelled)
                throw e
            }
        }
    }

    private val LoginError.messageRes: Int
        get() = when (this) {
            LoginError.InvalidCredentials -> R.string.error_invalid_credentials
            LoginError.GoogleFailed -> R.string.error_google_failed
            is LoginError.Other -> error.messageRes
        }
}
