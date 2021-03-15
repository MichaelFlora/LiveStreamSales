package tv.wfc.livestreamsales.features.productorder.viewmodel

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Scheduler
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.disposables.Disposable
import io.reactivex.rxjava3.kotlin.addTo
import io.reactivex.rxjava3.kotlin.subscribeBy
import tv.wfc.contentloader.model.ViewModelPreparationState
import tv.wfc.livestreamsales.application.di.modules.reactivex.qualifiers.MainThreadScheduler
import tv.wfc.livestreamsales.application.model.products.Product
import tv.wfc.livestreamsales.application.model.products.ProductGroup
import tv.wfc.livestreamsales.features.productorder.model.ProductInCart
import tv.wfc.livestreamsales.application.model.products.ProductVariant
import tv.wfc.livestreamsales.application.model.products.specification.Specification
import tv.wfc.livestreamsales.application.repository.products.IProductsRepository
import tv.wfc.livestreamsales.application.tools.errors.IApplicationErrorsLogger
import tv.wfc.livestreamsales.features.productorder.model.ProductBoxData
import tv.wfc.livestreamsales.features.productorder.model.SelectableSpecification
import javax.inject.Inject

class ProductOrderViewModel @Inject constructor(
    private val productsRepository: IProductsRepository,
    @MainThreadScheduler
    private val mainThreadScheduler: Scheduler,
    private val applicationErrorsLogger: IApplicationErrorsLogger
): ViewModel(), IProductOrderViewModel {
    private val disposables = CompositeDisposable()

    private val currentProductVariants = mutableSetOf<ProductVariant>()

    private val selectableSpecificationsByProductGroup = mutableMapOf<ProductGroup, MutableList<SelectableSpecification<*>>>()

    private val mutableCart = mutableListOf<ProductInCart>()

    private var dataPreparationDisposable: Disposable? = null

    private var selectedProductGroup: ProductGroup? = null

    private var selectedProduct: Product? = null

    private lateinit var productGroups: List<ProductGroup>

    override val dataPreparationState = MutableLiveData<ViewModelPreparationState>()

    override val productsCount = MutableLiveData<Int>()

    override val productBoxesData = MutableLiveData<List<ProductBoxData>>()

    override val currentProductGroupName = MutableLiveData<String>()

    override val currentProductGroupImageUrl = MutableLiveData<String?>()

    override val currentProductGroupSpecifications = MutableLiveData<List<Specification<*>>>()

    override val currentSelectableSpecifications = MutableLiveData<List<SelectableSpecification<*>>>()

    override val isProductSelected = MutableLiveData(false)
    override val selectedProductPrice = MutableLiveData<Float?>()
    override val selectedProductOldPrice = MutableLiveData<Float?>()
    override val selectedProductAmount = MutableLiveData<Int?>()
    override val cart = MutableLiveData<List<ProductInCart>>()

    @Synchronized
    override fun prepareData(broadcastId: Long){
        if(dataPreparationState.value != ViewModelPreparationState.DataIsNotPrepared &&
            dataPreparationState.value != null) return

        dataPreparationDisposable?.dispose()

        dataPreparationDisposable = Completable
            .mergeArray(
                prepareProductsInformation(broadcastId)
            )
            .observeOn(mainThreadScheduler)
            .doOnSubscribe { dataPreparationState.value = ViewModelPreparationState.DataIsBeingPrepared }
            .doOnError(applicationErrorsLogger::logError)
            .subscribeBy(
                onComplete = {
                    dataPreparationState.value = ViewModelPreparationState.DataIsPrepared
                },
                onError = {
                    dataPreparationState.value = ViewModelPreparationState.FailedToPrepareData()
                }
            )
            .addTo(disposables)

    }

    @Synchronized
    override fun selectProductGroupByPosition(position: Int) {
        val selectedProduct = productGroups.getOrNull(position) ?: return

        selectProductGroup(selectedProduct)
    }

    @Synchronized
    override fun filter(specificationPosition: Int, valuePosition: Int) {
        val selectedProductGroup = this.selectedProductGroup ?: return
        val selectableSpecifications = selectableSpecificationsByProductGroup[selectedProductGroup] ?: return

        if(selectedProduct != null) deselectProductVariant()

        for(i in selectableSpecifications.lastIndex downTo specificationPosition + 1){
            selectableSpecifications.removeAt(i)
        }

        val selectableSpecification = selectableSpecifications
            .elementAtOrNull(specificationPosition)
            ?.apply { selectValue(valuePosition) } ?: return

        if(selectableSpecification.selectedValue != null){
            val filteredProductVariants = currentProductVariants.filter(selectableSpecifications)

            val nextSelectableSpecification = createNextSelectableSpecification(
                selectableSpecifications,
                filteredProductVariants
            )

            if(nextSelectableSpecification == null){
                val filteredProductVariant = filteredProductVariants.firstOrNull()
                if(filteredProductVariant == null){
                    deselectProductVariant()
                } else{
                    Product.create(selectedProductGroup, filteredProductVariant.id)?.let{
                        selectProduct(it)
                    }
                }
            } else{
                selectableSpecifications.add(nextSelectableSpecification)
            }
        }

        this.currentSelectableSpecifications.value = selectableSpecifications
    }

    @JvmName("filterProductVariantBySelectableSpecifications")
    private fun Collection<ProductVariant>.filter(
        selectableSpecifications: Collection<SelectableSpecification<*>>
    ): List<ProductVariant>{
        val selectedSpecifications = selectableSpecifications.toSpecifications()

        return filter(selectedSpecifications)
    }

    @JvmName("filterProductVariantBySpecifications")
    private fun Collection<ProductVariant>.filter(
        specifications: Collection<Specification<*>>
    ): List<ProductVariant>{
        return filter filterProductVariants@{ productVariant ->
            specifications.forEach { selectedSpecification ->
                if(!productVariant.specifications.contains(selectedSpecification)) return@filterProductVariants false
            }

            return@filterProductVariants true
        }
    }

    @Synchronized
    override fun decreaseSelectedProductAmount() {
        val product = selectedProduct ?: return

        val productInCart = removeProductFromCart(product)

        selectedProductAmount.value = productInCart?.amount ?: 0
    }

    @Synchronized
    override fun increaseSelectedProductAmount() {
        val product = selectedProduct ?: return

        val productInCart = addProductToCart(product)

        selectedProductAmount.value = productInCart.amount
    }

    @Synchronized
    override fun deleteProductFromCart(productId: Long) {
        mutableCart.find { it.product.id == productId }?.let{ productInCart ->
            removeProductFromCart(productInCart)

            val selectedProductVariant = this.selectedProduct

            if(selectedProductVariant?.id == productId){
                selectProduct(selectedProductVariant)
            }
        }
    }

    private fun selectProduct(product: Product){
        selectedProduct = product
        isProductSelected.value = true
        selectedProductPrice.value = product.price
        selectedProductOldPrice.value = product.oldPrice
        updateSelectedProductAmount(product)
    }

    private fun updateSelectedProductAmount(product: Product){
        val productInCart = mutableCart.find { it.product == product }

        if(productInCart != null){
            selectedProductAmount.value = productInCart.amount
            return
        }

        selectedProductAmount.value = 0
    }

    private fun deselectProductVariant(){
        selectedProduct = null
        isProductSelected.value = false

        selectedProductPrice.value = null
        selectedProductOldPrice.value = null
        selectedProductAmount.value = null
    }

    private fun prepareProductsInformation(broadcastId: Long): Completable{
        return getProductGroupsFromRemote(broadcastId)
            .observeOn(mainThreadScheduler)
            .flatMapCompletable { products ->
                Completable.mergeArray(
                    prepareProductsData(products)
                )
            }
    }

    private fun getProductGroupsFromRemote(broadcastId: Long): Single<List<ProductGroup>>{
        return productsRepository
            .getProducts(broadcastId)
            .lastOrError()
            .filter{ it.isNotEmpty() }
            .toSingle()
    }

    private fun prepareProductsData(productGroups: List<ProductGroup>): Completable{
        return Completable.fromRunnable {
            updateProductGroups(productGroups)
        }
    }

    @Synchronized
    private fun updateProductGroups(newProductGroups: List<ProductGroup>){
        this.productGroups = newProductGroups
        productsCount.value = productGroups.size
        updateProductBoxesData(productGroups)
        val groupToSelect = productGroups.getOrNull(0) ?: return
        selectProductGroup(groupToSelect)
    }

    @Synchronized
    private fun selectProductGroup(newProductGroup: ProductGroup){
        this.selectedProductGroup = newProductGroup

        currentProductGroupName.value = newProductGroup.name
        currentProductGroupImageUrl.value = newProductGroup.image
        currentProductGroupSpecifications.value = newProductGroup.specifications

        deselectProductVariant()
        updateCurrentProductVariants(newProductGroup)
        changeSelectableSpecifications(newProductGroup)
    }

    @Synchronized
    private fun updateCurrentProductVariants(productGroup: ProductGroup){
        val newProductVariants = productGroup.productVariants

        currentProductVariants.clear()
        currentProductVariants.addAll(newProductVariants)
    }

    private fun updateProductBoxesData(productGroups: List<ProductGroup>){
        val productBoxesData = mutableListOf<ProductBoxData>()

        productGroups.forEachIndexed { productPosition, product ->
            val productImageUrl = product.image
            val productCountInStock = product.countInStock()

            productBoxesData.add(
                ProductBoxData(
                    productPosition,
                    productImageUrl,
                    productCountInStock
                )
            )
        }

        this.productBoxesData.value = productBoxesData
    }

    @Synchronized
    private fun changeSelectableSpecifications(productGroup: ProductGroup){
        val doesSelectableSpecificationsExist = selectableSpecificationsByProductGroup.containsKey(productGroup)

        if(!doesSelectableSpecificationsExist){
            val newSelectableSpecifications = mutableListOf<SelectableSpecification<*>>()
            selectableSpecificationsByProductGroup[productGroup] = newSelectableSpecifications

            createNextSelectableSpecification(
                newSelectableSpecifications,
                productGroup.productVariants
            )?.let{ nextSelectableSpecification ->
                addSelectableSpecification(productGroup, nextSelectableSpecification)
            }
        }

        val selectableSpecifications = selectableSpecificationsByProductGroup[productGroup] ?: return

        val specifications = selectableSpecifications.toSpecifications()

        val filteredProductVariants = productGroup.productVariants.filter(specifications)

        if(selectableSpecifications.none { it.selectedValue == null } &&
            createNextSelectableSpecification(selectableSpecifications, filteredProductVariants) == null){
            filteredProductVariants.getOrNull(0)?.let{ selectedProductVariant ->
                Product.create(productGroup, selectedProductVariant.id)?.let{ selectedProduct ->
                    selectProduct(selectedProduct)
                }
            }
        }

        this.currentSelectableSpecifications.value = selectableSpecifications
    }

    private fun ProductGroup.countInStock(): Int{
        return productVariants
            .map{ it.quantityInStock }
            .reduce{ acc, next -> acc + next}
    }

    // region Products in cart

    private fun addProductToCart(
        product: Product,
        amount: Int = 1
    ): ProductInCart{
        val additionalAmount = amount.coerceAtLeast(0)

        var productInCart = mutableCart.find{ it.product == product }

        productInCart = if(productInCart != null){
            val currentAmount = productInCart.amount
            val newAmount = (currentAmount + additionalAmount).coerceAtMost(product.quantityInStock)
            mutableCart.remove(productInCart)
            productInCart.copy(amount = newAmount)
        } else{
            ProductInCart(product, additionalAmount)
        }

        mutableCart.add(productInCart)

        cart.value = mutableCart.toList()

        return productInCart
    }

    private fun removeProductFromCart(
        product: Product,
        amount: Int = 1
    ): ProductInCart?{
        val amountToRemove = amount.coerceAtLeast(0)
        val result: ProductInCart?

        val productInCart = mutableCart.find{ it.product == product }

        if(productInCart != null){
            val currentAmount = productInCart.amount
            val newAmount = (currentAmount - amountToRemove).coerceAtLeast(0)

            mutableCart.remove(productInCart)

            if(newAmount <= 0){
                result = null
            } else {
                result = productInCart.copy(amount = newAmount)
                mutableCart.add(result)
            }

            cart.value = mutableCart.toList()
        } else result = null

        return result
    }

    private fun removeProductFromCart(productInCart: ProductInCart){
        mutableCart.remove(productInCart)
        cart.value = mutableCart.toList()
    }

    // endregion

    // region Selectable specifications

    private fun createNextSelectableSpecification(
        selectableSpecifications: Collection<SelectableSpecification<*>>,
        filteredProductVariants: Collection<ProductVariant>
    ): SelectableSpecification<*>?{
        val lastSelectableSpecificationIndex = selectableSpecifications.size.minus(1)
        val nextSelectableSpecificationIndex = lastSelectableSpecificationIndex + 1

        if(filteredProductVariants.isEmpty()) return null

        val nextSpecification = filteredProductVariants
            .elementAt(0)
            .specifications
            .getOrNull(nextSelectableSpecificationIndex) ?: return null

        val nextSpecificationClass = nextSpecification::class
        val nextSelectableSpecificationName = nextSpecification.name

        val nextFilterValues = mutableSetOf<Any>()

        for(productVariant in filteredProductVariants) {
            val selectableSpecificationValue = productVariant
                .specifications
                .firstOrNull { nextSpecificationClass.isInstance(it) && it.name == nextSelectableSpecificationName }
                ?.value ?: continue

            nextFilterValues.add(selectableSpecificationValue)
        }

        return when(nextSpecification){
            is Specification.ColorSpecification -> {
                val availableValues = nextFilterValues.map{ it as Int }.toSet()
                SelectableSpecification.ColorSpecification(
                    nextSelectableSpecificationName,
                    availableValues
                )
            }
            is Specification.DescriptiveSpecification -> {
                val availableValues = nextFilterValues.map{ it as String }.toSet()
                SelectableSpecification.DescriptiveSpecification(
                    nextSelectableSpecificationName,
                    availableValues
                )
            }
        }
    }

    @Synchronized
    private fun addSelectableSpecification(
        productGroup: ProductGroup,
        selectableSpecification: SelectableSpecification<*>
    ){
        val selectableSpecifications = selectableSpecificationsByProductGroup[productGroup]

        if(selectableSpecifications != null){
            selectableSpecifications.add(selectableSpecification)
        } else{
            val newSelectableSpecifications = mutableListOf(selectableSpecification)
            selectableSpecificationsByProductGroup[productGroup] = newSelectableSpecifications
        }

        if(selectedProductGroup == productGroup){
            this.currentSelectableSpecifications.value = selectableSpecifications
        }
    }

    private fun clearSelectableSpecifications(productGroup: ProductGroup){
        val selectableSpecifications = selectableSpecificationsByProductGroup[productGroup]

        if(selectableSpecifications != null){
            selectableSpecificationsByProductGroup.remove(productGroup)
        }

        this.currentSelectableSpecifications.value = emptyList()
    }

    private fun SelectableSpecification<*>.toSpecification(): Specification<*>?{
        return when(this){
            is SelectableSpecification.ColorSpecification -> {
                Specification.ColorSpecification(name, selectedValue ?: return null)
            }
            is SelectableSpecification.DescriptiveSpecification -> {
                Specification.DescriptiveSpecification(name, selectedValue ?: return null)
            }
        }
    }

    private fun Collection<SelectableSpecification<*>>.toSpecifications(): Set<Specification<*>>{
        val specifications = mutableSetOf<Specification<*>>()

        forEach { selectableSpecification ->
            selectableSpecification.toSpecification()?.let {
                specifications.add(it)
            }
        }

        return specifications
    }

    // endregion
}