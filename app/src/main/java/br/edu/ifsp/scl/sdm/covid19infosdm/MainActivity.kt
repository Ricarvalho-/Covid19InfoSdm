package br.edu.ifsp.scl.sdm.covid19infosdm

import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import br.edu.ifsp.scl.sdm.covid19infosdm.model.dataclass.ByCountryResponseList
import br.edu.ifsp.scl.sdm.covid19infosdm.model.dataclass.DayOneResponseList
import br.edu.ifsp.scl.sdm.covid19infosdm.viewmodel.Covid19ViewModel
import com.jjoe64.graphview.helper.DateAsXAxisLabelFormatter
import com.jjoe64.graphview.series.DataPoint
import com.jjoe64.graphview.series.LineGraphSeries
import kotlinx.android.synthetic.main.activity_main.*
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : AppCompatActivity() {
    private var countryNameSlugMap = emptyMap<String, String>()
    private val viewModel: Covid19ViewModel by viewModels()
    private val selectedCountry get() = countrySp.selectedItem?.toString().orEmpty()
    private val selectedStatus get() = statusSp.selectedItem?.toString().orEmpty()

    /* Classe para os serviços que serão acessados */
    private enum class Information(val type: String){
        DAY_ONE("Day one"),
        BY_COUNTRY("By country")
    }

    /* Classe para o status que será buscado no serviço */
    private enum class Status(val type: String){
        CONFIRMED("Confirmed"),
        RECOVERED("Recovered"),
        DEATHS("Deaths")
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        countryAdapterInit()
        informationAdapterInit()
        statusAdapterInit()
    }

    fun onRetrieveClick(view: View) {
        if (countryNameSlugMap.isEmpty()) return
        loading.visibility = View.VISIBLE

        when (infoSp.selectedItem.toString()) {
            Information.DAY_ONE.type -> fetchDayOne()
            Information.BY_COUNTRY.type -> fetchByCountry()
        }
    }

    private fun countryAdapterInit() {
        /* Preenchido por Web Service */
        val countryAdapter = ArrayAdapter<String>(this, android.R.layout.simple_list_item_1)
        countrySp.adapter = countryAdapter
        loading.visibility = View.VISIBLE
        viewModel.fetchCountries().observe(
            this,
            Observer { countryList ->
                loading.visibility = View.GONE
                countryNameSlugMap = countryList
                    .filterNot { it.country.isEmpty() }
                    .sortedBy { it.country }
                    .onEach { countryAdapter.add(it.country) }
                    .associate { it.country to it.slug }
            }
        )
    }

    private fun informationAdapterInit() {
        val informationList = Information.values().map { it.type }

        infoSp.adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, informationList)
        infoSp.onItemSelectedListener = object: AdapterView.OnItemSelectedListener {
            override fun onNothingSelected(parent: AdapterView<*>?) { }

            // A nova versão dos serviços alterou a forma como dispomos os dados
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                when (position) {
                    Information.DAY_ONE.ordinal -> View.VISIBLE
                    Information.BY_COUNTRY.ordinal -> View.GONE
                    else -> null
                }?.let {
                    viewModeTv.visibility = it
                    viewModeRg.visibility = it
                }
            }
        }
    }

    private fun statusAdapterInit() {
        val statusList = Status.values().map { it.type }
        statusSp.adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, statusList)
    }

    private fun fetchDayOne() {
        val countrySlug = countryNameSlugMap[selectedCountry] ?: return

        viewModel.fetchDayOne(countrySlug, selectedStatus).observe(
            this,
            Observer { casesList ->
                loading.visibility = View.GONE
                if (viewModeTextRb.isChecked) updateTextData(casesList)
                else updateChartData(casesList)
            }
        )
    }

    private fun updateTextData(casesList: DayOneResponseList) {
        textMode()
        resultTv.text = casesListToString(casesList)
    }

    private fun updateChartData(casesList: DayOneResponseList) {
        if (casesList.isEmpty()) {
            textMode()
            resultTv.text = getString(R.string.no_data)
            return
        }

        chartMode()
        resultGv.removeAllSeries()
        resultGv.gridLabelRenderer.resetStyles()

        /* Preparando pontos */
        val points = casesList.map {
            val clippedDate = it.date.substring(0, 10)
            val date = SimpleDateFormat("yyyy-MM-dd", Locale.US).parse(clippedDate)
            DataPoint(date, it.cases.toDouble())
        }

        /* Formatando gráfico */
        resultGv.gridLabelRenderer.setHumanRounding(false)
        resultGv.gridLabelRenderer.labelFormatter = DateAsXAxisLabelFormatter(this)

        resultGv.gridLabelRenderer.numHorizontalLabels = 4
        resultGv.viewport.setMinX(points.first().x)
        resultGv.viewport.setMaxX(points.last().x)
        resultGv.viewport.isXAxisBoundsManual = true

        resultGv.gridLabelRenderer.numVerticalLabels = 4
        resultGv.viewport.setMinY(points.first().y)
        resultGv.viewport.setMaxY(points.last().y)
        resultGv.viewport.isYAxisBoundsManual = true

        val pointsSeries = LineGraphSeries(points.toTypedArray())
        resultGv.addSeries(pointsSeries)
    }

    private fun fetchByCountry() {
        val countrySlug = countryNameSlugMap[selectedCountry] ?: return

        textMode()
        viewModel.fetchByCountry(countrySlug, selectedStatus).observe(
            this,
            Observer {
                loading.visibility = View.GONE
                resultTv.text = casesListToString(it)
            }
        )
    }

    private fun textMode() {
        resultTv.visibility = View.VISIBLE
        resultGv.visibility = View.GONE
    }

    private fun chartMode() {
        resultTv.visibility = View.GONE
        resultGv.visibility = View.VISIBLE
    }

    private fun casesListToString(responseList: DayOneResponseList) =
        if (responseList.isEmpty()) getString(R.string.no_data)
        else StringBuilder().apply {
            responseList.forEach {
                append(
                    """
                    Casos: ${it.cases}
                    Data: ${it.date.substring(0, 10)}
                    
                    
                """.trimIndent()
                )
            }
        }.toString()

    private fun casesListToString(responseList: ByCountryResponseList) =
        if (responseList.isEmpty()) getString(R.string.no_data)
        else StringBuilder().apply {
            responseList.forEach { responseItem ->
                responseItem.province.takeUnless { it.isNullOrEmpty() }?.let {
                    appendln("Estado/Província: $it")
                }
                responseItem.city.takeUnless { it.isNullOrEmpty() }?.let {
                    appendln("Cidade: $it")
                }
                append(
                    """
                    Casos: ${responseItem.cases}
                    Data: ${responseItem.date.substring(0, 10)}
                    
                    
                """.trimIndent()
                )
            }
        }.toString()
}
