package com.rosetta.smsblocker

import android.app.Activity
import android.app.AlertDialog
import android.app.role.RoleManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.BlockedNumberContract
import android.provider.CallLog
import android.provider.Telephony
import android.telephony.TelephonyManager
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class PhoneAcivity : AppCompatActivity() {

    private lateinit var intentLauncher: ActivityResultLauncher<Intent>
    private var hasPermision = false;
    private lateinit var dbHelper: DBHelper2
    private lateinit var phrase :EditText
    private lateinit var addPhrase:Button
    private lateinit var smsClear:Button
    private lateinit var deletePhrases:Button
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: PhraseAdapter

    companion object {
        // The requested role.


        private const val REQUEST_CODE = 123
        private fun showToast(mainActivity: PhoneAcivity, message: String) {
            Toast.makeText(mainActivity, message, Toast.LENGTH_SHORT).show()
        }
    }
        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)
            setContentView(R.layout.activity_phone)


            dbHelper = DBHelper2(this)

            phrase = findViewById(R.id.phrase2)
            addPhrase = findViewById(R.id.addphrase2)
            smsClear = findViewById(R.id.smsClear)
            deletePhrases = findViewById(R.id.deletePhrases2)

                smsClear.setOnClickListener{
                    val intent = Intent(this, MainActivity::class.java)
                    startActivity(intent)
                }

            addPhrase.setOnClickListener {
                val phraseText = phrase.text.toString().trim()
                if (phraseText.isNotEmpty()) {
                    // Add the phrase to the database
                    val id = dbHelper.addPhrase(phraseText)
                    if (id != -1L) {
                        Toast.makeText(this, "Phrase saved to database", Toast.LENGTH_SHORT).show()
                        phrase.setText("")

                            loadPhrases()
                        removeSpamCalls()

                    } else {
                        Toast.makeText(this, "Failed to save phrase to database", Toast.LENGTH_SHORT).show()
                    }

                } else {
                    Toast.makeText(this, "Please enter a phrase", Toast.LENGTH_SHORT).show()
                }
            }
            deletePhrases.setOnClickListener {


                    removeSpamCalls()


            }
            recyclerView = findViewById(R.id.recyclerView2)
            adapter = PhraseAdapter(ArrayList(), this::onPhraseClicked, this::deletePhrase)

            recyclerView.layoutManager = LinearLayoutManager(this)
            recyclerView.adapter = adapter

            loadPhrases()

            if (checkPermissions(this)) {
                removeSpamCalls()
            }

            // Use the phrases from the database for SMS deletion



        }

    private fun loadPhrases() {
        val phrases = dbHelper.getAllPhrases()
        adapter.updateData(phrases)
    }
    private fun onPhraseClicked(phrase: String) {
        // Handle click event, e.g., open edit dialog or activity
        // For simplicity, we'll just display a toast message
        Toast.makeText(this, "Clicked on phrase: $phrase", Toast.LENGTH_SHORT).show()
    }
    private fun removeSpamCalls() {
        // Check if the app has the necessary permissions
        if (checkPermissions(this)) {
            // Retrieve phrases from the database
            val phrases = dbHelper.getAllPhrases().toTypedArray()
            Toast.makeText(this, "Started Deleteing Service", Toast.LENGTH_SHORT).show()
            // Start the MessageDeletionService service
            val serviceIntent = Intent(this, CallLogDeleteionService::class.java)
            serviceIntent.putExtra("phrases", phrases)
            startService(serviceIntent)
        } else {
            // Request the necessary permissions
            requestPermissions(this)
        }
    }
    private fun isNumberBlocked(phoneNumber: String): Boolean {
        return try {
            val uri = BlockedNumberContract.BlockedNumbers.CONTENT_URI
            val projection = arrayOf(BlockedNumberContract.BlockedNumbers.COLUMN_ORIGINAL_NUMBER)
            val selection = "${BlockedNumberContract.BlockedNumbers.COLUMN_ORIGINAL_NUMBER} = ?"
            val selectionArgs = arrayOf(phoneNumber)
            val cursor = contentResolver.query(uri, projection, selection, selectionArgs, null)

            val isBlocked = cursor?.use {
                it.count > 0
            } ?: false

            if (isBlocked) {
                showToast(this,"Number $phoneNumber is blocked")
            }

            isBlocked
        } catch (e: Exception) {
            false
        }
    }
    private fun checkPermissions(context: Context): Boolean {
        return ContextCompat.checkSelfPermission(context, "android.permission.READ_CALL_LOG") == PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(context, "android.permission.WRITE_CALL_LOG") == PackageManager.PERMISSION_GRANTED
    }

    private fun requestPermissions(activity: Activity) {
        val permissions = arrayOf("android.permission.READ_CALL_LOG", "android.permission.WRITE_CALL_LOG")
        ActivityCompat.requestPermissions(activity, permissions, PhoneAcivity.REQUEST_CODE)
    }

    private fun deletePhrase(phrase: String) {
        // Delete the phrase from the database
        val deletedRows = dbHelper.deletePhrase(phrase)
        if (deletedRows > 0) {
            // Refresh the list after deletion
            loadPhrases()
        } else {
            // Handle the case where the deletion failed
            Toast.makeText(this, "Failed to delete phrase: $phrase", Toast.LENGTH_SHORT).show()
        }
    }
}