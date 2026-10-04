package com.abrarshakhi.galva.navigation

import androidx.navigation3.runtime.NavKey

class Navigator(private val state: NavigationState) {

    fun navigate(route: NavKey) {
        if (route in state.backStacks.keys) {
            state.topLevelRoute = route
        } else {
            state.backStacks.getValue(state.topLevelRoute).add(route)
        }
    }

    fun goBack() {
        val currentStack = state.backStacks.getValue(state.topLevelRoute)
        if (currentStack.last() == state.topLevelRoute) {
            state.topLevelRoute = state.startRoute
        } else {
            currentStack.removeLastOrNull()
        }
    }
}