package com.mundovivo.ui.model

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mundovivo.llm.*
import com.mundovivo.util.AssetCopier
import com.mundovivo.util.DeviceInfo
import com.mundovivo.util.RamFormatter
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel para tela de teste LLM.
 */
class LLMTestViewModel(private val context: android.content.Context) : ViewModel() {

    private val llamaEngine = LlamaEngine(context)
    private val narrativeGenerator = NarrativeGenerator(llamaEngine)
    private val modelManager = ModelManager(context)
    private val deviceInfo = DeviceInfo(context)

    // GBNF grammar copiada para filesDir na primeira execução — necessária para
    // constrained decoding no llama.cpp (força contrato JSON válido).
    private val grammarFile = AssetCopier.getNarratorGrammarFile(context)

    private val _uiState = MutableStateFlow<TestUiState>(TestUiState.NotLoaded)
    val uiState: StateFlow<TestUiState> = _uiState.asStateFlow()

    private val _streamingText = MutableStateFlow("")
    val streamingText: StateFlow<String> = _streamingText.asStateFlow()

    fun loadModelAndTest(model: ModelInfo) {
        viewModelScope.launch {
            _uiState.value = TestUiState.Loading("Carregando modelo...")

            val modelFile = modelManager.getModelFile(model.id)
            // Passa a grammar GBNF — essencial para validar constrained decoding.
            val loadResult = llamaEngine.loadModel(modelFile, grammarFile)

            if (loadResult.isFailure) {
                _uiState.value = TestUiState.Error(
                    loadResult.exceptionOrNull()?.message ?: "Falha ao carregar modelo"
                )
                return@launch
            }

            _uiState.value = TestUiState.Generating
            _streamingText.value = ""

            narrativeGenerator.generateTest(model.template).collect { result ->
                when (result) {
                    is NarrativeGenerator.NarrativeResult.PromptBuilt -> { /* debug */ }
                    is NarrativeGenerator.NarrativeResult.StreamingToken -> {
                        _streamingText.value += result.token
                    }
                    is NarrativeGenerator.NarrativeResult.GenerationComplete -> { /* aguarda */ }
                    is NarrativeGenerator.NarrativeResult.Success -> {
                        _uiState.value = TestUiState.Success(
                            contract = result.contract,
                            metrics = result.metrics,
                            deviceRamMB = deviceInfo.availableRamMB,
                            rawJson = _streamingText.value
                        )
                    }
                    is NarrativeGenerator.NarrativeResult.SuccessWithFallback -> {
                        _uiState.value = TestUiState.PartialSuccess(
                            contract = result.contract,
                            error = result.originalError,
                            rawJson = _streamingText.value
                        )
                    }
                    is NarrativeGenerator.NarrativeResult.Failed -> {
                        _uiState.value = TestUiState.Error(result.error)
                    }
                }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        // freeModel() agora é síncrono/non-suspend — não depende do viewModelScope,
        // que já pode estar cancelado neste ponto. Isso garante que o modelo nativo
        // seja realmente liberado quando o ViewModel morre.
        llamaEngine.freeModel()
    }

    sealed class TestUiState {
        object NotLoaded : TestUiState()
        data class Loading(val message: String) : TestUiState()
        object Generating : TestUiState()
        data class Success(
            val contract: NarratorContract,
            val metrics: LlamaEngine.GenerationResult.Complete?,
            val deviceRamMB: Long,
            val rawJson: String
        ) : TestUiState()
        data class PartialSuccess(
            val contract: NarratorContract,
            val error: String,
            val rawJson: String
        ) : TestUiState()
        data class Error(val message: String) : TestUiState()
    }
}

/**
 * Tela de teste do LLM.
 *
 * Aceite Fase 0:
 * - Prompt taverna → JSON válido (GBNF)
 * - Arrays corretos
 * - Campo error existe
 * - Tone no enum
 * - Métricas: tok/s, TTFT, RAM
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LLMTestScreen(
    model: ModelInfo,
    onBack: () -> Unit = {}
) {
    val context = LocalContext.current
    val viewModel = remember { LLMTestViewModel(context) }
    val uiState by viewModel.uiState.collectAsState()
    val streamingText by viewModel.streamingText.collectAsState()

    LaunchedEffect(model) {
        viewModel.loadModelAndTest(model)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Teste LLM - Taverna") },
                actions = {
                    IconButton(onClick = { viewModel.loadModelAndTest(model) }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Rodar novamente")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            ModelBadge(model)
            PromptCard()

            when (val state = uiState) {
                is LLMTestViewModel.TestUiState.NotLoaded -> Text("Aguardando início...")
                is LLMTestViewModel.TestUiState.Loading -> LoadingCard(state.message)
                is LLMTestViewModel.TestUiState.Generating -> StreamingCard(streamingText)
                is LLMTestViewModel.TestUiState.Success -> {
                    MetricsCard(state.metrics, state.deviceRamMB)
                    ValidationCard(state.contract, valid = true)
                    NarrativeCard(state.contract.narrative)
                    RawJsonCard(state.rawJson)
                }
                is LLMTestViewModel.TestUiState.PartialSuccess -> {
                    WarningCard("Fallback ativado: ${state.error}")
                    ValidationCard(state.contract, valid = false)
                    NarrativeCard(state.contract.narrative)
                    RawJsonCard(state.rawJson)
                }
                is LLMTestViewModel.TestUiState.Error -> ErrorCard(state.message)
            }
        }
    }
}

@Composable
fun ModelBadge(model: ModelInfo) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(Icons.Default.PlayArrow, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Column {
                Text(text = "Modelo Ativo", style = MaterialTheme.typography.labelSmall)
                Text(text = model.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun PromptCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(text = "Prompt de Teste", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            Text(
                text = "Cenário: Taverna à noite, Torvin no canto, jogador pede bebida",
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@Composable
fun LoadingCard(message: String) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            CircularProgressIndicator(modifier = Modifier.size(24.dp))
            Text(message, style = MaterialTheme.typography.bodyLarge)
        }
    }
}

@Composable
fun StreamingCard(text: String) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                CircularProgressIndicator(modifier = Modifier.size(16.dp))
                Text(text = "Gerando (streaming)...", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            }
            Text(text = text, style = MaterialTheme.typography.bodySmall, fontFamily = FontFamily.Monospace)
        }
    }
}

@Composable
fun MetricsCard(metrics: LlamaEngine.GenerationResult.Complete?, deviceRamMB: Long) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(text = "Métricas de Performance", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            if (metrics != null) {
                MetricRow("Tokens/segundo", "%.2f tok/s".format(metrics.tokensPerSecond))
                MetricRow("TTFT", "${metrics.ttftMs}ms")
                MetricRow("Tempo total", "${metrics.totalTimeMs}ms")
                MetricRow("Tokens gerados", metrics.tokensGenerated.toString())
            }
            MetricRow("RAM disponível", RamFormatter.formatMB(deviceRamMB))
        }
    }
}

@Composable
fun MetricRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(text = label, style = MaterialTheme.typography.bodyMedium)
        Text(text = value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun ValidationCard(contract: NarratorContract, valid: Boolean) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (valid) Color(0xFF1B5E20).copy(alpha = 0.2f) else MaterialTheme.colorScheme.errorContainer
        )
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(
                    imageVector = if (valid) Icons.Default.CheckCircle else Icons.Default.Error,
                    contentDescription = null,
                    tint = if (valid) Color(0xFF4CAF50) else MaterialTheme.colorScheme.error
                )
                Text(
                    text = if (valid) "JSON Válido (GBNF)" else "JSON com Fallback",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            ValidationItem("contract_version", contract.contract_version)
            ValidationItem("response_type", contract.response_type.name)
            ValidationItem("tone", contract.tone.name)
            ValidationItem("sensory_focus (array)", contract.sensory_focus.joinToString())
            ValidationItem("npcs_mentioned (array)", contract.npcs_mentioned.joinToString())
            ValidationItem("warnings (array)", contract.warnings.joinToString().ifEmpty { "[]" })
            ValidationItem("error", contract.error ?: "null")
        }
    }
}

@Composable
fun ValidationItem(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(text = "$label:", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
        Text(text = value, style = MaterialTheme.typography.bodySmall, fontFamily = FontFamily.Monospace, modifier = Modifier.padding(start = 8.dp))
    }
}

@Composable
fun NarrativeCard(narrative: String) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(text = "Narrativa Gerada", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(text = narrative, style = MaterialTheme.typography.bodyLarge, lineHeight = 24.sp)
        }
    }
}

@Composable
fun RawJsonCard(json: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(text = "JSON Raw", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            Text(text = json, style = MaterialTheme.typography.bodySmall, fontFamily = FontFamily.Monospace, fontSize = 10.sp)
        }
    }
}

@Composable
fun WarningCard(message: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(Icons.Default.Error, contentDescription = null, tint = MaterialTheme.colorScheme.error)
            Text(text = message, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onErrorContainer)
        }
    }
}

@Composable
fun ErrorCard(message: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Default.Error, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                Text(text = "Erro", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
            Text(text = message, style = MaterialTheme.typography.bodyMedium)
        }
    }
}
