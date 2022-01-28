package com.nabeel130.buzztalk.activity

import android.app.Activity
import android.content.Intent
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.browser.customtabs.CustomTabsIntent
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import com.nabeel130.buzztalk.R
import com.nabeel130.buzztalk.daos.UserDao
import com.nabeel130.buzztalk.databinding.ActivitySignInBinding
import com.nabeel130.buzztalk.models.User
import androidx.core.content.ContextCompat
import com.nabeel130.buzztalk.utility.Constants
import com.nabeel130.buzztalk.utility.PreferenceManager
import kotlinx.coroutines.*
import kotlinx.coroutines.tasks.await


class SignInActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySignInBinding
    private lateinit var googleSignInClient: GoogleSignInClient
    private lateinit var auth: FirebaseAuth
    private var TAG = "SIGN_IN_LOG"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySignInBinding.inflate(layoutInflater)
        setContentView(binding.root)

        PreferenceManager(this)
        setStatusBarGradiant(this)
        Log.d(TAG, "in on create method sign in")
        auth = Firebase.auth

        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken("270364308072-f87u344doh6a96otasu8n8o5grcjmk7i.apps.googleusercontent.com")
            .requestEmail()
            .build()

        googleSignInClient = GoogleSignIn.getClient(this, gso)
        updateUI(auth.currentUser)
        binding.googleSignInBtn.setOnClickListener {
            if (binding.checkBoxforPrivacy.isChecked) {
                signIn()
            } else {
                Toast.makeText(
                    applicationContext,
                    "Accept Terms and Conditions",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

        binding.learnMoreTxt.setOnClickListener {
            val builder = CustomTabsIntent.Builder().build()
            builder.launchUrl(this, Uri.parse(getString(R.string.privacy_policy_link)))
        }

    }

    private fun setStatusBarGradiant(activity: Activity) {
        val window = activity.window
        val background =
            ContextCompat.getDrawable(applicationContext, R.drawable.sign_in_background)
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
        window.statusBarColor =
            ContextCompat.getColor(applicationContext, android.R.color.transparent)
        window.navigationBarColor =
            ContextCompat.getColor(applicationContext, android.R.color.transparent)
        window.setBackgroundDrawable(background)
    }

    private val launcher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        if (it.resultCode == Activity.RESULT_OK) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(it.data)
            binding.googleSignInBtn.visibility = View.GONE
            binding.progressBarLogin.visibility = View.VISIBLE
            try {
                val account = task.getResult(ApiException::class.java)
                firebaseAuthWithGoogle(account.idToken)
            } catch (e: Exception) {
                Toast.makeText(applicationContext, "Couldn't sign in!", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun firebaseAuthWithGoogle(idToken: String?) {
        val credential = GoogleAuthProvider.getCredential(idToken, null)

        auth.signInWithCredential(credential).addOnCompleteListener(this) {
            if (it.isSuccessful) {
                val user = auth.currentUser ?: return@addOnCompleteListener

                GlobalScope.launch {
                    if (!isAccountExistAsync(user).await()) {
                        Log.d(TAG, "Account does not exit....")
                        PreferenceManager.setString(Constants.IMAGE_URL, user.photoUrl.toString())
                        val userObj =
                            User(
                                user.uid,
                                user.email,
                                user.displayName,
                                user.photoUrl.toString(),
                                ""
                            )

                        val userDao = UserDao()
                        userDao.addUser(userObj)
                    }
                    Log.d(TAG, "skipping....")
                    updateUI(user)
                }
            } else {
                Toast.makeText(applicationContext, "Couldn't sign in!", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun isAccountExistAsync(user: FirebaseUser): Deferred<Boolean> {
        val userDao = UserDao()
        val res =  GlobalScope.async(Dispatchers.Main) {
            var temp = false
            Log.d(TAG, "Temp value 0: $temp")
            val currUser = userDao.getUserById(user.uid).await().toObject(User::class.java)
            if (currUser != null) {
                temp = true
                PreferenceManager.setString(Constants.IMAGE_URL, currUser.imageUrl ?: "")
                PreferenceManager.setString(Constants.USER_BIO, currUser.bio ?: "")
            }
            Log.d(TAG, "Temp value 1 : $temp")

            return@async temp
        }
        return res
    }

    private fun updateUI(user: FirebaseUser?) {
        if (user != null) {
            Log.d(TAG, "updating UI")
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        } else {
            PreferenceManager.deletePref()
            googleSignInClient.signOut()
            binding.googleSignInBtn.visibility = View.VISIBLE
            binding.progressBarLogin.visibility = View.GONE
        }
    }

    private fun signIn() {
        val signInIntent = googleSignInClient.signInIntent
        launcher.launch(signInIntent)
    }
}