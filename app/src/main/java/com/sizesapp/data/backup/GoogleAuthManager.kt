package com.sizesapp.data.backup

import android.app.Activity
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.google.android.gms.auth.api.identity.AuthorizationRequest
import com.google.android.gms.auth.api.identity.Identity
import com.google.android.gms.common.api.Scope
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

/** The scope needed to read/write only this app's own hidden Drive folder -- never the user's visible Drive files. */
const val DRIVE_APP_DATA_SCOPE = "https://www.googleapis.com/auth/drive.appdata"

sealed interface AuthorizationOutcome {
    data class Granted(val accessToken: String) : AuthorizationOutcome
    data class ConsentRequired(val pendingIntent: PendingIntent) : AuthorizationOutcome
    data class Failed(val message: String) : AuthorizationOutcome
}

/**
 * Wraps Google Play Services' Identity Authorization API to obtain an OAuth
 * access token scoped to this app's Drive "appDataFolder" -- the same hidden,
 * per-app storage area WhatsApp/Signal-style backups use. No sign-in identity
 * (name/email) is requested, only the storage scope.
 *
 * Requires an Android-type OAuth client to be registered for this app's
 * package name + signing certificate in Google Cloud Console; see README.
 */
class GoogleAuthManager(private val context: Context) {

    private val authorizationClient by lazy { Identity.getAuthorizationClient(context) }

    suspend fun requestDriveAuthorization(): AuthorizationOutcome {
        val request = AuthorizationRequest.Builder()
            .setRequestedScopes(listOf(Scope(DRIVE_APP_DATA_SCOPE)))
            .build()

        return suspendCancellableCoroutine { continuation ->
            authorizationClient.authorize(request)
                .addOnSuccessListener { result ->
                    val outcome = when {
                        result.hasResolution() ->
                            AuthorizationOutcome.ConsentRequired(result.pendingIntent!!)
                        result.accessToken != null ->
                            AuthorizationOutcome.Granted(result.accessToken!!)
                        else ->
                            AuthorizationOutcome.Failed("Authorization returned no token and no resolution.")
                    }
                    continuation.resume(outcome)
                }
                .addOnFailureListener { error ->
                    continuation.resume(AuthorizationOutcome.Failed(error.message ?: "Unknown authorization error"))
                }
        }
    }

    /** Call from the launcher registered for [AuthorizationOutcome.ConsentRequired]'s pending intent. */
    fun outcomeFromActivityResult(resultCode: Int, data: Intent?): AuthorizationOutcome {
        if (resultCode != Activity.RESULT_OK || data == null) {
            return AuthorizationOutcome.Failed("User did not grant Drive access.")
        }
        return try {
            val result = authorizationClient.getAuthorizationResultFromIntent(data)
            val token = result.accessToken
            if (token != null) AuthorizationOutcome.Granted(token)
            else AuthorizationOutcome.Failed("No access token in authorization result.")
        } catch (e: Exception) {
            AuthorizationOutcome.Failed(e.message ?: "Failed to parse authorization result")
        }
    }
}
