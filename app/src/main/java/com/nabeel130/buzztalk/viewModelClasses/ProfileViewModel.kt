package com.nabeel130.buzztalk.viewModelClasses

import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.nabeel130.buzztalk.daos.PostDao
import com.nabeel130.buzztalk.fragments.ProfileFragment2
import com.nabeel130.buzztalk.models.Post
import com.nabeel130.buzztalk.utility.Constants
import kotlinx.coroutines.*
import kotlinx.coroutines.tasks.await

class ProfileViewModel: ViewModel(){

    var mListOfPost: MutableLiveData<Post>
    private var listOfId: ArrayList<String> = ProfileFragment2.list
    private var postDao: PostDao = PostDao()

    init {
        mListOfPost = loadPost()
    }

    private fun loadPost(): MutableLiveData<Post> {
        val listOfPost = MutableLiveData<Post>()
        GlobalScope.launch(Dispatchers.Main) {
            for (id in listOfId) {
                val post = postDao.getPostById(id).await().toObject(Post::class.java)!!
//                withContext(Dispatchers.Main) {
                    listOfPost.value = post
//                }
                Log.d(Constants.TAG, "id: ${post.createdBy.uid} = ${post.postText}")
                Log.d(Constants.TAG, id)
            }
        }
        return listOfPost
    }
}