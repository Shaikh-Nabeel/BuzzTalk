package com.nabeel130.buzztalk.viewModelClasses

import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.nabeel130.buzztalk.daos.PostDao
import com.nabeel130.buzztalk.models.Post
import com.nabeel130.buzztalk.utility.Constants
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class ProfileViewModel private constructor(list2: ArrayList<String>):
    ViewModel() {

    var mListOfPost: MutableLiveData<Post>
    var listOfId: ArrayList<String> = list2
    private var postDao: PostDao = PostDao()

    init {
        mListOfPost = loadPost()
    }

    private fun loadPost(): MutableLiveData<Post> {
        val listOfPost = MutableLiveData<Post>()
        runBlocking {
            for (id in listOfId) {
                val post = postDao.getPostById(id).await().toObject(Post::class.java)!!
                listOfPost.value = post
                Log.d(Constants.TAG, "id: ${post.createdBy.uid} = ${post.postText}")
//                withContext(Dispatchers.Main) {
//                    profileAdapter.differ.submitList(listOfPost.toMutableList())
//                }
                Log.d(Constants.TAG, id)
            }
        }
        return listOfPost
    }
}