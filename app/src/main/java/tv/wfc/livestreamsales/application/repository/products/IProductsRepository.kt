package tv.wfc.livestreamsales.application.repository.products

import io.reactivex.rxjava3.core.Observable
import tv.wfc.livestreamsales.application.model.products.ProductGroup

interface IProductsRepository {
    fun getProductGroups(broadcastId: Long): Observable<List<ProductGroup>>
}