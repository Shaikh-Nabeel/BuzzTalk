package com.nabeel130.buzztalk.fragments

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.SimpleItemAnimator
import com.bumptech.glide.Glide
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import com.nabeel130.buzztalk.activity.MainActivity
import com.nabeel130.buzztalk.R
import com.nabeel130.buzztalk.adapter.IProfileAdapter
import com.nabeel130.buzztalk.adapter.ProfileAdapter
import com.nabeel130.buzztalk.daos.PostDao
import com.nabeel130.buzztalk.daos.UserPostDao
import com.nabeel130.buzztalk.databinding.FragmentProfileBinding
import com.nabeel130.buzztalk.models.Post
import com.nabeel130.buzztalk.models.UserPost
import com.nabeel130.buzztalk.utility.Constants
import com.nabeel130.buzztalk.utility.Constants.Companion.TAG
import com.nabeel130.buzztalk.utility.PreferenceManager
import kotlinx.coroutines.*
import kotlinx.coroutines.tasks.await
import java.lang.NullPointerException

class ProfileFragment : Fragment(), IProfileAdapter {

    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!

    private lateinit var profileAdapter: ProfileAdapter
    private lateinit var postDao: PostDao
    private var listOfPost: Deferred<ArrayList<Post>>? = null
    private lateinit var list: ArrayList<String>
    private val user = Firebase.auth.currentUser!!
    private var listOfIdJob: Job? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    @SuppressLint("SetTextI18n")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        try {
            binding.userNameForFragments.text = user.displayName
            Glide.with(binding.profilePicForFragments.context)
                .load(PreferenceManager.getString(Constants.IMAGE_URL))
                .circleCrop().into(binding.profilePicForFragments)

            //fetching all post id of post, posted by current user
            val userPostDao = UserPostDao()
            binding.progressBarForProfile.visibility = View.VISIBLE
            GlobalScope.launch(Dispatchers.IO) {
                userPostDao.getUserPost().addOnCompleteListener {
                    if (it.isSuccessful && _binding != null) {
                        if (!it.result.exists()) return@addOnCompleteListener
                        val userPost = it.result.toObject(UserPost::class.java)!!
                        binding.numberOfPost.text = "${userPost.listOfPosts.size}\nPosts"

                        //reversing list to sort post according to date (descending order)
                        list = if (userPost.listOfPosts.size <= 1) {
                            userPost.listOfPosts
                        } else {
                            userPost.listOfPosts.reversed() as ArrayList
                        }

                        listOfPost = GlobalScope.async(Dispatchers.IO) {
                            val pList = loadPost(list)
                            withContext(Dispatchers.Main){
                                binding.progressBarForProfile.visibility = View.GONE
                                profileAdapter.differ.submitList(pList)
                            }
                            return@async pList
                        }

                        GlobalScope.launch(Dispatchers.Main) {
                            profileAdapter = ProfileAdapter(list, this@ProfileFragment)
                            binding.recyclerViewForProfile.adapter = profileAdapter
                            binding.recyclerViewForProfile.layoutManager = LinearLayoutManager(context)
                            (binding.recyclerViewForProfile.itemAnimator as SimpleItemAnimator)
                                .supportsChangeAnimations = false
//                            profileAdapter.differ.submitList(listOfPost?.await())
                        }

                    }
                }
            }
            postDao = PostDao()
        }catch (e: Exception){
            e.printStackTrace()
        }
    }

    private fun loadPost(list: ArrayList<String>): ArrayList<Post> {
        val listOfPost = ArrayList<Post>()
        runBlocking {
            for (id in list) {
                val post = postDao.getPostById(id).await().toObject(Post::class.java)!!
                listOfPost.add(post)
                Log.d(TAG, "id: ${post.createdBy.uid} = ${post.postText}")
                withContext(Dispatchers.Main) {
                    profileAdapter.differ.submitList(listOfPost.toMutableList())
                }

                Log.d(TAG, id)
            }
        }
        return listOfPost
    }

//    private fun loadPost(list: ArrayList<String>): ArrayList<Post> {
//        val pListOfPost = ArrayList<Post>()
//        runBlocking {
//            for (id in list) {
//                postDao.getPostById(id).addOnCompleteListener {
//                    if (it.isSuccessful) {
//                        val post = it.result.toObject(Post::class.java)!!
//                        pListOfPost.add(post)
//                    }
//                }
//                Log.d(TAG, "loading : $id")
//            }
//        }
//        Log.d(TAG, "loading succesfull......")
//        pListOfPost.sortBy { it.createdAt }
//        return pListOfPost
//    }

    override fun onDestroyView() {
        super.onDestroyView()
        try{
            listOfIdJob?.cancel()
            listOfPost?.cancel()
        }catch (e: Exception){
            e.printStackTrace()
        }
        _binding = null
    }

    override fun onPostLiked(post: Post, postId: String) {

        if(binding.progressBarForProfile.visibility == View.VISIBLE) return

        val postCopy = post.copy(likedBy = post.likedBy.toMutableList() as ArrayList)
        val newList = runBlocking {
            listOfPost?.await()!!.toMutableList() as ArrayList
        }
        val likeList = postCopy.likedBy
        if (likeList.contains(user.uid)) likeList.remove(user.uid) else likeList.add(user.uid)

        postDao.likedPost(postCopy, postId).addOnCompleteListener {
            if (it.isSuccessful)
                Log.d(TAG, "Post like updated, like: " + post.likedBy.size)
        }
        Log.d(TAG, "id : ${list.indexOf(postId)} , ${post.likedBy.size}, ${postCopy.likedBy.size}")

        newList[list.indexOf(postId)] = postCopy
        profileAdapter.differ.submitList(newList)

        runBlocking {
            listOfPost?.await()?.set(list.indexOf(postId), postCopy)
        }

    }

    override fun onCommentButtonClicked(postId: String, createdBy: String) {

        if(binding.progressBarForProfile.visibility == View.VISIBLE) return

        val bundle = Bundle()
        bundle.putString("postId", postId)
        bundle.putString("createdBy", createdBy)

        val commentsFragment = CommentsFragment()
        commentsFragment.arguments = bundle

        Constants.isOpenedFromProfile = true
        parentFragmentManager.beginTransaction().apply {
            add(R.id.frameLayoutForFragments, commentsFragment)
            addToBackStack(null)
            commit()
        }
    }

    @SuppressLint("SetTextI18n")
    override fun onDeletePostClicked(postId: String, position: Int) {

        if(binding.progressBarForProfile.visibility == View.VISIBLE) return

        val dialog = Constants.buildDialogBox(
            requireContext(),
            getString(R.string.areYouSure),
            getString(R.string.dialog_text_1)
        )
        val confirmBtn: Button = dialog.findViewById(R.id.confirm_button)
        val denyBtn: Button = dialog.findViewById(R.id.deny_button)

        confirmBtn.setOnClickListener {
            dialog.dismiss()
            postDao.deletePost(postId).addOnCompleteListener {
                if (it.isSuccessful) {
                    val updatedList = runBlocking {
                        listOfPost?.await()?.toMutableList()!!
                    }
                    val delPost = updatedList.removeAt(position)
                    Log.d(TAG, "text:  ${delPost.postText}, size: ${updatedList.size} $updatedList")
                    Log.d(TAG, list.toString())
                    list.removeAt(position)
                    GlobalScope.launch(Dispatchers.Main) {
                        profileAdapter.differ.submitList(updatedList)
                    }
                    binding.numberOfPost.text = "${profileAdapter.itemCount}\nPosts"
                    GlobalScope.launch(Dispatchers.IO){
                        val itm = listOfPost?.await()?.removeAt(position)
                        if (itm != null) {
                            Log.d(TAG, "item deleted from main list: ${itm.postText}")
                        }
                    }


                    Log.d(TAG, list.toString())
                    Toast.makeText(context, "Deleted", Toast.LENGTH_SHORT).show()
                } else Toast.makeText(context, "Couldn't delete", Toast.LENGTH_SHORT).show()
            }
        }

        denyBtn.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }

    override fun onShareClicked(text: String, userName: String) {

        if(binding.progressBarForProfile.visibility == View.VISIBLE) return

        val intent = Intent(Intent.ACTION_SEND).setType("text/plain")
        intent.putExtra(Intent.EXTRA_SUBJECT, "BuzzTalk")
        intent.putExtra(Intent.EXTRA_TEXT, text)
        startActivity(Intent.createChooser(intent, "Post from $userName"))
    }

}