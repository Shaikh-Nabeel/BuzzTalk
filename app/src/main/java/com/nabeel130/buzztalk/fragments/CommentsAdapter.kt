package com.nabeel130.buzztalk.fragments

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.AsyncListDiffer
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.firebase.firestore.DocumentSnapshot
import com.nabeel130.buzztalk.R
import com.nabeel130.buzztalk.daos.UserDao
import com.nabeel130.buzztalk.models.Comments
import com.nabeel130.buzztalk.models.Post
import com.nabeel130.buzztalk.models.User
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.*

private val userDao = UserDao()
class CommentsAdapter() : RecyclerView.Adapter<CommentsAdapter.CommentViewHolder>() {

    class CommentViewHolder(item: View):RecyclerView.ViewHolder(item){
        val userImage: ImageView = item.findViewById(R.id.profilePicForComment)
        val userName: TextView = item.findViewById(R.id.userNameForComment)
        val dateOfComment: TextView = item.findViewById(R.id.dateOfComment)
        val comment: TextView = item.findViewById(R.id.comment)
    }

    private val differCallback = object : DiffUtil.ItemCallback<DocumentSnapshot>(){
        override fun areItemsTheSame(
            oldItem: DocumentSnapshot,
            newItem: DocumentSnapshot
        ): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(
            oldItem: DocumentSnapshot,
            newItem: DocumentSnapshot
        ): Boolean {
            return oldItem == newItem
        }

    }

    val differ = AsyncListDiffer(this,differCallback)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CommentViewHolder {
        return CommentViewHolder(
            LayoutInflater.from(parent.context)
                .inflate(
                    R.layout.comment_item_view,
                    parent,
                    false
                )
        )
    }

    @SuppressLint("SimpleDateFormat")
    override fun onBindViewHolder(holder: CommentViewHolder, position: Int) {
        val document = differ.currentList[position]
        val comments = document.toObject(Comments::class.java)!!
        holder.comment.text = comments.comment

        val dateFormat: DateFormat = SimpleDateFormat("MMM dd, yyyy h:mm a")
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = comments.date!!
        holder.dateOfComment.text = dateFormat.format(calendar.time)

        GlobalScope.launch(Dispatchers.IO){
            userDao.getUserById(comments.uid).addOnCompleteListener{
                if(!it.isSuccessful)return@addOnCompleteListener
                val currentUser = it.result.toObject(User::class.java)!!
                holder.userName.text = currentUser.userName
                Glide.with(holder.userImage.context).load(currentUser.imageUrl)
                    .circleCrop().placeholder(R.drawable.user).into(holder.userImage)
            }
        }
    }

    override fun getItemCount(): Int {
        return differ.currentList.size
    }
}