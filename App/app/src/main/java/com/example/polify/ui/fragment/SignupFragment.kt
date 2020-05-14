package com.example.polify.ui.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import coil.api.load
import com.andruid.magic.game.server.RetrofitClient.DEFAULT_AVATAR_URL
import com.example.polify.R
import com.example.polify.databinding.FragmentSignupBinding
import com.example.polify.eventbus.AvatarEvent
import com.example.polify.ui.dialog.AvatarDialogFragment
import com.example.polify.util.isValidPhoneNumber
import com.example.polify.util.isValidUserName
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode

class SignupFragment : Fragment() {
    private lateinit var binding: FragmentSignupBinding

    private var avatarUrl = DEFAULT_AVATAR_URL

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = FragmentSignupBinding.inflate(inflater, container, false)

        initListeners()

        return binding.root
    }

    override fun onStart() {
        super.onStart()
        EventBus.getDefault().register(this)
    }

    override fun onStop() {
        super.onStop()
        EventBus.getDefault().unregister(this)
    }

    private fun initListeners() {
        binding.apply {
            imgAvatar.load(DEFAULT_AVATAR_URL)

            imgAvatar.setOnClickListener {
                val dialog = AvatarDialogFragment.getInstance()
                dialog.show(childFragmentManager, "avatarDialog")
            }

            countryCodePicker.registerCarrierNumberEditText(phoneET)

            phoneET.addTextChangedListener {
                if (!isValidPhoneNumber(countryCodePicker.fullNumberWithPlus))
                    phoneTextInput.error = getString(R.string.error_invalid_number)
                else
                    phoneTextInput.error = null
            }

            submitBtn.setOnClickListener {
                val number = countryCodePicker.fullNumberWithPlus
                val userName = userNameET.text.toString().trim()

                if (isValidPhoneNumber(number) && isValidUserName(userName))
                    findNavController().navigate(
                            SignupFragmentDirections.actionSignupFragmentToOtpFragment(number, userName, avatarUrl))
                else
                    Toast.makeText(requireContext(), R.string.error_invalid_number, Toast.LENGTH_SHORT).show()
            }

            textLogin.setOnClickListener {
                findNavController().navigate(SignupFragmentDirections.actionSignupFragmentToLoginFragment())
            }
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onAvatarEvent(avatarEvent: AvatarEvent) {
        avatarUrl = avatarEvent.avatarUrl
        binding.imgAvatar.load(avatarUrl)
    }
}