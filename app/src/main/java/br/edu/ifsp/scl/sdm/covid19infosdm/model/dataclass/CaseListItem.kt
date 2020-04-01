package br.edu.ifsp.scl.sdm.covid19infosdm.model.dataclass

import com.google.gson.annotations.SerializedName

data class CaseListItem(
    @SerializedName("Cases")
    val cases: Int,
    @SerializedName("Country")
    val country: String,
    @SerializedName("Date")
    val date: String,
    @SerializedName("Lat")
    val lat: Double,
    @SerializedName("Lon")
    val lon: Double,
    @SerializedName("Province")
    val province: String,
    @SerializedName("Status")
    val status: String
)