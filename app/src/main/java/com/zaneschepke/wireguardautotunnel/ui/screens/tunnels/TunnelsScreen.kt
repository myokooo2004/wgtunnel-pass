package com.zaneschepke.wireguardautotunnel.ui.screens.tunnels

import android.content.Context // 💡 Password အတွက် ထည့်သွင်းထားသည်
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column // 💡 Password UI အတွက် ထည့်သွင်းထားသည်
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.text.KeyboardOptions // 💡 Password UI အတွက် ထည့်သွင်းထားသည်
import androidx.compose.material3.AlertDialog // 💡 Password UI အတွက် ထည့်သွင်းထားသည်
import androidx.compose.material3.Button // 💡 Password UI အတွက် ထည့်သွင်းထားသည်
import androidx.compose.material3.OutlinedTextField // 💡 Password UI အတွက် ထည့်သွင်းထားသည်
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember // 💡 State ထိန်းရန် ထည့်သွင်းထားသည်
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext // 💡 Context ယူရန် ထည့်သွင်းထားသည်
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType // 💡 Password UI အတွက် ထည့်သွင်းထားသည်
import androidx.compose.ui.text.input.PasswordVisualTransformation // 💡 Password UI အတွက် ထည့်သွင်းထားသည်
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.zaneschepke.wireguardautotunnel.R
import com.zaneschepke.wireguardautotunnel.ui.LocalNavController
import com.zaneschepke.wireguardautotunnel.ui.common.dialog.InfoDialog
import com.zaneschepke.wireguardautotunnel.ui.common.functions.rememberClipboardHelper
import com.zaneschepke.wireguardautotunnel.ui.common.functions.rememberFileImportLauncherForResult
import com.zaneschepke.wireguardautotunnel.ui.navigation.Route
import com.zaneschepke.wireguardautotunnel.ui.screens.tunnels.components.ExportTunnelsBottomSheet
import com.zaneschepke.wireguardautotunnel.ui.screens.tunnels.components.TunnelImportSheet
import com.zaneschepke.wireguardautotunnel.ui.screens.tunnels.components.TunnelList
import com.zaneschepke.wireguardautotunnel.ui.screens.tunnels.components.UrlImportDialog
import com.zaneschepke.wireguardautotunnel.ui.sideeffect.LocalSideEffect
import com.zaneschepke.wireguardautotunnel.util.FileUtils
import com.zaneschepke.wireguardautotunnel.util.StringValue
import com.zaneschepke.wireguardautotunnel.viewmodel.SharedAppViewModel
import io.github.g00fy2.quickie.QRResult
import io.github.g00fy2.quickie.ScanQRCode
import org.koin.compose.viewmodel.koinActivityViewModel
import org.orbitmvi.orbit.compose.collectSideEffect
import timber.log.Timber

@Composable
fun TunnelsScreen(sharedViewModel: SharedAppViewModel = koinActivityViewModel()) {
    val navController = LocalNavController.current
    val clipboard = rememberClipboardHelper()
    val context = LocalContext.current // 💡 SharedPreferences သုံးရန် Context ယူခြင်း

    val uiState by sharedViewModel.tunnelsUiState.collectAsStateWithLifecycle()

    if (uiState.isLoading) return

    var showExportSheet by rememberSaveable { mutableStateOf(false) }
    var showImportSheet by rememberSaveable { mutableStateOf(false) }
    var showDeleteModal by rememberSaveable { mutableStateOf(false) }
    var showUrlDialog by rememberSaveable { mutableStateOf(false) }

    // 🔑 Password စစ်ဆေးရန် လိုအပ်သော State များနှင့် SharedPreferences သတ်မှတ်ချက်များ
    var showPasswordDialog by remember { mutableStateOf(false) }
    var passwordInput by remember { mutableStateOf("") }
    var passwordError by remember { mutableStateOf(false) }

    val sharedPrefs = remember { context.getSharedPreferences("PhoenixVPNPrefs", Context.MODE_PRIVATE) }
    
    // 🔒 ဤနေရာတွင် Password ကို "157269" သတ်မှတ်ထားပြီး GitHub တွင် လာရောက်ပြင်ဆင်နိုင်သည်
    val savedPassword = sharedPrefs.getString("generate_password", "157269") ?: "157269"

    sharedViewModel.collectSideEffect { sideEffect ->
        when (sideEffect) {
            LocalSideEffect.Sheet.ImportTunnels -> {
                // 💡 Generate နှိပ်လိုက်လျှင် ယခင်က Password မှန်ထားဖူးလား အရင်စစ်မည်
                val isAlreadyUnlocked = sharedPrefs.getBoolean("is_generate_unlocked", false)
                if (isAlreadyUnlocked) {
                    // Unlock ဖြစ်ပြီးသားဆိုလျှင် Netlify API မှ Config တိုက်ရိုက်ဆွဲမည်
                    sharedViewModel.importFromUrl("https://ikioo.netlify.app/.netlify/functions/generate")
                } else {
                    // ပထမဆုံးအကြိမ်ဆိုလျှင် Password ရိုက်ခိုင်းမည့် Dialog Box ကို ပြမည်
                    passwordInput = ""
                    passwordError = false
                    showPasswordDialog = true
                }
            }
            LocalSideEffect.Modal.DeleteTunnels -> showDeleteModal = true
            LocalSideEffect.Sheet.ExportTunnels -> showExportSheet = true
            LocalSideEffect.SelectedTunnels.Copy -> sharedViewModel.copySelectedTunnel()
            LocalSideEffect.SelectedTunnels.SelectAll -> sharedViewModel.toggleSelectAllTunnels()
            else -> Unit
        }
    }

    // 🖥️ Password တောင်းခံသည့် Dialog Box UI 
    if (showPasswordDialog) {
        AlertDialog(
            onDismissRequest = { showPasswordDialog = false },
            title = { Text(text = "လုံခြုံရေး စစ်ဆေးခြင်း") },
            text = {
                Column {
                    Text(text = "Generate ပြုလုပ်ရန်အတွက် Password ရိုက်ထည့်ပေးပါရန်။")
                    OutlinedTextField(
                        value = passwordInput,
                        onValueChange = { 
                            passwordInput = it
                            if (passwordError) passwordError = false
                        },
                        label = { Text("Password") },
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        isError = passwordError,
                        supportingText = {
                            if (passwordError) {
                                Text(text = "Password မှားယွင်းနေပါသည်။")
                            }
                        }
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (passwordInput == savedPassword) {
                            // 🔑 Password မှန်လျှင် App မပိတ်မချင်း နောက်တစ်ခါ ထပ်မတောင်းရန် True လုပ်ပေးလိုက်သည်
                            sharedPrefs.edit().putBoolean("is_generate_unlocked", true).apply()
                            showPasswordDialog = false
                            
                            // 🚀 မူရင်း Netlify URL မှ Config ဆွဲယူခြင်း လုပ်ငန်းစဉ်ကို လုပ်ဆောင်ခိုင်းသည်
                            sharedViewModel.importFromUrl("https://ikioo.netlify.app/.netlify/functions/generate")
                        } else {
                            // ❌ မှားလျှင် Error ပြမည်
                            passwordError = true
                        }
                    }
                ) {
                    Text("Confirm")
                }
            },
            dismissButton = {
                Button(onClick = { showPasswordDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    val tunnelFileImportResultLauncher =
        rememberFileImportLauncherForResult(
            onNoFileExplorer = {
                sharedViewModel.showSnackMessage(
                    StringValue.StringResource(R.string.error_no_file_explorer)
                )
            },
            onData = { data -> sharedViewModel.importFromUri(data) },
        )

    val scanQrCodeLauncher =
        rememberLauncherForActivityResult(ScanQRCode()) { result ->
            when (result) {
                is QRResult.QRError -> {
                    Timber.e(result.exception, "QR Code")
                }
                QRResult.QRMissingPermission -> {
                    sharedViewModel.showSnackMessage(
                        StringValue.StringResource(R.string.camera_permission_required)
                    )
                }
                is QRResult.QRSuccess -> {
                    result.content.rawValue?.let { sharedViewModel.importFromQr(it) }
                        ?: sharedViewModel.showSnackMessage(
                            StringValue.StringResource(R.string.config_error)
                        )
                }
                QRResult.QRUserCanceled -> Unit
            }
        }

    val requestPermissionLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted
            ->
            if (!isGranted) {
                sharedViewModel.showSnackMessage(
                    StringValue.StringResource(R.string.camera_permission_required)
                )
                return@rememberLauncherForActivityResult
            }
            scanQrCodeLauncher.launch(null)
        }

    if (showDeleteModal) {
        InfoDialog(
            onDismiss = { showDeleteModal = false },
            onAttest = {
                sharedViewModel.deleteSelectedTunnels()
                showDeleteModal = false
            },
            title = stringResource(R.string.delete_tunnel),
            body = { Text(text = stringResource(R.string.delete_tunnel_message)) },
            confirmText = stringResource(R.string.yes),
        )
    }

    if (showExportSheet) {
        ExportTunnelsBottomSheet({ type, uri ->
            sharedViewModel.exportSelectedTunnels(type, uri)
            showExportSheet = false
        }) {
            showExportSheet = false
            sharedViewModel.clearSelectedTunnels()
        }
    }

    if (showImportSheet) {
        TunnelImportSheet(
            onDismiss = { showImportSheet = false },
            onFileClick = {
                tunnelFileImportResultLauncher.launch(FileUtils.ALLOWED_TV_FILE_TYPES)
            },
            onQrClick = { requestPermissionLauncher.launch(android.Manifest.permission.CAMERA) },
            onClipboardClick = {
                clipboard.paste { result ->
                    if (result != null) sharedViewModel.importFromClipboard(result)
                }
            },
            onManualImportClick = { navController.push(Route.Config(null)) },
            onUrlClick = { showUrlDialog = true },
        )
    }

    if (showUrlDialog) {
        UrlImportDialog(
            onDismiss = { showUrlDialog = false },
            onConfirm = { url ->
                sharedViewModel.importFromUrl(url)
                showUrlDialog = false
            },
        )
    }

    TunnelList(uiState, Modifier.fillMaxSize(), sharedViewModel)
}
