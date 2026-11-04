package com.monager.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

// Hoisted once: no allocation per recomposition.
// Cheap "3D": 2-stop gradient + 1px highlight. No blur, no per-item shadow().
private val RaisedShape = RoundedCornerShape(20.dp)
private val RaisedBrush = Brush.verticalGradient(listOf(Color(0xFFFFFBF2), Color(0xFFEFE6D2)))
private val Highlight = Color.White.copy(alpha = 0.35f)

fun Modifier.raised(): Modifier =
    this.background(RaisedBrush, RaisedShape).border(1.dp, Highlight, RaisedShape)
