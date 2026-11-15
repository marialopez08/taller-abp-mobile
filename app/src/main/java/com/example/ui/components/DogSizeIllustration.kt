package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.domain.model.DogSize

/**
 * Ilustración vectorial escalable para cada tamaño de perro (Pequeño, Mediano, Grande).
 * Utiliza Canvas de Compose para diferenciar claramente proporciones, rasgos y escala.
 */
@Composable
fun DogSizeIllustration(
  dogSize: DogSize,
  modifier: Modifier = Modifier,
  containerSize: Dp = 84.dp,
) {
  val bgColor = when (dogSize) {
    DogSize.PEQUENO -> Color(0xFFD9F3E8)
    DogSize.MEDIANO -> Color(0xFFDCEEF8)
    DogSize.GRANDE -> Color(0xFFF3EAD9)
  }

  Box(
    modifier = modifier
      .size(containerSize)
      .clip(CircleShape)
      .background(bgColor),
    contentAlignment = Alignment.Center,
  ) {
    Canvas(modifier = Modifier.size(containerSize * 0.82f)) {
      val w = size.width
      val h = size.height

      when (dogSize) {
        DogSize.PEQUENO -> drawSmallDog(w, h)
        DogSize.MEDIANO -> drawMediumDog(w, h)
        DogSize.GRANDE -> drawLargeDog(w, h)
      }
    }
  }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawSmallDog(w: Float, h: Float) {
  val fur = Color(0xFFE7BA83)
  val furDark = Color(0xFFC99254)
  val collar = Color(0xFF2B8C74)
  val nose = Color(0xFF2F2822)

  // Soap bubbles accent
  drawCircle(color = Color.White.copy(alpha = 0.85f), radius = w * 0.06f, center = Offset(w * 0.22f, h * 0.26f))
  drawCircle(color = Color.White.copy(alpha = 0.7f), radius = w * 0.04f, center = Offset(w * 0.78f, h * 0.22f))

  // Compact body
  drawRoundRect(
    color = fur,
    topLeft = Offset(w * 0.34f, h * 0.56f),
    size = Size(w * 0.34f, h * 0.24f),
    cornerRadius = CornerRadius(w * 0.12f, w * 0.12f),
  )
  // Cute curled tail
  drawArc(
    color = furDark,
    startAngle = -30f,
    sweepAngle = 210f,
    useCenter = false,
    topLeft = Offset(w * 0.62f, h * 0.48f),
    size = Size(w * 0.15f, h * 0.15f),
    style = Stroke(width = w * 0.055f, cap = StrokeCap.Round),
  )
  // Short legs
  drawRoundRect(color = furDark, topLeft = Offset(w * 0.37f, h * 0.73f), size = Size(w * 0.08f, h * 0.13f), cornerRadius = CornerRadius(w * 0.04f))
  drawRoundRect(color = furDark, topLeft = Offset(w * 0.56f, h * 0.73f), size = Size(w * 0.08f, h * 0.13f), cornerRadius = CornerRadius(w * 0.04f))

  // Pointy small dog ears
  val leftEar = Path().apply {
    moveTo(w * 0.32f, h * 0.41f)
    lineTo(w * 0.25f, h * 0.24f)
    lineTo(w * 0.42f, h * 0.35f)
    close()
  }
  val rightEar = Path().apply {
    moveTo(w * 0.60f, h * 0.41f)
    lineTo(w * 0.67f, h * 0.24f)
    lineTo(w * 0.50f, h * 0.35f)
    close()
  }
  drawPath(leftEar, color = furDark)
  drawPath(rightEar, color = furDark)

  // Head
  drawCircle(color = fur, radius = w * 0.18f, center = Offset(w * 0.46f, h * 0.46f))
  // Collar
  drawRoundRect(
    color = collar,
    topLeft = Offset(w * 0.36f, h * 0.59f),
    size = Size(w * 0.20f, h * 0.055f),
    cornerRadius = CornerRadius(w * 0.03f),
  )
  // Eyes & Nose
  drawCircle(color = nose, radius = w * 0.025f, center = Offset(w * 0.40f, h * 0.44f))
  drawCircle(color = nose, radius = w * 0.025f, center = Offset(w * 0.52f, h * 0.44f))
  drawOval(color = nose, topLeft = Offset(w * 0.435f, h * 0.485f), size = Size(w * 0.055f, h * 0.04f))
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawMediumDog(w: Float, h: Float) {
  val fur = Color(0xFFD99B66)
  val cream = Color(0xFFFDF5E8)
  val furDark = Color(0xFF9C5E34)
  val collar = Color(0xFF3E7FA6)
  val nose = Color(0xFF2B221D)

  // Bubbles
  drawCircle(color = Color.White.copy(alpha = 0.85f), radius = w * 0.055f, center = Offset(w * 0.16f, h * 0.28f))
  drawCircle(color = Color.White.copy(alpha = 0.7f), radius = w * 0.04f, center = Offset(w * 0.82f, h * 0.26f))

  // Medium proportional body
  drawRoundRect(
    color = fur,
    topLeft = Offset(w * 0.30f, h * 0.50f),
    size = Size(w * 0.44f, h * 0.26f),
    cornerRadius = CornerRadius(w * 0.13f, w * 0.13f),
  )
  // Chest patch
  drawCircle(color = cream, radius = w * 0.11f, center = Offset(w * 0.38f, h * 0.62f))

  // Tail
  drawLine(
    color = furDark,
    start = Offset(w * 0.72f, h * 0.56f),
    end = Offset(w * 0.84f, h * 0.40f),
    strokeWidth = w * 0.065f,
    cap = StrokeCap.Round,
  )
  // Legs
  drawRoundRect(color = furDark, topLeft = Offset(w * 0.34f, h * 0.69f), size = Size(w * 0.09f, h * 0.19f), cornerRadius = CornerRadius(w * 0.045f))
  drawRoundRect(color = furDark, topLeft = Offset(w * 0.60f, h * 0.69f), size = Size(w * 0.09f, h * 0.19f), cornerRadius = CornerRadius(w * 0.045f))

  // Head
  drawCircle(color = fur, radius = w * 0.19f, center = Offset(w * 0.38f, h * 0.38f))
  // Floppy beagle-style ears
  drawRoundRect(
    color = furDark,
    topLeft = Offset(w * 0.17f, h * 0.30f),
    size = Size(w * 0.11f, h * 0.21f),
    cornerRadius = CornerRadius(w * 0.055f),
  )
  drawRoundRect(
    color = furDark,
    topLeft = Offset(w * 0.48f, h * 0.30f),
    size = Size(w * 0.11f, h * 0.21f),
    cornerRadius = CornerRadius(w * 0.055f),
  )
  // Collar
  drawRoundRect(
    color = collar,
    topLeft = Offset(w * 0.28f, h * 0.53f),
    size = Size(w * 0.20f, h * 0.055f),
    cornerRadius = CornerRadius(w * 0.03f),
  )
  // Muzzle & Eyes
  drawOval(color = cream, topLeft = Offset(w * 0.29f, h * 0.39f), size = Size(w * 0.18f, h * 0.12f))
  drawCircle(color = nose, radius = w * 0.026f, center = Offset(w * 0.32f, h * 0.35f))
  drawCircle(color = nose, radius = w * 0.026f, center = Offset(w * 0.44f, h * 0.35f))
  drawOval(color = nose, topLeft = Offset(w * 0.35f, h * 0.405f), size = Size(w * 0.06f, h * 0.042f))
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawLargeDog(w: Float, h: Float) {
  val golden = Color(0xFFE0B462)
  val goldenLight = Color(0xFFF3D495)
  val goldenDark = Color(0xFFB58434)
  val collar = Color(0xFF8A6E4B)
  val nose = Color(0xFF2A211B)

  // Bubbles
  drawCircle(color = Color.White.copy(alpha = 0.85f), radius = w * 0.055f, center = Offset(w * 0.14f, h * 0.20f))
  drawCircle(color = Color.White.copy(alpha = 0.7f), radius = w * 0.04f, center = Offset(w * 0.86f, h * 0.22f))

  // Tall broad body
  drawRoundRect(
    color = golden,
    topLeft = Offset(w * 0.25f, h * 0.44f),
    size = Size(w * 0.54f, h * 0.31f),
    cornerRadius = CornerRadius(w * 0.15f, w * 0.15f),
  )
  // Fluffy chest mane
  drawCircle(color = goldenLight, radius = w * 0.14f, center = Offset(w * 0.36f, h * 0.57f))

  // Fluffy curved tail
  drawArc(
    color = goldenDark,
    startAngle = -60f,
    sweepAngle = 160f,
    useCenter = false,
    topLeft = Offset(w * 0.72f, h * 0.32f),
    size = Size(w * 0.19f, h * 0.26f),
    style = Stroke(width = w * 0.075f, cap = StrokeCap.Round),
  )
  // Strong tall legs
  drawRoundRect(color = goldenDark, topLeft = Offset(w * 0.29f, h * 0.67f), size = Size(w * 0.105f, h * 0.24f), cornerRadius = CornerRadius(w * 0.05f))
  drawRoundRect(color = goldenDark, topLeft = Offset(w * 0.63f, h * 0.67f), size = Size(w * 0.105f, h * 0.24f), cornerRadius = CornerRadius(w * 0.05f))

  // Large head
  drawCircle(color = golden, radius = w * 0.21f, center = Offset(w * 0.36f, h * 0.32f))
  // Soft dropped ears
  drawRoundRect(
    color = goldenDark,
    topLeft = Offset(w * 0.13f, h * 0.24f),
    size = Size(w * 0.11f, h * 0.20f),
    cornerRadius = CornerRadius(w * 0.055f),
  )
  drawRoundRect(
    color = goldenDark,
    topLeft = Offset(w * 0.48f, h * 0.24f),
    size = Size(w * 0.11f, h * 0.20f),
    cornerRadius = CornerRadius(w * 0.055f),
  )
  // Collar
  drawRoundRect(
    color = collar,
    topLeft = Offset(w * 0.24f, h * 0.49f),
    size = Size(w * 0.24f, h * 0.06f),
    cornerRadius = CornerRadius(w * 0.03f),
  )
  // Friendly snout & eyes
  drawOval(color = goldenLight, topLeft = Offset(w * 0.26f, h * 0.33f), size = Size(w * 0.20f, h * 0.13f))
  drawCircle(color = nose, radius = w * 0.028f, center = Offset(w * 0.29f, h * 0.285f))
  drawCircle(color = nose, radius = w * 0.028f, center = Offset(w * 0.43f, h * 0.285f))
  drawOval(color = nose, topLeft = Offset(w * 0.325f, h * 0.345f), size = Size(w * 0.07f, h * 0.048f))
}
