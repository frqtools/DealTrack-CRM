package com.frqtools.dealtrackcrm.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.frqtools.dealtrackcrm.ui.theme.*
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

// Formats currency nicely
fun formatCurrency(amount: Double, currency: String = "PKR"): String {
    val formatter = NumberFormat.getNumberInstance(Locale.getDefault())
    return "$currency ${formatter.format(amount)}"
}

// Formats timestamp to human readable date
fun formatDate(timestamp: Long): String {
    val date = Date(timestamp)
    val sdf = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
    return sdf.format(date)
}

// Formats timestamp to human readable date + time
fun formatDateTime(timestamp: Long): String {
    val date = Date(timestamp)
    val sdf = SimpleDateFormat("dd MMM yyyy, h:mm a", Locale.getDefault())
    return sdf.format(date)
}

@Composable
fun ClientAvatar(name: String, modifier: Modifier = Modifier, size: Dp = 40.dp) {
    val initials = name.split(" ")
        .filter { it.isNotEmpty() }
        .map { it.first().uppercaseChar() }
        .take(2)
        .joinToString("")

    val colorIndex = kotlin.math.abs(name.hashCode()) % AvatarColors.size
    val bgColor = AvatarColors.getOrElse(colorIndex) { PrimaryBlue }

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(bgColor)
    ) {
        Text(
            text = initials.ifEmpty { "?" },
            color = Color.White,
            fontWeight = FontWeight.SemiBold,
            fontSize = if (size > 48.dp) 20.sp else 14.sp,
            textAlign = TextAlign.Center
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatusChip(status: String) {
    val (textColor, bgColor) = when (status.lowercase(Locale.getDefault())) {
        "won" -> Pair(WonGreen, WonGreenContainer)
        "lost" -> Pair(LostRed, LostRedContainer)
        "open" -> Pair(PrimaryBlue, PrimaryContainer)
        "in progress" -> Pair(WarningAmber, WarningContainer)
        "proposal sent" -> Pair(ProposalPurple, ProposalPurpleContainer)
        "on hold" -> Pair(OnHoldGray, OnHoldContainer)
        else -> Pair(PrimaryBlue, PrimaryContainer)
    }

    Surface(
        color = bgColor,
        shape = RoundedCornerShape(50),
        modifier = Modifier.padding(2.dp)
    ) {
        Text(
            text = status,
            color = textColor,
            style = AppTypography.labelSmall,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}

@Composable
fun PriorityChip(priority: String) {
    val (textColor, bgColor) = when (priority.lowercase(Locale.getDefault())) {
        "urgent" -> Pair(LostRed, LostRedContainer)
        "important" -> Pair(WarningAmber, WarningContainer)
        else -> Pair(OnHoldGray, OnHoldContainer)
    }

    Surface(
        color = bgColor,
        shape = RoundedCornerShape(4.dp),
        modifier = Modifier.padding(2.dp)
    ) {
        Text(
            text = priority,
            color = textColor,
            style = AppTypography.labelSmall,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
        )
    }
}

@Composable
fun OutlinedFormTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    placeholder: String,
    modifier: Modifier = Modifier,
    isError: Boolean = false,
    errorMessage: String? = null,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    singleLine: Boolean = true,
    maxLines: Int = 1,
    testTag: String = ""
) {
    Column(modifier = modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            label = { Text(label) },
            placeholder = { Text(placeholder) },
            isError = isError,
            keyboardOptions = keyboardOptions,
            singleLine = singleLine,
            maxLines = maxLines,
            shape = RoundedCornerShape(8.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = PrimaryBlue,
                unfocusedBorderColor = OutlineColor,
                errorBorderColor = LostRed,
                focusedLabelColor = PrimaryBlue,
                unfocusedLabelColor = OnSurfaceVariantText
            ),
            modifier = Modifier
                .fillMaxWidth()
                .testTag(testTag)
        )
        if (isError && errorMessage != null) {
            Text(
                text = errorMessage,
                color = LostRed,
                style = AppTypography.bodySmall,
                modifier = Modifier.padding(start = 4.dp, top = 4.dp)
            )
        }
    }
}

@Composable
fun EmptyStateView(
    icon: ImageVector,
    title: String,
    description: String,
    buttonText: String? = null,
    onButtonClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = OnSurfaceVariantText,
            modifier = Modifier
                .size(64.dp)
                .padding(bottom = 16.dp)
        )
        Text(
            text = title,
            style = AppTypography.titleLarge,
            color = OnSurfaceText,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        Text(
            text = description,
            style = AppTypography.bodyMedium,
            color = OnSurfaceVariantText,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(bottom = 24.dp)
        )
        if (buttonText != null && onButtonClick != null) {
            Button(
                onClick = onButtonClick,
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier.height(48.dp)
            ) {
                Text(buttonText, style = AppTypography.labelLarge, color = OnPrimaryBlue)
            }
        }
    }
}

// Sanitizes a phone number for direct dialing (preserves '+' if it starts with it, filters out other non-digits)
fun cleanPhoneForDialing(phone: String): String {
    val trimmed = phone.trim()
    val hasPlus = trimmed.startsWith("+")
    val digits = trimmed.filter { it.isDigit() }
    return if (hasPlus) "+$digits" else digits
}

// Sanitizes a phone number for WhatsApp links (wa.me)
// - Strips leading +
// - Strips leading 00
// - Converts local Pakistan number (starting with 0) to include international country code (92)
// - Filters out all other non-digits (spaces, hyphens, parentheses, etc.)
fun cleanPhoneForWhatsApp(phone: String, currency: String = "PKR"): String {
    var trimmed = phone.trim()
    
    // If it starts with 00, treat it as + country code start
    if (trimmed.startsWith("00")) {
        trimmed = "+" + trimmed.substring(2)
    }
    
    // If it starts with +, clean it and remove the + since wa.me expects digits only
    if (trimmed.startsWith("+")) {
        return trimmed.filter { it.isDigit() }
    }
    
    val digitsOnly = trimmed.filter { it.isDigit() }
    
    // Pakistani local number starts with 0
    if (digitsOnly.startsWith("0")) {
        val withoutLeadingZero = digitsOnly.substring(1)
        val countryCode = if (currency == "PKR") "92" else "92" // default to 92 for Pakistan/PKR
        return "$countryCode$withoutLeadingZero"
    }
    
    // If it's a 10-digit number without leading 0 and currency is PKR, prepend 92
    if (digitsOnly.length == 10 && currency == "PKR") {
        return "92$digitsOnly"
    }
    
    return digitsOnly
}

// Cleans numeric string inputs (e.g., removing commas, currency symbols, and spaces) for parsing to double
fun cleanPriceInput(input: String): String {
    return input.replace(",", "")
                .replace(" ", "")
                .replace("$", "")
                .replace("€", "")
                .replace("£", "")
                .replace("PKR", "")
                .replace("pkr", "")
                .trim()
}

