package com.manu.proyectogrupal.adapters // Asegúrate de que el paquete sea correcto

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.manu.proyectogrupal.R // Importa R para acceder a los layouts e IDs
import com.manu.proyectogrupal.User // Importa tu data class User

/**
 * Interfaz para comunicar eventos de clic desde el Adapter hacia la Activity/Fragment.
 * La Activity/Fragment que use este adapter deberá implementar esta interfaz.
 */
interface OnUserClickListener {
    /**
     * Se llama cuando se hace clic en un elemento de la lista.
     * @param user El objeto User correspondiente al elemento clickeado.
     */
    fun onUserClick(user: User)
}

/**
 * Adaptador para mostrar la lista de usuarios en un RecyclerView.
 *
 * @property users Lista de objetos User a mostrar.
 * @property listener Objeto que implementa OnUserClickListener para manejar los clics.
 */
class UserAdapter(
    private var users: List<User>, // Lista de usuarios (inicialmente puede estar vacía)
    private val listener: OnUserClickListener // La Activity/Fragment que escucha los clics
) : RecyclerView.Adapter<UserAdapter.UserViewHolder>() {

    /**
     * ViewHolder: Representa y gestiona la vista de una única fila (item) en el RecyclerView.
     * Mantiene referencias a las subvistas dentro de la fila (ej: TextView) para acceso rápido.
     *
     * @param itemView La vista raíz de la fila (inflada desde item_user.xml).
     */
    inner class UserViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        // Referencia al TextView dentro de item_user.xml donde mostraremos el nombre
        val userNameTextView: TextView = itemView.findViewById(R.id.tvUserName)

        // Bloque de inicialización del ViewHolder: se ejecuta cuando se crea una instancia.
        // Aquí configuramos el OnClickListener para toda la vista de la fila (itemView).
        init {
            itemView.setOnClickListener {
                // Obtenemos la posición del item clickeado dentro del adapter.
                val position = adapterPosition
                // Es importante verificar que la posición sea válida antes de usarla.
                // adapterPosition puede devolver NO_POSITION si el item está siendo eliminado
                // o si el clic ocurre durante una animación o cambio de layout.
                if (position != RecyclerView.NO_POSITION) {
                    // Si la posición es válida, obtenemos el usuario de nuestra lista 'users'
                    // en esa posición y notificamos al 'listener' (la Activity) a través
                    // de la interfaz OnUserClickListener.
                    listener.onUserClick(users[position])
                }
            }
        }
    }

    /**
     * Se llama cuando RecyclerView necesita crear un nuevo ViewHolder.
     * Infla el layout XML de la fila (item_user.xml) y crea una instancia del ViewHolder.
     *
     * @param parent El ViewGroup al que se añadirá la nueva vista después de ser vinculada.
     * @param viewType El tipo de vista del nuevo elemento (útil si tienes diferentes tipos de filas).
     * @return Una nueva instancia de UserViewHolder que contiene la vista de la fila.
     */
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UserViewHolder {
        // Inflamos (creamos la vista) a partir del layout XML definido en R.layout.item_user.
        // El contexto se obtiene del parent.
        // 'false' en attachToRoot significa que RecyclerView se encargará de añadir la vista al parent.
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_user, parent, false)
        // Creamos y devolvemos el ViewHolder pasándole la vista inflada.
        return UserViewHolder(view)
    }

    /**
     * Se llama cuando RecyclerView necesita mostrar los datos en un ViewHolder específico.
     * Obtiene los datos del modelo (lista 'users') para la posición dada y actualiza
     * las vistas dentro del ViewHolder correspondiente.
     *
     * @param holder El ViewHolder que debe ser actualizado para representar el contenido
     *               del elemento en la posición dada.
     * @param position La posición del elemento dentro del conjunto de datos del adaptador.
     */
    override fun onBindViewHolder(holder: UserViewHolder, position: Int) {
        // Obtenemos el objeto User de nuestra lista 'users' en la posición actual.
        val user = users[position]
        // Establecemos el texto del TextView dentro del ViewHolder con el nombre del usuario.
        // Como actualmente el nombre es igual al ID, mostramos eso.
        // Podrías cambiarlo si tuvieras nombres distintos: "${user.name} (${user.userId})"
        holder.userNameTextView.text = user.name
    }

    /**
     * Devuelve el número total de elementos en el conjunto de datos que maneja el adaptador.
     * RecyclerView lo usa para saber cuántos items debe mostrar.
     *
     * @return El tamaño de la lista 'users'.
     */
    override fun getItemCount(): Int {
        return users.size
    }

    /**
     * Método público para actualizar la lista de usuarios que muestra el adaptador.
     * La Activity llamará a este método cuando reciba nuevos datos (p. ej., de Firebase).
     *
     * @param newUsers La nueva lista de usuarios a mostrar.
     */
    fun updateUsers(newUsers: List<User>) {
        // Actualizamos nuestra lista interna 'users' con la nueva lista recibida.
        this.users = newUsers
        // Notificamos al RecyclerView que todo el conjunto de datos ha cambiado.
        // Esto hace que RecyclerView redibuje toda la lista.
        // NOTA: Para optimización en listas grandes, se recomienda usar ListAdapter con DiffUtil
        // en lugar de notifyDataSetChanged(), ya que calcula diferencias y anima los cambios.
        // Pero para este caso, notifyDataSetChanged() es suficiente y más simple.
        notifyDataSetChanged()
    }
}