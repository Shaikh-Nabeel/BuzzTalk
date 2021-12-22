package com.nabeel130.buzztalk

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
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.browser.customtabs.CustomTabsIntent
import androidx.core.view.GravityCompat
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
import com.nabeel130.buzztalk.daos.PostDao
import com.nabeel130.buzztalk.databinding.ActivityMainBinding
import com.nabeel130.buzztalk.fragments.CommentsFragment
import com.nabeel130.buzztalk.fragments.ProfileFragment
import com.nabeel130.buzztalk.models.Post
import com.nabeel130.buzztalk.notifications.Notifications
import com.nabeel130.buzztalk.notifications.PushNotification
import com.nabeel130.buzztalk.utility.Helper
import com.nabeel130.buzztalk.utility.Helper.Companion.MESSAGE
import com.nabeel130.buzztalk.utility.Helper.Companion.TOPIC
import com.nabeel130.buzztalk.utility.RetrofitInstance
import kotlinx.coroutines.*

class MainActivity : AppCompatActivity(), IPostAdapter,
    NavigationView.OnNavigationItemSelectedListener {

    private lateinit var binding: ActivityMainBinding
    private lateinit var adapter: PostAdapter
    private lateinit var postDao: PostDao
    private lateinit var likeAdapter: LikeAdapter
    private lateinit var toggle: ActionBarDrawerToggle

    companion object{
        var isPostingCompleted = true
        private var instance: MainActivity? = null

        fun getInstance(): MainActivity{
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

        binding.navigationView.bringToFront()
        toggle = ActionBarDrawerToggle(this,binding.drawableLayout,binding.customToolB,R.string.navigation_open,R.string.navigation_close)
        binding.drawableLayout.addDrawerListener(toggle)
        toggle.syncState()
        toggle.isDrawerIndicatorEnabled = false
        toggle.setHomeAsUpIndicator(R.drawable.ic_baseline_menu_24)
        toggle.setToolbarNavigationClickListener {
            if(binding.drawableLayout.isDrawerVisible(GravityCompat.START)){
                binding.drawableLayout.closeDrawer(GravityCompat.START)
            }else{
                binding.drawableLayout.openDrawer(GravityCompat.START)
            }
        }
        binding.navigationView.setNavigationItemSelectedListener(this)

        val view = binding.navigationView.getHeaderView(0)
        val profile: ImageView = view.findViewById(R.id.profilePicForMenuBar)
        val userName: TextView = view.findViewById(R.id.userNameForMenuBar)

        val user = Firebase.auth.currentUser!!
        userName.text = user.displayName
        currentUserId = user.uid
        currentUserName = user.displayName ?: ""
        currentUserProfileUrl = user.photoUrl.toString()
        Glide.with(applicationContext).load(user.photoUrl).circleCrop().into(profile)

        binding.createPostBtn.setOnClickListener {
            startActivity(Intent(this,CreatePostActivity::class.java))
            overridePendingTransition(android.R.anim.fade_in,android.R.anim.fade_out)
        }

        loadAllPost()

        //subscribing to topic to receive notification on current topic
        FirebaseMessaging.getInstance().subscribeToTopic(TOPIC)

//        val comment = Comments(currentUserId,"delete the fucking post",System.currentTimeMillis())
//        postDao.postComment("kDpXAEZO5hOl4BJZC2fY",comment)


    }

    private fun sendNotifications(notifications: PushNotification)= CoroutineScope(Dispatchers.IO).launch{
        try{
            val response = RetrofitInstance.api.postNotification(notifications)
            if(response.isSuccessful){
                Log.d(Helper.TAG, "Response: $response")
            }else{
                Log.d(Helper.TAG, "Response: ${response.errorBody()}")
            }
        }catch (e: Exception){
            e.printStackTrace()
        }
    }

    private fun loadAllPost() {
        postDao = PostDao()
        val postCollection = postDao.postCollection
        val query = postCollection.orderBy("createdAt",Query.Direction.DESCENDING)
        val recyclerViewOption = FirestoreRecyclerOptions.Builder<Post>().setQuery(query,Post::class.java).build()
        adapter = PostAdapter(recyclerViewOption, this)
        (binding.recyclerView.itemAnimator as SimpleItemAnimator).supportsChangeAnimations = false
        binding.recyclerView.adapter = adapter
        binding.recyclerView.layoutManager = LinearLayoutManager(this)
    }

    override fun onStart() {
        super.onStart()
        adapter.startListening()

        //code to show posting message while post is being posted
        if(!isPostingCompleted) {
            binding.postingMssg.visibility = View.VISIBLE
            GlobalScope.launch(Dispatchers.IO) {
                while (true) {
                    if (isPostingCompleted) {
                        withContext(Dispatchers.Main) {
                            binding.postingMssg.visibility = View.GONE
                            Toast.makeText(
                                applicationContext,
                                "Posted", Toast.LENGTH_SHORT
                            ).show()
                        }
                        //sending notification when post is uploaded
                        sendNotifications(PushNotification(getNotificationBody(),TOPIC))
                        break
                    }
                    delay(500)
                }
            }
        }
    }

    private fun getNotificationBody(): Notifications{
        return Notifications(
            currentUserName,
            "$MESSAGE $currentUserName",
            currentUserProfileUrl,
            currentUserId
        )
    }

    override fun onStop() {
        super.onStop()
        adapter.stopListening()
    }

    fun visibleComponentOfMainActivity(){
        binding.recyclerView.visibility = View.VISIBLE
        binding.createPostBtn.visibility = View.VISIBLE
    }

    override fun onPostLiked(postId: String) {
        GlobalScope.launch(Dispatchers.IO) {
            postDao.likedPost(postId)
        }
    }

    private var listOfLikedUser: ArrayList<String> = ArrayList()

    //function to show name of user who have liked the current post
    @SuppressLint("NotifyDataSetChanged")
    override fun onLikeCountClicked(postId: String) {

        binding.progressBarForLikes.visibility = View.VISIBLE
        binding.relativeLayoutForLikes.visibility = View.VISIBLE
        binding.recyclerView.visibility = View.GONE
        binding.createPostBtn.visibility = View.GONE

        binding.closeBtnForLikes.setOnClickListener {
            binding.relativeLayoutForLikes.visibility = View.GONE
            binding.recyclerView.visibility = View.VISIBLE
            binding.createPostBtn.visibility = View.VISIBLE
            listOfLikedUser.clear()
            likeAdapter.notifyDataSetChanged()
        }

        postDao.getPostById(postId).addOnCompleteListener {
            if (it.isSuccessful) {
                val post = it.result.toObject(Post::class.java)!!
                listOfLikedUser = post.likedBy
                likeAdapter = LikeAdapter(listOfLikedUser)
                binding.recyclerViewForLike.adapter = likeAdapter
                binding.progressBarForLikes.visibility = View.GONE
                binding.recyclerViewForLike.layoutManager = LinearLayoutManager(binding.relativeLayoutForLikes.context)
            }
        }
    }

    override fun onPostCommentPressed(postId: String) {
        val bundle = Bundle()
        bundle.putString("postId", postId)
        val commentsFragment = CommentsFragment()
        commentsFragment.arguments = bundle
        binding.recyclerView.visibility = View.GONE
        binding.createPostBtn.visibility = View.GONE
        Helper.isOpenedFromProfile = false
        supportFragmentManager.beginTransaction().apply {
            replace(R.id.frameLayoutForFragments,commentsFragment)
            addToBackStack(null)
            commit()
        }
    }

    override fun onDeletePostClicked(postId: String, uuid: String?) {

        val dialog = Helper.buildDialogBox(this,getString(R.string.areYouSure),getString(R.string.dialog_text_1))
        val confirmBtn: Button = dialog.findViewById(R.id.confirm_button)
        val denyBtn: Button = dialog.findViewById(R.id.deny_button)

        confirmBtn.setOnClickListener {
            dialog.dismiss()
            if(uuid != null) {
                val storageRef = FirebaseStorage.getInstance().getReference("images/$uuid")
                storageRef.delete().addOnSuccessListener {
                    Log.d(Helper.TAG, "Image delete uuid: $uuid")
                    deletePostData(postId)
                }.addOnFailureListener {
                    Log.d(Helper.TAG, it.message.toString())
                }
            } else{
                deletePostData(postId)
            }
        }

        denyBtn.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun deletePostData(postId: String){
        postDao.deletePost(postId).addOnCompleteListener {
            if (it.isSuccessful)
                Toast.makeText(applicationContext, "Deleted", Toast.LENGTH_SHORT).show()
            else
                Toast.makeText(applicationContext, "Couldn't delete", Toast.LENGTH_SHORT).show()
            Log.d(Helper.TAG, "Post deleted status: " + it.exception?.message)
        }
    }

    override fun onShareClicked(text: String, userName: String) {
        val intent = Intent(Intent.ACTION_SEND).setType("text/plain")
        intent.putExtra(Intent.EXTRA_SUBJECT,"BuzzTalk")
        intent.putExtra(Intent.EXTRA_TEXT, text)
        startActivity(Intent.createChooser(intent,"Post from $userName"))
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        binding.drawableLayout.closeDrawer(binding.navigationView)
        return when (item.itemId) {
            R.id.profileDetails -> {
                if(binding.recyclerView.visibility == View.GONE)
                    return true
                val profileFragment = ProfileFragment()
                binding.recyclerView.visibility = View.GONE
                binding.createPostBtn.visibility = View.GONE
                supportFragmentManager.beginTransaction().apply {
                    add(R.id.frameLayoutForFragments,profileFragment)
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
                Toast.makeText(applicationContext,"Coming soon",Toast.LENGTH_SHORT).show()
                true
            }
            else -> false
        }
    }

    private val authStateListener = FirebaseAuth.AuthStateListener{
        if(it.currentUser ==  null){
            val intent = Intent(this,SignInActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK
            startActivity(intent)
            finish()
        }
    }


    @SuppressLint("SetTextI18n")
    private fun singOut() {
        val dialog = Helper.buildDialogBox(this,getString(R.string.logout),getString(R.string.dialog_text_2))

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