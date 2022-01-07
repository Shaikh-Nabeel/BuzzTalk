package com.nabeel130.buzztalk.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.nabeel130.buzztalk.R
import com.nabeel130.buzztalk.daos.UserDao
import com.nabeel130.buzztalk.models.User

class LikeAdapter(private val likedBy: ArrayList<String>) :
    RecyclerView.Adapter<LikeAdapter.LikeViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LikeViewHolder {
        return LikeViewHolder(
            LayoutInflater.from(parent.context).inflate(R.layout.likes_view_item, parent, false)
        )
    }

    class LikeViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val userName: TextView = view.findViewById(R.id.userNameForLike)
        val userImage: ImageView = view.findViewById(R.id.profilePicForLike)
    }

    override fun onBindViewHolder(holder: LikeViewHolder, position: Int) {
        val userDao = UserDao()
        userDao.getUserById(likedBy[position]).addOnCompleteListener {
            if (it.isSuccessful) {
                val user = it.result.toObject(User::class.java)!!
                holder.userName.text = user.userName
                try {
                    Glide.with(holder.userImage.context).load(user.imageUrl).circleCrop()
                        .into(holder.userImage)
                } catch (e: Exception) {

                }
            }
        }
    }

    override fun getItemCount(): Int {
        return likedBy.size
    }

}