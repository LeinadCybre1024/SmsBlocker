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


class MainActivity : AppCompatActivity() {

    private lateinit var intentLauncher: ActivityResultLauncher<Intent>
    private var hasPermision = false;
    private lateinit var dbHelper: DBHelper
    private lateinit var phrase :EditText
    private lateinit var addPhrase:Button
    private lateinit var deletePhrases:Button
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: PhraseAdapter

    companion object {
        // The requested role.
        const val role = RoleManager.ROLE_SMS

        private const val REQUEST_CODE = 123
        private fun showToast(mainActivity: MainActivity, message: String) {
            Toast.makeText(mainActivity, message, Toast.LENGTH_SHORT).show()
        }
    }



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        var isSmsDefaultApp: Boolean
        try {
            isSmsDefaultApp = Telephony.Sms.getDefaultSmsPackage(this) == packageName
        } catch (e: Exception) {
            // Handle any exceptions that might occur
            isSmsDefaultApp = false
        }

        if (!isSmsDefaultApp) {
            // Proceed with asking for default SMS handler permission
            prepareIntentLauncher()
            askDefaultSmsHandlerPermission()
        }

        val intentLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                // Handle the result here
                showToast("Success requesting ROLE_SMS!")
                hasPermision = true
                // Example phrases to delete
            } else {
                showToast("Failed requesting ROLE_SMS")
            }
        }
        dbHelper = DBHelper(this)

        phrase = findViewById(R.id.phrase)
        addPhrase = findViewById(R.id.addphrase)
        deletePhrases = findViewById(R.id.deletePhrases)

        addPhrase.setOnClickListener {
            val phraseText = phrase.text.toString().trim()
            if (phraseText.isNotEmpty()) {
                // Add the phrase to the database
                val id = dbHelper.addPhrase(phraseText)
                if (id != -1L) {
                    Toast.makeText(this, "Phrase saved to database", Toast.LENGTH_SHORT).show()
                    phrase.setText("")
                    if (!isSmsDefaultApp) {
                        // Proceed with asking for default SMS handler permission
                        showDefaultSmsAppDialog()
                    }else {
                        // Clear the input field after adding the phrase


                        deleteSms()

                        // Perform additional actions after adding the phrase

                        loadPhrases()
                    }
                } else {
                    Toast.makeText(this, "Failed to save phrase to database", Toast.LENGTH_SHORT).show()
                }

            } else {
                Toast.makeText(this, "Please enter a phrase", Toast.LENGTH_SHORT).show()
            }
        }
        deletePhrases.setOnClickListener {
            if (!isSmsDefaultApp) {
                // Proceed with asking for default SMS handler permission
                showDefaultSmsAppDialog()
            }else {
                // Clear the input field after adding the phrase


                deleteSms()

                // Perform additional actions after adding the phrase

                loadPhrases()

                removeSpamCalls()
            }

        }
        recyclerView = findViewById(R.id.recyclerView)
        adapter = PhraseAdapter(ArrayList(), this::onPhraseClicked, this::deletePhrase)

        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter

        loadPhrases()

        if (checkPermissions(this)) {
            removeSpamCalls()
        }

        // Use the phrases from the database for SMS deletion



    }

    private fun showDefaultSmsAppDialog() {
        AlertDialog.Builder(this)
            .setTitle("Permission Required")
            .setMessage("This app needs to be set as the default SMS app to perform this action. Do you want to set it as the default SMS app now?")
            .setPositiveButton("Yes") { dialog, which ->
                // Proceed with asking for default SMS handler permission
                askDefaultSmsHandlerPermission()
            }
            .setNegativeButton("No") { dialog, which ->
                // Do nothing or handle accordingly
            }
            .show()
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

    private fun removeSpamCalls() {
        // Check if the app has the necessary permissions
        if (checkPermissions(this)) {
            // Get the call log entries
            val callLogUri = CallLog.Calls.CONTENT_URI
            val projection = arrayOf(CallLog.Calls.NUMBER, CallLog.Calls.DATE)
            val cursor = contentResolver.query(callLogUri, projection, null, null, null)

            // Define the array of phone number prefixes to check
            val prefixesToCheck = arrayOf("020", "0730", "0709")

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
                            showToast("Call record deleted: $phoneNumber")
                        } else {
                            showToast("Error deleting call record: $phoneNumber")
                        }
                    }
                }
            }
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
                showToast("Number $phoneNumber is blocked")
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
        ActivityCompat.requestPermissions(activity, permissions, REQUEST_CODE)
    }
    private fun deleteSms()
    {
        // Retrieve phrases from the database
        val phrases = dbHelper.getAllPhrases().toTypedArray()
        Toast.makeText(this, "Started Deleteing Service", Toast.LENGTH_SHORT).show()
        // Start the MessageDeletionService service
        val serviceIntent = Intent(this, MessageDeletionService::class.java)
        serviceIntent.putExtra("phrases", phrases)
        startService(serviceIntent)
    }

    private fun prepareIntentLauncher() {
        intentLauncher =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
                if (it.resultCode == Activity.RESULT_OK) {
                    showToast("Success requesting , Enjoy !!!")

                    // Example phrases to delete
                    deleteSms()
                    loadPhrases()
                } else {
                    showToast("Failed requesting ROLE_SMS")
                }
            }
    }



    private fun askDefaultSmsHandlerPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val roleManager: RoleManager = getSystemService(RoleManager::class.java)
            // check if the app is having permission to be as default SMS app
            val isRoleAvailable = roleManager.isRoleAvailable(role)
            if (isRoleAvailable) {
                // check whether your app is already holding the default SMS app role.
                val isRoleHeld = roleManager.isRoleHeld(role)
                if (!isRoleHeld) {
                    intentLauncher.launch(roleManager.createRequestRoleIntent(role))
                } else {
                    // Request permission for SMS
                }
            }
        } else {
            val intent = Intent(Telephony.Sms.Intents.ACTION_CHANGE_DEFAULT)
            intent.putExtra(Telephony.Sms.Intents.EXTRA_PACKAGE_NAME, packageName)
            startActivityForResult(intent, 1001)
        }

    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }


}