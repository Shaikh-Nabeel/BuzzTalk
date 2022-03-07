package com.nabeel130.buzztalk.fragments

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import com.nabeel130.buzztalk.R
import com.nabeel130.buzztalk.activity.MainActivity
import com.nabeel130.buzztalk.adapter.LikeAdapter
import com.nabeel130.buzztalk.daos.PostDao
import com.nabeel130.buzztalk.databinding.FragmentHomeBinding
import com.nabeel130.buzztalk.databinding.FragmentLikeBinding
import com.nabeel130.buzztalk.models.Post

class LikeFragment : Fragment() {

    private var _binding: FragmentLikeBinding? = null
    private val binding get() = _binding!!
    private val postDao = PostDao()
    private lateinit var likeAdapter: LikeAdapter
    private var postId = ""

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        try {
            postId = arguments?.getString("postId")!!
        } catch (e: Exception) {
            e.printStackTrace()
        }
        _binding = FragmentLikeBinding.inflate(inflater,container,false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        var listOfLikedUser: ArrayList<String>

        binding.closeBtnForLikes.setOnClickListener {
            (activity as MainActivity).supportFragmentManager.popBackStack()
        }

        binding.progressBarForLikes.visibility = View.VISIBLE

        postDao.getPostById(postId).addOnCompleteListener {
            if (it.isSuccessful) {
                val post = it.result.toObject(Post::class.java)!!
                listOfLikedUser = post.likedBy
                likeAdapter = LikeAdapter(listOfLikedUser)
                binding.recyclerViewForLike.adapter = likeAdapter
                binding.progressBarForLikes.visibility = View.GONE
                binding.recyclerViewForLike.layoutManager =
                    LinearLayoutManager(binding.relativeLayoutForLikes.context)
            }
        }
    }

}