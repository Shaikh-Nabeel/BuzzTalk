package com.nabeel130.buzztalk.daos


import com.google.android.gms.tasks.Task
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.QuerySnapshot
import com.google.firebase.ktx.Firebase
import com.nabeel130.buzztalk.models.Post
import com.nabeel130.buzztalk.models.User
import com.nabeel130.buzztalk.models.UserPost
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class PostDao {

    val db = FirebaseFirestore.getInstance()
    val postCollection = db.collection("posts")
    private val auth = Firebase.auth
    private val userDao = UserDao()
    private val userPostDao = UserPostDao()

    companion object {
        lateinit var lastPost: Post
    }


    fun addPost(text: String, uuid: String?){
        GlobalScope.launch(Dispatchers.IO) {
            val createdAt = System.currentTimeMillis()
            lateinit var createdBy: User
            userDao.getUserById(auth.currentUser!!.uid).addOnCompleteListener{ user ->
                if (user.isSuccessful) {
                    createdBy = user.result.toObject(User::class.java)!!
                    val post = Post(text,createdBy,createdAt,uuid)
                    lastPost = post
                    postCollection.document().set(post)

                    //adding current post id in "UserPost" collection
                    userPostDao.getUserPost().addOnCompleteListener { userSnapshot ->
                        if(userSnapshot.isSuccessful){
                            val userPost = if(userSnapshot.result.exists()){
                                userSnapshot.result.toObject(UserPost::class.java)!!
                            }else{
                                UserPost()
                            }
                            getLastPostId().addOnCompleteListener {
                                if(it.isSuccessful){
                                    val documents = it.result.documents
                                    if(documents.size > 0){
                                        userPost.listOfPosts.add(documents[0].id)
                                        userPostDao.addUserPost(userPost)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }



    private fun getLastPostId(): Task<QuerySnapshot> {
        return postCollection.whereEqualTo("createdAt", lastPost.createdAt)
            .whereEqualTo("postText", lastPost.postText)
            .get()
    }

    fun deletePost(uid: String): Task<Void>{
        GlobalScope.launch(Dispatchers.IO){
            userPostDao.getUserPost().addOnCompleteListener {
                if(it.isSuccessful){
                    val userPost = it.result.toObject(UserPost::class.java)!!
                    userPost.listOfPosts.remove(uid)
                    userPostDao.addUserPost(userPost)
                }
            }
        }
        return postCollection.document(uid).delete()
    }

    fun getPostById(uid: String): Task<DocumentSnapshot> {
        return postCollection.document(uid).get()
    }

    fun likedPost(postId: String): Task<DocumentSnapshot> {
        val uid = auth.currentUser?.uid!!

        return getPostById(postId).addOnSuccessListener {
            val post = it.toObject(Post::class.java)!!
            val isLiked = post.likedBy.contains(uid)

            if (isLiked)
                post.likedBy.remove(uid)
            else
                post.likedBy.add(uid)
            postCollection.document(postId).set(post)
        }.addOnFailureListener {
            it.printStackTrace()
        }
    }
}