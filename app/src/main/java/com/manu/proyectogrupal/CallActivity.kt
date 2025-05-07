package com.manu.proyectogrupal

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import com.zegocloud.uikit.prebuilt.call.ZegoUIKitPrebuiltCallConfig
// Importaciones ZegoUIKit
import com.zegocloud.uikit.prebuilt.call.ZegoUIKitPrebuiltCallFragment
import com.zegocloud.uikit.service.defines.ZegoUIKitUser


class CallActivity : AppCompatActivity() {

    private val TAG = "CALL_ACTIVITY_DEBUG"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_call)

        Log.d(TAG, "onCreate - Iniciando CallActivity.")

        // 1. Recuperar datos del Intent
        val targetUserId = intent.getStringExtra("TARGET_USER_ID")
        val targetUserName = intent.getStringExtra("TARGET_USER_NAME")
        val localUserId = intent.getStringExtra("LOCAL_USER_ID")
        val isVideoCall = intent.getBooleanExtra("IS_VIDEO_CALL", true)

        // 2. Validar datos esenciales
        if (targetUserId.isNullOrEmpty() || localUserId.isNullOrEmpty()) {
            Log.e(TAG, "Error: Faltan IDs de usuario (local o target) en el Intent.")
            Toast.makeText(this, "Error al iniciar llamada: IDs de usuario faltantes.", Toast.LENGTH_LONG).show()
            finish()
            return
        }

        val localUserName = localUserId
        val finalTargetUserName = targetUserName ?: targetUserId

        Log.d(TAG, "Datos recibidos -> Local: $localUserName ($localUserId), Target: $finalTargetUserName ($targetUserId), Video: $isVideoCall")

        // 3. Añadir el Fragmento de ZegoUIKitPrebuiltCallFragment
        addCallFragment(localUserId, localUserName, targetUserId, finalTargetUserName, isVideoCall)
    }

    private fun addCallFragment(
        localUserId: String,
        localUserName: String,
        targetUserId: String,
        targetUserName: String,
        isVideoCall: Boolean
    ) {
        Log.d(TAG, "Añadiendo ZegoUIKitPrebuiltCallFragment usando newInstance (Corregido)...")

        // Credenciales Zego
        val appID: Long = 1057763744 // TU_APP_ID
        val appSign: String = "a185e9d0b5fac218203bc61085d137efe1ea58a8a0544d6d506aaad5991234c0" // TU_APP_SIGN

        // CallID
        val callID = if (localUserId < targetUserId) {
            "${localUserId}_${targetUserId}"
        } else {
            "${targetUserId}_${localUserId}"
        }
        Log.d(TAG, "Generado CallID: $callID")

        // Configuración
        val config = if (isVideoCall) {
            ZegoUIKitPrebuiltCallConfig.oneOnOneVideoCall()
        } else {
            ZegoUIKitPrebuiltCallConfig.oneOnOneVoiceCall()
        }

        // Construir el Fragmento usando newInstance (SIN application)
        try {
            // --- CORRECCIÓN AQUÍ: Se elimina 'application' como primer argumento ---
            val callFragment = ZegoUIKitPrebuiltCallFragment.newInstance(
                // application, // <-- ELIMINADO
                appID,
                appSign,
                localUserId,
                localUserName,
                callID,
                config // Pasar la configuración aquí
            )
            // --- FIN CORRECCIÓN ---

            // Pasar los invitees a través de arguments sigue siendo necesario
            val bundle = Bundle()
            bundle.putParcelableArrayList("zego_uikit_prebuilt_call_fragment_invitees", arrayListOf(ZegoUIKitUser(targetUserId, targetUserName)))
            callFragment.arguments = bundle
            Log.d(TAG, "Arguments seteados para el fragmento con invitees.")


            // Añadir el fragmento al contenedor
            Log.d(TAG, "Intentando añadir el fragmento (newInstance corregido) al contenedor...")
            supportFragmentManager.beginTransaction()
                .replace(R.id.fragment_container_view_zego, callFragment)
                .commitNow()
            Log.d(TAG, "Fragmento añadido correctamente.")

        } catch (e: Exception) {
            Log.e(TAG, "Error CRÍTICO al construir o añadir el fragmento Zego: ${e.message}", e)
            Toast.makeText(this, "Error fatal al iniciar Zego: ${e.message}", Toast.LENGTH_LONG).show()
            finish()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "onDestroy - CallActivity destruida.")
    }
}