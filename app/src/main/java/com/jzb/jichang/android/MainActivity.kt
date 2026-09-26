package com.jzb.jichang.android

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.ArrowDownward
import androidx.compose.material.icons.outlined.CloudDownload
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Devices
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Hub
import androidx.compose.material.icons.outlined.Link
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.jzb.jichang.android.model.AppState
import com.jzb.jichang.android.model.ProxyNode
import com.jzb.jichang.android.model.PolicyGroup
import com.jzb.jichang.android.service.MihomoConfigGenerator
import com.jzb.jichang.android.share.LocalShareController
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.getInsetsController(window, window.decorView).apply {
            isAppearanceLightStatusBars = true
            isAppearanceLightNavigationBars = true
        }
        setContent { JichangApp() }
    }
}

private enum class AppPage(val label: String) { Sources("订阅"), Nodes("节点"), Rules("规则"), Export("配置") }
private enum class DialogKind { Source, Node, Group, Rule }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun JichangApp(viewModel: AppViewModel = viewModel()) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current
    var page by remember { mutableStateOf(AppPage.Sources) }
    var dialog by remember { mutableStateOf<DialogKind?>(null) }
    var editingGroup by remember { mutableStateOf<PolicyGroup?>(null) }
    val shareController = remember { LocalShareController(context) }
    val shareUrl by shareController.url.collectAsState()
    val shareError by shareController.error.collectAsState()
    var configText by remember { mutableStateOf(viewModel.generate().yaml) }

    val saveConfig = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/yaml")) { uri: Uri? ->
        if (uri != null) {
            val content = configText
            viewModel.run {
                withContext(Dispatchers.IO) { context.contentResolver.openOutputStream(uri)?.use { it.write(content.toByteArray()) } }
                "Mihomo 配置已保存"
            }
        }
    }

    DisposableEffect(shareController) { onDispose { shareController.unbind() } }
    LaunchedEffect(state) {
        configText = viewModel.generate().yaml
        shareController.updateConfig(configText)
    }

    MaterialTheme {
        Scaffold(
            contentWindowInsets = WindowInsets(0),
            topBar = {
                TopAppBar(
                    title = {
                        Column {
                            Text(stringResource(R.string.app_name), fontWeight = FontWeight.SemiBold)
                            Text("Mihomo 配置管理", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    },
                    actions = {
                        if (page == AppPage.Sources) IconButton(onClick = { dialog = DialogKind.Source }) { Icon(Icons.Outlined.Add, "添加订阅") }
                        if (page == AppPage.Nodes) IconButton(onClick = { dialog = DialogKind.Node }) { Icon(Icons.Outlined.Add, "添加节点") }
                        if (page == AppPage.Rules) IconButton(onClick = { editingGroup = null; dialog = DialogKind.Group }) { Icon(Icons.Outlined.Add, "添加策略组") }
                    },
                )
            },
            bottomBar = {
                NavigationBar {
                    AppPage.entries.forEach { item ->
                        val icon = when (item) {
                            AppPage.Sources -> Icons.Outlined.CloudDownload
                            AppPage.Nodes -> Icons.Outlined.Devices
                            AppPage.Rules -> Icons.Outlined.Tune
                            AppPage.Export -> Icons.Outlined.Description
                        }
                        NavigationBarItem(selected = page == item, onClick = { page = item }, icon = { Icon(icon, null) }, label = { Text(item.label) })
                    }
                }
            },
            floatingActionButton = {
                if (page == AppPage.Rules) FloatingActionButton(onClick = { dialog = DialogKind.Rule }) { Icon(Icons.Outlined.Add, "添加规则") }
            },
        ) { insets ->
            Column(Modifier.fillMaxSize().padding(insets)) {
                viewModel.message?.let { message ->
                    Card(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)) {
                        Text(message, Modifier.padding(12.dp), style = MaterialTheme.typography.bodySmall)
                    }
                }
                when (page) {
                    AppPage.Sources -> SourcesPage(state, viewModel, { dialog = DialogKind.Source }, Modifier.weight(1f))
                    AppPage.Nodes -> NodesPage(state, viewModel, Modifier.weight(1f)) { dialog = DialogKind.Node }
                    AppPage.Rules -> RulesPage(state, viewModel, Modifier.weight(1f), onEditGroup = { editingGroup = it; dialog = DialogKind.Group })
                    AppPage.Export -> ExportPage(
                        state = state,
                        configText = configText,
                        shareUrl = shareUrl,
                        shareError = shareError,
                        onGenerate = {
                            configText = viewModel.generate().yaml
                            shareController.updateConfig(configText)
                        },
                        onSave = { saveConfig.launch("mihomo.yaml") },
                        onShare = { shareController.start(configText) },
                        onStopShare = { shareController.stop() },
                        onShareLink = {
                            val intent = Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, shareUrl)
                            context.startActivity(Intent.createChooser(intent, "分享配置链接"))
                        },
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }

    when (dialog) {
        DialogKind.Source -> SourceDialog(onDismiss = { dialog = null }, onSave = { name, url -> viewModel.addSource(name, url); dialog = null })
        DialogKind.Node -> NodeDialog(onDismiss = { dialog = null }, onSave = { raw -> viewModel.importNodes(raw); dialog = null })
        DialogKind.Group -> GroupDialog(state, editingGroup, onDismiss = { dialog = null; editingGroup = null }, onSave = { name, type, members ->
            val existing = editingGroup
            if (existing == null) viewModel.addGroup(name, type, members) else viewModel.updateGroup(existing.name, name, type, members)
            dialog = null; editingGroup = null
        })
        DialogKind.Rule -> RuleDialog(state, onDismiss = { dialog = null }, onSave = { type, value, group -> viewModel.addRule(type, value, group); dialog = null })
        null -> Unit
    }
}

@Composable
private fun SourcesPage(state: AppState, viewModel: AppViewModel, onAdd: () -> Unit, modifier: Modifier = Modifier) {
    LazyColumn(modifier, contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            HeaderCard("本机管理", "订阅和节点只保存在此设备。刷新在本机解析，配置由 Mihomo 格式生成。")
        }
        if (state.sources.isEmpty()) item { EmptyCard("还没有订阅", "添加机场订阅，或到“节点”页直接导入节点链接。", onAdd) }
        items(state.sources, key = { it.id }) { source ->
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(source.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Medium)
                            Text(runCatching { java.net.URI(source.url).host }.getOrNull() ?: "订阅地址", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        IconButton(onClick = { viewModel.refreshSource(source.id) }) { Icon(Icons.Outlined.ArrowDownward, "刷新订阅") }
                        IconButton(onClick = { viewModel.removeSource(source.id) }) { Icon(Icons.Outlined.Delete, "删除订阅") }
                    }
                    val count = state.nodes.count { it.sourceId == source.id }
                    Text(if (source.lastError != null) source.lastError else "$count 个节点${source.updatedAt?.let { " · 已更新" } ?: " · 尚未刷新"}", style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}

@Composable
private fun NodesPage(state: AppState, viewModel: AppViewModel, modifier: Modifier = Modifier, onAdd: () -> Unit) {
    var filter by remember { mutableStateOf("") }
    val nodes = state.nodes.filter { filter.isBlank() || it.name.contains(filter, true) || it.server.contains(filter, true) }
    Column(modifier.padding(horizontal = 16.dp)) {
        OutlinedTextField(filter, { filter = it }, Modifier.fillMaxWidth().padding(top = 8.dp), label = { Text("筛选节点") }, singleLine = true)
        if (state.nodes.isEmpty()) EmptyCard("还没有节点", "可导入 Clash/Mihomo YAML、Base64 订阅，或常见节点链接。", onAdd)
        else LazyColumn(Modifier.weight(1f), contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(nodes, key = { it.id }) { node -> NodeRow(node, onToggle = { viewModel.toggleNode(node.id) }, onDelete = { viewModel.removeNode(node.id) }) }
        }
        Text("已启用 ${state.nodes.count { it.enabled }} / ${state.nodes.size}", Modifier.padding(bottom = 8.dp), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun NodeRow(node: ProxyNode, onToggle: () -> Unit, onDelete: () -> Unit) {
    Card(Modifier.fillMaxWidth()) {
        Row(Modifier.padding(start = 14.dp, end = 6.dp, top = 8.dp, bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(node.name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Medium)
                Text("${node.type.uppercase()} · ${node.server}:${node.port}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Switch(checked = node.enabled, onCheckedChange = { onToggle() })
            IconButton(onClick = onDelete) { Icon(Icons.Outlined.Delete, "删除节点") }
        }
    }
}

@Composable
private fun RulesPage(state: AppState, viewModel: AppViewModel, modifier: Modifier = Modifier, onEditGroup: (PolicyGroup) -> Unit) {
    LazyColumn(modifier, contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item { HeaderCard("策略组", "组可包含节点、其他组，或 DIRECT / REJECT。规则按列表顺序写入 Mihomo 配置。") }
        items(state.ruleProfile.groups, key = { it.name }) { group ->
            Card(Modifier.fillMaxWidth()) {
                Row(Modifier.padding(start = 16.dp, end = 4.dp, top = 10.dp, bottom = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.Hub, null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(group.name, style = MaterialTheme.typography.titleSmall)
                        Text("${group.type} · ${group.members.size} 个自定义成员", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    IconButton(onClick = { onEditGroup(group) }) { Icon(Icons.Outlined.Edit, "编辑策略组") }
                    IconButton(onClick = { viewModel.removeGroup(group.name) }) { Icon(Icons.Outlined.Delete, "删除策略组") }
                }
            }
        }
        item { Text("路由规则", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 12.dp)) }
        if (state.ruleProfile.rules.isEmpty()) item { Text("还没有自定义规则；配置会自动追加 MATCH 兜底。", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
        items(state.ruleProfile.rules.size) { index ->
            val rule = state.ruleProfile.rules[index]
            Card(Modifier.fillMaxWidth()) {
                Row(Modifier.padding(start = 14.dp, end = 4.dp, top = 8.dp, bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("${rule.type}, ${rule.value}", style = MaterialTheme.typography.bodyMedium)
                        Text("→ ${rule.group}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                    }
                    IconButton(onClick = { viewModel.removeRule(index) }) { Icon(Icons.Outlined.Delete, "删除规则") }
                }
            }
        }
        item { Spacer(Modifier.height(72.dp)) }
    }
}

@Composable
private fun ExportPage(
    state: AppState,
    configText: String,
    shareUrl: String?,
    shareError: String?,
    onGenerate: () -> Unit,
    onSave: () -> Unit,
    onShare: () -> Unit,
    onStopShare: () -> Unit,
    onShareLink: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val generated = remember(state) { MihomoConfigGenerator().generate(state) }
    Column(modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            AssistChip(onClick = {}, label = { Text("${generated.exportedNodes} 个节点") })
            if (generated.skippedNodes > 0) AssistChip(onClick = {}, label = { Text("跳过 ${generated.skippedNodes} 个不支持项") })
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = onGenerate, modifier = Modifier.weight(1f)) { Icon(Icons.Outlined.Tune, null); Spacer(Modifier.width(6.dp)); Text("重新生成") }
            OutlinedButton(onClick = onSave, modifier = Modifier.weight(1f)) { Icon(Icons.Outlined.Description, null); Spacer(Modifier.width(6.dp)); Text("保存 YAML") }
        }
        if (shareUrl == null) {
            if (shareError != null) Text(shareError, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            Button(onClick = onShare, modifier = Modifier.fillMaxWidth()) { Icon(Icons.Outlined.Link, null); Spacer(Modifier.width(8.dp)); Text("开始局域网分享") }
        } else Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)) {
            Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("局域网分享已开启", style = MaterialTheme.typography.titleSmall)
                SelectionContainer { Text(shareUrl, style = MaterialTheme.typography.bodySmall, fontFamily = FontFamily.Monospace) }
                Text("接收设备需连接同一局域网。随机链接包含访问凭据，请只分享给信任的人。", style = MaterialTheme.typography.bodySmall)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = onShareLink, modifier = Modifier.weight(1f)) { Icon(Icons.Outlined.Share, null); Spacer(Modifier.width(5.dp)); Text("分享链接") }
                    TextButton(onClick = onStopShare) { Text("停止") }
                }
            }
        }
        Text("Mihomo YAML 预览", style = MaterialTheme.typography.titleSmall)
        Card(Modifier.fillMaxSize(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
            SelectionContainer {
                Text(configText, Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(12.dp), fontFamily = FontFamily.Monospace, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
private fun HeaderCard(title: String, detail: String) {
    Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Text(detail, style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun EmptyCard(title: String, detail: String, onAction: () -> Unit) {
    Card(Modifier.fillMaxWidth().padding(vertical = 16.dp)) {
        Column(Modifier.fillMaxWidth().padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(detail, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            OutlinedButton(onClick = onAction) { Icon(Icons.Outlined.Add, null); Spacer(Modifier.width(6.dp)); Text("添加") }
        }
    }
}

@Composable
private fun SourceDialog(onDismiss: () -> Unit, onSave: (String, String) -> Unit) {
    var name by remember { mutableStateOf("") }
    var url by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("添加订阅") },
        text = { Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(name, { name = it }, label = { Text("名称（可选）") }, singleLine = true)
            OutlinedTextField(url, { url = it }, label = { Text("HTTP(S) 订阅地址") }, singleLine = true)
        } },
        confirmButton = { TextButton(onClick = { onSave(name, url) }, enabled = url.isNotBlank()) { Text("添加并刷新") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } },
    )
}

@Composable
private fun NodeDialog(onDismiss: () -> Unit, onSave: (String) -> Unit) {
    var raw by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("导入节点") },
        text = { Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("粘贴 Mihomo/Clash YAML、Base64 订阅、节点链接或 Surge 节点行。")
            OutlinedTextField(raw, { raw = it }, Modifier.fillMaxWidth().height(220.dp), label = { Text("节点内容") }, minLines = 6)
        } },
        confirmButton = { TextButton(onClick = { onSave(raw) }, enabled = raw.isNotBlank()) { Text("解析并添加") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } },
    )
}

@Composable
private fun GroupDialog(state: AppState, initial: PolicyGroup?, onDismiss: () -> Unit, onSave: (String, String, List<String>) -> Unit) {
    var name by remember(initial?.name) { mutableStateOf(initial?.name.orEmpty()) }
    var type by remember(initial?.name) { mutableStateOf(initial?.type ?: "select") }
    var expanded by remember { mutableStateOf(false) }
    val allNodeKeys = state.nodes.map { "node:${it.id}" }.toSet()
    var members by remember(initial?.name, state.nodes) {
        mutableStateOf(initial?.members?.toSet()?.let { if (it.isEmpty()) allNodeKeys else it } ?: allNodeKeys)
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("添加策略组") },
        text = { Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(name, { name = it }, label = { Text("组名称") }, singleLine = true)
            Box {
                OutlinedButton(onClick = { expanded = true }) { Text("类型：$type") }
                DropdownMenu(expanded, { expanded = false }) {
                    listOf("select", "url-test", "fallback", "load-balance").forEach { option -> DropdownMenuItem(text = { Text(option) }, onClick = { type = option; expanded = false }) }
                }
            }
            Text("组成员", style = MaterialTheme.typography.titleSmall)
            Column(Modifier.height(180.dp).verticalScroll(rememberScrollState())) {
                state.nodes.forEach { node ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = "node:${node.id}" in members, onCheckedChange = { checked ->
                            members = if (checked) members + "node:${node.id}" else members - "node:${node.id}"
                        })
                        Text(node.name, style = MaterialTheme.typography.bodySmall)
                    }
                }
                listOf("DIRECT", "REJECT").forEach { special ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = special in members, onCheckedChange = { checked ->
                            members = if (checked) members + special else members - special
                        })
                        Text(special, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        } },
        confirmButton = { TextButton(onClick = { onSave(name, type, members.toList()) }, enabled = name.isNotBlank()) { Text(if (initial == null) "添加" else "保存") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } },
    )
}

@Composable
private fun RuleDialog(state: AppState, onDismiss: () -> Unit, onSave: (String, String, String) -> Unit) {
    var type by remember { mutableStateOf("DOMAIN-SUFFIX") }
    var value by remember { mutableStateOf("") }
    var group by remember { mutableStateOf(state.ruleProfile.groups.firstOrNull()?.name ?: "DIRECT") }
    var typeExpanded by remember { mutableStateOf(false) }
    var groupExpanded by remember { mutableStateOf(false) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("添加路由规则") },
        text = { Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Box {
                OutlinedButton(onClick = { typeExpanded = true }) { Text(type) }
                DropdownMenu(typeExpanded, { typeExpanded = false }) {
                    listOf("DOMAIN-SUFFIX", "DOMAIN", "DOMAIN-KEYWORD", "IP-CIDR", "IP-CIDR6", "PROCESS-NAME").forEach { option -> DropdownMenuItem(text = { Text(option) }, onClick = { type = option; typeExpanded = false }) }
                }
            }
            OutlinedTextField(value, { value = it }, label = { Text("匹配内容") }, singleLine = true)
            Box {
                OutlinedButton(onClick = { groupExpanded = true }) { Text("策略组：$group") }
                DropdownMenu(groupExpanded, { groupExpanded = false }) {
                    (state.ruleProfile.groups.map { it.name } + listOf("DIRECT", "REJECT")).distinct().forEach { option -> DropdownMenuItem(text = { Text(option) }, onClick = { group = option; groupExpanded = false }) }
                }
            }
        } },
        confirmButton = { TextButton(onClick = { onSave(type, value, group) }, enabled = value.isNotBlank()) { Text("添加") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } },
    )
}
