package com.abrarshakhi.galva.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.abrarshakhi.galva.core.data.repository.DocumentRepository
import com.abrarshakhi.galva.core.designsystem.markdown.MarkdownBlock
import com.abrarshakhi.galva.core.designsystem.markdown.parseMarkdown
import com.abrarshakhi.galva.core.model.AppDocument
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class DocumentUiState(
    val document: AppDocument,
    val blocks: List<MarkdownBlock> = emptyList(),
    val isLoading: Boolean = true,
    val failed: Boolean = false,
)

class DocumentViewModel(
    document: AppDocument,
    private val repository: DocumentRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(DocumentUiState(document))
    val state: StateFlow<DocumentUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            val blocks = runCatching { parseMarkdown(repository.read(document)) }
                .map { blocks -> blocks.dropWhile { it is MarkdownBlock.Heading && it.level == 1 } }
            _state.update {
                it.copy(
                    blocks = blocks.getOrDefault(emptyList()),
                    isLoading = false,
                    failed = blocks.isFailure,
                )
            }
        }
    }
}
