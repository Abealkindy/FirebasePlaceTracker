package com.rosinante.firebasetracker

import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.Manifest
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.IBinder
import android.support.v4.app.NotificationCompat
import android.support.v4.content.ContextCompat
import android.util.Log


import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.firebase.auth.FirebaseAuth

import com.google.firebase.database.FirebaseDatabase

class TrackerService : Service() {

    private var stopReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            Log.d(TAG, "received stop broadcast")
            // Stop the service when the notification is tapped
            unregisterReceiver(this)
            stopSelf()
        }
    }

    override fun onBind(intent: Intent): IBinder? {
        return null
    }

    override fun onCreate() {
        super.onCreate()
        buildNotification()
        loginToFirebase()
    }

    private fun buildNotification() {
        val stop = "stop"
        registerReceiver(stopReceiver, IntentFilter(stop))
        val broadcastIntent = PendingIntent.getBroadcast(
                this, 0, Intent(stop), PendingIntent.FLAG_UPDATE_CURRENT)
        // Create the persistent notification
        val builder = NotificationCompat.Builder(this)
                .setContentTitle(getString(R.string.app_name))
                .setContentText(getString(R.string.notification_text))
                .setOngoing(true)
                .setContentIntent(broadcastIntent)
                .setSmallIcon(R.drawable.ic_tracker)
        startForeground(1, builder.build())
    }

    private fun loginToFirebase() {
        // Authenticate with Firebase, and request location updates
        val email = getString(R.string.firebase_email)
        val password = getString(R.string.firebase_password)
        FirebaseAuth.getInstance().signInWithEmailAndPassword(
                email, password).addOnCompleteListener { task ->
            if (task.isSuccessful) {
                Log.d(TAG, "firebase auth success")
                requestLocationUpdates()
            } else {
                Log.d(TAG, "firebase auth failed")
            }
        }
    }

    private fun requestLocationUpdates() {
        val request = LocationRequest()
        request.interval = 10000
        request.fastestInterval = 5000
        request.priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        val client = LocationServices.getFusedLocationProviderClient(this)
        val path = "location" + "/" + "123"
        val permission = ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION)
        if (permission == PackageManager.PERMISSION_GRANTED) {
            // Request location updates and when an update is
            // received, store the location in Firebase
            client.requestLocationUpdates(request, object : LocationCallback() {
                override fun onLocationResult(locationResult: LocationResult?) {
                    val ref = FirebaseDatabase.getInstance().getReference(path)
                    val location = locationResult!!.lastLocation
                    if (location != null) {
                        Log.d(TAG, "location update " + location)
                        ref.setValue(location)
                    }
                }
            }, null)
        }
    }

    companion object {
        private val TAG = TrackerService::class.java.simpleName
    }

}
