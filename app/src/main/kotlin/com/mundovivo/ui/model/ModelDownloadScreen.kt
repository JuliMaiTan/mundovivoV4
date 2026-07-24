package com.mundovivo.ui.model

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mundovivo.llm.*
import com.mundovivo.util.RamFormatter
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel para download de modelo.
 */
class ModelDownloadViewModel(private val modelManager: ModelManager) : ViewModel() {

    private val _uiState = MutableStateFlow<DownloadUiState>(DownloadUiState.Idle)
    val uiState: StateFlow<DownloadUiState> = _uiState.asStateFlow()

    fun startDownload(model: ModelInfo) {
        viewModelScope.launch {
            _uiState.value = DownloadUiState.Downloading(model, 0, 0.0)

            try {
                modelManager.downloadModel(model).collect { progress ->
                    _uiState.value = DownloadUiState.Downloading(
                        model = model,
                        percentage = progress.percentage,
                        speedMBps = progress.speedMBps
                    )
                }

                // Download completo, verifica integridade
                _uiState.value = DownloadUiState.Verifying(model)

                val status = modelManager.getModelStatus(model.id)
                if (status == ModelStatus.READY) {
                    _uiState.value = DownloadUiState.Success(model)
                } else {
                    _uiState.value = DownloadUiState.Error(model, "Arquivo corrompido")
                }
            } catch (e: Exception) {
                _uiState.value = DownloadUiState.Error(model, e.message ?: "Erro desconhecido")
            }
        }
    }

    sealed class DownloadUiState {
        object Idle : DownloadUiState()
        data class Downloading(
            val model: ModelInfo,
            val percentage: Int,
            val speedMBps: Double
        ) : DownloadUiState()
        data class Verifying(val model: ModelInfo) : DownloadUiState()
        data class Success(val model: ModelInfo) : DownloadUiState()
        data class Error(val model: ModelInfo, val message: String) : DownloadUiState()
    }
}

/**
 * Tela de download de modelo.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModelDownloadScreen(
    model: ModelInfo,
    onDownloadComplete: () -> Unit = {},
    onCancel: () -> Unit = {}
) {
    val context = LocalContext.current
    val modelManager = remember { ModelManager(context) }
    val viewModel = remember { ModelDownloadViewModel(modelManager) }
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(model) {
        viewModel.startDownload(model)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Download do Modelo") }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp, Alignment.CenterVertically)
        ) {
            when (val state = uiState) {
                is ModelDownloadViewModel.DownloadUiState.Idle -> {
                    CircularProgressIndicator()
                    Text("Preparando download...")
                }

                is ModelDownloadViewModel.DownloadUiState.Downloading -> {
                    ModelInfoCard(state.model)
                    
                    Text(
                        text = "${state.percentage}%",
                        style = MaterialTheme.typography.displayMedium,
                        fontWeight = FontWeight.Bold
                    )

                    LinearProgressIndicator(
                        progress = state.percentage / 100f,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Baixando...",
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Text(
                            text = "%.1f MB/s".format(state.speedMBps),
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    OutlinedButton(
                        onClick = onCancel,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Cancelar")
                    }
                }

                is ModelDownloadViewModel.DownloadUiState.Verifying -> {
                    CircularProgressIndicator()
                    Text(
                        text = "Verificando integridade...",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = "Calculando SHA256",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                is ModelDownloadViewModel.DownloadUiState.Success -> {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(64.dp)
                    )
                    Text(
                        text = "Download Completo!",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = state.model.name,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Button(
                        onClick = onDownloadComplete,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Continuar")
                    }
                }

                is ModelDownloadViewModel.DownloadUiState.Error -> {
                    Icon(
                        imageVector = Icons.Default.Error,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(64.dp)
                    )
                    Text(
                        text = "Erro no Download",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.error
                    )
                    Text(
                        text = state.message,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = onCancel,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Voltar")
                        }
                        Button(
                            onClick = { viewModel.startDownload(state.model) },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Tentar Novamente")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ModelInfoCard(model: ModelInfo) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = model.name,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Tamanho:", style = MaterialTheme.typography.bodySmall)
                Text(
                    text = RamFormatter.formatBytes(model.sizeBytes),
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Template:", style = MaterialTheme.typography.bodySmall)
                Text(
                    text = model.template.name,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
