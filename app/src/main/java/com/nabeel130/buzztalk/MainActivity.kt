package com.nabeel130.buzztalk

import android.annotation.SuppressLint
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.firebase.ui.firestore.FirestoreRecyclerOptions
import com.google.firebase.firestore.Query
import com.nabeel130.buzztalk.daos.PostDao
import com.nabeel130.buzztalk.daos.UserDao
import com.nabeel130.buzztalk.databinding.ActivityMainBinding
import com.nabeel130.buzztalk.models.Post
import com.nabeel130.buzztalk.models.User
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity(), IPostAdapter {
    private lateinit var binding: ActivityMainBinding
    private lateinit var adapter: PostAdapter
    private lateinit var postDao: PostDao
    private lateinit var userDao: UserDao

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.createPostBtn.setOnClickListener {
            startActivity(Intent(this,CreatePostActivity::class.java))
        }
        userDao = UserDao()
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
    }

    override fun onStop() {
        super.onStop()
        adapter.stopListening()
    }

    override fun onPostLiked(postId: String) {
        postDao.likedPost(postId)
    }

    private lateinit var listOfLikedUser: ArrayList<String>
    private lateinit var customAdapter: CustomAdapter

    override fun onLikeCountClicked(postId: String) {

        binding.progressBarForLikes.visibility = View.VISIBLE
        binding.relativeLayoutForLikes.visibility = View.VISIBLE
        binding.recyclerView.visibility = View.GONE
        binding.closeBtnForLikes.setOnClickListener {
            binding.relativeLayoutForLikes.visibility = View.GONE
            binding.recyclerView.visibility = View.VISIBLE
        }

        GlobalScope.launch(Dispatchers.IO) {
            postDao.getPostById(postId).addOnCompleteListener {
                if (it.isSuccessful) {
                    val post = it.result.toObject(Post::class.java)!!
                    listOfLikedUser = post.likedBy
                    customAdapter = CustomAdapter()
                    binding.listViewForLike.adapter = customAdapter
                    binding.progressBarForLikes.visibility = View.GONE
                }
            }
        }

    }

    inner class CustomAdapter: BaseAdapter() {
        override fun getCount(): Int {
            return listOfLikedUser.size
        }

        override fun getItem(position: Int): Any? {
            return null
        }

        override fun getItemId(position: Int): Long {
            return 0
        }

        @SuppressLint("InflateParams", "ViewHolder")
        override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
            val view = layoutInflater.inflate(R.layout.likes_view_item,null)
            val userName = view.findViewById<TextView>(R.id.userNameForLike)
            val userImage = view.findViewById<ImageView>(R.id.profilePicForLike)

            userDao.getUserById(listOfLikedUser[position]).addOnCompleteListener {
                if(it.isSuccessful){
                    val user = it.result.toObject(User::class.java)!!
                    userName.text = user.userName
                    Glide.with(applicationContext).load(user.imageUrl).circleCrop().into(userImage)
                }
            }
            return view
        }
    }
}