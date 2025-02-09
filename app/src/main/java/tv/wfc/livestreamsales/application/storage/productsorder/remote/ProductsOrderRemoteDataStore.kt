package tv.wfc.livestreamsales.application.storage.productsorder.remote

import androidx.core.graphics.toColorInt
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.Scheduler
import io.reactivex.rxjava3.core.Single
import org.joda.time.DateTime
import tv.wfc.core.entity.IEntityMapper
import tv.wfc.livestreamsales.application.di.modules.reactivex.qualifiers.IoScheduler
import tv.wfc.livestreamsales.application.model.address.Address
import tv.wfc.livestreamsales.application.model.exception.storage.FailedToUpdateDataInStorageException
import tv.wfc.livestreamsales.application.model.exception.storage.NoSuchDataInStorageException
import tv.wfc.livestreamsales.application.model.exception.storage.ReceivedDataWithWrongFormatException
import tv.wfc.livestreamsales.application.model.orders.Order
import tv.wfc.livestreamsales.application.model.orders.OrderRecipient
import tv.wfc.livestreamsales.application.model.orders.OrderedProduct
import tv.wfc.livestreamsales.application.model.products.Product
import tv.wfc.livestreamsales.application.model.products.specification.Specification
import tv.wfc.livestreamsales.application.model.storage.StorageDataUpdateResult
import tv.wfc.livestreamsales.application.storage.productsorder.IProductsOrderDataStore
import tv.wfc.livestreamsales.features.rest.api.authorized.productsorders.IProductsOrdersApi
import tv.wfc.livestreamsales.features.rest.api.authorized.productsorders.confirmorder.ConfirmOrderRequestBody
import tv.wfc.livestreamsales.features.rest.model.api.orderproducts.OrderProductsRequestBody
import tv.wfc.livestreamsales.features.rest.model.api.orderproducts.Property
import javax.inject.Inject

private typealias RemoteOrder = tv.wfc.livestreamsales.features.rest.api.authorized.productsorders.models.Order
private typealias RemoteOrderedProduct = tv.wfc.livestreamsales.features.rest.api.authorized.productsorders.models.OrderedProduct
private typealias RemoteProduct = tv.wfc.livestreamsales.features.rest.api.authorized.productsorders.models.Product
private typealias RemoteAddress = tv.wfc.livestreamsales.features.rest.api.authorized.productsorders.models.Address
private typealias RemoteOrderRecipient = tv.wfc.livestreamsales.features.rest.api.authorized.productsorders.models.OrderRecipient

class ProductsOrderRemoteDataStore @Inject constructor(
    private val productsOrdersApi: IProductsOrdersApi,
    private val jodaDateTimeToIso8601StringMapper: IEntityMapper<DateTime, String>,
    private val iso8601StringToJodaDateTimeMapper: IEntityMapper<String, DateTime>,
    @IoScheduler
    private val ioScheduler: Scheduler
) : IProductsOrderDataStore {
    override fun orderProducts(products: List<OrderedProduct>): Completable {
        val orderProductsRequestBody = OrderProductsRequestBody(products.toRemoteProductsOrder())
        return productsOrdersApi
            .orderProducts(orderProductsRequestBody)
            .flatMapCompletable {
                when (it.areProductsOrdered) {
                    true -> Completable.complete()
                    false -> Completable.error(FailedToUpdateDataInStorageException())
                    else -> Completable.error(ReceivedDataWithWrongFormatException())
                }
            }
            .subscribeOn(ioScheduler)
    }

    override fun addProductToCart(product: Product, amount: Int): Completable {
        return Completable
            .error(NotImplementedError())
            .subscribeOn(ioScheduler)
    }

    override fun addProductsToCart(products: List<Product>): Completable {
        return Completable
            .error(NotImplementedError())
            .subscribeOn(ioScheduler)
    }

    override fun getOrderedProductsFromCartSingle(): Single<List<OrderedProduct>> {
        return Single
            .error<List<OrderedProduct>>(NotImplementedError())
            .subscribeOn(ioScheduler)
    }

    override fun getOrderedProductsFromCart(): Observable<List<OrderedProduct>> {
        return Observable
            .error<List<OrderedProduct>>(NotImplementedError())
            .subscribeOn(ioScheduler)
    }

    override fun removeProductFromCart(product: Product, amount: Int): Completable {
        return Completable
            .error(NotImplementedError())
            .subscribeOn(ioScheduler)
    }

    override fun removeProductFromCart(productId: Long, amount: Int): Completable {
        return Completable
            .error(NotImplementedError())
            .subscribeOn(ioScheduler)
    }

    override fun removeAllProductUnitsFromCart(product: Product): Completable {
        return Completable
            .error(NotImplementedError())
            .subscribeOn(ioScheduler)
    }

    override fun removeAllProductUnitsFromCart(productId: Long): Completable {
        return Completable
            .error(NotImplementedError())
            .subscribeOn(ioScheduler)
    }

    override fun removeAllProductsUnitsFromCart(products: List<Product>): Completable {
        return Completable
            .error(NotImplementedError())
            .subscribeOn(ioScheduler)
    }

    override fun removeAllProductsUnitsFromCart(vararg productIds: Long): Completable {
        return Completable
            .error(NotImplementedError())
            .subscribeOn(ioScheduler)
    }

    override fun removeAllProductsUnitsFromCart(): Completable {
        return Completable
            .error(NotImplementedError())
            .subscribeOn(ioScheduler)
    }

    override fun getOrders(): Single<List<Order>> {
        return productsOrdersApi
            .getOrders()
            .map { it.orders ?: throw NoSuchDataInStorageException() }
            .map { orders -> orders.mapNotNull { it.toApplicationOrder() } }
            .subscribeOn(ioScheduler)
    }

    override fun getOrder(id: Long): Single<Order> {
        return productsOrdersApi
            .getOrder(id)
            .map { it.order ?: throw NoSuchDataInStorageException() }
            .map { remoteOrder ->
                remoteOrder.toApplicationOrder() ?: throw ReceivedDataWithWrongFormatException()
            }
            .subscribeOn(ioScheduler)
    }

    override fun confirmOrder(
        orderId: Long,
        deliveryAddress: Address,
        deliveryDate: DateTime?
    ): Single<StorageDataUpdateResult> {
        val deliveryJodaDateTime = deliveryDate?.let(jodaDateTimeToIso8601StringMapper::map)

        val confirmOrderRequestBody = ConfirmOrderRequestBody(
            deliveryDate = deliveryJodaDateTime,
            deliveryAddress = deliveryAddress.toRemoteAddress()
        )

        return productsOrdersApi
            .confirmOrder(orderId, confirmOrderRequestBody)
            .flatMap {
                if (it.isOrderConfirmed == null) {
                    Single.error(ReceivedDataWithWrongFormatException())
                } else {
                    Single.just(
                        StorageDataUpdateResult(
                            it.isOrderConfirmed,
                            it.message
                        )
                    )
                }
            }
            .subscribeOn(ioScheduler)
    }

    private fun List<OrderedProduct>.toRemoteProductsOrder(): List<tv.wfc.livestreamsales.features.rest.model.api.orderproducts.Product> {
        return map {
            tv.wfc.livestreamsales.features.rest.model.api.orderproducts.Product(
                it.product.id,
                it.amount
            )
        }
    }

    private fun RemoteOrder.toApplicationOrder(): Order? {
        val id = this.id ?: return null
        val status = this.status?.toStatus() ?: return null
        val orderDate = this.orderDate?.let(iso8601StringToJodaDateTimeMapper::map) ?: return null
        val deliveryDate =
            this.deliveryDate?.let(iso8601StringToJodaDateTimeMapper::map) ?: return null
        val products = this.products?.mapNotNull { it.toApplicationOrderedProduct() } ?: return null
        val deliveryAddress = this.deliveryAddress?.toApplicationAddress()
        val orderRecipient = this.orderRecipient?.toApplicationOrderRecipient()

        return Order(
            id,
            status,
            orderDate,
            deliveryDate,
            products,
            deliveryAddress,
            orderRecipient
        )
    }

    private fun String.toStatus(): Order.Status? {
        return when (this) {
            "paid" -> Order.Status.PAID
            "created" -> Order.Status.CREATED
            "waiting" -> Order.Status.WAITING
            "done" -> Order.Status.DONE
            else -> null
        }
    }

    private fun RemoteOrderedProduct.toApplicationOrderedProduct(): OrderedProduct? {
        val product = this.product?.toApplicationProduct() ?: return null
        val amount = this.amount ?: 0

        return OrderedProduct(
            product,
            amount
        )
    }

    private fun RemoteProduct.toApplicationProduct(): Product? {
        val id = this.id ?: return null
        val name = this.name ?: return null
        val price = this.price ?: return null
        val specifications = properties?.mapNotNull { it.toSpecification() } ?: emptyList()

        return Product(
            id,
            name,
            price,
            image = imageUrl,
            specifications = specifications
        )
    }

    private fun RemoteAddress.toApplicationAddress(): Address? {
        val city = this.city ?: return null
        val street = this.street ?: return null
        val building = this.building ?: return null
        val flat = this.flat ?: return null

        return Address(
            city,
            street,
            building,
            flat,
            floor
        )
    }

    private fun Address.toRemoteAddress(): RemoteAddress {
        return RemoteAddress(city, street, building, flat, floor)
    }

    private fun RemoteOrderRecipient.toApplicationOrderRecipient(): OrderRecipient? {
        val name = this.name ?: return null

        return OrderRecipient(
            name,
            surname,
            email
        )
    }

    private fun Property.toSpecification(): Specification<*>? {
        val specificationType = type ?: return null
        val name = name ?: return null
        val value = value ?: return null

        return when (specificationType) {
            "regular" -> {
                Specification.DescriptiveSpecification(name, value)
            }
            "color" -> {
                try {
                    val color = colorHex?.toColorInt() ?: return null
                    Specification.ColorSpecification(
                        name = name,
                        color = color,
                        colorName = value
                    )
                } catch (ex: IllegalArgumentException) {
                    null
                }
            }
            else -> null
        }
    }
}