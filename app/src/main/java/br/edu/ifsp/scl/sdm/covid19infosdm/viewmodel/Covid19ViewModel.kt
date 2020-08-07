package br.edu.ifsp.scl.sdm.covid19infosdm.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import br.edu.ifsp.scl.sdm.covid19infosdm.model.Covid19Service
import br.edu.ifsp.scl.sdm.covid19infosdm.model.dataclass.ByCountryResponseList
import br.edu.ifsp.scl.sdm.covid19infosdm.model.dataclass.DayOneResponseList
import java.util.*

class Covid19ViewModel(application: Application) : AndroidViewModel(application) {
    private val model = Covid19Service(application.applicationContext)

    fun fetchCountries() = model.callGetCountries()

    fun fetchDayOne(countryName: String, status: String) = model.callService(countryName,
        status.toLowerCase(Locale.getDefault()),
        DayOneResponseList::class.java
    )
    fun fetchByCountry(countryName: String, status: String) = model.callService(countryName,
        status.toLowerCase(Locale.getDefault()),
        ByCountryResponseList::class.java
    )
}