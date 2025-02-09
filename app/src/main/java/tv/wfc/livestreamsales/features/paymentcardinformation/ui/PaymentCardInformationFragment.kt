package tv.wfc.livestreamsales.features.paymentcardinformation.ui

import android.app.Activity.RESULT_CANCELED
import android.app.Activity.RESULT_OK
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.navigation.fragment.findNavController
import com.google.android.material.snackbar.Snackbar
import com.jakewharton.rxbinding4.view.clicks
import io.reactivex.rxjava3.core.Scheduler
import io.reactivex.rxjava3.kotlin.addTo
import io.reactivex.rxjava3.kotlin.subscribeBy
import ru.yoomoney.sdk.kassa.payments.Checkout
import tv.wfc.livestreamsales.R
import tv.wfc.livestreamsales.application.di.modules.reactivex.qualifiers.ComputationScheduler
import tv.wfc.livestreamsales.application.di.modules.reactivex.qualifiers.MainThreadScheduler
import tv.wfc.livestreamsales.application.tools.errors.IApplicationErrorsLogger
import tv.wfc.livestreamsales.application.ui.base.BaseFragment
import tv.wfc.livestreamsales.databinding.FragmentPaymentCardInformationBinding
import tv.wfc.livestreamsales.features.paymentcardinformation.di.PaymentCardInformationComponent
import tv.wfc.livestreamsales.features.paymentcardinformation.viewmodel.IPaymentCardInformationViewModel
import java.util.concurrent.TimeUnit
import javax.inject.Inject

class PaymentCardInformationFragment: BaseFragment(R.layout.fragment_payment_card_information) {
    private companion object{
        private const val REQUEST_CODE_TOKENIZE = 0
        private const val REQUEST_CODE_3D_SECURE = 1
    }

    private val navigationController by lazy { findNavController() }

    private var viewBinding: FragmentPaymentCardInformationBinding? = null

    private lateinit var paymentCardInformationComponent: PaymentCardInformationComponent

    @Inject
    lateinit var viewModel: IPaymentCardInformationViewModel

    @Inject
    @MainThreadScheduler
    lateinit var mainThreadScheduler: Scheduler

    @Inject
    @ComputationScheduler
    lateinit var computationScheduler: Scheduler

    @Inject
    lateinit var applicationErrorsLogger: IApplicationErrorsLogger

    override fun onAttach(context: Context) {
        super.onAttach(context)
        createRegistrationPaymentCardInformationComponent()
        injectDependencies()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        bindView(view)
        initializeSwipeRefreshLayout()
        initializeContentLoader()
        manageNavigation()
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.menu_exit, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when(item.itemId){
            R.id.close -> exit()
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        val viewBinding = this.viewBinding ?: return

        when(requestCode){
            REQUEST_CODE_TOKENIZE -> {
                if(data == null) return

                when(resultCode){
                    RESULT_OK -> {
                        viewModel.startPaymentCardBinding(data)
                    }
                    RESULT_CANCELED -> {
                        Snackbar
                            .make(
                                viewBinding.root,
                                getString(R.string.fragment_payment_card_information_binding_default_error),
                                Snackbar.LENGTH_LONG
                            )
                            .show()
                    }
                }
            }
            REQUEST_CODE_3D_SECURE -> {
                when(resultCode){
                    RESULT_OK -> {
                        viewModel.refreshData()
                    }
                    RESULT_CANCELED -> {
                        Snackbar
                            .make(
                                viewBinding.root,
                                getString(R.string.fragment_payment_card_information_binding_3ds_canceled_error),
                                Snackbar.LENGTH_LONG
                            )
                            .show()
                    }
                    Checkout.RESULT_ERROR -> {
                        if(data == null) return
                        viewModel.notify3dsErrorOccurred(data)
                    }
                }
            }
        }
    }

    override fun onDestroyView() {
        unbindView()
        super.onDestroyView()
    }

    private fun createRegistrationPaymentCardInformationComponent(){
        paymentCardInformationComponent = appComponent
            .paymentCardInformationComponent()
            .create(this)
    }

    private fun injectDependencies(){
        paymentCardInformationComponent.inject(this)
    }

    private fun bindView(view: View){
        viewBinding = FragmentPaymentCardInformationBinding.bind(view)
    }

    private fun unbindView(){
        viewBinding = null
    }

    private fun initializeSwipeRefreshLayout(){
        viewBinding?.swipeRefreshLayout?.run{
            viewModel.isDataBeingRefreshed.observe(viewLifecycleOwner){ isDataBeingRefreshed ->
                isRefreshing = isDataBeingRefreshed
            }

            setOnRefreshListener {
                viewModel.refreshData()
            }
        }
    }

    private fun initializeContentLoader(){
        viewBinding?.contentLoader?.apply {
            clearPreparationListeners()
            attachViewModel(viewLifecycleOwner, viewModel)
            addOnDataIsPreparedListener(::onDataIsPrepared)

            viewModel.isAnyOperationInProgress.observe(viewLifecycleOwner){ isAnyOperationInProgress ->
                if(isAnyOperationInProgress){
                    showOperationProgress()
                } else {
                    hideOperationProgress()
                }
            }
        }
    }

    private fun onDataIsPrepared() {
        initializeLinkPaymentCardButton()
        initializeSkipButton()
        initializeSnackBar()
        initializeBoundPaymentCardTitleText()
        initializeBoundPaymentCardText()
    }

    private fun manageNavigation(){
        viewModel.paymentCardBindingConfirmationUrl.observe(viewLifecycleOwner){ confirmationUrl ->
            confirmationUrl?.let { navigateTo3DSecure(it) }
        }
    }

    private fun initializeLinkPaymentCardButton(){
        viewBinding?.linkPaymentCardButton?.run{
            clicks()
                .throttleLatest(500L, TimeUnit.MILLISECONDS, computationScheduler)
                .observeOn(mainThreadScheduler)
                .subscribeBy(
                    onNext = { navigateToPaymentCardLinker() },
                    onError = applicationErrorsLogger::logError
                )
                .addTo(viewScopeDisposables)

            viewModel.paymentCardBindingState.observe(viewLifecycleOwner){ state ->
                text = when(state){
                    is IPaymentCardInformationViewModel.CardBindingState.Bound,
                    IPaymentCardInformationViewModel.CardBindingState.WillBeBoundSoon -> getString(R.string.fragment_payment_card_information_link_another_card_button)
                    else -> getString(R.string.fragment_payment_card_information_link_card_button)
                }
            }
        }
    }

    private fun initializeSkipButton(){
        viewBinding?.skipButton?.run{
            clicks()
                .throttleLatest(500L, TimeUnit.MILLISECONDS, computationScheduler)
                .observeOn(mainThreadScheduler)
                .subscribeBy(
                    onNext = { exit() },
                    onError = applicationErrorsLogger::logError
                )
                .addTo(viewScopeDisposables)
        }
    }

    private fun initializeSnackBar(){
        val context = this.context ?: return
        viewModel.paymentCardBindingError.observe(viewLifecycleOwner){ error ->
            Toast.makeText(context, error, Toast.LENGTH_LONG).show()
        }
    }

    private fun initializeBoundPaymentCardTitleText(){
        viewBinding?.boundPaymentCardTitleText?.apply{
            viewModel.paymentCardBindingState.observe(viewLifecycleOwner){ state ->
                visibility = when(state) {
                    is IPaymentCardInformationViewModel.CardBindingState.Bound,
                    IPaymentCardInformationViewModel.CardBindingState.WillBeBoundSoon -> View.VISIBLE
                    else -> View.GONE
                }
            }
        }
    }

    private fun initializeBoundPaymentCardText(){
        viewBinding?.boundPaymentCardText?.apply{
            viewModel.paymentCardBindingState.observe(viewLifecycleOwner){ state ->
                when(state) {
                    is IPaymentCardInformationViewModel.CardBindingState.Bound ->{
                        visibility = View.VISIBLE
                        text = context.getString(
                            R.string.fragment_payment_card_information_number_text,
                            state.cardNumber
                        )
                    }
                    IPaymentCardInformationViewModel.CardBindingState.WillBeBoundSoon -> {
                        visibility = View.VISIBLE
                        text = context.getString(R.string.fragment_payment_card_information_card_will_be_bound_soon)
                    }
                    else -> {
                        text = ""
                        visibility = View.GONE
                    }
                }
            }
        }
    }

    private fun navigateToPaymentCardLinker(){
        val context = this.context ?: return
        val paymentCardBindingParameters = viewModel.paymentCardBindingParameters.value ?: return

        val tokenizeIntent = Checkout.createTokenizeIntent(
            context,
            paymentCardBindingParameters,
        )

        startActivityForResult(tokenizeIntent, REQUEST_CODE_TOKENIZE)
    }

    private fun navigateTo3DSecure(confirmationUrl: String){
        val context = this.context ?: return

        val `3dSecureIntent` = Checkout.create3dsIntent(
            context,
            confirmationUrl
        )

        startActivityForResult(`3dSecureIntent`, REQUEST_CODE_3D_SECURE)
    }

    private fun exit(){
        navigationController.navigateUp()
    }
}