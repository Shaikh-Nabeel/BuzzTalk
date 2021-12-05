package com.nabeel130.buzztalk

import android.app.Activity
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
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
import com.nabeel130.buzztalk.daos.UserDao
import com.nabeel130.buzztalk.databinding.ActivitySignInBinding
import com.nabeel130.buzztalk.models.User
import com.nabeel130.buzztalk.utility.Helper.Companion.PREF_KEY
import com.nabeel130.buzztalk.utility.Helper.Companion.USER_NAME

class SignInActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySignInBinding
    private lateinit var googleSignInClient: GoogleSignInClient
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySignInBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = Firebase.auth
        updateUI(auth.currentUser)

        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken("270364308072-f87u344doh6a96otasu8n8o5grcjmk7i.apps.googleusercontent.com")
            .requestEmail()
            .build()

        googleSignInClient = GoogleSignIn.getClient(this,gso)
        binding.googleSignInBtn.setOnClickListener {
            if(binding.checkBoxforPrivacy.isChecked) {
                signIn()
            }else{
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

//    override fun onStart() {
//        super.onStart()
//
//    }

    private val launcher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        if(it.resultCode == Activity.RESULT_OK){
            val task = GoogleSignIn.getSignedInAccountFromIntent(it.data)
            binding.googleSignInBtn.visibility = View.GONE
            binding.progressBarLogin.visibility = View.VISIBLE
            try{
                val account = task.getResult(ApiException::class.java)
                firebaseAuthWithGoogle(account.idToken)
            }catch(e: Exception){
                Toast.makeText(applicationContext,"Couldn't sign in!",Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun firebaseAuthWithGoogle(idToken: String?) {
        val credential = GoogleAuthProvider.getCredential(idToken, null)

        auth.signInWithCredential(credential).addOnCompleteListener(this) {
            if (it.isSuccessful) {
                val user = auth.currentUser
//                val pref = getPreferences().edit()
//                pref.putString(USER_NAME, user?.displayName)
                updateUI(user)
            } else {
                Toast.makeText(applicationContext, "Couldn't sign in!", Toast.LENGTH_SHORT).show()
            }
        }
    }

//    private fun getPreferences():SharedPreferences{
//        return applicationContext.getSharedPreferences(PREF_KEY, MODE_PRIVATE)
//    }

    private fun updateUI(user: FirebaseUser?) {
        if(user != null){
            val userObj = User(user.uid,user.email,user.displayName,user.photoUrl.toString())
            val userDao = UserDao()
            userDao.addUser(userObj)
            startActivity(Intent(this,MainActivity::class.java))
            finish()
        }else{
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