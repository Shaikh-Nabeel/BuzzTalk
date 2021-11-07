package com.nabeel130.buzztalk.fragments

import android.annotation.SuppressLint
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.GridLayoutManager
import com.bumptech.glide.Glide
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import com.nabeel130.buzztalk.IPostAdapter
import com.nabeel130.buzztalk.MainActivity
import com.nabeel130.buzztalk.daos.PostDao
import com.nabeel130.buzztalk.daos.UserDao
import com.nabeel130.buzztalk.daos.UserPostDao
import com.nabeel130.buzztalk.databinding.FragmentProfileBinding
import com.nabeel130.buzztalk.models.User
import com.nabeel130.buzztalk.models.UserPost
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class ProfileFragment : Fragment(), IPostAdapter {

    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!
    private lateinit var userDao: UserDao
    private val TAG = "BuzzReport"
    private lateinit var profileAdapter: ProfileAdapter
    private lateinit var postDao: PostDao

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    @SuppressLint("SetTextI18n")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.progressBarForProfile.visibility = View.VISIBLE
        userDao = UserDao()
        GlobalScope.launch(Dispatchers.IO){
            val userId = Firebase.auth.currentUser?.uid!!
            userDao.getUserById(userId).addOnCompleteListener {
                if (it.isSuccessful) {
                    val user = it.result.toObject(User::class.java)
                    if (user != null) {
                        binding.userNameForFragments.text = user.userName
                        Glide.with(binding.profilePicForFragments.context).load(user.imageUrl)
                            .circleCrop().into(binding.profilePicForFragments)
                        binding.progressBarForProfile.visibility = View.GONE
                    }
                }
            }
        }

        val userPostDao = UserPostDao()
        GlobalScope.launch(Dispatchers.IO){
            userPostDao.getUserPost().addOnCompleteListener {
                if(it.isSuccessful){
                    val userPost = it.result.toObject(UserPost::class.java)!!
                    binding.numberOfPost.text = "${userPost.listOfPosts.size}\nPosts"
                    profileAdapter = ProfileAdapter(userPost.listOfPosts, this@ProfileFragment)
                    binding.recyclerViewForProfile.adapter = profileAdapter
                    binding.recyclerViewForProfile.layoutManager = GridLayoutManager(context,2)
                    binding.recyclerViewForProfile.setHasFixedSize(true)
                }
            }
        }
        postDao = PostDao()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
        MainActivity.getInstance().visibleComponentOfMainActivity()
    }

    override fun onPostLiked(postId: String) {
        postDao.likedPost(postId)
    }

    override fun onLikeCountClicked(postId: String) {
        TODO("Not yet implemented")
    }

    override fun onDeletePostClicked(postId: String) {
        TODO("Not yet implemented")
    }

    override fun onShareClicked(text: String, userName: String) {
        TODO("Not yet implemented")
    }

}