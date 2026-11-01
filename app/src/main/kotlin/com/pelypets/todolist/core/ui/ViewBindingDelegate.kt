package com.pelypets.todolist.core.ui

import android.view.View
import androidx.fragment.app.Fragment
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.viewbinding.ViewBinding
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty

/**
 * `private val binding by viewBinding(FragmentXBinding::bind)`: created on first access after `onCreateView`
 * and dropped in `onDestroyView`, so a fragment on the back stack does not leak its views.
 */
fun <T : ViewBinding> Fragment.viewBinding(bind: (View) -> T): ReadOnlyProperty<Fragment, T> =
    object : ReadOnlyProperty<Fragment, T>, DefaultLifecycleObserver {
        private var binding: T? = null

        override fun getValue(thisRef: Fragment, property: KProperty<*>): T {
            binding?.let { return it }
            val viewLifecycle = thisRef.viewLifecycleOwner.lifecycle
            return bind(thisRef.requireView()).also {
                binding = it
                viewLifecycle.addObserver(this)
            }
        }

        override fun onDestroy(owner: LifecycleOwner) {
            owner.lifecycle.removeObserver(this)
            binding = null
        }
    }
