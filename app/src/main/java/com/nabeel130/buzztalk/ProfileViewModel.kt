package com.nabeel130.buzztalk

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.ktx.Firebase
import com.nabeel130.buzztalk.daos.PostDao
import com.nabeel130.buzztalk.daos.UserDao
import com.nabeel130.buzztalk.daos.UserPostDao
import com.nabeel130.buzztalk.models.Post
import com.nabeel130.buzztalk.models.User
import com.nabeel130.buzztalk.models.UserPost
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class ProfileViewModel(application: Application): AndroidViewModel(application) {

//    private val currentUserUid = Firebase.auth.currentUser?.uid!!
//
//    private val userPostDao: UserPostDao
//    private val userDao: UserDao
//    lateinit var currentUser: User
//
//    init {
//        userDao = UserDao()
//        userPostDao = UserPostDao()
//        GlobalScope.launch(Dispatchers.IO) {
//            currentUser = userDao.getUserById(currentUserUid).await().toObject(User::class.java)!!
//        }
//    }

}