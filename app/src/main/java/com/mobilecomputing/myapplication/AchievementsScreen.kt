package com.mobilecomputing.myapplication

import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mobilecomputing.myapplication.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AchievementsScreen(
    viewModel: ExpenseViewModel,
    onBack: () -> Unit
) {
    val achievements by viewModel.achievements.collectAsState()
    val unlocked = achievements.count { it.isUnlocked }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Achievements 🏆", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(padding)
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Progress header
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(
                    modifier = Modifier
                        .padding(24.dp)
                        .fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("🏆", fontSize = 48.sp)
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "$unlocked / ${achievements.size} Unlocked",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 22.sp,
                        color = AccentYellow
                    )
                    Spacer(Modifier.height(12.dp))
                    LinearProgressIndicator(
                        progress = { unlocked.toFloat() / achievements.size.coerceAtLeast(1) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(10.dp)
                            .clip(RoundedCornerShape(5.dp)),
                        color = AccentYellow,
                        trackColor = MaterialTheme.colorScheme.background
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        if (unlocked == achievements.size) "You're a Finance Master! 🎉"
                        else "${achievements.size - unlocked} more to unlock!",
                        color = TextSecondary,
                        fontSize = 13.sp
                    )
                }
            }

            // Achievements Grid — use for loop to avoid type inference issues
            val rows = achievements.toList().chunked(2)
            for (row in rows) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    for (achievement in row) {
                        AchievementCard(
                            achievement = achievement,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    if (row.size == 1) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@Composable
fun AchievementCard(achievement: Achievement, modifier: Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "badge")
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (achievement.isUnlocked) 1.05f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "badge_scale"
    )

    Card(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (achievement.isUnlocked)
                AccentYellow.copy(alpha = 0.1f)
            else
                MaterialTheme.colorScheme.surfaceVariant
        ),
        border = if (achievement.isUnlocked)
            BorderStroke(1.5.dp, AccentYellow.copy(alpha = 0.5f))
        else
            null
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(60.dp)
                    .scale(if (achievement.isUnlocked) scale else 1f)
                    .clip(CircleShape)
                    .background(
                        if (achievement.isUnlocked)
                            AccentYellow.copy(alpha = 0.2f)
                        else
                            Color.Gray.copy(alpha = 0.1f)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (achievement.isUnlocked) achievement.emoji else "🔒",
                    fontSize = 28.sp
                )
            }
            Text(
                text = achievement.title,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
                textAlign = TextAlign.Center,
                color = if (achievement.isUnlocked) AccentYellow else TextSecondary
            )
            Text(
                text = achievement.description,
                fontSize = 10.sp,
                textAlign = TextAlign.Center,
                color = TextSecondary,
                lineHeight = 13.sp
            )
            if (achievement.isUnlocked) {
                Text(
                    "✅ Unlocked!",
                    fontSize = 10.sp,
                    color = PrimaryGreen,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}
