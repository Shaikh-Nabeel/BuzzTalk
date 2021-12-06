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
import com.nabeel130.buzztalk.utility.Helper.Companion.TAG
import id.zelory.compressor.Compressor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.io.File
import java.io.IOException
import java.util.*


class CreatePostActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCreatePostBinding
    private var mUri: Uri? = null
    private var isImageSelected = false
    private var isImageFromCamera = false
    private val postDao = PostDao()
    private var currentFile: File? = null

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

                    runBlocking {
                        val compressedImg = Compressor.compress(applicationContext, currentFile!!)
                        Log.d(TAG,  "Compression successful")

                        mUri = if(isImageFromCamera) {
                            Uri.fromFile(compressedImg)
                        }else{
                            Uri.fromFile(compressedImg)
                        }
                    }

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
                currentFile = currentImageFile
                val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
                intent.putExtra(MediaStore.EXTRA_OUTPUT, mUri)
                camLauncher.launch(intent)
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }else verifyStoragePermissions(this)
    }

    private fun openGallery(){
        if (ActivityCompat.checkSelfPermission(this,
                Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            val intent = Intent(Intent.ACTION_PICK)
            intent.type = "image/*"
            galleryLauncher.launch(intent)
        }else verifyStoragePermissions(this)
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
            isImageFromCamera = true
        } catch (e: IOException) {
            Log.d(TAG, "IOException : " + e.message)
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
                getFileFromMediaStore(mUri!!)
                isImageSelected = true
                isImageFromCamera = false
            }else{
                Toast.makeText(
                    applicationContext,
                    "Couldn't pick image!",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun getFileFromMediaStore(uri: Uri){
        val cursor = contentResolver.query(uri, arrayOf(MediaStore.Images.Media.DATA),null,null,null)

        if(cursor != null){
            if(cursor.moveToFirst()){
                val filePath = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA))
                currentFile = File(filePath)
                Log.d(TAG, "Got file path")
            }else{
                Log.d(TAG, "Could not find file")
            }
        }
        cursor?.close()
    }

    private fun uploadImage(text: String, uri: Uri, uuid: String){
        MainActivity.isPostingCompleted = false
        GlobalScope.launch(Dispatchers.IO) {
            val storageRef = FirebaseStorage.getInstance().reference
            val ref = storageRef.child("images/$uuid")

            ref.putFile(uri).addOnCompleteListener {
                if (it.isSuccessful) {
                    Log.d(TAG, "Image uploaded")
                    postDao.addPost(text,uuid)
                    MainActivity.isPostingCompleted = true
                }
            }.addOnProgressListener {
                val progress = (100.0 * it.bytesTransferred / it.totalByteCount)
                val percent = progress.toInt()
                Log.d(TAG, "$percent%")
            }
        }
    }


    private fun verifyStoragePermissions(activity: Activity?) {

        val cameraPermission = ActivityCompat.checkSelfPermission(
            activity!!, Manifest.permission.CAMERA)

        val readStoragePermission = ActivityCompat.checkSelfPermission(
            activity,Manifest.permission.READ_EXTERNAL_STORAGE)

        Log.d(TAG, "$cameraPermission -- $readStoragePermission")
        if (cameraPermission != PackageManager.PERMISSION_GRANTED
            || readStoragePermission != PackageManager.PERMISSION_GRANTED) {
                Log.d(TAG, "sending intent..")
            ActivityCompat.requestPermissions(
                activity,
                Helper.PERMISSIONS_STORAGE_CAMERA,
                10
            )
        }
    }

}