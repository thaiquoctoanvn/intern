package io.edenx.convenience

import android.app.Activity
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.android.billingclient.api.*
import java.lang.ref.WeakReference
import java.util.*

class BillingUtil(
    context: Context
) {

    companion object {
        const val tag = "BillingUtil"
        private var instance: WeakReference<BillingUtil>?= null


    }

    var billingClient: BillingClient? = null
    private val receivers: WeakHashMap<String, (BillingResult, MutableList<Purchase>?) -> Unit> = WeakHashMap()

    init {
        initBillingClient(context)
    }

    private fun initBillingClient(
        context: Context
    ): BillingClient? {
        if (billingClient == null) {
            billingClient = BillingClient.newBuilder(context)
                .setListener { billingResult, purchases ->
                    receivers.forEach {
                        it.value(billingResult, purchases)
                    }
                }
                .enablePendingPurchases()
                .build()
        }
        return billingClient
    }

    fun connectToPlayStore(
        onConnected: (BillingResult) -> Unit = {},
        onDisconnected: () -> Unit = {}
    ) {
        billingClient?.startConnection(object : BillingClientStateListener {
            override fun onBillingServiceDisconnected() {
                Log.d(tag, "onBillingServiceDisconnected")
                onDisconnected()
            }

            override fun onBillingSetupFinished(billingResult: BillingResult) {
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    Log.d(tag, "onBillingSetupFinished")
                    onConnected(billingResult)
                }
            }
        })
    }

    fun queryProductDetails(
        productIdAndTypeList: Map<String, String>,
        onQueryResponded: (BillingResult, List<ProductDetails>) -> Unit
    ) {
        val queryProductDetailsParams =
            QueryProductDetailsParams.newBuilder()
                .setProductList(productIdAndTypeList.map {
                    QueryProductDetailsParams.Product.newBuilder()
                        .setProductId(it.key)
                        .setProductType(it.value)
                        .build()
                })
                .build()
        billingClient?.queryProductDetailsAsync(queryProductDetailsParams) { billingResult, productDetailsList ->
            Log.d(tag, "queryProductDetailsAsync: ${billingResult.responseCode}")
            Handler(Looper.getMainLooper()).post {
                onQueryResponded(billingResult, productDetailsList)
            }
        }
    }

    fun queryPurchases(
        productType: String,
        onQueryResponded: (BillingResult, List<Purchase>) -> Unit
    ) {
        billingClient?.queryPurchasesAsync(
            QueryPurchasesParams.newBuilder().setProductType(productType).build()
        ) { billingResult, purchaseList ->
            Handler(Looper.getMainLooper()).post {
                onQueryResponded(billingResult, purchaseList)
            }
        }
    }

    fun launchPurchaseFlow(activity: Activity, productDetails: ProductDetails, oldPurchase: Purchase?): BillingResult? {
        val productDetailsParamsList = listOf(
            BillingFlowParams.ProductDetailsParams.newBuilder()
                .setProductDetails(productDetails)
                .setOfferToken(productDetails.subscriptionOfferDetails?.first()?.offerToken ?: "")
                .build()
        )
        val billingFlowParams = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(productDetailsParamsList)
        oldPurchase?.let {
            val subscriptionUpdateParams = BillingFlowParams.SubscriptionUpdateParams.newBuilder()
                .setOldPurchaseToken(oldPurchase.purchaseToken)
                .setSubscriptionReplacementMode(BillingFlowParams.SubscriptionUpdateParams.ReplacementMode.CHARGE_FULL_PRICE)
                .build()
            billingFlowParams.setSubscriptionUpdateParams(subscriptionUpdateParams)
        }

        return billingClient?.launchBillingFlow(activity, billingFlowParams.build())
    }

    // For subs
    fun acknowledgePurchase(
        purchase: Purchase,
        onPurchaseSucceed: (BillingResult) -> Unit = {},
        onPurchaseFailed: (BillingResult) -> Unit = {},
        onNotPurchasedYet: (Purchase) -> Unit = {},
        onAcknowledged: (Purchase) -> Unit = {}
    ) {
        if (purchase.purchaseState == Purchase.PurchaseState.PURCHASED) {
            if (!purchase.isAcknowledged) {
                val acknowledgePurchaseParams = AcknowledgePurchaseParams.newBuilder()
                    .setPurchaseToken(purchase.purchaseToken)
                    .build()
                billingClient?.acknowledgePurchase(acknowledgePurchaseParams) {
                    if (it.responseCode == BillingClient.BillingResponseCode.OK) onPurchaseSucceed(
                        it
                    )
                    else onPurchaseFailed(it)
                }
            } else onAcknowledged(purchase)
        } else onNotPurchasedYet(purchase)
    }

    fun registerPurchasesUpdatedCallback(tag: String, callback: (BillingResult, MutableList<Purchase>?) -> Unit) {
        if (receivers.contains(tag)) receivers.replace(tag, callback)
        else receivers[tag] = callback
    }

    fun unregisterPurchasesUpdatedCallback(tag: String) {
        receivers.remove(tag)
    }
}