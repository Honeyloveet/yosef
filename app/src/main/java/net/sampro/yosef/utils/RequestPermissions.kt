package net.sampro.yosef.utils

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Environment
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat

class RequestPermissions(private val context: Context, private val activity: AppCompatActivity) {

    fun hasWriteExternalStoragePermission() =
        ActivityCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED

    fun hasReadExternalStoragePermission() =
        ActivityCompat.checkSelfPermission(context, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED

    fun hasManageExternalStoragePermission() : Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Environment.isExternalStorageManager()
        } else {
            true
        }
    }

    fun requestManageExternalStoragePermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            ActivityCompat.requestPermissions(activity, arrayOf(Manifest.permission.MANAGE_EXTERNAL_STORAGE), 6)
        }
    }

    fun requestWritePermission() {
        ActivityCompat.requestPermissions(activity, arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE), 5)
    }

    fun hasSmsPermission(): Boolean {
        return ActivityCompat.checkSelfPermission(context, Manifest.permission.SEND_SMS) == PackageManager.PERMISSION_GRANTED
    }

    fun hasCallPermission(): Boolean {
        return ActivityCompat.checkSelfPermission(context, Manifest.permission.CALL_PHONE) == PackageManager.PERMISSION_GRANTED
    }

    fun requestSmsPermissions() {
        ActivityCompat.requestPermissions(activity, arrayOf(Manifest.permission.SEND_SMS), 101)
    }

    fun requestCallPermissions() {
        ActivityCompat.requestPermissions(activity, arrayOf(Manifest.permission.CALL_PHONE), 121)
    }

    fun requestReadWritePermissions() {
        val permissionToRequest = mutableListOf<String>()

        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
            if (!hasWriteExternalStoragePermission()) {
                permissionToRequest.add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            }
        }

        if (!hasReadExternalStoragePermission()) {
            permissionToRequest.add(Manifest.permission.READ_EXTERNAL_STORAGE)
        }

        if (permissionToRequest.isNotEmpty()) {
            ActivityCompat.requestPermissions(activity, permissionToRequest.toTypedArray(), 0)
        }
    }

    fun checkPermission(): Boolean {
        return if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
            hasWriteExternalStoragePermission()
        } else {
            hasReadExternalStoragePermission()
        }
    }

}