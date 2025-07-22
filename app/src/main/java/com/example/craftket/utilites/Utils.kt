package com.example.craftket.utilites

import android.app.Activity
import android.content.Context
import android.content.Intent
import com.example.craftket.LoginActivity
import com.firebase.ui.auth.AuthUI

object Utils {
    fun signOut(context: Context) {
        AuthUI.getInstance()
            .signOut(context)
            .addOnCompleteListener {
                val intent = Intent(context, LoginActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                context.startActivity(intent)
                if (context is Activity) {
                    context.finish()
                }
            }
    }

}