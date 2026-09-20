package com.abrarshakhi.galva.common.mvi

/** The complete, immutable description of what a screen renders. */
interface UiState

/** Something the user did. The only way state changes. */
interface UiIntent

/** A one-shot consequence — navigate, show a snackbar, launch a system dialog. Never replayed. */
interface UiEffect
