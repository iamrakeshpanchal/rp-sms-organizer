package com.rpsms.org.adapters

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.rpsms.org.fragments.AllMessagesFragment
import com.rpsms.org.fragments.OTPFragment
import com.rpsms.org.fragments.PromotionalFragment
import com.rpsms.org.fragments.GroupsFragment

class ViewPagerAdapter(fragmentActivity: FragmentActivity) : 
    FragmentStateAdapter(fragmentActivity) {
    
    override fun getItemCount(): Int = 4
    
    override fun createFragment(position: Int): Fragment {
        return when(position) {
            0 -> AllMessagesFragment()
            1 -> OTPFragment()
            2 -> PromotionalFragment()
            3 -> GroupsFragment()
            else -> AllMessagesFragment()
        }
    }
}
