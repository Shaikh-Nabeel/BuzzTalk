package com.nabeel130.buzztalk

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import androidx.recyclerview.widget.LinearLayoutManager
import com.firebase.ui.firestore.FirestoreRecyclerOptions
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.Query
import com.google.firebase.ktx.Firebase
import com.nabeel130.buzztalk.daos.PostDao
import com.nabeel130.buzztalk.databinding.ActivityMainBinding
import com.nabeel130.buzztalk.models.Post

class MainActivity : AppCompatActivity(), IPostAdapter {
    private lateinit var binding: ActivityMainBinding
    private lateinit var adapter: PostAdapter
    private lateinit var postDao: PostDao

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.createPostBtn.setOnClickListener {
            startActivity(Intent(this,CreatePostActivity::class.java))
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
    }

    override fun onStop() {
        super.onStop()
        adapter.stopListening()
    }

    override fun onPostLiked(postId: String) {
        postDao.likedPost(postId)
    }
}