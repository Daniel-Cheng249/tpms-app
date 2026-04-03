package com.tpms.monitor.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tpms.monitor.ble.BleDevice
import com.tpms.monitor.data.TireDeviceMapping
import com.tpms.monitor.data.TirePosition
import com.tpms.monitor.ui.theme.PrimaryBlue
import com.tpms.monitor.ui.theme.TPMSMonitorTheme

/**
 * 设备绑定界面
 * 用于将扫描到的 BLE 设备与轮胎位置进行绑定
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeviceMappingScreen(
    scannedDevices: List<BleDevice>,
    tireMapping: TireDeviceMapping,
    isScanning: Boolean,
    onStartScan: () -> Unit,
    onStopScan: () -> Unit,
    onBindDevice: (TirePosition, BleDevice) -> Unit,
    onUnbindDevice: (TirePosition) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedPosition by remember { mutableStateOf<TirePosition?>(null) }
    var showDeviceDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("传感器绑定") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "返回")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        },
        modifier = modifier
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            // 说明文字
            Text(
                text = "点击轮胎位置，选择对应的传感器设备",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            // 轮胎位置网格
            TirePositionGrid(
                tireMapping = tireMapping,
                onPositionClick = { position ->
                    selectedPosition = position
                    showDeviceDialog = true
                    if (!isScanning && scannedDevices.isEmpty()) {
                        onStartScan()
                    }
                },
                onUnbindClick = onUnbindDevice,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(24.dp))

            // 扫描控制区域
            ScanControlSection(
                isScanning = isScanning,
                deviceCount = scannedDevices.size,
                onStartScan = onStartScan,
                onStopScan = onStopScan
            )

            Spacer(modifier = Modifier.height(16.dp))

            // 已扫描设备列表预览
            Text(
                text = "附近设备",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            if (scannedDevices.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(100.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (isScanning) "正在扫描设备..." else "点击上方按钮开始扫描",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 14.sp
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(scannedDevices) { device ->
                        val isBound = tireMapping.getTirePosition(device.address) != null
                        ScannedDeviceItem(
                            device = device,
                            isBound = isBound,
                            boundPosition = tireMapping.getTirePosition(device.address)?.displayName,
                            onClick = {
                                if (!isBound) {
                                    selectedPosition?.let { position ->
                                        onBindDevice(position, device)
                                    }
                                }
                            }
                        )
                    }
                }
            }
        }
    }

    // 设备选择对话框
    if (showDeviceDialog && selectedPosition != null) {
        DeviceSelectionDialog(
            position = selectedPosition!!,
            devices = scannedDevices.filter {
                tireMapping.getTirePosition(it.address) == null
            },
            isScanning = isScanning,
            onDismiss = { showDeviceDialog = false },
            onDeviceSelected = { device ->
                onBindDevice(selectedPosition!!, device)
                showDeviceDialog = false
            },
            onStartScan = onStartScan
        )
    }
}

/**
 * 轮胎位置网格 (2x2 车辆布局)
 */
@Composable
private fun TirePositionGrid(
    tireMapping: TireDeviceMapping,
    onPositionClick: (TirePosition) -> Unit,
    onUnbindClick: (TirePosition) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // 前排
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            TirePositionCard(
                position = TirePosition.FRONT_LEFT,
                deviceAddress = tireMapping.getDeviceAddress(TirePosition.FRONT_LEFT),
                onClick = { onPositionClick(TirePosition.FRONT_LEFT) },
                onUnbind = { onUnbindClick(TirePosition.FRONT_LEFT) },
                modifier = Modifier.weight(1f)
            )
            TirePositionCard(
                position = TirePosition.FRONT_RIGHT,
                deviceAddress = tireMapping.getDeviceAddress(TirePosition.FRONT_RIGHT),
                onClick = { onPositionClick(TirePosition.FRONT_RIGHT) },
                onUnbind = { onUnbindClick(TirePosition.FRONT_RIGHT) },
                modifier = Modifier.weight(1f)
            )
        }

        // 车辆图标示意
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "🚗 车辆俯视图",
                fontSize = 24.sp
            )
        }

        // 后排
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            TirePositionCard(
                position = TirePosition.REAR_LEFT,
                deviceAddress = tireMapping.getDeviceAddress(TirePosition.REAR_LEFT),
                onClick = { onPositionClick(TirePosition.REAR_LEFT) },
                onUnbind = { onUnbindClick(TirePosition.REAR_LEFT) },
                modifier = Modifier.weight(1f)
            )
            TirePositionCard(
                position = TirePosition.REAR_RIGHT,
                deviceAddress = tireMapping.getDeviceAddress(TirePosition.REAR_RIGHT),
                onClick = { onPositionClick(TirePosition.REAR_RIGHT) },
                onUnbind = { onUnbindClick(TirePosition.REAR_RIGHT) },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

/**
 * 单个轮胎位置卡片
 */
@Composable
private fun TirePositionCard(
    position: TirePosition,
    deviceAddress: String?,
    onClick: () -> Unit,
    onUnbind: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isBound = deviceAddress != null

    Card(
        modifier = modifier
            .height(120.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(
            width = 2.dp,
            color = if (isBound) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
        ),
        colors = CardDefaults.cardColors(
            containerColor = if (isBound) {
                MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
            } else {
                MaterialTheme.colorScheme.surface
            }
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // 位置名称
            Text(
                text = position.displayName,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Text(
                text = "(${position.tireName})",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(8.dp))

            if (isBound) {
                // 已绑定状态
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )

                Text(
                    text = deviceAddress!!.takeLast(5),
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                // 解绑按钮
                IconButton(
                    onClick = onUnbind,
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "解绑",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(16.dp)
                    )
                }
            } else {
                // 未绑定状态
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.outline,
                    modifier = Modifier.size(24.dp)
                )

                Text(
                    text = "点击绑定",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.outline
                )
            }
        }
    }
}

/**
 * 扫描控制区域
 */
@Composable
private fun ScanControlSection(
    isScanning: Boolean,
    deviceCount: Int,
    onStartScan: () -> Unit,
    onStopScan: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = "蓝牙扫描",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "发现 $deviceCount 个设备",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Button(
                onClick = if (isScanning) onStopScan else onStartScan,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isScanning) {
                        MaterialTheme.colorScheme.error
                    } else {
                        PrimaryBlue
                    }
                )
            ) {
                Text(if (isScanning) "停止扫描" else "开始扫描")
            }
        }
    }
}

/**
 * 扫描到的设备列表项
 */
@Composable
private fun ScannedDeviceItem(
    device: BleDevice,
    isBound: Boolean,
    boundPosition: String?,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = !isBound, onClick = onClick),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isBound) {
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            } else {
                MaterialTheme.colorScheme.surface
            }
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 信号强度指示
            SignalIndicator(rssi = device.rssi)

            Spacer(modifier = Modifier.width(12.dp))

            // 设备信息
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = device.name ?: "未知设备",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = if (isBound) {
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    }
                )
                Text(
                    text = device.address,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // 绑定状态
            if (isBound) {
                Text(
                    text = "已绑定: $boundPosition",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Medium
                )
            } else {
                Text(
                    text = "${device.rssi} dBm",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/**
 * 信号强度指示器
 */
@Composable
private fun SignalIndicator(rssi: Int) {
    val (color, level) = when {
        rssi > -50 -> Color(0xFF4CAF50) to 3
        rssi > -70 -> Color(0xFFFF9800) to 2
        else -> Color(0xFFF44336) to 1
    }

    Row(
        verticalAlignment = Alignment.Bottom,
        horizontalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        repeat(3) { index ->
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height((8 + index * 6).dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(
                        if (index < level) color else Color.Gray.copy(alpha = 0.3f)
                    )
            )
        }
    }
}

/**
 * 设备选择对话框
 */
@Composable
private fun DeviceSelectionDialog(
    position: TirePosition,
    devices: List<BleDevice>,
    isScanning: Boolean,
    onDismiss: () -> Unit,
    onDeviceSelected: (BleDevice) -> Unit,
    onStartScan: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("选择 ${position.displayName} 传感器") },
        text = {
            Column {
                if (devices.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(120.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            if (isScanning) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(32.dp),
                                    strokeWidth = 2.dp
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text("正在扫描...")
                            } else {
                                Text("未发现可用设备")
                                Spacer(modifier = Modifier.height(8.dp))
                                TextButton(onClick = onStartScan) {
                                    Text("重新扫描")
                                }
                            }
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.height(200.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(devices) { device ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onDeviceSelected(device) },
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    SignalIndicator(rssi = device.rssi)
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = device.name ?: "未知设备",
                                            fontWeight = FontWeight.Medium
                                        )
                                        Text(
                                            text = device.address,
                                            fontSize = 12.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Text(
                                        text = "${device.rssi} dBm",
                                        fontSize = 12.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}

@Preview(showBackground = true)
@Composable
fun DeviceMappingScreenPreview() {
    TPMSMonitorTheme {
        // Preview 需要 Android 上下文，此处仅展示 UI 结构
        Text("DeviceMappingScreen Preview - Run on device to see full UI")
    }
}
