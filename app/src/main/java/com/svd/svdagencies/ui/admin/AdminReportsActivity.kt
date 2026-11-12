package com.svd.svdagencies.ui.admin

import com.svd.svdagencies.utils.SharedDatePickerDialog as DatePickerDialog
import android.graphics.Color
import android.os.Bundle
import android.view.ViewGroup
import android.widget.*
import androidx.lifecycle.lifecycleScope
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.google.android.material.button.MaterialButton
import com.svd.svdagencies.utils.WhiteMaterialAlertDialogBuilder
import com.google.gson.JsonObject
import com.svd.svdagencies.R
import com.svd.svdagencies.data.api.admin.AdminReportsApi
import com.svd.svdagencies.data.api.auth.ApiClient
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import retrofit2.HttpException
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class AdminGstActivity : AdminReportsActivity()
class AdminPurchaseReportsActivity : AdminReportsActivity()

open class AdminReportsActivity : AdminBaseActivity() {
    private val purchase get() = this is AdminPurchaseReportsActivity
    private val api by lazy { ApiClient.retrofit.create(AdminReportsApi::class.java) }
    private lateinit var content: LinearLayout
    private lateinit var results: LinearLayout
    private lateinit var company: Spinner
    private lateinit var period: Spinner
    private lateinit var firstDate: TextView
    private lateinit var lastDate: TextView
    private lateinit var status: TextView
    private var companyIds = listOf("")
    private var config = JsonObject()
    private var purchaseUi: PurchaseReportUi? = null
    private var gstPeriod = 0
    private var reloadPending = false
    private val periodButtons = mutableListOf<MaterialButton>()
    private var busy = false
    private var filtersReady = false
    private val controls = mutableListOf<android.view.View>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.admin_simple_dashboard)
        setupAdminLayout(if(purchase) "Purchase reports" else "GST summary")
        findViewById<SwipeRefreshLayout>(R.id.swipeRefresh).isEnabled=false
        content=findViewById(R.id.contentContainer)
        if (purchase) {
            val ui = PurchaseReportUi(this, content)
            purchaseUi = ui
            company = ui.company
            firstDate = ui.firstDate
            lastDate = ui.lastDate
            period = Spinner(this).apply { adapter=ArrayAdapter(this@AdminReportsActivity,com.svd.svdagencies.R.layout.delivery_dropdown_item,listOf("Day","Week","Month","Year")) }
            status = ui.status
            results = ui.results
            controls.addAll(listOf(company, firstDate, lastDate, ui.refresh, ui.download))
            firstDate.setOnClickListener { pickDate(firstDate) }
            lastDate.setOnClickListener { pickDate(lastDate) }
            ui.refresh.setOnClickListener { if (!busy) clearFilters() }
            ui.download.setOnClickListener { if (!busy) download() }
        } else {
        content.addView(label(if(purchase) "Purchase history and company payments" else "GST paid, billed and estimated payable",20f).apply { visibility = android.view.View.GONE })
        content.addView(label("Choose your company and dates, then view or download a report.",13f).apply { visibility = android.view.View.GONE })
        fun row() = com.svd.svdagencies.utils.CompactLinearLayout(this).apply {
            orientation=LinearLayout.HORIZONTAL; tag="svd-fixed-row"
            layoutParams=LinearLayout.LayoutParams(-1,-2)
        }
        fun cell(title:String, view:android.view.View)=com.svd.svdagencies.utils.CompactLinearLayout(this).apply {
            orientation=LinearLayout.VERTICAL
            layoutParams=LinearLayout.LayoutParams(0,-2,1f).apply { setMargins(4,0,4,8) }
            addView(label(title,13f));addView(view,LinearLayout.LayoutParams(-1,-2))
        }
        company=Spinner(this).apply { tag="svd-full-width"; contentDescription="Company" }
        company.adapter=ArrayAdapter(this,com.svd.svdagencies.R.layout.delivery_dropdown_item,listOf("All companies"))
        period=Spinner(this).apply {
            tag="svd-full-width";contentDescription="Period"
            adapter=ArrayAdapter(this@AdminReportsActivity,com.svd.svdagencies.R.layout.delivery_dropdown_item,listOf("Day","Week","Month","Year"))
        }
        content.addView(cell("Company",company).apply { layoutParams=LinearLayout.LayoutParams(-1,-2) })
        firstDate=MaterialButton(this).apply {
            text=SimpleDateFormat("yyyy-MM-dd",Locale.US).format(Calendar.getInstance().time)
            tag="svd-full-width";contentDescription="GST report from date";isAllCaps=false
            setOnClickListener { pickDate(this) }
        }
        lastDate=MaterialButton(this).apply {
            text=firstDate.text
            tag="svd-full-width";contentDescription="GST report to date";isAllCaps=false
            setOnClickListener { pickDate(this) }
        }
        val refresh=MaterialButton(this).apply {
            text="";contentDescription="Refresh";tag="svd-full-width"
            setIconResource(R.drawable.ic_refresh);iconGravity=MaterialButton.ICON_GRAVITY_TEXT_START;iconPadding=0
            setOnClickListener { if(!busy)clearFilters() }
        }
        content.addView(row().apply { addView(cell("From",firstDate));addView(cell("To",lastDate)) })
        content.addView(refresh)
        controls.addAll(listOf(company,period,firstDate,lastDate,refresh))
        val actions=twoColumns()
        listOf("Print GST report" to { download() },"GST settings" to { gstSettings() }).forEach { (title,action) ->
            actions.addView(MaterialButton(this).apply { text=title;isAllCaps=false;textSize=12f;layoutParams=gridCell();setOnClickListener { if(!busy)action() };controls.add(this) })
        }
        content.addView(actions)
        status=label("",13f);content.addView(status)
        results=com.svd.svdagencies.utils.CompactLinearLayout(this).apply { orientation=LinearLayout.VERTICAL };content.addView(results)
        }
        lifecycleScope.launch {
            try{
                val companies=ApiClient.adminCompaniesApi.getCompanies().companies
                companyIds=listOf("")+companies.map{it.id.toString()}
                company.adapter=ArrayAdapter(this@AdminReportsActivity,com.svd.svdagencies.R.layout.delivery_dropdown_item,listOf("All companies")+companies.map{it.name})
                setupAutomaticFilters()
                filtersReady = true
                load()
            }catch(error:Exception){if(error is CancellationException)throw error;status.text=errorMessage(error)}
        }
    }

    private fun label(text:String,size:Float=14f)=TextView(this).apply { this.text=text;textSize=size;setTextColor(androidx.core.content.ContextCompat.getColor(this@AdminReportsActivity, com.svd.svdagencies.R.color.textColorPrimary));setPadding(8,14,8,14) }
    private fun button(text:String,action:()->Unit){content.addView(MaterialButton(this).apply{this.text=text;isAllCaps=false;if(text=="Clear"){this.text="";contentDescription="Refresh";setIconResource(R.drawable.ic_refresh);iconTint=android.content.res.ColorStateList.valueOf(androidx.core.content.ContextCompat.getColor(this@AdminReportsActivity, com.svd.svdagencies.R.color.surface_background));iconPadding=0};setOnClickListener{if(!busy)action()};controls.add(this)})}
    private fun dateControl(title:String,value:String):TextView {
        content.addView(label(title,13f))
        return label(value,16f).apply { setBackgroundColor(androidx.core.content.ContextCompat.getColor(this@AdminReportsActivity, com.svd.svdagencies.R.color.surface_background));minHeight=(48*resources.displayMetrics.density).toInt();setOnClickListener{pickDate(this)};content.addView(this);controls.add(this) }
    }
    private fun pickDate(view:TextView){
        val parts=view.text.toString().split("-").mapNotNull{it.toIntOrNull()}
        val calendar=Calendar.getInstance()
        if(parts.size==3)calendar.set(parts[0],parts[1]-1,parts[2])
        DatePickerDialog(this,R.style.Theme_Svd_CompactDatePicker,{_,year,month,day->view.text=String.format(Locale.US,"%04d-%02d-%02d",year,month+1,day); if(filtersReady)load()},calendar.get(Calendar.YEAR),calendar.get(Calendar.MONTH),calendar.get(Calendar.DAY_OF_MONTH)).show()
    }
    private fun setupAutomaticFilters() {
        val selectionListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: android.view.View?, position: Int, id: Long) {
                if (filtersReady) load()
            }
            override fun onNothingSelected(parent: AdapterView<*>?) = Unit
        }
        company.onItemSelectedListener = selectionListener
        period.onItemSelectedListener = selectionListener
    }
    private fun clearFilters() {
        company.setSelection(0)
        period.setSelection(0)
        gstPeriod=0
        periodButtons.forEachIndexed { i,b -> b.alpha=if(i==0)1f else .55f }
        val now = Calendar.getInstance()
        val format = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        if (purchase) now.set(Calendar.DAY_OF_MONTH, 1)
        firstDate.text = format.format(now.time)
        if (!purchase) lastDate.text = firstDate.text
        if (purchase) {
            now.set(Calendar.DAY_OF_MONTH, now.getActualMaximum(Calendar.DAY_OF_MONTH))
            lastDate.text = format.format(now.time)
        }
        load()
    }
    private fun filters():Map<String,String>{
        val values=mutableMapOf("company" to companyIds.getOrElse(company.selectedItemPosition){""})
        if(purchase){values["from_date"]=firstDate.text.toString();values["to_date"]=lastDate.text.toString()}
        else{values["from_date"]=firstDate.text.toString();values["to_date"]=lastDate.text.toString()}
        return values
    }
    private fun setBusy(value:Boolean){busy=value;controls.forEach{it.isEnabled=!value};if(value){status.visibility=android.view.View.GONE;showScreenLoading()}else{hideScreenLoading();status.visibility=android.view.View.VISIBLE}}
    private fun load(){
        if(busy){reloadPending=true;return}
        val selection=filters();setBusy(true);status.text="Loading summary…"
        lifecycleScope.launch {try{
            val data=if(purchase)api.purchases(selection)else api.gst(selection)
            results.removeAllViews()
            if(purchase){
                purchaseUi?.render(data)
            }else{
                config=data.getAsJsonObject("config")
                val metrics=twoColumns()
                listOf("GST paid to companies" to "purchase_tax","Customer GST" to "sales_tax","GST payable" to "cash_estimate","Eligible credit" to "eligible_tax").forEach { (title,key) ->
                    val card=com.google.android.material.card.MaterialCardView(this@AdminReportsActivity).apply { layoutParams=gridCell();radius=12*resources.displayMetrics.density;cardElevation=0f;strokeWidth=1;strokeColor=Color.LTGRAY }
                    val body=LinearLayout(this@AdminReportsActivity).apply { orientation=LinearLayout.VERTICAL;setPadding(16,12,16,12);addView(label(title,12f));addView(label(money(data,key),20f)) }
                    card.addView(body);metrics.addView(card)
                }
                results.addView(metrics)
                val rows=data.getAsJsonArray("records").map{it.asJsonObject}.map{listOf(string(it,"date"),money(it,"purchases"),money(it,"sales"),money(it,"credit"))}
                section("Summary records", true) {
                    table(listOf("Date","Purchase GST","Customer GST","Eligible credit"), rows + listOf(
                        listOf("Period total",money(data,"purchase_tax"),money(data,"sales_tax"),money(data,"eligible_tax")),
                        listOf("Estimated GST payable","","",money(data,"cash_estimate"))))
                }
                section("Generate GST records") {
                    resultAction("Generate past invoice GST") { generate("sales") }
                    resultAction("Generate purchase invoices") { generate("purchases") }
                }
                section("Purchase credit review") {
                    resultAction("Review purchases") { reviewPurchases() }
                }
                val heads=data.getAsJsonArray("heads").map{it.asJsonObject}.map{listOf(string(it,"name"),money(it,"output"),money(it,"opening"),money(it,"input"),money(it,"cash"),money(it,"remaining"))}
                section("CGST, SGST and IGST breakdown") {
                    table(listOf("Tax","Sales GST","Opening credit","Purchase credit","Estimated payable","Credit remaining"),heads)
                }
                section("GST registration settings") { resultAction("Edit GST settings") { gstSettings() } }
                section("Product GST rates") {
                    resultAction("View and edit product rates") { startActivity(android.content.Intent(this@AdminReportsActivity,com.svd.svdagencies.ui.admin.items.AdminItemsActivity::class.java)) }
                }

            }
            status.text=""
        }catch(error:Exception){if(error is CancellationException)throw error;status.text=errorMessage(error)}finally{setBusy(false);if(reloadPending){reloadPending=false;load()}}}
    }
    private fun twoColumns()=GridLayout(this).apply { columnCount=2;layoutParams=LinearLayout.LayoutParams(-1,-2) }
    private fun gridCell()=GridLayout.LayoutParams().apply {
        width=0;height=ViewGroup.LayoutParams.WRAP_CONTENT
        columnSpec=GridLayout.spec(GridLayout.UNDEFINED,1f)
        val gap=(4*resources.displayMetrics.density).toInt();setMargins(gap,gap,gap,gap)
    }
    private fun section(title:String, expanded:Boolean=false, body:()->Unit) {
        val outer=results
        val card=com.google.android.material.card.MaterialCardView(this).apply {
            radius=12*resources.displayMetrics.density;cardElevation=0f;strokeWidth=1
            strokeColor=Color.rgb(220,230,233)
            layoutParams=LinearLayout.LayoutParams(-1,-2).apply { bottomMargin=(12*resources.displayMetrics.density).toInt() }
        }
        val spacing=(12*resources.displayMetrics.density).toInt()
        val column=LinearLayout(this).apply { orientation=LinearLayout.VERTICAL;setPadding(spacing,0,spacing,spacing/2) }
        val header=LinearLayout(this).apply {
            orientation=LinearLayout.HORIZONTAL;gravity=android.view.Gravity.CENTER_VERTICAL
            minimumHeight=(48*resources.displayMetrics.density).toInt()
            isFocusable=true;isClickable=true
        }
        val heading=label(title,14f).apply {
            setTypeface(typeface,android.graphics.Typeface.BOLD)
            layoutParams=LinearLayout.LayoutParams(0,-2,1f)
        }
        val indicator=label(if(expanded) "−" else "+",20f).apply {
            gravity=android.view.Gravity.CENTER
            layoutParams=LinearLayout.LayoutParams((32*resources.displayMetrics.density).toInt(),-2)
            importantForAccessibility=android.view.View.IMPORTANT_FOR_ACCESSIBILITY_NO
        }
        val inner=LinearLayout(this).apply { orientation=LinearLayout.VERTICAL;visibility=if(expanded)android.view.View.VISIBLE else android.view.View.GONE }
        fun describe() { header.contentDescription=title+if(inner.visibility==android.view.View.VISIBLE) ", expanded" else ", collapsed" }
        header.setOnClickListener {
            val show=inner.visibility!=android.view.View.VISIBLE
            inner.visibility=if(show)android.view.View.VISIBLE else android.view.View.GONE
            indicator.text=if(show) "−" else "+"
            describe()
        }
        describe()
        results=inner
        try { body() } finally { results=outer }
        header.addView(heading);header.addView(indicator)
        column.addView(header);column.addView(inner);card.addView(column);outer.addView(card)
    }

    private fun resultAction(title:String, action:()->Unit) {
        results.addView(MaterialButton(this).apply { text=title;isAllCaps=false;textSize=13f;setOnClickListener { if(!busy)action() } })
    }
    private fun gstSettings() {
        val form=LinearLayout(this).apply { orientation=LinearLayout.VERTICAL;setPadding(32,16,32,16) }
        val gstin=EditText(this).apply { hint="Registered GSTIN";setText(string(config,"gstin")) }
        val date=EditText(this).apply { hint="GST start date (yyyy-MM-dd)";setText(string(config,"effective_from")) }
        val enabled=CheckBox(this).apply { text="Collect GST on new invoices";isChecked=config.get("enabled")?.asBoolean?:false }
        form.addView(label("Changes apply to new invoices; existing records stay unchanged.",12f));form.addView(gstin);form.addView(date);form.addView(enabled)
        val dialog=WhiteMaterialAlertDialogBuilder(this).setTitle("GST registration settings").setView(form).setNegativeButton("Cancel",null).setPositiveButton("Save",null).create()
        dialog.setOnShowListener { dialog.getButton(android.content.DialogInterface.BUTTON_POSITIVE).setOnClickListener {
            val body=JsonObject().apply { addProperty("gstin",gstin.text.toString().trim());addProperty("effective_from",date.text.toString().trim());addProperty("enabled",enabled.isChecked) }
            lifecycleScope.launch { try { api.settings(body);dialog.dismiss();load() } catch(error:Exception) { if(error is CancellationException)throw error;date.error=errorMessage(error) } }
        } }
        dialog.show()
    }
    private fun table(headers:List<String>,rows:List<List<String>>){
        val scroll=HorizontalScrollView(this, null, android.R.attr.horizontalScrollViewStyle);val table=com.svd.svdagencies.utils.CompactLinearLayout(this).apply{orientation=LinearLayout.VERTICAL}
        (listOf(headers)+rows).forEachIndexed{index,values->val row=com.svd.svdagencies.utils.CompactLinearLayout(this);values.forEach{value->row.addView(label(value,13f).apply{layoutParams=LinearLayout.LayoutParams((125*resources.displayMetrics.density).toInt(),ViewGroup.LayoutParams.WRAP_CONTENT);setBackgroundColor(if(index==0)androidx.core.content.ContextCompat.getColor(this@AdminReportsActivity, com.svd.svdagencies.R.color.surface_variant)else androidx.core.content.ContextCompat.getColor(this@AdminReportsActivity, com.svd.svdagencies.R.color.surface_background))})};table.addView(row)}
        scroll.addView(table);results.addView(scroll)
        if(rows.isEmpty())results.addView(label("No records for this selection."))
    }
    private fun generate(kind:String){
        val selection=filters();val body=JsonObject().apply{selection.forEach{(k,v)->addProperty(k,v)};addProperty("kind",kind)}
        setBusy(true);status.text="Generating database records…"
        lifecycleScope.launch {var saved=false;try{val result=api.generate(body);status.text=string(result,"message");saved=true}catch(error:Exception){if(error is CancellationException)throw error;status.text=errorMessage(error)}finally{setBusy(false)};if(saved)load()}
    }
    private fun download(){
        val selection=filters();setBusy(true);status.text="Downloading report…"
        lifecycleScope.launch {try{
            val response=if(purchase)api.purchasePdf(selection)else api.gstPdf(selection)
            val filename=(if(purchase)"purchase"else"gst")+"-${System.currentTimeMillis()}.pdf"
            val file=ReportDownloads.save(this@AdminReportsActivity,response,filename)
            status.text="Report downloaded. Choose a PDF app to view, share or print."
            ReportDownloads.open(this@AdminReportsActivity,file)
        }catch(error:Exception){if(error is CancellationException)throw error;status.text=errorMessage(error)}finally{setBusy(false)}}
    }
    private fun reviewPurchases(page:Int=1){
        val selection=filters()+mapOf("page" to page.toString());setBusy(true)
        lifecycleScope.launch {try{
            val response=api.purchaseReviews(selection)
            val records=response.getAsJsonArray("records").map{it.asJsonObject}
            val labels=records.map { "${string(it,"date")} · ${string(it,"item")} · GST ${money(it,"gst")}" }.toMutableList()
            if(response.get("has_next").asBoolean)labels.add("Next page")
            if(page>1)labels.add("Previous page")
            if(labels.isEmpty()){status.text="No purchases for this selection.";return@launch}
            WhiteMaterialAlertDialogBuilder(this@AdminReportsActivity).setTitle("Review purchase credit").setItems(labels.toTypedArray()){_,index->
                if(index<records.size)reviewPurchase(records[index])
                else if(labels[index]=="Next page")reviewPurchases(page+1)else reviewPurchases(page-1)
            }.setNegativeButton("Close",null).show()
        }catch(error:Exception){if(error is CancellationException)throw error;status.text=errorMessage(error)}finally{setBusy(false)}}
    }
    private fun reviewPurchase(record:JsonObject){
        val form=com.svd.svdagencies.utils.CompactLinearLayout(this).apply{orientation=LinearLayout.VERTICAL;setPadding(32,16,32,16)}
        val gstin=EditText(this).apply{hint="Supplier GSTIN";setText(string(record,"supplier_gstin"))}
        val reference=EditText(this).apply{hint="Supplier invoice number";setText(string(record,"supplier_invoice"))}
        val interstate=CheckBox(this).apply{text="Purchased from another state (IGST)";isChecked=record.get("gst_interstate").asBoolean}
        val confirmed=CheckBox(this).apply{text="Reviewed against supplier invoice and GSTR-2B; eligible for credit";isChecked=record.get("itc_confirmed").asBoolean}
        form.addView(gstin);form.addView(reference);form.addView(interstate);form.addView(confirmed)
        WhiteMaterialAlertDialogBuilder(this).setTitle(string(record,"item")).setView(form).setNegativeButton("Cancel",null).setPositiveButton("Save review"){_,_->
            val body=JsonObject().apply{addProperty("id",record.get("id").asInt);addProperty("supplier_gstin",gstin.text.toString());addProperty("supplier_invoice",reference.text.toString());addProperty("gst_interstate",interstate.isChecked);addProperty("itc_confirmed",confirmed.isChecked)}
            setBusy(true);lifecycleScope.launch{var saved=false;try{api.reviewPurchase(body);saved=true}catch(error:Exception){if(error is CancellationException)throw error;status.text=errorMessage(error)}finally{setBusy(false)};if(saved)load()}
        }.show()
    }
    private fun string(data:JsonObject,key:String):String=data.get(key)?.takeUnless{it.isJsonNull}?.asString.orEmpty()
    private fun money(data:JsonObject,key:String):String="₹"+String.format(Locale.US,"%,.2f",data.get(key)?.takeUnless{it.isJsonNull}?.asDouble?:0.0)
    private fun errorMessage(error:Exception):String=if(error is HttpException){"Unable to complete request (${error.code()}). "+(error.response()?.errorBody()?.string()?.take(400)?:"Try again.")}else error.message?:"Unable to connect. Please try again."
}
