package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AdjustmentItem
import com.example.data.model.PhotoAdjustments
import com.example.ui.theme.IPhoneBlack
import com.example.ui.theme.IPhoneCardDark
import com.example.ui.theme.IPhoneCardElevated
import com.example.ui.theme.IPhoneControlFill
import com.example.ui.theme.IPhoneTextPrimary
import com.example.ui.theme.IPhoneTextSecondary
import com.example.ui.theme.IPhoneYellow
import kotlin.math.roundToInt

@Composable
fun AdjustmentToolBar(
    adjustments: PhotoAdjustments,
    selectedItem: AdjustmentItem,
    onSelectItem: (AdjustmentItem) -> Unit,
    onValueChange: (AdjustmentItem, Float) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(IPhoneBlack)
            .padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Horizontal Carousel of Tool Icons (iPhone style)
        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(AdjustmentItem.values()) { item ->
                val currentValue = getAdjustmentValue(adjustments, item)
                val isSelected = item == selectedItem
                val isModified = currentValue != item.defaultVal

                ToolIconItem(
                    item = item,
                    value = currentValue,
                    isSelected = isSelected,
                    isModified = isModified,
                    onClick = { onSelectItem(item) }
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Selected Tool Name and Current Value readout
        val currentValue = getAdjustmentValue(adjustments, selectedItem)
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = selectedItem.title.uppercase(),
                color = if (currentValue != selectedItem.defaultVal) IPhoneYellow else IPhoneTextSecondary,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
            Spacer(modifier = Modifier.width(8.dp))
            val sign = if (currentValue > 0) "+" else ""
            Text(
                text = "$sign${currentValue.roundToInt()}",
                color = if (currentValue != selectedItem.defaultVal) IPhoneYellow else Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
            if (currentValue != selectedItem.defaultVal) {
                Spacer(modifier = Modifier.width(12.dp))
                // Quick Reset chip
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = IPhoneControlFill,
                    modifier = Modifier.clickable { onValueChange(selectedItem, selectedItem.defaultVal) }
                ) {
                    Text(
                        text = "RESET",
                        color = Color.White.copy(alpha = 0.8f),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // iPhone Dial Slider
        IPhoneDialSlider(
            value = currentValue,
            min = selectedItem.min,
            max = selectedItem.max,
            defaultVal = selectedItem.defaultVal,
            onValueChange = { onValueChange(selectedItem, it) },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
        )
    }
}

@Composable
private fun ToolIconItem(
    item: AdjustmentItem,
    value: Float,
    isSelected: Boolean,
    isModified: Boolean,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clickable(onClick = onClick)
            .testTag("tool_${item.name.lowercase()}")
    ) {
        Box(
            modifier = Modifier
                .size(52.dp)
                .clip(CircleShape)
                .background(if (isSelected) IPhoneControlFill else IPhoneCardDark)
                .border(
                    width = if (isSelected) 2.dp else if (isModified) 1.5.dp else 0.dp,
                    color = if (isSelected) IPhoneYellow else if (isModified) IPhoneYellow.copy(alpha = 0.6f) else Color.Transparent,
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = item.icon,
                contentDescription = item.title,
                tint = if (isSelected || isModified) IPhoneYellow else Color.White.copy(alpha = 0.85f),
                modifier = Modifier.size(24.dp)
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = if (isModified) "${if (value > 0) "+" else ""}${value.roundToInt()}" else item.title,
            color = if (isModified) IPhoneYellow else if (isSelected) Color.White else IPhoneTextSecondary,
            fontSize = 10.sp,
            fontWeight = if (isSelected || isModified) FontWeight.Bold else FontWeight.Normal,
            maxLines = 1
        )
    }
}

@Composable
private fun IPhoneDialSlider(
    value: Float,
    min: Float,
    max: Float,
    defaultVal: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = min..max,
            colors = SliderDefaults.colors(
                thumbColor = IPhoneYellow,
                activeTrackColor = IPhoneYellow,
                inactiveTrackColor = IPhoneCardElevated
            ),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("adjustment_slider")
        )

        // Center zero tick mark label
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "${min.roundToInt()}",
                color = IPhoneTextSecondary,
                fontSize = 10.sp
            )
            if (min < 0 && max > 0) {
                Text(
                    text = "• 0 •",
                    color = if (value.roundToInt() == 0) IPhoneYellow else IPhoneTextSecondary,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.clickable { onValueChange(defaultVal) }
                )
            }
            Text(
                text = "+${max.roundToInt()}",
                color = IPhoneTextSecondary,
                fontSize = 10.sp
            )
        }
    }
}

private fun getAdjustmentValue(adj: PhotoAdjustments, item: AdjustmentItem): Float {
    return when (item) {
        AdjustmentItem.EXPOSURE -> adj.exposure
        AdjustmentItem.BRILLIANCE -> adj.brilliance
        AdjustmentItem.HIGHLIGHTS -> adj.highlights
        AdjustmentItem.SHADOWS -> adj.shadows
        AdjustmentItem.CONTRAST -> adj.contrast
        AdjustmentItem.BRIGHTNESS -> adj.brightness
        AdjustmentItem.BLACK_POINT -> adj.blackPoint
        AdjustmentItem.SATURATION -> adj.saturation
        AdjustmentItem.VIBRANCE -> adj.vibrance
        AdjustmentItem.WARMTH -> adj.warmth
        AdjustmentItem.TINT -> adj.tint
        AdjustmentItem.SHARPNESS -> adj.sharpness
        AdjustmentItem.DEFINITION -> adj.definition
        AdjustmentItem.NOISE_REDUCTION -> adj.noiseReduction
        AdjustmentItem.VIGNETTE -> adj.vignette
    }
}
