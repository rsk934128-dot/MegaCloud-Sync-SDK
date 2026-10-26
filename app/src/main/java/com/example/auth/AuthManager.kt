package com.example.auth

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.util.Log
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.NoCredentialException
import com.example.R
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential.Companion.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

/**
 * Extension function to safely traverse ContextWrapper chains (e.g. ContextThemeWrapper)
 * and extract the underlying Activity required by Jetpack CredentialManager.
 */
fun Context.findActivity(): Activity? {
    var ctx = this
    while (ctx is ContextWrapper) {
        if (ctx is Activity) {
            return ctx
        }
        ctx = ctx.baseContext
    }
    return null
}

object AuthManager {

    private val auth: FirebaseAuth get() = FirebaseAuth.getInstance()

    fun getWebClientId(context: Context): String {
        val resId = context.resources.getIdentifier("default_web_client_id", "string", context.packageName)
        if (resId != 0) {
            val id = try { context.getString(resId) } catch (e: Exception) { null }
            if (!id.isNullOrBlank()) return id
        }
        return "869767926548-lg0vrh8s054osjkr4bmvfvr5rjgra981.apps.googleusercontent.com"
    }

    fun attemptAutoSignIn(
        context: Context,
        credentialManager: CredentialManager,
        onAuthSuccess: () -> Unit,
        onUnauthenticated: () -> Unit,
        scope: CoroutineScope
    ) {
        if (auth.currentUser != null) {
            onAuthSuccess()
            return
        }
        val clientId = getWebClientId(context)

        val googleIdOption = GetGoogleIdOption.Builder()
            .setFilterByAuthorizedAccounts(true)
            .setServerClientId(clientId)
            .setAutoSelectEnabled(true)
            .build()

        val request = GetCredentialRequest.Builder().addCredentialOption(googleIdOption).build()

        scope.launch {
            try {
                val result = credentialManager.getCredential(context, request)
                val credential = result.credential
                if (credential is CustomCredential && credential.type == TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                    val googleIdToken = GoogleIdTokenCredential.createFrom(credential.data).idToken
                    val authCredential = GoogleAuthProvider.getCredential(googleIdToken, null)
                    auth.signInWithCredential(authCredential).await()
                    onAuthSuccess()
                } else {
                    onUnauthenticated()
                }
            } catch (e: Exception) {
                onUnauthenticated()
            }
        }
    }

    fun onGoogleSignInClicked(
        context: Context,
        credentialManager: CredentialManager,
        onAuthSuccess: () -> Unit,
        onAuthError: (String) -> Unit,
        scope: CoroutineScope,
        onAuthCancelled: () -> Unit = {}
    ) {
        val clientId = getWebClientId(context)

        val signInOption = GetSignInWithGoogleOption.Builder(serverClientId = clientId).build()
        val request = GetCredentialRequest.Builder().addCredentialOption(signInOption).build()

        scope.launch {
            try {
                // Safely resolve Activity from ContextWrapper chain
                val activity = context.findActivity()
                    ?: throw IllegalStateException("Could not extract Activity from context. Please ensure view is attached to an Activity window.")

                val result = credentialManager.getCredential(activity, request)
                val credential = result.credential
                if (credential is CustomCredential && credential.type == TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                    val googleIdToken = GoogleIdTokenCredential.createFrom(credential.data).idToken
                    val authCredential = GoogleAuthProvider.getCredential(googleIdToken, null)
                    auth.signInWithCredential(authCredential).await()
                    onAuthSuccess()
                } else {
                    onAuthError("Unexpected credential format received from Google Identity.")
                }
            } catch (e: GetCredentialCancellationException) {
                Log.w("Auth", "Google Sign-In flow cancelled: ${e.message}")
                onAuthCancelled()
            } catch (e: NoCredentialException) {
                Log.w("Auth", "No Google credentials available on device: ${e.message}")
                onAuthError("No Google account found on device/emulator. Please use Email/Password or Demo Operator mode below.")
            } catch (e: GetCredentialException) {
                Log.e("Auth", "CredentialManager failed: ${e.message}", e)
                val detailedMsg = when {
                    e.message?.contains("16") == true -> "No Google account registered in Google Play Services. Try Email Login or Demo Mode below."
                    e.message?.contains("10") == true -> "Google OAuth configuration mismatch (Code 10). Use Demo Operator mode or Email Login below."
                    else -> e.localizedMessage ?: "Google Sign-In failed"
                }
                onAuthError(detailedMsg)
            } catch (e: Exception) {
                Log.e("Auth", "Sign-In exception", e)
                onAuthError(e.localizedMessage ?: "Sign-in encountered an unexpected error.")
            }
        }
    }

    suspend fun signInWithEmail(email: String, pass: String): Result<FirebaseUser> = withContext(Dispatchers.IO) {
        try {
            val result = auth.signInWithEmailAndPassword(email, pass).await()
            val user = result.user ?: throw IllegalStateException("User object returned null")
            Result.success(user)
        } catch (e: Exception) {
            // If user does not exist yet, attempt auto-registration
            try {
                val regResult = auth.createUserWithEmailAndPassword(email, pass).await()
                val newUser = regResult.user ?: throw e
                Result.success(newUser)
            } catch (regEx: Exception) {
                Result.failure(e)
            }
        }
    }

    fun signOut(
        credentialManager: CredentialManager,
        onSignOutComplete: () -> Unit,
        scope: CoroutineScope
    ) {
        auth.signOut()
        scope.launch {
            try {
                credentialManager.clearCredentialState(ClearCredentialStateRequest())
            } catch (e: Exception) {
                Log.e("Auth", "Failed to clear credential state", e)
            } finally {
                onSignOutComplete()
            }
        }
    }
}
