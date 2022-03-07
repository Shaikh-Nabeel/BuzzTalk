package com.nabeel130.buzztalk.activity

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.browser.customtabs.CustomTabsIntent
import androidx.core.view.GravityCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.SimpleItemAnimator
import com.bumptech.glide.Glide
import com.firebase.ui.firestore.FirestoreRecyclerOptions
import com.google.android.material.navigation.NavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.Query
import com.google.firebase.ktx.Firebase
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.storage.FirebaseStorage
import com.nabeel130.buzztalk.R
import com.nabeel130.buzztalk.adapter.IPostAdapter
import com.nabeel130.buzztalk.adapter.LikeAdapter
import com.nabeel130.buzztalk.adapter.PostAdapter
import com.nabeel130.buzztalk.daos.PostDao
import com.nabeel130.buzztalk.databinding.ActivityMainBinding
import com.nabeel130.buzztalk.fragments.CommentsFragment
import com.nabeel130.buzztalk.fragments.HomeFragment
import com.nabeel130.buzztalk.fragments.ProfileFragment
import com.nabeel130.buzztalk.fragments.ProfileFragment2
import com.nabeel130.buzztalk.models.Post
import com.nabeel130.buzztalk.notifications.Notifications
import com.nabeel130.buzztalk.notifications.PushNotification
import com.nabeel130.buzztalk.utility.Constants
import com.nabeel130.buzztalk.utility.Constants.Companion.MESSAGE
import com.nabeel130.buzztalk.utility.Constants.Companion.TOPIC
import com.nabeel130.buzztalk.utility.GlideApp
import com.nabeel130.buzztalk.utility.PreferenceManager
import com.nabeel130.buzztalk.utility.RetrofitInstance
import kotlinx.coroutines.*
import java.util.*
import kotlin.collections.ArrayList

class MainActivity : AppCompatActivity(),
    NavigationView.OnNavigationItemSelectedListener {

    private lateinit var binding: ActivityMainBinding


    private lateinit var toggle: ActionBarDrawerToggle

    companion object {
        var isPostingCompleted = true
        private var instance: MainActivity? = null

        fun getInstance(): MainActivity {
            return instance ?: MainActivity().apply {
                instance = this
            }
        }

        lateinit var currentUserName: String
        lateinit var currentUserId: String
        lateinit var currentUserProfileUrl: String
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        instance = this
        binding.customToolB.title = getString(R.string.app_name)
        binding.customToolB.setTitleTextColor(Color.WHITE)
        setSupportActionBar(binding.customToolB)

        try {
            setUpNavigationDrawer()
            setUpNavigationHeader()
//            binding.bottomNavigation.setItemSelected(R.id.homeBtn,true)
//            binding.bottomNavigation.setOnItemSelectedListener {
//                when(it){
//                    R.id.homeBtn -> supportFragmentManager.popBackStack()
//                    R.id.notification -> Log.d("BottomNavTest", "notification")
//                    R.id.profile -> loadFragmentWithBack(ProfileFragment())
//                }
//            }
            loadFragment(HomeFragment())

            //subscribing to topic to receive notification on current topic
            FirebaseMessaging.getInstance().subscribeToTopic(TOPIC)

        } catch (e: Exception) {
            e.printStackTrace()
        }
        
    }

    private fun loadFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction().apply {
            add(R.id.frameLayoutForFragments, fragment)
            commit()
        }
    }

    private fun loadFragmentWithBack(fragment: Fragment){
        supportFragmentManager.beginTransaction().apply {
            add(R.id.frameLayoutForFragments, fragment)
            addToBackStack(null)
            commit()
        }
    }


    private fun setUpNavigationDrawer() {
        binding.navigationView.bringToFront()
        toggle = ActionBarDrawerToggle(
            this, binding.drawableLayout, binding.customToolB,
            R.string.navigation_open,
            R.string.navigation_close
        )
        binding.drawableLayout.addDrawerListener(toggle)
        toggle.syncState()
        toggle.isDrawerIndicatorEnabled = false
        toggle.setHomeAsUpIndicator(R.drawable.ic_baseline_menu_24)
        toggle.setToolbarNavigationClickListener {
            if (binding.drawableLayout.isDrawerVisible(GravityCompat.START)) {
                binding.drawableLayout.closeDrawer(GravityCompat.START)
            } else {
                binding.drawableLayout.openDrawer(GravityCompat.START)
            }
        }
        binding.navigationView.setNavigationItemSelectedListener(this)
    }

    private fun setUpNavigationHeader() {
        val view = binding.navigationView.getHeaderView(0)
        val profile: ImageView = view.findViewById(R.id.profilePicForMenuBar)
        val userName: TextView = view.findViewById(R.id.userNameForMenuBar)

        val user = Firebase.auth.currentUser!!
        userName.text = user.displayName
        currentUserId = user.uid
        currentUserName = user.displayName ?: ""
        currentUserProfileUrl = PreferenceManager.getString(Constants.IMAGE_URL)

        if (currentUserProfileUrl == user.photoUrl.toString()) {
            Glide.with(applicationContext).load(currentUserProfileUrl).circleCrop().into(profile)
        } else {
            val ref =
                FirebaseStorage.getInstance().reference.child(Constants.USER_IMG + "/" + currentUserProfileUrl)
            GlideApp.with(applicationContext).load(ref).circleCrop().into(profile)
        }
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        binding.drawableLayout.closeDrawer(binding.navigationView)
        return when (item.itemId) {
            R.id.profileDetails -> {
                val profileFragment = ProfileFragment()
                supportFragmentManager.beginTransaction().apply {
                    add(R.id.frameLayoutForFragments, profileFragment)
                    addToBackStack(null)
                    commit()
                }
                true
            }
            R.id.privacyPolicy -> {
                val builder = CustomTabsIntent.Builder().build()
                builder.launchUrl(this, Uri.parse(getString(R.string.privacy_policy_link)))
                true
            }
            R.id.logOutBtn -> {
                singOut()
                true
            }
            R.id.shareApp -> {
                Toast.makeText(applicationContext, "Coming soon", Toast.LENGTH_SHORT).show()
                true
            }
            else -> false
        }
    }

    private val authStateListener = FirebaseAuth.AuthStateListener {
        if (it.currentUser == null) {
            val intent = Intent(this, SignInActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK
            startActivity(intent)
            finish()
        }
    }


    @SuppressLint("SetTextI18n")
    private fun singOut() {
        val dialog = Constants.buildDialogBox(
            this,
            getString(R.string.logout),
            getString(R.string.dialog_text_2)
        )

        val confirmBtn: Button = dialog.findViewById(R.id.confirm_button)
        val denyBtn: Button = dialog.findViewById(R.id.deny_button)
        confirmBtn.text = "Log Out"
        denyBtn.text = "Cancel"

        confirmBtn.setOnClickListener {
            dialog.dismiss()
            val firebaseAuth = FirebaseAuth.getInstance()
            firebaseAuth.addAuthStateListener(authStateListener)
            firebaseAuth.signOut()
        }
        denyBtn.setOnClickListener {
            dialog.dismiss()
        }
        dialog.show()
    }

}