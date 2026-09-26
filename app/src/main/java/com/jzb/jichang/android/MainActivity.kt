package com.jzb.jichang.android

import android.content.Intent
import android.content.ContentValues
import android.content.Context
import android.app.Activity
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
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
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.KeyboardArrowUp
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
import androidx.compose.material3.RadioButton
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
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.foundation.isSystemInDarkTheme
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
import com.jzb.jichang.android.service.ConfigExportOptions
import com.jzb.jichang.android.service.ConfigSourceMode
import com.jzb.jichang.android.service.NodeAutoGroups
import com.jzb.jichang.android.share.LocalShareController
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { JichangApp() }
    }
}

private enum class AppPage(val label: String) { Home("首页"), Sources("订阅"), Nodes("节点"), Rules("规则"), Export("配置") }
private enum class DialogKind { Source, Node, Group, Rule }
private enum class ExportAction { Download, Share }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun JichangApp(viewModel: AppViewModel = viewModel()) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current
    val isDark = isSystemInDarkTheme()
    val localView = LocalView.current
    SideEffect {
        (context as? Activity)?.window?.let { window ->
            WindowCompat.getInsetsController(window, localView).apply {
                isAppearanceLightStatusBars = !isDark
                isAppearanceLightNavigationBars = !isDark
            }
        }
    }
    var page by remember { mutableStateOf(AppPage.Home) }
    var dialog by remember { mutableStateOf<DialogKind?>(null) }
    var editingGroup by remember { mutableStateOf<PolicyGroup?>(null) }
    var editingRule by remember { mutableStateOf<Pair<Int, com.jzb.jichang.android.model.RoutingRule>?>(null) }
    var sourceMode by remember { mutableStateOf(ConfigSourceMode.EMBED_NODES) }
    var excludedNodeIds by remember { mutableStateOf(emptySet<String>()) }
    var regionOverrides by remember { mutableStateOf(emptyMap<String, String>()) }
    var enabledRegions by remember { mutableStateOf(NodeAutoGroups.allKeys) }
    var showExportSetup by remember { mutableStateOf(false) }
    var pendingSensitiveAction by remember { mutableStateOf<ExportAction?>(null) }
    val shareController = remember { LocalShareController(context) }
    val shareUrl by shareController.url.collectAsState()
    val shareError by shareController.error.collectAsState()
    val generator = remember { MihomoConfigGenerator() }
    val exportOptions = remember(sourceMode, excludedNodeIds, regionOverrides, enabledRegions) {
        ConfigExportOptions(sourceMode, excludedNodeIds, regionOverrides, enabledRegions)
    }
    val generated = remember(state, exportOptions) { generator.generate(state, exportOptions) }
    val configText = generated.yaml

    val saveLegacyConfig = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/yaml")) { uri: Uri? ->
        if (uri != null) {
            val content = configText
            viewModel.run {
                withContext(Dispatchers.IO) {
                    checkNotNull(context.contentResolver.openOutputStream(uri)) { "无法打开目标文件" }.use { it.write(content.toByteArray()) }
                }
                "配置已下载"
            }
        }
    }

    DisposableEffect(shareController) { onDispose { shareController.unbind() } }
    LaunchedEffect(configText) { shareController.updateConfig(configText) }

    fun downloadConfig() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            saveLegacyConfig.launch("鸡场.yaml")
        } else viewModel.run {
            withContext(Dispatchers.IO) { saveToDownloads(context, configText) }
            "已下载到 Downloads/鸡场/鸡场.yaml"
        }
    }

    fun requestExport(action: ExportAction) {
        if (sourceMode == ConfigSourceMode.REFERENCE_SUBSCRIPTIONS && state.sources.any { it.enabled && it.providerCompatible == true }) pendingSensitiveAction = action
        else if (action == ExportAction.Download) downloadConfig() else shareController.start(configText)
    }

    JichangTheme(darkTheme = isDark) {
        Scaffold(
            contentWindowInsets = WindowInsets(0),
            containerColor = MaterialTheme.colorScheme.background,
            topBar = {
                TopAppBar(
                    title = {
                        Column {
                            Text(page.label, fontWeight = FontWeight.SemiBold)
                            Text(stringResource(R.string.app_name), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
                            AppPage.Home -> Icons.Outlined.Home
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
                    AppPage.Home -> HomePage(state, onNavigate = { page = it }, modifier = Modifier.weight(1f))
                    AppPage.Sources -> SourcesPage(state, viewModel, { dialog = DialogKind.Source }, Modifier.weight(1f))
                    AppPage.Nodes -> NodesPage(state, viewModel, Modifier.weight(1f)) { dialog = DialogKind.Node }
                    AppPage.Rules -> RulesPage(
                        state, viewModel, Modifier.weight(1f),
                        onEditGroup = { editingGroup = it; dialog = DialogKind.Group },
                        onEditRule = { index, rule -> editingRule = index to rule; dialog = DialogKind.Rule },
                    )
                    AppPage.Export -> ExportPage(
                        state = state,
                        options = exportOptions,
                        generatedNodes = generated.exportedNodes,
                        skippedNodes = generated.skippedNodes,
                        referencedSubscriptions = generated.referencedSubscriptions,
                        configText = configText,
                        shareUrl = shareUrl,
                        shareError = shareError,
                        onModeChange = { sourceMode = it },
                        onOpenFilters = { showExportSetup = true },
                        onDownload = { requestExport(ExportAction.Download) },
                        onShare = { requestExport(ExportAction.Share) },
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
        DialogKind.Rule -> RuleDialog(state, editingRule, onDismiss = { dialog = null; editingRule = null }, onSave = { index, type, value, group, noResolve ->
            if (index == null) viewModel.addRule(type, value, group, noResolve) else viewModel.updateRule(index, type, value, group, noResolve)
            dialog = null; editingRule = null
        })
        null -> Unit
    }

    if (showExportSetup) ExportSetupDialog(
        state = state,
        options = exportOptions,
        excludedNodeIds = excludedNodeIds,
        regionOverrides = regionOverrides,
        onToggleNode = { id -> excludedNodeIds = if (id in excludedNodeIds) excludedNodeIds - id else excludedNodeIds + id },
        onRegionChange = { id, key -> regionOverrides = regionOverrides + (id to key) },
        onToggleRegion = { key -> enabledRegions = if (key in enabledRegions) enabledRegions - key else enabledRegions + key },
        onDismiss = { showExportSetup = false },
    )

    pendingSensitiveAction?.let { action ->
        AlertDialog(
            onDismissRequest = { pendingSensitiveAction = null },
            title = { Text("配置包含机场订阅凭据") },
            text = { Text("引用模式会把已启用订阅地址写进 YAML。拿到文件或局域网分享链接的人可以使用这些订阅。请只分享给可信对象。") },
            confirmButton = { TextButton(onClick = {
                pendingSensitiveAction = null
                if (action == ExportAction.Download) downloadConfig() else shareController.start(configText)
            }) { Text("继续") } },
            dismissButton = { TextButton(onClick = { pendingSensitiveAction = null }) { Text("返回") } },
        )
    }
}

@Composable
private fun HomePage(state: AppState, onNavigate: (AppPage) -> Unit, modifier: Modifier = Modifier) {
    LazyColumn(modifier, contentPadding = androidx.compose.foundation.layout.PaddingValues(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
                Row(Modifier.fillMaxWidth().padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("你的配置，一目了然", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                        Text("订阅、策略与 Mihomo 配置都在本机管理。", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onPrimaryContainer)
                    }
                    Icon(Icons.Outlined.Hub, null, Modifier.size(38.dp), tint = MaterialTheme.colorScheme.primary)
                }
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                MetricCard("订阅", state.sources.count { it.enabled }.toString(), "${state.sources.size} 个来源", Modifier.weight(1f), AppPage.Sources, onNavigate)
                MetricCard("可用节点", state.nodes.count { it.enabled }.toString(), "共 ${state.nodes.size} 个", Modifier.weight(1f), AppPage.Nodes, onNavigate)
                MetricCard("规则", state.ruleProfile.rules.size.toString(), "${state.ruleProfile.groups.size} 个策略组", Modifier.weight(1f), AppPage.Rules, onNavigate)
            }
        }
        item {
            Text("快捷入口", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
        }
        item {
            DashboardAction("订阅管理", "刷新机场来源并在本机解析", Icons.Outlined.CloudDownload) { onNavigate(AppPage.Sources) }
        }
        item {
            DashboardAction("节点筛选", "启用、停用或手动导入节点", Icons.Outlined.Devices) { onNavigate(AppPage.Nodes) }
        }
        item {
            DashboardAction("规则与策略", "编辑分流规则和代理策略组", Icons.Outlined.Tune) { onNavigate(AppPage.Rules) }
        }
        item {
            DashboardAction("生成 Mihomo 配置", "下载 YAML 或在局域网内分享", Icons.Outlined.Description) { onNavigate(AppPage.Export) }
        }
    }
}

@Composable
private fun MetricCard(title: String, value: String, detail: String, modifier: Modifier, destination: AppPage, onNavigate: (AppPage) -> Unit) {
    Card(onClick = { onNavigate(destination) }, modifier = modifier, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(title, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            Text(detail, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun DashboardAction(title: String, detail: String, icon: androidx.compose.ui.graphics.vector.ImageVector, onClick: () -> Unit) {
    Card(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text(detail, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
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
                        Switch(checked = source.enabled, onCheckedChange = { viewModel.toggleSource(source.id) })
                        IconButton(onClick = { viewModel.refreshSource(source.id) }) { Icon(Icons.Outlined.ArrowDownward, "刷新订阅") }
                        IconButton(onClick = { viewModel.removeSource(source.id) }) { Icon(Icons.Outlined.Delete, "删除订阅") }
                    }
                    val count = state.nodes.count { it.sourceId == source.id }
                    Text(if (source.lastError != null) source.lastError else "$count 个节点${source.updatedAt?.let { " · 已更新" } ?: " · 尚未刷新"}", style = MaterialTheme.typography.bodySmall)
                    if (source.enabled) when (source.providerCompatible) {
                        true -> Text("已检测为 Mihomo YAML，可在配置中远程引用。", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary)
                        false -> Text("此订阅不是 Mihomo YAML；引用模式会把已解析节点内嵌。", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary)
                        null -> Text("尚未检测格式；刷新后可判断能否远程引用。", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary)
                    }
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
private fun RulesPage(
    state: AppState,
    viewModel: AppViewModel,
    modifier: Modifier = Modifier,
    onEditGroup: (PolicyGroup) -> Unit,
    onEditRule: (Int, com.jzb.jichang.android.model.RoutingRule) -> Unit,
) {
    LazyColumn(modifier, contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item { HeaderCard("策略组", "选择规则要使用的策略。策略组支持手选、自动测速、故障转移和负载均衡。") }
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
        item {
            Row(Modifier.fillMaxWidth().padding(top = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("路由规则", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                    Text("按从上到下的顺序匹配，兜底规则始终最后执行。", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        if (state.ruleProfile.rules.isEmpty()) item { Text("还没有自定义规则；Mihomo 配置会自动追加 MATCH 兜底。", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
        items(state.ruleProfile.rules.size) { index ->
            val rule = state.ruleProfile.rules[index]
            Card(Modifier.fillMaxWidth()) {
                Row(Modifier.padding(start = 14.dp, end = 4.dp, top = 8.dp, bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(if (rule.type == "MATCH") "兜底规则" else "${rule.type}  ·  ${rule.value}", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                        Text("→ ${rule.group}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                    }
                    IconButton(onClick = { viewModel.moveRule(index, -1) }, enabled = index > 0) { Icon(Icons.Outlined.KeyboardArrowUp, "上移规则") }
                    IconButton(onClick = { viewModel.moveRule(index, 1) }, enabled = index < state.ruleProfile.rules.lastIndex) { Icon(Icons.Outlined.KeyboardArrowDown, "下移规则") }
                    IconButton(onClick = { onEditRule(index, rule) }) { Icon(Icons.Outlined.Edit, "编辑规则") }
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
    options: ConfigExportOptions,
    generatedNodes: Int,
    skippedNodes: Int,
    referencedSubscriptions: Int,
    configText: String,
    shareUrl: String?,
    shareError: String?,
    onModeChange: (ConfigSourceMode) -> Unit,
    onOpenFilters: () -> Unit,
    onDownload: () -> Unit,
    onShare: () -> Unit,
    onStopShare: () -> Unit,
    onShareLink: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Card(Modifier.fillMaxWidth().padding(top = 8.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Mihomo 配置", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text("${generatedNodes} 个内嵌节点${if (referencedSubscriptions > 0) " · $referencedSubscriptions 个订阅引用" else ""}${if (skippedNodes > 0) " · 跳过 $skippedNodes 个不支持节点" else ""}", style = MaterialTheme.typography.bodySmall)
            }
        }
        Text("节点来源", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            SourceModeCard("内嵌节点", "节点写进 YAML", options.sourceMode == ConfigSourceMode.EMBED_NODES, Modifier.weight(1f)) { onModeChange(ConfigSourceMode.EMBED_NODES) }
            SourceModeCard("引用订阅", "Mihomo 远程更新", options.sourceMode == ConfigSourceMode.REFERENCE_SUBSCRIPTIONS, Modifier.weight(1f)) { onModeChange(ConfigSourceMode.REFERENCE_SUBSCRIPTIONS) }
        }
        if (options.sourceMode == ConfigSourceMode.REFERENCE_SUBSCRIPTIONS && state.sources.any { it.enabled }) {
            Text(
                if (referencedSubscriptions > 0) "订阅地址会写入配置文件；文件接收者可以使用这些订阅。" else "当前没有可远程引用的 Mihomo YAML 订阅；节点将以内嵌方式导出。",
                color = if (referencedSubscriptions > 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall,
            )
        }
        OutlinedButton(onClick = onOpenFilters, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Outlined.Tune, null); Spacer(Modifier.width(8.dp)); Text("筛选节点与地区策略组")
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Button(onClick = onDownload, modifier = Modifier.weight(1f)) { Icon(Icons.Outlined.CloudDownload, null); Spacer(Modifier.width(6.dp)); Text("下载配置") }
            OutlinedButton(onClick = onShare, modifier = Modifier.weight(1f)) { Icon(Icons.Outlined.Link, null); Spacer(Modifier.width(6.dp)); Text(if (shareUrl == null) "局域网分享" else "更新分享") }
        }
        if (shareUrl == null) {
            if (shareError != null) Text(shareError, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
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
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("配置预览", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
            Text("YAML", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
        }
        Card(Modifier.weight(1f).fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
            SelectionContainer {
                Text(configText, Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(12.dp), fontFamily = FontFamily.Monospace, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
private fun SourceModeCard(title: String, detail: String, selected: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Card(onClick = onClick, modifier = modifier, colors = CardDefaults.cardColors(containerColor = if (selected) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surface)) {
        Row(Modifier.padding(horizontal = 10.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            RadioButton(selected = selected, onClick = onClick)
            Column(Modifier.padding(start = 4.dp)) {
                Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                Text(detail, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
private fun RuleDialog(
    state: AppState,
    initial: Pair<Int, com.jzb.jichang.android.model.RoutingRule>?,
    onDismiss: () -> Unit,
    onSave: (Int?, String, String, String, Boolean) -> Unit,
) {
    val existing = initial?.second
    var type by remember(initial) { mutableStateOf(existing?.type ?: "DOMAIN-SUFFIX") }
    var value by remember(initial) { mutableStateOf(existing?.value.orEmpty()) }
    var group by remember(initial) { mutableStateOf(existing?.group ?: state.ruleProfile.groups.firstOrNull()?.name ?: "DIRECT") }
    var noResolve by remember(initial) { mutableStateOf(existing?.noResolve ?: false) }
    var typeExpanded by remember { mutableStateOf(false) }
    var groupExpanded by remember { mutableStateOf(false) }
    val isMatch = type == "MATCH"
    val canNoResolve = type in setOf("IP-CIDR", "IP-CIDR6", "GEOIP")
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial == null) "新建规则" else "编辑规则") },
        text = { Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("常用规则", style = MaterialTheme.typography.labelLarge)
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                AssistChip(onClick = { type = "DOMAIN-SUFFIX"; value = "google.com"; group = state.ruleProfile.groups.firstOrNull()?.name ?: "PROXY" }, label = { Text("Google") })
                AssistChip(onClick = { type = "DOMAIN-SUFFIX"; value = "apple.com"; group = "DIRECT" }, label = { Text("Apple 直连") })
                AssistChip(onClick = { type = "GEOIP"; value = "CN"; group = "DIRECT"; noResolve = true }, label = { Text("中国 IP") })
            }
            Box {
                OutlinedButton(onClick = { typeExpanded = true }, modifier = Modifier.fillMaxWidth()) { Text("规则类型：${if (isMatch) "兜底 (MATCH)" else type}") }
                DropdownMenu(expanded = typeExpanded, onDismissRequest = { typeExpanded = false }) {
                    val order = listOf("DOMAIN", "DOMAIN-SUFFIX", "DOMAIN-KEYWORD", "DOMAIN-WILDCARD", "DOMAIN-REGEX", "GEOSITE", "IP-CIDR", "IP-CIDR6", "GEOIP", "DST-PORT", "SRC-PORT", "PROCESS-NAME", "NETWORK", "MATCH")
                    order.forEach { option -> DropdownMenuItem(text = { Text(if (option == "MATCH") "兜底 · MATCH" else option) }, onClick = { type = option; if (option == "MATCH") value = ""; typeExpanded = false }) }
                }
            }
            if (!isMatch) OutlinedTextField(value, { value = it }, modifier = Modifier.fillMaxWidth(), label = { Text("匹配内容") }, placeholder = { Text(rulePlaceholder(type)) }, singleLine = true)
            else Text("此规则会作为配置最后一条 Mihomo MATCH 兜底规则。", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Box {
                OutlinedButton(onClick = { groupExpanded = true }, modifier = Modifier.fillMaxWidth()) { Text("策略：$group") }
                DropdownMenu(expanded = groupExpanded, onDismissRequest = { groupExpanded = false }) {
                    (state.ruleProfile.groups.map { it.name } + listOf("DIRECT", "REJECT")).distinct().forEach { option -> DropdownMenuItem(text = { Text(option) }, onClick = { group = option; groupExpanded = false }) }
                }
            }
            if (canNoResolve) Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(checked = noResolve, onCheckedChange = { noResolve = it })
                Text("no-resolve", style = MaterialTheme.typography.bodyMedium)
            }
        } },
        confirmButton = { TextButton(onClick = { onSave(initial?.first, type, value, group, noResolve) }, enabled = isMatch || value.isNotBlank()) { Text(if (initial == null) "添加规则" else "保存") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } },
    )
}

@Composable
private fun ExportSetupDialog(
    state: AppState,
    options: ConfigExportOptions,
    excludedNodeIds: Set<String>,
    regionOverrides: Map<String, String>,
    onToggleNode: (String) -> Unit,
    onRegionChange: (String, String) -> Unit,
    onToggleRegion: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    val enabledSourceIds = state.sources.filter { it.enabled }.map { it.id }.toSet()
    val nodes = state.nodes.filter { it.enabled && (it.sourceId == null || it.sourceId in enabledSourceIds) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("筛选节点与策略组") },
        text = { Column(Modifier.height(480.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("自动根据节点名称归类。你可以关闭某个地区组、排除节点或手动调整归属。", style = MaterialTheme.typography.bodySmall)
            Text("地区策略组", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            NodeAutoGroups.regions.forEach { region ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = region.key in options.enabledRegions, onCheckedChange = { onToggleRegion(region.key) })
                    Text(region.title)
                    Spacer(Modifier.weight(1f))
                    Text(nodes.count { (regionOverrides[it.id] ?: NodeAutoGroups.classify(it.name)) == region.key }.toString(), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(checked = NodeAutoGroups.OTHER in options.enabledRegions, onCheckedChange = { onToggleRegion(NodeAutoGroups.OTHER) })
                Text("其他")
                Spacer(Modifier.weight(1f))
                Text(nodes.count { (regionOverrides[it.id] ?: NodeAutoGroups.classify(it.name)) == NodeAutoGroups.OTHER }.toString(), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text("包含节点", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 4.dp))
            nodes.forEach { node ->
                var expanded by remember(node.id) { mutableStateOf(false) }
                val selectedRegion = regionOverrides[node.id] ?: NodeAutoGroups.classify(node.name)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = node.id !in excludedNodeIds, onCheckedChange = { onToggleNode(node.id) })
                    Column(Modifier.weight(1f)) {
                        Text(node.name, style = MaterialTheme.typography.bodyMedium)
                        Text("${node.type.uppercase()} · ${node.server}:${node.port}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Box {
                        TextButton(onClick = { expanded = true }) { Text(NodeAutoGroups.title(selectedRegion)) }
                        DropdownMenu(expanded, { expanded = false }) {
                            (NodeAutoGroups.regions.map { it.key } + NodeAutoGroups.OTHER).forEach { key ->
                                DropdownMenuItem(text = { Text(NodeAutoGroups.title(key)) }, onClick = { onRegionChange(node.id, key); expanded = false })
                            }
                        }
                    }
                }
            }
        } },
        confirmButton = { TextButton(onClick = onDismiss) { Text("完成") } },
    )
}

private fun rulePlaceholder(type: String): String = when (type) {
    "DOMAIN", "DOMAIN-SUFFIX" -> "example.com"
    "DOMAIN-KEYWORD" -> "google"
    "IP-CIDR", "IP-CIDR6" -> "192.168.0.0/16"
    "GEOIP" -> "CN"
    "DST-PORT", "SRC-PORT" -> "443"
    "GEOSITE" -> "category-ads-all"
    "PROCESS-NAME" -> "com.example.app"
    "NETWORK" -> "udp"
    else -> "请输入规则值"
}

private fun saveToDownloads(context: Context, content: String) {
    val resolver = context.contentResolver
    val collection = MediaStore.Downloads.EXTERNAL_CONTENT_URI
    val filename = "鸡场.yaml"
    val relativePath = "${Environment.DIRECTORY_DOWNLOADS}/鸡场"
    val existingId = resolver.query(
        collection,
        arrayOf(MediaStore.Downloads._ID),
        "${MediaStore.Downloads.DISPLAY_NAME}=? AND ${MediaStore.Downloads.RELATIVE_PATH}=?",
        arrayOf(filename, relativePath),
        null,
    )?.use { cursor -> if (cursor.moveToFirst()) cursor.getLong(0) else null }
    val uri = existingId?.let { Uri.withAppendedPath(collection, it.toString()) } ?: resolver.insert(
        collection,
        ContentValues().apply {
            put(MediaStore.Downloads.DISPLAY_NAME, filename)
            put(MediaStore.Downloads.MIME_TYPE, "application/yaml")
            put(MediaStore.Downloads.RELATIVE_PATH, relativePath)
            put(MediaStore.Downloads.IS_PENDING, 1)
        },
    ) ?: error("无法创建下载文件")
    try {
        checkNotNull(resolver.openOutputStream(uri, "wt")) { "无法写入下载文件" }.use { it.write(content.toByteArray(Charsets.UTF_8)) }
        resolver.update(uri, ContentValues().apply { put(MediaStore.Downloads.IS_PENDING, 0) }, null, null)
    } catch (error: Throwable) {
        resolver.delete(uri, null, null)
        throw error
    }
}
