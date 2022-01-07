package com.nabeel130.buzztalk.fragments

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.ktx.Firebase
import com.nabeel130.buzztalk.activity.MainActivity
import com.nabeel130.buzztalk.R
import com.nabeel130.buzztalk.adapter.CommentsAdapter
import com.nabeel130.buzztalk.adapter.ICommentAdapter
import com.nabeel130.buzztalk.daos.PostDao
import com.nabeel130.buzztalk.databinding.FragmentCommentsBinding
import com.nabeel130.buzztalk.models.Comments
import com.nabeel130.buzztalk.utility.Helper
import com.nabeel130.buzztalk.utility.Helper.Companion.TAG
import kotlinx.coroutines.*
import kotlinx.coroutines.tasks.await

class CommentsFragment : Fragment(), ICommentAdapter {

    private var _binding: FragmentCommentsBinding? = null
    private val binding get() = _binding!!
    private lateinit var postDao: PostDao
    private lateinit var adapter: CommentsAdapter
    private var user = Firebase.auth.currentUser!!
    private lateinit var listOfComment: Deferred<MutableList<DocumentSnapshot>>
    private lateinit var postId: String
    private lateinit var createdBy: String

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        try {
            postId = arguments?.getString("postId")!!
            createdBy = arguments?.getString("createdBy")!!
        } catch (e: Exception) {
            e.printStackTrace()
        }
        _binding = FragmentCommentsBinding.inflate(inflater, container, false)
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

        adapter = CommentsAdapter(this)
        binding.recyclerViewForComments.layoutManager = LinearLayoutManager(context)
        binding.recyclerViewForComments.adapter = adapter
        submitList()
        binding.postComment.setOnClickListener { postComment() }
    }

    private fun postComment() {
        val comment = binding.commentTextET.text.toString()
        binding.commentTextET.setText("")
        if (comment.isNotEmpty() && comment.isNotBlank()) {
            val comments = Comments(user.uid, comment.trim(), System.currentTimeMillis())
            postDao.postComment(postId, comments)
                .addOnCompleteListener {
                    if (it.isSuccessful) Log.d(TAG, "Comment posted")
                    listOfComment = GlobalScope.async(Dispatchers.IO) {
                        loadComments()
                    }
                    submitList()
                }
        }
    }

    private suspend fun loadComments(): MutableList<DocumentSnapshot> {
        return postDao.loadComments(postId).await().documents
    }

    private fun submitList() {

        GlobalScope.launch(Dispatchers.Main) {
            if (listOfComment.await().size > 0 && _binding != null) {
                binding.commentBg.visibility = View.GONE
                binding.noCommentT.visibility = View.GONE
            } else {
                binding.progressBarComment.visibility = View.INVISIBLE
                return@launch
            }
            adapter.differ.submitList(listOfComment.await())
            binding.noOfComments.text = listOfComment.await().size.toString()
            binding.progressBarComment.visibility = View.INVISIBLE
        }

    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
        if (Helper.isOpenedFromProfile) return
        MainActivity.getInstance().visibleComponentOfMainActivity()
    }

    override fun onLongClick(commentSnapId: String, commentId: String, position: Int) {

        if (commentId != Firebase.auth.currentUser?.uid) {
            Log.d(TAG, "Comment is not created by user")
            if (createdBy != Firebase.auth.currentUser?.uid) {
                Log.d(TAG, "Post is created by the user")
                return
            }
        }

        Log.d(TAG, "position: $position, id: $commentSnapId")
        val dialog = Helper.buildDialogBox(
            requireContext(),
            "Are you sure?",
            "Do you want to delete this comment?"
        )

        val confirmBtn: Button = dialog.findViewById(R.id.confirm_button)
        val denyBtn: Button = dialog.findViewById(R.id.deny_button)

        dialog.window?.attributes?.windowAnimations = android.R.anim.fade_in

        confirmBtn.setOnClickListener {
            dialog.dismiss()
            postDao.deleteComment(commentSnapId, postId).addOnCompleteListener {
                if (!it.isSuccessful) return@addOnCompleteListener
                val copyOfComment = runBlocking {
                    listOfComment.await().toMutableList()
                }
                GlobalScope.launch(Dispatchers.Main) {
                    listOfComment.await().removeAt(position)
                }
                copyOfComment.removeAt(position)
                if (copyOfComment.size < 1) {
                    binding.commentBg.visibility = View.VISIBLE
                    binding.noCommentT.visibility = View.VISIBLE
                }
                adapter.differ.submitList(copyOfComment)
                binding.noOfComments.text = copyOfComment.size.toString()
            }
        }

        denyBtn.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }

}