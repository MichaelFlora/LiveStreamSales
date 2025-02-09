package tv.wfc.livestreamsales.application.ui.base

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.*
import androidx.annotation.CallSuper
import androidx.annotation.LayoutRes
import androidx.fragment.app.DialogFragment
import com.laurus.p.tools.activity.getWindowSize
import io.reactivex.rxjava3.disposables.CompositeDisposable
import tv.wfc.livestreamsales.R
import tv.wfc.livestreamsales.application.LiveStreamSalesApplication
import tv.wfc.livestreamsales.application.di.AppComponent

abstract class BaseDialogFragment(
    @LayoutRes
    private val contentLayoutId: Int
): DialogFragment() {
    private var dialogWidth: Int
        get(){
            return getDialogDimensionSize(getWidth = true)
        }
        set(value){
            setDialogDimensionSize(value, setWidth = true)
        }

    private var dialogHeight: Int
        get(){
            return getDialogDimensionSize(getWidth = false)
        }
        set(value){
            setDialogDimensionSize(value, setWidth = false)
        }

    protected var dialogMarginHorizontal: Int = 0
    protected var dialogMarginVertical: Int = 0

    protected var dialogWidthAdaptationType = DialogDimensionAdaptationType.MAX_SIZE
    protected var dialogHeightAdaptationType = DialogDimensionAdaptationType.WRAP_CONTENT

    protected lateinit var viewScopeDisposables: CompositeDisposable
        private set

    protected lateinit var appComponent: AppComponent
        private set

    @CallSuper
    override fun onAttach(context: Context) {
        super.onAttach(context)
        appComponent = (context.applicationContext as LiveStreamSalesApplication).appComponent
    }

    @CallSuper
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        dialogMarginHorizontal = resources.getDimensionPixelSize(R.dimen.baseDialogFragment_marginHorizontal_default)
        dialogMarginVertical = resources.getDimensionPixelSize(R.dimen.baseDialogFragment_marginVertical_default)
    }

    final override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        viewScopeDisposables = CompositeDisposable()

        val view = inflater
            .inflate(contentLayoutId, container, false)
            .apply { replaceEmptyBackground() }

        dialog?.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog?.window?.requestFeature(Window.FEATURE_NO_TITLE)

        return view
    }

    override fun onResume() {
        adaptDialogSize(
            widthAdaptationType = dialogWidthAdaptationType,
            heightAdaptationType = dialogHeightAdaptationType
        )
        super.onResume()
    }

    @CallSuper
    override fun onDestroyView() {
        super.onDestroyView()
        viewScopeDisposables.dispose()
    }

    private fun View.replaceEmptyBackground(){
        if(background == null){
            setDefaultBackground()
        }
    }

    private fun View.setDefaultBackground(){
        setBackgroundResource(R.drawable.drawable_dialog_rounded_corners)
        clipToOutline = true
    }

    private fun adaptDialogSize(
        widthAdaptationType: DialogDimensionAdaptationType,
        heightAdaptationType: DialogDimensionAdaptationType,
    ){
        adaptDialogWidthSize(widthAdaptationType)
        adaptDialogHeightSize(heightAdaptationType)
    }

    private fun adaptDialogWidthSize(
        widthAdaptationType: DialogDimensionAdaptationType
    ){
        if(widthAdaptationType == DialogDimensionAdaptationType.MAX_SIZE){
            dialogWidth = calculateMaxDialogWidth()
        } else{
            val maxDialogWidth = calculateMaxDialogWidth()
            if(dialogWidth > maxDialogWidth){
                dialogWidth = maxDialogWidth
            }
        }
    }

    private fun adaptDialogHeightSize(
        heightAdaptationType: DialogDimensionAdaptationType
    ){
        val maxDialogHeight = calculateMaxDialogHeight()

        if(heightAdaptationType == DialogDimensionAdaptationType.MAX_SIZE){
            dialogHeight = maxDialogHeight
        } else{
            if(dialogHeight > maxDialogHeight){
                dialogHeight = maxDialogHeight
            }
        }
    }

    private fun setDialogDimensionSize(size: Int, setWidth: Boolean = true){
        val params = dialog?.window?.attributes?.apply {
            if(setWidth){
                width = size
            } else{
                height = size
            }
        } as WindowManager.LayoutParams

        dialog?.window?.attributes = params
    }

    private fun getDialogDimensionSize(getWidth: Boolean = true): Int{
        return dialog!!.window!!.attributes!!.run {
            when(getWidth){
                true -> width
                false -> height
            }
        }
    }

    private fun calculateMaxDialogWidth(): Int{
        val windowWidth = requireActivity().getWindowSize().first
        return windowWidth - dialogMarginHorizontal
    }

    private fun calculateMaxDialogHeight(): Int{
        val windowHeight = requireActivity().getWindowSize().second
        return windowHeight - dialogMarginVertical
    }

    enum class DialogDimensionAdaptationType{
        MAX_SIZE,
        WRAP_CONTENT
    }
}