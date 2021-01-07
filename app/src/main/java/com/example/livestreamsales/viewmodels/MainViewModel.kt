package com.example.livestreamsales.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.livestreamsales.authorization.IAuthorizationManager
import com.example.livestreamsales.di.components.app.ReactiveXModule
import com.example.livestreamsales.network.rest.errors.IResponseErrorsManager
import io.reactivex.rxjava3.core.Scheduler
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.kotlin.addTo
import javax.inject.Inject
import javax.inject.Named

class MainViewModel @Inject constructor(
    authorizationManager: IAuthorizationManager,
    private val responseErrorsManager: IResponseErrorsManager,
    @Named(ReactiveXModule.DEPENDENCY_NAME_MAIN_THREAD_SCHEDULER)
    private val mainThreadScheduler: Scheduler
): ViewModel(), IMainViewModel{
    private val disposables = CompositeDisposable()

//    override val isUserLoggedIn: LiveData<Boolean> = LiveDataReactiveStreams.fromPublisher(
//        authorizationManager.isUserLoggedIn.toFlowable(BackpressureStrategy.LATEST)
//    )

    override val isUserLoggedIn: LiveData<Boolean> = MutableLiveData<Boolean>().apply {
        authorizationManager.isUserLoggedIn
            .subscribeOn(mainThreadScheduler)
            .subscribe(this::setValue)
            .addTo(disposables)
    }

    override fun onCleared() {
        disposables.dispose()
        super.onCleared()
    }
}