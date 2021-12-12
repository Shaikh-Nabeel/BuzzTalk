package com.nabeel130.buzztalk.fragments

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.ktx.Firebase
import com.nabeel130.buzztalk.MainActivity
import com.nabeel130.buzztalk.daos.PostDao
import com.nabeel130.buzztalk.databinding.FragmentCommentsBinding
import com.nabeel130.buzztalk.models.Comments
import com.nabeel130.buzztalk.utility.Helper.Companion.TAG
import kotlinx.coroutines.*
import kotlinx.coroutines.tasks.await

class CommentsFragment : Fragment() {

    private var _binding: FragmentCommentsBinding? = null
    private val binding get() = _binding!!
    private lateinit var postDao: PostDao
    private lateinit var adapter: CommentsAdapter
    private var user = Firebase.auth.currentUser!!
    private lateinit var listOfComment: Deferred<MutableList<DocumentSnapshot>>
    private lateinit var postId: String

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        postId = arguments?.getString("postId")!!
        _binding = FragmentCommentsBinding.inflate(inflater,container,false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        postDao = PostDao()

        listOfComment = GlobalScope.async(Dispatchers.IO) {
            Log.d(TAG, "loading post....")
            loadComments()
        }
        Log.d(TAG, "loading post done......")

        adapter = CommentsAdapter()
        binding.recyclerViewForComments.layoutManager = LinearLayoutManager(context)
        binding.recyclerViewForComments.adapter = adapter
        submitList()
        binding.postComment.setOnClickListener { postComment() }
    }

    private fun postComment() {
        val comment = binding.commentTextET.text.toString()
        binding.commentTextET.setText("")
        if(comment.isNotEmpty() || comment.isNotBlank()){
            val comments = Comments(user.uid,comment,System.currentTimeMillis())
            postDao.postComment(
                postId,
                comments)
                .addOnCompleteListener {
                    if(it.isSuccessful) Log.d(TAG, "Comment posted")
                    listOfComment = GlobalScope.async(Dispatchers.IO) {
                        loadComments()
                    }
                    submitList()
                }
        }
    }

    private suspend fun loadComments(): MutableList<DocumentSnapshot> {
//        return GlobalScope.async(Dispatchers.IO) {
            return postDao.loadComments(postId).await().documents
//        }
    }

    private fun submitList(){
        GlobalScope.launch(Dispatchers.Main) {
            adapter.differ.submitList(listOfComment.await())
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
        MainActivity.getInstance().visibleComponentOfMainActivity()
    }

}