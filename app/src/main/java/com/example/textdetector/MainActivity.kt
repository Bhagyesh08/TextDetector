package com.example.textdetector

import android.annotation.SuppressLint
import android.app.Activity
import android.app.Instrumentation
import android.app.ProgressDialog
import android.content.ContentValues
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.Build.VERSION_CODES.P
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.LiveFolders.INTENT
import android.provider.MediaStore
import android.view.Menu
import android.widget.PopupMenu
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.TextRecognizer
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.android.synthetic.main.activity_main.*
import java.util.jar.Manifest


private const val CAM_REQUEST_CODE=1
private const val STORAGE_REQUEST_CODE=2
class MainActivity : AppCompatActivity() {
   var uri:Uri?=null
   private lateinit var cam:Array<String>
   private lateinit var storage:Array<String>
   private lateinit var progressDialog:ProgressDialog
   private lateinit var textRecognizer: TextRecognizer

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        supportActionBar!!.setBackgroundDrawable(ColorDrawable(Color.parseColor("#305EF6")))
        cam= arrayOf(android.Manifest.permission.CAMERA,android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
        storage= arrayOf(android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
       progressDialog= ProgressDialog(this)
        progressDialog.setTitle("Please wait..")
        progressDialog.setCanceledOnTouchOutside(false)
        textRecognizer=TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
        camera.setOnClickListener {
         input()
        }
        recognized.setOnClickListener {
         if(uri==null){
             Toast.makeText(this,"Pick image first!!",Toast.LENGTH_SHORT).show()

         }else{
          RecognizetextfromImage()
         }
        }

    }
    @SuppressLint("SuspiciousIndentation")
    private fun RecognizetextfromImage(){
       progressDialog.setMessage("Loading Image...")
        progressDialog.show()
        try {
         val inputImage=InputImage.fromFilePath(this,uri!!)
            progressDialog.setMessage("Recognizing test...")
            val textResult=textRecognizer.process(inputImage)
                .addOnCompleteListener { text->
                    progressDialog.dismiss()
                    val recog=text.result.text
                    text2.setText(recog)

                }
                .addOnFailureListener {
                    progressDialog.dismiss()
                    Toast.makeText(this,"Fail to recognize",Toast.LENGTH_SHORT).show()

                }
        }
        catch (e:Exception){
            progressDialog.dismiss()
            Toast.makeText(this,"Failed to load..",Toast.LENGTH_SHORT).show()

        }
    }
    private fun input(){
        val popupMenu=PopupMenu(this,camera)
        popupMenu.menu.add(Menu.NONE,1,1,"Camera")
        popupMenu.menu.add(Menu.NONE,2,2,"Gallery")
        popupMenu.show()
        popupMenu.setOnMenuItemClickListener { menuItem->
            val id=menuItem.itemId
            if(id==1){
                if(cameraPermission()){
                    pickImageFromCamera()
                }else{
                    requestCamera()
                }
            }else if(id==2){
                if(storagePermission()){
                    pickImageFromGallery()
                }else{
                    requestStorage()
                }
            }
            return@setOnMenuItemClickListener true
        }
    }
    private fun pickImageFromGallery(){
    val intent =Intent(Intent.ACTION_PICK)
        intent.type="image/*"
        galleryLauncher.launch(intent)
    }
    private val galleryLauncher=registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ){result ->
        if(result.resultCode== Activity.RESULT_OK){
            val data=result.data
            uri=data!!.data
            image.setImageURI(uri)
        }else{
            Toast.makeText(this,"Request Cancelled!!",Toast.LENGTH_SHORT).show()
        }
    }
    private fun pickImageFromCamera(){
        val value=ContentValues()
        value.put(MediaStore.Images.Media.TITLE,"title")
        value.put(MediaStore.Images.Media.DESCRIPTION,"Description")
        uri=contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI,value)
        val intent=Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        intent.putExtra(MediaStore.EXTRA_OUTPUT,uri)
        cameraLauncher.launch(intent)
    }
    private val cameraLauncher=registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()){
            result->
        if(result.resultCode==Activity.RESULT_OK){
            image.setImageURI(uri)
        }else{
            Toast.makeText(this,"Request Cancelled!!",Toast.LENGTH_SHORT).show()
        }
    }
    private fun storagePermission():Boolean{
        return ContextCompat.checkSelfPermission(this,android.Manifest.permission.WRITE_EXTERNAL_STORAGE)==PackageManager.PERMISSION_GRANTED
    }
    private fun cameraPermission():Boolean{
        val cam= ContextCompat.checkSelfPermission(this,android.Manifest.permission.CAMERA)==PackageManager.PERMISSION_GRANTED
        val storage=ContextCompat.checkSelfPermission(this,android.Manifest.permission.WRITE_EXTERNAL_STORAGE)==PackageManager.PERMISSION_GRANTED
        return cam&&storage
    }
    private fun requestStorage(){
        ActivityCompat.requestPermissions(this,storage, STORAGE_REQUEST_CODE)
    }
    private fun requestCamera(){
        ActivityCompat.requestPermissions(this,cam, CAM_REQUEST_CODE)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when(requestCode){
          CAM_REQUEST_CODE->{
              if(grantResults.isNotEmpty()){
                  val camAccepted=grantResults[0]==PackageManager.PERMISSION_GRANTED
                  val storageAccepted=grantResults[1]==PackageManager.PERMISSION_GRANTED
                  if(camAccepted&&storageAccepted){
                      pickImageFromCamera()
                  }else{
                      Toast.makeText(this,"Require permission for camera and storage",Toast.LENGTH_SHORT).show()
                  }
              }
          }
            STORAGE_REQUEST_CODE ->{
                if(grantResults.isNotEmpty()){
                    val storageAccepted=grantResults[0]==PackageManager.PERMISSION_GRANTED
                    if(storageAccepted){
                        pickImageFromGallery()
                    }else{
                        Toast.makeText(this,"Storage Permission required",Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }
}