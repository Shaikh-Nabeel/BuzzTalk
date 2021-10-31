package com.nabeel130.buzztalk

import android.graphics.Color
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.nabeel130.buzztalk.daos.PostDao
import com.nabeel130.buzztalk.databinding.ActivityCreatePostBinding

class CreatePostActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCreatePostBinding

    companion object{
        val instance = CreatePostActivity()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCreatePostBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.customToolB2.title = getString(R.string.app_name)
        binding.customToolB2.setTitleTextColor(Color.WHITE)

        val postDao = PostDao()

        binding.postBtn.setOnClickListener {
            val text = binding.postText.text.toString()
            if(text.isNotBlank() && text.isNotEmpty()){
                postDao.addPost(text)
                finish()
            }
        }

    }
}