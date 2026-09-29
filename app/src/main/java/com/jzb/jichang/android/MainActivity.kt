package com.jzb.jichang.android

import android.Manifest
import android.content.Intent
import android.content.ContentValues
import android.content.Context
import android.app.Activity
import android.content.pm.PackageManager
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
import androidx.activity.compose.BackHandler
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
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
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
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
import androidx.compose.material.icons.outlined.QrCodeScanner
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
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
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
import androidx.compose.runtime.key
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.semantics.Role
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.LocalDensity
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
import com.jzb.jichang.android.service.SingBoxConfigGenerator
import com.jzb.jichang.android.service.GeneratedSingBoxConfig
import com.jzb.jichang.android.service.SingBoxExportIssue
import com.jzb.jichang.android.service.GeneratedConfig
import com.jzb.jichang.android.service.MihomoTemplateParser
import com.jzb.jichang.android.service.TemplateSubscriptionParameter
import com.jzb.jichang.android.service.ConfigExportOptions
import com.jzb.jichang.android.service.ConfigSourceMode
import com.jzb.jichang.android.service.NodeAutoGroups
import com.jzb.jichang.android.service.RuleDiagnostics
import com.jzb.jichang.android.service.RemoteConfigDownloader
import com.jzb.jichang.android.service.LanShareQrCode
import com.jzb.jichang.android.service.MihomoSettings
import com.jzb.jichang.android.service.MihomoNodeOptionsYaml
import com.jzb.jichang.android.share.LocalShareController
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        window.isStatusBarContrastEnforced = false
        window.isNavigationBarContrastEnforced = false
        setContent { JichangApp() }
    }
}

private enum class AppPage(val label: String) { Home("概览"), Resources("资源"), Rules("规则"), Share("分享") }
private enum class ResourceTab(val label: String) { Sources("订阅"), Nodes("节点"), Templates("模板") }
private enum class RuleSection(val label: String) { List("规则"), Groups("策略组"), Providers("规则集"), SubRules("子规则"), Diagnostics("校验"), General("基础配置"), Advanced("高级 YAML") }
private enum class DialogKind { Source, Node, Group, Rule, Providers, Profile }
private enum class ExportAction { Download, Share }
private enum class ExportFormat(val label: String) { Mihomo("Mihomo"), SingBox("sing-box") }
private val emptyGeneratedConfig = GeneratedConfig("", 0, 0)

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

@Composable
private fun FullScreenEditorDialog(
    title: String,
    dirty: Boolean,
    valid: Boolean,
    saveLabel: String = "保存",
    onDismiss: () -> Unit,
    onSave: () -> Unit,
    content: @Composable () -> Unit,
) {
    var confirmDiscard by remember { mutableStateOf(false) }
    fun requestDismiss() { if (dirty) confirmDiscard = true else onDismiss() }
    Dialog(onDismissRequest = ::requestDismiss, properties = DialogProperties(usePlatformDefaultWidth = false, dismissOnClickOutside = false)) {
        Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            Column {
                Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = ::requestDismiss) { Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "返回") }
                    Text(title, Modifier.weight(1f), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Button(onClick = onSave, enabled = valid) { Text(saveLabel) }
                }
                Box(Modifier.fillMaxWidth().weight(1f).padding(horizontal = JichangSpacing.pageHorizontal)) { content() }
            }
        }
    }
    if (confirmDiscard) JichangAlertDialog(onDismissRequest = { confirmDiscard = false }, title = { Text("放弃未保存修改？") },
        text = { Text("返回后本次编辑的内容不会保存。") },
        confirmButton = { TextButton(onClick = { confirmDiscard = false; onDismiss() }) { Text("放弃修改", color = MaterialTheme.colorScheme.error) } },
        dismissButton = { TextButton(onClick = { confirmDiscard = false }) { Text("继续编辑") } })
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
    var page by rememberSaveable { mutableStateOf(AppPage.Home) }
    var resourceTab by rememberSaveable { mutableStateOf(ResourceTab.Sources) }
    var ruleSection by rememberSaveable { mutableStateOf(RuleSection.List) }
    var showYamlPreview by rememberSaveable { mutableStateOf(false) }
    var exportFormat by rememberSaveable { mutableStateOf(ExportFormat.Mihomo) }
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
    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(viewModel.message) {
        viewModel.message?.let { snackbarHostState.showSnackbar(it, duration = if (viewModel.messageIsError) SnackbarDuration.Long else SnackbarDuration.Short) }
    }
    val remoteDownloader = remember { RemoteConfigDownloader() }
    val shareController = remember { LocalShareController(context) }
    val shareUrl by shareController.url.collectAsState()
    val shareError by shareController.error.collectAsState()
    val generator = remember { MihomoConfigGenerator() }
    val singBoxGenerator = remember { SingBoxConfigGenerator() }
    val profile = state.activeProfile
    LaunchedEffect(profile.id) {
        ruleSection = RuleSection.List
        showYamlPreview = false
    }
    val inDetail = (page == AppPage.Rules && ruleSection != RuleSection.List) || (page == AppPage.Share && showYamlPreview)
    BackHandler(enabled = inDetail || page != AppPage.Home) {
        when {
            page == AppPage.Rules && ruleSection != RuleSection.List -> ruleSection = RuleSection.List
            page == AppPage.Share && showYamlPreview -> showYamlPreview = false
            else -> page = AppPage.Home
        }
    }
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
    val generationState = remember(state.sources, state.nodes, state.templates, profile) {
        AppState(sources = state.sources, nodes = state.nodes, profiles = listOf(profile), activeProfileId = profile.id, templates = state.templates)
    }
    val generatedResult by key(generationState, exportOptions) {
        produceState<Result<GeneratedConfig>?>(initialValue = null) {
            value = withContext(Dispatchers.Default) {
                runCatching { generator.generate(generationState, profile, exportOptions, parsedTemplate) }
            }
        }
    }
    val singBoxResult by key(generationState, state.ruleProviderStatuses, exportFormat) {
        produceState<Result<GeneratedSingBoxConfig>?>(initialValue = null) {
            if (exportFormat == ExportFormat.SingBox) value = withContext(Dispatchers.IO) {
                runCatching { singBoxGenerator.generate(generationState.copy(ruleProviderStatuses = state.ruleProviderStatuses), profile, File(context.filesDir, "rule-providers")) }
            }
        }
    }
    val singBoxGenerated = singBoxResult?.getOrNull()
    val configReady = if (exportFormat == ExportFormat.Mihomo) generatedResult?.isSuccess == true else singBoxGenerated?.ready == true
    val generationError = if (exportFormat == ExportFormat.Mihomo) generatedResult?.exceptionOrNull()?.message else singBoxResult?.exceptionOrNull()?.message
    val generated = generatedResult?.getOrNull() ?: emptyGeneratedConfig
    val configText = if (exportFormat == ExportFormat.Mihomo) generated.yaml else singBoxGenerated?.json.orEmpty()
    val validationIssues = remember(state.ruleProfile, state.nodes) {
        RuleDiagnostics.inspect(state.ruleProfile, state.nodes.map { it.id }.toSet())
    }
    val refreshIssues = remember(state.ruleProviderStatuses, state.ruleProfile.providers, profile.id) {
        state.ruleProviderStatuses.filter { it.profileId == profile.id && !it.error.isNullOrBlank() }
            .mapNotNull { status -> state.ruleProfile.providers.firstOrNull { it.id == status.providerId }?.let { provider ->
                com.jzb.jichang.android.service.RuleIssue(-1, RoutingRule("RULE-SET", provider.name, ""), "规则集刷新失败：${status.error}", providerId = provider.id)
            } }
    }
    val ruleIssues = remember(validationIssues, refreshIssues) { validationIssues + refreshIssues }
    val filename = remember(profile.fileName, exportFormat) { safeConfigFileName(profile.fileName, exportFormat) }
    val effectiveVisualSettings = remember(profile, parsedTemplate) {
        MihomoSettings.effectiveVisualSettings(parsedTemplate?.rawRoot.orEmpty(), profile)
    }
    val sensitiveReasons = buildList {
        if (exportFormat == ExportFormat.Mihomo && generated.referencedSubscriptions > 0) add("机场订阅地址")
        if (exportFormat == ExportFormat.Mihomo && !effectiveVisualSettings["external-controller"].toString().isNullOrBlank() && effectiveVisualSettings["external-controller"] != null) add("外部控制器地址")
        if (exportFormat == ExportFormat.Mihomo && !effectiveVisualSettings["secret"].toString().isNullOrBlank() && effectiveVisualSettings["secret"] != null) add("控制器密钥")
        val authentication = effectiveVisualSettings["authentication"] as? List<*>
        if (exportFormat == ExportFormat.Mihomo && !authentication.isNullOrEmpty()) add("监听账号密码")
    }
    var templateExportContent by remember { mutableStateOf<String?>(null) }

    val saveLegacyConfig = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/octet-stream")) { uri: Uri? ->
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
    var sharingFormat by remember { mutableStateOf(exportFormat) }
    LaunchedEffect(exportFormat) {
        if (sharingFormat != exportFormat) {
            shareController.stop()
            pendingSensitiveAction = null
            sharingFormat = exportFormat
        }
    }
    LaunchedEffect(configText, filename, configReady) { if (configReady) shareController.updateConfig(configText, filename) }

    fun downloadConfig() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            saveLegacyConfig.launch(filename)
        } else viewModel.run {
            withContext(Dispatchers.IO) { saveToDownloads(context, configText, filename) }
            "已下载到 Downloads/鸡场/$filename"
        }
    }

    fun requestExport(action: ExportAction) {
        if (!configReady) {
            viewModel.run { throw IllegalStateException(generationError ?: "配置生成中，请稍候") }
        } else if (exportFormat == ExportFormat.Mihomo && generated.unresolvedTemplateProviders.isNotEmpty()) {
            viewModel.run { throw IllegalStateException("请先为模板订阅绑定机场：${generated.unresolvedTemplateProviders.joinToString("、")}") }
        } else if (sensitiveReasons.isNotEmpty()) pendingSensitiveAction = action
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
            snackbarHost = { SnackbarHost(snackbarHostState) },
            topBar = {
                TopAppBar(
                    title = { Text(if (page == AppPage.Rules) ruleSection.label else if (showYamlPreview && page == AppPage.Share) "配置预览" else page.label, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold) },
                    navigationIcon = {
                        if (inDetail) IconButton(onClick = { if (page == AppPage.Rules) ruleSection = RuleSection.List else showYamlPreview = false }) {
                            Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "返回")
                        }
                    },
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
                                Text(profile.name, modifier = Modifier.widthIn(max = 132.dp), maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurface)
                                Icon(Icons.Outlined.KeyboardArrowDown, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            DropdownMenu(expanded = profileMenu, onDismissRequest = { profileMenu = false }) {
                                state.profiles.forEach { item -> DropdownMenuItem(
                                    text = { Text(if (item.id == profile.id) "✓ ${item.name}" else item.name) },
                                    onClick = { viewModel.switchProfile(item.id); ruleSection = RuleSection.List; showYamlPreview = false; profileMenu = false },
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
                if (!inDetail) {
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
                            AppPage.Resources -> Icons.Outlined.Devices
                            AppPage.Rules -> Icons.Outlined.Tune
                            AppPage.Share -> Icons.Outlined.Share
                        }
                        NavigationBarItem(
                            selected = page == item,
                            onClick = { page = item; ruleSection = RuleSection.List; showYamlPreview = false },
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
                }
            },
        ) { insets ->
            Column(Modifier.fillMaxSize().padding(insets)) {
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
                        issues = ruleIssues,
                        onNavigate = { page = it },
                        onOpenTemplates = { page = AppPage.Resources; resourceTab = ResourceTab.Templates },
                        onOpenIssues = { page = AppPage.Rules; ruleSection = RuleSection.Diagnostics },
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
                    AppPage.Rules -> key(profile.id) { RulesPage(
                        state, viewModel, issues = ruleIssues, section = ruleSection, onSectionChange = { ruleSection = it }, modifier = Modifier.fillMaxSize(),
                        templateRoot = parsedTemplate?.rawRoot.orEmpty(),
                        onEditGroup = { editingGroup = it; dialog = DialogKind.Group },
                        onEditRule = { index, rule -> editingRule = index to rule; dialog = DialogKind.Rule },
                        onAddRule = { editingRule = null; dialog = DialogKind.Rule },
                        onAddGroup = { editingGroup = null; dialog = DialogKind.Group },
                        onProviders = { type -> providerInitialType = type; dialog = DialogKind.Providers },
                        onSaveRuleProfile = viewModel::saveRuleProfile,
                        onSaveMihomoSettings = viewModel::saveMihomoSettings,
                        onSaveAdvancedYaml = viewModel::saveAdvancedYaml,
                    ) }
                    AppPage.Share -> if (showYamlPreview) {
                        if (configReady) YamlPreviewPage(configText, Modifier.fillMaxSize())
                        else Text(generationError ?: "正在生成配置…", Modifier.fillMaxSize().padding(JichangSpacing.pageHorizontal))
                    } else ExportPage(
                                state = state, options = exportOptions, format = exportFormat,
                                onFormatChange = { exportFormat = it },
                                singBoxIssues = singBoxGenerated?.issues.orEmpty(),
                                onOpenSingBoxIssue = { issue -> when (issue.kind) {
                                    "node" -> { page = AppPage.Resources; resourceTab = ResourceTab.Nodes }
                                    "provider" -> { page = AppPage.Rules; ruleSection = RuleSection.Providers }
                                    "group" -> { page = AppPage.Rules; ruleSection = RuleSection.Groups }
                                    "rule" -> { page = AppPage.Rules; ruleSection = RuleSection.List }
                                    "template" -> { page = AppPage.Resources; resourceTab = ResourceTab.Templates }
                                    else -> { page = AppPage.Rules; ruleSection = RuleSection.General }
                                } },
                                generatedNodes = if (exportFormat == ExportFormat.Mihomo) generated.exportedNodes else singBoxGenerated?.exportedNodes ?: 0,
                                skippedNodes = if (exportFormat == ExportFormat.Mihomo) generated.skippedNodes else 0,
                                referencedSubscriptions = if (exportFormat == ExportFormat.Mihomo) generated.referencedSubscriptions else 0,
                                configReady = configReady, generationError = generationError,
                                shareUrl = shareUrl, shareError = shareError, filename = filename,
                                unresolvedTemplateProviders = if (exportFormat == ExportFormat.Mihomo) generated.unresolvedTemplateProviders else emptyList(),
                                templateParameters = parsedTemplate?.subscriptionParameters.orEmpty(),
                                templateBindings = profile.templateProviderBindings,
                                ruleIssues = ruleIssues,
                                hasRegionalProxyGroups = parsedTemplate?.hasRegionalProxyGroups == true,
                                onTemplateBinding = viewModel::bindTemplateProvider,
                                onModeChange = { viewModel.updateExportSettings(it.name, profile.enabledRegions, profile.regionOverrides) },
                                onOpenFilters = { showExportSetup = true }, onDownload = { requestExport(ExportAction.Download) },
                                onShare = { requestExport(ExportAction.Share) }, onStopShare = { shareController.stop() },
                                onOpenIssues = { page = AppPage.Rules; ruleSection = RuleSection.Diagnostics },
                                onPreview = { showYamlPreview = true },
                                onShareLink = {
                                    val intent = Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, shareUrl)
                                    context.startActivity(Intent.createChooser(intent, "分享配置链接"))
                                }, modifier = Modifier.fillMaxSize(),
                            )

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
        page = AppPage.Rules
        ruleSection = RuleSection.List
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
            title = { Text("配置包含敏感信息") },
            text = { Text("导出内容包含${sensitiveReasons.joinToString("、")}。拿到配置文件或局域网分享链接的人可能访问这些服务，请只分享给可信对象。") },
            confirmButton = { TextButton(enabled = configReady, onClick = {
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
    issues: List<com.jzb.jichang.android.service.RuleIssue>,
    onNavigate: (AppPage) -> Unit,
    onOpenTemplates: () -> Unit,
    onOpenIssues: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val profile = state.activeProfile
    val blocking = issues.count { !it.warning }
    val selectedNodes = state.nodes.count { it.id in profile.enabledNodeIds && (it.sourceId == null || it.sourceId in profile.selectedSourceIds) }
    LazyColumn(modifier, contentPadding = PaddingValues(horizontal = JichangSpacing.pageHorizontal, vertical = JichangSpacing.pageVertical), verticalArrangement = Arrangement.spacedBy(JichangSpacing.section)) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("当前配置", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(profile.name, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Text("$selectedNodes 个可用节点 · ${profile.ruleProfile.rules.size} 条规则 · ${profile.ruleProfile.groups.size} 个策略组", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        item {
            Card(onClick = if (issues.isEmpty()) ({ onNavigate(AppPage.Share) }) else onOpenIssues, colors = CardDefaults.cardColors(containerColor = if (blocking > 0) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.surface)) {
                Row(Modifier.fillMaxWidth().padding(JichangSpacing.card), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(if (blocking > 0) "需要修正规则" else if (issues.isNotEmpty()) "有 ${issues.size} 项规则提醒" else "可以预览或分享", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        Text(if (blocking > 0) "$blocking 个错误会阻止导出，点击查看校验。" else if (issues.isNotEmpty()) "可能冲突仅供检查，点击查看。" else "配置校验正常，进入分享页生成 YAML。", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Text("查看", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                }
            }
        }
        item { Text("继续操作", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold) }
        item { RuleManagementEntry("准备资源", "${profile.selectedSourceIds.size} 个订阅已选 · $selectedNodes 个节点可用") { onNavigate(AppPage.Resources) } }
        item { RuleManagementEntry("整理规则", "${profile.ruleProfile.rules.size} 条分流规则 · ${profile.ruleProfile.providers.size} 个规则集") { onNavigate(AppPage.Rules) } }
        item { RuleManagementEntry("导出与分享", "设置节点来源、预览配置并下载或分享") { onNavigate(AppPage.Share) } }
        item { TextButton(onClick = onOpenTemplates) { Text("从模板开始新配置") } }
    }
}

@Composable
private fun SourcesPage(state: AppState, viewModel: AppViewModel, onAdd: () -> Unit, modifier: Modifier = Modifier) {
    val profile = state.activeProfile
    val nodeCounts = remember(state.nodes) { state.nodes.groupingBy { it.sourceId }.eachCount() }
    var sourceToDelete by remember { mutableStateOf<com.jzb.jichang.android.model.SubscriptionSource?>(null) }
    LazyColumn(modifier, contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = JichangSpacing.pageHorizontal, vertical = JichangSpacing.pageVertical), verticalArrangement = Arrangement.spacedBy(JichangSpacing.section)) {
        item {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("订阅资源", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold)
                    Text("当前配置启用 ${profile.selectedSourceIds.size} / ${state.sources.size}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Button(onClick = onAdd) { Icon(Icons.Outlined.Add, null); Text("订阅") }
            }
            TextButton(onClick = { viewModel.refreshAllSources() }, enabled = state.sources.isNotEmpty() && viewModel.refreshingSourceIds.isEmpty()) {
                if (viewModel.refreshingSourceIds.isNotEmpty()) CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp)
                else Icon(Icons.Outlined.ArrowDownward, null)
                Spacer(Modifier.width(6.dp)); Text("刷新全部订阅")
            }
        }
        if (state.sources.isEmpty()) item { EmptyCard("还没有订阅", "使用上方按钮添加机场订阅，或到“节点”页直接导入节点链接。") }
        items(state.sources, key = { it.id }) { source ->
            var menuExpanded by remember(source.id) { mutableStateOf(false) }
            Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                Column(Modifier.padding(JichangSpacing.card), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(source.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Medium)
                            Text(runCatching { java.net.URI(source.url).host }.getOrNull() ?: "订阅地址", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Switch(checked = source.id in profile.selectedSourceIds, onCheckedChange = { viewModel.toggleSource(source.id) })
                        Box {
                            IconButton(onClick = { menuExpanded = true }) { Icon(Icons.Outlined.MoreVert, "订阅操作") }
                            DropdownMenu(menuExpanded, { menuExpanded = false }) {
                                DropdownMenuItem(text = { Text("刷新") }, enabled = source.id !in viewModel.refreshingSourceIds, onClick = { menuExpanded = false; viewModel.refreshSource(source.id) })
                                DropdownMenuItem(text = { Text("删除") }, onClick = { menuExpanded = false; sourceToDelete = source })
                            }
                        }
                    }
                    val count = nodeCounts[source.id] ?: 0
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
    sourceToDelete?.let { source -> JichangAlertDialog(onDismissRequest = { sourceToDelete = null },
        title = { Text("删除订阅？") }, text = { Text("将删除“${source.name}”及其导入的节点。") },
        confirmButton = { TextButton(onClick = { viewModel.removeSource(source.id); sourceToDelete = null }) { Text("删除", color = MaterialTheme.colorScheme.error) } },
        dismissButton = { TextButton(onClick = { sourceToDelete = null }) { Text("取消") } }) }
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
    var filtersExpanded by remember { mutableStateOf(false) }
    var bulkActions by remember { mutableStateOf(false) }
    var expandedProtocol by remember { mutableStateOf(false) }
    var editingNode by remember { mutableStateOf<ProxyNode?>(null) }
    var nodeToDelete by remember { mutableStateOf<ProxyNode?>(null) }
    val profile = state.activeProfile
    val regionKeys = NodeAutoGroups.regions.map { it.key } + NodeAutoGroups.OTHER
    val regions = listOf("全部地区") + regionKeys.map(NodeAutoGroups::title)
    val protocols = remember(state.nodes) { listOf("全部协议") + state.nodes.map { it.type.uppercase() }.distinct().sorted() }
    val sourceNames = remember(state.sources) { state.sources.associate { it.id to it.name } }
    val filtersActive = protocolFilter != "全部协议" || sourceFilter != "全部来源" || enabledFilter != "全部" || regionFilter != "全部地区"
    val filtered = remember(state.nodes, profile, sourceNames, query, protocolFilter, sourceFilter, enabledFilter, regionFilter) {
        state.nodes.filter { node ->
            (query.isBlank() || node.name.contains(query, true) || node.server.contains(query, true)) &&
                (protocolFilter == "全部协议" || node.type.equals(protocolFilter, true)) &&
                (sourceFilter == "全部来源" || (sourceFilter == "手动导入" && node.sourceId == null) || node.sourceId?.let(sourceNames::get) == sourceFilter) &&
                (enabledFilter == "全部" || (node.id in profile.enabledNodeIds) == (enabledFilter == "已启用")) &&
                (regionFilter == "全部地区" || NodeAutoGroups.title(profile.regionOverrides[node.id] ?: NodeAutoGroups.classify(node.name)) == regionFilter)
        }
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
                Button(onClick = onAdd) { Icon(Icons.Outlined.Add, null); Text("添加节点") }
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("搜索节点名称或服务器") },
                    singleLine = true,
                )
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text("${filtered.size} 个结果", Modifier.weight(1f), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    TextButton(onClick = { filtersExpanded = !filtersExpanded }) { Text(if (filtersExpanded) "收起筛选" else if (filtersActive) "筛选中" else "筛选") }
                    TextButton(onClick = { bulkActions = !bulkActions }, enabled = filtered.isNotEmpty() || bulkActions) { Text(if (bulkActions) "完成" else "批量操作") }
                }
                if (filtersActive) TextButton(onClick = { protocolFilter = "全部协议"; sourceFilter = "全部来源"; enabledFilter = "全部"; regionFilter = "全部地区" }) { Text("清除筛选") }
                if (filtersExpanded) Row(horizontalArrangement = Arrangement.spacedBy(JichangSpacing.item), modifier = Modifier.fillMaxWidth()) {
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
                if (filtersExpanded) Row(horizontalArrangement = Arrangement.spacedBy(JichangSpacing.item), modifier = Modifier.fillMaxWidth()) {
                    ChoiceMenu(sourceFilter, listOf("全部来源", "手动导入") + state.sources.map { it.name }, { sourceFilter = it }, Modifier.weight(1f))
                    DropdownMenuFilter(enabledFilter, { enabledFilter = it }, Modifier.weight(1f))
                }
                if (bulkActions) Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = { viewModel.setNodesEnabled(filtered.map { it.id }.toSet(), true); bulkActions = false }, enabled = filtered.isNotEmpty()) { Text("启用筛选结果") }
                    TextButton(onClick = { viewModel.setNodesEnabled(filtered.map { it.id }.toSet(), false); bulkActions = false }, enabled = filtered.isNotEmpty()) { Text("停用筛选结果") }
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
                onDelete = { nodeToDelete = node },
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
    nodeToDelete?.let { node -> JichangAlertDialog(onDismissRequest = { nodeToDelete = null }, title = { Text("删除节点？") },
        text = { Text("将从本机删除“${node.name}”。") },
        confirmButton = { TextButton(onClick = { viewModel.removeNode(node.id); nodeToDelete = null }) { Text("删除", color = MaterialTheme.colorScheme.error) } },
        dismissButton = { TextButton(onClick = { nodeToDelete = null }) { Text("取消") } }) }
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
private fun RuleManagementEntry(title: String, detail: String, modifier: Modifier = Modifier, compact: Boolean = false, onClick: () -> Unit) {
    Card(onClick = onClick, modifier = modifier.fillMaxWidth().heightIn(min = JichangSpacing.touchTarget), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Row(Modifier.fillMaxWidth().padding(horizontal = if (compact) 12.dp else JichangSpacing.card, vertical = if (compact) 10.dp else 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                Text(detail, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = if (compact) 1 else 2, overflow = TextOverflow.Ellipsis)
            }
            if (!compact) Text("进入", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
        }
    }
}

@Composable
private fun MihomoSettingsPage(
    profile: ConfigProfile,
    templateRoot: Map<String, Any?>,
    onSave: (Map<String, Any?>) -> Unit,
    modifier: Modifier = Modifier,
) {
    val base = remember(profile.id, profile.mihomoSettings, templateRoot) {
        MihomoSettings.effectiveVisualSettings(templateRoot, profile)
    }
    var values by remember(profile.id, profile.mihomoSettings, templateRoot) { mutableStateOf(base) }
    var error by remember(profile.id) { mutableStateOf<String?>(null) }
    val changed = values != base

    fun setRoot(key: String, value: Any?) {
        values = LinkedHashMap(values).apply { if (value == null) remove(key) else put(key, value) }
    }
    fun block(key: String): Map<String, Any?> = (values[key] as? Map<*, *>)
        ?.entries?.associate { it.key.toString() to it.value }.orEmpty()
    fun setBlock(key: String, field: String, value: Any?) {
        val updated = LinkedHashMap(block(key)).apply {
            if (value == null || (value is String && value.isBlank())) remove(field) else put(field, value)
        }
        setRoot(key, updated)
    }
    fun value(key: String, fallback: String = ""): String = values[key]?.toString() ?: fallback
    fun blockValue(blockKey: String, field: String, fallback: String = ""): String = block(blockKey)[field]?.toString() ?: fallback
    fun linesValue(blockKey: String, field: String, fallback: String = ""): String = when (val raw = block(blockKey)[field]) {
        is List<*> -> raw.joinToString("\n")
        null -> fallback
        else -> raw.toString()
    }
    fun checked(blockKey: String, field: String, fallback: Boolean = false): Boolean =
        (block(blockKey)[field] as? Boolean) ?: fallback

    Column(modifier.fillMaxSize()) {
        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = JichangSpacing.pageHorizontal, vertical = JichangSpacing.pageVertical),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text("Mihomo 基础配置", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold)
            Text("设置只写入当前配置文件。未填写的选项沿用模板值或 Mihomo 默认行为。", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            SettingsPanel("常规与监听") {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SettingsTextField("mixed-port", value("mixed-port", "7890"), { setRoot("mixed-port", it) }, Modifier.weight(1f), "HTTP 与 SOCKS 混合端口")
                    SettingsTextField("port", value("port"), { setRoot("port", it) }, Modifier.weight(1f), "HTTP 代理端口")
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SettingsTextField("socks-port", value("socks-port"), { setRoot("socks-port", it) }, Modifier.weight(1f), "SOCKS5 端口")
                    SettingsTextField("redir-port", value("redir-port"), { setRoot("redir-port", it) }, Modifier.weight(1f), "透明代理端口")
                }
                SettingsTextField("tproxy-port", value("tproxy-port"), { setRoot("tproxy-port", it) }, Modifier.fillMaxWidth(), "Linux TProxy 端口；Android App 不会启动内核")
                SettingsSwitchRow("允许局域网连接", "开放监听后，同一网络的设备可能访问代理端口。", values["allow-lan"] as? Boolean ?: false) { setRoot("allow-lan", it) }
                SettingsTextField("bind-address", value("bind-address", "*"), { setRoot("bind-address", it) }, Modifier.fillMaxWidth(), "监听地址，例如 * 或 127.0.0.1")
                SelectField("工作模式", value("mode", "rule"), listOf("rule", "global", "direct")) { setRoot("mode", it) }
                SelectField("日志等级", value("log-level", "info"), listOf("silent", "error", "warning", "info", "debug")) { setRoot("log-level", it) }
                SelectField("进程匹配模式", value("process-mode", "strict"), listOf("strict", "always", "off")) { setRoot("process-mode", it) }
                SettingsSwitchRow("IPv6", "控制 Mihomo 对 IPv6 流量的处理。", values["ipv6"] as? Boolean ?: true) { setRoot("ipv6", it) }
                SettingsSwitchRow("统一延迟", "比较策略组节点时统一延迟计算方式。", values["unified-delay"] as? Boolean ?: false) { setRoot("unified-delay", it) }
                SettingsSwitchRow("TCP 并发连接", "启用并发连接尝试。", values["tcp-concurrent"] as? Boolean ?: false) { setRoot("tcp-concurrent", it) }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SettingsTextField("保活空闲时长", value("keep-alive-idle"), { setRoot("keep-alive-idle", it) }, Modifier.weight(1f), "秒，可留空")
                    SettingsTextField("保活间隔", value("keep-alive-interval"), { setRoot("keep-alive-interval", it) }, Modifier.weight(1f), "秒，可留空")
                }
                SettingsTextField("出口网卡", value("interface-name"), { setRoot("interface-name", it) }, Modifier.fillMaxWidth(), "例如 wlan0，可留空")
                SettingsTextField("路由标记", value("routing-mark"), { setRoot("routing-mark", it) }, Modifier.fillMaxWidth(), "Linux fwmark，可留空")
                SettingsTextField("全局 TLS 指纹", value("global-client-fingerprint"), { setRoot("global-client-fingerprint", it) }, Modifier.fillMaxWidth(), "例如 chrome，可留空")
                SettingsTextField("外部控制器", value("external-controller"), { setRoot("external-controller", it) }, Modifier.fillMaxWidth(), "例如 127.0.0.1:9090；请谨慎分享")
                SettingsTextField("控制器 UI 目录", value("external-ui"), { setRoot("external-ui", it) }, Modifier.fillMaxWidth(), "UI 静态文件目录，可留空")
                SettingsTextField("控制器 UI 下载地址", value("external-ui-url"), { setRoot("external-ui-url", it) }, Modifier.fillMaxWidth(), "UI 下载 URL，可留空")
                SettingsTextField("控制器密钥", value("secret"), { setRoot("secret", it) }, Modifier.fillMaxWidth(), "限制外部控制器访问")
                OutlinedTextField(
                    value = when (val auth = values["authentication"]) { is List<*> -> auth.joinToString("\n"); null -> ""; else -> auth.toString() },
                    onValueChange = { setRoot("authentication", it) },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("监听认证") },
                    supportingText = { Text("每行 username:password；没有认证时留空。") },
                    minLines = 2,
                )
            }
            SettingsPanel("DNS") {
                SettingsSwitchRow("启用 DNS 模块", "开启后将输出 dns 配置。", checked("dns", "enable")) { setBlock("dns", "enable", it) }
                SettingsTextField("DNS 监听地址", blockValue("dns", "listen", "127.0.0.1:1053"), { setBlock("dns", "listen", it) }, Modifier.fillMaxWidth(), "例如 0.0.0.0:1053")
                SelectField("增强模式", blockValue("dns", "enhanced-mode", "redir-host"), listOf("fake-ip", "redir-host")) { setBlock("dns", "enhanced-mode", it) }
                SettingsTextField("Fake-IP 地址段", blockValue("dns", "fake-ip-range", "198.18.0.1/16"), { setBlock("dns", "fake-ip-range", it) }, Modifier.fillMaxWidth())
                SettingsSwitchRow("DNS IPv6", "允许 DNS 返回 IPv6 地址。", checked("dns", "ipv6", true)) { setBlock("dns", "ipv6", it) }
                SettingsSwitchRow("使用配置 hosts", "响应 Mihomo 配置中的 hosts 记录。", checked("dns", "use-hosts", true)) { setBlock("dns", "use-hosts", it) }
                SettingsSwitchRow("使用系统 hosts", "将系统 hosts 记录纳入解析。", checked("dns", "use-system-hosts", true)) { setBlock("dns", "use-system-hosts", it) }
                SettingsSwitchRow("遵循路由规则", "DNS 连接遵循规则；启用时应配置代理节点解析 DNS。", checked("dns", "respect-rules")) { setBlock("dns", "respect-rules", it) }
                SettingsTextField("默认 DNS", linesValue("dns", "default-nameserver"), { setBlock("dns", "default-nameserver", it) }, Modifier.fillMaxWidth(), "每行一个地址")
                SettingsTextField("上游 DNS", linesValue("dns", "nameserver"), { setBlock("dns", "nameserver", it) }, Modifier.fillMaxWidth(), "每行一个地址")
                SettingsTextField("Fallback DNS", linesValue("dns", "fallback"), { setBlock("dns", "fallback", it) }, Modifier.fillMaxWidth(), "每行一个地址，可留空")
                SettingsTextField("代理节点解析 DNS", linesValue("dns", "proxy-server-nameserver"), { setBlock("dns", "proxy-server-nameserver", it) }, Modifier.fillMaxWidth(), "每行一个地址，可留空")
            }
            SettingsPanel("TUN") {
                SettingsSwitchRow("启用 TUN", "只生成 Mihomo 配置；鸡场不会启动 VPN 或代理内核。", checked("tun", "enable")) { setBlock("tun", "enable", it) }
                SelectField("协议栈", blockValue("tun", "stack", "mixed"), listOf("system", "gvisor", "mixed")) { setBlock("tun", "stack", it) }
                SettingsSwitchRow("自动路由", "由 Mihomo 接管匹配流量的路由。", checked("tun", "auto-route", true)) { setBlock("tun", "auto-route", it) }
                SettingsSwitchRow("自动检测网卡", "自动选择出口网卡。", checked("tun", "auto-detect-interface", true)) { setBlock("tun", "auto-detect-interface", it) }
                SettingsSwitchRow("严格路由", "减少非预期流量绕过 TUN。", checked("tun", "strict-route")) { setBlock("tun", "strict-route", it) }
                SettingsTextField("DNS 劫持", linesValue("tun", "dns-hijack", "any:53\ntcp://any:53"), { setBlock("tun", "dns-hijack", it) }, Modifier.fillMaxWidth(), "每行一个目标")
                SettingsTextField("MTU", blockValue("tun", "mtu"), { setBlock("tun", "mtu", it) }, Modifier.fillMaxWidth(), "可留空使用内核默认值")
            }
            SettingsPanel("流量嗅探") {
                SettingsSwitchRow("启用嗅探", "识别部分连接的目标域名。", checked("sniffer", "enable")) { setBlock("sniffer", "enable", it) }
                SettingsSwitchRow("强制 DNS 映射", "将嗅探结果关联到 DNS 映射。", checked("sniffer", "force-dns-mapping")) { setBlock("sniffer", "force-dns-mapping", it) }
                SettingsSwitchRow("解析纯 IP", "对纯 IP 目标也尝试解析域名。", checked("sniffer", "parse-pure-ip")) { setBlock("sniffer", "parse-pure-ip", it) }
                SettingsSwitchRow("覆盖目标地址", "用嗅探到的域名覆盖连接目标。", checked("sniffer", "override-destination")) { setBlock("sniffer", "override-destination", it) }
            }
            SettingsPanel("时间同步") {
                SettingsSwitchRow("启用 NTP", "生成可选的 Mihomo 时间同步配置。", checked("ntp", "enable")) { setBlock("ntp", "enable", it) }
                SettingsTextField("NTP 服务器", blockValue("ntp", "server", "time.apple.com"), { setBlock("ntp", "server", it) }, Modifier.fillMaxWidth(), "默认 time.apple.com")
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SettingsTextField("端口", blockValue("ntp", "port", "123"), { setBlock("ntp", "port", it) }, Modifier.weight(1f))
                    SettingsTextField("同步间隔（分钟）", blockValue("ntp", "interval", "30"), { setBlock("ntp", "interval", it) }, Modifier.weight(1f))
                }
                SettingsSwitchRow("写入系统时间", "需要 Mihomo 运行环境具备相应权限。", checked("ntp", "write-to-system")) { setBlock("ntp", "write-to-system", it) }
            }
            if (error != null) Text(error!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
        }
        Row(Modifier.fillMaxWidth().padding(horizontal = JichangSpacing.pageHorizontal, vertical = 8.dp), horizontalArrangement = Arrangement.End) {
            Button(enabled = changed, onClick = {
                runCatching { MihomoSettings.normalizeVisualSettings(values) }
                    .onSuccess { normalized -> error = null; onSave(normalized) }
                    .onFailure { error = it.message ?: "配置值无效" }
            }) { Text("保存基础配置") }
        }
    }
}

@Composable
private fun AdvancedYamlPage(
    profile: ConfigProfile,
    templateRoot: Map<String, Any?>,
    onSave: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val initial = remember(profile.id, profile.advancedYaml, templateRoot) {
        profile.advancedYaml ?: MihomoSettings.dumpAdvancedFields(templateRoot)
    }
    var yaml by remember(profile.id, initial) { mutableStateOf(initial) }
    var error by remember(profile.id) { mutableStateOf<String?>(null) }
    var validation by remember(profile.id) { mutableStateOf<String?>(null) }
    Column(modifier.fillMaxSize().padding(horizontal = JichangSpacing.pageHorizontal, vertical = JichangSpacing.pageVertical), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("高级 YAML", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold)
        Text("仅编辑表单未管理的 Mihomo 字段。代理、策略组、规则、规则集和可视化设置字段不能在这里覆盖；DNS、TUN 等区块中未由表单展示的字段仍可编辑。", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        OutlinedTextField(
            value = yaml,
            onValueChange = { yaml = it; error = null; validation = null },
            modifier = Modifier.weight(1f).fillMaxWidth(),
            label = { Text("高级字段 YAML") },
            placeholder = { Text("例如：\nprofile:\n  store-selected: true") },
            textStyle = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
            minLines = 12,
        )
        error?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
        validation?.let { Text(it, color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.bodySmall) }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End)) {
            OutlinedButton(onClick = {
                runCatching { MihomoSettings.validateAdvancedYaml(yaml) }
                    .onSuccess { error = null; validation = "YAML 有效 · ${it.size} 个顶层字段" }
                    .onFailure { error = it.message ?: "高级 YAML 无效"; validation = null }
            }) { Text("校验") }
            Button(onClick = {
                runCatching { MihomoSettings.validateAdvancedYaml(yaml) }
                    .onSuccess { error = null; validation = null; onSave(yaml) }
                    .onFailure { error = it.message ?: "高级 YAML 无效" }
            }) { Text("保存高级字段") }
        }
    }
}

@Composable
private fun SettingsPanel(title: String, content: @Composable ColumnScope.() -> Unit) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            content()
        }
    }
}

@Composable
private fun SettingsSwitchRow(title: String, summary: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f).padding(end = 12.dp)) {
            Text(title, style = MaterialTheme.typography.bodyMedium)
            Text(summary, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun SettingsTextField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    supportingText: String? = null,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier,
        label = { Text(label) },
        supportingText = supportingText?.let { { Text(it) } },
        singleLine = true,
    )
}

@Composable
private fun RulesPage(
    state: AppState,
    viewModel: AppViewModel,
    issues: List<com.jzb.jichang.android.service.RuleIssue>,
    section: RuleSection,
    onSectionChange: (RuleSection) -> Unit,
    modifier: Modifier = Modifier,
    onEditGroup: (PolicyGroup) -> Unit,
    onEditRule: (Int, RoutingRule) -> Unit,
    onAddRule: () -> Unit,
    onAddGroup: () -> Unit,
    onProviders: (String?) -> Unit,
    onSaveRuleProfile: (RuleProfile) -> Unit,
    templateRoot: Map<String, Any?>,
    onSaveMihomoSettings: (Map<String, Any?>) -> Unit,
    onSaveAdvancedYaml: (String) -> Unit,
) {
    var query by remember(state.activeProfile.id) { mutableStateOf("") }
    var category by remember(state.activeProfile.id) { mutableStateOf("全部类型") }
    var strategy by remember(state.activeProfile.id) { mutableStateOf("全部策略") }
    var filtersExpanded by remember(state.activeProfile.id) { mutableStateOf(false) }
    var selecting by remember(state.activeProfile.id) { mutableStateOf(false) }
    var selectedRules by remember(state.activeProfile.id) { mutableStateOf(setOf<Int>()) }
    var deleting by remember { mutableStateOf(false) }
    var deletingIndex by remember { mutableStateOf<Int?>(null) }
    var groupToDelete by remember { mutableStateOf<PolicyGroup?>(null) }
    var showSimulator by remember { mutableStateOf(false) }
    var showTemplateRuleSetApply by remember { mutableStateOf(false) }
    var addProviderMenu by remember { mutableStateOf(false) }
    var providerFilter by remember(state.activeProfile.id) { mutableStateOf("全部来源") }
    var focusedProviderId by remember { mutableStateOf<String?>(null) }
    var focusedSubRuleName by remember { mutableStateOf<String?>(null) }
    var bulkTarget by remember { mutableStateOf("") }
    LaunchedEffect(section) {
        if (section != RuleSection.List) { selecting = false; selectedRules = emptySet() }
    }
    val blockingIssues = issues.count { !it.warning }
    val targetNames = (state.ruleProfile.groups.map { it.name } + listOf("DIRECT", "REJECT")).distinct()
    val categories = listOf("全部类型", "域名", "IP 与地理", "端口与网络", "进程", "规则集", "逻辑与兜底")
    val filtered = query.isNotBlank() || category != "全部类型" || strategy != "全部策略"
    val lastMovableIndex = state.ruleProfile.rules.lastIndex - if (state.ruleProfile.rules.any { it.type == "MATCH" }) 1 else 0
    val visibleRules = remember(section, state.ruleProfile.rules, query, category, strategy) {
        if (section != RuleSection.List) emptyList() else state.ruleProfile.rules.mapIndexed { index, rule -> index to rule }.filter { (_, rule) ->
            val matchesText = query.isBlank() || run {
                val conditionText = buildString {
                    fun appendCondition(condition: RuleCondition) {
                        append(' '); append(condition.type.orEmpty()); append(' '); append(condition.value); append(' '); append(condition.argument.orEmpty())
                        condition.children.forEach(::appendCondition)
                    }
                    rule.conditions.forEach(::appendCondition)
                }
                listOf(rule.type, rule.value, rule.group, rule.rawLine.orEmpty(), rule.extraParameters.joinToString(" "), conditionText).any { it.contains(query, true) }
            }
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
    }
    when (section) {
        RuleSection.List -> LazyColumn(modifier, contentPadding = PaddingValues(horizontal = JichangSpacing.pageHorizontal, vertical = JichangSpacing.pageVertical), verticalArrangement = Arrangement.spacedBy(JichangSpacing.item)) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("分流规则", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold)
                    Text("按列表顺序匹配，MATCH 固定在最后。", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    if (issues.isNotEmpty()) Card(onClick = { onSectionChange(RuleSection.Diagnostics) }, colors = CardDefaults.cardColors(containerColor = if (blockingIssues > 0) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.tertiaryContainer)) {
                        Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text("$blockingIssues 个错误 · ${issues.size - blockingIssues} 个可能冲突", Modifier.weight(1f), style = MaterialTheme.typography.labelLarge)
                            Text("查看校验", style = MaterialTheme.typography.labelMedium)
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        OutlinedTextField(query, { query = it; selectedRules = emptySet() }, Modifier.weight(1f).heightIn(min = 56.dp), label = { Text("搜索规则") }, singleLine = true)
                        Button(onClick = onAddRule, modifier = Modifier.heightIn(min = 56.dp)) { Icon(Icons.Outlined.Add, null); Text("规则") }
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("${visibleRules.size} 条规则", Modifier.weight(1f), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        TextButton(onClick = { filtersExpanded = !filtersExpanded }) { Text(if (filtersExpanded) "收起筛选" else "筛选") }
                        TextButton(onClick = { showSimulator = true }) { Text("模拟") }
                    }
                    if (filtersExpanded) Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        ChoiceMenu(category, categories, { category = it; selectedRules = emptySet() }, Modifier.weight(1f))
                        ChoiceMenu(strategy, listOf("全部策略") + targetNames, { strategy = it; selectedRules = emptySet() }, Modifier.weight(1f))
                    }
                    if (filtered) Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("筛选期间暂停排序。", Modifier.weight(1f), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        TextButton(onClick = { query = ""; category = "全部类型"; strategy = "全部策略"; selectedRules = emptySet() }) { Text("清除筛选") }
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("管理", Modifier.weight(1f), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                        TextButton(onClick = { selecting = !selecting; selectedRules = emptySet() }, enabled = state.ruleProfile.rules.any { it.type != "MATCH" } || selecting) { Text(if (selecting) "取消选择" else "批量操作") }
                    }
                    if (selecting && selectedRules.isNotEmpty()) {
                        Text("已选 ${selectedRules.size} 条", style = MaterialTheme.typography.labelMedium)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                            Box(Modifier.weight(1f)) { SelectField("更改目标", bulkTarget, targetNames) { bulkTarget = it } }
                            TextButton(enabled = bulkTarget in targetNames, onClick = { viewModel.changeRuleTargets(selectedRules, bulkTarget); selectedRules = emptySet(); selecting = false }) { Text("应用") }
                            TextButton(onClick = { deleting = true }) { Text("删除", color = MaterialTheme.colorScheme.error) }
                        }
                    }
                    Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        RuleManagementEntry("策略组", "${state.ruleProfile.groups.size} 个", Modifier.weight(1f).fillMaxHeight(), compact = true) { onSectionChange(RuleSection.Groups) }
                        RuleManagementEntry("规则集", "${state.ruleProfile.providers.size} 个", Modifier.weight(1f).fillMaxHeight(), compact = true) { onSectionChange(RuleSection.Providers) }
                    }
                    Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        RuleManagementEntry("子规则", "${state.ruleProfile.subRules.size} 个", Modifier.weight(1f).fillMaxHeight(), compact = true) { onSectionChange(RuleSection.SubRules) }
                        RuleManagementEntry("校验", if (issues.isEmpty()) "配置正常" else "${issues.size} 项提醒", Modifier.weight(1f).fillMaxHeight(), compact = true) { onSectionChange(RuleSection.Diagnostics) }
                    }
                    Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        RuleManagementEntry("基础配置", "通用 · DNS · TUN · 嗅探 · NTP", Modifier.weight(1f).fillMaxHeight(), compact = true) { onSectionChange(RuleSection.General) }
                        RuleManagementEntry("高级 YAML", "模板未托管字段", Modifier.weight(1f).fillMaxHeight(), compact = true) { onSectionChange(RuleSection.Advanced) }
                    }
                    Text("规则列表", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                }
            }
            if (state.ruleProfile.rules.isEmpty()) item { EmptyCard("还没有分流规则", "添加规则，或从规则集页面应用模板规则。") }
            else if (visibleRules.isEmpty()) item { EmptyCard("没有符合条件的规则", "清除搜索词或筛选条件后再试。") }
            else items(visibleRules, key = { it.first }) { (index, rule) ->
                RuleListCard(rule = rule, onEdit = { onEditRule(index, rule) }, onMoveUp = { viewModel.moveRule(index, -1) }, onMoveDown = { viewModel.moveRule(index, 1) },
                    onDelete = { deletingIndex = index }, canMoveUp = !filtered && index > 0 && rule.type != "MATCH", canMoveDown = !filtered && index < lastMovableIndex && rule.type != "MATCH",
                    selectable = selecting && !rule.type.equals("MATCH", true), checked = index in selectedRules,
                    onCheckedChange = { checked -> selectedRules = if (checked) selectedRules + index else selectedRules - index },
                    onDuplicate = { viewModel.duplicateRule(index) }, showReorder = !filtered && !selecting)
            }
        }
        RuleSection.Groups -> Column(modifier.padding(horizontal = JichangSpacing.pageHorizontal)) {
            Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("规则的出口目标", Modifier.weight(1f), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Button(onClick = onAddGroup) { Icon(Icons.Outlined.Add, null); Text("策略组") }
            }
            if (state.ruleProfile.groups.isEmpty()) EmptyCard("没有策略组", "添加策略组，为规则设置出口目标。")
            else LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp), contentPadding = PaddingValues(bottom = 16.dp)) {
                items(state.ruleProfile.groups, key = { it.name }) { group ->
                    val references = state.ruleProfile.rules.count { it.group == group.name } + state.ruleProfile.subRules.sumOf { sub -> sub.rules.count { it.group == group.name } }
                    val activeNodes = state.nodes.count { node -> (node.sourceId == null || node.sourceId in state.activeProfile.selectedSourceIds) && node.id in state.activeProfile.enabledNodeIds }
                    val memberSummary = if (group.members.isEmpty() && !group.membersExplicit) "$activeNodes 个默认节点" else "${group.members.size} 个成员"
                    Card(onClick = { onEditGroup(group) }, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                        Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) { Text(group.name, style = MaterialTheme.typography.titleSmall); Text("${group.type} · $memberSummary · 被引用 $references 次", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                            IconButton(enabled = state.ruleProfile.groups.size > 1, onClick = { groupToDelete = group }) { Icon(Icons.Outlined.Delete, "删除策略组") }
                        }
                    }
                }
            }
        }
        RuleSection.Providers -> Column(modifier.padding(horizontal = JichangSpacing.pageHorizontal)) {
            Text("从模板应用已有规则集，或添加远程下载地址、本机规则项。保存规则集后，还需在规则列表添加 RULE-SET 规则并选择目标策略。", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("规则集来源", Modifier.weight(1f), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Box {
                    Button(onClick = { addProviderMenu = true }) { Icon(Icons.Outlined.Add, null); Text("规则集") }
                    DropdownMenu(addProviderMenu, { addProviderMenu = false }) {
                        DropdownMenuItem(text = { Text("从模板应用") }, onClick = { addProviderMenu = false; showTemplateRuleSetApply = true })
                        DropdownMenuItem(text = { Text("添加远程规则集") }, onClick = { addProviderMenu = false; onProviders("http") })
                        DropdownMenuItem(text = { Text("编写本机规则集") }, onClick = { addProviderMenu = false; onProviders("inline") })
                    }
                }
            }
            ChoiceMenu(providerFilter, listOf("全部来源", "来自模板", "远程", "本机"), { providerFilter = it }, Modifier.fillMaxWidth())
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically) {
                OutlinedButton(
                    enabled = state.ruleProfile.providers.any { it.type.equals("http", true) } && !viewModel.refreshingAllRuleProviders && viewModel.refreshingRuleProviderIds.isEmpty(),
                    onClick = { viewModel.refreshAllRuleProviders(state.activeProfile) },
                ) { Text(if (viewModel.refreshingAllRuleProviders) "刷新中…" else "刷新全部远程") }
                TextButton(onClick = { onProviders(null) }) { Text("管理") }
            }
            val providers = state.ruleProfile.providers.filter { provider -> when (providerFilter) {
                "来自模板" -> provider.sourceTemplateId != null
                "远程" -> provider.sourceTemplateId == null && provider.type == "http"
                "本机" -> provider.sourceTemplateId == null && provider.type != "http"
                else -> true
            } }
            if (providers.isEmpty()) EmptyCard("没有规则集", if (state.ruleProfile.providers.isEmpty()) "可从模板应用，或添加远程和本机规则集。" else "当前来源没有规则集。")
            else LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp), contentPadding = PaddingValues(top = 8.dp, bottom = 16.dp)) {
                items(providers, key = { it.id }) { provider ->
                    val status = state.ruleProviderStatuses.firstOrNull { it.profileId == state.activeProfile.id && it.providerId == provider.id }
                    val refreshing = provider.id in viewModel.refreshingRuleProviderIds
                    val originLabel = when {
                        provider.sourceTemplateId != null -> "来自模板${provider.sourceTemplateName?.let { " · $it" }.orEmpty()}"
                        provider.type == "http" -> "远程"
                        else -> "本机"
                    }
                    Card(colors = CardDefaults.cardColors(containerColor = if (focusedProviderId == provider.id) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface)) {
                        Column(Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(provider.name, style = MaterialTheme.typography.titleSmall)
                            Text("$originLabel · ${provider.behavior} · ${provider.format}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(status?.error?.let { "刷新失败：$it" } ?: status?.refreshedAt?.let { "上次刷新 ${java.text.DateFormat.getDateTimeInstance().format(java.util.Date(it))}" } ?: if (provider.type == "inline") "内嵌 ${provider.payload.size} 项" else "尚未刷新", style = MaterialTheme.typography.labelSmall, color = if (status?.error != null) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant)
                            Row {
                                if (provider.type == "http") TextButton(enabled = !refreshing && !viewModel.refreshingAllRuleProviders, onClick = { viewModel.refreshRuleProvider(state.activeProfile, provider) }) { Text(if (refreshing) "刷新中…" else "刷新") }
                                TextButton(onClick = { viewModel.previewRuleProvider(state.activeProfile, provider, status) }) { Text("预览") }
                                TextButton(onClick = { onProviders("edit:${provider.id}") }) { Text("编辑") }
                            }
                        }
                    }
                }
            }
        }
        RuleSection.SubRules -> Column(modifier.padding(horizontal = JichangSpacing.pageHorizontal)) {
            Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("可在主规则中引用", Modifier.weight(1f), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Button(onClick = { onProviders("subrules") }) { Text("管理子规则") }
            }
            if (state.ruleProfile.subRules.isEmpty()) EmptyCard("没有子规则", "添加子规则集合，在主规则中复用。")
            else LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp), contentPadding = PaddingValues(bottom = 16.dp)) {
                items(state.ruleProfile.subRules, key = { it.name }) { sub ->
                    val references = state.ruleProfile.rules.count { it.type == "SUB-RULE" && it.value == sub.name }
                    Card(onClick = { focusedSubRuleName = sub.name; onProviders("subrules") }, colors = CardDefaults.cardColors(containerColor = if (focusedSubRuleName == sub.name) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface)) {
                        Column(Modifier.fillMaxWidth().padding(12.dp)) {
                            Text(sub.name, style = MaterialTheme.typography.titleSmall)
                            Text("${sub.rules.size} 条规则 · 被引用 $references 次", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            sub.rules.take(2).forEach { Text("${it.type} · ${it.value} → ${it.group}", style = MaterialTheme.typography.labelSmall, maxLines = 1, overflow = TextOverflow.Ellipsis) }
                        }
                    }
                }
            }
        }
        RuleSection.Diagnostics -> LazyColumn(modifier, contentPadding = PaddingValues(horizontal = JichangSpacing.pageHorizontal, vertical = JichangSpacing.pageVertical), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            item { Text("检查策略目标、规则集引用、条件格式和兜底顺序。", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
            if (issues.isEmpty()) item { EmptyCard("规则配置正常", "当前规则及其引用没有发现静态校验问题。") }
            else items(issues) { issue ->
                Card(onClick = {
                    when {
                        issue.index >= 0 && issue.index in state.ruleProfile.rules.indices -> onEditRule(issue.index, state.ruleProfile.rules[issue.index])
                        issue.providerId != null -> { focusedProviderId = issue.providerId; onSectionChange(RuleSection.Providers) }
                        issue.subRuleName != null -> { focusedSubRuleName = issue.subRuleName; onSectionChange(RuleSection.SubRules) }
                        issue.groupName != null -> { onSectionChange(RuleSection.Groups); state.ruleProfile.groups.firstOrNull { it.name == issue.groupName }?.let(onEditGroup) }
                    }
                }, colors = CardDefaults.cardColors(containerColor = if (issue.warning) MaterialTheme.colorScheme.tertiaryContainer else MaterialTheme.colorScheme.errorContainer)) {
                    Column(Modifier.fillMaxWidth().padding(14.dp)) {
                        Text("${if (issue.warning) "可能冲突 · " else ""}${if (issue.index >= 0) "第 ${issue.index + 1} 条 · ${issue.rule.type}" else issue.rule.type.ifBlank { "配置" }}", style = MaterialTheme.typography.labelLarge)
                        Text(issue.message, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
        RuleSection.General -> MihomoSettingsPage(
            profile = state.activeProfile,
            templateRoot = templateRoot,
            onSave = onSaveMihomoSettings,
            modifier = modifier,
        )
        RuleSection.Advanced -> AdvancedYamlPage(
            profile = state.activeProfile,
            templateRoot = templateRoot,
            onSave = onSaveAdvancedYaml,
            modifier = modifier,
        )
    }
    if (deleting) JichangAlertDialog(onDismissRequest = { deleting = false }, title = { Text("删除所选规则？") },
        text = { Text("将从当前配置中删除 ${selectedRules.size} 条规则，此操作无法撤销。") },
        confirmButton = { TextButton(onClick = { viewModel.removeRules(selectedRules); selectedRules = emptySet(); selecting = false; deleting = false }) { Text("删除", color = MaterialTheme.colorScheme.error) } },
        dismissButton = { TextButton(onClick = { deleting = false }) { Text("取消") } })
    deletingIndex?.let { index -> state.ruleProfile.rules.getOrNull(index)?.let { rule -> JichangAlertDialog(
        onDismissRequest = { deletingIndex = null }, title = { Text("删除规则？") },
        text = { Text("${rule.type} · ${ruleSummary(rule)} → ${rule.group}") },
        confirmButton = { TextButton(onClick = { viewModel.removeRule(index); deletingIndex = null }) { Text("删除", color = MaterialTheme.colorScheme.error) } },
        dismissButton = { TextButton(onClick = { deletingIndex = null }) { Text("取消") } }) } }
    groupToDelete?.let { group -> JichangAlertDialog(onDismissRequest = { groupToDelete = null }, title = { Text("删除策略组？") },
        text = { Text("主规则和子规则中指向“${group.name}”的目标都会改为其他现有策略组。") },
        confirmButton = { TextButton(onClick = { viewModel.removeGroup(group.name); groupToDelete = null }) { Text("删除", color = MaterialTheme.colorScheme.error) } },
        dismissButton = { TextButton(onClick = { groupToDelete = null }) { Text("取消") } }) }
    if (showSimulator) RuleSimulationDialog(state.activeProfile, state.ruleProviderStatuses, onDismiss = { showSimulator = false })
    if (showTemplateRuleSetApply) TemplateRuleSetApplyDialog(templates = state.templates, current = state.ruleProfile,
        onDismiss = { showTemplateRuleSetApply = false }, onApply = { updated -> onSaveRuleProfile(updated); showTemplateRuleSetApply = false })
    viewModel.ruleProviderPreview?.let { (name, content) -> JichangAlertDialog(onDismissRequest = viewModel::dismissRuleProviderPreview,
        title = { Text("规则集预览：$name") }, text = { Column(Modifier.heightIn(max = 540.dp).verticalScroll(rememberScrollState())) { Text(content, style = MaterialTheme.typography.bodySmall, fontFamily = FontFamily.Monospace) } },
        confirmButton = { TextButton(onClick = viewModel::dismissRuleProviderPreview) { Text("关闭") } }) }
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
    FullScreenEditorDialog(
        title = "应用模板规则集",
        dirty = selected.isNotEmpty() || conflictActions.isNotEmpty() || renamed.isNotEmpty(),
        valid = template != null && selected.isNotEmpty() && !unresolvedConflict && !duplicateName,
        saveLabel = "应用",
        onDismiss = onDismiss,
        onSave = {
            template?.let { selectedTemplate ->
                val actions = selected.mapNotNull { id -> conflictActions[id]?.let { action ->
                    id to when (action) {
                        "保留现有" -> com.jzb.jichang.android.service.TemplateProviderConflictAction.KEEP_EXISTING
                        "替换现有" -> com.jzb.jichang.android.service.TemplateProviderConflictAction.REPLACE
                        "改名导入项" -> com.jzb.jichang.android.service.TemplateProviderConflictAction.RENAME
                        else -> null
                    }
                } }.mapNotNull { (id, action) -> action?.let { id to it } }.toMap()
                runCatching { com.jzb.jichang.android.service.TemplateRuleSetImporter.apply(current, selectedTemplate, selected, actions, renamed) }
                    .onSuccess(onApply).onFailure { importError = it.message ?: "模板规则集导入失败" }
            }
        },
        content = { Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
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
    )
}

@Composable
private fun RuleSimulationDialog(profile: ConfigProfile, statuses: List<com.jzb.jichang.android.model.RuleProviderStatus>, onDismiss: () -> Unit) {
    val ruleProfile = profile.ruleProfile
    val cacheDirectory = java.io.File(LocalContext.current.filesDir, "rule-providers/${profile.id}")
    val scope = rememberCoroutineScope()
    var host by remember { mutableStateOf("") }
    var destinationPort by remember { mutableStateOf("") }
    var sourcePort by remember { mutableStateOf("") }
    var network by remember { mutableStateOf("") }
    var processName by remember { mutableStateOf("") }
    var result by remember { mutableStateOf<com.jzb.jichang.android.service.RuleSimulationResult?>(null) }
    var loading by remember { mutableStateOf(false) }
    var cacheSummary by remember { mutableStateOf<String?>(null) }
    val certainty = result?.certainty
    JichangAlertDialog(onDismissRequest = onDismiss, title = { Text("规则模拟 · 静态推演") }, text = {
        Column(Modifier.heightIn(max = 560.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("按当前规则及本机已保存的 YAML/文本规则集推演。未刷新、MRS 格式及 Geo 数据等无法读取的条件会标记为未知；结果不代表 Mihomo 运行时。", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            OutlinedTextField(host, { host = it }, Modifier.fillMaxWidth(), label = { Text("域名或目标 IP") }, singleLine = true)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(destinationPort, { destinationPort = it.filter(Char::isDigit) }, Modifier.weight(1f), label = { Text("目标端口") }, singleLine = true)
                OutlinedTextField(sourcePort, { sourcePort = it.filter(Char::isDigit) }, Modifier.weight(1f), label = { Text("来源端口") }, singleLine = true)
            }
            SelectField("网络协议", network.ifBlank { "不指定" }, listOf("不指定", "TCP", "UDP")) { network = if (it == "不指定") "" else it }
            OutlinedTextField(processName, { processName = it }, Modifier.fillMaxWidth(), label = { Text("进程名（可选）") }, singleLine = true)
            OutlinedButton(enabled = host.isNotBlank() && !loading, onClick = {
                val input = com.jzb.jichang.android.service.RuleSimulationInput(
                    hostOrIp = host,
                    destinationPort = destinationPort.toIntOrNull(), sourcePort = sourcePort.toIntOrNull(),
                    network = network.takeIf(String::isNotBlank), processName = processName.takeIf(String::isNotBlank),
                )
                loading = true
                result = null
                scope.launch {
                    try {
                        val (outcome, summary) = withContext(Dispatchers.IO) {
                            val reader = com.jzb.jichang.android.service.RuleProviderRefresher()
                            val localRuleSets = mutableMapOf<String, List<String>>()
                            val unavailable = mutableListOf<String>()
                            val referencedNames = ruleProfile.rules.filter { it.type.equals("RULE-SET", true) }.map { it.value }.toSet()
                            ruleProfile.providers.filter { it.name in referencedNames && !it.type.equals("inline", true) }.forEach { provider ->
                                val status = statuses.firstOrNull { it.profileId == profile.id && it.providerId == provider.id }
                                val entries = runCatching { reader.simulationEntries(provider, cacheDirectory, status?.cacheFileName) }.getOrNull()
                                if (entries == null) unavailable += provider.name else localRuleSets[provider.id] = entries
                            }
                            val simulation = com.jzb.jichang.android.service.RuleSimulator.simulate(ruleProfile, input, localRuleSets)
                            val cacheInfo = if (unavailable.isEmpty()) null else "未读取的规则集：${unavailable.joinToString("、")}。相关规则按未知处理，可先刷新远程规则集。"
                            simulation to cacheInfo
                        }
                        result = outcome
                        cacheSummary = summary
                    } catch (error: Exception) {
                        cacheSummary = "模拟失败：${error.message ?: "无法读取规则"}"
                    } finally {
                        loading = false
                    }
                }
            }, modifier = Modifier.fillMaxWidth()) { Text(if (loading) "模拟中…" else "模拟") }
            cacheSummary?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
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
                    outcome.uncertainIndexes.forEach { index -> ruleProfile.rules.getOrNull(index)?.let { Text("第 ${index + 1} 条 · ${it.type} → ${it.group} · 依赖外部数据或未提供条件", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) } }
                }
            }
        }
    }, confirmButton = { TextButton(onClick = onDismiss) { Text("完成") } })
}

@Composable
private fun RuleListCard(rule: RoutingRule, onEdit: () -> Unit, onMoveUp: () -> Unit, onMoveDown: () -> Unit, onDelete: () -> Unit, canMoveUp: Boolean, canMoveDown: Boolean, selectable: Boolean, checked: Boolean, onCheckedChange: (Boolean) -> Unit, onDuplicate: () -> Unit, showReorder: Boolean = true) {
    var expanded by remember { mutableStateOf(false) }
    val dragThreshold = with(LocalDensity.current) { 64.dp.toPx() }
    var dragRemainder by remember(rule, selectable) { mutableStateOf(0f) }
    Card(onClick = { if (selectable) onCheckedChange(!checked) else onEdit() }, modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Row(Modifier.padding(start = 14.dp, end = 4.dp, top = 8.dp, bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            if (selectable) Checkbox(checked = checked, onCheckedChange = onCheckedChange)
            Column(Modifier.weight(1f)) {
                Text(if (rule.type == "MATCH") "最终兜底 · MATCH" else rule.type, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
                Text(ruleSummary(rule), style = MaterialTheme.typography.bodyMedium, maxLines = 2)
                val parameters = buildList { if (rule.noResolve) add("no-resolve"); if (rule.source) add("src"); addAll(rule.extraParameters) }
                Text("策略  ${rule.group}${parameters.takeIf { it.isNotEmpty() }?.joinToString(prefix = " · 参数 ", separator = ", ").orEmpty()}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (showReorder && !selectable && rule.type != "MATCH") Icon(Icons.Outlined.DragHandle, contentDescription = "长按拖动调整顺序", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier
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
                }.padding(8.dp))
            Box {
                IconButton(onClick = { expanded = true }) { Icon(Icons.Outlined.MoreVert, "规则操作") }
                DropdownMenu(expanded, { expanded = false }) {
                    DropdownMenuItem(text = { Text("编辑") }, onClick = { expanded = false; onEdit() })
                    DropdownMenuItem(enabled = rule.type != "MATCH", text = { Text("复制") }, onClick = { expanded = false; onDuplicate() })
                    if (showReorder) DropdownMenuItem(text = { Text("上移") }, enabled = canMoveUp, onClick = { expanded = false; onMoveUp() })
                    if (showReorder) DropdownMenuItem(text = { Text("下移") }, enabled = canMoveDown, onClick = { expanded = false; onMoveDown() })
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
    format: ExportFormat,
    onFormatChange: (ExportFormat) -> Unit,
    singBoxIssues: List<SingBoxExportIssue>,
    onOpenSingBoxIssue: (SingBoxExportIssue) -> Unit,
    configReady: Boolean,
    generationError: String?,
    generatedNodes: Int,
    skippedNodes: Int,
    referencedSubscriptions: Int,
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
    onOpenIssues: () -> Unit,
    onPreview: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val profile = state.activeProfile
    val blockingRuleIssues = ruleIssues.filterNot { it.warning }
    Column(modifier.padding(horizontal = JichangSpacing.pageHorizontal).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(JichangSpacing.section)) {
        SegmentedTabs(ExportFormat.entries.map { it.label }, format.ordinal) { onFormatChange(ExportFormat.entries[it]) }
        if (!configReady && singBoxIssues.isEmpty()) Card(Modifier.fillMaxWidth().padding(top = 6.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
            Text(generationError ?: "正在生成配置…", Modifier.padding(JichangSpacing.card), style = MaterialTheme.typography.bodySmall, color = if (generationError == null) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.error)
        }
        if (format == ExportFormat.SingBox && singBoxIssues.isNotEmpty()) {
            Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)) {
                Column(Modifier.padding(JichangSpacing.card), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("sing-box 导出受阻 · ${singBoxIssues.size} 项", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onErrorContainer)
                    singBoxIssues.forEach { issue ->
                        TextButton(onClick = { onOpenSingBoxIssue(issue) }, modifier = Modifier.fillMaxWidth()) {
                            Text("${issue.location}：${issue.message}", modifier = Modifier.weight(1f), textAlign = TextAlign.Start, color = MaterialTheme.colorScheme.onErrorContainer)
                            Text("查看", color = MaterialTheme.colorScheme.onErrorContainer)
                        }
                    }
                }
            }
        }
        if (configReady && ruleIssues.isEmpty() && unresolvedTemplateProviders.isEmpty()) Card(Modifier.fillMaxWidth().padding(top = 6.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
            Text("配置可导出", Modifier.padding(JichangSpacing.card), style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onPrimaryContainer)
        }
        if (ruleIssues.isNotEmpty()) Card(onClick = onOpenIssues, modifier = Modifier.fillMaxWidth().padding(top = 6.dp), colors = CardDefaults.cardColors(containerColor = if (blockingRuleIssues.isEmpty()) MaterialTheme.colorScheme.tertiaryContainer else MaterialTheme.colorScheme.errorContainer)) {
            Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("规则校验：${blockingRuleIssues.size} 个错误 · ${ruleIssues.size - blockingRuleIssues.size} 个提醒", style = MaterialTheme.typography.titleSmall)
                    Text(if (blockingRuleIssues.isEmpty()) "可以继续导出，建议检查可能冲突。" else "请先修正规则错误后再导出。", style = MaterialTheme.typography.bodySmall)
                }
                Text("查看", style = MaterialTheme.typography.labelMedium)
            }
        }
        if (unresolvedTemplateProviders.isNotEmpty()) Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)) {
            Text("尚未绑定模板订阅：${unresolvedTemplateProviders.joinToString("、")}", Modifier.padding(12.dp), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onErrorContainer)
        }
        shareUrl?.let { ActiveShareCard(it, onShareLink, onStopShare) }
        Card(Modifier.fillMaxWidth().padding(top = 6.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
            Column(Modifier.padding(JichangSpacing.card), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(profile.name, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text("${generatedNodes} 个内嵌节点${if (referencedSubscriptions > 0) " · $referencedSubscriptions 个订阅引用" else ""}${if (skippedNodes > 0) " · 跳过 $skippedNodes 个不支持节点" else ""}", style = MaterialTheme.typography.bodySmall)
                Text("文件名：$filename", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        if (format == ExportFormat.Mihomo) {
        Text("节点来源", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
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
        } else {
            Text("面向发布时最新版 sing-box 稳定版与 Android SFA。订阅使用手机上已保存的节点快照；刷新订阅后需重新导出 JSON。", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("规则集使用本地已刷新内容。若规则集尚未刷新，请到「规则 → 规则集」刷新后再导出。", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Text(
            "在当前手机导入时，建议直接下载配置文件，再从 Downloads/鸡场 中选择；局域网链接主要用于其他设备。",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (!hasRegionalProxyGroups) OutlinedButton(onClick = onOpenFilters, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Outlined.Tune, null); Spacer(Modifier.width(8.dp)); Text("导出地区策略组")
        } else Text("模板已包含地区筛选策略组，直接沿用模板分组。", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Button(onClick = onDownload, enabled = configReady && unresolvedTemplateProviders.isEmpty() && blockingRuleIssues.isEmpty(), modifier = Modifier.weight(1f).height(JichangSpacing.touchTarget), contentPadding = PaddingValues(horizontal = 8.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.CloudDownload, null, Modifier.size(18.dp)); Spacer(Modifier.width(5.dp))
                    Text("下载配置", maxLines = 1, softWrap = false, overflow = TextOverflow.Ellipsis)
                }
            }
            OutlinedButton(onClick = onShare, enabled = configReady && unresolvedTemplateProviders.isEmpty() && blockingRuleIssues.isEmpty(), modifier = Modifier.weight(1f).height(JichangSpacing.touchTarget), contentPadding = PaddingValues(horizontal = 8.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.Link, null, Modifier.size(18.dp)); Spacer(Modifier.width(5.dp))
                    Text(if (shareUrl == null) "开启分享" else "重新分享", maxLines = 1, softWrap = false, overflow = TextOverflow.Ellipsis)
                }
            }
        }
        if (shareUrl == null) {
            if (shareError != null) Text(shareError, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
        }
        OutlinedButton(onClick = onPreview, enabled = configReady, modifier = Modifier.fillMaxWidth()) { Text(if (format == ExportFormat.Mihomo) "预览生成的 YAML" else "预览生成的 JSON") }
        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun ActiveShareCard(url: String, onShareLink: () -> Unit, onStopShare: () -> Unit) {
    Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(Modifier.padding(JichangSpacing.card), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("局域网分享已开启", Modifier.weight(1f), style = MaterialTheme.typography.titleSmall)
                TextButton(onClick = onStopShare) { Text("停止分享") }
            }
            LanShareQrCodeCard(url)
            SelectionContainer { Text(url, style = MaterialTheme.typography.bodySmall, fontFamily = FontFamily.Monospace) }
            Text("接收设备需连接可互访的同一局域网。二维码和随机链接都可访问配置，请只展示给信任的人。", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("分享在后台通过前台服务继续运行，请保留状态栏通知。若系统、省电策略或网络切换结束服务，当前链接会失效；回到应用后可重新开启。", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            OutlinedButton(onClick = onShareLink, modifier = Modifier.fillMaxWidth()) { Icon(Icons.Outlined.Share, null); Spacer(Modifier.width(5.dp)); Text("分享链接") }
        }
    }
}

@Composable
private fun YamlPreviewPage(configText: String, modifier: Modifier = Modifier) {
    SelectionContainer {
        Text(configText, modifier.verticalScroll(rememberScrollState()).padding(JichangSpacing.pageHorizontal),
            fontFamily = FontFamily.Monospace, style = MaterialTheme.typography.bodySmall)
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
            var menuExpanded by remember(template.id) { mutableStateOf(false) }
            Card(onClick = { onPreview(template) }, modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                Column(Modifier.padding(JichangSpacing.card), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Outlined.Description, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(template.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                            Text("${template.fileName} · ${if (users == 0) "尚未使用" else "$users 个配置在使用"}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Box {
                            IconButton(onClick = { menuExpanded = true }) { Icon(Icons.Outlined.MoreVert, "模板操作") }
                            DropdownMenu(menuExpanded, { menuExpanded = false }) {
                                DropdownMenuItem(text = { Text("预览") }, onClick = { menuExpanded = false; onPreview(template) })
                                DropdownMenuItem(text = { Text("导出") }, onClick = { menuExpanded = false; onExport(template) })
                                DropdownMenuItem(text = { Text("重命名") }, onClick = { menuExpanded = false; onRename(template) })
                                DropdownMenuItem(text = { Text("删除") }, onClick = { menuExpanded = false; onDelete(template) })
                            }
                        }
                    }
                    Button(onClick = { onCreate(template) }, modifier = Modifier.fillMaxWidth()) { Text("用模板新建配置") }
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
    val context = LocalContext.current
    var raw by remember { mutableStateOf("") }
    var scanError by remember { mutableStateOf<String?>(null) }
    var permissionDenied by remember { mutableStateOf(false) }
    val scanner = rememberLauncherForActivityResult(ScanContract()) { result ->
        result.contents?.let { scanned ->
            if (scanned.isBlank()) scanError = "二维码没有可导入的内容"
            else {
                raw = scanned
                scanError = null
            }
        }
    }
    fun openScanner() {
        scanError = null
        permissionDenied = false
        runCatching {
            scanner.launch(
                ScanOptions()
                    .setDesiredBarcodeFormats(ScanOptions.QR_CODE)
                    .setPrompt("将节点二维码放入取景框")
                    .setBeepEnabled(false)
                    .setCaptureActivity(NodeCaptureActivity::class.java),
            )
        }.onFailure { scanError = "无法打开相机，请重试或粘贴节点内容。" }
    }
    val cameraPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        permissionDenied = !granted
        if (granted) openScanner()
        else scanError = "需要相机权限才能扫码；您也可以粘贴节点内容。"
    }
    JichangAlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("导入节点") },
        text = { Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("粘贴或扫描 Mihomo/Clash 节点内容、节点分享链接。机场订阅二维码请在订阅页添加。")
            OutlinedButton(
                onClick = {
                    scanError = null
                    when {
                        !context.packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA_ANY) ->
                            scanError = "设备没有可用相机，请粘贴节点内容。"
                        ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED ->
                            openScanner()
                        else -> cameraPermission.launch(Manifest.permission.CAMERA)
                    }
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(Icons.Outlined.QrCodeScanner, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("扫描节点二维码")
            }
            scanError?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
            if (permissionDenied) TextButton(onClick = {
                context.startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${context.packageName}")))
            }) { Text("打开应用设置授权") }
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
    val groupTypes = listOf(
        "select" to ("手动选择" to "在成员中手动选择出口"),
        "url-test" to ("延迟优选" to "自动选择响应更快的成员"),
        "fallback" to ("故障切换" to "当前成员不可用时切换到其他成员"),
        "load-balance" to ("负载均衡" to "在可用成员间分配连接"),
    )
    val selectedGroupType = groupTypes.firstOrNull { it.first == type } ?: groupTypes.first()
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
    val initialMembers = initial?.let { if (it.membersExplicit) it.members.toSet() else if (it.members.isEmpty()) nodeNames.keys else it.members.toSet() } ?: nodeNames.keys
    FullScreenEditorDialog(
        title = if (initial == null) "新建策略组" else "编辑策略组",
        dirty = name != initial?.name.orEmpty() || type != (initial?.type ?: "select") || members != initialMembers.intersect(validMemberKeys) || selectedRules.isNotEmpty() || selectedProviders.isNotEmpty(),
        valid = name.isNotBlank(),
        saveLabel = if (initial == null) "添加" else "保存",
        onDismiss = onDismiss,
        onSave = { onSave(name, type, members.toList(), selectedRules, selectedProviders) },
        content = { Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(name, { name = it }, modifier = Modifier.fillMaxWidth(), label = { Text("组名称") }, singleLine = true)
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("策略类型", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Box {
                        OutlinedButton(onClick = { expanded = true }, modifier = Modifier.fillMaxWidth()) {
                            Column(Modifier.weight(1f), horizontalAlignment = Alignment.Start) {
                                Text(selectedGroupType.second.first, style = MaterialTheme.typography.bodyLarge)
                                Text(selectedGroupType.second.second, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Icon(Icons.Outlined.KeyboardArrowDown, contentDescription = "选择策略类型")
                        }
                        DropdownMenu(expanded, { expanded = false }) {
                            groupTypes.forEach { (value, details) ->
                                DropdownMenuItem(
                                    text = { Column {
                                        Text(details.first)
                                        Text(details.second, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    } },
                                    onClick = { type = value; expanded = false },
                                )
                            }
                        }
                    }
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text("代理成员", Modifier.weight(1f), style = MaterialTheme.typography.titleSmall)
                    Text("${members.size} 项已选", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                }
                Text("选择此组可使用的节点、其他策略组或系统出口。", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (eligibleNodes.isEmpty()) Text("当前配置没有已启用节点，可到“资源 → 节点”启用。", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                eligibleNodes.forEach { node ->
                    val key = "node:${node.id}"
                    GroupSelectionRow(node.name, key in members) { checked ->
                        members = if (checked) members + key else members - key
                    }
                }
                availableGroups.forEach { group ->
                    GroupSelectionRow("策略组 · ${group.name}", group.name in members) { checked ->
                        members = if (checked) members + group.name else members - group.name
                    }
                }
                listOf("DIRECT", "REJECT").forEach { special ->
                    GroupSelectionRow(special, special in members) { checked ->
                        members = if (checked) members + special else members - special
                    }
                }
            }
            if (initial == null) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Text("规则目标", Modifier.weight(1f), style = MaterialTheme.typography.titleSmall)
                        Text("${selectedRules.size + selectedProviders.size} 项已选", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                    }
                    Text("勾选后会把这些规则的目标设为新策略组，并为所选规则集添加路由规则。", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    state.ruleProfile.rules.forEachIndexed { index, rule ->
                        if (!rule.type.equals("MATCH", true)) GroupSelectionRow(
                            title = rule.type,
                            supportingText = "${ruleSummary(rule)} → ${rule.group}",
                            checked = index in selectedRules,
                        ) { checked ->
                            selectedRules = if (checked) selectedRules + index else selectedRules - index
                        }
                    }
                    state.ruleProfile.providers.forEach { provider ->
                        GroupSelectionRow("规则集 · ${provider.name}", provider.id in selectedProviders) { checked ->
                            selectedProviders = if (checked) selectedProviders + provider.id else selectedProviders - provider.id
                        }
                    }
                    if (state.ruleProfile.rules.none { !it.type.equals("MATCH", true) } && state.ruleProfile.providers.isEmpty()) {
                        Text("暂无可应用的规则或规则集。", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            Spacer(Modifier.height(4.dp))
        } },
    )
}

@Composable
private fun GroupSelectionRow(
    title: String,
    checked: Boolean,
    supportingText: String? = null,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        Modifier.fillMaxWidth().heightIn(min = 52.dp).toggleable(
            value = checked,
            role = Role.Checkbox,
            onValueChange = onCheckedChange,
        ).padding(end = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Checkbox(checked = checked, onCheckedChange = null)
        Column(Modifier.weight(1f).padding(vertical = 6.dp)) {
            Text(title, style = MaterialTheme.typography.bodyMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
            supportingText?.let {
                Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
        }
    }
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
    val draftRule = RoutingRule(type, value, group, noResolve, source, conditions, extraParameters.lines().map(String::trim).filter(String::isNotBlank), rawLine.takeIf(String::isNotBlank))
    val baseline = existing ?: RoutingRule("DOMAIN-SUFFIX", "", state.ruleProfile.groups.firstOrNull()?.name ?: "DIRECT")
    FullScreenEditorDialog(
        title = if (initial == null) "新建路由规则" else "编辑路由规则",
        dirty = draftRule != baseline,
        valid = valid,
        saveLabel = if (initial == null) "添加" else "保存",
        onDismiss = onDismiss,
        onSave = { onSave(initial?.first, draftRule) },
        content = { Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(9.dp)) {
                Text("规则类型", style = MaterialTheme.typography.labelLarge)
                OutlinedButton(onClick = { showTypePicker = true }, modifier = Modifier.fillMaxWidth()) { Text(if (type == "MATCH") "MATCH · 最终兜底" else type) }
                Text(ruleTypeDescription(type), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                when {
                    type == "MATCH" -> Text("MATCH 会被固定在规则列表末尾。", style = MaterialTheme.typography.bodySmall)
                    composite -> Text("此规则包含组合条件，请展开高级条件编辑条件树。", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    type == "RULE-SET" -> {
                        SelectField("规则集", value, state.ruleProfile.providers.map { it.name }, { value = it })
                        if (state.ruleProfile.providers.isEmpty()) Text("请先到规则集页面添加规则集。", color = MaterialTheme.colorScheme.error)
                    }
                    type == "SUB-RULE" -> {
                        SelectField("子规则", value, state.ruleProfile.subRules.map { it.name }, { value = it })
                        if (state.ruleProfile.subRules.isEmpty()) Text("请先到子规则页面添加子规则。", color = MaterialTheme.colorScheme.error)
                    }
                    type == "GEOSITE" -> {
                        val common = listOf("category-ads-all", "cn", "private", "google", "youtube", "apple", "microsoft", "telegram", "github", "geolocation-!cn")
                        OutlinedTextField(geositeSearch, { geositeSearch = it }, modifier = Modifier.fillMaxWidth(), label = { Text("搜索 GEOSITE 分类") }, singleLine = true)
                        OutlinedTextField(value, { value = it }, modifier = Modifier.fillMaxWidth(), label = { Text("匹配值") }, placeholder = { Text("可直接填写自定义分类") }, singleLine = true)
                        val suggestions = common.filter { geositeSearch.isBlank() || it.contains(geositeSearch, true) }
                        androidx.compose.foundation.lazy.LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) { items(suggestions) { tag -> AssistChip(onClick = { value = tag }, label = { Text(tag) }) } }
                    }
                    else -> OutlinedTextField(value, { value = it }, modifier = Modifier.fillMaxWidth(), label = { Text(if (type.startsWith("PROCESS") || type == "UID") "进程 / 用户匹配" else "匹配内容") }, placeholder = { Text(rulePlaceholder(type)) }, singleLine = true)
                }
                Box {
                    OutlinedButton(onClick = { strategyMenu = true }, modifier = Modifier.fillMaxWidth()) { Text("策略目标：$group") }
                    DropdownMenu(strategyMenu, { strategyMenu = false }) {
                        (state.ruleProfile.groups.map { it.name } + listOf("DIRECT", "REJECT")).distinct().forEach { item -> DropdownMenuItem(text = { Text(item) }, onClick = { group = item; strategyMenu = false }) }
                    }
                }
            TextButton(onClick = { editorSection = if (editorSection == 0) 1 else 0 }) { Text(if (editorSection == 0) "显示高级条件与参数" else "收起高级条件与参数") }
            if (editorSection == 1) {
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

private fun ruleTypeDescription(type: String): String = when (type) {
    "DOMAIN" -> "完整域名精确匹配。例如 example.com 不会匹配 www.example.com。"
    "DOMAIN-SUFFIX" -> "匹配域名本身及其子域名。例如 example.com 可匹配 www.example.com。"
    "DOMAIN-KEYWORD" -> "域名中包含指定文字即匹配。例如填写 google 可匹配含 google 的域名。"
    "DOMAIN-WILDCARD" -> "按域名通配符匹配；仅支持 *（任意长度）和 ?（单个字符）。例如 *.example.com。"
    "DOMAIN-REGEX" -> "用正则表达式匹配完整域名。适合需要组合条件的场景；例如 ^.+\\.example\\.com$。"
    "GEOSITE" -> "按 Mihomo 的 Geosite 域名分类匹配。例如 cn、google、category-ads-all。"
    "IP-CIDR" -> "按目标 IPv4 网段匹配，使用 CIDR 写法，例如 192.168.0.0/16。可在高级选项设置 no-resolve。"
    "IP-CIDR6" -> "按目标 IPv6 网段匹配，使用 CIDR 写法，例如 2001:db8::/32。可在高级选项设置 no-resolve。"
    "IP-SUFFIX" -> "按目标 IP 后缀范围匹配，填写 IP 网段，例如 8.8.8.0/24。"
    "IP-ASN" -> "按目标 IP 所属自治系统编号（ASN）匹配，例如 13335。"
    "GEOIP" -> "按目标 IP 的国家或地区代码匹配，例如 CN。"
    "SRC-GEOIP" -> "按来源 IP 的国家或地区代码匹配，例如 CN。"
    "SRC-IP-ASN" -> "按来源 IP 所属自治系统编号（ASN）匹配，例如 9808。"
    "SRC-IP-CIDR" -> "按来源 IPv4 网段匹配，使用 CIDR 写法，例如 192.168.1.0/24。"
    "SRC-IP-SUFFIX" -> "按来源 IP 后缀范围匹配，填写 IP 网段，例如 192.168.1.0/24。"
    "DST-PORT" -> "按连接的目标端口或端口范围匹配，例如 443 或 80-443。"
    "SRC-PORT" -> "按发起连接时使用的来源端口或端口范围匹配，例如 50000-60000。"
    "IN-PORT" -> "按 Mihomo 接收连接的入站端口匹配，也支持端口范围，例如 7890 或 7890-7900。"
    "IN-TYPE" -> "按接收连接的入站类型匹配，例如 SOCKS、HTTP 或 mixed。"
    "IN-USER" -> "按入站认证用户名匹配；多个用户名可用 / 分隔。"
    "IN-NAME" -> "按接收连接的入站名称匹配。"
    "REMATCH-NAME" -> "按 Rematch 出站写入的名称匹配。"
    "PROCESS-PATH" -> "按进程完整路径精确匹配，例如 /usr/bin/curl。"
    "PROCESS-PATH-WILDCARD" -> "按进程路径通配符匹配；仅支持 * 和 ?，例如 /usr/*/curl。"
    "PROCESS-PATH-REGEX" -> "用正则表达式匹配进程完整路径，例如 .*/bin/curl$。"
    "PROCESS-NAME" -> "按进程名称精确匹配；Android 上可填写应用包名，例如 com.example.app。"
    "PROCESS-NAME-WILDCARD" -> "按进程名称通配符匹配；仅支持 * 和 ?，Android 上也可匹配包名。"
    "PROCESS-NAME-REGEX" -> "用正则表达式匹配进程名称；Android 上也可匹配应用包名。"
    "UID" -> "按 Linux 用户 ID 匹配，填写数字，例如 1001。"
    "NETWORK" -> "按连接协议匹配，填写 tcp 或 udp。"
    "DSCP" -> "按数据包 DSCP 标记匹配，仅适用于 TProxy 的 UDP 入站。"
    "RULE-SET" -> "引用规则集中的匹配项；先在“规则 → 规则集”添加规则集，再选择名称。"
    "SUB-RULE" -> "引用已创建的子规则集合；匹配结果使用子规则中对应的目标策略。"
    "AND" -> "所有子条件都匹配时才命中。适合组合域名、网络等多个条件。"
    "OR" -> "任意一个子条件匹配时即命中。适合多个可选匹配条件。"
    "NOT" -> "子条件不匹配时命中，用于排除某类请求。"
    "MATCH" -> "无条件匹配剩余请求；始终作为最后一条规则。"
    else -> "Mihomo 规则类型 $type。请按该类型要求填写匹配值。"
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
    var editing by remember(initialProviderType) { mutableStateOf<RuleProvider?>(when {
        initialProviderType == "http" || initialProviderType == "inline" -> RuleProvider(id = java.util.UUID.randomUUID().toString(), name = "新规则集", type = initialProviderType)
        initialProviderType?.startsWith("edit:") == true -> initial.providers.firstOrNull { it.id == initialProviderType.removePrefix("edit:") }
        else -> null
    }) }
    var editingSubRule by remember { mutableStateOf<Pair<String, Int?>?>(null) }
    var subName by remember { mutableStateOf("") }
    var providerToDelete by remember { mutableStateOf<RuleProvider?>(null) }
    var subRuleToDelete by remember { mutableStateOf<String?>(null) }
    val managementSection = if (initialProviderType == "subrules") 1 else 0
    val invalidProviderSetup = profile.providers.any { it.name.isBlank() || (it.type == "http" && !it.url.startsWith("http")) || (it.type == "inline" && it.payload.isEmpty()) } ||
        profile.providers.map { it.name.lowercase() }.toSet().size != profile.providers.size ||
        profile.subRules.any { it.name.isBlank() } || profile.subRules.map { it.name.lowercase() }.toSet().size != profile.subRules.size
    if (editing != null) {
        RuleProviderEditor(editing!!, onDismiss = { if (initialProviderType == "http" || initialProviderType == "inline" || initialProviderType?.startsWith("edit:") == true) onDismiss() else editing = null }, onSave = { item ->
            val old = providers.indexOfFirst { it.id == editing?.id }
            val updated = if (old < 0) providers + item else providers.toMutableList().also { it[old] = item }
            if (initialProviderType == "http" || initialProviderType == "inline" || initialProviderType?.startsWith("edit:") == true) onSave(profile.copy(providers = updated))
            else { profile = profile.copy(providers = updated); editing = null }
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
    } else FullScreenEditorDialog(title = if (managementSection == 0) "规则集管理" else "子规则管理",
        dirty = profile != initial, valid = !invalidProviderSetup, onDismiss = onDismiss, onSave = { onSave(profile) },
        content = { Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Column(Modifier.weight(1f).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (managementSection == 0) {
                    providers.forEach { provider -> Card(onClick = { editing = provider }, modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) { Row(Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) { Column(Modifier.weight(1f)) { Text(provider.name); Text("${provider.type} · ${provider.behavior} · ${provider.format}", style = MaterialTheme.typography.labelSmall) }; IconButton(onClick = { providerToDelete = provider }) { Icon(Icons.Outlined.Delete, "删除规则集提供者") } } } }
                    OutlinedButton(onClick = { editing = RuleProvider(id = java.util.UUID.randomUUID().toString(), name = "新规则集", type = "http") }, modifier = Modifier.fillMaxWidth()) { Text("添加规则集") }
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
        } })
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
    val parsedHeaders = headers.lines().mapNotNull { line -> line.split("=", limit = 2).takeIf { it.size == 2 }?.let { it[0].trim() to it[1].split(",").map(String::trim) } }.toMap()
    val draft = initial.copy(name = name.trim(), type = type, behavior = behavior, url = url, path = path, format = format, interval = interval.toIntOrNull() ?: 86400, payload = payload.lines().filter(String::isNotBlank), headers = parsedHeaders)
    FullScreenEditorDialog(title = "规则集", dirty = draft != initial, valid = name.isNotBlank() && (type != "http" || url.startsWith("http")) && (type != "inline" || payload.isNotBlank()),
        onDismiss = onDismiss, onSave = { onSave(draft) }, content = { Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("规则集保存后，在规则列表添加 RULE-SET 规则才能参与分流。远程规则集需要刷新，才能在本机预览和模拟。", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        TextButton(onClick = { section = if (section == 0) 1 else 0 }) { Text(if (section == 0) "显示高级参数" else "收起高级参数") }
            OutlinedTextField(name, { name = it }, modifier = Modifier.fillMaxWidth(), label = { Text("名称") }, singleLine = true)
            SelectField("来源类型", type, listOf("http", "file", "inline")) { type = it }
            SelectField("匹配行为", behavior, listOf("domain", "ipcidr", "classical")) { behavior = it }
            Text("domain 匹配域名，ipcidr 匹配 IP 网段，classical 使用 DOMAIN-SUFFIX,example.com 等完整规则项。", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (type == "http") OutlinedTextField(url, { url = it }, modifier = Modifier.fillMaxWidth(), label = { Text("下载 URL") }, singleLine = true)
            if (type == "file") OutlinedTextField(path, { path = it }, modifier = Modifier.fillMaxWidth(), label = { Text("本地文件路径（应用规则集目录内）") }, singleLine = true)
            if (type == "inline") OutlinedTextField(payload, { payload = it }, modifier = Modifier.fillMaxWidth(), label = { Text("规则项，每行一条") }, minLines = 4)
            Text(when (type) {
                "http" -> "填写可直接下载的 HTTP(S) 地址；保存后到规则集页刷新。"
                "file" -> "填写应用规则集目录中的相对路径；本机文件需先放入该目录。"
                else -> "每行填写一个匹配项；domain 可用 example.com 或 +.example.com，ipcidr 填写 IP 网段。"
            }, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        if (section == 1) {
            SelectField("格式", format, listOf("yaml", "text", "mrs")) { format = it }
            Text("YAML 读取 payload 列表，text 按行读取；MRS 为二进制格式，不能用于本机规则模拟。", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (type == "http") OutlinedTextField(interval, { interval = it.filter(Char::isDigit) }, modifier = Modifier.fillMaxWidth(), label = { Text("更新间隔（秒）") }, singleLine = true)
            if (type == "http") OutlinedTextField(path, { path = it }, modifier = Modifier.fillMaxWidth(), label = { Text("本地缓存路径") }, singleLine = true)
            if (type == "http") OutlinedTextField(headers, { headers = it }, modifier = Modifier.fillMaxWidth(), label = { Text("请求头，每行 key=value") }, minLines = 2)
            Text("这些参数会保存在当前规则集配置中；刷新缓存和状态仅保存在本机。", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    } })
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
    var extra by remember(node.id) { mutableStateOf(MihomoNodeOptionsYaml.dump(node.options)) }
    val parsedOptions = remember(extra) { runCatching { MihomoNodeOptionsYaml.parse(extra) } }
    JichangAlertDialog(onDismissRequest = onDismiss, title = { Text("编辑节点") }, text = { Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(7.dp)) {
        OutlinedTextField(name, { name = it }, modifier = Modifier.fillMaxWidth(), label = { Text("名称") }, singleLine = true)
        OutlinedTextField(type, { type = it }, modifier = Modifier.fillMaxWidth(), label = { Text("Mihomo 协议类型") }, supportingText = { Text("例如 ss、ssr、vmess、vless、trojan、hysteria2、tuic、anytls") }, singleLine = true)
        OutlinedTextField(server, { server = it }, modifier = Modifier.fillMaxWidth(), label = { Text("服务器") }, singleLine = true)
        OutlinedTextField(port, { port = it.filter(Char::isDigit) }, modifier = Modifier.fillMaxWidth(), label = { Text("端口") }, singleLine = true)
        OutlinedTextField(extra, { extra = it }, modifier = Modifier.fillMaxWidth(), label = { Text("协议参数 YAML") }, supportingText = { Text("保留布尔值、列表和 TLS/传输层嵌套结构。") }, textStyle = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace), minLines = 6)
        parsedOptions.exceptionOrNull()?.let { Text(it.message ?: "协议参数 YAML 无效", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
    } }, confirmButton = { TextButton(enabled = name.isNotBlank() && type.isNotBlank() && server.isNotBlank() && port.toIntOrNull() in 1..65535 && parsedOptions.isSuccess, onClick = { parsedOptions.getOrNull()?.let { onSave(name, type, server, port.toInt(), it) } }) { Text("保存") } }, dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } })
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

private fun safeConfigFileName(value: String, format: ExportFormat): String {
    val clean = value.trim().replace("[\\\\/:*?\"<>|\\p{Cntrl}]".toRegex(), "_").trim('.', ' ')
        .removeSuffix(".yaml").removeSuffix(".yml").removeSuffix(".json").ifBlank { "鸡场" }
    return "$clean.${if (format == ExportFormat.Mihomo) "yaml" else "json"}"
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
            put(MediaStore.Downloads.MIME_TYPE, if (filename.endsWith(".json", true)) "application/json" else "application/yaml")
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
