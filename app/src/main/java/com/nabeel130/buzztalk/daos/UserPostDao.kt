package com.nabeel130.buzztalk.daos

import com.google.android.gms.tasks.Task
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.ktx.Firebase
import com.nabeel130.buzztalk.models.UserPost
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class UserPostDao {

    private val db = FirebaseFirestore.getInstance()
    val userPostCollection = db.collection("UserPost")
    private val userId = Firebase.auth.currentUser!!.uid

    fun addUserPost(userPost: UserPost){
        GlobalScope.launch(Dispatchers.IO){
            userPostCollection.document(userId).set(userPost)
        }
    }

    fun getUserPost(): Task<DocumentSnapshot> {
        return userPostCollection.document(userId).get()
    }

    fun getUserPostById(uid: String): Task<DocumentSnapshot> {
        return userPostCollection.document(uid).get()
    }
}