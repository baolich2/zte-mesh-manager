package com.example.ztemeshmanager

import android.app.AlertDialog
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.widget.Button
import android.widget.TextView
import android.widget.ListView

/** Shared dark, rounded dialog styling used by all native app dialogs. */
fun AlertDialog.Builder.showZteStyled(): AlertDialog {
    val dialog = show()
    val bg = GradientDrawable().apply {
        setColor(Color.rgb(17, 27, 42))
        cornerRadius = 22f
        setStroke(1, Color.rgb(51, 68, 94))
    }
    dialog.window?.setBackgroundDrawable(bg)
    dialog.window?.setDimAmount(0.68f)
    val titleId = dialog.context.resources.getIdentifier("alertTitle", "id", "android")
    dialog.findViewById<TextView>(titleId)?.apply {
        setTextColor(Color.rgb(239, 245, 255))
        textSize = 20f
    }
    val messageId = dialog.context.resources.getIdentifier("message", "id", "android")
    dialog.findViewById<TextView>(messageId)?.apply {
        setTextColor(Color.rgb(190, 205, 225))
        textSize = 15f
    }
    for (id in intArrayOf(-1, -2, -3)) {
        dialog.getButton(id)?.apply {
            setTextColor(Color.rgb(96, 190, 220))
            textSize = 14f
            isAllCaps = false
        }
    }
    dialog.listView?.apply {
        setBackgroundColor(Color.rgb(17, 27, 42))
        divider = android.graphics.drawable.ColorDrawable(Color.rgb(41, 56, 78))
        dividerHeight = 1
        for (i in 0 until childCount) (getChildAt(i) as? TextView)?.setTextColor(Color.rgb(230, 239, 252))
    }
    return dialog
}
