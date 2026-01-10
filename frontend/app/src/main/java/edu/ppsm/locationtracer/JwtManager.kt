package edu.ppsm.locationtracer
import android.content.Context
import android.content.SharedPreferences

class JwtManager {
    companion object {
        private const val PREFS_NAME = "auth_prefs"
        private const val KEY_TOKEN = "jwt_token"

        fun saveJwt(context: Context, token: String) {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            prefs.edit()
                .putString(KEY_TOKEN, token)
                .apply()
        }

        fun getJwt(context: Context): String? {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            return prefs.getString(KEY_TOKEN, null)
        }

        fun clearJwt(context: Context) {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            prefs.edit().remove(KEY_TOKEN).apply()
        }
    }
}