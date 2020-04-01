package br.edu.ifsp.scl.sdm.covid19infosdm.model

import android.content.Context
import android.util.Log
import androidx.lifecycle.MutableLiveData
import br.edu.ifsp.scl.sdm.covid19infosdm.R
import br.edu.ifsp.scl.sdm.covid19infosdm.model.Covid19Api.BASE_URL
import br.edu.ifsp.scl.sdm.covid19infosdm.model.Covid19Api.COUNTRIES_ENDPOINT
import br.edu.ifsp.scl.sdm.covid19infosdm.model.dataclass.CaseList
import br.edu.ifsp.scl.sdm.covid19infosdm.model.dataclass.CountryList
import com.android.volley.Request
import com.android.volley.toolbox.JsonArrayRequest
import com.android.volley.toolbox.Volley
import com.google.gson.Gson
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class Covid19Service(val context: Context) {
    private val requestQueue = Volley.newRequestQueue(context)
    private val gson = Gson()

    /* Cria uma implementação da interface usando um objeto retrofit */
    private val retrofitServices = with (Retrofit.Builder()) {
        baseUrl(BASE_URL)
        addConverterFactory(GsonConverterFactory.create())
        build()
    }.create(Covid19Api.RetrofitServices::class.java)

    /* Acesso a Web Service usando Volley */
    fun callGetCountries(): MutableLiveData<CountryList> {
        val url = "${BASE_URL}${COUNTRIES_ENDPOINT}"

        val countriesListLd = MutableLiveData<CountryList>()

        val request = JsonArrayRequest(
            Request.Method.GET,
            url,
            null,
            { countriesList ->
                countriesListLd.value = gson.fromJson(countriesList.toString(), CountryList::class.java)
            },
            { error ->  Log.e(context.getString(R.string.app_name), "${error.message}")}
        )

        requestQueue.add(request)

        return countriesListLd
    }

    /* Acesso a Web Service usando Retrofit. Como os serviços retornam o mesmo tipo de resposta
    * foram aglutinados numa mesma função */
    fun callService(countryName: String, status: String, service: String): MutableLiveData<CaseList> {
        val caseList: MutableLiveData<CaseList> = MutableLiveData()

        /* Callback usado pelos serviços que retornam o mesmo tipo de JSON */
        val callback = object: Callback<CaseList> {
            override fun onResponse(call: Call<CaseList>, response: Response<CaseList>) {
                if (response.isSuccessful){
                    caseList.value = response.body()
                }
            }
            override fun onFailure(call: Call<CaseList>, t: Throwable) {
                Log.e(context.getString(R.string.app_name), "")
            }
        }

        /* O Serviço correto é chamado */
        when (service) {
            Covid19Api.RetrofitServices.Services.DAY_ONE -> { retrofitServices.getDayOne(countryName, status).enqueue(callback) }
            Covid19Api.RetrofitServices.Services.BY_COUNTRY -> { retrofitServices.getByCountry(countryName, status).enqueue(callback) }
        }

        return caseList
    }
}