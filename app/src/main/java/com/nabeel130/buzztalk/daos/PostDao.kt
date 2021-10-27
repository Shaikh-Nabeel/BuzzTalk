package com.nabeel130.buzztalk.daos

import android.util.Log
import android.widget.Toast
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.ktx.Firebase
import com.nabeel130.buzztalk.CreatePostActivity
import com.nabeel130.buzztalk.models.Post
import com.nabeel130.buzztalk.models.User
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class PostDao {

    val db = FirebaseFirestore.getInstance()
    val postCollection = db.collection("posts")
    private val auth = Firebase.auth
    private val userDao = UserDao()

    fun addPost(text: String){
        GlobalScope.launch(Dispatchers.IO) {
            val createdAt = System.currentTimeMillis()
            lateinit var createdBy: User
            userDao.getUserById(auth.uid!!).addOnCompleteListener(CreatePostActivity.instance
            ) {
                if (it.isSuccessful) {
                    createdBy = it.result.toObject(User::class.java)!!
                    val post = Post(text,createdBy,createdAt)
                    postCollection.document().set(post)
                    Log.d("TestCode","Foundd user in db ${createdBy.userName}")
                }else{
                    Log.d("TestCode", "cannot find user in db")
                }
            }

        }
    }
}