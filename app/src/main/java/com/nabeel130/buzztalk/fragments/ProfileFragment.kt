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
import com.bumptech.glide.Glide
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import com.nabeel130.buzztalk.MainActivity
import com.nabeel130.buzztalk.R
import com.nabeel130.buzztalk.daos.PostDao
import com.nabeel130.buzztalk.daos.UserPostDao
import com.nabeel130.buzztalk.databinding.FragmentProfileBinding
import com.nabeel130.buzztalk.models.Post
import com.nabeel130.buzztalk.models.UserPost
import com.nabeel130.buzztalk.utility.Helper
import com.nabeel130.buzztalk.utility.Helper.Companion.TAG
import kotlinx.coroutines.*
import kotlinx.coroutines.tasks.await

class ProfileFragment : Fragment(), IProfileAdapter {

    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!
//    private lateinit var userDao: UserDao
    private lateinit var profileAdapter: ProfileAdapter
    private lateinit var postDao: PostDao
    private var listOfPost: Deferred<ArrayList<Post>>? = null
    private lateinit var list: ArrayList<String>
    private val user = Firebase.auth.currentUser!!

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

//        userDao = UserDao()
        binding.userNameForFragments.text = user.displayName
        Glide.with(binding.profilePicForFragments.context).load(user.photoUrl)
            .circleCrop().into(binding.profilePicForFragments)

        //fetching all post id of post, posted by current user
        Log.d(TAG, "Loading post list..")
        val userPostDao = UserPostDao()
        GlobalScope.launch(Dispatchers.IO){
            userPostDao.getUserPost().addOnCompleteListener {
                if(it.isSuccessful){
                    if(!it.result.exists()) return@addOnCompleteListener
                    val userPost = it.result.toObject(UserPost::class.java)!!
                    binding.numberOfPost.text = "${userPost.listOfPosts.size}\nPosts"

                    //reversing list to sort post according to date (descending order)
                    list = if(userPost.listOfPosts.size <= 1){
                        userPost.listOfPosts
                    }else {
                        userPost.listOfPosts.reversed() as ArrayList
                    }

                    listOfPost = GlobalScope.async(Dispatchers.IO){
                        loadPost(list)
                    }

                    GlobalScope.launch(Dispatchers.Main) {
                        profileAdapter = ProfileAdapter(list,this@ProfileFragment)
                        binding.recyclerViewForProfile.adapter = profileAdapter
                        binding.recyclerViewForProfile.layoutManager = LinearLayoutManager(context)
                        profileAdapter.differ.submitList(listOfPost?.await())
                        Log.d(TAG, "Loading successful for post list")
                    }
                }
            }
        }
        postDao = PostDao()
    }

    private fun loadPost(list: ArrayList<String>): ArrayList<Post>{
        val listOfPost = ArrayList<Post>()
        for(id in list){
            runBlocking {
                val post = postDao.getPostById(id).await().toObject(Post::class.java)!!
                listOfPost.add(post)
                Log.d(TAG, post.postText)
//                postDao.getPostById(id).addOnCompleteListener {
//                    if(it.isSuccessful){
//                        Log.d(TAG, "snapshot id: "+ it.result.id)
//                        val post = it.result.toObject(Post::class.java)!!
//                        listOfPost.add(post)
//                        Log.d(TAG, post.postText)
//                    }
//                }
                withContext(Dispatchers.Main){
                    profileAdapter.differ.submitList(listOfPost)
                }
            }
            Log.d(TAG, id)
        }
        return listOfPost
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
        MainActivity.getInstance().visibleComponentOfMainActivity()
    }

    override fun onPostLiked(post: Post,postId: String) {

            val postCopy = post.copy(likedBy = post.likedBy.toMutableList() as ArrayList)
            val newList = runBlocking {
                listOfPost?.await()!!.toMutableList() as ArrayList
            }
            val likeList = postCopy.likedBy
            if(likeList.contains(user.uid)) likeList.remove(user.uid) else likeList.add(user.uid)

            postDao.likedPost(postCopy, postId).addOnCompleteListener{
                if(it.isSuccessful)
                    Log.d(TAG, "Post like updated, like: "+post.likedBy.size)
            }
            Log.d(TAG, "id : ${list.indexOf(postId)} , ${post.likedBy.size}, ${postCopy.likedBy.size}")

            newList[list.indexOf(postId)] = postCopy
            profileAdapter.differ.submitList(newList)

    }

    @SuppressLint("SetTextI18n")
    override fun onDeletePostClicked(postId: String, position: Int) {
        val dialog = Helper.buildDialogBox(requireContext(),getString(R.string.areYouSure),getString(R.string.dialog_text_1))
        val confirmBtn: Button = dialog.findViewById(R.id.confirm_button)
        val denyBtn: Button = dialog.findViewById(R.id.deny_button)

        confirmBtn.setOnClickListener {
            dialog.dismiss()
            postDao.deletePost(postId).addOnCompleteListener {
                if (it.isSuccessful) {
                    profileAdapter.notifyItemRemoved(position)
                    profileAdapter.notifyItemRangeChanged(position, profileAdapter.itemCount-position)
                    binding.numberOfPost.text = "${profileAdapter.itemCount}\nPosts"
                    Toast.makeText(context, "Deleted", Toast.LENGTH_SHORT).show()
                }
                else Toast.makeText(context, "Couldn't delete", Toast.LENGTH_SHORT).show()
            }
        }

        denyBtn.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }

    override fun onShareClicked(text: String, userName: String) {
        val intent = Intent(Intent.ACTION_SEND).setType("text/plain")
        intent.putExtra(Intent.EXTRA_SUBJECT,"BuzzTalk")
        intent.putExtra(Intent.EXTRA_TEXT, text)
        startActivity(Intent.createChooser(intent,"Post from $userName"))
    }

}