package com.svd.svdagencies.data.model.customer

import com.google.gson.annotations.SerializedName

data class CatalogResponse(
    @SerializedName("status") val status: String,
    @SerializedName("categories_count") val categoriesCount: Int,
    @SerializedName("catalog") val catalog: List<CategoryData>
)

data class CategoryData(
    @SerializedName("category_id") val categoryId: Int,
    @SerializedName("category_name") val categoryName: String,
    @SerializedName("products_count") val productsCount: Int,
    @SerializedName("products") val products: List<ProductData>
)

data class ProductData(
    @SerializedName("id") val id: Int,
    @SerializedName("name") val name: String,
    @SerializedName("company") val company: String,
    @SerializedName("mrp") val mrp: Double,
    @SerializedName("selling_price") val sellingPrice: Double,
    @SerializedName("buying_price") val buyingPrice: Double,
    @SerializedName("margin") val margin: Double,
    @SerializedName("stock") val stock: Int,
    @SerializedName("pcs_count") val pcsCount: Int,
    @SerializedName("image") val image: String,
    @SerializedName("description") val description: String
)
