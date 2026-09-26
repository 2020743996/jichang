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
import androidx.compose.foundation.background
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
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.jzb.jichang.android.model.ConfigProfile
import com.jzb.jichang.android.model.RuleCondition
import com.jzb.jichang.android.model.RuleProvider
import com.jzb.jichang.android.model.RuleProfile
import com.jzb.jichang.android.model.SubRuleProfile
import com.jzb.jichang.android.model.RoutingRule
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

private enum class AppPage(val label: String) { Home("概览"), Resources("资源"), Config("配置") }
private enum class ResourceTab(val label: String) { Sources("订阅"), Nodes("节点") }
private enum class ConfigTab(val label: String) { Rules("规则"), Export("分享") }
private enum class DialogKind { Source, Node, Group, Rule, Providers, Profile }
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
    var resourceTab by remember { mutableStateOf(ResourceTab.Sources) }
    var configTab by remember { mutableStateOf(ConfigTab.Rules) }
    var dialog by remember { mutableStateOf<DialogKind?>(null) }
    var editingGroup by remember { mutableStateOf<PolicyGroup?>(null) }
    var editingRule by remember { mutableStateOf<Pair<Int, com.jzb.jichang.android.model.RoutingRule>?>(null) }
    var createProfileFromCurrent by remember { mutableStateOf(true) }
    var profileMenu by remember { mutableStateOf(false) }
    var profileActionMenu by remember { mutableStateOf(false) }
    var showProfileDeleteConfirmation by remember { mutableStateOf<String?>(null) }
    var showExportSetup by remember { mutableStateOf(false) }
    var pendingSensitiveAction by remember { mutableStateOf<ExportAction?>(null) }
    val shareController = remember { LocalShareController(context) }
    val shareUrl by shareController.url.collectAsState()
    val shareError by shareController.error.collectAsState()
    val generator = remember { MihomoConfigGenerator() }
    val profile = state.activeProfile
    val exportOptions = remember(profile) {
        ConfigExportOptions(
            sourceMode = runCatching { ConfigSourceMode.valueOf(profile.sourceMode) }.getOrDefault(ConfigSourceMode.EMBED_NODES),
            regionOverrides = profile.regionOverrides,
            enabledRegions = profile.enabledRegions,
            selectedSourceIds = profile.selectedSourceIds,
            enabledNodeIds = profile.enabledNodeIds,
        )
    }
    val generated = remember(state, profile) { generator.generate(state, profile, exportOptions) }
    val configText = generated.yaml
    val filename = remember(profile.fileName) { safeYamlFileName(profile.fileName) }

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
    LaunchedEffect(configText, filename) { shareController.updateConfig(configText, filename) }

    fun downloadConfig() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            saveLegacyConfig.launch(filename)
        } else viewModel.run {
            withContext(Dispatchers.IO) { saveToDownloads(context, configText, filename) }
            "已下载到 Downloads/鸡场/$filename"
        }
    }

    fun requestExport(action: ExportAction) {
        if (profile.sourceMode == ConfigSourceMode.REFERENCE_SUBSCRIPTIONS.name && state.sources.any { it.id in profile.selectedSourceIds && it.providerCompatible == true }) pendingSensitiveAction = action
        else if (action == ExportAction.Download) downloadConfig() else shareController.start(configText, filename)
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
                        Box {
                            TextButton(onClick = { profileMenu = true }) { Text(profile.name, maxLines = 1); Icon(Icons.Outlined.KeyboardArrowDown, null) }
                            DropdownMenu(expanded = profileMenu, onDismissRequest = { profileMenu = false }) {
                                state.profiles.forEach { item -> DropdownMenuItem(
                                    text = { Text(if (item.id == profile.id) "✓ ${item.name}" else item.name) },
                                    onClick = { viewModel.switchProfile(item.id); profileMenu = false },
                                ) }
                                DropdownMenuItem(text = { Text("新建配置…") }, onClick = { createProfileFromCurrent = true; dialog = DialogKind.Profile; profileMenu = false })
                                DropdownMenuItem(text = { Text("管理当前配置…") }, onClick = { createProfileFromCurrent = false; dialog = DialogKind.Profile; profileMenu = false })
                                if (state.profiles.size > 1) DropdownMenuItem(text = { Text("删除当前配置") }, onClick = { showProfileDeleteConfirmation = profile.id; profileMenu = false })
                            }
                        }
                        if (page == AppPage.Resources && resourceTab == ResourceTab.Sources) IconButton(onClick = { dialog = DialogKind.Source }) { Icon(Icons.Outlined.Add, "添加订阅") }
                        if (page == AppPage.Resources && resourceTab == ResourceTab.Nodes) IconButton(onClick = { dialog = DialogKind.Node }) { Icon(Icons.Outlined.Add, "添加节点") }
                        if (page == AppPage.Config && configTab == ConfigTab.Rules) IconButton(onClick = { editingGroup = null; dialog = DialogKind.Group }) { Icon(Icons.Outlined.Add, "添加策略组") }
                    },
                )
            },
            bottomBar = {
                NavigationBar(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f)) {
                    AppPage.entries.forEach { item ->
                        val icon = when (item) {
                            AppPage.Home -> Icons.Outlined.Home
                            AppPage.Resources -> Icons.Outlined.Devices
                            AppPage.Config -> Icons.Outlined.Description
                        }
                        NavigationBarItem(selected = page == item, onClick = { page = item }, icon = { Icon(icon, null) }, label = { Text(item.label) })
                    }
                }
            },
            floatingActionButton = {
                if (page == AppPage.Config && configTab == ConfigTab.Rules) FloatingActionButton(onClick = { editingRule = null; dialog = DialogKind.Rule }) { Icon(Icons.Outlined.Add, "添加规则") }
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
                    AppPage.Resources -> Column(Modifier.weight(1f)) {
                        SegmentedTabs(ResourceTab.entries.map { it.label }, resourceTab.ordinal) { resourceTab = ResourceTab.entries[it] }
                        when (resourceTab) {
                            ResourceTab.Sources -> SourcesPage(state, viewModel, { dialog = DialogKind.Source }, Modifier.weight(1f))
                            ResourceTab.Nodes -> NodesPage(state, viewModel, Modifier.weight(1f)) { dialog = DialogKind.Node }
                        }
                    }
                    AppPage.Config -> Column(Modifier.weight(1f)) {
                        SegmentedTabs(ConfigTab.entries.map { it.label }, configTab.ordinal) { configTab = ConfigTab.entries[it] }
                        when (configTab) {
                            ConfigTab.Rules -> RulesPage(
                                state, viewModel, Modifier.weight(1f),
                                onEditGroup = { editingGroup = it; dialog = DialogKind.Group },
                                onEditRule = { index, rule -> editingRule = index to rule; dialog = DialogKind.Rule },
                                onProviders = { dialog = DialogKind.Providers },
                            )
                            ConfigTab.Export -> ExportPage(
                                state = state, options = exportOptions, generatedNodes = generated.exportedNodes,
                                skippedNodes = generated.skippedNodes, referencedSubscriptions = generated.referencedSubscriptions,
                                configText = configText, shareUrl = shareUrl, shareError = shareError, filename = filename,
                                onModeChange = { viewModel.updateExportSettings(it.name, profile.enabledRegions, profile.regionOverrides) },
                                onOpenFilters = { showExportSetup = true }, onDownload = { requestExport(ExportAction.Download) },
                                onShare = { requestExport(ExportAction.Share) }, onStopShare = { shareController.stop() },
                                onShareLink = {
                                    val intent = Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, shareUrl)
                                    context.startActivity(Intent.createChooser(intent, "分享配置链接"))
                                }, modifier = Modifier.weight(1f),
                            )
                        }
                    }
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
        DialogKind.Rule -> RuleDialog(state, editingRule, onDismiss = { dialog = null; editingRule = null }, onSave = { index, rule ->
            if (index == null) viewModel.addRule(rule) else viewModel.updateRule(index, rule)
            dialog = null; editingRule = null
        })
        DialogKind.Providers -> RuleProvidersDialog(state.ruleProfile, onDismiss = { dialog = null }, onSave = { rules ->
            viewModel.saveRuleProfile(rules); dialog = null
        })
        DialogKind.Profile -> ProfileDialog(profile, createProfileFromCurrent, onDismiss = { dialog = null }, onCreate = { name, fileName, copy ->
            viewModel.createProfile(name, fileName, copy); dialog = null
        }, onSave = { id, name, fileName -> viewModel.updateProfile(id, name, fileName); dialog = null })
        null -> Unit
    }

    if (showExportSetup) ExportSetupDialog(
        state = state,
        options = exportOptions,
        excludedNodeIds = state.nodes.filter { it.id !in profile.enabledNodeIds }.map { it.id }.toSet(),
        regionOverrides = profile.regionOverrides,
        onToggleNode = { id -> viewModel.toggleNode(id) },
        onRegionChange = { id, key -> viewModel.updateExportSettings(profile.sourceMode, profile.enabledRegions, profile.regionOverrides + (id to key)) },
        onToggleRegion = { key -> viewModel.updateExportSettings(profile.sourceMode, if (key in profile.enabledRegions) profile.enabledRegions - key else profile.enabledRegions + key, profile.regionOverrides) },
        onDismiss = { showExportSetup = false },
    )

    pendingSensitiveAction?.let { action ->
        AlertDialog(
            onDismissRequest = { pendingSensitiveAction = null },
            title = { Text("配置包含机场订阅凭据") },
            text = { Text("引用模式会把已启用订阅地址写进 YAML。拿到文件或局域网分享链接的人可以使用这些订阅。请只分享给可信对象。") },
            confirmButton = { TextButton(onClick = {
                pendingSensitiveAction = null
                if (action == ExportAction.Download) downloadConfig() else shareController.start(configText, filename)
            }) { Text("继续") } },
            dismissButton = { TextButton(onClick = { pendingSensitiveAction = null }) { Text("返回") } },
        )
    }

    showProfileDeleteConfirmation?.let { targetId ->
        AlertDialog(onDismissRequest = { showProfileDeleteConfirmation = null }, title = { Text("删除配置？") },
            text = { Text("只会删除此配置的规则和选择，不会删除共享订阅或节点。") },
            confirmButton = { TextButton(onClick = { viewModel.deleteProfile(targetId); showProfileDeleteConfirmation = null }) { Text("删除") } },
            dismissButton = { TextButton(onClick = { showProfileDeleteConfirmation = null }) { Text("取消") } })
    }
}

@Composable
private fun HomePage(state: AppState, onNavigate: (AppPage) -> Unit, modifier: Modifier = Modifier) {
    val profile = state.activeProfile
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
                MetricCard("订阅", state.sources.count { it.id in profile.selectedSourceIds }.toString(), "${state.sources.size} 个来源", Modifier.weight(1f), AppPage.Resources, onNavigate)
                MetricCard("节点", profile.enabledNodeIds.size.toString(), "共 ${state.nodes.size} 个", Modifier.weight(1f), AppPage.Resources, onNavigate)
                MetricCard("规则", profile.ruleProfile.rules.size.toString(), "${profile.ruleProfile.groups.size} 个策略组", Modifier.weight(1f), AppPage.Config, onNavigate)
            }
        }
        item {
            Text("快捷入口", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
        }
        item {
            DashboardAction("订阅与节点", "管理共享资源，按配置独立选择", Icons.Outlined.CloudDownload) { onNavigate(AppPage.Resources) }
        }
        item {
            DashboardAction("规则与策略", "可视化编辑 Mihomo 分流配置", Icons.Outlined.Tune) { onNavigate(AppPage.Config) }
        }
        item {
            DashboardAction("导出 ${profile.name}", "${safeYamlFileName(profile.fileName)} · 下载或局域网分享", Icons.Outlined.Description) { onNavigate(AppPage.Config) }
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
    val profile = state.activeProfile
    LazyColumn(modifier, contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("订阅资源", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text("当前配置启用 ${profile.selectedSourceIds.size} / ${state.sources.size}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                OutlinedButton(onClick = { viewModel.refreshAllSources() }, enabled = state.sources.isNotEmpty() && viewModel.refreshingSourceIds.isEmpty()) {
                    if (viewModel.refreshingSourceIds.isNotEmpty()) CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp)
                    else Icon(Icons.Outlined.ArrowDownward, null)
                    Spacer(Modifier.width(6.dp)); Text("全部刷新")
                }
            }
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
                        Switch(checked = source.id in profile.selectedSourceIds, onCheckedChange = { viewModel.toggleSource(source.id) })
                        IconButton(onClick = { viewModel.refreshSource(source.id) }, enabled = source.id !in viewModel.refreshingSourceIds) {
                            if (source.id in viewModel.refreshingSourceIds) CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                            else Icon(Icons.Outlined.ArrowDownward, "刷新订阅")
                        }
                        IconButton(onClick = { viewModel.removeSource(source.id) }) { Icon(Icons.Outlined.Delete, "删除订阅") }
                    }
                    val count = state.nodes.count { it.sourceId == source.id }
                    Text(if (source.lastError != null) source.lastError else "$count 个节点${source.updatedAt?.let { " · 已更新" } ?: " · 尚未刷新"}", style = MaterialTheme.typography.bodySmall)
                    if (source.id in profile.selectedSourceIds) when (source.providerCompatible) {
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
    var protocolFilter by remember { mutableStateOf("全部协议") }
    var sourceFilter by remember { mutableStateOf("全部来源") }
    var enabledFilter by remember { mutableStateOf("全部") }
    var expanded by remember { mutableStateOf(false) }
    val profile = state.activeProfile
    val filtered = state.nodes.filter { node ->
            (filter.isBlank() || node.name.contains(filter, true) || node.server.contains(filter, true)) &&
            (protocolFilter == "全部协议" || node.type.equals(protocolFilter, true)) &&
            (sourceFilter == "全部来源" || (sourceFilter == "手动导入" && node.sourceId == null) || state.sources.firstOrNull { it.id == node.sourceId }?.name == sourceFilter) &&
            (enabledFilter == "全部" || (node.id in profile.enabledNodeIds) == (enabledFilter == "已启用"))
    }
    val protocols = listOf("全部协议") + state.nodes.map { it.type.uppercase() }.distinct().sorted()
    var editingNode by remember { mutableStateOf<ProxyNode?>(null) }
    Column(modifier.padding(horizontal = 16.dp)) {
        OutlinedTextField(filter, { filter = it }, Modifier.fillMaxWidth().padding(top = 6.dp), label = { Text("搜索节点名称或服务器") }, singleLine = true)
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Box {
                OutlinedButton(onClick = { expanded = true }) { Text(protocolFilter); Icon(Icons.Outlined.KeyboardArrowDown, null) }
                DropdownMenu(expanded, { expanded = false }) {
                    protocols.forEach { protocol ->
                        DropdownMenuItem(text = { Text(protocol) }, onClick = { protocolFilter = protocol; expanded = false })
                    }
                }
            }
            DropdownMenuFilter(enabledFilter, { enabledFilter = it })
            Spacer(Modifier.weight(1f))
            TextButton(onClick = { viewModel.setNodesEnabled(filtered.map { it.id }.toSet(), true) }, enabled = filtered.isNotEmpty()) { Text("全开") }
            TextButton(onClick = { viewModel.setNodesEnabled(filtered.map { it.id }.toSet(), false) }, enabled = filtered.isNotEmpty()) { Text("全关") }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ChoiceMenu("来源：$sourceFilter", listOf("全部来源", "手动导入") + state.sources.map { it.name }, { sourceFilter = it }, Modifier.weight(1f))
            Text("${filtered.size} 个结果", Modifier.align(Alignment.CenterVertically), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        if (state.nodes.isEmpty()) EmptyCard("还没有节点", "导入 Mihomo YAML、Base64 订阅或节点链接。", onAdd)
        else if (filtered.isEmpty()) EmptyCard("没有匹配的节点", "换个搜索词或清除筛选条件。", onAdd)
        else LazyColumn(Modifier.weight(1f), contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(filtered, key = { it.id }) { node -> NodeRow(
                node, node.id in profile.enabledNodeIds,
                onToggle = { viewModel.toggleNode(node.id) }, onDelete = { viewModel.removeNode(node.id) }, onEdit = { editingNode = node },
            ) }
        }
        Text("此配置启用 ${filtered.count { it.id in profile.enabledNodeIds }} / ${filtered.size} · 共 ${state.nodes.size} 个节点", Modifier.padding(bottom = 8.dp), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
    editingNode?.let { node -> NodeEditDialog(node, onDismiss = { editingNode = null }, onSave = { name, type, server, port, options ->
        viewModel.updateNode(node.id, name, type, server, port, options); editingNode = null
    }) }
}

@Composable
private fun DropdownMenuFilter(value: String, onValue: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        OutlinedButton(onClick = { expanded = true }) { Text(value); Icon(Icons.Outlined.KeyboardArrowDown, null) }
        DropdownMenu(expanded, { expanded = false }) { listOf("全部", "已启用", "已停用").forEach { option -> DropdownMenuItem(text = { Text(option) }, onClick = { onValue(option); expanded = false }) } }
    }
}

@Composable
private fun ChoiceMenu(label: String, options: List<String>, onValue: (String) -> Unit, modifier: Modifier = Modifier) {
    var expanded by remember { mutableStateOf(false) }
    Box(modifier) {
        OutlinedButton(onClick = { expanded = true }, modifier = Modifier.fillMaxWidth()) { Text(label, maxLines = 1); Icon(Icons.Outlined.KeyboardArrowDown, null) }
        DropdownMenu(expanded, { expanded = false }) { options.distinct().forEach { option -> DropdownMenuItem(text = { Text(option) }, onClick = { onValue(option); expanded = false }) } }
    }
}

@Composable
private fun NodeRow(node: ProxyNode, enabled: Boolean, onToggle: () -> Unit, onDelete: () -> Unit, onEdit: () -> Unit) {
    Card(Modifier.fillMaxWidth()) {
        Row(Modifier.padding(start = 14.dp, end = 6.dp, top = 8.dp, bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(node.name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Medium)
                Text("${node.type.uppercase()} · ${node.server}:${node.port}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Switch(checked = enabled, onCheckedChange = { onToggle() })
            IconButton(onClick = onEdit) { Icon(Icons.Outlined.Edit, "编辑节点") }
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
    onProviders: () -> Unit,
) {
    LazyColumn(modifier, contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item { HeaderCard("策略组", "选择规则要使用的策略。策略组支持手选、自动测速、故障转移和负载均衡。") }
        item {
            Card(onClick = onProviders, modifier = Modifier.fillMaxWidth()) {
                Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.Description, null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) {
                        Text("规则集", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                        Text("${state.ruleProfile.providers.size} 个提供者 · 支持远程、文件与内嵌", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Icon(Icons.Outlined.Edit, null)
                }
            }
        }
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
    filename: String,
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
    val profile = state.activeProfile
    Column(modifier.padding(horizontal = 16.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Card(Modifier.fillMaxWidth().padding(top = 6.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(profile.name, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text("${generatedNodes} 个内嵌节点${if (referencedSubscriptions > 0) " · $referencedSubscriptions 个订阅引用" else ""}${if (skippedNodes > 0) " · 跳过 $skippedNodes 个不支持节点" else ""}", style = MaterialTheme.typography.bodySmall)
                Text("文件名：$filename", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onPrimaryContainer)
            }
        }
        Text("节点来源", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            SourceModeCard("内嵌节点", "节点写进 YAML", options.sourceMode == ConfigSourceMode.EMBED_NODES, Modifier.weight(1f)) { onModeChange(ConfigSourceMode.EMBED_NODES) }
            SourceModeCard("引用订阅", "Mihomo 远程更新", options.sourceMode == ConfigSourceMode.REFERENCE_SUBSCRIPTIONS, Modifier.weight(1f)) { onModeChange(ConfigSourceMode.REFERENCE_SUBSCRIPTIONS) }
        }
        if (options.sourceMode == ConfigSourceMode.REFERENCE_SUBSCRIPTIONS && state.sources.any { it.id in profile.selectedSourceIds }) {
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
        Card(Modifier.fillMaxWidth().height(320.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
            SelectionContainer {
                Text(configText, Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(12.dp), fontFamily = FontFamily.Monospace, style = MaterialTheme.typography.bodySmall)
            }
        }
        Spacer(Modifier.height(16.dp))
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
private fun SegmentedTabs(labels: List<String>, selected: Int, onSelect: (Int) -> Unit) {
    Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp).background(MaterialTheme.colorScheme.surface.copy(alpha = 0.72f), RoundedCornerShape(18.dp)).padding(4.dp)) {
        labels.forEachIndexed { index, label ->
            TextButton(onClick = { onSelect(index) }, modifier = Modifier.weight(1f), colors = androidx.compose.material3.ButtonDefaults.textButtonColors(
                containerColor = if (selected == index) MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.92f) else Color.Transparent,
            )) { Text(label, fontWeight = if (selected == index) FontWeight.SemiBold else FontWeight.Normal) }
        }
    }
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

private val visualRuleTypes = listOf(
    "DOMAIN", "DOMAIN-SUFFIX", "DOMAIN-KEYWORD", "DOMAIN-WILDCARD", "DOMAIN-REGEX", "GEOSITE",
    "IP-CIDR", "IP-CIDR6", "IP-SUFFIX", "IP-ASN", "GEOIP", "SRC-GEOIP", "SRC-IP-ASN", "SRC-IP-CIDR", "SRC-IP-SUFFIX",
    "DST-PORT", "SRC-PORT", "IN-PORT", "IN-TYPE", "IN-USER", "IN-NAME", "REMATCH-NAME",
    "PROCESS-PATH", "PROCESS-PATH-WILDCARD", "PROCESS-PATH-REGEX", "PROCESS-NAME", "PROCESS-NAME-WILDCARD", "PROCESS-NAME-REGEX",
    "UID", "NETWORK", "DSCP", "RULE-SET", "SUB-RULE", "AND", "OR", "NOT", "MATCH",
)

@Composable
private fun RuleDialog(state: AppState, initial: Pair<Int, RoutingRule>?, onDismiss: () -> Unit, onSave: (Int?, RoutingRule) -> Unit) {
    val existing = initial?.second
    var type by remember(initial) { mutableStateOf(existing?.type ?: "DOMAIN-SUFFIX") }
    var value by remember(initial) { mutableStateOf(existing?.value.orEmpty()) }
    var group by remember(initial) { mutableStateOf(existing?.group ?: state.ruleProfile.groups.firstOrNull()?.name ?: "DIRECT") }
    var noResolve by remember(initial) { mutableStateOf(existing?.noResolve ?: false) }
    var source by remember(initial) { mutableStateOf(existing?.source ?: false) }
    var conditions by remember(initial) { mutableStateOf(existing?.conditions ?: emptyList()) }
    var menu by remember { mutableStateOf(false) }
    var strategyMenu by remember { mutableStateOf(false) }
    val composite = type in setOf("AND", "OR", "NOT")
    val valueRequired = type !in setOf("MATCH", "AND", "OR", "NOT")
    val valid = (!valueRequired || value.isNotBlank()) && (!composite || conditions.size >= if (type == "NOT") 1 else 2)
    AlertDialog(onDismissRequest = onDismiss, title = { Text(if (initial == null) "新建路由规则" else "编辑路由规则") },
        text = { Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(9.dp)) {
            Text("规则类型", style = MaterialTheme.typography.labelLarge)
            Box {
                OutlinedButton(onClick = { menu = true }, modifier = Modifier.fillMaxWidth()) { Text(if (type == "MATCH") "MATCH · 最终兜底" else type) }
                DropdownMenu(menu, { menu = false }) {
                    visualRuleTypes.forEach { option -> DropdownMenuItem(text = { Text(option) }, onClick = {
                        type = option
                        if (option == "MATCH") value = ""
                        if (option == "RULE-SET" && value.isBlank()) value = state.ruleProfile.providers.firstOrNull()?.name.orEmpty()
                        if (option == "SUB-RULE" && value.isBlank()) value = state.ruleProfile.subRules.firstOrNull()?.name.orEmpty()
                        menu = false
                    }) }
                }
            }
            when {
                type == "MATCH" -> Text("MATCH 会被固定在规则列表末尾。", style = MaterialTheme.typography.bodySmall)
                composite -> {
                    Text("条件 · ${if (type == "NOT") "只需 1 项" else "至少 2 项"}", style = MaterialTheme.typography.titleSmall)
                    conditions.forEachIndexed { index, condition ->
                        ConditionEditor(condition, state.ruleProfile.providers.map { it.name }, onChange = { changed -> conditions = conditions.toMutableList().also { it[index] = changed } }, onDelete = { conditions = conditions.filterIndexed { i, _ -> i != index } })
                    }
                    OutlinedButton(onClick = { conditions = conditions + RuleCondition(type = "DOMAIN-SUFFIX", value = "example.com") }, modifier = Modifier.fillMaxWidth()) { Text("添加子条件") }
                }
                type == "RULE-SET" -> {
                    SelectField("规则集", value, state.ruleProfile.providers.map { it.name }, { value = it })
                    if (state.ruleProfile.providers.isEmpty()) Text("请先在“规则集”中添加提供者。", color = MaterialTheme.colorScheme.error)
                }
                type == "SUB-RULE" -> {
                    SelectField("子规则", value, state.ruleProfile.subRules.map { it.name }, { value = it })
                    if (state.ruleProfile.subRules.isEmpty()) Text("请先在规则集页添加子规则。", color = MaterialTheme.colorScheme.error)
                }
                type == "GEOSITE" -> {
                    val common = listOf("category-ads-all", "cn", "private", "google", "youtube", "apple", "microsoft", "telegram", "github", "geolocation-!cn")
                    OutlinedTextField(value, { value = it }, modifier = Modifier.fillMaxWidth(), label = { Text("GEOSITE 分类") }, placeholder = { Text("category-ads-all 或自定义值") }, singleLine = true)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) { common.take(4).forEach { tag -> AssistChip(onClick = { value = tag }, label = { Text(tag) }) } }
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) { common.drop(4).forEach { tag -> AssistChip(onClick = { value = tag }, label = { Text(tag) }) } }
                }
                else -> OutlinedTextField(value, { value = it }, modifier = Modifier.fillMaxWidth(), label = { Text(if (type.startsWith("PROCESS") || type == "UID") "进程 / 用户匹配" else "匹配内容") }, placeholder = { Text(rulePlaceholder(type)) }, singleLine = true)
            }
            Box {
                OutlinedButton(onClick = { strategyMenu = true }, modifier = Modifier.fillMaxWidth()) { Text("策略目标：$group") }
                DropdownMenu(strategyMenu, { strategyMenu = false }) {
                    (state.ruleProfile.groups.map { it.name } + listOf("DIRECT", "REJECT")).distinct().forEach { item -> DropdownMenuItem(text = { Text(item) }, onClick = { group = item; strategyMenu = false }) }
                }
            }
            if (type in setOf("IP-CIDR", "IP-CIDR6", "IP-SUFFIX", "IP-ASN", "GEOIP", "SRC-GEOIP", "SRC-IP-ASN", "SRC-IP-CIDR", "SRC-IP-SUFFIX")) {
                Row(verticalAlignment = Alignment.CenterVertically) { Checkbox(noResolve, { noResolve = it }); Text("no-resolve") }
                if (type in setOf("IP-CIDR", "IP-CIDR6", "IP-SUFFIX", "IP-ASN", "GEOIP")) Row(verticalAlignment = Alignment.CenterVertically) { Checkbox(source, { source = it }); Text("按来源地址匹配 (src)") }
            }
        } },
        confirmButton = { TextButton(enabled = valid, onClick = { onSave(initial?.first, RoutingRule(type, value, group, noResolve, source, conditions)) }) { Text(if (initial == null) "添加规则" else "保存") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } })
}

@Composable
private fun SelectField(label: String, value: String, items: List<String>, onSelect: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Box { OutlinedButton(onClick = { expanded = true }, modifier = Modifier.fillMaxWidth()) { Text("$label：${value.ifBlank { "请选择" }}") }
        DropdownMenu(expanded, { expanded = false }) { items.forEach { DropdownMenuItem(text = { Text(it) }, onClick = { onSelect(it); expanded = false }) } } }
}

@Composable
private fun ConditionEditor(condition: RuleCondition, providerNames: List<String>, onChange: (RuleCondition) -> Unit, onDelete: () -> Unit) {
    var typeMenu by remember { mutableStateOf(false) }
    var opMenu by remember { mutableStateOf(false) }
    val isLogic = condition.operator != null
    Card(Modifier.fillMaxWidth()) { Column(Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.weight(1f)) {
                TextButton(onClick = { if (isLogic) opMenu = true else typeMenu = true }) { Text(condition.operator ?: condition.type ?: "类型") }
                DropdownMenu(typeMenu, { typeMenu = false }) {
                    visualRuleTypes.filterNot { it in setOf("MATCH", "SUB-RULE") }.forEach { option -> DropdownMenuItem(text = { Text(option) }, onClick = {
                        if (option in setOf("AND", "OR", "NOT")) onChange(condition.copy(operator = option, type = null, children = if (option == "NOT") listOf(RuleCondition(type = "DOMAIN", value = "example.com")) else listOf(RuleCondition(type = "DOMAIN", value = "example.com"), RuleCondition(type = "NETWORK", value = "udp"))))
                        else onChange(condition.copy(type = option, operator = null))
                        typeMenu = false
                    }) }
                }
                DropdownMenu(opMenu, { opMenu = false }) { listOf("AND", "OR", "NOT").forEach { DropdownMenuItem(text = { Text(it) }, onClick = { onChange(condition.copy(operator = it, type = null)); opMenu = false }) } }
            }
            IconButton(onClick = onDelete) { Icon(Icons.Outlined.Delete, "移除条件") }
        }
        if (isLogic) {
            condition.children.forEachIndexed { index, child -> ConditionEditor(child, providerNames, { updated -> onChange(condition.copy(children = condition.children.toMutableList().also { it[index] = updated })) }, { onChange(condition.copy(children = condition.children.filterIndexed { i, _ -> i != index })) }) }
            TextButton(onClick = { onChange(condition.copy(children = condition.children + RuleCondition(type = "DOMAIN", value = "example.com"))) }) { Text("添加嵌套条件") }
        } else if (condition.type == "RULE-SET") SelectField("规则集", condition.value, providerNames) { onChange(condition.copy(value = it)) }
        else OutlinedTextField(condition.value, { onChange(condition.copy(value = it)) }, modifier = Modifier.fillMaxWidth(), label = { Text("匹配内容") }, singleLine = true)
    } }
}

@Composable
private fun RuleProvidersDialog(initial: RuleProfile, onDismiss: () -> Unit, onSave: (RuleProfile) -> Unit) {
    var profile by remember(initial) { mutableStateOf(initial) }
    val providers = profile.providers
    var editing by remember { mutableStateOf<RuleProvider?>(null) }
    var editingSubRule by remember { mutableStateOf<Pair<String, Int?>?>(null) }
    var subName by remember { mutableStateOf("") }
    if (editing != null) {
        RuleProviderEditor(editing!!, onDismiss = { editing = null }, onSave = { item ->
            val old = providers.indexOfFirst { it.id == editing?.id }
            val updated = if (old < 0) providers + item else providers.toMutableList().also { it[old] = item }
            profile = profile.copy(providers = updated)
            editing = null
        })
    } else if (editingSubRule != null) {
        val (subNameKey, index) = editingSubRule!!
        val subRule = profile.subRules.first { it.name == subNameKey }
        val nestedState = AppState(profiles = listOf(ConfigProfile("nested", "nested", ruleProfile = profile)))
        RuleDialog(nestedState, index?.let { it to subRule.rules[it] }, onDismiss = { editingSubRule = null }, onSave = { position, rule ->
            val updatedRules = subRule.rules.toMutableList()
            if (position == null) updatedRules += rule else updatedRules[position] = rule
            profile = profile.copy(subRules = profile.subRules.map { if (it.name == subNameKey) it.copy(rules = updatedRules) else it })
            editingSubRule = null
        })
    } else AlertDialog(onDismissRequest = onDismiss, title = { Text("规则集与子规则") },
        text = { Column(Modifier.height(440.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("规则集提供者", style = MaterialTheme.typography.titleSmall)
            providers.forEach { provider -> Card(onClick = { editing = provider }, modifier = Modifier.fillMaxWidth()) { Row(Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) { Column(Modifier.weight(1f)) { Text(provider.name); Text("${provider.type} · ${provider.behavior}", style = MaterialTheme.typography.labelSmall) }; IconButton(onClick = { profile = profile.copy(providers = providers - provider) }) { Icon(Icons.Outlined.Delete, null) } } } }
            OutlinedButton(onClick = { editing = RuleProvider(id = java.util.UUID.randomUUID().toString(), name = "新规则集") }, modifier = Modifier.fillMaxWidth()) { Text("添加规则集提供者") }
            Text("本地子规则", style = MaterialTheme.typography.titleSmall)
            profile.subRules.forEach { rule ->
                Card(Modifier.fillMaxWidth()) { Column(Modifier.padding(horizontal = 8.dp, vertical = 4.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) { Text(rule.name); Text("${rule.rules.size} 条规则", style = MaterialTheme.typography.labelSmall) }
                        TextButton(onClick = { editingSubRule = rule.name to null }) { Text("添加规则") }
                        IconButton(onClick = { profile = profile.copy(subRules = profile.subRules - rule) }) { Icon(Icons.Outlined.Delete, "删除子规则") }
                    }
                    rule.rules.forEachIndexed { index, item -> TextButton(onClick = { editingSubRule = rule.name to index }) { Text("${item.type} · ${item.value} → ${item.group}") } }
                } }
            }
            Row(verticalAlignment = Alignment.CenterVertically) { OutlinedTextField(subName, { subName = it }, Modifier.weight(1f), label = { Text("新子规则名称") }, singleLine = true); TextButton(enabled = subName.isNotBlank() && profile.subRules.none { it.name == subName.trim() }, onClick = { profile = profile.copy(subRules = profile.subRules + SubRuleProfile(subName.trim())); subName = "" }) { Text("添加") } }
            Text("规则集文件、子规则由接收端 Mihomo 根据此配置读取。", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        } },
        confirmButton = { TextButton(onClick = { onSave(profile) }) { Text("保存") } }, dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } })
}

@Composable
private fun RuleProviderEditor(initial: RuleProvider, onDismiss: () -> Unit, onSave: (RuleProvider) -> Unit) {
    var name by remember(initial.id) { mutableStateOf(initial.name) }; var type by remember(initial.id) { mutableStateOf(initial.type) }
    var behavior by remember(initial.id) { mutableStateOf(initial.behavior) }; var url by remember(initial.id) { mutableStateOf(initial.url) }
    var path by remember(initial.id) { mutableStateOf(initial.path) }; var format by remember(initial.id) { mutableStateOf(initial.format) }
    var interval by remember(initial.id) { mutableStateOf(initial.interval.toString()) }
    var payload by remember(initial.id) { mutableStateOf(initial.payload.joinToString("\n")) }
    var headers by remember(initial.id) { mutableStateOf(initial.headers.entries.joinToString("\n") { (key, values) -> key + "=" + values.joinToString(",") }) }
    AlertDialog(onDismissRequest = onDismiss, title = { Text("规则集提供者") }, text = { Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(name, { name = it }, label = { Text("名称") }, singleLine = true)
        SelectField("来源类型", type, listOf("http", "file", "inline")) { type = it }
        SelectField("匹配行为", behavior, listOf("domain", "ipcidr", "classical")) { behavior = it }
        if (type == "http") OutlinedTextField(url, { url = it }, label = { Text("下载 URL") }, singleLine = true)
        if (type == "http") OutlinedTextField(interval, { interval = it.filter(Char::isDigit) }, label = { Text("更新间隔（秒）") }, singleLine = true)
        OutlinedTextField(path, { path = it }, label = { Text("本地缓存/文件路径") }, singleLine = true)
        SelectField("格式", format, listOf("yaml", "text", "mrs")) { format = it }
        if (type == "http") OutlinedTextField(headers, { headers = it }, label = { Text("请求头，每行 key=value") }, minLines = 2)
        if (type == "inline") OutlinedTextField(payload, { payload = it }, label = { Text("规则项，每行一条") }, minLines = 4)
    } }, confirmButton = { TextButton(enabled = name.isNotBlank() && (type != "http" || url.startsWith("http")) && (type != "inline" || payload.isNotBlank()), onClick = {
        val parsedHeaders = headers.lines().mapNotNull { line -> line.split("=", limit = 2).takeIf { it.size == 2 }?.let { it[0].trim() to it[1].split(",").map(String::trim) } }.toMap()
        onSave(initial.copy(name = name.trim(), type = type, behavior = behavior, url = url, path = path, format = format, interval = interval.toIntOrNull() ?: 86400, payload = payload.lines().filter(String::isNotBlank), headers = parsedHeaders))
    }) { Text("保存") } }, dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } })
}

@Composable
private fun ProfileDialog(profile: ConfigProfile, creating: Boolean, onDismiss: () -> Unit, onCreate: (String, String, Boolean) -> Unit, onSave: (String, String, String) -> Unit) {
    var name by remember(profile.id, creating) { mutableStateOf(if (creating) "" else profile.name) }
    var fileName by remember(profile.id, creating) { mutableStateOf(if (creating) "" else profile.fileName) }
    var copy by remember { mutableStateOf(true) }
    AlertDialog(onDismissRequest = onDismiss, title = { Text(if (creating) "新建配置" else "配置设置") }, text = { Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(name, { name = it; if (creating && fileName.isBlank()) fileName = it }, label = { Text("配置名称") }, singleLine = true)
        OutlinedTextField(fileName, { fileName = it }, label = { Text("导出文件名（无需 .yaml）") }, singleLine = true)
        if (creating) Row(verticalAlignment = Alignment.CenterVertically) { Checkbox(copy, { copy = it }); Text("复制当前规则和选择") }
    } }, confirmButton = { TextButton(enabled = name.isNotBlank() && fileName.isNotBlank(), onClick = { if (creating) onCreate(name.trim(), fileName.trim(), copy) else onSave(profile.id, name.trim(), fileName.trim()) }) { Text("保存") } }, dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } })
}

@Composable
private fun NodeEditDialog(node: ProxyNode, onDismiss: () -> Unit, onSave: (String, String, String, Int, Map<String, Any?>) -> Unit) {
    var name by remember(node.id) { mutableStateOf(node.name) }; var type by remember(node.id) { mutableStateOf(node.type) }
    var server by remember(node.id) { mutableStateOf(node.server) }; var port by remember(node.id) { mutableStateOf(node.port.toString()) }
    var extra by remember(node.id) { mutableStateOf(node.options.filterKeys { it !in setOf("name", "type", "server", "port") }.entries.joinToString("\n") { "${it.key}=${it.value}" }) }
    AlertDialog(onDismissRequest = onDismiss, title = { Text("编辑节点") }, text = { Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(7.dp)) {
        OutlinedTextField(name, { name = it }, label = { Text("名称") }, singleLine = true)
        OutlinedTextField(type, { type = it }, label = { Text("协议类型") }, singleLine = true)
        OutlinedTextField(server, { server = it }, label = { Text("服务器") }, singleLine = true)
        OutlinedTextField(port, { port = it.filter(Char::isDigit) }, label = { Text("端口") }, singleLine = true)
        OutlinedTextField(extra, { extra = it }, label = { Text("其他选项，每行 key=value") }, minLines = 4)
    } }, confirmButton = { TextButton(enabled = name.isNotBlank() && server.isNotBlank() && port.toIntOrNull() in 1..65535, onClick = { val options = extra.lines().mapNotNull { line -> line.split("=", limit = 2).takeIf { it.size == 2 }?.let { it[0].trim() to it[1].trim() } }.toMap(); onSave(name, type, server, port.toInt(), options) }) { Text("保存") } }, dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } })
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
    val enabledSourceIds = state.activeProfile.selectedSourceIds
    val nodes = state.nodes.filter { it.sourceId == null || it.sourceId in enabledSourceIds }
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

private fun safeYamlFileName(value: String): String {
    val clean = value.trim().replace("[\\\\/:*?\"<>|\\p{Cntrl}]".toRegex(), "_").trim('.', ' ')
        .removeSuffix(".yaml").removeSuffix(".yml").ifBlank { "鸡场" }
    return "$clean.yaml"
}

private fun saveToDownloads(context: Context, content: String, filename: String) {
    val resolver = context.contentResolver
    val collection = MediaStore.Downloads.EXTERNAL_CONTENT_URI
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
