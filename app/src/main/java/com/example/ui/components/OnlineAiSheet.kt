package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.TipsAndUpdates
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AiAnalysisReport
import com.example.data.model.PhotoAdjustments
import com.example.ui.theme.IPhoneBlack
import com.example.ui.theme.IPhoneCardDark
import com.example.ui.theme.IPhoneCardElevated
import com.example.ui.theme.IPhoneControlFill
import com.example.ui.theme.IPhoneCyan
import com.example.ui.theme.IPhoneGreen
import com.example.ui.theme.IPhonePurple
import com.example.ui.theme.IPhoneTextSecondary
import com.example.ui.theme.IPhoneYellow

@Composable
fun OnlineAiStudio(
    isAnalyzing: Boolean,
    report: AiAnalysisReport?,
    hasOnlineKey: Boolean,
    onAnalyze: () -> Unit,
    onApplyReport: (AiAnalysisReport) -> Unit,
    onApplyPrompt: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var customPrompt by remember { mutableStateOf("") }
    val quickPrompts = listOf(
        "✨ Golden Hour Glow",
        "🎬 Shot on iPhone 16 Pro Cinematic",
        "🧖 Natural Face Beauty & Soft Skin",
        "🌆 Cyberpunk Neon Night",
        "🖤 Deep Mood Monochrome",
        "🌿 Lush Vibrant Nature"
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(IPhoneBlack)
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        // Status Badge Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .background(if (hasOnlineKey) IPhoneGreen else IPhoneCyan, CircleShape)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = if (hasOnlineKey) "ONLINE GEMINI AI READY" else "SMART IPHONE AI ENGINE (OFFLINE)",
                    color = if (hasOnlineKey) IPhoneGreen else IPhoneCyan,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // AI Smart Analysis Card
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            color = IPhoneCardDark,
            border = androidx.compose.foundation.BorderStroke(1.dp, IPhoneControlFill)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "AI Photo Intelligence",
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Analyze lighting, dynamic range & skin balance",
                            color = IPhoneTextSecondary,
                            fontSize = 11.sp
                        )
                    }

                    Button(
                        onClick = onAnalyze,
                        enabled = !isAnalyzing,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = IPhoneYellow,
                            contentColor = IPhoneBlack
                        ),
                        shape = RoundedCornerShape(20.dp),
                        modifier = Modifier.testTag("ai_analyze_button")
                    ) {
                        if (isAnalyzing) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                color = IPhoneBlack,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(Icons.Default.AutoAwesome, null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("ANALYZE", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                if (report != null) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(IPhoneCardElevated, RoundedCornerShape(10.dp))
                            .padding(12.dp)
                    ) {
                        Column {
                            Text(
                                text = "“${report.summary}”",
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceAround
                            ) {
                                ScoreChip(label = "Lighting", score = report.lightingScore)
                                ScoreChip(label = "Dynamic Range", score = report.dynamicRangeScore)
                                ScoreChip(label = "Tone", textVal = report.colorTone)
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            report.recommendations.take(2).forEach { rec ->
                                Row(
                                    modifier = Modifier.padding(vertical = 2.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        tint = IPhoneCyan,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(text = rec, color = Color.White.copy(alpha = 0.85f), fontSize = 11.sp)
                                }
                            }
                            Spacer(modifier = Modifier.height(10.dp))
                            Button(
                                onClick = { onApplyReport(report) },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = IPhoneCyan,
                                    contentColor = IPhoneBlack
                                ),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth().testTag("apply_report_btn")
                            ) {
                                Text(
                                    text = "APPLY SMART IPHONE GRADING",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // AI Prompt Retouch Section
        Text(
            text = "AI PROMPT RETOUCH",
            color = IPhoneTextSecondary,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Quick Preset Prompts
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(vertical = 4.dp)
        ) {
            items(quickPrompts) { prompt ->
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = IPhoneCardDark,
                    border = androidx.compose.foundation.BorderStroke(1.dp, IPhoneControlFill),
                    modifier = Modifier.clickable { onApplyPrompt(prompt) }
                ) {
                    Text(
                        text = prompt,
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Custom Prompt Input Field
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = customPrompt,
                onValueChange = { customPrompt = it },
                placeholder = { Text("Describe look (e.g. moody dusk, warm film)...", fontSize = 11.sp, color = Color.Gray) },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = IPhoneYellow,
                    unfocusedBorderColor = IPhoneControlFill,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    cursorColor = IPhoneYellow
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .weight(1f)
                    .testTag("ai_prompt_input")
            )
            Spacer(modifier = Modifier.width(8.dp))
            IconButton(
                onClick = {
                    if (customPrompt.isNotBlank()) {
                        onApplyPrompt(customPrompt)
                        customPrompt = ""
                    }
                },
                modifier = Modifier
                    .size(48.dp)
                    .background(IPhoneYellow, RoundedCornerShape(12.dp))
                    .testTag("ai_send_prompt_btn")
            ) {
                Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Apply prompt", tint = IPhoneBlack)
            }
        }
    }
}

@Composable
private fun ScoreChip(label: String, score: Int? = null, textVal: String? = null) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = label, color = IPhoneTextSecondary, fontSize = 9.sp)
        Text(
            text = score?.let { "$it/100" } ?: (textVal ?: ""),
            color = IPhoneYellow,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold
        )
    }
}
