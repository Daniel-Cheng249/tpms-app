package com.tpms.monitor.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tpms.monitor.data.TirePosition
import com.tpms.monitor.data.UiState
import com.tpms.monitor.ui.theme.*
import com.tpms.monitor.ui.viewmodel.TirePressureViewModel

/**
 * 主界面 - 仪表盘
 */
@Composable
fun DashboardScreen(
    viewModel: TirePressureViewModel,
    onNavigateToMapping: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val tirePressures by viewModel.tirePressures.collectAsState()
    val isBluetoothEnabled by viewModel.isBluetoothEnabled.collectAsState()
    val tireMapping by viewModel.tireMapping.collectAsState()

    val isScanning = uiState is UiState.Scanning
    val isConnected = uiState is UiState.Connected || uiState is UiState.PartiallyConnected

    // 计算已绑定的传感器数量
    val boundCount = tireMapping.mapping.size

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        // 标题区域
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "🚗",
                    fontSize = 24.sp
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "TPMS UAES",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 状态指示器
            StatusIndicators(
                isScanning = isScanning,
                isConnected = isConnected
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // 蓝牙权限检查
        if (!isBluetoothEnabled) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
            ) {
                Text(
                    text = "请授予蓝牙权限以使用此功能",
                    modifier = Modifier.padding(16.dp),
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
            }
        }

        // 绑定状态提示
        if (boundCount < 4) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                ),
                onClick = onNavigateToMapping
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
                            text = "传感器绑定",
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                        Text(
                            text = "已绑定 $boundCount/4 个传感器，点击完成绑定",
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "设置",
                        tint = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            }
        }

        // 轮胎压力显示区 - 2x2 网格
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            // 上排（前排轮胎）
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                tirePressures[TirePosition.FRONT_LEFT.tireName]?.let { data ->
                    TirePressureCard(
                        data = data,
                        modifier = Modifier.weight(1f)
                    )
                }

                tirePressures[TirePosition.FRONT_RIGHT.tireName]?.let { data ->
                    TirePressureCard(
                        data = data,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // 下排（后排轮胎）
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                tirePressures[TirePosition.REAR_LEFT.tireName]?.let { data ->
                    TirePressureCard(
                        data = data,
                        modifier = Modifier.weight(1f)
                    )
                }

                tirePressures[TirePosition.REAR_RIGHT.tireName]?.let { data ->
                    TirePressureCard(
                        data = data,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // 操作按钮
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Button(
                onClick = { viewModel.startScan() },
                enabled = isBluetoothEnabled && !isScanning,
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp),
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text("开始扫描", fontSize = 15.sp)
            }

            // 绑定设置按钮
            OutlinedButton(
                onClick = onNavigateToMapping,
                modifier = Modifier
                    .height(48.dp),
                shape = RoundedCornerShape(10.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = "绑定设置"
                )
            }

            if (isConnected) {
                Button(
                    onClick = { viewModel.disconnect() },
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE57373)),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("断开", fontSize = 15.sp)
                }
            } else if (boundCount > 0) {
                // 有绑定设备但未连接时显示连接按钮
                Button(
                    onClick = { viewModel.connectAllBoundDevices() },
                    enabled = isBluetoothEnabled && !isScanning,
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF66BB6A)),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("连接", fontSize = 15.sp)
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun DashboardScreenPreview() {
    TPMSMonitorTheme {
        // Preview 实现暂略 - 需要 Application 上下文
    }
}
