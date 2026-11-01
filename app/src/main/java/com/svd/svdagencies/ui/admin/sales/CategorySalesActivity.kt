package com.svd.svdagencies.ui.admin.sales

import android.os.Bundle
import android.graphics.Color
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.view.View
import android.widget.*
import com.google.android.material.card.MaterialCardView
import com.google.android.material.button.MaterialButton
import com.svd.svdagencies.R
import com.svd.svdagencies.ui.admin.AdminBaseActivity
import com.svd.svdagencies.data.api.admin.*
import com.svd.svdagencies.data.api.auth.ApiClient
import retrofit2.*
import java.text.NumberFormat
import java.util.Locale

class CategorySalesActivity : AdminBaseActivity() {
    private lateinit var content:LinearLayout
    private lateinit var results:LinearLayout
    private lateinit var period:Spinner
    private lateinit var customer:Spinner
    private lateinit var range:TextView
    private lateinit var refresh:androidx.swiperefreshlayout.widget.SwipeRefreshLayout
    private var periods=listOf("overall","today","week","month","year")
    private var customerIds=listOf<Int?>(null)
    private var customerId:Int?=null
    private var selectedPeriod="overall"
    private var call:Call<CategorySalesResponse>?=null
    private val ink=Color.rgb(24,59,71)
    private val muted=Color.rgb(107,127,137)
    private val currency=NumberFormat.getCurrencyInstance(Locale("en","IN"))
    override fun onCreate(state:Bundle?) {
        super.onCreate(state);setContentView(R.layout.admin_simple_dashboard);setupAdminLayout("Category sales")
        content=findViewById(R.id.contentContainer);content.setBackgroundColor(Color.WHITE)
        refresh=findViewById(R.id.swipeRefresh)
        selectedPeriod=state?.getString("period")?:"overall"
        customerId=state?.getInt("customer")?.takeIf{it>0}
        range=text("All time",12f)
        content.addView(range)
        val filters=column()
        val row=LinearLayout(this).apply{orientation=LinearLayout.HORIZONTAL;gravity=android.view.Gravity.CENTER_VERTICAL;tag="svd-fixed-row"}
        period=Spinner(this).apply{tag="svd-full-width";contentDescription="Period";adapter=ArrayAdapter(this@CategorySalesActivity,R.layout.delivery_dropdown_item,listOf("All time","Today","This week","This month","This year"));setSelection(periods.indexOf(selectedPeriod).coerceAtLeast(0))}
        customer=Spinner(this).apply{tag="svd-full-width";contentDescription="Customer";adapter=ArrayAdapter(this@CategorySalesActivity,R.layout.delivery_dropdown_item,listOf("All customers"))}
        row.addView(field("Period",period),LinearLayout.LayoutParams(0,-2,1f).apply{marginEnd=dp(8)})
        row.addView(field("Customer",customer),LinearLayout.LayoutParams(0,-2,1f))
        val reset=MaterialButton(this).apply{text="";contentDescription="Clear filters";setIconResource(R.drawable.ic_refresh);iconPadding=0;setOnClickListener{selectedPeriod="overall";customerId=null;period.setSelection(0);customer.setSelection(0);load()}}
        filters.addView(row);filters.addView(reset,LinearLayout.LayoutParams(dp(32),dp(32)).apply{gravity=android.view.Gravity.END;topMargin=dp(8)})
        content.addView(card(filters));results=column();content.addView(results)
        period.onItemSelectedListener=listener{val next=periods[period.selectedItemPosition];if(next!=selectedPeriod){selectedPeriod=next;load()}}
        customer.onItemSelectedListener=listener{val next=customerIds.getOrNull(customer.selectedItemPosition);if(next!=customerId){customerId=next;load()}}
        refresh.setOnRefreshListener{load()};load()
    }
    private fun listener(action:()->Unit)=object:AdapterView.OnItemSelectedListener{
        override fun onNothingSelected(p:AdapterView<*>?){}
        override fun onItemSelected(p:AdapterView<*>?,v:View?,position:Int,id:Long){action()}
    }
    private fun load(){
        call?.cancel();refresh.isRefreshing=true
        val request=ApiClient.categorySalesApi.getSummary(period=selectedPeriod,customer=customerId);call=request
        request.enqueue(object:Callback<CategorySalesResponse>{
            override fun onResponse(c:Call<CategorySalesResponse>,r:Response<CategorySalesResponse>){
                if(c!==call)return
                refresh.isRefreshing=false
                val data=r.body();if(!r.isSuccessful||data==null){failure();return}
                val ids=listOf<Int?>(null)+data.customers.map{it.id}
                if(ids!=customerIds){customerIds=ids;customer.adapter=ArrayAdapter(this@CategorySalesActivity,R.layout.delivery_dropdown_item,listOf("All customers")+data.customers.map{it.name});customer.setSelection(ids.indexOf(customerId).coerceAtLeast(0))}
                range.text=if(selectedPeriod=="overall")"All time" else "${data.start_date.orEmpty()} – ${data.end_date.orEmpty()}"
                render(data)
            }
            override fun onFailure(c:Call<CategorySalesResponse>,t:Throwable){if(c!==call||c.isCanceled)return;refresh.isRefreshing=false;failure()}
        })
    }
    private fun render(data:CategorySalesResponse){
        results.removeAllViews()
        val names=(data.categories+data.amount_by_category.keys+data.data_by_category.keys).distinct()
        val metrics=listOf("Sales amount" to currency.format(data.amount_by_category.values.sum()),"Sales volume" to litres(data.total_volume),
            (if(selectedPeriod=="overall")"Categories with sales" else "Previous period volume") to (if(selectedPeriod=="overall")names.count{(data.amount_by_category[it]?:0.0)!=0.0}.toString() else litres(data.total_compare_volume)),
            (if(selectedPeriod=="overall")"Top category by sales" else "Volume change") to (if(selectedPeriod=="overall")data.amount_by_category.maxByOrNull{it.value}?.key?:"—" else "${litres(data.diff_volume)} · ${data.diff_percent?.let{String.format(Locale.US,"%.2f%%",it)}?:"—"}"))
        val grid=GridLayout(this).apply{columnCount=2}
        metrics.forEachIndexed{i,pair->val body=column();body.addView(text(pair.first,12f));body.addView(text(pair.second,20f,true));val c=card(body);if(i==0){c.setCardBackgroundColor(ink);for(n in 0 until body.childCount)(body.getChildAt(n) as? TextView)?.setTextColor(Color.WHITE)};grid.addView(c,GridLayout.LayoutParams().apply{width=0;height=-2;columnSpec=GridLayout.spec(GridLayout.UNDEFINED,1f);setMargins(dp(4),dp(4),dp(4),dp(4))})}
        results.addView(grid)
        val trend=column();trend.addView(text("Volume trend",16f,true));trend.addView(text("Last 12 months · L",12f))
        val series=data.trend_by_category.map{(name,points)->name to points.map{it.volume}}
        trend.addView(VolumeChart(series,false),LinearLayout.LayoutParams(-1,dp(220)))
        series.forEachIndexed{i,p->trend.addView(text("● ${p.first}",11f).apply{setTextColor(chartColors[i%chartColors.size])})}
        results.addView(card(trend))
        val yearly=column();yearly.addView(text("Yearly volume",16f,true));yearly.addView(text("All years · L",12f));yearly.addView(VolumeChart(data.year_totals.map{it.key to listOf(it.value)},true),LinearLayout.LayoutParams(-1,dp(220)));results.addView(card(yearly))
        val tableBody=column();tableBody.addView(text("Category breakdown",16f,true))
        val table=TableLayout(this)
        val heads=mutableListOf("Category","Volume (L)");if(selectedPeriod!="overall")heads.add("Previous volume (L)");heads.add("Sales amount")
        fun row(values:List<String>,bold:Boolean=false){table.addView(TableRow(this).apply{values.forEach{value->addView(text(value,13f,bold).apply{minWidth=dp(125);setPadding(dp(10),dp(12),dp(10),dp(12));if(bold)setBackgroundColor(Color.rgb(241,246,247))})}})}
        row(heads,true)
        names.forEach{name->val values=mutableListOf(name,number(data.data_by_category[name]?:0.0));if(selectedPeriod!="overall")values.add(number(data.compare_data_by_category[name]?:0.0));values.add(currency.format(data.amount_by_category[name]?:0.0));row(values)}
        val total=mutableListOf("Total",number(data.total_volume));if(selectedPeriod!="overall")total.add(number(data.total_compare_volume));total.add(currency.format(data.amount_by_category.values.sum()));row(total,true)
        if(names.isEmpty())tableBody.addView(text("No sales in this period.",13f))
        tableBody.addView(HorizontalScrollView(this).apply{addView(table)});results.addView(card(tableBody))
    }
    private val chartColors=intArrayOf(Color.rgb(8,127,140),Color.rgb(65,105,180),Color.rgb(230,155,45),Color.rgb(135,90,160),Color.rgb(65,155,100))
    private inner class VolumeChart(val series:List<Pair<String,List<Double>>>,val bars:Boolean):View(this@CategorySalesActivity){
        override fun onDraw(c:Canvas){super.onDraw(c);val p=Paint(Paint.ANTI_ALIAS_FLAG);val left=dp(44).toFloat();val right=width-dp(12).toFloat();val top=dp(12).toFloat();val bottom=height-dp(30).toFloat();val max=(series.flatMap{it.second}.maxOrNull()?:0.0).coerceAtLeast(1.0)
            for(i in 0..4){val y=bottom-(bottom-top)*i/4;p.color=Color.LTGRAY;p.strokeWidth=1f;c.drawLine(left,y,right,y,p);p.color=muted;p.textSize=dp(9).toFloat();c.drawText(String.format(Locale.US,"%.0f",max*i/4),0f,y,p)}
            series.forEachIndexed{index,entry->p.color=chartColors[index%chartColors.size];if(bars){val slot=(right-left)/series.size.coerceAtLeast(1);val x=left+slot*index;val y=bottom-((entry.second.firstOrNull()?:0.0)/max*(bottom-top)).toFloat();c.drawRect(x+slot*.2f,y,x+slot*.8f,bottom,p);p.textSize=dp(10).toFloat();c.drawText(entry.first,x+slot*.15f,bottom+dp(18),p)}else{p.style=Paint.Style.STROKE;p.strokeWidth=dp(2).toFloat();val path=Path();entry.second.forEachIndexed{i,v->val x=left+(right-left)*i/(entry.second.size-1).coerceAtLeast(1);val y=bottom-(v/max*(bottom-top)).toFloat();if(i==0)path.moveTo(x,y)else path.lineTo(x,y)};c.drawPath(path,p);p.style=Paint.Style.FILL}}
        }
    }
    private fun failure(){results.removeAllViews();results.addView(text("Could not load category sales. Pull down to retry.",14f))}
    private fun column()=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL}
    private fun field(title:String,view:View)=column().apply{addView(text(title,12f));addView(view,LinearLayout.LayoutParams(-1,-2))}
    private fun text(value:String,size:Float,bold:Boolean=false)=TextView(this).apply{text=value;textSize=size;setTextColor(if(bold)ink else muted);setPadding(0,dp(4),0,dp(4));if(bold)setTypeface(typeface,android.graphics.Typeface.BOLD)}
    private fun card(body:LinearLayout)=MaterialCardView(this).apply{radius=dp(12).toFloat();cardElevation=0f;strokeWidth=dp(1);strokeColor=Color.rgb(223,232,236);setCardBackgroundColor(Color.WHITE);body.setPadding(dp(14),dp(12),dp(14),dp(12));addView(body);layoutParams=LinearLayout.LayoutParams(-1,-2).apply{topMargin=dp(12)}}
    private fun number(v:Double)=String.format(Locale.US,"%,.3f",v)
    private fun litres(v:Double)=number(v)+" L"
    private fun dp(v:Int)=(v*resources.displayMetrics.density+.5f).toInt()
    override fun onSaveInstanceState(out:Bundle){out.putString("period",selectedPeriod);customerId?.let{out.putInt("customer",it)};super.onSaveInstanceState(out)}
    override fun onDestroy(){call?.cancel();super.onDestroy()}
}
