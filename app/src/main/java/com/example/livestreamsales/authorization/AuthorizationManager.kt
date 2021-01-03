package com.example.livestreamsales.authorization

import com.example.livestreamsales.di.components.app.ReactiveXModule
import com.example.livestreamsales.di.components.authorizeduser.AuthorizedUserComponent
import com.example.livestreamsales.model.request.SendCodeRequestBody
import com.example.livestreamsales.network.rest.api.IAuthorizationApi
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.Scheduler
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.kotlin.addTo
import io.reactivex.rxjava3.subjects.BehaviorSubject
import javax.inject.Inject
import javax.inject.Named

class AuthorizationManager @Inject constructor(
    private val authorizationApi: IAuthorizationApi,
    private val authorizedUserComponentFactory: AuthorizedUserComponent.Factory,
    @Named(ReactiveXModule.DEPENDENCY_NAME_MAIN_THREAD_SCHEDULER)
    private val mainThreadScheduler: Scheduler
): IAuthorizationManager {
    private val disposables = CompositeDisposable()

    override val authorizedUserComponent: BehaviorSubject<AuthorizedUserComponent?> = BehaviorSubject.create<AuthorizedUserComponent?>()
    override val token: BehaviorSubject<String?> = BehaviorSubject.createDefault<String?>(null)
    override val isUserLoggedIn: Observable<Boolean> = token.map{ it != null }

    init{
        manageAuthorizedUserComponentLifecycle()
    }

    override fun sendCode(telephoneNumber: String){
        val sendCodeRequestBody = SendCodeRequestBody(telephoneNumber)

        authorizationApi.sendCode(sendCodeRequestBody)
    }

    // TODO: extract subscribe body into another function
    private fun manageAuthorizedUserComponentLifecycle(){
        token
            .observeOn(mainThreadScheduler)
            .subscribe {
                val authorizedUserComponentNewValue = if(it != null){
                    authorizedUserComponentFactory.create(it)
                } else null

                authorizedUserComponent.onNext(authorizedUserComponentNewValue)
            }
            .addTo(disposables)
    }
}