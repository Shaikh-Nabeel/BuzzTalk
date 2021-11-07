package com.nabeel130.buzztalk

import android.graphics.Color
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import com.nabeel130.buzztalk.daos.PostDao
import com.nabeel130.buzztalk.databinding.ActivityCreatePostBinding

class CreatePostActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCreatePostBinding

    override fun onBackPressed() {
        super.onBackPressed()
        overridePendingTransition(android.R.anim.fade_in,android.R.anim.fade_out)
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
            if(text.length > 300){
                Toast.makeText(this,"Length should be less than 300 characters",Toast.LENGTH_SHORT).show()
            }else if(text.isNotBlank() && text.isNotEmpty()){
                postDao.addPost(text)
                finish()
                overridePendingTransition(android.R.anim.slide_out_right,android.R.anim.fade_out)
            }
        }

    }
}