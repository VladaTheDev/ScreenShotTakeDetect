package com.vladadev.screenshottakedetect

import android.Manifest.permission.READ_EXTERNAL_STORAGE
import android.content.ContentResolver
import android.database.ContentObserver
import android.database.Cursor
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import java.util.*
import kotlin.math.abs

class MainActivity : AppCompatActivity() {

    companion object {
        private val EXTERNAL_CONTENT_URI_MATCHER =
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI.toString()
        private val INTERNAL_CONTENT_URI_MATCHER =
            MediaStore.Images.Media.INTERNAL_CONTENT_URI.toString()
        private val PROJECTION = arrayOf(
            MediaStore.Images.Media.DISPLAY_NAME,
            MediaStore.Images.Media.DATA,
            MediaStore.Images.Media.DATE_ADDED
        )
        private const val SORT_ORDER = MediaStore.Images.Media.DATE_ADDED + " DESC"
        private const val DEFAULT_DETECT_WINDOW_SECONDS: Long = 10
        private const val REQUEST_PERMISSION_CODE = 12157
    }

    private lateinit var resolver: ContentResolver
    private val contentObserver = object : ContentObserver(null) {
        override fun onChange(selfChange: Boolean, uri: Uri) {
            if (uri.toString().startsWith(EXTERNAL_CONTENT_URI_MATCHER) ||
                uri.toString().startsWith(INTERNAL_CONTENT_URI_MATCHER)
            ) {
                checkIfIsScreenshot(uri)
            }
            super.onChange(selfChange, uri)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        resolver = this.contentResolver
    }

    override fun onStart() {
        super.onStart()
        requestPermissionForReadExternalStorage()
    }

    private fun requestPermissionForReadExternalStorage() {
        try {
            ActivityCompat.requestPermissions(
                this, arrayOf(READ_EXTERNAL_STORAGE),
                REQUEST_PERMISSION_CODE
            )
        } catch (e: Exception) {
            //nop
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_PERMISSION_CODE) {
            resolver.registerContentObserver(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI, true, contentObserver
            )
        }
    }

    private fun checkIfIsScreenshot(uri: Uri) {
        val cursor: Cursor? = resolver.query(uri, PROJECTION, null, null, SORT_ORDER)
        try {
            if (cursor?.moveToFirst() == true) {
                val path = cursor.getString(
                    cursor.getColumnIndex(MediaStore.Images.Media.DATA)
                )
                val dateAdded = cursor.getLong(
                    cursor.getColumnIndex(
                        MediaStore.Images.Media.DATE_ADDED
                    )
                )
                val currentTime = System.currentTimeMillis() / 1000
                if (matchPath(path) && matchTime(currentTime, dateAdded)) {
                    runOnUiThread {
                        Toast.makeText(applicationContext, "New screenshot", Toast.LENGTH_LONG)
                            .show()
                    }
                }
            }
        } catch (e: Exception) {
            //nop
        } finally {
            cursor?.close()
        }
    }

    private fun matchPath(path: String): Boolean {
        return path.toLowerCase(Locale.getDefault()).contains("screenshot")
    }

    private fun matchTime(currentTime: Long, dateAdded: Long): Boolean {
        return abs(currentTime - dateAdded) <= DEFAULT_DETECT_WINDOW_SECONDS
    }

    override fun onPause() {
        super.onPause()
        contentResolver.unregisterContentObserver(contentObserver)
    }
}
