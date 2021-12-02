package com.nabeel130.buzztalk

import android.annotation.SuppressLint
import android.graphics.drawable.Drawable
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.RequestBuilder
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import com.firebase.ui.firestore.FirestoreRecyclerAdapter
import com.firebase.ui.firestore.FirestoreRecyclerOptions
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.FirebaseStorage
import com.nabeel130.buzztalk.models.Post
import com.nabeel130.buzztalk.utility.GlideApp
import com.nabeel130.buzztalk.utility.Helper
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.*

val user = Firebase.auth.currentUser!!.uid
val storageRef = FirebaseStorage.getInstance().reference

class PostAdapter(options: FirestoreRecyclerOptions<Post>, private val listener: IPostAdapter):
    FirestoreRecyclerAdapter<Post, PostAdapter.PostViewHolder>(options) {

    class PostViewHolder(view: View): RecyclerView.ViewHolder(view) {
        val postText: TextView = view.findViewById(R.id.postContent)
        val userName: TextView = view.findViewById(R.id.userName)
        val createdAt: TextView = view.findViewById(R.id.createdAt)
        val likeCount: TextView = view.findViewById(R.id.likeCount)
        val userImage: ImageView = view.findViewById(R.id.profilePic)
        val likeBtn: ToggleButton = view.findViewById(R.id.likeBtn)
        val progress: ProgressBar = view.findViewById(R.id.progressBarForImage)
        val optionBtn: ImageView = view.findViewById(R.id.postOptionMenu)
        val postImage: ImageView = view.findViewById(R.id.postImage)
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
        if (!model.postText.contentEquals("")) {
            holder.postText.text = model.postText
        } else {
            holder.postText.visibility = View.GONE
        }
        holder.userName.text = model.createdBy.userName

        //like button handling
        val likes: String = if (model.likedBy.size > 1) {
            model.likedBy.size.toString() + " likes"
        } else {
            model.likedBy.size.toString() + " like"
        }
        holder.likeCount.text = likes

        //date formatting from timeInMillis
        val dateFormat: DateFormat = SimpleDateFormat("MMM dd, yyyy h:mm a")
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = model.createdAt
        holder.createdAt.text = dateFormat.format(calendar.time)

        //loading user profile
        Glide.with(holder.userImage.context).load(model.createdBy.imageUrl).circleCrop()
            .into(holder.userImage)

        //loading post image if exist
        if (model.imageUuid != null) {
            holder.progress.visibility = View.VISIBLE
            holder.postImage.visibility = View.VISIBLE
            val ref = storageRef.child("images/${model.imageUuid}")
            GlideApp.with(holder.postImage.context)
                .load(ref)
                .listener(object: RequestListener<Drawable> {
                    override fun onLoadFailed(
                        e: GlideException?, model: Any?, target: Target<Drawable>?,
                        isFirstResource: Boolean): Boolean {
                        holder.progress.visibility = View.GONE
                        return false
                    }

                    override fun onResourceReady(
                        resource: Drawable?, model: Any?,
                        target: Target<Drawable>?, dataSource: DataSource?,
                        isFirstResource: Boolean
                    ): Boolean {
                        holder.progress.visibility = View.GONE
                        return false
                    }
                })
                .into(holder.postImage)
        } else {
            holder.postImage.visibility = View.GONE
        }

        val isLiked = model.likedBy.contains(user)
        holder.likeBtn.isChecked = isLiked
        holder.optionBtn.setOnClickListener {
            val menu = PopupMenu(holder.optionBtn.context, holder.optionBtn, Gravity.CENTER)
            menu.menuInflater.inflate(R.menu.menu_post_options, menu.menu)
            menu.setOnMenuItemClickListener {
                val id = it.itemId
                if (id == R.id.deletePost) {
                    listener.onDeletePostClicked(snapshots.getSnapshot(holder.adapterPosition).id,model.imageUuid)
                } else if (id == R.id.sharePost) {
                    val textToSend = "[App: BuzzTalk]\nPost{ ${model.createdBy.userName} : '${model.postText}' }"
                    listener.onShareClicked(textToSend, model.createdBy.userName!!)
                }
                false
            }
            if (user != model.createdBy.uid) {
                menu.menu.removeItem(R.id.deletePost)
            }
            menu.show()
        }
    }
}

interface IPostAdapter{
    fun onPostLiked(postId: String)
    fun onLikeCountClicked(postId: String)
    fun onDeletePostClicked(postId: String, uuid: String?)
    fun onShareClicked(text: String, userName: String)
}