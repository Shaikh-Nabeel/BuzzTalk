package com.nabeel130.buzztalk.fragments

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.SimpleItemAnimator
import com.firebase.ui.firestore.FirestoreRecyclerOptions
import com.google.firebase.firestore.Query
import com.google.firebase.storage.FirebaseStorage
import com.nabeel130.buzztalk.R
import com.nabeel130.buzztalk.activity.CreatePostActivity
import com.nabeel130.buzztalk.activity.MainActivity
import com.nabeel130.buzztalk.adapter.IPostAdapter
import com.nabeel130.buzztalk.adapter.LikeAdapter
import com.nabeel130.buzztalk.adapter.PostAdapter
import com.nabeel130.buzztalk.daos.PostDao
import com.nabeel130.buzztalk.databinding.FragmentCommentsBinding
import com.nabeel130.buzztalk.databinding.FragmentHomeBinding
import com.nabeel130.buzztalk.models.Post
import com.nabeel130.buzztalk.notifications.Notifications
import com.nabeel130.buzztalk.notifications.PushNotification
import com.nabeel130.buzztalk.utility.Constants
import com.nabeel130.buzztalk.utility.RetrofitInstance
import kotlinx.coroutines.*

class HomeFragment : Fragment(), IPostAdapter {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!
    private lateinit var adapter: PostAdapter
    private lateinit var postDao: PostDao


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.createPostBtn.setOnClickListener {
            createPostLauncher.launch(Intent(activity, CreatePostActivity::class.java))
            activity?.overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
        }
        loadAllPost()
    }

    private fun loadAllPost() {
        postDao = PostDao()
        val postCollection = postDao.postCollection
        val query = postCollection.orderBy("createdAt", Query.Direction.DESCENDING)
        val recyclerViewOption =
            FirestoreRecyclerOptions.Builder<Post>().setQuery(query, Post::class.java).build()
        binding.recyclerView.layoutManager = LinearLayoutManager(activity)
        (binding.recyclerView.itemAnimator as SimpleItemAnimator).supportsChangeAnimations = false
        adapter = PostAdapter(recyclerViewOption, this)
        binding.recyclerView.adapter = adapter
        Log.d(Constants.TAG, "Reached here..............")
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
        GlobalScope.launch(Dispatchers.IO) {
            postDao.likedPost(postId)
        }
    }

    override fun onPostCommentPressed(postId: String, createdBy: String) {
        val bundle = Bundle()
        bundle.putString("postId", postId)
        bundle.putString("createdBy", createdBy)
        val commentsFragment = CommentsFragment()
        commentsFragment.arguments = bundle
        Constants.isOpenedFromProfile = false
        (activity as MainActivity).supportFragmentManager.beginTransaction().apply {
            add(R.id.frameLayoutForFragments, commentsFragment)
            addToBackStack(null)
            commit()
        }
    }

    override fun onDeletePostClicked(postId: String, uuid: String?) {

        val dialog = Constants.buildDialogBox(
            requireContext(),
            getString(R.string.areYouSure),
            getString(R.string.dialog_text_1)
        )
        val confirmBtn: Button = dialog.findViewById(R.id.confirm_button)
        val denyBtn: Button = dialog.findViewById(R.id.deny_button)

        confirmBtn.setOnClickListener {
            dialog.dismiss()
            if (uuid != null) {
                val storageRef = FirebaseStorage.getInstance().getReference("images/$uuid")
                storageRef.delete().addOnSuccessListener {
                    Log.d(Constants.TAG, "Image delete uuid: $uuid")
                    deletePostData(postId)
                }.addOnFailureListener {
                    Log.d(Constants.TAG, it.message.toString())
                }
            } else {
                deletePostData(postId)
            }
        }

        denyBtn.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun deletePostData(postId: String) {
        postDao.deletePost(postId).addOnCompleteListener {
            if (it.isSuccessful)
                Toast.makeText(context, "Deleted", Toast.LENGTH_SHORT).show()
            else
                Toast.makeText(context, "Couldn't delete", Toast.LENGTH_SHORT).show()
            Log.d(Constants.TAG, "Post deleted status: " + it.exception?.message)
        }
    }

    override fun onShareClicked(text: String, userName: String) {
        val intent = Intent(Intent.ACTION_SEND).setType("text/plain")
        intent.putExtra(Intent.EXTRA_SUBJECT, "BuzzTalk")
        intent.putExtra(Intent.EXTRA_TEXT, text)
        startActivity(Intent.createChooser(intent, "Post from $userName"))
    }

    override fun onLikeCountClicked(postId: String) {
        val bundle = Bundle()
        bundle.putString("postId", postId)
        val likeFragment = LikeFragment()
        likeFragment.arguments = bundle
        (activity as MainActivity).supportFragmentManager.beginTransaction().apply {
            add(R.id.frameLayoutForFragments,likeFragment)
            addToBackStack(null)
            commit()
        }
    }

    private val createPostLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) {
        if (it.resultCode == AppCompatActivity.RESULT_OK) {
            try {
                val data = it.data!!
                if (data.getStringExtra("post").equals("true")) {
                    Toast.makeText(
                        context,
                        "Posted", Toast.LENGTH_SHORT
                    ).show()
                    binding.recyclerView.scrollToPosition(0);
                    sendNotifications(PushNotification(getNotificationBody(), Constants.TOPIC))
                    Log.d(Constants.TAG, "Found data")
                }
            } catch (e: Exception) {
                Log.d(Constants.TAG, "data not found")
                e.printStackTrace()
            }
        }
    }

    private fun sendNotifications(notifications: PushNotification) =
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = RetrofitInstance.api.postNotification(notifications)
                if (response.isSuccessful) {
                    Log.d(Constants.TAG, "Response: $response")
                } else {
                    Log.d(Constants.TAG, "Response: ${response.errorBody()}")
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

    private fun getNotificationBody(): Notifications {
        return Notifications(
            MainActivity.currentUserName,
            "${Constants.MESSAGE} ${MainActivity.currentUserName}",
            MainActivity.currentUserProfileUrl,
            MainActivity.currentUserId
        )
    }


    override fun onResume() {
        super.onResume()

        //code to show posting message while post(image) is being posted
        if (!MainActivity.isPostingCompleted) {
            binding.postingMssg.visibility = View.VISIBLE
            GlobalScope.launch(Dispatchers.IO) {
                while (true) {
                    if (MainActivity.isPostingCompleted) {
                        withContext(Dispatchers.Main) {
                            binding.postingMssg.visibility = View.GONE
                            Toast.makeText(
                                context,
                                "Posted", Toast.LENGTH_SHORT
                            ).show()
                        }
                        //sending notification when post is uploaded
                        sendNotifications(PushNotification(getNotificationBody(), Constants.TOPIC))
                        break
                    }
                    delay(500)
                }
            }
        }
    }

}