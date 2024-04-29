package com.rosetta.smsblocker



import android.app.Service
import android.app.role.RoleManager
import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Handler
import android.os.IBinder
import android.provider.CallLog
import android.provider.Telephony
import android.util.Log
import android.widget.Toast

class CallLogDeleteionService : Service() {

    private val handler = Handler()
    private lateinit var phrasesToDelete: Array<String>
    private lateinit var dbHelper: DBHelper2
    companion object {
        private fun showToast(mainActivity: CallLogDeleteionService, message: String) {
            Toast.makeText(mainActivity, message, Toast.LENGTH_SHORT).show()
        }
    }

    private val deleteMessagesRunnable = object : Runnable {
        override fun run() {
            val phrasesToDelete = dbHelper.getAllPhrases().toTypedArray()
            if (!phrasesToDelete.isNullOrEmpty()) {
                deleteCallLogsWithPhrases(applicationContext, phrasesToDelete)
            }
            // Schedule the next execution after 2 minutes (120,000 milliseconds)
            handler.postDelayed(this, 120000)
        }
    }

    override fun onCreate() {
        super.onCreate()
        dbHelper = DBHelper2(this) // Initialize dbHelper here
    }
    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {

        // Toast.makeText(this, "Started 1", Toast.LENGTH_SHORT).show()
        val phrasesToDelete = dbHelper.getAllPhrases().toTypedArray()

        if (!phrasesToDelete.isNullOrEmpty()) {
            deleteCallLogsWithPhrases(this, phrasesToDelete) // Pass the context as the first argument
            Toast.makeText(this, "Messages deletion successfully", Toast.LENGTH_SHORT).show()

        }
        handler.post(deleteMessagesRunnable)
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacks(deleteMessagesRunnable) // Remove the runnable callback when the service is destroyed
        Toast.makeText(this, "Service stopped", Toast.LENGTH_SHORT).show()
    }


    fun deleteCallLogsWithPhrases(context: Context, phrasesToDelete: Array<String>) {
        val callLogUri = CallLog.Calls.CONTENT_URI
        val projection = arrayOf(CallLog.Calls.NUMBER, CallLog.Calls.DATE)
        val cursor = contentResolver.query(callLogUri, projection, null, null, null)

        // Define the array of phone number prefixes to check
        val prefixesToCheck = phrasesToDelete

        // Loop through the call log entries
        cursor?.use { c ->
            while (c.moveToNext()) {
                val phoneNumber = c.getString(c.getColumnIndexOrThrow(CallLog.Calls.NUMBER))
                //showToast("Call : $phoneNumber")

                // Check if the phone number starts with any of the prefixes
                if (prefixesToCheck.any { phoneNumber.startsWith(it) }) {
                    // Delete the call log entry
                    val selection = "${CallLog.Calls.NUMBER} = ?"
                    val selectionArgs = arrayOf(phoneNumber)
                    val deletedRows = contentResolver.delete(callLogUri, selection, selectionArgs)

                    if (deletedRows > 0) {

                        showToast(this,"Call record deleted: $phoneNumber")
                    } else {
                        showToast(this,"Error deleting call record: $phoneNumber")
                    }
                }
            }
        }
    }



}
