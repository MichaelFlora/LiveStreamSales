package tv.wfc.livestreamsales.features.orderediting.ui.orderediting.adapters.oderedproducts

import android.graphics.drawable.Drawable
import android.view.View
import androidx.recyclerview.widget.RecyclerView
import coil.ImageLoader
import coil.request.Disposable
import coil.request.ImageRequest
import com.laurus.p.tools.context.getDrawableCompat
import tv.wfc.livestreamsales.R
import tv.wfc.livestreamsales.application.model.orders.OrderedProduct
import tv.wfc.livestreamsales.databinding.ListItemOrderEditingOrderedProductBinding

class OrderedProductViewHolder(
    orderedProductView: View,
    private val imageLoader: ImageLoader
): RecyclerView.ViewHolder(orderedProductView) {
    private val viewBinding = ListItemOrderEditingOrderedProductBinding.bind(orderedProductView)
    private val context = orderedProductView.context

    private var imageLoaderDisposable: Disposable? = null

    fun bind(orderedProduct: OrderedProduct){
        clear()
        initializeProductImageView(orderedProduct)
        initializeProductsAmountLayout(orderedProduct)
        initializeProductsAmountText(orderedProduct)
        initializeProductNameText(orderedProduct)
        initializeProductPriceText(orderedProduct)
    }

    private fun clear(){
        clearProductImageView()
        clearProductsAmountLayout()
        clearProductsAmountText()
        clearProductNameText()
        clearProductPriceText()
    }

    private fun initializeProductImageView(orderedProduct: OrderedProduct){
        val productImageUrl = orderedProduct.product.image

        if(productImageUrl == null){
            setImageDefaultDrawable()
        } else{
            val imageRequest = ImageRequest.Builder(context)
                .data(productImageUrl)
                .target(
                    onError = { setImageDefaultDrawable() },
                    onSuccess = ::setImageDrawable
                )
                .build()

            imageLoaderDisposable?.dispose()
            imageLoaderDisposable = imageLoader.enqueue(imageRequest)
        }
    }

    private fun setImageDrawable(drawable: Drawable){
        viewBinding.productImageView.setImageDrawable(drawable)
    }

    private fun setImageDefaultDrawable(){
        viewBinding.productImageView.run{
            val placeholder = createImageDefaultDrawable()
            setImageDrawable(placeholder)
        }
    }

    private fun createImageDefaultDrawable(): Drawable?{
        return context.getDrawableCompat(
            R.drawable.ic_baseline_shopping_bag_24,
            R.color.color_black
        )
    }

    private fun clearProductImageView(){
        imageLoaderDisposable?.dispose()
        viewBinding.productImageView.setImageDrawable(null)
    }

    private fun initializeProductsAmountLayout(orderedProduct: OrderedProduct){
        val amount = orderedProduct.amount

        viewBinding.productsAmountLayout.run{
            visibility = if(amount <= 0) View.GONE else View.VISIBLE
        }
    }

    private fun clearProductsAmountLayout(){
        viewBinding.productsAmountLayout.visibility = View.GONE
    }

    private fun initializeProductsAmountText(orderedProduct: OrderedProduct){
        viewBinding.productsAmountText.run{
            visibility = if(orderedProduct.amount <= 0) View.GONE else View.VISIBLE
            text = orderedProduct.amount.toString()
        }
    }

    private fun clearProductsAmountText(){
        viewBinding.productsAmountText.run{
            visibility = View.GONE
            text = ""
        }
    }

    private fun initializeProductNameText(orderedProduct: OrderedProduct){
        viewBinding.productNameText.text = orderedProduct.product.name
    }

    private fun clearProductNameText(){
        viewBinding.productNameText.text = ""
    }

    private fun initializeProductPriceText(orderedProduct: OrderedProduct){
        viewBinding.productPriceText.text = context.getString(
            R.string.fragment_order_information_sum_text,
            orderedProduct.product.price
        )
    }

    private fun clearProductPriceText(){
        viewBinding.productPriceText.text = ""
    }
}