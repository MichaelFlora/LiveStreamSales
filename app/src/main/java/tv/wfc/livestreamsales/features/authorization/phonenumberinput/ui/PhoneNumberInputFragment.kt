package tv.wfc.livestreamsales.features.authorization.phonenumberinput.ui

import android.content.Context
import android.os.Bundle
import android.text.Spannable
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.view.View
import androidx.core.content.ContextCompat
import androidx.navigation.fragment.findNavController
import com.google.android.material.snackbar.Snackbar
import com.jakewharton.rxbinding4.view.clicks
import com.laurus.p.edittextformatters.PhoneNumberTextFormatter
import io.reactivex.rxjava3.core.Scheduler
import io.reactivex.rxjava3.kotlin.addTo
import tv.wfc.livestreamsales.R
import tv.wfc.livestreamsales.application.di.modules.reactivex.qualifiers.ComputationScheduler
import tv.wfc.livestreamsales.application.di.modules.reactivex.qualifiers.MainThreadScheduler
import tv.wfc.livestreamsales.application.ui.base.BaseFragment
import tv.wfc.livestreamsales.databinding.FragmentPhoneNumberInputBinding
import tv.wfc.livestreamsales.features.authorization.phonenumberinput.di.PhoneNumberInputComponent
import tv.wfc.livestreamsales.features.authorization.phonenumberinput.viewmodel.IPhoneNumberInputViewModel
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit
import javax.inject.Inject

class PhoneNumberInputFragment: BaseFragment(R.layout.fragment_phone_number_input) {
    private val codeRequestErrorSnackbar: Snackbar? by lazy {
        viewBinding?.root?.let{
            Snackbar.make(
                it,
                R.string.activity_authorization_code_request_error,
                Snackbar.LENGTH_LONG
            )
        }
    }

    private val navigationController by lazy{
        findNavController()
    }

    private var viewBinding: FragmentPhoneNumberInputBinding? = null

    lateinit var phoneNumberInputComponent: PhoneNumberInputComponent
        private set

    @Inject
    lateinit var viewModel: IPhoneNumberInputViewModel

    @Inject
    @MainThreadScheduler
    lateinit var mainThreadScheduler: Scheduler

    @Inject
    @ComputationScheduler
    lateinit var computationScheduler: Scheduler

    override fun onAttach(context: Context) {
        super.onAttach(context)
        createPhoneNumberInputComponent()
        injectDependencies()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        bindView(view)
        initializeContentLoader()
        manageNavigation()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        unbindView()
    }

    private fun createPhoneNumberInputComponent(){
        val authorizationComponent = appComponent
            .authorizationComponent()
            .create()

        phoneNumberInputComponent = authorizationComponent
            .phoneNumberInputComponent()
            .create(this)
    }

    private fun injectDependencies(){
        phoneNumberInputComponent.inject(this)
    }

    private fun bindView(view: View){
        viewBinding = FragmentPhoneNumberInputBinding.bind(view)
    }

    private fun unbindView(){
        viewBinding = null
    }

    private fun initializeContentLoader(){
        viewBinding?.contentLoader?.apply {
            clearPreparationListeners()
            attachViewModel(viewLifecycleOwner, viewModel)
            addOnDataIsPreparedListener(::onDataIsPrepared)
        }
    }

    private fun onDataIsPrepared() {
        initializePhoneNumberEditText()
        initializeSendCodeButton()
        initializeNewCodeTimerText()
        initializeCodeRequestErrorSnackbar()
    }

    private fun initializePhoneNumberEditText(){
        viewBinding?.run{
            phoneNumberEditText.setText(viewModel.phoneNumber.value ?: "")

            phoneNumberEditText.addTextChangedListener(PhoneNumberTextFormatter{
                viewModel.updatePhoneNumber(it.toString())
            })
        }
    }

    private fun initializeSendCodeButton(){
        viewBinding?.sendCodeButton?.apply{
            viewModel.canUserRequestCode.observe(
                viewLifecycleOwner,
                ::setEnabled
            )

            clicks()
                .throttleLatest(2, TimeUnit.SECONDS, computationScheduler)
                .observeOn(mainThreadScheduler)
                .subscribe{
                    viewModel.requestConfirmationCode()
                }
                .addTo(viewScopeDisposables)
        }
    }

    private fun initializeNewCodeTimerText(){
        viewModel.isCodeRequestAvailable.observe(
            viewLifecycleOwner,
            ::manageNewCodeTimerTextVisibility
        )

        viewModel.newCodeRequestWaitingTime.observe(
            viewLifecycleOwner,
            ::updateNewCodeWaitingTimeText
        )
    }

    private fun manageNewCodeTimerTextVisibility(isCodeRequestAvailable: Boolean){
        viewBinding?.newCodeTimerText?.apply{
            visibility = if(isCodeRequestAvailable){
                View.INVISIBLE
            } else{
                View.VISIBLE
            }
        }
    }

    private fun manageNavigation(){
        viewModel.isConfirmationCodeSent.observe(viewLifecycleOwner, { isConfirmationCodeSent ->
            if(isConfirmationCodeSent){
                val action = PhoneNumberInputFragmentDirections.actionPhoneNumberInputDestinationToPhoneNumberConfirmationDestination()
                navigationController.navigate(action)
            }
        })
    }

    private fun initializeCodeRequestErrorSnackbar(){
        viewModel.isConfirmationCodeSent.observe(viewLifecycleOwner, { isConfirmationCodeSent ->
            if(!isConfirmationCodeSent){
                codeRequestErrorSnackbar?.show()
            }
        })
    }

    private fun updateNewCodeWaitingTimeText(timeLeft: Long){
        viewBinding?.newCodeTimerText?.apply {
            val date = Date(timeLeft * 1000)
            val formattedTime = SimpleDateFormat("mm:ss", Locale.getDefault()).format(date)

            val originText = getString(R.string.fragment_phone_number_input_new_code_message)
            val textWithSubstitutions = originText.format(formattedTime)

            val phoneNumberTextStart = originText.indexOfFirst{it == '%'}
            val phoneNumberTextEnd = phoneNumberTextStart + formattedTime.toString().length

            val phoneNumberTextColor = ContextCompat.getColor(context, R.color.colorPrimary)

            val styledText = SpannableString(textWithSubstitutions).apply {
                setSpan(
                    ForegroundColorSpan(phoneNumberTextColor),
                    phoneNumberTextStart,
                    phoneNumberTextEnd,
                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                )
            }

            text = styledText
        }
    }
}