package tv.wfc.livestreamsales.features.productorder.viewmodel

import androidx.lifecycle.LiveData
import tv.wfc.contentloader.viewmodel.INeedPreparationViewModel
import tv.wfc.livestreamsales.application.model.products.specification.Specification
import tv.wfc.livestreamsales.features.productorder.model.ProductBoxData
import tv.wfc.livestreamsales.application.model.orders.OrderedProduct
import tv.wfc.livestreamsales.features.productorder.model.SelectableSpecification

interface IProductOrderViewModel: INeedPreparationViewModel{
    val isAnyOperationInProgress: LiveData<Boolean>
    val productsCount: LiveData<Int>
    val productBoxesData: LiveData<List<ProductBoxData>>
    val currentProductGroupName: LiveData<String>
    val currentProductGroupImageUrl: LiveData<String?>
    val currentProductGroupSpecifications: LiveData<List<Specification<*>>>
    val currentSelectableSpecifications: LiveData<List<SelectableSpecification<*>>>
    val isProductSelected: LiveData<Boolean>
    val selectedProductPrice: LiveData<Float?>
    val selectedProductOldPrice: LiveData<Float?>
    val selectedProductAmount: LiveData<Int?>
    val isIncreasingOfSelectedProductAmountAllowed: LiveData<Boolean>
    val cart: LiveData<List<OrderedProduct>>
    val orderedProductsFinalPrice: LiveData<Float>
    val areProductsOrderedEvent: LiveData<Unit>
    fun prepareData(broadcastId: Long)
    fun selectProductGroupByPosition(position: Int)
    fun filter(specificationPosition: Int, valuePosition: Int)
    fun decreaseSelectedProductAmount()
    fun increaseSelectedProductAmount()
    fun deleteProductFromCart(productId: Long)
    fun orderProducts()
}