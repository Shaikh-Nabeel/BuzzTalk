package com.nabeel130.buzztalk.daos

import android.util.Log
import android.widget.Toast
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.DocumentSnapshot
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
            userDao.getUserById(auth.currentUser!!.uid).addOnCompleteListener(CreatePostActivity.instance
            ) {
                if (it.isSuccessful) {
                    createdBy = it.result.toObject(User::class.java)!!
                    val post = Post(text,createdBy,createdAt)
                    postCollection.document().set(post)
                }else{
                    Log.d("PostReport", "could not find user")
                }
            }

        }
    }

    fun getPostById(uid: String): Task<DocumentSnapshot> {
        return postCollection.document(uid).get()
    }

    fun likedPost(postId: String) {
        GlobalScope.launch(Dispatchers.IO){
            val uid = auth.currentUser?.uid!!
            getPostById(postId).addOnCompleteListener {
                if(it.isSuccessful){
                    val post = it.result.toObject(Post::class.java)!!
                    val isLiked = post.likedBy.contains(uid)

                    if(isLiked)
                        post.likedBy.remove(uid)
                    else
                        post.likedBy.add(uid)

                    postCollection.document(postId).set(post)
                }
            }
        }
    }
}