package com.nabeel130.buzztalk.models

data class Post(
    val postText: String = "",
    val createdBy : User = User(),
    val createdAt : Long = 0L,
    val likedBy: ArrayList<String> = ArrayList()
)
