package com.nabeel130.buzztalk

import android.annotation.SuppressLint
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.firebase.ui.firestore.FirestoreRecyclerAdapter
import com.firebase.ui.firestore.FirestoreRecyclerOptions
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import com.nabeel130.buzztalk.models.Post
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.*

class PostAdapter(options: FirestoreRecyclerOptions<Post>,val listener: IPostAdapter):
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
        val viewHolder =  PostViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.recycler_view_item,parent,false))
        viewHolder.likeBtn.setOnClickListener {
            listener.onPostLiked(snapshots.getSnapshot(viewHolder.adapterPosition).id)
//            if(viewHolder.likeBtn.drawable.current == ContextCompat.getDrawable(viewHolder.likeBtn.context,R.drawable.ic_baseline_favorite_border_24)){
//                Log.d("PostReport","in if block")
//            //viewHolder.likeBtn.setImageDrawable(ContextCompat.getDrawable(viewHolder.likeBtn.context,R.drawable.ic_baseline_favorite_24))
//            }
//            else{
//                Log.d("PostReport","in else block")
//                //viewHolder.likeBtn.setImageDrawable(ContextCompat.getDrawable(viewHolder.likeBtn.context,R.drawable.ic_baseline_favorite_border_24))
//            }
        }
        viewHolder.likeCount.setOnClickListener {
            listener.onLikeCountClicked(snapshots.getSnapshot(viewHolder.adapterPosition).id)
        }
        return viewHolder
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

        val auth = Firebase.auth
        val uid = auth.currentUser!!.uid
        val isLiked = model.likedBy.contains(uid)

        if(isLiked){
            holder.likeBtn.setImageDrawable(ContextCompat.getDrawable(holder.likeBtn.context,R.drawable.ic_baseline_favorite_24))
        }else{
            holder.likeBtn.setImageDrawable(ContextCompat.getDrawable(holder.likeBtn.context,R.drawable.ic_baseline_favorite_border_24))
        }
    }
}

interface IPostAdapter{
    fun onPostLiked(postId: String)
    fun onLikeCountClicked(postId: String)
}