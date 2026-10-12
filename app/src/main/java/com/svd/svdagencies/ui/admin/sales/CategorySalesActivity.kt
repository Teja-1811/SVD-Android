package com.svd.svdagencies.ui.admin.sales

import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.LinearLayout
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import com.svd.svdagencies.data.api.admin.CategorySalesResponse
import com.svd.svdagencies.data.api.auth.ApiClient
import com.svd.svdagencies.ui.admin.AdminBaseActivity
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class CategorySalesActivity : AdminBaseActivity() {
    private lateinit var content: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(32, 32, 32, 32) }
        val periods = listOf("overall", "today", "week", "month", "year")
        root.addView(Spinner(this).apply {
            adapter = ArrayAdapter(this@CategorySalesActivity, android.R.layout.simple_spinner_dropdown_item, periods)
            setSelection(0)
            onItemSelectedListener = object : android.widget.AdapterView.OnItemSelectedListener {
                override fun onNothingSelected(parent: android.widget.AdapterView<*>?) = Unit
                override fun onItemSelected(parent: android.widget.AdapterView<*>?, view: android.view.View?, position: Int, id: Long) = load(periods[position])
            }
        })
        content = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        root.addView(content); setContentView(root)
        load("overall")
    }

    private fun load(period: String) = ApiClient.categorySalesApi.getSummary(period).enqueue(object : Callback<CategorySalesResponse> {
        override fun onResponse(call: Call<CategorySalesResponse>, response: Response<CategorySalesResponse>) {
            val data = response.body() ?: return failure()
            content.removeAllViews()
            content.addView(TextView(this@CategorySalesActivity).apply { text = "Total volume: %.3f L".format(data.total_volume); textSize = 20f })
            data.categories.forEach { category ->
                content.addView(TextView(this@CategorySalesActivity).apply { text = "$category  •  %.3f L  •  ₹%.2f".format(data.data_by_category[category] ?: 0.0, data.amount_by_category[category] ?: 0.0); textSize = 16f; setPadding(0, 16, 0, 16) })
            }
        }
        override fun onFailure(call: Call<CategorySalesResponse>, error: Throwable) = failure()
    })

    private fun failure() = Toast.makeText(this, "Unable to load category sales", Toast.LENGTH_SHORT).show()
}
