package com.abrarshakhi.galva.feature.viewer.di

import com.abrarshakhi.galva.core.model.MediaSource
import com.abrarshakhi.galva.core.ui.di.coreUiModule
import com.abrarshakhi.galva.feature.viewer.ViewerViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val viewerModule = module {
    includes(coreUiModule)

    viewModel { (source: MediaSource, initialMediaId: Long) ->
        ViewerViewModel(
            source = source,
            initialMediaId = initialMediaId,
            observeMedia = get(),
            mediaActions = get(),
        )
    }
}
