package com.nabeel130.buzztalk

import android.annotation.SuppressLint
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.PopupMenu
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

val user = Firebase.auth.currentUser?.uid
val auth = Firebase.auth
val uid = auth.currentUser!!.uid

class PostAdapter(options: FirestoreRecyclerOptions<Post>, private val listener: IPostAdapter):
    FirestoreRecyclerAdapter<Post, PostAdapter.PostViewHolder>(options) {

    class PostViewHolder(view: View): RecyclerView.ViewHolder(view) {
        val postText: TextView = view.findViewById(R.id.postContent)
        val userName: TextView = view.findViewById(R.id.userName)
        val createdAt: TextView = view.findViewById(R.id.createdAt)
        val likeCount: TextView = view.findViewById(R.id.likeCount)
        val userImage: ImageView = view.findViewById(R.id.profilePic)
        val likeBtn: ImageView = view.findViewById(R.id.likeBtn)
        val optionBtn: ImageView = view.findViewById(R.id.postOptionMenu)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PostViewHolder {
        val viewHolder =  PostViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.recycler_view_item,parent,false))
        viewHolder.likeBtn.setOnClickListener {
            listener.onPostLiked(snapshots.getSnapshot(viewHolder.adapterPosition).id)
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

        val likes: String = if(model.likedBy.size > 1){
            model.likedBy.size.toString()+" likes"
        }else{
            model.likedBy.size.toString()+" like"
        }
        holder.likeCount.text = likes

        val dateFormat: DateFormat = SimpleDateFormat("dd-MM-yyyy h:mm a")
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = model.createdAt
        holder.createdAt.text = dateFormat.format(calendar.time)
        Glide.with(holder.userImage.context).load(model.createdBy.imageUrl).circleCrop().into(holder.userImage)

        val isLiked = model.likedBy.contains(uid)

        if(isLiked){
            holder.likeBtn.setImageDrawable(ContextCompat.getDrawable(holder.likeBtn.context,R.drawable.ic_baseline_favorite_24))
        }else{
            holder.likeBtn.setImageDrawable(ContextCompat.getDrawable(holder.likeBtn.context,R.drawable.ic_baseline_favorite_border_24))
        }

        holder.optionBtn.setOnClickListener {
            val menu = PopupMenu(holder.optionBtn.context,holder.optionBtn,Gravity.CENTER)
            menu.menuInflater.inflate(R.menu.menu_post_options,menu.menu)
            menu.setOnMenuItemClickListener {
                val id = it.itemId
                if(id == R.id.deletePost){
                    listener.onDeletePostClicked(snapshots.getSnapshot(holder.adapterPosition).id)
                }else if(id == R.id.sharePost){
                    val textToSend = "[App: BuzzTalk]\nPost{ ${model.createdBy.userName} : '${model.postText}' }"
                    listener.onShareClicked(textToSend,model.createdBy.userName!!)
                }
                false
            }
            if(user != model.createdBy.uid){
                menu.menu.removeItem(R.id.deletePost)
            }
            menu.show()
        }
    }
}

interface IPostAdapter{
    fun onPostLiked(postId: String)
    fun onLikeCountClicked(postId: String)
    fun onDeletePostClicked(postId: String)
    fun onShareClicked(text: String, userName: String)
}