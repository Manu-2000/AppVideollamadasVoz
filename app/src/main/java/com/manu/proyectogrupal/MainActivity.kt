package com.manu.proyectogrupal

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.database.*
import com.manu.proyectogrupal.adapters.OnUserClickListener
import com.manu.proyectogrupal.adapters.UserAdapter
import com.manu.proyectogrupal.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity(), OnUserClickListener {

    private lateinit var binding: ActivityMainBinding
    private var localUserId: String? = null
    private lateinit var usersRef: DatabaseReference
    private lateinit var userListener: ValueEventListener
    private lateinit var userAdapter: UserAdapter
    private var userList = mutableListOf<User>()
    private val TAG = "MAIN_ACTIVITY_DEBUG"
    private val databaseUrl = "https://proyectogrupalllamadas-default-rtdb.europe-west1.firebasedatabase.app"

    private var targetUserIdForPermission: String? = null
    private var targetUserNameForPermission: String? = null
    private var startVideoCallAfterPermission: Boolean = false

    private val requestMultiplePermissionsLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            val allGranted = permissions.entries.all { it.value }

            if (allGranted) {
                Log.d(TAG, "Permisos de Cámara y Micrófono CONCEDIDOS por el usuario.")
                if (targetUserIdForPermission != null && targetUserNameForPermission != null) {
                    navigateToCallScreen(
                        targetUserId = targetUserIdForPermission!!,
                        targetUserName = targetUserNameForPermission!!,
                        isVideoCall = startVideoCallAfterPermission
                    )
                } else {
                    Log.w(TAG, "Permisos concedidos, pero no hay datos de llamada pendiente guardados.")
                }
            } else {
                Log.w(TAG, "Permisos de Cámara y/o Micrófono DENEGADOS por el usuario.")
                Toast.makeText(this, "Se necesitan permisos de cámara y micrófono para realizar llamadas.", Toast.LENGTH_LONG).show()
            }

            targetUserIdForPermission = null
            targetUserNameForPermission = null
            startVideoCallAfterPermission = false
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        localUserId = intent.getStringExtra("userId")

        if (localUserId == null) {
            Toast.makeText(this, "Error: Usuario local no identificado.", Toast.LENGTH_LONG).show()
            finish()
            return
        }

        binding.tvNameUserId.text = "¡Hola $localUserId! \n ¿A quién deseas llamar?"

        setupRecyclerView()
        setupFirebaseListener()
        setupCallButtons()
    }

    private fun setupRecyclerView() {
        userAdapter = UserAdapter(userList, this)
        binding.rvUsers.apply {
            layoutManager = LinearLayoutManager(this@MainActivity)
            adapter = userAdapter
        }
    }

    private fun setupFirebaseListener() {
        usersRef = FirebaseDatabase.getInstance(databaseUrl).getReference("users")

        userListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                userList.clear()
                for (userSnapshot in snapshot.children) {
                    try {
                        val user = userSnapshot.getValue(User::class.java)
                        if (user != null && user.userId.isNotEmpty() && user.userId != localUserId) {
                            userList.add(user)
                        }
                    } catch (e: DatabaseException) {
                        Log.e(TAG, "Error al convertir snapshot a User", e)
                    }
                }
                userAdapter.updateUsers(userList)
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e(TAG, "Error al leer usuarios de Firebase: ${error.message}", error.toException())
                Toast.makeText(applicationContext, "Error al leer usuarios: ${error.message}", Toast.LENGTH_LONG).show()
            }
        }
        usersRef.addValueEventListener(userListener)
    }

    override fun onUserClick(user: User) {
        val targetUserId = user.userId
        val targetUserName = user.name

        targetUserIdForPermission = targetUserId
        targetUserNameForPermission = targetUserName

        binding.btnStartVideoCall.visibility = View.VISIBLE
        binding.btnStartVoiceCall.visibility = View.VISIBLE

        Toast.makeText(this, "Seleccionaste: $targetUserName", Toast.LENGTH_SHORT).show()
    }

    private fun setupCallButtons() {
        binding.btnStartVideoCall.setOnClickListener {
            if (targetUserIdForPermission != null && targetUserNameForPermission != null) {
                checkAndRequestPermissions(
                    targetUserIdForPermission!!,
                    targetUserNameForPermission!!,
                    isVideo = true
                )
            } else {
                Toast.makeText(this, "Selecciona un usuario primero", Toast.LENGTH_SHORT).show()
            }
        }

        binding.btnStartVoiceCall.setOnClickListener {
            if (targetUserIdForPermission != null && targetUserNameForPermission != null) {
                checkAndRequestPermissions(
                    targetUserIdForPermission!!,
                    targetUserNameForPermission!!,
                    isVideo = false
                )
            } else {
                Toast.makeText(this, "Selecciona un usuario primero", Toast.LENGTH_SHORT).show()
            }
        }

        binding.btnStartVideoCall.visibility = View.GONE
        binding.btnStartVoiceCall.visibility = View.GONE
    }

    private fun checkAndRequestPermissions(targetId: String, targetName: String, isVideo: Boolean) {
        val requiredPermissions = arrayOf(
            Manifest.permission.CAMERA,
            Manifest.permission.RECORD_AUDIO
        )

        val cameraGranted = ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        val audioGranted = ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED

        if (cameraGranted && audioGranted) {
            navigateToCallScreen(targetId, targetName, isVideo)
        } else {
            targetUserIdForPermission = targetId
            targetUserNameForPermission = targetName
            startVideoCallAfterPermission = isVideo

            requestMultiplePermissionsLauncher.launch(requiredPermissions)
        }
    }

    private fun navigateToCallScreen(targetUserId: String, targetUserName: String, isVideoCall: Boolean) {
        val intent = Intent(this, CallActivity::class.java).apply {
            putExtra("TARGET_USER_ID", targetUserId)
            putExtra("TARGET_USER_NAME", targetUserName)
            putExtra("LOCAL_USER_ID", localUserId)
            putExtra("IS_VIDEO_CALL", isVideoCall)
        }
        startActivity(intent)

        // Ocultamos los botones después de iniciar la llamada
        binding.btnStartVideoCall.visibility = View.GONE
        binding.btnStartVoiceCall.visibility = View.GONE
    }

    override fun onDestroy() {
        super.onDestroy()
        if (::usersRef.isInitialized && ::userListener.isInitialized) {
            usersRef.removeEventListener(userListener)
        }
    }
}
