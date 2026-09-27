package com.jzb.jichang.android

import android.content.Intent
import android.content.ContentValues
import android.content.Context
import android.app.Activity
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import android.provider.MediaStore
import androidx.activity.ComponentActivity
import androidx.activity.enableEdgeToEdge
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.material.icons.outlined.DragHandle
import androidx.compose.material.icons.outlined.Devices
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.FolderOpen
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
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.scaleIn
import androidx.compose.animation.togetherWith
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.Image
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material3.Slider
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
import com.jzb.jichang.android.service.MihomoTemplateParser
import com.jzb.jichang.android.service.TemplateSubscriptionParameter
import com.jzb.jichang.android.service.ConfigExportOptions
import com.jzb.jichang.android.service.ConfigSourceMode
import com.jzb.jichang.android.service.NodeAutoGroups
import com.jzb.jichang.android.service.RuleDiagnostics
import com.jzb.jichang.android.service.RemoteConfigDownloader
import com.jzb.jichang.android.service.LanShareQrCode
import com.jzb.jichang.android.share.LocalShareController
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        window.isStatusBarContrastEnforced = false
        window.isNavigationBarContrastEnforced = false
        setContent { JichangApp() }
    }
}

private enum class AppPage(val label: String) { Home("概览"), Share("分享"), Resources("资源") }
private enum class ResourceTab(val label: String) { Sources("订阅"), Nodes("节点"), Templates("模板") }
private enum class ConfigTab(val label: String) { Rules("规则"), Export("分享") }
private enum class DialogKind { Source, Node, Group, Rule, Providers, Profile }
private enum class ExportAction { Download, Share }

@Composable
private fun JichangAlertDialog(
    onDismissRequest: () -> Unit,
    confirmButton: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    dismissButton: (@Composable () -> Unit)? = null,
    icon: (@Composable () -> Unit)? = null,
    title: (@Composable () -> Unit)? = null,
    text: (@Composable () -> Unit)? = null,
    properties: DialogProperties = DialogProperties(),
) {
    AlertDialog(
        onDismissRequest = onDismissRequest,
        confirmButton = confirmButton,
        modifier = modifier,
        dismissButton = dismissButton,
        icon = icon,
        title = title,
        text = text,
        shape = MaterialTheme.shapes.large,
        containerColor = MaterialTheme.colorScheme.surface,
        iconContentColor = MaterialTheme.colorScheme.primary,
        titleContentColor = MaterialTheme.colorScheme.onSurface,
        textContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        tonalElevation = 0.dp,
        properties = properties,
    )
}

@Composable
private fun JichangDialogSurface(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface,
        tonalElevation = 0.dp,
        content = content,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun JichangApp(viewModel: AppViewModel = viewModel()) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current
    val motionEnabled = remember(context) {
        runCatching { Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) > 0f }.getOrDefault(true)
    }
    val localView = LocalView.current
    SideEffect {
        (context as? Activity)?.window?.let { window ->
            WindowCompat.getInsetsController(window, localView).apply {
                isAppearanceLightStatusBars = true
                isAppearanceLightNavigationBars = true
            }
        }
    }
    var page by remember { mutableStateOf(AppPage.Home) }
    var resourceTab by remember { mutableStateOf(ResourceTab.Sources) }
    var configTab by remember { mutableStateOf(ConfigTab.Rules) }
    var dialog by remember { mutableStateOf<DialogKind?>(null) }
    var providerInitialType by remember { mutableStateOf<String?>(null) }
    var editingGroup by remember { mutableStateOf<PolicyGroup?>(null) }
    var editingRule by remember { mutableStateOf<Pair<Int, com.jzb.jichang.android.model.RoutingRule>?>(null) }
    var createProfileFromCurrent by remember { mutableStateOf(true) }
    var profileMenu by remember { mutableStateOf(false) }
    var profileActionMenu by remember { mutableStateOf(false) }
    var showProfileDeleteConfirmation by remember { mutableStateOf<String?>(null) }
    var showExportSetup by remember { mutableStateOf(false) }
    var showRemoteConfigDialog by remember { mutableStateOf(false) }
    var remoteDownloading by remember { mutableStateOf(false) }
    var remoteProgress by remember { mutableStateOf<Float?>(null) }
    var remoteStatus by remember { mutableStateOf<String?>(null) }
    var remoteError by remember { mutableStateOf<String?>(null) }
    var templateToPreview by remember { mutableStateOf<com.jzb.jichang.android.model.ConfigTemplate?>(null) }
    var templateToCreate by remember { mutableStateOf<com.jzb.jichang.android.model.ConfigTemplate?>(null) }
    var templateToRename by remember { mutableStateOf<com.jzb.jichang.android.model.ConfigTemplate?>(null) }
    var templateToDelete by remember { mutableStateOf<com.jzb.jichang.android.model.ConfigTemplate?>(null) }
    var pendingSensitiveAction by remember { mutableStateOf<ExportAction?>(null) }
    val glassPreferences = remember(context) { context.getSharedPreferences("appearance", Context.MODE_PRIVATE) }
    var glassOpacity by remember { mutableStateOf(glassPreferences.getFloat("glass_opacity", 0.45f).coerceIn(0.2f, 0.7f)) }
    var appearanceDialog by remember { mutableStateOf(false) }
    val liquidGlassScene = remember { LiquidGlassScene() }
    var glassTopRect by remember { mutableStateOf(Rect.Zero) }
    var glassBottomRect by remember { mutableStateOf(Rect.Zero) }
    val scope = rememberCoroutineScope()
    val remoteDownloader = remember { RemoteConfigDownloader() }
    val shareController = remember { LocalShareController(context) }
    val shareUrl by shareController.url.collectAsState()
    val shareError by shareController.error.collectAsState()
    val generator = remember { MihomoConfigGenerator() }
    val profile = state.activeProfile
    val parsedTemplate = remember(profile.templateId, state.templates) {
        profile.templateId?.let { id -> state.templates.firstOrNull { it.id == id }?.let { runCatching { MihomoTemplateParser().parse(it.rawYaml) }.getOrNull() } }
    }
    val exportOptions = remember(profile) {
        ConfigExportOptions(
            sourceMode = runCatching { ConfigSourceMode.valueOf(profile.sourceMode) }.getOrDefault(ConfigSourceMode.EMBED_NODES),
            regionOverrides = profile.regionOverrides,
            enabledRegions = profile.enabledRegions,
            selectedSourceIds = profile.selectedSourceIds,
            enabledNodeIds = profile.enabledNodeIds,
        )
    }
    val generated = remember(state, profile, exportOptions) { generator.generate(state, profile, exportOptions) }
    val configText = generated.yaml
    val filename = remember(profile.fileName) { safeYamlFileName(profile.fileName) }
    var templateExportContent by remember { mutableStateOf<String?>(null) }

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

    val saveTemplateLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/yaml")) { uri: Uri? ->
        val content = templateExportContent
        templateExportContent = null
        if (uri != null && content != null) scope.launch {
            runCatching {
                withContext(Dispatchers.IO) {
                    checkNotNull(context.contentResolver.openOutputStream(uri)) { "无法打开目标文件" }.use { it.write(content.toByteArray(Charsets.UTF_8)) }
                }
            }.onSuccess { viewModel.run { "模板已导出" } }
                .onFailure { viewModel.run { throw it } }
        }
    }

    fun startRemoteDownload(url: String, requestedFileName: String) {
        remoteDownloading = true
        remoteError = null
        remoteStatus = null
        remoteProgress = null
        scope.launch {
            try {
                val file = withContext(Dispatchers.IO) {
                    remoteDownloader.download(url, requestedFileName) { progress -> remoteProgress = progress.fraction }
                }
                val rawYaml = String(file.bytes, Charsets.UTF_8)
                withContext(Dispatchers.Default) { com.jzb.jichang.android.service.MihomoTemplateParser().parse(rawYaml) }
                val templateName = file.fileName.removeSuffix(".yaml").ifBlank { "远程模板" }
                viewModel.saveTemplateNow(templateName, rawYaml, file.fileName)
                remoteStatus = "模板“$templateName”已保存到本机"
                showRemoteConfigDialog = false
            } catch (error: Throwable) {
                remoteError = error.message ?: "远程配置下载失败，请重试"
            } finally {
                remoteDownloading = false
                remoteProgress = null
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
        if (generated.unresolvedTemplateProviders.isNotEmpty()) {
            viewModel.run { throw IllegalStateException("请先为模板订阅绑定机场：${generated.unresolvedTemplateProviders.joinToString("、")}") }
        } else if (generated.referencedSubscriptions > 0) pendingSensitiveAction = action
        else if (action == ExportAction.Download) downloadConfig() else shareController.start(configText, filename)
    }

    JichangTheme(glassOpacity = glassOpacity) {
        Scaffold(
            modifier = Modifier.fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .graphicsLayer {
                    liquidGlassScene.shader.setFloatUniform("topRect", glassTopRect.left, glassTopRect.top, glassTopRect.right, glassTopRect.bottom)
                    liquidGlassScene.shader.setFloatUniform("bottomRect", glassBottomRect.left, glassBottomRect.top, glassBottomRect.right, glassBottomRect.bottom)
                    liquidGlassScene.shader.setFloatUniform("topRadius", 0f)
                    liquidGlassScene.shader.setFloatUniform("bottomRadius", 0f)
                    liquidGlassScene.shader.setFloatUniform("opacity", glassOpacity)
                    renderEffect = liquidGlassScene.renderEffect
                },
            contentWindowInsets = WindowInsets(0),
            containerColor = Color.Transparent,
            topBar = {
                TopAppBar(
                    title = { Text(page.label, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold) },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
                    modifier = Modifier
                        .onGloballyPositioned { coordinates ->
                            val pos = coordinates.positionInRoot()
                            glassTopRect = Rect(pos.x, pos.y, pos.x + coordinates.size.width, pos.y + coordinates.size.height)
                        }
                        .glassMaterial(RectangleShape, glassOpacity),
                    actions = {
                        Box {
                            Row(
                                Modifier.clip(RoundedCornerShape(10.dp))
                                    .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { profileMenu = true }
                                    .padding(horizontal = 8.dp, vertical = 9.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(2.dp),
                            ) {
                                Text(profile.name, maxLines = 1, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurface)
                                Icon(Icons.Outlined.KeyboardArrowDown, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            DropdownMenu(expanded = profileMenu, onDismissRequest = { profileMenu = false }) {
                                state.profiles.forEach { item -> DropdownMenuItem(
                                    text = { Text(if (item.id == profile.id) "✓ ${item.name}" else item.name) },
                                    onClick = { viewModel.switchProfile(item.id); profileMenu = false },
                                ) }
                                DropdownMenuItem(text = { Text("新建配置…") }, onClick = { createProfileFromCurrent = true; dialog = DialogKind.Profile; profileMenu = false })
                                DropdownMenuItem(text = { Text("管理当前配置…") }, onClick = { createProfileFromCurrent = false; dialog = DialogKind.Profile; profileMenu = false })
                                DropdownMenuItem(text = { Text("外观与玻璃效果") }, onClick = { appearanceDialog = true; profileMenu = false })
                                if (state.profiles.size > 1) DropdownMenuItem(text = { Text("删除当前配置") }, onClick = { showProfileDeleteConfirmation = profile.id; profileMenu = false })
                            }
                        }
                    },
                )
            },
            bottomBar = {
                NavigationBar(
                    modifier = Modifier
                        .onGloballyPositioned { coordinates ->
                            val pos = coordinates.positionInRoot()
                            glassBottomRect = Rect(pos.x, pos.y, pos.x + coordinates.size.width, pos.y + coordinates.size.height)
                        }
                        .glassMaterial(RectangleShape, glassOpacity),
                    containerColor = Color.Transparent,
                    tonalElevation = 0.dp,
                ) {
                    AppPage.entries.forEach { item ->
                        val icon = when (item) {
                            AppPage.Home -> Icons.Outlined.Home
                            AppPage.Share -> Icons.Outlined.Share
                            AppPage.Resources -> Icons.Outlined.Devices
                        }
                        NavigationBarItem(
                            selected = page == item,
                            onClick = { page = item },
                            icon = { Icon(icon, null) },
                            label = { Text(item.label) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = MaterialTheme.colorScheme.primary,
                                selectedTextColor = MaterialTheme.colorScheme.primary,
                                unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                indicatorColor = Color.Transparent,
                            ),
                        )
                    }
                }
            },
        ) { insets ->
            Column(Modifier.fillMaxSize().padding(insets)) {
                viewModel.message?.let { message ->
                    Card(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                        Text(message, Modifier.padding(12.dp), style = MaterialTheme.typography.bodySmall, color = if (viewModel.messageIsError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface)
                    }
                }
                AnimatedContent(
                    targetState = page,
                    modifier = Modifier.weight(1f),
                    transitionSpec = {
                        if (motionEnabled) (fadeIn() + scaleIn(initialScale = 0.99f)).togetherWith(fadeOut())
                        else EnterTransition.None togetherWith ExitTransition.None
                    },
                    label = "main-page-transition",
                ) { currentPage -> when (currentPage) {
                    AppPage.Home -> HomePage(
                        state = state,
                        onNavigate = { page = it },
                        onOpenTemplates = { page = AppPage.Resources; resourceTab = ResourceTab.Templates },
                        modifier = Modifier.fillMaxSize(),
                    )
                    AppPage.Resources -> Column(Modifier.fillMaxSize()) {
                        SegmentedTabs(ResourceTab.entries.map { it.label }, resourceTab.ordinal) { resourceTab = ResourceTab.entries[it] }
                        when (resourceTab) {
                            ResourceTab.Sources -> SourcesPage(state, viewModel, { dialog = DialogKind.Source }, Modifier.weight(1f))
                            ResourceTab.Nodes -> NodesPage(
                                state = state,
                                viewModel = viewModel,
                                modifier = Modifier.weight(1f),
                                onAdd = { dialog = DialogKind.Node },
                                onRegionChange = { id, key ->
                                    val overrides = if (key == null) profile.regionOverrides - id else profile.regionOverrides + (id to key)
                                    viewModel.updateExportSettings(profile.sourceMode, profile.enabledRegions, overrides)
                                },
                            )
                            ResourceTab.Templates -> TemplatesPage(
                                templates = state.templates,
                                profiles = state.profiles,
                                downloading = remoteDownloading,
                                progress = remoteProgress,
                                status = remoteStatus,
                                error = remoteError,
                                onAdd = { showRemoteConfigDialog = true; remoteError = null },
                                onPreview = { templateToPreview = it },
                                onCreate = { templateToCreate = it },
                                onRename = { templateToRename = it },
                                onDelete = { templateToDelete = it },
                                onExport = { template -> templateExportContent = template.rawYaml; saveTemplateLauncher.launch(template.fileName) },
                                modifier = Modifier.weight(1f),
                            )
                        }
                    }
                    AppPage.Share -> Column(Modifier.fillMaxSize()) {
                        SegmentedTabs(ConfigTab.entries.map { it.label }, configTab.ordinal) { configTab = ConfigTab.entries[it] }
                        when (configTab) {
                            ConfigTab.Rules -> RulesPage(
                                state, viewModel, Modifier.weight(1f),
                                onEditGroup = { editingGroup = it; dialog = DialogKind.Group },
                                onEditRule = { index, rule -> editingRule = index to rule; dialog = DialogKind.Rule },
                                onAddRule = { editingRule = null; dialog = DialogKind.Rule },
                                onAddGroup = { editingGroup = null; dialog = DialogKind.Group },
                                onProviders = { type -> providerInitialType = type; dialog = DialogKind.Providers },
                                onSaveRuleProfile = viewModel::saveRuleProfile,
                            )
                            ConfigTab.Export -> ExportPage(
                                state = state, options = exportOptions, generatedNodes = generated.exportedNodes,
                                skippedNodes = generated.skippedNodes, referencedSubscriptions = generated.referencedSubscriptions,
        configText = configText, shareUrl = shareUrl, shareError = shareError, filename = filename,
                                unresolvedTemplateProviders = generated.unresolvedTemplateProviders,
                                templateParameters = parsedTemplate?.subscriptionParameters.orEmpty(),
                                templateBindings = profile.templateProviderBindings,
                                ruleIssues = RuleDiagnostics.inspect(profile.ruleProfile, state.nodes.map { it.id }.toSet()),
                                hasRegionalProxyGroups = parsedTemplate?.hasRegionalProxyGroups == true,
                                onTemplateBinding = viewModel::bindTemplateProvider,
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

                } }
            }
        }

    if (showRemoteConfigDialog) RemoteConfigDialog(
        downloading = remoteDownloading,
        progress = remoteProgress,
        error = remoteError,
        onDismiss = { if (!remoteDownloading) showRemoteConfigDialog = false },
        onDownload = ::startRemoteDownload,
    )

    templateToPreview?.let { template -> TemplatePreviewDialog(template, onDismiss = { templateToPreview = null }) }
    templateToCreate?.let { template -> TemplateCreateDialog(template, onDismiss = { templateToCreate = null }, onCreate = { name, fileName ->
        viewModel.createProfileFromTemplate(name, fileName, template.id)
        templateToCreate = null
        page = AppPage.Share
        configTab = ConfigTab.Rules
    }) }
    templateToRename?.let { template -> TemplateRenameDialog(template, onDismiss = { templateToRename = null }, onRename = { name ->
        viewModel.renameTemplate(template.id, name)
        templateToRename = null
    }) }
    templateToDelete?.let { template -> JichangAlertDialog(
        onDismissRequest = { templateToDelete = null },
        title = { Text("删除模板？") },
        text = { Text("模板原文会从本机移除。正在使用此模板的配置需要先删除或改用其他模板。") },
        confirmButton = { TextButton(onClick = { viewModel.deleteTemplate(template.id); templateToDelete = null }) { Text("删除") } },
        dismissButton = { TextButton(onClick = { templateToDelete = null }) { Text("取消") } },
    ) }

    when (dialog) {
        DialogKind.Source -> SourceDialog(onDismiss = { dialog = null }, onSave = { name, url -> viewModel.addSource(name, url); dialog = null })
        DialogKind.Node -> NodeDialog(onDismiss = { dialog = null }, onSave = { raw -> viewModel.importNodes(raw); dialog = null })
        DialogKind.Group -> GroupDialog(state, editingGroup, onDismiss = { dialog = null; editingGroup = null }, onSave = { name, type, members, ruleIndices, providerIds ->
            val existing = editingGroup
            if (existing == null) viewModel.addGroup(name, type, members, ruleIndices, providerIds) else viewModel.updateGroup(existing.name, name, type, members)
            dialog = null; editingGroup = null
        })
        DialogKind.Rule -> RuleDialog(state, editingRule, onDismiss = { dialog = null; editingRule = null }, onSave = { index, rule ->
            if (index == null) viewModel.addRule(rule) else viewModel.updateRule(index, rule)
            dialog = null; editingRule = null
        })
        DialogKind.Providers -> RuleProvidersDialog(state.ruleProfile, initialProviderType = providerInitialType, onDismiss = { dialog = null }, onSave = { rules ->
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
        onToggleRegion = { key -> viewModel.updateExportSettings(profile.sourceMode, if (key in profile.enabledRegions) profile.enabledRegions - key else profile.enabledRegions + key, profile.regionOverrides) },
        onDismiss = { showExportSetup = false },
    )

    pendingSensitiveAction?.let { action ->
        JichangAlertDialog(
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
        JichangAlertDialog(onDismissRequest = { showProfileDeleteConfirmation = null }, title = { Text("删除配置？") },
            text = { Text("只会删除此配置的规则和选择，不会删除共享订阅或节点。") },
            confirmButton = { TextButton(onClick = { viewModel.deleteProfile(targetId); showProfileDeleteConfirmation = null }) { Text("删除") } },
            dismissButton = { TextButton(onClick = { showProfileDeleteConfirmation = null }) { Text("取消") } })
    }
    if (appearanceDialog) {
        JichangAlertDialog(
            onDismissRequest = { appearanceDialog = false },
            title = { Text("外观与玻璃效果") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("透明度　${(glassOpacity * 100).toInt()}%", style = MaterialTheme.typography.titleSmall)
                    Slider(
                        value = glassOpacity,
                        onValueChange = { value ->
                            glassOpacity = value
                            glassPreferences.edit().putFloat("glass_opacity", value).apply()
                        },
                        valueRange = 0.2f..0.7f,
                        steps = 9,
                    )
                    Text("只调整顶栏、底栏和分段控件。正文保持清晰。", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                        Text("玻璃预览", Modifier.glassMaterial(RoundedCornerShape(18.dp), glassOpacity).padding(horizontal = 28.dp, vertical = 12.dp), fontWeight = FontWeight.SemiBold)
                    }
                }
            },
            confirmButton = { TextButton(onClick = { appearanceDialog = false }) { Text("完成") } },
        )
    }
    }
}

@Composable
private fun HomePage(
    state: AppState,
    onNavigate: (AppPage) -> Unit,
    onOpenTemplates: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val profile = state.activeProfile
    LazyColumn(modifier, contentPadding = PaddingValues(horizontal = JichangSpacing.pageHorizontal, vertical = JichangSpacing.pageVertical), verticalArrangement = Arrangement.spacedBy(JichangSpacing.section)) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("配置一目了然", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold)
                Text("管理 Mihomo 配置与节点资源", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        item {
            Card(onClick = { onNavigate(AppPage.Share) }, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                Column(Modifier.fillMaxWidth().padding(JichangSpacing.card), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("当前配置", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(profile.name, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                        }
                        Icon(Icons.Outlined.Description, null, Modifier.size(24.dp), tint = MaterialTheme.colorScheme.primary)
                    }
                    Text("${profile.enabledNodeIds.size} 个节点  ·  ${profile.ruleProfile.rules.size} 条规则  ·  ${profile.ruleProfile.groups.size} 个策略组", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("打开规则与分享  →", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary)
                }
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                MetricCard("订阅来源", "${state.sources.size}", "${profile.selectedSourceIds.size} 个用于此配置", Modifier.weight(1f)) { onNavigate(AppPage.Resources) }
                MetricCard("节点资源", "${state.nodes.size}", "${profile.enabledNodeIds.size} 个已启用", Modifier.weight(1f)) { onNavigate(AppPage.Resources) }
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                MetricCard("分流规则", "${profile.ruleProfile.rules.size}", "${profile.ruleProfile.groups.size} 个策略组", Modifier.weight(1f)) { onNavigate(AppPage.Share) }
                MetricCard("本地模板", "${state.templates.size}", "可作为新配置起点", Modifier.weight(1f), onOpenTemplates)
            }
        }
        item {
            Text("快捷入口", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        }
        item {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                Column {
                    DashboardAction("订阅与节点", "管理来源和可用节点", Icons.Outlined.Devices) { onNavigate(AppPage.Resources) }
                    androidx.compose.material3.HorizontalDivider(Modifier.padding(start = 54.dp))
                    DashboardAction("规则与分享", "编辑 Mihomo 规则，预览或分享配置", Icons.Outlined.Tune) { onNavigate(AppPage.Share) }
                    androidx.compose.material3.HorizontalDivider(Modifier.padding(start = 54.dp))
                    DashboardAction("模板库", "从远程 Mihomo YAML 建立模板", Icons.Outlined.FolderOpen, onOpenTemplates)
                }
            }
        }
    }
}

@Composable
private fun MetricCard(title: String, value: String, detail: String, modifier: Modifier, onClick: () -> Unit) {
    Card(onClick = onClick, modifier = modifier, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(Modifier.padding(JichangSpacing.card), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(title, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            Text(detail, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun DashboardAction(title: String, detail: String, icon: androidx.compose.ui.graphics.vector.ImageVector, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp).clickable(onClick = onClick), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text(detail, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
    }
}

@Composable
private fun SourcesPage(state: AppState, viewModel: AppViewModel, onAdd: () -> Unit, modifier: Modifier = Modifier) {
    val profile = state.activeProfile
    LazyColumn(modifier, contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = JichangSpacing.pageHorizontal, vertical = JichangSpacing.pageVertical), verticalArrangement = Arrangement.spacedBy(JichangSpacing.section)) {
        item {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("订阅资源", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold)
                    Text("当前配置启用 ${profile.selectedSourceIds.size} / ${state.sources.size}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                OutlinedButton(onClick = { viewModel.refreshAllSources() }, enabled = state.sources.isNotEmpty() && viewModel.refreshingSourceIds.isEmpty()) {
                    if (viewModel.refreshingSourceIds.isNotEmpty()) CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp)
                    else Icon(Icons.Outlined.ArrowDownward, null)
                    Spacer(Modifier.width(6.dp)); Text("全部刷新")
                }
            }
            OutlinedButton(onClick = onAdd, modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
                Icon(Icons.Outlined.Add, null); Spacer(Modifier.width(8.dp)); Text("添加订阅")
            }
        }
        if (state.sources.isEmpty()) item { EmptyCard("还没有订阅", "使用上方按钮添加机场订阅，或到“节点”页直接导入节点链接。") }
        items(state.sources, key = { it.id }) { source ->
            Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                Column(Modifier.padding(JichangSpacing.card), verticalArrangement = Arrangement.spacedBy(8.dp)) {
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
                    Text(
                        if (source.lastError != null) source.lastError else "$count 个节点${source.updatedAt?.let { " · 已更新" } ?: " · 尚未刷新"}",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (source.lastError != null) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    if (source.id in profile.selectedSourceIds) when (source.providerCompatible) {
                        true -> Text("已检测为 Mihomo YAML，可在配置中远程引用。", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        false -> Text("此订阅不是 Mihomo YAML；引用模式会把已解析节点内嵌。", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        null -> Text("尚未检测格式；刷新后可判断能否远程引用。", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}

@Composable
private fun NodesPage(
    state: AppState,
    viewModel: AppViewModel,
    modifier: Modifier = Modifier,
    onAdd: () -> Unit,
    onRegionChange: (String, String?) -> Unit,
) {
    var query by remember { mutableStateOf("") }
    var protocolFilter by remember { mutableStateOf("全部协议") }
    var sourceFilter by remember { mutableStateOf("全部来源") }
    var enabledFilter by remember { mutableStateOf("全部") }
    var regionFilter by remember { mutableStateOf("全部地区") }
    var expandedProtocol by remember { mutableStateOf(false) }
    var editingNode by remember { mutableStateOf<ProxyNode?>(null) }
    val profile = state.activeProfile
    val regionKeys = NodeAutoGroups.regions.map { it.key } + NodeAutoGroups.OTHER
    val regions = listOf("全部地区") + regionKeys.map(NodeAutoGroups::title)
    val protocols = listOf("全部协议") + state.nodes.map { it.type.uppercase() }.distinct().sorted()
    val filtered = state.nodes.filter { node ->
        val selectedRegion = profile.regionOverrides[node.id] ?: NodeAutoGroups.classify(node.name)
        (query.isBlank() || node.name.contains(query, true) || node.server.contains(query, true)) &&
            (protocolFilter == "全部协议" || node.type.equals(protocolFilter, true)) &&
            (sourceFilter == "全部来源" || (sourceFilter == "手动导入" && node.sourceId == null) || state.sources.firstOrNull { it.id == node.sourceId }?.name == sourceFilter) &&
            (enabledFilter == "全部" || (node.id in profile.enabledNodeIds) == (enabledFilter == "已启用")) &&
            (regionFilter == "全部地区" || NodeAutoGroups.title(selectedRegion) == regionFilter)
    }
    LazyColumn(
        modifier = modifier.fillMaxSize().padding(horizontal = JichangSpacing.pageHorizontal),
        contentPadding = PaddingValues(top = 8.dp, bottom = 8.dp),
        verticalArrangement = Arrangement.spacedBy(JichangSpacing.item),
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(JichangSpacing.item)) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("节点", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold)
                    Text("启用状态和地区归属按当前配置保存", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                OutlinedButton(onClick = onAdd, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Outlined.Add, null); Spacer(Modifier.width(8.dp)); Text("添加节点")
                }
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("搜索节点名称或服务器") },
                    singleLine = true,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(JichangSpacing.item), modifier = Modifier.fillMaxWidth()) {
                    Box(Modifier.weight(1f)) {
                        OutlinedButton(onClick = { expandedProtocol = true }, modifier = Modifier.fillMaxWidth()) {
                            Text(protocolFilter, Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Icon(Icons.Outlined.KeyboardArrowDown, null)
                        }
                        DropdownMenu(expandedProtocol, { expandedProtocol = false }) {
                            protocols.forEach { protocol -> DropdownMenuItem(text = { Text(protocol) }, onClick = { protocolFilter = protocol; expandedProtocol = false }) }
                        }
                    }
                    ChoiceMenu(regionFilter, regions, { regionFilter = it }, Modifier.weight(1f))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(JichangSpacing.item), modifier = Modifier.fillMaxWidth()) {
                    ChoiceMenu(sourceFilter, listOf("全部来源", "手动导入") + state.sources.map { it.name }, { sourceFilter = it }, Modifier.weight(1f))
                    DropdownMenuFilter(enabledFilter, { enabledFilter = it }, Modifier.weight(1f))
                }
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text("${filtered.size} 个结果", Modifier.weight(1f), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    TextButton(onClick = { viewModel.setNodesEnabled(filtered.map { it.id }.toSet(), true) }, enabled = filtered.isNotEmpty()) { Text("全部启用") }
                    TextButton(onClick = { viewModel.setNodesEnabled(filtered.map { it.id }.toSet(), false) }, enabled = filtered.isNotEmpty()) { Text("全部停用") }
                }
            }
        }
        if (state.nodes.isEmpty()) item { EmptyCard("还没有节点", "使用上方按钮导入 Mihomo YAML、Base64 订阅或节点链接。") }
        else if (filtered.isEmpty()) item { EmptyCard("没有匹配的节点", "换个搜索词或清除筛选条件。") }
        else items(filtered, key = { it.id }) { node ->
            val region = profile.regionOverrides[node.id] ?: NodeAutoGroups.classify(node.name)
            NodeRow(
                node = node,
                enabled = node.id in profile.enabledNodeIds,
                region = region,
                onToggle = { viewModel.toggleNode(node.id) },
                onRegionChange = { onRegionChange(node.id, it) },
                onDelete = { viewModel.removeNode(node.id) },
                onEdit = { editingNode = node },
            )
        }
        item {
            Text(
                "当前配置启用 ${filtered.count { it.id in profile.enabledNodeIds }} / ${filtered.size} · 共 ${state.nodes.size} 个节点",
                Modifier.padding(vertical = 4.dp),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
    editingNode?.let { node -> NodeEditDialog(node, onDismiss = { editingNode = null }, onSave = { name, type, server, port, options ->
        viewModel.updateNode(node.id, name, type, server, port, options); editingNode = null
    }) }
}

@Composable
private fun DropdownMenuFilter(value: String, onValue: (String) -> Unit, modifier: Modifier = Modifier) {
    var expanded by remember { mutableStateOf(false) }
    Box(modifier) {
        OutlinedButton(onClick = { expanded = true }, modifier = Modifier.fillMaxWidth()) {
            Text(value, Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
            Icon(Icons.Outlined.KeyboardArrowDown, null)
        }
        DropdownMenu(expanded, { expanded = false }) {
            listOf("全部", "已启用", "已停用").forEach { option ->
                DropdownMenuItem(text = { Text(option) }, onClick = { onValue(option); expanded = false })
            }
        }
    }
}

@Composable
private fun ChoiceMenu(label: String, options: List<String>, onValue: (String) -> Unit, modifier: Modifier = Modifier) {
    var expanded by remember { mutableStateOf(false) }
    Box(modifier) {
        OutlinedButton(onClick = { expanded = true }, modifier = Modifier.fillMaxWidth()) {
            Text(label, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
            Icon(Icons.Outlined.KeyboardArrowDown, null)
        }
        DropdownMenu(expanded, { expanded = false }) {
            options.distinct().forEach { option -> DropdownMenuItem(text = { Text(option) }, onClick = { onValue(option); expanded = false }) }
        }
    }
}

@Composable
private fun NodeRow(
    node: ProxyNode,
    enabled: Boolean,
    region: String,
    onToggle: () -> Unit,
    onRegionChange: (String?) -> Unit,
    onDelete: () -> Unit,
    onEdit: () -> Unit,
) {
    var regionMenu by remember { mutableStateOf(false) }
    Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(Modifier.padding(start = 14.dp, end = 6.dp, top = 8.dp, bottom = 4.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(node.name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Medium)
                    Text("${node.type.uppercase()} · ${node.server}:${node.port}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
                }
                Switch(checked = enabled, onCheckedChange = { onToggle() })
            }
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.weight(1f)) {
                    TextButton(onClick = { regionMenu = true }, contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)) {
                        Text("地区：${NodeAutoGroups.title(region)}")
                        Icon(Icons.Outlined.KeyboardArrowDown, null)
                    }
                    DropdownMenu(expanded = regionMenu, onDismissRequest = { regionMenu = false }) {
                        DropdownMenuItem(text = { Text("自动识别 · ${NodeAutoGroups.title(NodeAutoGroups.classify(node.name))}") }, onClick = { onRegionChange(null); regionMenu = false })
                        (NodeAutoGroups.regions.map { it.key } + NodeAutoGroups.OTHER).forEach { key ->
                            DropdownMenuItem(text = { Text(NodeAutoGroups.title(key)) }, onClick = { onRegionChange(key); regionMenu = false })
                        }
                    }
                }
                IconButton(onClick = onEdit) { Icon(Icons.Outlined.Edit, "编辑节点") }
                IconButton(onClick = onDelete) { Icon(Icons.Outlined.Delete, "删除节点") }
            }
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
    onAddRule: () -> Unit,
    onAddGroup: () -> Unit,
    onProviders: (String?) -> Unit,
    onSaveRuleProfile: (RuleProfile) -> Unit,
) {
    val context = LocalContext.current
    val workspacePrefs = remember(context) { context.getSharedPreferences("rules_ui", Context.MODE_PRIVATE) }
    var workspace by remember(workspacePrefs) { mutableStateOf(workspacePrefs.getString("workspace", "basic") ?: "basic") }
    var advancedSection by remember { mutableStateOf(0) }
    var query by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("全部类型") }
    var strategy by remember { mutableStateOf("全部策略") }
    var selecting by remember { mutableStateOf(false) }
    var selectedRules by remember { mutableStateOf(setOf<Int>()) }
    var deleting by remember { mutableStateOf(false) }
    var deletingIndex by remember { mutableStateOf<Int?>(null) }
    var groupToDelete by remember { mutableStateOf<PolicyGroup?>(null) }
    var showSimulator by remember { mutableStateOf(false) }
    var basicEditing by remember { mutableStateOf<Pair<Int?, RoutingRule?>?>(null) }
    var focusedProviderId by remember { mutableStateOf<String?>(null) }
    var focusedSubRuleName by remember { mutableStateOf<String?>(null) }
    var bulkTarget by remember { mutableStateOf("") }
    var showTemplateRuleSetApply by remember { mutableStateOf(false) }
    var ruleSetKind by remember { mutableStateOf(0) }
    val issues = remember(state.ruleProfile, state.ruleProviderStatuses, state.activeProfile.id) {
        RuleDiagnostics.inspect(state.ruleProfile, state.nodes.map { it.id }.toSet()) + state.ruleProviderStatuses
            .filter { it.profileId == state.activeProfile.id && !it.error.isNullOrBlank() }
            .mapNotNull { status -> state.ruleProfile.providers.firstOrNull { it.id == status.providerId }?.let { provider ->
                com.jzb.jichang.android.service.RuleIssue(-1, RoutingRule("RULE-SET", provider.name, ""), "规则集刷新失败：${status.error}", providerId = provider.id)
            } }
    }
    val categories = listOf("全部类型", "域名", "IP 与地理", "端口与网络", "进程", "规则集", "逻辑与兜底")
    val lastMovableIndex = state.ruleProfile.rules.lastIndex - if (state.ruleProfile.rules.any { it.type == "MATCH" }) 1 else 0
    val targetNames = state.ruleProfile.groups.map { it.name } + listOf("DIRECT", "REJECT")
    val visibleRules = state.ruleProfile.rules.mapIndexed { index, rule -> index to rule }.filter { (_, rule) ->
        val conditionText = buildString { fun appendCondition(condition: com.jzb.jichang.android.model.RuleCondition) { append(' '); append(condition.type.orEmpty()); append(' '); append(condition.value); append(' '); append(condition.argument.orEmpty()); condition.children.forEach(::appendCondition) }; rule.conditions.forEach(::appendCondition) }
        val matchesText = query.isBlank() || listOf(rule.type, rule.value, rule.group, rule.rawLine.orEmpty(), rule.extraParameters.joinToString(" "), conditionText).any { it.contains(query, true) }
        val matchesStrategy = strategy == "全部策略" || rule.group == strategy
        val matchesCategory = when (category) {
            "域名" -> rule.type.startsWith("DOMAIN") || rule.type == "GEOSITE"
            "IP 与地理" -> rule.type.contains("IP") || rule.type.contains("GEO")
            "端口与网络" -> rule.type.contains("PORT") || rule.type in setOf("NETWORK", "IN-TYPE", "DSCP")
            "进程" -> rule.type.startsWith("PROCESS") || rule.type == "UID"
            "规则集" -> rule.type in setOf("RULE-SET", "SUB-RULE")
            "逻辑与兜底" -> rule.type in setOf("AND", "OR", "NOT", "MATCH")
            else -> true
        }
        matchesText && matchesStrategy && matchesCategory
    }
    Column(modifier) {
        SegmentedTabs(listOf("基础分流", "高级规则管理"), if (workspace == "basic") 0 else 1) { selected ->
            workspace = if (selected == 0) "basic" else "advanced"
            workspacePrefs.edit().putString("workspace", workspace).apply()
            selecting = false
            selectedRules = emptySet()
        }
        if (workspace == "basic") {
            Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("常用分流", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Text("规则按列表顺序匹配；兜底规则固定在最后。", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                OutlinedButton(onClick = { showSimulator = true }) { Text("规则模拟") }
            }
            Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(query, { query = it }, Modifier.weight(1f), label = { Text("搜索规则") }, singleLine = true)
                OutlinedButton(onClick = { basicEditing = null to null }) { Icon(Icons.Outlined.Add, null); Text("添加") }
            }
            Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                ChoiceMenu(category, listOf("全部类型", "域名", "IP 与地理", "规则集"), { category = it }, Modifier.weight(1f))
                ChoiceMenu(strategy, listOf("全部策略") + targetNames, { strategy = it }, Modifier.weight(1f))
            }
            Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                Spacer(Modifier.weight(1f))
                Text("${visibleRules.size} 条", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            val blockingIssues = issues.count { !it.warning }
            if (issues.isNotEmpty()) Card(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp), colors = CardDefaults.cardColors(containerColor = if (blockingIssues == 0) MaterialTheme.colorScheme.tertiaryContainer else MaterialTheme.colorScheme.errorContainer)) {
                val color = if (blockingIssues == 0) MaterialTheme.colorScheme.onTertiaryContainer else MaterialTheme.colorScheme.onErrorContainer
                Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("$blockingIssues 个配置错误 · ${issues.size - blockingIssues} 个可能冲突", Modifier.weight(1f), color = color, style = MaterialTheme.typography.labelLarge)
                    TextButton(onClick = { workspace = "advanced"; advancedSection = 4; workspacePrefs.edit().putString("workspace", workspace).apply() }) { Text("查看详情") }
                }
            }
            if (state.ruleProfile.rules.isEmpty()) EmptyCard("还没有分流规则", "添加规则，或从规则集分区应用模板规则。")
            else if (visibleRules.isEmpty()) Text("没有符合筛选条件的规则。", Modifier.padding(20.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)
            else LazyColumn(Modifier.weight(1f), contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(visibleRules, key = { it.first }) { (index, rule) ->
                    val isAdvancedRule = !isBasicWorkspaceRule(rule)
                    BasicRuleCard(rule, index, index > 0 && rule.type != "MATCH", index < lastMovableIndex && rule.type != "MATCH", isAdvancedRule,
                        onEdit = { if (isAdvancedRule) { workspace = "advanced"; advancedSection = 0; workspacePrefs.edit().putString("workspace", workspace).apply(); onEditRule(index, rule) } else basicEditing = index to rule }, onMoveUp = { viewModel.moveRule(index, -1) }, onMoveDown = { viewModel.moveRule(index, 1) },
                        onDelete = { deletingIndex = index })
                }
            }
        } else {
            SegmentedTabs(listOf("完整规则", "策略组", "规则集", "子规则", "校验 ${issues.size}"), advancedSection) { advancedSection = it }
            when (advancedSection) {
                0 -> {
                    Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                        OutlinedTextField(query, { query = it }, Modifier.weight(1f), label = { Text("搜索类型、内容、条件、策略与参数") }, singleLine = true)
                        Spacer(Modifier.width(8.dp)); OutlinedButton(onClick = onAddRule) { Icon(Icons.Outlined.Add, null); Text("规则") }
                    }
                    Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        ChoiceMenu(category, categories, { category = it }, Modifier.weight(1f))
                        ChoiceMenu(strategy, listOf("全部策略") + targetNames, { strategy = it }, Modifier.weight(1f))
                    }
                    Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text("按从上到下的顺序匹配", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.weight(1f))
                        TextButton(onClick = { selecting = !selecting; selectedRules = emptySet() }) { Text(if (selecting) "取消选择" else "批量操作") }
                        if (selecting) Text("已选 ${selectedRules.size}", style = MaterialTheme.typography.labelSmall)
                    }
                    if (selecting && selectedRules.isNotEmpty()) {
                        Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            SelectField("批量更改目标", bulkTarget, targetNames) { bulkTarget = it }
                            TextButton(enabled = bulkTarget in targetNames, onClick = { viewModel.changeRuleTargets(selectedRules, bulkTarget); selectedRules = emptySet(); selecting = false }) { Text("应用") }
                            TextButton(onClick = { deleting = true }) { Text("删除", color = MaterialTheme.colorScheme.error) }
                        }
                    }
                    if (state.ruleProfile.rules.isEmpty()) EmptyCard("还没有规则", "添加规则后，可在此使用完整类型和参数管理匹配顺序。")
                    else LazyColumn(Modifier.weight(1f), contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(visibleRules, key = { it.first }) { (index, rule) -> RuleListCard(
                            rule = rule, onEdit = { onEditRule(index, rule) }, onMoveUp = { viewModel.moveRule(index, -1) }, onMoveDown = { viewModel.moveRule(index, 1) },
                            onDelete = { deletingIndex = index }, canMoveUp = index > 0 && rule.type != "MATCH", canMoveDown = index < lastMovableIndex && rule.type != "MATCH",
                            selectable = selecting && !rule.type.equals("MATCH", true), checked = index in selectedRules,
                            onCheckedChange = { checked -> selectedRules = if (checked) selectedRules + index else selectedRules - index },
                            onDuplicate = { viewModel.duplicateRule(index) },
                        ) }
                    }
                }
                1 -> Column(Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
                    HeaderCard("策略组", "规则的出口目标。显示成员数和被规则引用数。")
                    OutlinedButton(onClick = onAddGroup, modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) { Icon(Icons.Outlined.Add, null); Spacer(Modifier.width(8.dp)); Text("新建策略组") }
                    LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) { items(state.ruleProfile.groups, key = { it.name }) { group ->
                        val references = state.ruleProfile.rules.count { it.group == group.name } + state.ruleProfile.subRules.sumOf { sub -> sub.rules.count { it.group == group.name } }
                        val activeNodes = state.nodes.count { node -> (node.sourceId == null || node.sourceId in state.activeProfile.selectedSourceIds) && node.id in state.activeProfile.enabledNodeIds }
                        val memberSummary = if (group.members.isEmpty() && !group.membersExplicit) "$activeNodes 个默认节点" else "${group.members.size} 个成员"
                        Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) { Row(Modifier.padding(start = 16.dp, end = 8.dp, top = 10.dp, bottom = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Outlined.Hub, null, tint = MaterialTheme.colorScheme.primary); Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) { Text(group.name, style = MaterialTheme.typography.titleSmall); Text("${group.type} · $memberSummary · 被引用 $references 次", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                            TextButton(onClick = { onEditGroup(group) }) { Text("编辑") }
                            IconButton(enabled = state.ruleProfile.groups.size > 1, onClick = { groupToDelete = group }) { Icon(Icons.Outlined.Delete, "删除策略组") }
                        } }
                    } }
                }
                2 -> Column(Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
                    SegmentedTabs(listOf("模板应用", "本机远程下载", "本机编写"), ruleSetKind) { ruleSetKind = it }
                    when (ruleSetKind) {
                        0 -> {
                            HeaderCard("应用模板规则集", "从资源中已下载的配置模板挑选规则集，并导入相关 RULE-SET 路由规则。")
                            OutlinedButton(onClick = { showTemplateRuleSetApply = true }, modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                                Icon(Icons.Outlined.Add, null); Spacer(Modifier.width(8.dp)); Text("浏览模板规则集")
                            }
                            if (state.templates.isEmpty()) EmptyCard("还没有配置模板", "先到“资源 → 模板”下载并保存 Mihomo YAML 模板。")
                            else LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                items(state.templates, key = { it.id }) { template ->
                                    val parsed = remember(template.id, template.rawYaml) { runCatching { MihomoTemplateParser().parse(template.rawYaml) }.getOrNull() }
                                    Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                                        Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                                            Column(Modifier.weight(1f)) {
                                                Text(template.name, style = MaterialTheme.typography.titleSmall)
                                                Text("${parsed?.ruleProfile?.providers?.size ?: 0} 个规则集 · ${parsed?.ruleProfile?.rules?.count { it.type == "RULE-SET" } ?: 0} 条规则集路由", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            }
                                            TextButton(onClick = { showTemplateRuleSetApply = true }) { Text("选择应用") }
                                        }
                                    }
                                }
                                item { HeaderCard("已应用到当前配置", "已从模板复制的规则集可在这里刷新、预览或编辑。") }
                                items(state.ruleProfile.providers.filter { it.sourceTemplateId != null }, key = { it.id }) { provider ->
                                    val status = state.ruleProviderStatuses.firstOrNull { it.profileId == state.activeProfile.id && it.providerId == provider.id }
                                    val refreshing = provider.id in viewModel.refreshingRuleProviderIds
                                    Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = if (focusedProviderId == provider.id) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface)) {
                                        Column(Modifier.fillMaxWidth().padding(14.dp)) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Column(Modifier.weight(1f)) {
                                                    Text(provider.name, style = MaterialTheme.typography.titleSmall)
                                                    Text("${provider.sourceTemplateName.orEmpty()} · ${provider.behavior} · ${provider.format}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                                }
                                                if (provider.type == "http") TextButton(enabled = !refreshing, onClick = { viewModel.refreshRuleProvider(state.activeProfile, provider) }) { Text(if (refreshing) "刷新中…" else "刷新") }
                                                TextButton(onClick = { viewModel.previewRuleProvider(state.activeProfile, provider, status) }) { Text("预览") }
                                            }
                                            Text(status?.error?.let { "刷新失败：$it" } ?: status?.refreshedAt?.let { "上次刷新 ${java.text.DateFormat.getDateTimeInstance().format(java.util.Date(it))}" } ?: "尚未刷新", style = MaterialTheme.typography.labelSmall, color = if (status?.error != null) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                    }
                                }
                            }
                        }
                        else -> {
                            val localOnly = ruleSetKind == 2
                            HeaderCard(if (localOnly) "本机编写" else "本机远程下载", if (localOnly) "以内嵌规则或应用规则目录中的本机文件保存。" else "规则内容可在本机刷新和预览；导出仍使用远程 URL。")
                            OutlinedButton(onClick = { onProviders(if (localOnly) "inline" else "http") }, modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                                Icon(Icons.Outlined.Add, null); Spacer(Modifier.width(8.dp)); Text(if (localOnly) "编写规则集" else "添加远程规则集")
                            }
                            val providers = state.ruleProfile.providers.filter { provider ->
                                provider.sourceTemplateId == null && if (localOnly) provider.type in setOf("inline", "file") else provider.type == "http"
                            }
                            if (providers.isEmpty()) EmptyCard(if (localOnly) "没有本机规则集" else "没有远程规则集", if (localOnly) "添加规则项并以内嵌方式导出。" else "添加 HTTP(S) 规则集地址后，可刷新并预览本机缓存。")
                            else LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) { items(providers, key = { it.id }) { provider ->
                                val status = state.ruleProviderStatuses.firstOrNull { it.profileId == state.activeProfile.id && it.providerId == provider.id }
                                val refreshing = provider.id in viewModel.refreshingRuleProviderIds
                                Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = if (focusedProviderId == provider.id) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface)) { Column(Modifier.fillMaxWidth().padding(14.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Column(Modifier.weight(1f)) { Text(provider.name, style = MaterialTheme.typography.titleSmall); Text("${provider.behavior} · ${provider.format}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                                        if (provider.type == "http") TextButton(enabled = !refreshing, onClick = { viewModel.refreshRuleProvider(state.activeProfile, provider) }) { Text(if (refreshing) "刷新中…" else "刷新") }
                                        TextButton(onClick = { viewModel.previewRuleProvider(state.activeProfile, provider, status) }) { Text("预览") }
                                        TextButton(onClick = { onProviders(if (localOnly) "inline" else "http") }) { Text("管理") }
                                    }
                                    Text(status?.error?.let { "刷新失败：$it" } ?: status?.refreshedAt?.let { "上次刷新 ${java.text.DateFormat.getDateTimeInstance().format(java.util.Date(it))} · ${status.itemCount?.let { count -> "$count 项" } ?: "内容已缓存"}" } ?: if (provider.type == "inline") "内嵌内容 ${provider.payload.size} 项" else "尚未刷新", style = MaterialTheme.typography.labelSmall, color = if (status?.error != null) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant)
                                } }
                            } }
                        }
                    }
                }
                3 -> Column(Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
                    HeaderCard("子规则", "子规则集合及其引用都属于当前配置。")
                    OutlinedButton(onClick = { onProviders(null) }, modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) { Text("管理子规则") }
                    LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) { items(state.ruleProfile.subRules, key = { it.name }) { sub ->
                        val references = state.ruleProfile.rules.count { it.type == "SUB-RULE" && it.value == sub.name }
                        Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = if (focusedSubRuleName == sub.name) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface)) { Column(Modifier.padding(14.dp)) { Text(sub.name, style = MaterialTheme.typography.titleSmall); Text("${sub.rules.size} 条规则 · 被引用 $references 次", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant); sub.rules.take(3).forEach { Text("${it.type} · ${it.value} → ${it.group}", style = MaterialTheme.typography.labelSmall) } } }
                    } }
                    if (state.ruleProfile.subRules.isEmpty()) EmptyCard("没有子规则", "从规则集管理中创建子规则集合。")
                }
                else -> Column(Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
                    HeaderCard("配置校验", "检查目标策略、资源引用、条件格式和兜底顺序。")
                    if (issues.isEmpty()) EmptyCard("规则配置正常", "当前规则及其引用没有发现静态校验问题。")
                    else LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) { items(issues) { issue ->
                        Card(onClick = {
                            when {
                                issue.index >= 0 && issue.index in state.ruleProfile.rules.indices -> onEditRule(issue.index, state.ruleProfile.rules[issue.index])
                                issue.providerId != null -> { advancedSection = 2; focusedProviderId = issue.providerId }
                                issue.subRuleName != null -> { advancedSection = 3; focusedSubRuleName = issue.subRuleName }
                                issue.groupName != null -> { advancedSection = 1; state.ruleProfile.groups.firstOrNull { it.name == issue.groupName }?.let(onEditGroup) }
                            }
                        }, modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = if (issue.warning) MaterialTheme.colorScheme.tertiaryContainer else MaterialTheme.colorScheme.errorContainer)) {
                            val issueColor = if (issue.warning) MaterialTheme.colorScheme.onTertiaryContainer else MaterialTheme.colorScheme.onErrorContainer
                            Column(Modifier.padding(14.dp)) { Text("${if (issue.warning) "可能冲突 · " else ""}${if (issue.index >= 0) "第 ${issue.index + 1} 条 · ${issue.rule.type}" else issue.rule.type.ifBlank { "配置" }}", style = MaterialTheme.typography.labelLarge, color = issueColor); Text(issue.message, style = MaterialTheme.typography.bodySmall, color = issueColor) }
                        }
                    } }
                }
            }
        }
    }
    if (deleting) JichangAlertDialog(
        onDismissRequest = { deleting = false },
        title = { Text("删除所选规则？") },
        text = { Text("将从当前配置中删除 ${selectedRules.size} 条规则，此操作无法撤销。") },
        confirmButton = { TextButton(onClick = { viewModel.removeRules(selectedRules); selectedRules = emptySet(); selecting = false; deleting = false }) { Text("删除", color = MaterialTheme.colorScheme.error) } },
        dismissButton = { TextButton(onClick = { deleting = false }) { Text("取消") } },
    )
    deletingIndex?.let { index -> state.ruleProfile.rules.getOrNull(index)?.let { rule -> JichangAlertDialog(
        onDismissRequest = { deletingIndex = null },
        title = { Text("删除规则？") },
        text = { Text("${rule.type} · ${ruleSummary(rule)} → ${rule.group}") },
        confirmButton = { TextButton(onClick = { viewModel.removeRule(index); deletingIndex = null }) { Text("删除", color = MaterialTheme.colorScheme.error) } },
        dismissButton = { TextButton(onClick = { deletingIndex = null }) { Text("取消") } },
    ) } }
    groupToDelete?.let { group -> JichangAlertDialog(
        onDismissRequest = { groupToDelete = null },
        title = { Text("删除策略组？") },
        text = { Text("主规则和子规则中指向“${group.name}”的目标都会改为其他现有策略组。") },
        confirmButton = { TextButton(onClick = { viewModel.removeGroup(group.name); groupToDelete = null }) { Text("删除") } },
        dismissButton = { TextButton(onClick = { groupToDelete = null }) { Text("取消") } },
    ) }
    if (showSimulator) RuleSimulationDialog(state.ruleProfile, onDismiss = { showSimulator = false })
    if (showTemplateRuleSetApply) TemplateRuleSetApplyDialog(
        templates = state.templates,
        current = state.ruleProfile,
        onDismiss = { showTemplateRuleSetApply = false },
        onApply = { updated -> onSaveRuleProfile(updated); showTemplateRuleSetApply = false },
    )
    basicEditing?.let { edit -> BasicRuleDialog(state, edit, onDismiss = { basicEditing = null }, onSave = { index, rule -> if (index == null) viewModel.addRule(rule) else viewModel.updateRule(index, rule); basicEditing = null }) }
    viewModel.ruleProviderPreview?.let { (name, content) ->
        JichangAlertDialog(onDismissRequest = viewModel::dismissRuleProviderPreview, title = { Text("规则集预览：$name") }, text = {
            Column(Modifier.heightIn(max = 540.dp).verticalScroll(rememberScrollState())) {
                Text(content, style = MaterialTheme.typography.bodySmall, fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace)
            }
        }, confirmButton = { TextButton(onClick = viewModel::dismissRuleProviderPreview) { Text("关闭") } })
    }
}

@Composable
private fun TemplateRuleSetApplyDialog(
    templates: List<com.jzb.jichang.android.model.ConfigTemplate>,
    current: RuleProfile,
    onDismiss: () -> Unit,
    onApply: (RuleProfile) -> Unit,
) {
    val names = templates.map { it.name }
    var templateName by remember(templates) { mutableStateOf(names.firstOrNull().orEmpty()) }
    val template = templates.firstOrNull { it.name == templateName }
    val parsed = remember(template?.id, template?.rawYaml) {
        template?.let { runCatching { MihomoTemplateParser().parse(it.rawYaml) }.getOrNull() }
    }
    val providers = parsed?.ruleProfile?.providers.orEmpty()
    val sourceRules = parsed?.ruleProfile?.rules.orEmpty()
    var selected by remember(template?.id, providers) { mutableStateOf(emptySet<String>()) }
    var conflictActions by remember(template?.id, current.providers) { mutableStateOf(emptyMap<String, String>()) }
    var renamed by remember(template?.id) { mutableStateOf(emptyMap<String, String>()) }
    var importError by remember(template?.id) { mutableStateOf<String?>(null) }
    val defaultTarget = current.groups.firstOrNull { it.name.equals("PROXY", true) }?.name ?: current.groups.firstOrNull()?.name ?: "DIRECT"
    val conflicts = providers.associate { provider ->
        provider.id to current.providers.any { it.name.equals("$templateName · ${provider.name}", true) }
    }
    val unresolvedConflict = selected.any { id ->
        conflicts[id] == true && (conflictActions[id].isNullOrBlank() ||
            (conflictActions[id] == "改名导入项" && renamed[id].isNullOrBlank()))
    }
    val selectedForImport = providers.filter { it.id in selected && conflictActions[it.id] != "保留现有" }
    val chosenNames = selectedForImport.map { provider ->
        if (conflictActions[provider.id] == "改名导入项") renamed[provider.id]?.trim().orEmpty() else "$templateName · ${provider.name}"
    }
    val duplicateName = chosenNames.any(String::isBlank) || chosenNames.map(String::lowercase).toSet().size != chosenNames.size ||
        selectedForImport.any { provider ->
            val finalName = if (conflictActions[provider.id] == "改名导入项") renamed[provider.id]?.trim().orEmpty() else "$templateName · ${provider.name}"
            conflictActions[provider.id] != "替换现有" && current.providers.any { it.name.equals(finalName, true) }
        }
    JichangAlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("应用模板规则集") },
        text = { Column(Modifier.heightIn(max = 580.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            if (templates.isEmpty()) {
                Text("资源中还没有已下载的配置模板。")
            } else {
                SelectField("来源模板", templateName, names) { selectedName ->
                    templateName = selectedName
                    selected = emptySet()
                    conflictActions = emptyMap()
                    renamed = emptyMap()
                    importError = null
                }
                if (parsed == null) Text("此模板无法解析为 Mihomo YAML。", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                else if (providers.isEmpty()) Text("模板中没有可应用的规则集。", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                providers.forEach { provider ->
                    val baseName = "$templateName · ${provider.name}"
                    val hasConflict = conflicts[provider.id] == true
                    Column(Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(checked = provider.id in selected, onCheckedChange = { checked ->
                                selected = if (checked) selected + provider.id else selected - provider.id
                            })
                            Column(Modifier.weight(1f)) {
                                Text(provider.name, style = MaterialTheme.typography.bodyMedium)
                                Text("${provider.type} · ${provider.behavior} · ${provider.format} · ${sourceRules.count { it.type.equals("RULE-SET", true) && it.value == provider.name }} 条关联规则", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                        if (provider.id in selected && hasConflict) {
                            Text("当前配置已有“$baseName”", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.error)
                            SelectField("同名处理", conflictActions[provider.id] ?: "请选择处理方式", listOf("请选择处理方式", "保留现有", "替换现有", "改名导入项")) { action ->
                                conflictActions = conflictActions + (provider.id to action.takeUnless { it == "请选择处理方式" }.orEmpty())
                            }
                            if (conflictActions[provider.id] == "改名导入项") OutlinedTextField(
                                value = renamed[provider.id] ?: "${baseName} 副本",
                                onValueChange = { renamed = renamed + (provider.id to it) },
                                modifier = Modifier.fillMaxWidth(),
                                label = { Text("导入名称") },
                                singleLine = true,
                            )
                        }
                    }
                }
                if (providers.isNotEmpty()) Text("应用时会复制选中规则集及模板中引用它们的 RULE-SET 规则；模板中不存在的目标策略会改用“$defaultTarget”。", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (selected.isNotEmpty() && duplicateName) Text("导入名称必须唯一；请为冲突项改名，或选择替换现有规则集。", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                importError?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
            }
        } },
        confirmButton = { TextButton(enabled = template != null && selected.isNotEmpty() && !unresolvedConflict && !duplicateName, onClick = {
            val selectedTemplate = template ?: return@TextButton
            val actions = selected.mapNotNull { id -> conflictActions[id]?.let { action ->
                id to when (action) {
                    "保留现有" -> com.jzb.jichang.android.service.TemplateProviderConflictAction.KEEP_EXISTING
                    "替换现有" -> com.jzb.jichang.android.service.TemplateProviderConflictAction.REPLACE
                    "改名导入项" -> com.jzb.jichang.android.service.TemplateProviderConflictAction.RENAME
                    else -> null
                }
            } }.mapNotNull { (id, action) -> action?.let { id to it } }.toMap()
            val result = runCatching {
                com.jzb.jichang.android.service.TemplateRuleSetImporter.apply(current, selectedTemplate, selected, actions, renamed)
            }
            result.onSuccess(onApply).onFailure { importError = it.message ?: "模板规则集导入失败" }
        }) { Text("应用") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } },
    )
}

@Composable
private fun BasicRuleCard(rule: RoutingRule, index: Int, canMoveUp: Boolean, canMoveDown: Boolean, isAdvanced: Boolean, onEdit: () -> Unit, onMoveUp: () -> Unit, onMoveDown: () -> Unit, onDelete: () -> Unit) {
    Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Row(Modifier.padding(start = 14.dp, end = 6.dp, top = 10.dp, bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("${index + 1}. ${basicRuleTitle(rule)}", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
                Text(basicRuleSummary(rule), style = MaterialTheme.typography.bodyMedium, maxLines = 2)
                Text("目标：${rule.group}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (isAdvanced) TextButton(onClick = onEdit, contentPadding = PaddingValues(horizontal = 0.dp, vertical = 0.dp)) { Text("在高级工作区查看 / 编辑", style = MaterialTheme.typography.labelSmall) }
            }
            Column {
                IconButton(enabled = canMoveUp, onClick = onMoveUp) { Icon(Icons.Outlined.KeyboardArrowUp, "上移规则") }
                IconButton(enabled = canMoveDown, onClick = onMoveDown) { Icon(Icons.Outlined.KeyboardArrowDown, "下移规则") }
            }
            Box {
                var expanded by remember { mutableStateOf(false) }
                IconButton(onClick = { expanded = true }) { Icon(Icons.Outlined.MoreVert, "规则操作") }
                DropdownMenu(expanded, { expanded = false }) {
                    DropdownMenuItem(text = { Text(if (isAdvanced) "在高级工作区编辑" else "编辑") }, onClick = { expanded = false; onEdit() })
                    DropdownMenuItem(text = { Text("删除") }, onClick = { expanded = false; onDelete() })
                }
            }
        }
    }
}

private fun basicRuleTitle(rule: RoutingRule) = when (rule.type.uppercase()) {
    "DOMAIN" -> "指定域名"; "DOMAIN-SUFFIX" -> "域名及子域名"; "DOMAIN-KEYWORD" -> "域名关键词"; "GEOSITE" -> "地理站点分类"
    "IP-CIDR", "IP-CIDR6" -> "IP 网段"; "GEOIP" -> "国家/地区 IP"; "RULE-SET" -> "规则集"; "SUB-RULE" -> "子规则集合"; "MATCH" -> "最终兜底"
    else -> rule.type
}

private fun basicRuleSummary(rule: RoutingRule) = when (rule.type.uppercase()) {
    "RULE-SET" -> "使用规则集“${rule.value}”"; "SUB-RULE" -> "匹配子规则“${rule.value}”"; "MATCH" -> "其余未匹配流量"
    else -> rule.value.ifBlank { "打开高级工作区查看匹配条件" }
}

private fun isBasicWorkspaceRule(rule: RoutingRule): Boolean =
    rule.type.uppercase() in setOf("DOMAIN", "DOMAIN-SUFFIX", "DOMAIN-KEYWORD", "GEOSITE", "IP-CIDR", "IP-CIDR6", "GEOIP", "RULE-SET", "MATCH") &&
        rule.conditions.isEmpty() && rule.rawLine.isNullOrBlank() && rule.extraParameters.isEmpty() && !rule.noResolve && !rule.source

@Composable
private fun BasicRuleDialog(state: AppState, editing: Pair<Int?, RoutingRule?>, onDismiss: () -> Unit, onSave: (Int?, RoutingRule) -> Unit) {
    val index = editing.first
    val initial = editing.second
    var type by remember(index) { mutableStateOf(initial?.type ?: "DOMAIN-SUFFIX") }
    var value by remember(index) { mutableStateOf(initial?.value.orEmpty()) }
    var group by remember(index) { mutableStateOf(initial?.group ?: state.ruleProfile.groups.firstOrNull()?.name ?: "DIRECT") }
    val options = listOf("DOMAIN", "DOMAIN-SUFFIX", "DOMAIN-KEYWORD", "GEOSITE", "IP-CIDR", "IP-CIDR6", "GEOIP", "RULE-SET", "MATCH")
    val groupNames = state.ruleProfile.groups.map { it.name } + listOf("DIRECT", "REJECT")
    val choices = if (type == "RULE-SET") state.ruleProfile.providers.map { it.name } else emptyList()
    val valid = when (type) {
        "MATCH" -> group in groupNames
        "RULE-SET" -> value in choices && group in groupNames
        else -> value.isNotBlank() && ',' !in value && '\n' !in value && '\r' !in value && group in groupNames
    }
    JichangAlertDialog(onDismissRequest = onDismiss, title = { Text(if (index == null) "添加常用规则" else "编辑常用规则") }, text = {
        Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            SelectField("匹配方式", type, options) { selected -> type = selected; if (selected == "RULE-SET") value = choices.firstOrNull().orEmpty() }
            when (type) {
                "RULE-SET" -> if (choices.isEmpty()) Text("先在高级规则管理中添加规则集。", color = MaterialTheme.colorScheme.error) else SelectField("规则集", value, choices) { value = it }
                "MATCH" -> Text("匹配前面规则都未命中的流量。兜底规则由应用固定放在末尾。", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                else -> OutlinedTextField(value, { value = it }, Modifier.fillMaxWidth(), label = { Text(when (type) { "GEOSITE" -> "Geosite 分类"; "GEOIP" -> "国家/地区代码"; "IP-CIDR", "IP-CIDR6" -> "CIDR 网段"; else -> "匹配内容" }) }, singleLine = true)
            }
            SelectField("目标策略", group, groupNames) { group = it }
            Text("更复杂的规则类型、组合条件和高级参数请进入高级规则管理。", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }, confirmButton = { TextButton(enabled = valid, onClick = {
        val rule = (initial ?: RoutingRule(type, value, group)).copy(type = type, value = value.trim(), group = group)
        onSave(index, rule)
    }) { Text(if (index == null) "添加规则" else "保存") } }, dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } })
}

@Composable
private fun RuleSimulationDialog(profile: com.jzb.jichang.android.model.RuleProfile, onDismiss: () -> Unit) {
    var host by remember { mutableStateOf("") }
    var destinationPort by remember { mutableStateOf("") }
    var sourcePort by remember { mutableStateOf("") }
    var network by remember { mutableStateOf("") }
    var processName by remember { mutableStateOf("") }
    var result by remember { mutableStateOf<com.jzb.jichang.android.service.RuleSimulationResult?>(null) }
    val certainty = result?.certainty
    JichangAlertDialog(onDismissRequest = onDismiss, title = { Text("规则模拟 · 静态推演") }, text = {
        Column(Modifier.heightIn(max = 560.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("仅按当前规则和已填写条件推演，不代表 Mihomo 运行时结果。GeoIP、GeoSite 和外部规则集等缺少数据时会标记为未知。", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            OutlinedTextField(host, { host = it }, Modifier.fillMaxWidth(), label = { Text("域名或目标 IP") }, singleLine = true)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(destinationPort, { destinationPort = it.filter(Char::isDigit) }, Modifier.weight(1f), label = { Text("目标端口") }, singleLine = true)
                OutlinedTextField(sourcePort, { sourcePort = it.filter(Char::isDigit) }, Modifier.weight(1f), label = { Text("来源端口") }, singleLine = true)
            }
            SelectField("网络协议", network.ifBlank { "不指定" }, listOf("不指定", "TCP", "UDP")) { network = if (it == "不指定") "" else it }
            OutlinedTextField(processName, { processName = it }, Modifier.fillMaxWidth(), label = { Text("进程名（可选）") }, singleLine = true)
            OutlinedButton(enabled = host.isNotBlank(), onClick = {
                result = com.jzb.jichang.android.service.RuleSimulator.simulate(profile, com.jzb.jichang.android.service.RuleSimulationInput(
                    hostOrIp = host,
                    destinationPort = destinationPort.toIntOrNull(), sourcePort = sourcePort.toIntOrNull(),
                    network = network.takeIf(String::isNotBlank), processName = processName.takeIf(String::isNotBlank),
                ))
            }, modifier = Modifier.fillMaxWidth()) { Text("模拟") }
            result?.let { outcome ->
                Card(colors = CardDefaults.cardColors(containerColor = when (outcome.certainty) { com.jzb.jichang.android.service.RuleSimulationCertainty.DEFINITE -> MaterialTheme.colorScheme.primaryContainer; com.jzb.jichang.android.service.RuleSimulationCertainty.NO_MATCH -> MaterialTheme.colorScheme.surfaceVariant; else -> MaterialTheme.colorScheme.tertiaryContainer })) {
                    Column(Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(when (outcome.certainty) { com.jzb.jichang.android.service.RuleSimulationCertainty.DEFINITE -> "确定命中"; com.jzb.jichang.android.service.RuleSimulationCertainty.CONDITIONAL -> "结果可能受未知条件影响"; com.jzb.jichang.android.service.RuleSimulationCertainty.UNKNOWN -> "无法确定最终结果"; com.jzb.jichang.android.service.RuleSimulationCertainty.NO_MATCH -> "无可确定匹配" }, style = MaterialTheme.typography.titleSmall)
                        outcome.matchedRule?.let { Text("第 ${outcome.matchedIndex?.plus(1)} 条 · ${it.type} → ${it.group}", style = MaterialTheme.typography.bodyMedium) }
                        Text(outcome.explanation, style = MaterialTheme.typography.bodySmall)
                    }
                }
                if (outcome.uncertainIndexes.isNotEmpty()) {
                    Text("可能改变结论的规则", style = MaterialTheme.typography.labelLarge)
                    outcome.uncertainIndexes.forEach { index -> profile.rules.getOrNull(index)?.let { Text("第 ${index + 1} 条 · ${it.type} → ${it.group} · 依赖外部数据或未提供条件", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) } }
                }
            }
        }
    }, confirmButton = { TextButton(onClick = onDismiss) { Text("完成") } })
}

@Composable
private fun RuleListCard(rule: RoutingRule, onEdit: () -> Unit, onMoveUp: () -> Unit, onMoveDown: () -> Unit, onDelete: () -> Unit, canMoveUp: Boolean, canMoveDown: Boolean, selectable: Boolean, checked: Boolean, onCheckedChange: (Boolean) -> Unit, onDuplicate: () -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    val dragThreshold = with(LocalDensity.current) { 64.dp.toPx() }
    var dragRemainder by remember(rule, selectable) { mutableStateOf(0f) }
    Card(onClick = { if (!selectable) onEdit() }, modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Row(Modifier.padding(start = 14.dp, end = 4.dp, top = 8.dp, bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            if (selectable) Checkbox(checked = checked, onCheckedChange = onCheckedChange)
            Column(Modifier.weight(1f)) {
                Text(if (rule.type == "MATCH") "最终兜底 · MATCH" else rule.type, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
                Text(ruleSummary(rule), style = MaterialTheme.typography.bodyMedium, maxLines = 2)
                val parameters = buildList { if (rule.noResolve) add("no-resolve"); if (rule.source) add("src"); addAll(rule.extraParameters) }
                Text("策略  ${rule.group}${parameters.takeIf { it.isNotEmpty() }?.joinToString(prefix = " · 参数 ", separator = ", ").orEmpty()}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (!selectable && rule.type != "MATCH") Icon(Icons.Outlined.DragHandle, contentDescription = "长按拖动调整顺序", modifier = Modifier
                .size(40.dp)
                .pointerInput(rule, canMoveUp, canMoveDown) {
                    detectDragGesturesAfterLongPress(
                        onDragStart = { dragRemainder = 0f },
                        onDragEnd = { dragRemainder = 0f },
                        onDragCancel = { dragRemainder = 0f },
                        onDrag = { change, amount ->
                            change.consume()
                            dragRemainder += amount.y
                            while (dragRemainder >= dragThreshold && canMoveDown) { onMoveDown(); dragRemainder -= dragThreshold }
                            while (dragRemainder <= -dragThreshold && canMoveUp) { onMoveUp(); dragRemainder += dragThreshold }
                        },
                    )
                })
            Box {
                IconButton(onClick = { expanded = true }) { Icon(Icons.Outlined.MoreVert, "规则操作") }
                DropdownMenu(expanded, { expanded = false }) {
                    DropdownMenuItem(text = { Text("编辑") }, onClick = { expanded = false; onEdit() })
                    DropdownMenuItem(enabled = rule.type != "MATCH", text = { Text("复制") }, onClick = { expanded = false; onDuplicate() })
                    DropdownMenuItem(text = { Text("上移") }, enabled = canMoveUp, onClick = { expanded = false; onMoveUp() })
                    DropdownMenuItem(text = { Text("下移") }, enabled = canMoveDown, onClick = { expanded = false; onMoveDown() })
                    DropdownMenuItem(text = { Text("删除") }, onClick = { expanded = false; onDelete() })
                }
            }
        }
    }
}

private fun ruleSummary(rule: RoutingRule): String = when (rule.type) {
    "AND", "OR", "NOT" -> if (rule.conditions.isEmpty()) rule.rawLine ?: "未解析组合表达式" else "${rule.conditions.size} 个条件"
    "RULE-SET" -> "规则集 · ${rule.value}"
    "SUB-RULE" -> "子规则 · ${rule.value}"
    "MATCH" -> "其余所有流量"
    else -> rule.value
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
    unresolvedTemplateProviders: List<String>,
    templateParameters: List<TemplateSubscriptionParameter>,
    templateBindings: Map<String, String>,
    ruleIssues: List<com.jzb.jichang.android.service.RuleIssue>,
    hasRegionalProxyGroups: Boolean,
    onTemplateBinding: (String, String?) -> Unit,
    onModeChange: (ConfigSourceMode) -> Unit,
    onOpenFilters: () -> Unit,
    onDownload: () -> Unit,
    onShare: () -> Unit,
    onStopShare: () -> Unit,
    onShareLink: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val profile = state.activeProfile
    val blockingRuleIssues = ruleIssues.filterNot { it.warning }
    Column(modifier.padding(horizontal = JichangSpacing.pageHorizontal).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(JichangSpacing.section)) {
        Card(Modifier.fillMaxWidth().padding(top = 6.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
            Column(Modifier.padding(JichangSpacing.card), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(profile.name, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text("${generatedNodes} 个内嵌节点${if (referencedSubscriptions > 0) " · $referencedSubscriptions 个订阅引用" else ""}${if (skippedNodes > 0) " · 跳过 $skippedNodes 个不支持节点" else ""}", style = MaterialTheme.typography.bodySmall)
                Text("文件名：$filename", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Text("节点来源", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        if (ruleIssues.isNotEmpty()) Card(colors = CardDefaults.cardColors(containerColor = if (blockingRuleIssues.isEmpty()) MaterialTheme.colorScheme.tertiaryContainer else MaterialTheme.colorScheme.errorContainer)) {
            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                val color = if (blockingRuleIssues.isEmpty()) MaterialTheme.colorScheme.onTertiaryContainer else MaterialTheme.colorScheme.onErrorContainer
                Text("规则校验：${blockingRuleIssues.size} 个错误 · ${ruleIssues.size - blockingRuleIssues.size} 个可能冲突", style = MaterialTheme.typography.titleSmall, color = color)
                ruleIssues.forEach { issue -> Text("${if (issue.index >= 0) "#${issue.index + 1} ${issue.rule.type}" else issue.rule.type.ifBlank { "配置" }}：${issue.message}", style = MaterialTheme.typography.bodySmall, color = color) }
                if (blockingRuleIssues.isNotEmpty()) Text("请返回规则页修正错误后再下载或分享；可能冲突仅供检查。", style = MaterialTheme.typography.bodySmall, color = color)
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            SourceModeCard("内嵌节点", "节点写进 YAML", options.sourceMode == ConfigSourceMode.EMBED_NODES, Modifier.weight(1f).fillMaxHeight()) { onModeChange(ConfigSourceMode.EMBED_NODES) }
            SourceModeCard("引用订阅", "Mihomo 远程更新", options.sourceMode == ConfigSourceMode.REFERENCE_SUBSCRIPTIONS, Modifier.weight(1f).fillMaxHeight()) { onModeChange(ConfigSourceMode.REFERENCE_SUBSCRIPTIONS) }
        }
        if (options.sourceMode == ConfigSourceMode.REFERENCE_SUBSCRIPTIONS && state.sources.any { it.id in profile.selectedSourceIds }) {
            Text(
                if (referencedSubscriptions > 0) "订阅地址会写入配置文件；文件接收者可以使用这些订阅。" else "当前没有可远程引用的 Mihomo YAML 订阅；节点将以内嵌方式导出。",
                color = if (referencedSubscriptions > 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall,
            )
        }
        val referenceMode = options.sourceMode == ConfigSourceMode.REFERENCE_SUBSCRIPTIONS
        val compatibleSelectedSources = state.sources.filter { it.id in profile.selectedSourceIds && it.providerCompatible == true }
        if (referenceMode && templateParameters.isNotEmpty() && compatibleSelectedSources.isEmpty()) {
            Text("当前没有可引用的 Mihomo 订阅；模板占位 provider 会从导出中移除，节点按内嵌模式处理。", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        if (referenceMode && templateParameters.isNotEmpty() && compatibleSelectedSources.size == 1) {
            Text("模板机场参数使用唯一的兼容订阅：${compatibleSelectedSources.single().name}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        if (referenceMode && templateParameters.isNotEmpty() && compatibleSelectedSources.size > 1) {
            Text("模板机场绑定", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Text("绑定到资源中的 Mihomo 订阅。链接保存在订阅资源中，模板不会保存或改写机场凭据。", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            templateParameters.forEach { parameter ->
                TemplateProviderBindingRow(
                    parameter = parameter,
                    selectedSourceId = templateBindings[parameter.providerName]?.takeIf { id -> compatibleSelectedSources.any { it.id == id } },
                    sources = compatibleSelectedSources,
                    onSelect = { onTemplateBinding(parameter.providerName, it) },
                )
            }
        }
        if (!hasRegionalProxyGroups) OutlinedButton(onClick = onOpenFilters, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Outlined.Tune, null); Spacer(Modifier.width(8.dp)); Text("导出地区策略组")
        } else Text("模板已包含地区筛选策略组，直接沿用模板分组。", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        if (unresolvedTemplateProviders.isNotEmpty()) Text(
            "尚未绑定：${unresolvedTemplateProviders.joinToString("、")}。请启用兼容订阅并完成绑定后再导出。",
            color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Button(onClick = onDownload, enabled = unresolvedTemplateProviders.isEmpty() && blockingRuleIssues.isEmpty(), modifier = Modifier.weight(1f).height(52.dp), contentPadding = PaddingValues(horizontal = 8.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.CloudDownload, null, Modifier.size(18.dp)); Spacer(Modifier.width(5.dp))
                    Text("下载配置", maxLines = 1, softWrap = false, overflow = TextOverflow.Ellipsis)
                }
            }
            OutlinedButton(onClick = onShare, enabled = unresolvedTemplateProviders.isEmpty() && blockingRuleIssues.isEmpty(), modifier = Modifier.weight(1f).height(52.dp), contentPadding = PaddingValues(horizontal = 8.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.Link, null, Modifier.size(18.dp)); Spacer(Modifier.width(5.dp))
                    Text(if (shareUrl == null) "开启分享" else "重新分享", maxLines = 1, softWrap = false, overflow = TextOverflow.Ellipsis)
                }
            }
        }
        if (shareUrl == null) {
            if (shareError != null) Text(shareError, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
        }
        shareUrl?.let { url -> Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
            Column(Modifier.padding(JichangSpacing.card), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("局域网分享已开启", style = MaterialTheme.typography.titleSmall)
                LanShareQrCodeCard(url)
                SelectionContainer { Text(url, style = MaterialTheme.typography.bodySmall, fontFamily = FontFamily.Monospace) }
                Text("接收设备需连接可互访的同一局域网。访客 Wi-Fi 或 VPN 可能阻断设备间连接。二维码和随机链接都可访问配置，请只展示给信任的人。", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = onShareLink, modifier = Modifier.weight(1f)) { Icon(Icons.Outlined.Share, null); Spacer(Modifier.width(5.dp)); Text("分享链接") }
                    TextButton(onClick = onStopShare) { Text("停止") }
                }
            }
        } }
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
private fun TemplateProviderBindingRow(
    parameter: TemplateSubscriptionParameter,
    selectedSourceId: String?,
    sources: List<com.jzb.jichang.android.model.SubscriptionSource>,
    onSelect: (String?) -> Unit,
) {
    val selected = sources.firstOrNull { it.id == selectedSourceId }
    var expanded by remember(parameter.providerName) { mutableStateOf(false) }
    Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(parameter.providerName, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            Box {
                OutlinedButton(onClick = { expanded = true }, modifier = Modifier.fillMaxWidth()) {
                    Text(selected?.name ?: "选择订阅资源", modifier = Modifier.weight(1f), textAlign = TextAlign.Start, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Icon(Icons.Outlined.KeyboardArrowDown, null)
                }
                DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                    sources.forEach { source -> DropdownMenuItem(text = { Text(source.name) }, onClick = { onSelect(source.id); expanded = false }) }
                    if (selectedSourceId != null) DropdownMenuItem(text = { Text("解除绑定") }, onClick = { onSelect(null); expanded = false })
                }
            }
            if (selected == null) Text("未绑定或原订阅已删除。请先在资源页启用一个兼容的 Mihomo 订阅。", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun LanShareQrCodeCard(url: String) {
    val result by produceState<Result<Bitmap>?>(initialValue = null, key1 = url) {
        value = withContext(Dispatchers.Default) { runCatching { LanShareQrCode.createBitmap(url) } }
    }
    Column(
        Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        when {
            result == null -> CircularProgressIndicator(Modifier.size(36.dp), strokeWidth = 3.dp)
            result!!.isSuccess -> Image(
                bitmap = result!!.getOrThrow().asImageBitmap(),
                contentDescription = "局域网配置下载二维码",
                contentScale = ContentScale.Fit,
                modifier = Modifier.size(232.dp).clip(RoundedCornerShape(12.dp)),
            )
            else -> Text(
                "二维码生成失败，可使用下方链接分享。",
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
            )
        }
        Text("用同一局域网内的设备扫描下载配置", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun TemplatesPage(
    templates: List<com.jzb.jichang.android.model.ConfigTemplate>,
    profiles: List<ConfigProfile>,
    downloading: Boolean,
    progress: Float?,
    status: String?,
    error: String?,
    onAdd: () -> Unit,
    onPreview: (com.jzb.jichang.android.model.ConfigTemplate) -> Unit,
    onCreate: (com.jzb.jichang.android.model.ConfigTemplate) -> Unit,
    onRename: (com.jzb.jichang.android.model.ConfigTemplate) -> Unit,
    onDelete: (com.jzb.jichang.android.model.ConfigTemplate) -> Unit,
    onExport: (com.jzb.jichang.android.model.ConfigTemplate) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(modifier, contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = JichangSpacing.pageHorizontal, vertical = JichangSpacing.pageVertical), verticalArrangement = Arrangement.spacedBy(JichangSpacing.section)) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("配置模板", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold)
                Text("从远程 Mihomo YAML 创建本地模板，再用它快速建立可编辑配置。模板仅保存在此设备。", color = MaterialTheme.colorScheme.onSurfaceVariant)
                Button(onClick = onAdd, enabled = !downloading, modifier = Modifier.fillMaxWidth()) {
                    if (downloading) CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary)
                    else Icon(Icons.Outlined.Add, null)
                    Spacer(Modifier.width(8.dp)); Text(if (downloading) "正在下载模板…" else "从链接添加模板")
                }
                if (downloading) {
                    if (progress == null) LinearProgressIndicator(Modifier.fillMaxWidth())
                    else LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth())
                }
                status?.let { Text(it, color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.bodySmall) }
                error?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
            }
        }
        if (templates.isEmpty()) item {
            EmptyCard("模板库还是空的", "使用上方按钮添加 Mihomo YAML 模板，以它为基础创建自己的配置。")
        }
        items(templates, key = { it.id }) { template ->
            val users = profiles.count { it.templateId == template.id }
            Card(onClick = { onPreview(template) }, modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                Column(Modifier.padding(JichangSpacing.card), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Outlined.Description, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(template.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                            Text("${template.fileName} · ${if (users == 0) "尚未使用" else "$users 个配置在使用"}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = { onCreate(template) }, modifier = Modifier.weight(1f)) { Text("用作模板新建配置") }
                        OutlinedButton(onClick = { onExport(template) }) { Text("导出") }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        TextButton(onClick = { onRename(template) }) { Text("重命名") }
                        TextButton(onClick = { onPreview(template) }) { Text("预览") }
                        TextButton(onClick = { onDelete(template) }) { Text("删除", color = MaterialTheme.colorScheme.error) }
                    }
                }
            }
        }
        item { Spacer(Modifier.height(12.dp)) }
    }
}

@Composable
private fun TemplatePreviewDialog(template: com.jzb.jichang.android.model.ConfigTemplate, onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        JichangDialogSurface(Modifier.fillMaxWidth(0.94f).widthIn(max = 760.dp).heightIn(max = 760.dp)) {
            Column(Modifier.padding(JichangSpacing.dialog), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(template.name, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        Text("${template.fileName} · 仅存储在本机", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    TextButton(onClick = onDismiss) { Text("关闭") }
                }
                SelectionContainer {
                    Text(template.rawYaml, Modifier.fillMaxWidth().weight(1f).verticalScroll(rememberScrollState()).background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(18.dp)).padding(14.dp), fontFamily = FontFamily.Monospace, style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}

@Composable
private fun TemplateCreateDialog(
    template: com.jzb.jichang.android.model.ConfigTemplate,
    onDismiss: () -> Unit,
    onCreate: (String, String) -> Unit,
) {
    var name by remember(template.id) { mutableStateOf("${template.name} 副本") }
    var fileName by remember(template.id) { mutableStateOf("${template.name} 副本") }
    JichangAlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("基于模板新建配置") },
        text = { Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("节点、策略组和规则会载入可视化编辑器；模板中的其他 Mihomo 字段会保留。")
            OutlinedTextField(name, { name = it; if (fileName == "${template.name} 副本") fileName = it }, modifier = Modifier.fillMaxWidth(), label = { Text("配置名称") }, singleLine = true)
            OutlinedTextField(fileName, { fileName = it }, modifier = Modifier.fillMaxWidth(), label = { Text("导出文件名") }, singleLine = true)
        } },
        confirmButton = { TextButton(onClick = { onCreate(name.trim(), fileName.trim()) }, enabled = name.isNotBlank() && fileName.isNotBlank()) { Text("创建") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } },
    )
}

@Composable
private fun TemplateRenameDialog(
    template: com.jzb.jichang.android.model.ConfigTemplate,
    onDismiss: () -> Unit,
    onRename: (String) -> Unit,
) {
    var name by remember(template.id) { mutableStateOf(template.name) }
    JichangAlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("重命名模板") },
        text = { OutlinedTextField(name, { name = it }, modifier = Modifier.fillMaxWidth(), label = { Text("模板名称") }, singleLine = true) },
        confirmButton = { TextButton(onClick = { onRename(name.trim()) }, enabled = name.isNotBlank()) { Text("保存") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } },
    )
}

@Composable
private fun RemoteConfigDialog(
    downloading: Boolean,
    progress: Float?,
    error: String?,
    onDismiss: () -> Unit,
    onDownload: (String, String) -> Unit,
) {
    var url by remember { mutableStateOf("") }
    var fileName by remember { mutableStateOf("") }
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        JichangDialogSurface(Modifier.fillMaxWidth(0.94f).widthIn(max = 560.dp).wrapContentHeight()) {
            Column(Modifier.padding(JichangSpacing.dialog), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("从链接添加模板", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold)
                Text("下载并在本机保存 Mihomo YAML 模板。模板可用于新建鸡场配置；链接不会保存。", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                OutlinedTextField(url, { url = it.trim() }, modifier = Modifier.fillMaxWidth(), label = { Text("配置链接") }, placeholder = { Text("https://example.com/config.yaml") }, singleLine = true, enabled = !downloading)
                OutlinedTextField(fileName, { fileName = it }, modifier = Modifier.fillMaxWidth(), label = { Text("模板名称（可选）") }, placeholder = { Text("默认使用远程文件名") }, singleLine = true, enabled = !downloading)
                if (downloading) {
                    if (progress == null) LinearProgressIndicator(Modifier.fillMaxWidth())
                    else LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth())
                }
                error?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically) {
                    TextButton(onClick = onDismiss, enabled = !downloading) { Text("取消") }
                    Spacer(Modifier.width(8.dp))
                    Button(onClick = { onDownload(url, fileName) }, enabled = url.isNotBlank() && !downloading) {
                        if (downloading) CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp) else Icon(Icons.Outlined.CloudDownload, null)
                        Spacer(Modifier.width(8.dp)); Text(if (downloading) "添加中…" else "下载为模板")
                    }
                }
            }
        }
    }
}

@Composable
private fun SourceModeCard(title: String, detail: String, selected: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Card(onClick = onClick, modifier = modifier, colors = CardDefaults.cardColors(containerColor = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface)) {
        Row(Modifier.fillMaxSize().padding(horizontal = 10.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            RadioButton(selected = selected, onClick = onClick)
            Column(Modifier.weight(1f).padding(start = 4.dp), verticalArrangement = Arrangement.Center) {
                Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                Text(detail, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun HeaderCard(title: String, detail: String) {
    Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(Modifier.padding(JichangSpacing.card), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Text(detail, style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun EmptyCard(title: String, detail: String) {
    Card(Modifier.fillMaxWidth().padding(vertical = 16.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(Modifier.fillMaxWidth().padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(detail, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun SourceDialog(onDismiss: () -> Unit, onSave: (String, String) -> Unit) {
    var name by remember { mutableStateOf("") }
    var url by remember { mutableStateOf("") }
    JichangAlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("添加订阅") },
        text = { Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(name, { name = it }, modifier = Modifier.fillMaxWidth(), label = { Text("名称（可选）") }, singleLine = true)
            OutlinedTextField(url, { url = it }, modifier = Modifier.fillMaxWidth(), label = { Text("HTTP(S) 订阅地址") }, singleLine = true)
        } },
        confirmButton = { TextButton(onClick = { onSave(name, url) }, enabled = url.isNotBlank()) { Text("添加并刷新") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } },
    )

}

@Composable
private fun SegmentedTabs(labels: List<String>, selected: Int, onSelect: (Int) -> Unit) {
    Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp).glassMaterial(RoundedCornerShape(20.dp)).padding(4.dp)) {
        labels.forEachIndexed { index, label ->
            TextButton(onClick = { onSelect(index) }, modifier = Modifier.weight(1f), colors = androidx.compose.material3.ButtonDefaults.textButtonColors(
                containerColor = if (selected == index) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.9f) else Color.Transparent,
            )) { Text(label, fontWeight = if (selected == index) FontWeight.SemiBold else FontWeight.Normal) }
        }
    }
}

@Composable
private fun NodeDialog(onDismiss: () -> Unit, onSave: (String) -> Unit) {
    var raw by remember { mutableStateOf("") }
    JichangAlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("导入节点") },
        text = { Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("粘贴 Mihomo/Clash YAML、Base64 订阅、节点链接或 Surge 节点行。")
            OutlinedTextField(raw, { raw = it }, Modifier.fillMaxWidth().heightIn(min = 160.dp, max = 240.dp), label = { Text("节点内容") }, minLines = 6)
        } },
        confirmButton = { TextButton(onClick = { onSave(raw) }, enabled = raw.isNotBlank()) { Text("解析并添加") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } },
    )
}

@Composable
private fun GroupDialog(
    state: AppState,
    initial: PolicyGroup?,
    onDismiss: () -> Unit,
    onSave: (String, String, List<String>, Set<Int>, Set<String>) -> Unit,
) {
    var name by remember(initial?.name) { mutableStateOf(initial?.name.orEmpty()) }
    var type by remember(initial?.name) { mutableStateOf(initial?.type ?: "select") }
    var expanded by remember { mutableStateOf(false) }
    val profile = state.activeProfile
    val eligibleNodes = state.nodes.filter { node ->
        (node.sourceId == null || node.sourceId in profile.selectedSourceIds) && node.id in profile.enabledNodeIds
    }
    val nodeNames = eligibleNodes.associate { "node:${it.id}" to it.name }
    val availableGroups = state.ruleProfile.groups.filter { it.name != initial?.name }
    val validMemberKeys = nodeNames.keys + availableGroups.map { it.name } + setOf("DIRECT", "REJECT")
    var members by remember(initial?.name, state.nodes, state.ruleProfile.groups, profile.enabledNodeIds, profile.selectedSourceIds) {
        val initialMembers = initial?.let { group ->
            when {
                group.membersExplicit -> group.members.toSet()
                group.members.isEmpty() -> nodeNames.keys
                else -> group.members.toSet()
            }
        } ?: nodeNames.keys
        mutableStateOf(initialMembers.intersect(validMemberKeys))
    }
    var selectedRules by remember(initial?.name, state.ruleProfile.rules) { mutableStateOf(emptySet<Int>()) }
    var selectedProviders by remember(initial?.name, state.ruleProfile.providers) { mutableStateOf(emptySet<String>()) }
    JichangAlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial == null) "新建策略组" else "编辑策略组") },
        text = { Column(Modifier.heightIn(max = 580.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(name, { name = it }, modifier = Modifier.fillMaxWidth(), label = { Text("组名称") }, singleLine = true)
            Box {
                OutlinedButton(onClick = { expanded = true }) { Text("类型：$type") }
                DropdownMenu(expanded, { expanded = false }) {
                    listOf("select", "url-test", "fallback", "load-balance").forEach { option -> DropdownMenuItem(text = { Text(option) }, onClick = { type = option; expanded = false }) }
                }
            }
            Text("代理成员", style = MaterialTheme.typography.titleSmall)
            Text("仅显示当前配置启用的节点和策略组。", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Column(Modifier.heightIn(max = 180.dp).verticalScroll(rememberScrollState())) {
                eligibleNodes.forEach { node ->
                    val key = "node:${node.id}"
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = key in members, onCheckedChange = { checked ->
                            members = if (checked) members + key else members - key
                        })
                        Text(node.name, style = MaterialTheme.typography.bodySmall)
                    }
                }
                availableGroups.forEach { group ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = group.name in members, onCheckedChange = { checked ->
                            members = if (checked) members + group.name else members - group.name
                        })
                        Text("策略组 · ${group.name}", style = MaterialTheme.typography.bodySmall)
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
            if (initial == null) {
                Text("规则目标", style = MaterialTheme.typography.titleSmall)
                Text("所选规则会改用新策略组；规则集会新增或更新对应的 RULE-SET 规则。", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Column(Modifier.heightIn(max = 160.dp).verticalScroll(rememberScrollState())) {
                    state.ruleProfile.rules.forEachIndexed { index, rule ->
                        if (!rule.type.equals("MATCH", true)) Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(checked = index in selectedRules, onCheckedChange = { checked ->
                                selectedRules = if (checked) selectedRules + index else selectedRules - index
                            })
                            Text("${rule.type} · ${ruleSummary(rule)} → ${rule.group}", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                    state.ruleProfile.providers.forEach { provider -> Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = provider.id in selectedProviders, onCheckedChange = { checked ->
                            selectedProviders = if (checked) selectedProviders + provider.id else selectedProviders - provider.id
                        })
                        Text("规则集 · ${provider.name}", style = MaterialTheme.typography.bodySmall)
                    } }
                }
            }
        } },
        confirmButton = { TextButton(onClick = { onSave(name, type, members.toList(), selectedRules, selectedProviders) }, enabled = name.isNotBlank()) { Text(if (initial == null) "添加" else "保存") } },
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
    var extraParameters by remember(initial) { mutableStateOf(existing?.extraParameters.orEmpty().joinToString("\n")) }
    var rawLine by remember(initial) { mutableStateOf(existing?.rawLine.orEmpty()) }
    var editorSection by remember(initial) { mutableStateOf(if (existing?.type in setOf("AND", "OR", "NOT") || existing?.extraParameters.orEmpty().isNotEmpty() || !existing?.rawLine.isNullOrBlank()) 1 else 0) }
    var showTypePicker by remember { mutableStateOf(false) }
    var strategyMenu by remember { mutableStateOf(false) }
    var geositeSearch by remember { mutableStateOf("") }
    val composite = type in setOf("AND", "OR", "NOT")
    val valueRequired = type !in setOf("MATCH", "AND", "OR", "NOT")
    val minimumConditions = if (type == "NOT") 1 else 2
    val valid = (!valueRequired || value.isNotBlank()) && (!composite || conditions.size >= minimumConditions || rawLine.isNotBlank())
    JichangAlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.fillMaxWidth(0.96f).widthIn(max = 620.dp),
        properties = DialogProperties(usePlatformDefaultWidth = false),
        title = { Text(if (initial == null) "新建路由规则" else "编辑路由规则") },
        text = { Column(Modifier.heightIn(max = 620.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(9.dp)) {
            SegmentedTabs(listOf("常用条件", "高级条件与参数"), editorSection) { editorSection = it }
            if (editorSection == 0) {
                Text("规则类型", style = MaterialTheme.typography.labelLarge)
                OutlinedButton(onClick = { showTypePicker = true }, modifier = Modifier.fillMaxWidth()) { Text(if (type == "MATCH") "MATCH · 最终兜底" else type) }
                when {
                    type == "MATCH" -> Text("MATCH 会被固定在规则列表末尾。", style = MaterialTheme.typography.bodySmall)
                    composite -> Text("此规则包含组合条件，请切换到“高级条件与参数”编辑条件树。", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    type == "RULE-SET" -> {
                        SelectField("规则集", value, state.ruleProfile.providers.map { it.name }, { value = it })
                        if (state.ruleProfile.providers.isEmpty()) Text("请先在高级规则管理的规则集页面添加提供者。", color = MaterialTheme.colorScheme.error)
                    }
                    type == "SUB-RULE" -> {
                        SelectField("子规则", value, state.ruleProfile.subRules.map { it.name }, { value = it })
                        if (state.ruleProfile.subRules.isEmpty()) Text("请先在高级规则管理的子规则页面添加子规则。", color = MaterialTheme.colorScheme.error)
                    }
                    type == "GEOSITE" -> {
                        val common = listOf("category-ads-all", "cn", "private", "google", "youtube", "apple", "microsoft", "telegram", "github", "geolocation-!cn")
                        OutlinedTextField(geositeSearch, { geositeSearch = it }, modifier = Modifier.fillMaxWidth(), label = { Text("搜索 GEOSITE 分类") }, singleLine = true)
                        OutlinedTextField(value, { value = it }, modifier = Modifier.fillMaxWidth(), label = { Text("匹配值") }, placeholder = { Text("可直接填写自定义分类") }, singleLine = true)
                        val suggestions = common.filter { geositeSearch.isBlank() || it.contains(geositeSearch, true) }
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) { suggestions.forEach { tag -> AssistChip(onClick = { value = tag }, label = { Text(tag) }) } }
                    }
                    else -> OutlinedTextField(value, { value = it }, modifier = Modifier.fillMaxWidth(), label = { Text(if (type.startsWith("PROCESS") || type == "UID") "进程 / 用户匹配" else "匹配内容") }, placeholder = { Text(rulePlaceholder(type)) }, singleLine = true)
                }
                Box {
                    OutlinedButton(onClick = { strategyMenu = true }, modifier = Modifier.fillMaxWidth()) { Text("策略目标：$group") }
                    DropdownMenu(strategyMenu, { strategyMenu = false }) {
                        (state.ruleProfile.groups.map { it.name } + listOf("DIRECT", "REJECT")).distinct().forEach { item -> DropdownMenuItem(text = { Text(item) }, onClick = { group = item; strategyMenu = false }) }
                    }
                }
                if (composite) TextButton(onClick = { editorSection = 1 }) { Text("前往高级条件编辑") }
            } else {
                if (composite) {
                    Text("条件 · ${if (type == "NOT") "只需 1 项" else "至少 2 项"}", style = MaterialTheme.typography.titleSmall)
                    conditions.forEachIndexed { index, condition ->
                        ConditionEditor(condition, state.ruleProfile.providers.map { it.name }, onChange = { changed -> conditions = conditions.toMutableList().also { it[index] = changed } }, onDelete = { conditions = conditions.filterIndexed { i, _ -> i != index } })
                    }
                    OutlinedButton(onClick = { conditions = conditions + RuleCondition(type = "DOMAIN-SUFFIX", value = "example.com") }, modifier = Modifier.fillMaxWidth()) { Text("添加子条件") }
                    OutlinedTextField(rawLine, { rawLine = it }, modifier = Modifier.fillMaxWidth(), label = { Text("原始组合规则（用于保留无法解析的导入表达式）") }, minLines = 2)
                }
                if (type in setOf("IP-CIDR", "IP-CIDR6", "IP-SUFFIX", "IP-ASN", "GEOIP", "SRC-GEOIP", "SRC-IP-ASN", "SRC-IP-CIDR", "SRC-IP-SUFFIX")) {
                    Row(verticalAlignment = Alignment.CenterVertically) { Checkbox(noResolve, { noResolve = it }); Text("no-resolve") }
                    if (type in setOf("IP-CIDR", "IP-CIDR6", "IP-SUFFIX", "IP-ASN", "GEOIP")) Row(verticalAlignment = Alignment.CenterVertically) { Checkbox(source, { source = it }); Text("按来源地址匹配 (src)") }
                }
                OutlinedTextField(extraParameters, { extraParameters = it }, modifier = Modifier.fillMaxWidth(), label = { Text("附加参数，每行一项") }, placeholder = { Text("例如：domain-suffix") }, minLines = 2)
                if (!composite && !rawLine.isNullOrBlank()) OutlinedTextField(rawLine, { rawLine = it }, modifier = Modifier.fillMaxWidth(), label = { Text("原始导入规则（只读保留，可编辑）") }, minLines = 2)
            }
        } },
        confirmButton = { TextButton(enabled = valid, onClick = { onSave(initial?.first, RoutingRule(type, value, group, noResolve, source, conditions, extraParameters.lines().map(String::trim).filter(String::isNotBlank), rawLine.takeIf(String::isNotBlank))) }) { Text(if (initial == null) "添加规则" else "保存") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } },
    )
    if (showTypePicker) RuleTypePickerDialog(type, onDismiss = { showTypePicker = false }, onSelect = { selected ->
        val previous = type
        type = selected
        if (previous in setOf("AND", "OR", "NOT") && selected !in setOf("AND", "OR", "NOT")) {
            conditions = emptyList()
            rawLine = ""
        }
        if (selected == "MATCH") value = ""
        if (selected == "RULE-SET") value = state.ruleProfile.providers.firstOrNull()?.name.orEmpty()
        if (selected == "SUB-RULE") value = state.ruleProfile.subRules.firstOrNull()?.name.orEmpty()
        showTypePicker = false
    })
}

@Composable
private fun RuleTypePickerDialog(selected: String, onDismiss: () -> Unit, onSelect: (String) -> Unit) {
    val categories = listOf("全部", "域名", "IP/地理", "端口/网络", "进程", "规则集", "组合")
    var query by remember { mutableStateOf("") }
    var category by remember { mutableStateOf(0) }
    val entries = visualRuleTypes.filter { type ->
        val categoryMatches = when (categories[category]) {
            "域名" -> type.startsWith("DOMAIN") || type == "GEOSITE"
            "IP/地理" -> type.contains("IP") || type.contains("GEO")
            "端口/网络" -> type.contains("PORT") || type in setOf("NETWORK", "IN-TYPE", "DSCP")
            "进程" -> type.startsWith("PROCESS") || type == "UID"
            "规则集" -> type in setOf("RULE-SET", "SUB-RULE")
            "组合" -> type in setOf("AND", "OR", "NOT", "MATCH")
            else -> true
        }
        categoryMatches && (query.isBlank() || type.contains(query, true))
    }
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        JichangDialogSurface(Modifier.fillMaxWidth(0.94f).widthIn(max = 560.dp).heightIn(max = 700.dp)) {
            Column(Modifier.padding(JichangSpacing.dialog)) {
                Text("选择规则类型", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold)
                OutlinedTextField(query, { query = it }, Modifier.fillMaxWidth().padding(top = 12.dp), label = { Text("搜索类型") }, singleLine = true)
                androidx.compose.foundation.lazy.LazyRow(contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(categories.size) { index -> androidx.compose.material3.FilterChip(selected = category == index, onClick = { category = index }, label = { Text(categories[index]) }) }
                }
                LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(entries) { type ->
                        Card(onClick = { onSelect(type) }, modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = if (type == selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface)) {
                            Row(Modifier.padding(horizontal = 14.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                                Column(Modifier.weight(1f)) { Text(type, fontWeight = FontWeight.Medium); Text(ruleTypeDescription(type), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                                if (type == selected) Text("已选", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelMedium)
                            }
                        }
                    }
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) { TextButton(onClick = onDismiss) { Text("关闭") } }
            }
        }
    }
}

/** Uses Android's real backdrop diffusion for floating glass where the OS supports it. */
@Composable
private fun ApplyDialogGlassBlur() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val view = LocalView.current
        val density = LocalDensity.current
        SideEffect {
            val window = (view.parent as? DialogWindowProvider)?.window ?: return@SideEffect
            window.setBackgroundBlurRadius(with(density) { 30.dp.roundToPx() })
            window.setDimAmount(0.12f)
        }
    }
}

private fun ruleTypeDescription(type: String): String = when {
    type.startsWith("DOMAIN") -> "按域名匹配"
    type == "GEOSITE" -> "按 Mihomo geodata 分类匹配"
    type.contains("IP") || type.contains("GEO") -> "按 IP 地址或地理信息匹配"
    type.contains("PORT") -> "按连接端口匹配"
    type.startsWith("PROCESS") || type == "UID" -> "按发起连接的进程匹配"
    type == "RULE-SET" -> "引用已配置的规则集提供者"
    type == "SUB-RULE" -> "进入本地子规则集合"
    type in setOf("AND", "OR", "NOT") -> "组合多个条件进行逻辑匹配"
    type == "MATCH" -> "匹配其余所有流量并作为最后一条规则"
    else -> "Mihomo $type 匹配类型"
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
    Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) { Column(Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
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
private fun RuleProvidersDialog(initial: RuleProfile, initialProviderType: String? = null, onDismiss: () -> Unit, onSave: (RuleProfile) -> Unit) {
    var profile by remember(initial) { mutableStateOf(initial) }
    val providers = profile.providers
    var editing by remember { mutableStateOf<RuleProvider?>(null) }
    var editingSubRule by remember { mutableStateOf<Pair<String, Int?>?>(null) }
    var subName by remember { mutableStateOf("") }
    var providerToDelete by remember { mutableStateOf<RuleProvider?>(null) }
    var subRuleToDelete by remember { mutableStateOf<String?>(null) }
    var managementSection by remember { mutableStateOf(0) }
    val invalidProviderSetup = profile.providers.any { it.name.isBlank() || (it.type == "http" && !it.url.startsWith("http")) || (it.type == "inline" && it.payload.isEmpty()) } ||
        profile.providers.map { it.name.lowercase() }.toSet().size != profile.providers.size ||
        profile.subRules.any { it.name.isBlank() } || profile.subRules.map { it.name.lowercase() }.toSet().size != profile.subRules.size
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
    } else JichangAlertDialog(onDismissRequest = onDismiss, title = { Text(if (managementSection == 0) "规则集管理" else "子规则管理") },
        text = { Column(Modifier.height(440.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            SegmentedTabs(listOf("规则集 ${providers.size}", "子规则 ${profile.subRules.size}"), managementSection) { managementSection = it }
            Column(Modifier.weight(1f).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (managementSection == 0) {
                    providers.forEach { provider -> Card(onClick = { editing = provider }, modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) { Row(Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) { Column(Modifier.weight(1f)) { Text(provider.name); Text("${provider.type} · ${provider.behavior} · ${provider.format}", style = MaterialTheme.typography.labelSmall) }; IconButton(onClick = { providerToDelete = provider }) { Icon(Icons.Outlined.Delete, "删除规则集提供者") } } } }
                    OutlinedButton(onClick = { editing = RuleProvider(id = java.util.UUID.randomUUID().toString(), name = "新规则集", type = initialProviderType ?: "http") }, modifier = Modifier.fillMaxWidth()) { Text(if (initialProviderType == "inline") "编写规则集" else "添加规则集提供者") }
                    if (providers.isEmpty()) Text("远程、文件或内嵌规则集都在此配置。", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                } else {
                    profile.subRules.forEach { rule ->
                        Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) { Column(Modifier.padding(horizontal = 8.dp, vertical = 4.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Column(Modifier.weight(1f)) { Text(rule.name); Text("${rule.rules.size} 条规则", style = MaterialTheme.typography.labelSmall) }
                                TextButton(onClick = { editingSubRule = rule.name to null }) { Text("添加规则") }
                                IconButton(onClick = { subRuleToDelete = rule.name }) { Icon(Icons.Outlined.Delete, "删除子规则") }
                            }
                            rule.rules.forEachIndexed { index, item -> TextButton(onClick = { editingSubRule = rule.name to index }) { Text("${item.type} · ${item.value} → ${item.group}") } }
                        } }
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) { OutlinedTextField(subName, { subName = it }, Modifier.weight(1f), label = { Text("新子规则名称") }, singleLine = true); TextButton(enabled = subName.isNotBlank() && profile.subRules.none { it.name == subName.trim() }, onClick = { profile = profile.copy(subRules = profile.subRules + SubRuleProfile(subName.trim())); subName = "" }) { Text("添加") } }
                    if (profile.subRules.isEmpty()) Text("子规则集合可以在规则中复用，并与规则集引用分开管理。", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                if (invalidProviderSetup) Text("请检查提供者与子规则名称、HTTP 地址及内嵌内容。", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
            }
        } },
        confirmButton = { TextButton(enabled = !invalidProviderSetup, onClick = { onSave(profile) }) { Text("保存") } }, dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } })
    providerToDelete?.let { provider ->
        val references = profile.rules.count { it.type == "RULE-SET" && it.value == provider.name } + profile.subRules.sumOf { sub -> sub.rules.count { it.type == "RULE-SET" && it.value == provider.name } }
        JichangAlertDialog(
            onDismissRequest = { providerToDelete = null },
            title = { Text("删除规则集提供者？") },
            text = { Text(if (references == 0) "“${provider.name}”将从当前配置移除。" else "有 $references 条规则引用“${provider.name}”。删除后这些引用会显示为校验错误。") },
            confirmButton = { TextButton(onClick = { profile = profile.copy(providers = profile.providers.filterNot { it.id == provider.id }); providerToDelete = null }) { Text("删除", color = MaterialTheme.colorScheme.error) } },
            dismissButton = { TextButton(onClick = { providerToDelete = null }) { Text("取消") } },
        )
    }
    subRuleToDelete?.let { name ->
        val references = profile.rules.count { it.type == "SUB-RULE" && it.value == name }
        JichangAlertDialog(
            onDismissRequest = { subRuleToDelete = null },
            title = { Text("删除子规则？") },
            text = { Text(if (references == 0) "“$name”及其中 ${profile.subRules.firstOrNull { it.name == name }?.rules?.size ?: 0} 条规则将被删除。" else "$references 条规则正在引用“$name”，删除后这些引用会显示为校验错误。") },
            confirmButton = { TextButton(onClick = { profile = profile.copy(subRules = profile.subRules.filterNot { it.name == name }); subRuleToDelete = null }) { Text("删除", color = MaterialTheme.colorScheme.error) } },
            dismissButton = { TextButton(onClick = { subRuleToDelete = null }) { Text("取消") } },
        )
    }
}

@Composable
private fun RuleProviderEditor(initial: RuleProvider, onDismiss: () -> Unit, onSave: (RuleProvider) -> Unit) {
    var name by remember(initial.id) { mutableStateOf(initial.name) }; var type by remember(initial.id) { mutableStateOf(initial.type) }
    var behavior by remember(initial.id) { mutableStateOf(initial.behavior) }; var url by remember(initial.id) { mutableStateOf(initial.url) }
    var path by remember(initial.id) { mutableStateOf(initial.path) }; var format by remember(initial.id) { mutableStateOf(initial.format) }
    var interval by remember(initial.id) { mutableStateOf(initial.interval.toString()) }
    var payload by remember(initial.id) { mutableStateOf(initial.payload.joinToString("\n")) }
    var headers by remember(initial.id) { mutableStateOf(initial.headers.entries.joinToString("\n") { (key, values) -> key + "=" + values.joinToString(",") }) }
    var section by remember(initial.id) { mutableStateOf(0) }
    JichangAlertDialog(onDismissRequest = onDismiss, title = { Text("规则集提供者") }, text = { Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SegmentedTabs(listOf("基本信息", "高级参数"), section) { section = it }
        if (section == 0) {
            OutlinedTextField(name, { name = it }, modifier = Modifier.fillMaxWidth(), label = { Text("名称") }, singleLine = true)
            SelectField("来源类型", type, listOf("http", "file", "inline")) { type = it }
            SelectField("匹配行为", behavior, listOf("domain", "ipcidr", "classical")) { behavior = it }
            if (type == "http") OutlinedTextField(url, { url = it }, modifier = Modifier.fillMaxWidth(), label = { Text("下载 URL") }, singleLine = true)
            if (type == "file") OutlinedTextField(path, { path = it }, modifier = Modifier.fillMaxWidth(), label = { Text("本地文件路径（应用规则集目录内）") }, singleLine = true)
            if (type == "inline") OutlinedTextField(payload, { payload = it }, modifier = Modifier.fillMaxWidth(), label = { Text("规则项，每行一条") }, minLines = 4)
        } else {
            SelectField("格式", format, listOf("yaml", "text", "mrs")) { format = it }
            if (type == "http") OutlinedTextField(interval, { interval = it.filter(Char::isDigit) }, modifier = Modifier.fillMaxWidth(), label = { Text("更新间隔（秒）") }, singleLine = true)
            if (type == "http") OutlinedTextField(path, { path = it }, modifier = Modifier.fillMaxWidth(), label = { Text("本地缓存路径") }, singleLine = true)
            if (type == "http") OutlinedTextField(headers, { headers = it }, modifier = Modifier.fillMaxWidth(), label = { Text("请求头，每行 key=value") }, minLines = 2)
            Text("这些参数会保存在当前规则集配置中；刷新缓存和状态仅保存在本机。", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
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
    JichangAlertDialog(onDismissRequest = onDismiss, title = { Text(if (creating) "新建配置" else "配置设置") }, text = { Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(name, { name = it; if (creating && fileName.isBlank()) fileName = it }, modifier = Modifier.fillMaxWidth(), label = { Text("配置名称") }, singleLine = true)
        OutlinedTextField(fileName, { fileName = it }, modifier = Modifier.fillMaxWidth(), label = { Text("导出文件名（无需 .yaml）") }, singleLine = true)
        if (creating) Row(verticalAlignment = Alignment.CenterVertically) { Checkbox(copy, { copy = it }); Text("复制当前规则和选择") }
    } }, confirmButton = { TextButton(enabled = name.isNotBlank() && fileName.isNotBlank(), onClick = { if (creating) onCreate(name.trim(), fileName.trim(), copy) else onSave(profile.id, name.trim(), fileName.trim()) }) { Text("保存") } }, dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } })
}

@Composable
private fun NodeEditDialog(node: ProxyNode, onDismiss: () -> Unit, onSave: (String, String, String, Int, Map<String, Any?>) -> Unit) {
    var name by remember(node.id) { mutableStateOf(node.name) }; var type by remember(node.id) { mutableStateOf(node.type) }
    var server by remember(node.id) { mutableStateOf(node.server) }; var port by remember(node.id) { mutableStateOf(node.port.toString()) }
    var extra by remember(node.id) { mutableStateOf(node.options.filterKeys { it !in setOf("name", "type", "server", "port") }.entries.joinToString("\n") { "${it.key}=${it.value}" }) }
    JichangAlertDialog(onDismissRequest = onDismiss, title = { Text("编辑节点") }, text = { Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(7.dp)) {
        OutlinedTextField(name, { name = it }, modifier = Modifier.fillMaxWidth(), label = { Text("名称") }, singleLine = true)
        OutlinedTextField(type, { type = it }, modifier = Modifier.fillMaxWidth(), label = { Text("协议类型") }, singleLine = true)
        OutlinedTextField(server, { server = it }, modifier = Modifier.fillMaxWidth(), label = { Text("服务器") }, singleLine = true)
        OutlinedTextField(port, { port = it.filter(Char::isDigit) }, modifier = Modifier.fillMaxWidth(), label = { Text("端口") }, singleLine = true)
        OutlinedTextField(extra, { extra = it }, modifier = Modifier.fillMaxWidth(), label = { Text("其他选项，每行 key=value") }, minLines = 4)
    } }, confirmButton = { TextButton(enabled = name.isNotBlank() && server.isNotBlank() && port.toIntOrNull() in 1..65535, onClick = { val options = extra.lines().mapNotNull { line -> line.split("=", limit = 2).takeIf { it.size == 2 }?.let { it[0].trim() to it[1].trim() } }.toMap(); onSave(name, type, server, port.toInt(), options) }) { Text("保存") } }, dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } })
}

@Composable
private fun ExportSetupDialog(
    state: AppState,
    options: ConfigExportOptions,
    onToggleRegion: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    val profile = state.activeProfile
    val enabledSourceIds = profile.selectedSourceIds
    val nodes = state.nodes.filter { node ->
        (node.sourceId == null || node.sourceId in enabledSourceIds) && node.id in profile.enabledNodeIds
    }
    val regions = NodeAutoGroups.regions.map { it.key to it.title } + (NodeAutoGroups.OTHER to "其他")
    JichangAlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("导出地区策略组") },
        text = {
            Column(
                Modifier.heightIn(max = 440.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    "选择要生成的地区策略组。节点启用和地区归属请在“资源 → 节点”中管理。",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(8.dp))
                regions.forEach { (key, title) ->
                    Row(
                        Modifier.fillMaxWidth().clip(MaterialTheme.shapes.small).padding(horizontal = 8.dp, vertical = 3.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Checkbox(checked = key in options.enabledRegions, onCheckedChange = { onToggleRegion(key) })
                        Text(title, Modifier.weight(1f).clickable { onToggleRegion(key) }, style = MaterialTheme.typography.bodyLarge)
                        Text(
                            nodes.count { node ->
                                (profile.regionOverrides[node.id] ?: NodeAutoGroups.classify(node.name)) == key
                            }.toString(),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        },
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
