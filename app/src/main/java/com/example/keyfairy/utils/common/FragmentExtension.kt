package com.example.keyfairy.utils.common

import androidx.fragment.app.Fragment

// Extensión para navegación simple
fun Fragment.navigateToFragment(
    fragment: Fragment,
    navigationType: NavigationManager.NavigationType = NavigationManager.NavigationType.REPLACE_WITH_CLEAR_STACK,
    containerId: Int = android.R.id.content
) {
    NavigationManager.navigateToFragment(
        fragmentManager = parentFragmentManager,
        fragment = fragment,
        containerId = containerId,
        navigationType = navigationType
    )
}

// Para navegación con retorno
fun Fragment.navigateWithBackStack(fragment: Fragment, containerId: Int = android.R.id.content) {
    navigateToFragment(fragment, NavigationManager.NavigationType.REPLACE_WITH_BACK_STACK, containerId)
}

// Para limpiar stack y navegar
fun Fragment.navigateAndClearStack(fragment: Fragment, containerId: Int = android.R.id.content) {
    navigateToFragment(fragment, NavigationManager.NavigationType.REPLACE_WITH_CLEAR_STACK, containerId)
}

// Para ir atrás
fun Fragment.goBack(): Boolean {
    return NavigationManager.goBack(parentFragmentManager)
}