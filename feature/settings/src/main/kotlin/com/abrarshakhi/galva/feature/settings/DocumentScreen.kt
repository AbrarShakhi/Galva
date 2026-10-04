package com.abrarshakhi.galva.feature.settings

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.ContainedLoadingIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.abrarshakhi.galva.core.designsystem.component.AdaptiveTopAppBar
import com.abrarshakhi.galva.core.designsystem.layout.ReadableWidth
import com.abrarshakhi.galva.core.designsystem.markdown.MarkdownBlockText
import com.abrarshakhi.galva.core.model.AppDocument
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf

@Composable
fun DocumentRoute(document: AppDocument, onBack: () -> Unit) {
    DocumentScreen(
        viewModel = koinViewModel(key = document.name) { parametersOf(document) },
        onBack = onBack,
    )
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun DocumentScreen(
    viewModel: DocumentViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            AdaptiveTopAppBar(
                title = { Text(state.document.title) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
                    }
                },
                scrollBehavior = scrollBehavior,
            )
        },
    ) { padding ->
        when {
            state.isLoading -> Box(Modifier.fillMaxSize().padding(padding), Alignment.Center) {
                ContainedLoadingIndicator(modifier = Modifier.size(72.dp))
            }

            state.failed -> Box(Modifier.fillMaxSize().padding(padding), Alignment.Center) {
                Text(
                    text = "This page couldn't be opened",
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(24.dp),
                )
            }

            else -> LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal)),
                horizontalAlignment = Alignment.CenterHorizontally,
                contentPadding = PaddingValues(
                    start = 24.dp,
                    end = 24.dp,
                    top = padding.calculateTopPadding() + 8.dp,
                    bottom = padding.calculateBottomPadding() + 32.dp,
                ),
            ) {
                itemsIndexed(state.blocks) { _, block ->
                    ReadableWidth {
                        MarkdownBlockText(
                            block = block,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp),
                        )
                    }
                }
            }
        }
    }
}

internal val AppDocument.title: String
    get() = when (this) {
        AppDocument.ABOUT -> "About Galva"
        AppDocument.TERMS -> "Terms of service"
        AppDocument.PRIVACY -> "Privacy policy"
        AppDocument.CREDITS -> "Credits"
    }
