package com.nabeel130.buzztalk.fragments

import android.annotation.SuppressLint
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.PopupMenu
import android.widget.TextView
import android.widget.ToggleButton
import androidx.recyclerview.widget.AsyncListDiffer
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import com.nabeel130.buzztalk.R
import com.nabeel130.buzztalk.daos.PostDao
import com.nabeel130.buzztalk.models.Post
import com.nabeel130.buzztalk.storageRef
import com.nabeel130.buzztalk.utility.GlideApp
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.*

val auth = Firebase.auth
val uid = auth.currentUser!!.uid

class ProfileAdapter(private val listOfId: ArrayList<String>,
                     private val listener: IProfileAdapter) :
    RecyclerView.Adapter<ProfileAdapter.ProfileViewHolder>() {

    class ProfileViewHolder(view: View): RecyclerView.ViewHolder(view){
        val createdAt: TextView = view.findViewById(R.id.createdAt_Profile)
        val postText: TextView = view.findViewById(R.id.postText_Profile)
        val likeBtn: ToggleButton = view.findViewById(R.id.likeBtn_Profile)
        val likeCount: TextView = view.findViewById(R.id.likeCount_Profile)
        val optionBtn: ImageView = view.findViewById(R.id.postOptionMenuFragment)
        val postImage: ImageView = view.findViewById(R.id.postImage_Profile)
    }

    private val differCallback = object : DiffUtil.ItemCallback<Post>(){
        override fun areItemsTheSame(oldItem: Post, newItem: Post): Boolean {
            return oldItem.createdAt == newItem.createdAt && oldItem.postText == newItem.postText
        }

        override fun areContentsTheSame(oldItem: Post, newItem: Post): Boolean {
            return oldItem.likedBy.size == newItem.likedBy.size
        }
    }

    val differ = AsyncListDiffer(this,differCallback)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProfileViewHolder {
        val viewHolder = ProfileViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.item_profile_adapter,parent,false))
        viewHolder.likeBtn.setOnClickListener {
            listener.onPostLiked(listOfId[viewHolder.adapterPosition],viewHolder.adapterPosition)
        }
        return viewHolder
    }

    @SuppressLint("SimpleDateFormat", "SetTextI18n")
    override fun onBindViewHolder(holder: ProfileViewHolder, position: Int) {
//        GlobalScope.launch(Dispatchers.IO) {
//            postDao.getPostById(listOfPost[position]).addOnCompleteListener {
//                if(it.isSuccessful){
                    val model = differ.currentList[position]

                    val dateFormat: DateFormat = SimpleDateFormat("MMM dd, yyyy h:mm a")
                    val calendar = Calendar.getInstance()
                    calendar.timeInMillis = model.createdAt
                    holder.createdAt.text = dateFormat.format(calendar.time)

                    if (!model.postText.contentEquals("")) {
                        holder.postText.text = model.postText
                    } else {
                        holder.postText.visibility = View.GONE
                    }

                    val likes: String = if(model.likedBy.size > 1){
                        model.likedBy.size.toString()+" likes"
                    }else{
                        model.likedBy.size.toString()+" like"
                    }
                    holder.likeCount.text = likes
                    holder.likeBtn.isChecked = model.likedBy.contains(uid)

                    if (model.imageUuid != null) {
                        holder.postImage.visibility = View.VISIBLE
                        val ref = storageRef.child("images/${model.imageUuid}")
                        GlideApp.with(holder.postImage.context)
                            .load(ref)
                            .into(holder.postImage)
                    } else {
                        holder.postImage.visibility = View.GONE
                    }

                    holder.optionBtn.setOnClickListener {
                        val menu = PopupMenu(holder.optionBtn.context,holder.optionBtn, Gravity.CENTER)
                        menu.menuInflater.inflate(R.menu.menu_post_options,menu.menu)
                        menu.setOnMenuItemClickListener { item ->
                            when (item.itemId) {
                                R.id.deletePost -> {
                                    listener.onDeletePostClicked(listOfId[position],holder.adapterPosition)
//                                    differ.currentList.removeAt(position)
                                    differ.currentList.removeAt(position)
                                    listOfId.removeAt(position)
                                    true
                                }
                                R.id.sharePost -> {
                                    val textToSend = "[App: BuzzTalk]\nPost{ ${model.createdBy.userName} : '${model.postText}' }"
                                    listener.onShareClicked(textToSend,model.createdBy.userName!!)
                                    true
                                }
                                else -> false
                            }
                        }
                        menu.show()
                    }

//                }
//            }
//        }

    }

    override fun getItemCount(): Int {
        return differ.currentList.size
    }
}

interface IProfileAdapter{
    fun onPostLiked(postId: String,position: Int)
    fun onDeletePostClicked(postId: String,position: Int)
    fun onShareClicked(text: String,userName: String)
}