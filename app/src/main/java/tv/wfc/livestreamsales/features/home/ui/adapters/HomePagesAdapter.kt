package tv.wfc.livestreamsales.features.home.ui.adapters

import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import tv.wfc.livestreamsales.features.myorders.ui.MyOrdersFragment
import tv.wfc.livestreamsales.features.mainpage.ui.MainPageFragment
import tv.wfc.livestreamsales.features.notifications.ui.NotificationsFragment
import tv.wfc.livestreamsales.features.profile.ui.ProfileFragment

class HomePagesAdapter(fragment: Fragment): FragmentStateAdapter(fragment){
    override fun getItemCount(): Int = 4

    override fun createFragment(position: Int): Fragment {
        return when(position){
            0 -> MainPageFragment()
            1 -> NotificationsFragment()
            2 -> MyOrdersFragment()
            else -> ProfileFragment()
        }
    }
}