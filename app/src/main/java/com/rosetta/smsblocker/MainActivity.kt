package com.rosetta.smsblocker
import android.app.Activity
import android.app.AlertDialog
import android.app.role.RoleManager
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.provider.Telephony
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
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
            }

        }
        recyclerView = findViewById(R.id.recyclerView)
        adapter = PhraseAdapter(ArrayList(), this::onPhraseClicked, this::deletePhrase)

        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter

        loadPhrases()

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

    private fun deleteSms()
    {
        // Retrieve phrases from the database
        val phrases = dbHelper.getAllPhrases().toTypedArray()
        Toast.makeText(this, "Started Deleteing", Toast.LENGTH_SHORT).show()
        // Start the MessageDeletionService service
        val serviceIntent = Intent(this, MessageDeletionService::class.java)
        serviceIntent.putExtra("phrases", phrases)
        startService(serviceIntent)
    }

    private fun prepareIntentLauncher() {
        intentLauncher =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
                if (it.resultCode == Activity.RESULT_OK) {
                    showToast("Success requesting ROLE_SMS!")

                    // Example phrases to delete
                    deleteSms()

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