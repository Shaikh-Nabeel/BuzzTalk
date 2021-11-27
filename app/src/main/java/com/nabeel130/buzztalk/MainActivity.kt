package com.nabeel130.buzztalk

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.*
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.browser.customtabs.CustomTabsIntent
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.firebase.ui.firestore.FirestoreRecyclerOptions
import com.google.android.material.navigation.NavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.Query
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.FirebaseStorage
import com.nabeel130.buzztalk.daos.PostDao
import com.nabeel130.buzztalk.daos.UserDao
import com.nabeel130.buzztalk.databinding.ActivityMainBinding
import com.nabeel130.buzztalk.fragments.ProfileFragment
import com.nabeel130.buzztalk.models.Post
import com.nabeel130.buzztalk.models.User
import com.nabeel130.buzztalk.utility.Helper
import kotlinx.coroutines.*

class MainActivity : AppCompatActivity(), IPostAdapter,
    NavigationView.OnNavigationItemSelectedListener {
    private lateinit var binding: ActivityMainBinding
    private lateinit var adapter: PostAdapter
    private lateinit var postDao: PostDao
    private lateinit var userDao: UserDao
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
        binding.navigationView.setNavigationItemSelectedListener(this)

        val view = binding.navigationView.getHeaderView(0)
        val profile: ImageView = view.findViewById(R.id.profilePicForMenuBar)
        val userName: TextView = view.findViewById(R.id.userNameForMenuBar)



        userDao = UserDao()
        GlobalScope.launch(Dispatchers.IO){
            val userId = Firebase.auth.currentUser?.uid!!
            userDao.getUserById(userId).addOnCompleteListener {
                if(it.isSuccessful){
                    val user = it.result.toObject(User::class.java)
                    if (user != null) {
                        userName.text = user.userName
                        Glide.with(applicationContext).load(user.imageUrl).circleCrop().into(profile)
                    }
                }
            }
        }

        binding.createPostBtn.setOnClickListener {
            startActivity(Intent(this,CreatePostActivity::class.java))
            overridePendingTransition(android.R.anim.fade_in,android.R.anim.fade_out)
        }

        loadAllPost()
    }

    private fun loadAllPost() {
        postDao = PostDao()
        val postCollection = postDao.postCollection
        val query = postCollection.orderBy("createdAt",Query.Direction.DESCENDING)
        val recyclerViewOption = FirestoreRecyclerOptions.Builder<Post>().setQuery(query,Post::class.java).build()
        adapter = PostAdapter(recyclerViewOption, this)
        binding.recyclerView.adapter = adapter
        binding.recyclerView.layoutManager = LinearLayoutManager(this)
    }

    override fun onStart() {
        super.onStart()
        adapter.startListening()

        //code to show posting message while post is being posted
        binding.postingMssg.visibility = View.VISIBLE
        GlobalScope.launch(Dispatchers.IO) {
            var count = 1
            while(true){
                if(isPostingCompleted){
                    Log.d(Helper.TAG, "count : $count")
                    withContext(Dispatchers.Main){
                        binding.postingMssg.visibility = View.GONE
                        Toast.makeText(applicationContext,
                        "Posted",Toast.LENGTH_SHORT).show()
                    }
                    break
                }
                count += 1
                Log.d(Helper.TAG, "count : $count")
                delay(500)
            }
        }
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
        return when (item.itemId) {
            R.id.profileDetails -> {
                val profileFragment = ProfileFragment()
                binding.recyclerView.visibility = View.GONE
                binding.createPostBtn.visibility = View.GONE
                supportFragmentManager.beginTransaction().apply {
                    replace(R.id.frameLayoutForFragments,profileFragment)
                    addToBackStack(null)
                    commit()
                }
                binding.drawableLayout.closeDrawer(binding.navigationView)
                true
            }
            R.id.privacyPolicy -> {
                val builder = CustomTabsIntent.Builder().build()
                builder.launchUrl(this, Uri.parse(getString(R.string.privacy_policy_link)))
                binding.drawableLayout.closeDrawer(binding.navigationView)
                true
            }
            R.id.logOutBtn -> {
                singOut()
                binding.drawableLayout.closeDrawer(binding.navigationView)
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
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
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