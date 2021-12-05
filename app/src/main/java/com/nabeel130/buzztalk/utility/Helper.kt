package com.nabeel130.buzztalk.utility

import android.Manifest
import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.view.Window
import android.widget.TextView
import com.nabeel130.buzztalk.R

class Helper {
    companion object {

        const val TAG = "BuzzReport"
        const val PREF_KEY = "BuzzTalkKey"

        //user detail
        const val USER_NAME = "UserName"

        const val BASE_URL = "https://fcm.googleapis.com"
        const val SERVER_KEY = "AAAAPvL38mg:APA91bHPLAUmJhCTPXKr2O84BmBUnJuJQPG6yRaNhZktXgomLcQjFcvvCFC73xbGgjoHMrL59KfKwiNMZLyjAKNsLsK8fwTcfk4HjHy15LkaR7P0AYHNaOBC9gBiQPgPARfVUW1QvGwv"
        const val CONTENT_TYPE = "application/json"

        //notification body
        const val TITLE = "BuzzTalk"
        const val MESSAGE = "See new post from"
        const val TOPIC = "/topics/globalPost"

        val PERMISSIONS_STORAGE_CAMERA = arrayOf(
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.CAMERA
        )

        fun buildDialogBox(context: Context, title: String, subTitle: String): Dialog {
            val dialog = Dialog(context)
            dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
            dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            dialog.setContentView(R.layout.custom_dialog_box)
            dialog.setCancelable(false)
            dialog.window?.attributes?.windowAnimations = R.style.PauseDialogAnimation

            val confirmText = dialog.findViewById<TextView>(R.id.txtAreYouSure)
            val aboutAction = dialog.findViewById<TextView>(R.id.txtForDeleteDialog)
            confirmText.text = title
            aboutAction.text = subTitle

            return dialog
        }
    }
}