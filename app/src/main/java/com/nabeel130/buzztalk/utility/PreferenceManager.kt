package com.nabeel130.buzztalk.utility

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.SharedPreferences

class PreferenceManager(context: Context) {
    companion object {
        var preference: SharedPreferences? = null
        var editor: SharedPreferences.Editor? = null


        fun setString(key: String, value: String){
            editor = preference?.edit()
            editor?.putString(key,value)
            editor?.apply()
        }

        fun getString(key: String): String{
            return preference?.getString(key, "").toString()
        }

        fun deletePref(){
            editor = preference?.edit()
            editor?.clear()
            editor?.apply()
        }
    }


    init {
        if (preference == null) preference =
            context.getSharedPreferences(context.packageName, Activity.MODE_PRIVATE)
    }

}