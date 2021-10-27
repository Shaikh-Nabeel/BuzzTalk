package com.nabeel130.buzztalk

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.firebase.ui.firestore.FirestoreRecyclerAdapter
import com.firebase.ui.firestore.FirestoreRecyclerOptions
import com.nabeel130.buzztalk.models.Post
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.*

class PostAdapter(options: FirestoreRecyclerOptions<Post>):
    FirestoreRecyclerAdapter<Post, PostAdapter.PostViewHolder>(options) {

    class PostViewHolder(view: View): RecyclerView.ViewHolder(view) {
        val postText: TextView = view.findViewById(R.id.postContent)
        val userName: TextView = view.findViewById(R.id.userName)
        val createdAt: TextView = view.findViewById(R.id.createdAt)
        val likeCount: TextView = view.findViewById(R.id.likeCount)
        val userImage: ImageView = view.findViewById(R.id.profilePic)
        val likeBtn: ImageView = view.findViewById(R.id.likeBtn)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PostViewHolder {
        return PostViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.recycler_view_item,parent,false))
    }

    @SuppressLint("SimpleDateFormat")
    override fun onBindViewHolder(holder: PostViewHolder, position: Int, model: Post) {
        holder.postText.text = model.postText
        holder.userName.text = model.createdBy.userName
        holder.likeCount.text = model.likedBy.size.toString()

        val dateFormat: DateFormat = SimpleDateFormat("dd-MM-yyyy h:mm a")
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = model.createdAt
        holder.createdAt.text = dateFormat.format(calendar.time)
        Glide.with(holder.userImage.context).load(model.createdBy.imageUrl).circleCrop().into(holder.userImage)
    }
}