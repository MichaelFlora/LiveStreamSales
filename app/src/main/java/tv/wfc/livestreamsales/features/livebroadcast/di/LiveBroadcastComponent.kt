package tv.wfc.livestreamsales.features.livebroadcast.di

import androidx.fragment.app.Fragment
import dagger.BindsInstance
import dagger.Subcomponent
import tv.wfc.livestreamsales.features.livebroadcast.di.modules.diffutils.DiffUtilsModule
import tv.wfc.livestreamsales.features.livebroadcast.di.modules.exoplayer.ExoPlayerModule
import tv.wfc.livestreamsales.features.livebroadcast.di.modules.viewmodel.LiveBroadcastViewModelModule
import tv.wfc.livestreamsales.features.livebroadcast.di.qualifiers.LiveBroadcastFragment
import tv.wfc.livestreamsales.features.livebroadcast.di.scope.LiveBroadcastFeatureScope

@LiveBroadcastFeatureScope
@Subcomponent(modules = [
    LiveBroadcastViewModelModule::class,
    ExoPlayerModule::class,
    DiffUtilsModule::class
])
interface LiveBroadcastComponent {
    @Subcomponent.Factory
    interface Factory {
        fun create(
            @BindsInstance
            @LiveBroadcastFragment
            fragment: Fragment
        ): LiveBroadcastComponent
    }

    fun inject(fragment: tv.wfc.livestreamsales.features.livebroadcast.ui.LiveBroadcastFragment)
}