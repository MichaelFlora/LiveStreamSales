package tv.wfc.livestreamsales.features.mainpage.ui.adapters.announcements

import android.content.Context
import android.graphics.drawable.Drawable
import android.view.View
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import coil.ImageLoader
import coil.request.Disposable
import coil.request.ImageRequest
import com.laurus.p.tools.context.getDrawableCompat
import org.joda.time.DateTime
import org.joda.time.DateTimeZone
import tv.wfc.livestreamsales.R
import tv.wfc.livestreamsales.application.model.stream.PublicStream
import tv.wfc.livestreamsales.databinding.ListItemBroadcastAnnouncementCardBinding

class AnnouncementViewHolder(
    broadcastAnnouncementPage: View,
    private val imageLoader: ImageLoader
): RecyclerView.ViewHolder(broadcastAnnouncementPage) {
    private val viewBinding = ListItemBroadcastAnnouncementCardBinding.bind(broadcastAnnouncementPage)
    private val context: Context
        get() = viewBinding.root.context

    private var announcementImageLoaderDisposable: Disposable? = null

    fun bind(stream: PublicStream){
        clearView()

        bindAnnouncementImage(stream.imageUrl)
        initializeAnnouncementTitleText(stream)
        bindAnnouncementDate(stream.startsAt)
        bindAnnouncementTime(stream.startsAt)
    }

    private fun clearView(){
        clearAnnouncementImage()
        clearAnnouncementTitleText()
        clearAnnouncementDateText()
        clearAnnouncementTimeText()
    }

    private fun clearAnnouncementImage(){
        announcementImageLoaderDisposable?.dispose()
        viewBinding.image.setImageDrawable(null)
    }

    private fun clearAnnouncementDateText(){
        viewBinding.dateText.text = ""
    }

    private fun clearAnnouncementTimeText(){
        viewBinding.timeText.text = ""
    }

    private fun bindAnnouncementImage(uri: String?){
        val context = viewBinding.image.context

        if(uri != null){
            val imageRequest = ImageRequest.Builder(context)
                .data(uri)
                .target(
                    onError = { setDefaultAnnouncementImage() },
                    onSuccess = ::setAnnouncementImageDrawable
                )
                .build()

            announcementImageLoaderDisposable = imageLoader.enqueue(imageRequest)
        } else{
            setDefaultAnnouncementImage()
        }
    }

    private fun setAnnouncementImageDrawable(drawable: Drawable){
        viewBinding.image.apply{
            scaleType = ImageView.ScaleType.CENTER_CROP
            setImageDrawable(drawable)
        }
    }

    private fun setDefaultAnnouncementImage(){
        viewBinding.image.apply{
            scaleType = ImageView.ScaleType.FIT_CENTER
            val defaultDrawable = createDefaultAnnouncementImage()

            setImageDrawable(defaultDrawable)
        }
    }

    private fun createDefaultAnnouncementImage(): Drawable? {
        return context.getDrawableCompat(
            R.drawable.ic_baseline_live_tv_24,
            R.color.broadcastAnnouncementItem_image_placeholderTint
        )
    }

    private fun initializeAnnouncementTitleText(stream: PublicStream){
        viewBinding.announcementTitleText.text = stream.title
    }

    private fun clearAnnouncementTitleText(){
        viewBinding.announcementTitleText.text = ""
    }

    private fun bindAnnouncementDate(dateTime: DateTime?){
        viewBinding.dateText.text = if(dateTime != null){
            getFormattedDate(dateTime)
        } else ""
    }

    private fun getFormattedDate(dateTime: DateTime): String{
        val currentTimeZone = DateTimeZone.getDefault()
        return dateTime.withZone(currentTimeZone).toString("dd.MM.yyyy")
    }

    private fun bindAnnouncementTime(dateTime: DateTime?) {
        val timeText = viewBinding.timeText
        val context = timeText.context

        timeText.text = if(dateTime != null){
            getFormattedTime(context, dateTime)
        } else ""
    }

    private fun getFormattedTime(context: Context, dateTime: DateTime): String{
        val currentTimeZone = DateTimeZone.getDefault()
        val time = dateTime.withZone(currentTimeZone).toString("HH:mm")
        return context.resources.getString(R.string.item_live_broadcast_page_time, time)
    }
}