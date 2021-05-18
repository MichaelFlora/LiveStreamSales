package tv.wfc.livestreamsales.application.di.modules.repository

import dagger.Binds
import dagger.Module
import tv.wfc.livestreamsales.application.di.scope.ApplicationScope
import tv.wfc.livestreamsales.application.repository.applicationsettings.ApplicationSettingsRepository
import tv.wfc.livestreamsales.application.repository.applicationsettings.IApplicationSettingsRepository
import tv.wfc.livestreamsales.application.repository.authorization.AuthorizationRepository
import tv.wfc.livestreamsales.application.repository.authorization.IAuthorizationRepository
import tv.wfc.livestreamsales.application.repository.broadcastsinformation.BroadcastsInformationRepository
import tv.wfc.livestreamsales.application.repository.broadcastsinformation.IBroadcastsInformationRepository
import tv.wfc.livestreamsales.application.repository.products.IProductsRepository
import tv.wfc.livestreamsales.application.repository.products.ProductsRepository
import tv.wfc.livestreamsales.application.repository.userinformation.IUserInformationRepository
import tv.wfc.livestreamsales.application.repository.userinformation.UserInformationRepository
import tv.wfc.livestreamsales.features.livebroadcast.repository.BroadcastAnalyticsRepository
import tv.wfc.livestreamsales.features.livebroadcast.repository.IBroadcastAnalyticsRepository

@Module
abstract class AppRepositoryModule {
    @ApplicationScope
    @Binds
    internal abstract fun bindUserInformationRepository(
        userInformationRepository: UserInformationRepository
    ): IUserInformationRepository

    @ApplicationScope
    @Binds
    internal abstract fun bindBroadcastAnalyticsRepository(
        broadcastAnalyticsRepository: BroadcastAnalyticsRepository
    ): IBroadcastAnalyticsRepository

    @ApplicationScope
    @Binds
    internal abstract fun bindApplicationSettingsRepository(
        applicationSettingsRepository: ApplicationSettingsRepository
    ): IApplicationSettingsRepository

    @ApplicationScope
    @Binds
    abstract fun bindAuthorizationRepository(
        authorizationRepository: AuthorizationRepository
    ): IAuthorizationRepository

    @ApplicationScope
    @Binds
    internal abstract fun bindBroadcastsInformationRepository(
        broadcastsInformationRepository: BroadcastsInformationRepository
    ): IBroadcastsInformationRepository

    @ApplicationScope
    @Binds
    internal abstract fun bindProductsRepository(
        productsRepository: ProductsRepository
    ): IProductsRepository
}