plugins {
    alias(libs.plugins.android.aipack)
}

aiPack {
    packName = "model_pack"
    dynamicDelivery {
        deliveryType = "on-demand"
    }
}