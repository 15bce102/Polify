package com.example.polify.ui.activity

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.util.Log
import android.view.WindowManager
import androidx.core.os.bundleOf
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.navigation.findNavController
import com.andruid.magic.game.model.data.OneVsOneBattle
import com.example.polify.R
import com.example.polify.data.ACTION_MATCH_FOUND
import com.example.polify.data.EXTRA_BATTLE
import com.example.polify.eventbus.BattleEvent
import org.greenrobot.eventbus.EventBus

class OneVsOneActivity : FullScreenActivity() {
    companion object {
        private val TAG = "${OneVsOneActivity::class.java.simpleName}Log"
    }

    private val gameReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                ACTION_MATCH_FOUND -> {
                    Log.d("cloudLog", "broadcast of match found in activity")
                    intent.extras?.let {
                        battle = it.getParcelable(EXTRA_BATTLE)!!
                        EventBus.getDefault().post(BattleEvent(battle))
                        findNavController(R.id.nav_host_fragment).navigate(
                                R.id.questionsFragment, bundleOf("battleId" to battle.battleId))
                    }
                }
            }
        }
    }

    private lateinit var battle: OneVsOneBattle

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_one_vs_one)

        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        LocalBroadcastManager.getInstance(this).registerReceiver(gameReceiver, IntentFilter(ACTION_MATCH_FOUND))
    }

    override fun onDestroy() {
        super.onDestroy()
        LocalBroadcastManager.getInstance(this).unregisterReceiver(gameReceiver)
    }
}