package com.rosetta.smsblocker

import android.app.Service
import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.IBinder
import android.provider.Telephony
import android.util.Log
import android.widget.Toast

class MessageDeletionService : Service() {

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {

        Toast.makeText(this, "Started 1", Toast.LENGTH_SHORT).show()
        val phrasesToDelete = intent?.getStringArrayExtra("phrases")

        if (!phrasesToDelete.isNullOrEmpty()) {
            deleteMessagesWithPhrases(this, phrasesToDelete) // Pass the context as the first argument
            Toast.makeText(this, "Messages deleted successfully", Toast.LENGTH_SHORT).show()
        }

        return START_NOT_STICKY
    }




    fun deleteMessagesWithPhrases(context: Context, phrasesToDelete: Array<String>) {
        val contentResolver: ContentResolver = context.contentResolver
        val uri = Uri.parse("content://sms/") // Inbox URI

        val projection = arrayOf("_id", "address", "body")

        val cursor = contentResolver.query(
            uri,
            projection,
            null,
            null,
            null
        )

        cursor?.use {
            while (cursor.moveToNext()) {
                val messageId = cursor.getLong(cursor.getColumnIndex("_id"))
                val sender = cursor.getString(cursor.getColumnIndex("address"))
                val messageBody = cursor.getString(cursor.getColumnIndex("body"))

                for (phrase in phrasesToDelete) {
                    if (sender.contains(phrase, ignoreCase = true) || messageBody.contains(phrase, ignoreCase = true)) {
                        deleteMessage(context, messageId)
                        Log.d("DeletedMessage", "Message deleted: $messageBody Id : $messageId")
                        break // Exit the loop if any phrase is found
                    }
                }
            }
        }
    }

    private fun deleteMessage(context: Context, messageId: Long) {
        try {
            val uri = Uri.parse("content://sms/$messageId") // Construct the delete URI
            val rowsDeleted = context.contentResolver.delete(uri, null, null)

            if (rowsDeleted > 0) {
                Toast.makeText(context, "Message with ID $messageId deleted successfully", Toast.LENGTH_SHORT).show()
                Log.d("DeleteMessage", "Message with ID $messageId deleted successfully")
            } else {
                Toast.makeText(context, "Message with ID $messageId not found or couldn't be deleted", Toast.LENGTH_SHORT).show()
                Log.e("DeleteMessage", "Message with ID $messageId not found or couldn't be deleted")
            }
        } catch (e: Exception) {
            Toast.makeText(context, "Error deleting message with ID $messageId: ${e.message}", Toast.LENGTH_SHORT).show()
            Log.e("DeleteMessage", "Error deleting message with ID $messageId: ${e.message}")
        }
    }

}
