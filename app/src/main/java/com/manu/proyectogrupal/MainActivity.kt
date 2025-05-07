package com.manu.proyectogrupal

import android.Manifest // Importar Manifest para nombres de permisos
import android.content.Intent // Importar Intent para navegación
import android.content.pm.PackageManager // Importar PackageManager para verificar permisos
import android.os.Bundle
import android.util.Log
import android.view.View // Sigue siendo necesario para View.VISIBLE/INVISIBLE si se usara, aunque ahora no
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts // Importar para el lanzador de permisos
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat // Importar para verificar permisos
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.database.*
import com.manu.proyectogrupal.adapters.OnUserClickListener // Asegúrate que la ruta sea correcta
import com.manu.proyectogrupal.adapters.UserAdapter         // Asegúrate que la ruta sea correcta
import com.manu.proyectogrupal.databinding.ActivityMainBinding
// import com.zegocloud.uikit.service.defines.ZegoUIKitUser // Ya no se usa aquí directamente
// import java.util.Collections // Ya no se usa aquí directamente

// MainActivity sigue implementando la interfaz del Adapter
class MainActivity : AppCompatActivity(), OnUserClickListener {

    private lateinit var binding: ActivityMainBinding
    private var localUserId: String? = null
    private lateinit var usersRef: DatabaseReference
    private lateinit var userListener: ValueEventListener
    private lateinit var userAdapter: UserAdapter
    private var userList = mutableListOf<User>()
    private val TAG = "MAIN_ACTIVITY_DEBUG"
    private val databaseUrl = "https://proyectogrupalllamadas-default-rtdb.europe-west1.firebasedatabase.app"

    // --- Variables para manejar el flujo de permisos y la llamada pendiente ---
    private var targetUserIdForPermission: String? = null
    private var targetUserNameForPermission: String? = null
    private var startVideoCallAfterPermission: Boolean = false

    // --- Lanzador para solicitar múltiples permisos ---
    private val requestMultiplePermissionsLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            // Callback que se ejecuta después de que el usuario responde a la solicitud de permisos

            // Verificamos si todos los permisos solicitados fueron concedidos
            val allGranted = permissions.entries.all { it.value }

            if (allGranted) {
                Log.d(TAG, "Permisos de Cámara y Micrófono CONCEDIDOS por el usuario.")
                // Si se concedieron y teníamos una llamada pendiente guardada, procedemos a navegar
                if (targetUserIdForPermission != null && targetUserNameForPermission != null) {
                    Log.d(TAG, "Procediendo a navegar a CallActivity con datos pendientes.")
                    navigateToCallScreen(
                        targetUserId = targetUserIdForPermission!!, // Usamos !! porque acabamos de comprobar que no es null
                        targetUserName = targetUserNameForPermission!!,
                        isVideoCall = startVideoCallAfterPermission // Usamos el valor guardado
                    )
                } else {
                    // Esto no debería pasar si la lógica es correcta, pero lo logueamos por si acaso
                    Log.w(TAG, "Permisos concedidos, pero no hay datos de llamada pendiente guardados.")
                }
            } else {
                // Uno o ambos permisos fueron denegados por el usuario
                Log.w(TAG, "Permisos de Cámara y/o Micrófono DENEGADOS por el usuario.")
                Toast.makeText(this, "Se necesitan permisos de cámara y micrófono para realizar llamadas.", Toast.LENGTH_LONG).show()
                // No hacemos nada más, el usuario no podrá llamar sin permisos.
            }

            // Limpiamos las variables pendientes independientemente del resultado
            targetUserIdForPermission = null
            targetUserNameForPermission = null
            startVideoCallAfterPermission = false
        }


    // --- onCreate: Configuración inicial de la Activity ---
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
        Log.d(TAG, "Usuario local identificado: $localUserId")

        binding.tvNameUserId.text = "¡Hola $localUserId! \n ¿A quién deseas llamar?"

        setupRecyclerView()
        setupFirebaseListener()

        // Ya no necesitamos configurar botones Zego aquí al inicio
    }

    // --- Configuración del RecyclerView ---
    private fun setupRecyclerView() {
        Log.d(TAG, "Configurando RecyclerView")
        // Inicializamos el adaptador con una lista vacía y 'this' como listener de clics
        userAdapter = UserAdapter(userList, this)
        binding.rvUsers.apply {
            layoutManager = LinearLayoutManager(this@MainActivity)
            adapter = userAdapter
        }
    }

    // --- Configuración del Listener de Firebase ---
    private fun setupFirebaseListener() {
        Log.d(TAG, "Configurando listener de Firebase en URL: $databaseUrl")
        usersRef = FirebaseDatabase.getInstance(databaseUrl).getReference("users")

        userListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                Log.d(TAG, "Datos recibidos/actualizados de Firebase: ${snapshot.childrenCount} usuarios totales")
                userList.clear() // Limpiamos la lista antes de llenarla
                for (userSnapshot in snapshot.children) {
                    try {
                        val user = userSnapshot.getValue(User::class.java)
                        // Añadimos solo si el usuario no es null y NO es el usuario local
                        if (user != null && user.userId.isNotEmpty() && user.userId != localUserId) {
                            userList.add(user)
                        } else if (user == null) {
                            Log.w(TAG, "Usuario null encontrado en snapshot: ${userSnapshot.key}")
                        }
                    } catch (e: DatabaseException) {
                        Log.e(TAG, "Error al convertir snapshot a User: ${userSnapshot.key}", e)
                    }
                }
                Log.d(TAG, "Lista filtrada para mostrar: ${userList.size} usuarios")
                userAdapter.updateUsers(userList) // Actualizamos el adaptador (y el RecyclerView)

                if (userList.isEmpty()) {
                    Log.d(TAG, "No hay otros usuarios para mostrar en la lista.")
                    // Podrías mostrar un TextView indicando que no hay usuarios
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e(TAG, "Error al leer usuarios de Firebase: ${error.message}", error.toException())
                Toast.makeText(applicationContext, "Error al leer usuarios: ${error.message}", Toast.LENGTH_LONG).show()
            }
        }
        // Añadimos el listener para que escuche cambios en el nodo "users"
        usersRef.addValueEventListener(userListener)
        Log.d(TAG, "ValueEventListener añadido a usersRef")
    }

    // --- Implementación del clic en un usuario de la lista ---
    // Este método es llamado por UserAdapter cuando se hace clic en una fila
    override fun onUserClick(user: User) {
        val targetUserId = user.userId
        val targetUserName = user.name // Obtenemos también el nombre
        Log.d(TAG, "Usuario seleccionado: $targetUserName (ID: $targetUserId)")
        Toast.makeText(this, "Iniciando llamada con: $targetUserName...", Toast.LENGTH_SHORT).show()

        // 1. Decide si será videollamada o llamada de voz.
        //    Por ahora, lo ponemos fijo a true (videollamada) para probar.
        //    En una app real, podrías tener dos botones (Video/Voz) que se muestren
        //    después de la selección y cada uno llamaría a checkAndRequestPermissions
        //    con el valor apropiado para 'isVideo'.
        val isVideoCall = true

        // 2. Comprueba permisos y procede a navegar si están concedidos, o solicítalos.
        checkAndRequestPermissions(targetUserId, targetUserName, isVideoCall)
    }

    // --- Función para verificar permisos y solicitar si es necesario ---
    private fun checkAndRequestPermissions(targetId: String, targetName: String, isVideo: Boolean) {
        // Permisos requeridos para llamadas de voz/video
        val requiredPermissions = arrayOf(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO)

        // Verificamos si ya tenemos los permisos concedidos
        val cameraPermissionGranted = ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        val audioPermissionGranted = ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED

        if (cameraPermissionGranted && audioPermissionGranted) {
            // Si ya tenemos ambos permisos, navegamos directamente a la pantalla de llamada
            Log.d(TAG, "Permisos ya concedidos. Navegando a CallActivity.")
            navigateToCallScreen(targetId, targetName, isVideo)
        } else {
            // Si falta al menos un permiso, los solicitamos
            Log.d(TAG, "Permisos NO concedidos completamente (Cam: $cameraPermissionGranted, Aud: $audioPermissionGranted). Solicitando...")
            // Guardamos los detalles de la llamada que se intentó hacer para ejecutarla si se conceden los permisos
            targetUserIdForPermission = targetId
            targetUserNameForPermission = targetName
            startVideoCallAfterPermission = isVideo

            // Lanzamos el diálogo del sistema para solicitar los permisos definidos en requiredPermissions
            requestMultiplePermissionsLauncher.launch(requiredPermissions)
        }
    }

    // --- Función para iniciar la CallActivity ---
    private fun navigateToCallScreen(targetUserId: String, targetUserName: String, isVideoCall: Boolean) {
        Log.d(TAG, "Navegando a CallActivity -> target: $targetUserName ($targetUserId), local: $localUserId, video: $isVideoCall")
        // Creamos un Intent para abrir CallActivity
        val intent = Intent(this, CallActivity::class.java).apply {
            // Añadimos la información necesaria como Extras para que CallActivity la reciba
            putExtra("TARGET_USER_ID", targetUserId)
            putExtra("TARGET_USER_NAME", targetUserName)
            putExtra("LOCAL_USER_ID", localUserId) // Es importante pasar el ID local también
            putExtra("IS_VIDEO_CALL", isVideoCall)
            // Podrías necesitar pasar más datos dependiendo de cómo configures ZegoUIKitPrebuiltCallFragment
        }
        // Iniciamos la CallActivity
        startActivity(intent)
    }


    // --- onDestroy: Limpieza al destruir la Activity ---
    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "onDestroy llamado - Removiendo listener de Firebase si está inicializado.")
        // Es crucial remover el listener de Firebase para evitar fugas de memoria
        if (::usersRef.isInitialized && ::userListener.isInitialized) {
            usersRef.removeEventListener(userListener)
            Log.d(TAG, "ValueEventListener de Firebase removido.")
        } else {
            Log.w(TAG, "Listener de Firebase no estaba inicializado, no se pudo remover.")
        }
        // NOTA: ZegoUIKit se limpia en LoginActivity.onDestroy. Considerar si debería limpiarse
        //       de forma más centralizada al cerrar la app completamente.
    }
}