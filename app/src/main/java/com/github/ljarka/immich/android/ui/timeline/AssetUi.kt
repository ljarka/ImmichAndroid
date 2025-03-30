package com.github.ljarka.immich.android.ui.timeline

import com.github.ljarka.immich.android.db.AssetType

data class AssetUi(
    val url: String,
    val span: Int,
    val id: String,
    val type: AssetType,
)