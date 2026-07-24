package com.mundovivo.ui.model

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.mundovivo.llm.ModelCatalog
import com.mundovivo.llm.ModelInfo
import com.mundovivo.llm.ModelSelectionPolicy
import com.mundovivo.ui.theme.MundoVivoTheme
import com.mundovivo.util.DeviceInfo
import com.mundovivo.util.RamFormatter

/**
 * Tela de seleção de modelo LLM.
 * 
 * Fase 0: UI estática para validação visual.
 * Fase 1: Integração com ViewModel + navegação.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModelSelectionScreen(
    modifier: Modifier = Modifier,
    onModelSelected: (ModelInfo) -> Unit = {}
) {
    val context = LocalContext.current
    val deviceInfo = remember { DeviceInfo(context) }
    val policy = remember { ModelSelectionPolicy(deviceInfo) }
    val recommendation = remember { policy.getRecommendation() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Escolha o Modelo de IA") }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Info do dispositivo
            DeviceInfoCard(deviceInfo)

            // Recomendação
            RecommendationCard(recommendation.recommended)

            // Modelo recomendado
            ModelCard(
                model = ModelCatalog.QWEN_1_5B,
                badge = "⚡ Rápido e leve",
                recommended = true,
                warnings = policy.getWarnings(ModelCatalog.QWEN_1_5B),
                onClick = { onModelSelected(ModelCatalog.QWEN_1_5B) }
            )

            // Alternativas
            val gemmaWarnings = policy.getWarnings(ModelCatalog.GEMMA_2B)
            ModelCard(
                model = ModelCatalog.GEMMA_2B,
                badge = "📖 Narrativa mais densa",
                recommended = false,
                warnings = gemmaWarnings,
                onClick = { onModelSelected(ModelCatalog.GEMMA_2B) }
            )
        }
    }
}

@Composable
fun DeviceInfoCard(deviceInfo: DeviceInfo) {
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
                text = "📱 Informações do Dispositivo",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "RAM: ${RamFormatter.formatMB(deviceInfo.totalRamMB)}",
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                text = "Espaço livre: ${deviceInfo.storageFreeGB}GB",
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                text = "Modelo: ${deviceInfo.deviceModel}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun RecommendationCard(recommended: ModelInfo) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
            Column {
                Text(
                    text = "Recomendado para este aparelho",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Text(
                    text = recommended.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
fun ModelCard(
    model: ModelInfo,
    badge: String,
    recommended: Boolean,
    warnings: List<String>,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick,
        colors = if (recommended) {
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.secondaryContainer
            )
        } else {
            CardDefaults.cardColors()
        }
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Badge + Nome
            Text(
                text = badge,
                style = MaterialTheme.typography.labelLarge,
                color = if (recommended) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.primary
            )
            Text(
                text = model.name,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )

            // Descrição
            Text(
                text = model.description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // Specs
            Divider(modifier = Modifier.padding(vertical = 8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "Tamanho",
                        style = MaterialTheme.typography.labelSmall
                    )
                    Text(
                        text = "%.1f GB".format(model.sizeGB),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                Column {
                    Text(
                        text = "RAM mínima",
                        style = MaterialTheme.typography.labelSmall
                    )
                    Text(
                        text = RamFormatter.formatMB(model.minRamMB.toLong()),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                Column {
                    Text(
                        text = "Template",
                        style = MaterialTheme.typography.labelSmall
                    )
                    Text(
                        text = model.template.name,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Warnings
            if (warnings.isNotEmpty()) {
                Divider(modifier = Modifier.padding(vertical = 8.dp))
                warnings.forEach { warning ->
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = warning,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun ModelSelectionScreenPreview() {
    MundoVivoTheme {
        ModelSelectionScreen()
    }
}
