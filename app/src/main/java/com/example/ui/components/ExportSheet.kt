package com.example.ui.components

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.HighQuality
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.UpscaleTarget
import com.example.ui.theme.IPhoneBlack
import com.example.ui.theme.IPhoneCardDark
import com.example.ui.theme.IPhoneCardElevated
import com.example.ui.theme.IPhoneControlFill
import com.example.ui.theme.IPhoneGreen
import com.example.ui.theme.IPhoneTextSecondary
import com.example.ui.theme.IPhoneYellow

@Composable
fun ExportDialog(
    isExporting: Boolean,
    savedUri: Uri?,
    onDismiss: () -> Unit,
    onSaveToGallery: (UpscaleTarget, Int) -> Unit,
    onShare: () -> Unit
) {
    var selectedTarget by remember { mutableStateOf(UpscaleTarget.UHD_4K) }
    var qualityPercent by remember { mutableIntStateOf(95) }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        shape = RoundedCornerShape(20.dp),
        color = IPhoneCardDark,
        border = androidx.compose.foundation.BorderStroke(1.dp, IPhoneControlFill)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "EXPORT PHOTO",
                        color = Color.White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = "Save high-fidelity iPhone grade output",
                        color = IPhoneTextSecondary,
                        fontSize = 11.sp
                    )
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.Gray)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Resolution Targets
            Text(
                text = "OUTPUT RESOLUTION",
                color = IPhoneTextSecondary,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                UpscaleTarget.values().forEach { target ->
                    val isSelected = target == selectedTarget
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .clickable { selectedTarget = target }
                            .testTag("export_res_${target.name.lowercase()}"),
                        shape = RoundedCornerShape(12.dp),
                        color = if (isSelected) IPhoneControlFill else IPhoneCardElevated,
                        border = androidx.compose.foundation.BorderStroke(
                            width = if (isSelected) 1.5.dp else 0.5.dp,
                            color = if (isSelected) IPhoneYellow else Color.Transparent
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(10.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = target.label,
                                color = if (isSelected) IPhoneYellow else Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = target.resolutionDesc,
                                color = IPhoneTextSecondary,
                                fontSize = 8.sp,
                                maxLines = 1
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // JPEG Quality
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(text = "JPEG Quality", color = IPhoneTextSecondary, fontSize = 11.sp)
                Text(text = "$qualityPercent%", color = IPhoneYellow, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
            Slider(
                value = qualityPercent.toFloat(),
                onValueChange = { qualityPercent = it.toInt() },
                valueRange = 70f..100f,
                colors = SliderDefaults.colors(
                    thumbColor = IPhoneYellow,
                    activeTrackColor = IPhoneYellow,
                    inactiveTrackColor = IPhoneCardElevated
                )
            )

            Spacer(modifier = Modifier.height(12.dp))

            if (savedUri != null) {
                // Success banner
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = IPhoneGreen.copy(alpha = 0.15f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, IPhoneGreen),
                    modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null, tint = IPhoneGreen)
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "Saved to Device Gallery!",
                                color = IPhoneGreen,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Saved via MediaStore in Pictures/iPhoneAIPhotoEditor (${selectedTarget.label})",
                                color = Color.White.copy(alpha = 0.8f),
                                fontSize = 10.sp
                            )
                        }
                    }
                }
            }

            // Save & Share Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = { onSaveToGallery(selectedTarget, qualityPercent) },
                    enabled = !isExporting,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = IPhoneYellow,
                        contentColor = IPhoneBlack
                    ),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("save_to_gallery_btn")
                ) {
                    if (isExporting) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            color = IPhoneBlack,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("SAVE TO GALLERY", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }

                if (savedUri != null) {
                    Button(
                        onClick = onShare,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = IPhoneControlFill,
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .height(48.dp)
                            .testTag("share_btn")
                    ) {
                        Icon(Icons.Default.Share, contentDescription = "Share", modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("SHARE", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
