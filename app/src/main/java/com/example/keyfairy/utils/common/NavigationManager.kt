package com.example.keyfairy.utils.common

import android.util.Log
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager

object NavigationManager {

    enum class NavigationType {
        REPLACE_WITH_CLEAR_STACK,    // Para flujos lineales
        REPLACE_WITH_BACK_STACK,     // Para navegación con retorno
        ADD_TO_STACK                 // Para modales/overlays
    }

    fun navigateToFragment(
        fragmentManager: FragmentManager,
        fragment: Fragment,
        containerId: Int,
        navigationType: NavigationType = NavigationType.REPLACE_WITH_CLEAR_STACK,
        tag: String? = null
    ) {
        try {
            val fragmentTag = tag ?: fragment::class.java.simpleName

            when (navigationType) {
                NavigationType.REPLACE_WITH_CLEAR_STACK -> {
                    // Limpiar stack completo y reemplazar
                    fragmentManager.popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE)
                    fragmentManager.beginTransaction()
                        .replace(containerId, fragment, fragmentTag)
                        .commit()
                    Log.d("NavigationManager", "Navigated to $fragmentTag with cleared stack")
                }

                NavigationType.REPLACE_WITH_BACK_STACK -> {
                    // Reemplazar pero mantener en back stack
                    fragmentManager.beginTransaction()
                        .replace(containerId, fragment, fragmentTag)
                        .addToBackStack(fragmentTag)
                        .commit()
                    Log.d("NavigationManager", "Navigated to $fragmentTag with back stack")
                }

                NavigationType.ADD_TO_STACK -> {
                    // Agregar encima del actual
                    fragmentManager.beginTransaction()
                        .add(containerId, fragment, fragmentTag)
                        .addToBackStack(fragmentTag)
                        .commit()
                    Log.d("NavigationManager", "Added $fragmentTag to stack")
                }
            }

        } catch (e: Exception) {
            Log.e("NavigationManager", "Error navigating to ${fragment::class.java.simpleName}: ${e.message}")
        }
    }

    fun goBack(fragmentManager: FragmentManager): Boolean {
        return if (fragmentManager.backStackEntryCount > 0) {
            fragmentManager.popBackStack()
            true
        } else {
            false
        }
    }

    fun clearBackStack(fragmentManager: FragmentManager) {
        fragmentManager.popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE)
    }
}