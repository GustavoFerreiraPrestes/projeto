package com.ifpr.androidapptemplate.baseclasses

data class Item(
    var nome: String? = null,
    var tipo: String? = null,
    var descricao: String? = null,
    var valor: Float? = null,
    val base64Image: String? = null
)
