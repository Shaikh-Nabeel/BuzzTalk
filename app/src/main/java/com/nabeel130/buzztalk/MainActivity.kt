package com.nabeel130.buzztalk

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Color
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.core.content.ContextCompat
import androidx.core.view.get
import androidx.core.view.iterator
import androidx.core.view.size
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.firebase.ui.firestore.FirestoreRecyclerOptions
import com.google.android.material.navigation.NavigationView
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.Query
import com.google.firebase.ktx.Firebase
import com.nabeel130.buzztalk.daos.PostDao
import com.nabeel130.buzztalk.daos.UserDao
import com.nabeel130.buzztalk.databinding.ActivityMainBinding
import com.nabeel130.buzztalk.models.Post
import com.nabeel130.buzztalk.models.User
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : AppCompatActivity(), IPostAdapter {
    private lateinit var binding: ActivityMainBinding
    private lateinit var adapter: PostAdapter
    private lateinit var postDao: PostDao
    private lateinit var userDao: UserDao
    private lateinit var likeAdapter: LikeAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.customToolB.title = getString(R.string.app_name)
        binding.customToolB.setTitleTextColor(Color.WHITE)
        setSupportActionBar(binding.customToolB)

        userDao = UserDao()


        val toggle = ActionBarDrawerToggle(this,binding.drawableLayout,binding.customToolB,R.string.navigation_open,R.string.navigation_close)
        binding.drawableLayout.addDrawerListener(toggle)
        toggle.syncState()

        val view = binding.navigationView.getHeaderView(0)
        val profile: ImageView = view.findViewById(R.id.profilePicForMenuBar)
        val userName: TextView = view.findViewById(R.id.userNameForMenuBar)

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

        createMenu()

        binding.createPostBtn.setOnClickListener {
            startActivity(Intent(this,CreatePostActivity::class.java))
        }
        loadAllPost()
    }

    private fun createMenu(){
        binding.navigationView.setNavigationItemSelectedListener {
            val id = it.itemId
            Log.d("BuzzReport", "reachingg....")
            when (id) {
                R.id.profileDetails -> {
                    Log.d("BuzzReport", "profileDetails")
                }
                R.id.privacyPolicy -> {
                    Log.d("BuzzReport", "privacy policy")
                }
                R.id.logOutBtn -> {
                    signOut()
                }
                R.id.shareApp -> {
                    Log.d("BuzzReport", "Share")
                }
            }
            true
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val id = item.itemId

        if(id == R.id.profileDetails){
            Toast.makeText(applicationContext,
            "Clickedd",
            Toast.LENGTH_SHORT).show()
        }

        return super.onOptionsItemSelected(item)
    }

    fun signOut(){

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
    }

    override fun onStop() {
        super.onStop()
        adapter.stopListening()
    }

    override fun onPostLiked(postId: String) {
        postDao.likedPost(postId)
    }

    private var listOfLikedUser: ArrayList<String> = ArrayList()

    //funtion to show name of user who have liked current post
    @SuppressLint("NotifyDataSetChanged")
    override fun onLikeCountClicked(postId: String) {

        binding.progressBarForLikes.visibility = View.VISIBLE
        binding.relativeLayoutForLikes.visibility = View.VISIBLE
        binding.recyclerView.visibility = View.GONE
        binding.closeBtnForLikes.setOnClickListener {
            binding.relativeLayoutForLikes.visibility = View.GONE
            binding.recyclerView.visibility = View.VISIBLE
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

    override fun onDeletePostClicked(postId: String) {
        postDao.deletePost(postId).addOnCompleteListener {
            if(it.isSuccessful)
                Toast.makeText(applicationContext,"Deleted",Toast.LENGTH_SHORT).show()
            else
                Toast.makeText(applicationContext,"Couldn't delete",Toast.LENGTH_SHORT).show()
            Log.d("BuzzReport","Post deleted status: "+it.exception?.message)
        }
    }

    override fun onShareClicked(text: String, userName: String) {
        val intent = Intent(Intent.ACTION_SEND).setType("text/plain")
        intent.putExtra(Intent.EXTRA_TEXT, text)
        startActivity(Intent.createChooser(intent,"Post from $userName"))
    }

}