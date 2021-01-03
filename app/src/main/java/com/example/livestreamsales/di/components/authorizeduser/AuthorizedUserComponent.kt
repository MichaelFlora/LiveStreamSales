package com.example.livestreamsales.di.components.authorizeduser

import com.example.livestreamsales.di.scopes.AuthorizedUserScope
import dagger.BindsInstance
import dagger.Subcomponent
import javax.inject.Named

@AuthorizedUserScope
@Subcomponent(modules = [

])
interface AuthorizedUserComponent {

    companion object {
        internal const val DEPENDENCY_NAME_AUTHORIZATION_TOKEN = "AUTHORIZATION_TOKEN"
    }

    @Subcomponent.Factory
    interface Factory{
        fun create(
            @BindsInstance
            @Named(DEPENDENCY_NAME_AUTHORIZATION_TOKEN)
            token:String
        ): AuthorizedUserComponent
    }
}