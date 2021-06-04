package tv.wfc.livestreamsales.application.di

import android.content.Context
import dagger.BindsInstance
import dagger.Component
import tv.wfc.livestreamsales.application.di.modules.certificates.CertificatesModule
import tv.wfc.livestreamsales.application.di.modules.coil.CoilModule
import tv.wfc.livestreamsales.application.di.modules.errorslogger.ErrorsLoggerModule
import tv.wfc.livestreamsales.application.di.modules.manager.ManagersModule
import tv.wfc.livestreamsales.application.di.modules.reactivex.ReactiveXModule
import tv.wfc.livestreamsales.application.di.modules.repository.AppRepositoryModule
import tv.wfc.livestreamsales.application.di.modules.rest.RestModule
import tv.wfc.livestreamsales.application.di.modules.restapi.ApiModule
import tv.wfc.livestreamsales.application.di.modules.sharedpreferences.SharedPreferencesModule
import tv.wfc.livestreamsales.application.di.modules.storage.AppStorageModule
import tv.wfc.livestreamsales.application.di.modules.subcomponents.AppSubComponentsModule
import tv.wfc.livestreamsales.application.di.modules.utils.UtilsModule
import tv.wfc.livestreamsales.application.di.modules.viewmodelprovider.ViewModelProviderModule
import tv.wfc.livestreamsales.application.di.scope.ApplicationScope
import tv.wfc.livestreamsales.features.greeting.di.GreetingComponent
import tv.wfc.livestreamsales.features.home.di.HomeComponent
import tv.wfc.livestreamsales.features.livebroadcast.di.LiveBroadcastComponent
import tv.wfc.livestreamsales.features.authorization.di.AuthorizationComponent
import tv.wfc.livestreamsales.features.mainappcontent.di.MainAppContentComponent
import tv.wfc.livestreamsales.features.mainpage.di.MainPageComponent
import tv.wfc.livestreamsales.features.paymentcardinformation.di.PaymentCardInformationComponent
import tv.wfc.livestreamsales.features.productorder.di.ProductOrderComponent
import tv.wfc.livestreamsales.features.productsareordered.di.ProductsAreOrderedComponent
import tv.wfc.livestreamsales.features.splash.di.SplashComponent
import tv.wfc.livestreamsales.features.usersettings.di.ProfileComponent

@ApplicationScope
@Component(modules = [
    AppSubComponentsModule::class,
    ReactiveXModule::class,
    ErrorsLoggerModule::class,
    RestModule::class,
    CertificatesModule::class,
    ManagersModule::class,
    SharedPreferencesModule::class,
    ViewModelProviderModule::class,
    ApiModule::class,
    AppStorageModule::class,
    AppRepositoryModule::class,
    CoilModule::class,
    UtilsModule::class
])
interface AppComponent {
    @Component.Factory
    interface Factory{
        fun create(@BindsInstance context: Context): AppComponent
    }

    fun splashComponent(): SplashComponent.Factory
    fun greetingComponent(): GreetingComponent.Factory
    fun authorizationComponent(): AuthorizationComponent.Factory
    fun mainAppContentComponent(): MainAppContentComponent.Factory
    fun homeComponent(): HomeComponent.Factory
    fun mainPageComponent(): MainPageComponent.Factory
    fun liveBroadcastComponent(): LiveBroadcastComponent.Factory
    fun productOrderComponent(): ProductOrderComponent.Factory
    fun profileComponent(): ProfileComponent.Factory
    fun productsAreOrderedComponent(): ProductsAreOrderedComponent.Factory
    fun paymentCardInformationComponent(): PaymentCardInformationComponent.Factory
}