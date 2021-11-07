package com.nabeel130.buzztalk.fragments

import android.annotation.SuppressLint
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import com.nabeel130.buzztalk.IPostAdapter
import com.nabeel130.buzztalk.R
import com.nabeel130.buzztalk.daos.PostDao
import com.nabeel130.buzztalk.models.Post
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList

private var postDao = PostDao()
val auth = Firebase.auth
val uid = auth.currentUser!!.uid

class ProfileAdapter(private val listOfPost: ArrayList<String>, private val listener: IPostAdapter) :
    RecyclerView.Adapter<ProfileAdapter.ProfileViewHolder>() {

    class ProfileViewHolder(view: View): RecyclerView.ViewHolder(view){
        val createdAt: TextView = view.findViewById(R.id.createdAt_Profile)
        val postText: TextView = view.findViewById(R.id.postText_Profile)
        val likeBtn: ImageView = view.findViewById(R.id.likeBtn_Profile)
        val likeCount: TextView = view.findViewById(R.id.likeCount_Profile)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProfileViewHolder {
        val viewHolder = ProfileViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.item_profile_adapter,parent,false))
        viewHolder.likeBtn.setOnClickListener {
            listener.onPostLiked(listOfPost[viewHolder.adapterPosition])
        }
        return viewHolder
    }

    @SuppressLint("SimpleDateFormat", "SetTextI18n")
    override fun onBindViewHolder(holder: ProfileViewHolder, position: Int) {
        GlobalScope.launch(Dispatchers.IO) {
            postDao.getPostById(listOfPost[position]).addOnCompleteListener {
                if(it.isSuccessful){
                    val model = it.result.toObject(Post::class.java)!!
                    val dateFormat: DateFormat = SimpleDateFormat("dd-MM-yyyy h:mm a")
                    val calendar = Calendar.getInstance()
                    calendar.timeInMillis = model.createdAt
                    holder.createdAt.text = dateFormat.format(calendar.time)
                    holder.postText.text = model.postText
                    holder.likeCount.text = "${model.likedBy.size} likes"

                    val isLiked = model.likedBy.contains(uid)
                    if(isLiked){
                        holder.likeBtn.setImageDrawable(ContextCompat.getDrawable(holder.likeBtn.context,R.drawable.ic_baseline_favorite_24))
                    }else{
                        holder.likeBtn.setImageDrawable(ContextCompat.getDrawable(holder.likeBtn.context,R.drawable.ic_baseline_favorite_border_24))
                    }
                }
            }
        }
    }

    override fun getItemCount(): Int {
        return listOfPost.size
    }
}
