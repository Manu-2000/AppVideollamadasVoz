package com.manu.proyectogrupal

import android.app.Application
import android.content.Intent
import android.os.Bundle
import android.util.Log // <-- Asegúrate de importar Log!
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.manu.proyectogrupal.databinding.ActivityLoginBinding
import com.zegocloud.uikit.prebuilt.call.ZegoUIKitPrebuiltCallService
import com.zegocloud.uikit.prebuilt.call.invite.ZegoUIKitPrebuiltCallInvitationConfig

// --- Definición de la Data Class para el Usuario ---
data class User(
    val userId: String = "",
    val name: String = ""
)
// --- Fin de la Data Class ---


class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    // Tag para los logs de depuración
    private val TAG = "LOGIN_DEBUG"
    // URL específica de tu base de datos regional
    private val databaseUrl = "https://proyectogrupalllamadas-default-rtdb.europe-west1.firebasedatabase.app"


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnLogin.setOnClickListener {
            Log.d(TAG, "Botón Ingresar Pulsado!") // Log 1

            val userId = binding.userId.text.toString().trim()
            Log.d(TAG, "UserID leído: '$userId'") // Log 2

            if (userId.isNotEmpty()) {
                Log.d(TAG, "UserID NO está vacío. Intentando guardar...") // Log 3

                // --- INICIO: Lógica de Firebase ---
                val userName = userId
                val userToSave = User(userId = userId, name = userName)
                // Obtenemos la referencia especificando la URL regional
                val usersRef: DatabaseReference = FirebaseDatabase.getInstance(databaseUrl).getReference("users")

                usersRef.child(userId).setValue(userToSave)
                    .addOnSuccessListener {
                        // --- Éxito al guardar en Firebase ---
                        Log.d(TAG, "Guardado en Firebase ¡ÉXITO!") // Log 4
                        Toast.makeText(applicationContext, "Usuario '$userId' registrado/actualizado.", Toast.LENGTH_SHORT).show()

                        // Continuamos solo si se guardó correctamente
                        setupZegoUIKit(userId)

                        val intent = Intent(this@LoginActivity, MainActivity::class.java)
                        intent.putExtra("userId", userId)
                        startActivity(intent)
                    }
                    .addOnFailureListener { databaseError ->
                        // --- Error al guardar en Firebase ---
                        Log.e(TAG, "Guardado en Firebase ¡FALLÓ!: ${databaseError.message}", databaseError) // Log 5
                        Toast.makeText(applicationContext, "Error al guardar en Firebase: ${databaseError.message}", Toast.LENGTH_LONG).show()
                    }
                // --- FIN: Lógica de Firebase ---

            } else {
                Log.d(TAG, "UserID ESTÁ vacío.") // Log 6
                Toast.makeText(applicationContext, "Ingrese su nombre de usuario", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // La función para inicializar ZegoUIKit (sin cambios)
    private fun setupZegoUIKit(userId: String) {
        val application: Application = application
        val appID: Long = 1057763744 // TU_APP_ID
        val appSign: String = "a185e9d0b5fac218203bc61085d137efe1ea58a8a0544d6d506aaad5991234c0" // TU_APP_SIGN
        val userName: String = userId

        val callInvitationConfig = ZegoUIKitPrebuiltCallInvitationConfig()
        ZegoUIKitPrebuiltCallService.init(application, appID, appSign, userId, userName, callInvitationConfig)
        Log.d(TAG, "ZegoUIKit inicializado para userId: $userId")
    }

    // Limpiar ZegoUIKit al destruir la Activity (sin cambios)
    override fun onDestroy() {
        super.onDestroy()
        try {
            ZegoUIKitPrebuiltCallService.unInit()
            Log.d(TAG, "ZegoUIKit desinicializado.")
        } catch (e: Exception) {
            Log.w(TAG, "Error al desinicializar ZegoUIKit: ${e.message}")
        }
    }
}