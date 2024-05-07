package com.rohit.pdftoimageconverttest

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import android.widget.Toast

const val ACTION_ONE = "ACTION_ONE"

class MyBroadcastReciever: BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        when(val action = intent?.action) {
            ACTION_ONE -> {
                context?.let {
                    var name = intent.extras?.getString("NAME")
                    Log.d("TESTING", "onReceive: ${intent.extras?.toString()}")
                    name = intent.getStringExtra("NAME")
                    Toast.makeText(context, "$name", Toast.LENGTH_SHORT).show()
                }
            }
            null -> {

            }
        }
    }
}