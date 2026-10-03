package com.ballooner.data.billing

import android.app.Activity
import android.content.Context
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.ConsumeParams
import com.android.billingclient.api.PendingPurchasesParams
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.queryProductDetails
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * The one-off tip behind the Community section's "Buy me a coffee".
 *
 * Play Billing needs a `Context` to talk to the store and an `Activity` to put its sheet over, so
 * it is kept here rather than in a ViewModel. The connection is opened once and left open for the
 * life of the process.
 */
@Singleton
class TipJar @Inject constructor(@ApplicationContext context: Context) : PurchasesUpdatedListener {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val _thanks = MutableSharedFlow<Unit>(extraBufferCapacity = 1)

    /** Emits once a tip has gone through, so the screen can thank the user. */
    val thanks: SharedFlow<Unit> = _thanks.asSharedFlow()

    private var coffee: ProductDetails? = null

    private val client = BillingClient.newBuilder(context)
        .setListener(this)
        .enablePendingPurchases(PendingPurchasesParams.newBuilder().enableOneTimeProducts().build())
        .build()

    init {
        connect()
    }

    /** Opens Play's purchase sheet. Does nothing until Play has said what the tip costs. */
    fun buyCoffee(activity: Activity) {
        val details = coffee ?: return
        val params = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(
                listOf(
                    BillingFlowParams.ProductDetailsParams.newBuilder()
                        .setProductDetails(details)
                        .build(),
                ),
            )
            .build()
        client.launchBillingFlow(activity, params)
    }

    private fun connect() {
        client.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(result: BillingResult) {
                if (result.responseCode == BillingClient.BillingResponseCode.OK) askWhatCoffeeCosts()
            }

            // The next purchase attempt reconnects; there is nothing useful to do here.
            override fun onBillingServiceDisconnected() = Unit
        })
    }

    private fun askWhatCoffeeCosts() {
        val params = QueryProductDetailsParams.newBuilder()
            .setProductList(
                listOf(
                    QueryProductDetailsParams.Product.newBuilder()
                        .setProductId(COFFEE_PRODUCT_ID)
                        .setProductType(BillingClient.ProductType.INAPP)
                        .build(),
                ),
            )
            .build()
        scope.launch {
            val result = client.queryProductDetails(params)
            if (result.billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                coffee = result.productDetailsList?.find { it.productId == COFFEE_PRODUCT_ID }
            }
        }
    }

    override fun onPurchasesUpdated(result: BillingResult, purchases: List<Purchase>?) {
        if (result.responseCode != BillingClient.BillingResponseCode.OK) return
        purchases.orEmpty()
            .filter { it.purchaseState == Purchase.PurchaseState.PURCHASED }
            .forEach(::consume)
    }

    // A tip is consumed rather than kept, so the same person can buy another coffee later.
    private fun consume(purchase: Purchase) {
        val params = ConsumeParams.newBuilder().setPurchaseToken(purchase.purchaseToken).build()
        client.consumeAsync(params) { result, _ ->
            if (result.responseCode == BillingClient.BillingResponseCode.OK) _thanks.tryEmit(Unit)
        }
    }
}

private const val COFFEE_PRODUCT_ID = "tip_coffee"
