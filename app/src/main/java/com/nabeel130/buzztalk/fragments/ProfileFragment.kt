package com.nabeel130.buzztalk.fragments

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import com.nabeel130.buzztalk.MainActivity
import com.nabeel130.buzztalk.R
import com.nabeel130.buzztalk.daos.PostDao
import com.nabeel130.buzztalk.daos.UserDao
import com.nabeel130.buzztalk.daos.UserPostDao
import com.nabeel130.buzztalk.databinding.FragmentProfileBinding
import com.nabeel130.buzztalk.models.User
import com.nabeel130.buzztalk.models.UserPost
import com.nabeel130.buzztalk.utility.Helper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class ProfileFragment : Fragment(), IProfileAdapter {

    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!
    private lateinit var userDao: UserDao
    private lateinit var profileAdapter: ProfileAdapter
    private lateinit var postDao: PostDao

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
        //setting up profile pic and username
//        binding.progressBarForProfile.visibility = View.VISIBLE
        Log.d(Helper.TAG, "Loading user details")
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
//                        binding.progressBarForProfile.visibility = View.GONE
                        Log.d(Helper.TAG, "Loading successful")
                    }
                }
            }
        }


//        profileViewModel = ViewModelProvider(this).get(ProfileViewModel::class.java)
//        profileViewModel.currentUser().let {
//            binding.userNameForFragments.text = it.userName
//            Glide.with(binding.profilePicForFragments.context).load(it.imageUrl)
//                .circleCrop().into(binding.profilePicForFragments)
//        }

        //fetching all post id of post, posted by current user
        Log.d(Helper.TAG, "Loading post list..")
        val userPostDao = UserPostDao()
        GlobalScope.launch(Dispatchers.IO){
            userPostDao.getUserPost().addOnCompleteListener {
                if(it.isSuccessful){
                    val userPost = it.result.toObject(UserPost::class.java)!!
                    binding.numberOfPost.text = "${userPost.listOfPosts.size}\nPosts"
                    //reversing list to sort post according to date (descending order)
                    val list = if(userPost.listOfPosts.size <= 1){
                        userPost.listOfPosts
                    }else {
                        userPost.listOfPosts.reversed() as ArrayList
                    }
                    profileAdapter = ProfileAdapter(list, this@ProfileFragment)
                    binding.recyclerViewForProfile.adapter = profileAdapter
                    binding.recyclerViewForProfile.layoutManager = LinearLayoutManager(context)
                    Log.d(Helper.TAG, "Loading successful for post list")

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

    override fun onPostLiked(postId: String,position: Int) {
        GlobalScope.launch(Dispatchers.IO) {
            postDao.likedPost(postId)
                .addOnCompleteListener {
                if (it.isSuccessful) {
                    profileAdapter.notifyItemChanged(position)
                }
            }
        }
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
                else
                    Toast.makeText(context, "Couldn't delete", Toast.LENGTH_SHORT).show()
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