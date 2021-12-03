package com.nabeel130.buzztalk

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.FileProvider
import com.bumptech.glide.Glide
import com.google.firebase.storage.FirebaseStorage
import com.nabeel130.buzztalk.daos.PostDao
import com.nabeel130.buzztalk.databinding.ActivityCreatePostBinding
import com.nabeel130.buzztalk.utility.Helper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.io.File
import java.io.IOException
import java.util.*


class CreatePostActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCreatePostBinding
    private var mUri: Uri? = null
    private var isImageSelected = false
    private val postDao = PostDao()

    override fun onBackPressed() {
        super.onBackPressed()
        overridePendingTransition(android.R.anim.fade_in,android.R.anim.fade_out)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCreatePostBinding.inflate(layoutInflater)
        setContentView(binding.root)

        verifyStoragePermissions(this)

        binding.customToolB2.title = getString(R.string.post)
        binding.customToolB2.setTitleTextColor(Color.WHITE)


        binding.postBtn.setOnClickListener {
            val text = binding.postText.text.toString()

            if(text.length > 150){
                Toast.makeText(this,"Length should be less than 150 characters",Toast.LENGTH_SHORT).show()
            }else if(text.isNotBlank() && text.isNotEmpty() || isImageSelected){

                var uuid: String? = null
                if(isImageSelected) {
                    isImageSelected = !isImageSelected
                    uuid = UUID.randomUUID().toString()
                    uploadImage(text,mUri!!, uuid)
                }else{
                    postDao.addPost(text,uuid)
                }

                finish()
                overridePendingTransition(android.R.anim.slide_out_right,android.R.anim.fade_out)
            }
        }

        binding.gallery.setOnClickListener { openGallery() }
        binding.camera.setOnClickListener { openCamera() }
    }

    private fun openCamera(){
        if (ActivityCompat.checkSelfPermission(this,
                Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            try {
                val storageDirectory: File? =
                    applicationContext.getExternalFilesDir(Environment.DIRECTORY_PICTURES)
                val fileName = "new_photo"
                val currentImageFile = File.createTempFile(fileName, ".jpg", storageDirectory)
                mUri = FileProvider.getUriForFile(
                    this,
                    "com.snabeel130.buzztalk.fileprovider",
                    currentImageFile
                )
                val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
                intent.putExtra(MediaStore.EXTRA_OUTPUT, mUri)
                camLauncher.launch(intent)
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
    }

    private fun openGallery(){
        if (ActivityCompat.checkSelfPermission(this,
                Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            val intent = Intent(Intent.ACTION_PICK)
            intent.type = "image/*"
            galleryLauncher.launch(intent)
        }
    }

    private var camLauncher = registerForActivityResult(StartActivityForResult()) { result: ActivityResult ->

        try {
            if(result.resultCode != RESULT_OK){
                mUri = null
                return@registerForActivityResult
            }
            binding.imageViewPost.visibility = View.VISIBLE
            Glide.with(binding.imageViewPost.context).load(mUri).into(binding.imageViewPost)
            isImageSelected = true
        } catch (e: IOException) {
            Log.d(Helper.TAG, "IOException : " + e.message)
            e.printStackTrace()
        }
    }

    private val galleryLauncher = registerForActivityResult( StartActivityForResult()
    ) { result ->
        if(result.resultCode == RESULT_OK){
            mUri = result.data?.data!!
            if(mUri != null) {
                binding.imageViewPost.visibility = View.VISIBLE
                Glide.with(this).load(mUri).into(binding.imageViewPost)
                isImageSelected = true
            }else{
                Toast.makeText(
                    applicationContext,
                    "Couldn't pick image!",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun uploadImage(text: String, uri: Uri, uuid: String){
        MainActivity.isPostingCompleted = false
        GlobalScope.launch(Dispatchers.IO) {
            val storageRef = FirebaseStorage.getInstance().reference
            val ref = storageRef.child("images/$uuid")

            ref.putFile(uri).addOnCompleteListener {
                if (it.isSuccessful) {
                    Log.d(Helper.TAG, "Image uploaded")
                    postDao.addPost(text,uuid)
                    MainActivity.isPostingCompleted = true
                }
            }.addOnProgressListener {
                val progress = (100.0 * it.bytesTransferred / it.totalByteCount)
                val percent = progress.toInt()
                Log.d(Helper.TAG, "$percent%")
            }
        }
    }


    private fun verifyStoragePermissions(activity: Activity?) {

        val cameraPermission = ActivityCompat.checkSelfPermission(
            activity!!, Manifest.permission.CAMERA)

        val readStoragePermission = ActivityCompat.checkSelfPermission(
            activity,Manifest.permission.READ_EXTERNAL_STORAGE)

        Log.d(Helper.TAG, "$cameraPermission -- $readStoragePermission")
        if (cameraPermission != PackageManager.PERMISSION_GRANTED
            || readStoragePermission != PackageManager.PERMISSION_GRANTED) {
                Log.d(Helper.TAG, "sending intent..")
            ActivityCompat.requestPermissions(
                activity,
                Helper.PERMISSIONS_STORAGE_CAMERA,
                10
            )
        }
    }

}