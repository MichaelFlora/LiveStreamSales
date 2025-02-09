package tv.wfc.livestreamsales.features.streamediting.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import io.reactivex.rxjava3.core.Scheduler
import io.reactivex.rxjava3.kotlin.addTo
import io.reactivex.rxjava3.kotlin.subscribeBy
import io.reactivex.rxjava3.subjects.BehaviorSubject
import io.reactivex.rxjava3.subjects.PublishSubject
import tv.wfc.contentloader.model.ViewModelPreparationState
import tv.wfc.livestreamsales.application.base.viewmodel.BaseViewModel
import tv.wfc.livestreamsales.application.di.modules.reactivex.qualifiers.MainThreadScheduler
import tv.wfc.livestreamsales.application.tools.errors.IApplicationErrorsLogger
import tv.wfc.livestreamsales.features.streamediting.model.NextDestination
import javax.inject.Inject

class StreamEditingViewModel @Inject constructor(
    @MainThreadScheduler
    private val mainThreadScheduler: Scheduler,
    private val applicationErrorsLogger: IApplicationErrorsLogger
): BaseViewModel(), IStreamEditingViewModel {
    private val dataPreparationStateSubject = BehaviorSubject.createDefault<ViewModelPreparationState>(ViewModelPreparationState.DataIsNotPrepared)

    private val nextDestinationEventSubject = PublishSubject.create<NextDestination>()

    override val dataPreparationState: LiveData<ViewModelPreparationState> = MutableLiveData<ViewModelPreparationState>().apply {
        dataPreparationStateSubject
            .observeOn(mainThreadScheduler)
            .subscribeBy(
                onNext = ::setValue,
                onError = applicationErrorsLogger::logError
            )
            .addTo(disposables)
    }

    override val isAnyOperationInProgress: LiveData<Boolean> = MutableLiveData<Boolean>().apply {
        isAnyOperationInProgressObservable
            .observeOn(mainThreadScheduler)
            .subscribeBy(
                onNext = ::setValue,
                onError = applicationErrorsLogger::logError
            )
            .addTo(disposables)
    }

    override val nextDestinationEvent: LiveData<NextDestination> = MutableLiveData<NextDestination>().apply {
        nextDestinationEventSubject
            .observeOn(mainThreadScheduler)
            .subscribeBy(
                onNext = ::setValue,
                onError = applicationErrorsLogger::logError
            )
            .addTo(disposables)
    }

    override fun prepare(streamId: Long) = Unit

    override fun requestToCloseCurrentDestination() {
        nextDestinationEventSubject.onNext(NextDestination.Close)
    }
}