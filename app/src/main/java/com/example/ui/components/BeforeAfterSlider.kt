package com.example.ui.components

import android.graphics.Bitmap
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Compare
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.IPhoneBlack
import com.example.ui.theme.IPhoneControlFill
import com.example.ui.theme.IPhoneYellow
import kotlin.math.roundToInt

@Composable
fun BeforeAfterSlider(
    originalBitmap: Bitmap?,
    editedBitmap: Bitmap?,
    isSplitActive: Boolean,
    onToggleSplit: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (editedBitmap == null) {
        Box(
            modifier = modifier.fillMaxSize().background(IPhoneBlack),
            contentAlignment = Alignment.Center
        ) {
            Text("No photo loaded", color = Color.Gray, fontSize = 14.sp)
        }
        return
    }

    var splitPosition by remember { mutableFloatStateOf(0.5f) }
    var isHoldingOriginal by remember { mutableStateOf(false) }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(12.dp))
            .background(IPhoneBlack)
            .testTag("before_after_container"),
        contentAlignment = Alignment.Center
    ) {
        val totalWidth = constraints.maxWidth.toFloat()
        val centerY = constraints.maxHeight / 2
        val splitX = (totalWidth * splitPosition).coerceIn(0f, totalWidth)

        val editedImageBitmap = remember(editedBitmap) { editedBitmap.asImageBitmap() }
        val originalImageBitmap = remember(originalBitmap) { originalBitmap?.asImageBitmap() }

        // If holding to compare original, show original directly
        if (isHoldingOriginal && originalImageBitmap != null) {
            Image(
                bitmap = originalImageBitmap,
                contentDescription = "Original photo comparison",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Fit
            )
            // Label
            Surface(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 16.dp),
                shape = RoundedCornerShape(16.dp),
                color = IPhoneControlFill.copy(alpha = 0.85f)
            ) {
                Text(
                    text = "ORIGINAL (HOLDING)",
                    color = IPhoneYellow,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                )
            }
        } else if (isSplitActive && originalImageBitmap != null) {
            // Split view: Left = Original, Right = Edited
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(totalWidth) {
                        detectDragGestures { change, dragAmount ->
                            change.consume()
                            val newX = (splitPosition * totalWidth + dragAmount.x).coerceIn(0f, totalWidth)
                            splitPosition = newX / totalWidth
                        }
                    }
            ) {
                // Background Layer: Edited image
                Image(
                    bitmap = editedImageBitmap,
                    contentDescription = "Edited photo",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Fit
                )

                // Clipped Left Layer: Original image
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .drawWithContent {
                            clipRect(left = 0f, top = 0f, right = splitX, bottom = size.height) {
                                this@drawWithContent.drawContent()
                            }
                        }
                ) {
                    Image(
                        bitmap = originalImageBitmap,
                        contentDescription = "Original photo left",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Fit
                    )
                }

                // Vertical Divider Line
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                ) {
                    Box(
                        modifier = Modifier
                            .offset { IntOffset(splitX.roundToInt() - 1.dp.roundToPx(), 0) }
                            .width(2.5.dp)
                            .fillMaxSize()
                            .background(Color.White.copy(alpha = 0.9f))
                    )

                    // Drag Handle Knob
                    Surface(
                        modifier = Modifier
                            .offset {
                                IntOffset(
                                    (splitX - 22.dp.roundToPx()).roundToInt(),
                                    centerY - 22.dp.roundToPx()
                                )
                            }
                            .size(44.dp)
                            .testTag("split_slider_knob"),
                        shape = CircleShape,
                        color = Color.White,
                        shadowElevation = 8.dp
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.SwapHoriz,
                                contentDescription = "Drag comparison slider",
                                tint = IPhoneBlack,
                                modifier = Modifier.size(26.dp)
                            )
                        }
                    }
                }

                // Pill indicators for Before / After
                RowPills(
                    splitRatio = splitPosition,
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 16.dp)
                )
            }
        } else {
            // Normal Edited Photo View with "Hold to Compare" overlay gesture
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(originalImageBitmap) {
                        detectTapGestures(
                            onPress = {
                                if (originalImageBitmap != null) {
                                    isHoldingOriginal = true
                                    tryAwaitRelease()
                                    isHoldingOriginal = false
                                }
                            }
                        )
                    }
            ) {
                Image(
                    bitmap = editedImageBitmap,
                    contentDescription = "Photo preview",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Fit
                )
            }
        }
    }
}

@Composable
private fun RowPills(splitRatio: Float, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(Color.Black.copy(alpha = 0.65f))
            .padding(horizontal = 14.dp, vertical = 6.dp)
    ) {
        androidx.compose.foundation.layout.Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "BEFORE",
                color = if (splitRatio > 0.15f) Color.White.copy(alpha = 0.7f) else Color.Gray,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "  |  ",
                color = Color.White.copy(alpha = 0.3f),
                fontSize = 11.sp
            )
            Text(
                text = "AFTER",
                color = if (splitRatio < 0.85f) IPhoneYellow else Color.Gray,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
