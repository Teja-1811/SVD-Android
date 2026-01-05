package com.svd.svdagencies.ui.customer.fragment

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.svd.svdagencies.R
import com.svd.svdagencies.data.api.auth.ApiClient
import com.svd.svdagencies.data.api.customer.CustomerApi
import com.svd.svdagencies.data.model.customer.CustomerDashboardResponse
import com.svd.svdagencies.utils.Refreshable
import com.svd.svdagencies.utils.SessionManager
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class CustomerHomeFragment :
    Fragment(R.layout.fragment_customer_home),
    Refreshable {

    private lateinit var swipeRefresh: SwipeRefreshLayout
    private lateinit var tvWelcome: TextView
    private lateinit var tvBalance: TextView
    private lateinit var tvShop: TextView
    private lateinit var tvStatus: TextView

    private lateinit var api: CustomerApi
    private lateinit var session: SessionManager
    private var userId: Int = -1

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Views
        // Note: The SwipeRefreshLayout is in the Activity layout (activity_customer_main.xml), 
        // but this Fragment implements Refreshable, which is called by the Activity.
        // However, if we want to control the refreshing state (stop the spinner), we need to access it 
        // or have the activity handle it.
        // Looking at CustomerMainActivity, it calls fragment.onRefresh() and then immediately sets isRefreshing = false.
        // This is why the spinner disappears immediately or behaves incorrectly.
        // The Activity should NOT set isRefreshing = false immediately. It should be the responsibility of the fragment 
        // to signal when it's done, OR the fragment should access the SwipeRefreshLayout if possible, 
        // OR the onRefresh method should return a Future/Callback.
        
        // But wait, the user says "the spinner is continuously shows loading". 
        // This likely means isRefreshing is set to true and never set to false, OR it is set to true 
        // and the network call hangs.
        
        // Let's look at CustomerMainActivity again.
        // swipeRefresh.setOnRefreshListener {
        //     val fragment = supportFragmentManager.findFragmentById(R.id.customerFragmentContainer)
        //     if (fragment is Refreshable) {
        //         fragment.onRefresh()
        //     }
        //     swipeRefresh.isRefreshing = false // <--- This runs IMMEDIATELY after calling onRefresh
        // }
        
        // If the user says "continuously shows loading", maybe they are running code that sets it to true and never false?
        // Or maybe my previous analysis of "CustomerMainActivity" was wrong?
        
        // In CustomerMainActivity:
        // swipeRefresh.setOnRefreshListener {
        //    val fragment = ...
        //    if (fragment is Refreshable) fragment.onRefresh()
        //    swipeRefresh.isRefreshing = false
        // }
        // This would make the spinner disappear immediately, not "continuously show loading".
        
        // However, if the user added `swipeRefresh.isRefreshing = true` inside the fragment's `loadDashboard` 
        // (like I did in other fragments), but the SwipeRefreshLayout is in the Activity, 
        // how does the fragment access it?
        
        // In CustomerBillsFragment, there is a SwipeRefreshLayout inside the fragment layout.
        // In CustomerOrdersFragment, I just added one.
        // In CustomerPaymentFragment, I just added one.
        
        // In CustomerHomeFragment, there is NO SwipeRefreshLayout in the fragment layout (fragment_customer_home.xml is just a ScrollView).
        // It relies on the one in CustomerMainActivity.
        
        // If the one in CustomerMainActivity is the one spinning, then:
        // 1. User pulls down.
        // 2. setOnRefreshListener triggers.
        // 3. Calls fragment.onRefresh().
        // 4. Sets isRefreshing = false.
        
        // So it should disappear immediately.
        
        // If the user says "continuously shows loading", maybe they changed the Activity code or I am misinterpreting.
        
        // Wait, I see `CustomerPaymentFragment` and `CustomerOrdersFragment` and `CustomerBillsFragment` have their OWN SwipeRefreshLayout now.
        // `CustomerHomeFragment` DOES NOT. It uses the one in the Activity.
        
        // If I am on `CustomerHomeFragment`, the Activity's SwipeRefreshLayout is used.
        // The Activity implementation is:
        /*
        swipeRefresh.setOnRefreshListener {
            val fragment = ...
            if (fragment is Refreshable) {
                fragment.onRefresh()
            }
            swipeRefresh.isRefreshing = false
        }
        */
        // This logic is flawed because `onRefresh` is usually async (network call). 
        // If it returns immediately, `isRefreshing = false` executes immediately, hiding the spinner before data loads.
        // BUT the user says "continuously shows loading". This is the opposite problem.
        
        // Maybe the user is talking about a different screen?
        // "when i use the drag down to refresh, the spinner is continuesly shows loding"
        
        // If I look at `CustomerBillsFragment`:
        /*
        private fun loadBills() {
            tvStatus.text = "Loading bills..."
            swipeRefresh.isRefreshing = true  <-- SETS TRUE
            
            api.getCustomerInvoices(...).enqueue(object : Callback... {
                onResponse {
                    swipeRefresh.isRefreshing = false <-- SETS FALSE
                }
                onFailure {
                    swipeRefresh.isRefreshing = false <-- SETS FALSE
                }
            })
        }
        */
        // This looks correct.
        
        // If I look at `CustomerOrdersFragment` (which I just edited):
        /*
        private fun loadCategories() {
            swipeRefresh.isRefreshing = true
            api.getCategories()... {
               onResponse {
                   if (categories.isNullOrEmpty()) {
                       swipeRefresh.isRefreshing = false
                       return
                   }
                   // ... loadProducts ...
               }
               onFailure { swipeRefresh.isRefreshing = false }
            }
        }
        
        private fun loadProducts(categoryId: Int) {
            swipeRefresh.isRefreshing = true // <-- Sets true again?
            api.getProducts... {
                onResponse { swipeRefresh.isRefreshing = false }
                onFailure { swipeRefresh.isRefreshing = false }
            }
        }
        */
        // If loadCategories succeeds, it calls loadProducts.
        // loadCategories does NOT set false if categories exist. 
        // It proceeds to call loadProducts.
        // loadProducts sets true (redundant but okay) and then sets false when done.
        // This looks okay, assuming `loadProducts` is called.
        
        // What about `CustomerHomeFragment`?
        // It relies on Activity's SwipeRefreshLayout.
        // If the Activity sets `isRefreshing = false` immediately, it just stops spinning.
        
        // If the user modified `CustomerMainActivity` to NOT set false immediately, and expects the fragment to do it?
        // But `CustomerHomeFragment` does not have access to the Activity's SwipeRefreshLayout easily, nor does it try to access it currently.
        
        // Let's fix `CustomerMainActivity` to handle this properly.
        // And also `CustomerHomeFragment`.
        
        // The better approach for `CustomerHomeFragment` since it doesn't have its own SwipeRefreshLayout is to:
        // 1. Add SwipeRefreshLayout to `fragment_customer_home.xml`.
        // 2. Remove SwipeRefreshLayout from `activity_customer_main.xml` (or disable it/make it only for Home if we want, but usually it's better per fragment).
        // 3. Since other fragments (`CustomerBillsFragment`, `CustomerOrdersFragment`, `CustomerPaymentFragment`) now have their OWN SwipeRefreshLayout, having one in the Activity wrapping the fragment container is redundant and problematic (nested SwipeRefreshLayouts).
        
        // If `activity_customer_main.xml` has a SwipeRefreshLayout wrapping the container, and `CustomerBillsFragment` has one inside the container, gestures might conflict or be confusing.
        // Usually, you don't wrap the FragmentContainerView in a SwipeRefreshLayout if individual fragments have their own.
        
        // The plan:
        // 1. Modify `fragment_customer_home.xml` to wrap content in `SwipeRefreshLayout`.
        // 2. Modify `CustomerHomeFragment.kt` to handle the refresh logic using its own SwipeRefreshLayout.
        // 3. Modify `activity_customer_main.xml` to REMOVE the `SwipeRefreshLayout`.
        // 4. Modify `CustomerMainActivity.kt` to remove the SwipeRefreshLayout logic.
        
        // This resolves the ambiguity and "continuous loading" (if it was caused by conflicting layouts or activity logic) and ensures data reload feedback is accurate per screen.
        
        tvWelcome = view.findViewById(R.id.tvWelcome)
        tvBalance = view.findViewById(R.id.tvBalance)
        tvShop = view.findViewById(R.id.tvShop)
        tvStatus = view.findViewById(R.id.tvStatus)
        swipeRefresh = view.findViewById(R.id.swipeRefresh)

        // Session
        session = SessionManager(requireContext())
        userId = session.getUserId()

        if (userId == -1) {
            Toast.makeText(requireContext(), "Session expired", Toast.LENGTH_SHORT).show()
            return
        }

        // API
        api = ApiClient.retrofit.create(CustomerApi::class.java)
        
        swipeRefresh.setOnRefreshListener {
            loadDashboard()
        }

        // Initial load
        // loadDashboard() // We can load, or let the user pull. Usually auto-load is good.
        // Let's auto-load but show spinner.
        swipeRefresh.post {
            swipeRefresh.isRefreshing = true
            loadDashboard()
        }
    }

    // ================= LOAD DASHBOARD =================
    private fun loadDashboard() {

        api.getDashboard(userId).enqueue(object : Callback<CustomerDashboardResponse> {

            override fun onResponse(
                call: Call<CustomerDashboardResponse>,
                response: Response<CustomerDashboardResponse>
            ) {
                swipeRefresh.isRefreshing = false
                
                if (!response.isSuccessful) {
                    Toast.makeText(
                        requireContext(),
                        "Server error: ${response.code()}",
                        Toast.LENGTH_LONG
                    ).show()
                    return
                }

                Log.d("CustomerHomeFragment", "Response: ${response.body()}")

                val customer = response.body() ?: return

                tvWelcome.text = "Welcome back, ${customer.customerName} 👋"
                tvShop.text = customer.shopName
                tvBalance.text = "₹ ${customer.balance}"

                if (customer.accountStatus == "Active") {
                    tvStatus.text = "Active"
                    tvStatus.setTextColor(
                        resources.getColor(R.color.icon_green, null)
                    )
                } else {
                    tvStatus.text = "Inactive"
                    tvStatus.setTextColor(
                        resources.getColor(R.color.login_text_red, null)
                    )
                }
            }

            override fun onFailure(call: Call<CustomerDashboardResponse>, t: Throwable) {
                swipeRefresh.isRefreshing = false
                Toast.makeText(
                    requireContext(),
                    "Network error: ${t.localizedMessage}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        })
    }

    // ================= PULL TO REFRESH =================
    override fun onRefresh() {
        // This method might be called by Activity if we didn't remove the listener there.
        // But we plan to remove it.
        // If we keep it for backward compatibility or if the interface requires it:
        loadDashboard()
    }
}
