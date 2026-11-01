package com.pelypets.todolist.feature.auth.google

import android.app.Activity
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenParsingException

sealed interface GoogleSignInResult {
    data class Success(val idToken: String) : GoogleSignInResult
    data object Cancelled : GoogleSignInResult
    data class Failure(val message: String?) : GoogleSignInResult
}

/**
 * Google sign-in through Credential Manager. [webClientId] is the OAuth client of type "Web application":
 * the ID token is issued for it, and the backend accepts it as an audience.
 * Needs an Activity to show its bottom sheet, so it lives in the UI layer, not in the ViewModel.
 */
class GoogleSignInClient(private val activity: Activity, private val webClientId: String) {

    suspend fun signIn(): GoogleSignInResult {
        if (webClientId.isBlank()) return GoogleSignInResult.Failure("Google sign-in is not configured")
        val option = GetGoogleIdOption.Builder()
            .setFilterByAuthorizedAccounts(false)
            .setServerClientId(webClientId)
            .setAutoSelectEnabled(false)
            .build()
        val request = GetCredentialRequest.Builder().addCredentialOption(option).build()
        return try {
            val credential = CredentialManager.create(activity).getCredential(activity, request).credential
            if (credential is CustomCredential && credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                GoogleSignInResult.Success(GoogleIdTokenCredential.createFrom(credential.data).idToken)
            } else {
                GoogleSignInResult.Failure("Unexpected credential type: ${credential.type}")
            }
        } catch (e: GetCredentialCancellationException) {
            GoogleSignInResult.Cancelled
        } catch (e: GetCredentialException) {
            GoogleSignInResult.Failure(e.message)
        } catch (e: GoogleIdTokenParsingException) {
            GoogleSignInResult.Failure(e.message)
        }
    }
}
