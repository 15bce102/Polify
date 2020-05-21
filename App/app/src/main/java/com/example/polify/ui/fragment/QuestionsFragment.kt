package com.example.polify.ui.fragment

import android.os.Bundle
import android.os.CountDownTimer
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.andruid.magic.game.model.data.Question
import com.andruid.magic.game.model.response.Result
import com.example.polify.R
import com.example.polify.data.BATTLE_MULTIPLAYER
import com.example.polify.data.BATTLE_ONE_VS_ONE
import com.example.polify.data.BATTLE_TEST
import com.example.polify.data.QUE_TIME_LIMIT_MS
import com.example.polify.databinding.FragmentQuestionsBinding
import com.example.polify.eventbus.OptionEvent
import com.example.polify.ui.adapter.QuestionAdapter
import com.example.polify.ui.viewholder.OptionViewHolder
import com.example.polify.ui.viewmodel.BaseViewModelFactory
import com.example.polify.ui.viewmodel.QuestionViewModel
import com.example.polify.util.showConfirmationDialog
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode

class QuestionsFragment : Fragment() {
    companion object {
        private val TAG = "${QuestionsFragment::class.java.simpleName}Log"
    }

    private val questionsAdapter = QuestionAdapter()
    private val mAuth by lazy { FirebaseAuth.getInstance() }
    private val questionsViewModel by viewModels<QuestionViewModel> {
        BaseViewModelFactory {
            QuestionViewModel(battle, battleType)
        }
    }

    private val args by navArgs<QuestionsFragmentArgs>()
    private val battle by lazy { args.battle }
    private val battleType by lazy { args.battleType }
    private val startTime by lazy { args.startTime }

    private lateinit var binding: FragmentQuestionsBinding

    private var score = 0
    private var qid: String? = null
    private var selectedOptPos = -1
    private var optionsEnabled = true

    private val callback = object : OnBackPressedCallback(true) {
        override fun handleOnBackPressed() {
            lifecycleScope.launch {
                val shouldQuit = requireContext().showConfirmationDialog(R.string.title_leave_battle,
                        R.string.desc_leave_battle)
                if (!shouldQuit)
                    return@launch

                requireActivity().finish()
            }
        }
    }
    private val countDownTimer = object : CountDownTimer(QUE_TIME_LIMIT_MS, 1000) {
        override fun onFinish() {
            val pos = binding.viewPager.currentItem
            val optionsRV = binding.viewPager.findViewById<RecyclerView>(R.id.optionsRV)
            highlightAns(optionsRV, pos)

            lifecycleScope.launch {
                delay(1000)
                if (pos == questionsAdapter.itemCount - 1) {
                    finishGame()
                } else
                    binding.viewPager.setCurrentItem(pos + 1, true)
            }
        }

        override fun onTick(millisUntilFinished: Long) {}
    }

    private fun finishGame() {
        try {
            when (battleType) {
                BATTLE_ONE_VS_ONE ->
                    findNavController().navigate(QuestionsFragmentDirections
                            .actionQuestionsFragmentToResultsFragment(battle?.battleId
                                    ?: "test", score))
                BATTLE_TEST ->
                    findNavController().navigate(QuestionsFragmentDirections
                            .actionQuestionsFragmentToResultsFragment(score = score))
                BATTLE_MULTIPLAYER ->
                    findNavController().navigate(QuestionsFragmentDirections
                            .actionQuestionsFragmentToResultsFragment(battle?.battleId
                                    ?: "test", score))
            }
        } catch (e: IllegalStateException) {
            e.printStackTrace()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requireActivity().onBackPressedDispatcher.addCallback(callback)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = FragmentQuestionsBinding.inflate(inflater, container, false)

        initViewPager()
        initPlayers()

        questionsViewModel.questions.observe(viewLifecycleOwner, Observer { result ->
            if (result.status == Result.Status.SUCCESS) {
                (result.data?.questions)?.let { questions ->
                    questionsAdapter.submitList(questions)
                    binding.progressQues.max = questions.size
                    startMatch(questions)
                }
            }
        })

        return binding.root
    }

    private fun initPlayers() {
        val currentUid = mAuth.currentUser?.uid ?: return

        val players = battle?.players ?: emptyList()

        val player = players.find { player -> player.uid == currentUid }
        val remaining = players.minus(player)

        binding.apply {
            player1 = player
            player2 = remaining.getOrNull(0)
            player3 = remaining.getOrNull(1)
            player4 = remaining.getOrNull(2)
            executePendingBindings()
        }
    }

    private fun initViewPager() {
        binding.viewPager.apply {
            adapter = questionsAdapter
            isUserInputEnabled = false
            registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
                override fun onPageSelected(position: Int) {
                    super.onPageSelected(position)
                    binding.progressQues.progress = position + 1

                    qid = questionsAdapter.currentList[position].qid

                    optionsEnabled = true

                    countDownTimer.cancel()
                    countDownTimer.start()
                    binding.timerAnimView.playAnimation()
                }
            })
        }
    }

    private fun startMatch(questions: List<Question>) {
        if (startTime == -1L)
            return

        val elapsedSeconds = (System.currentTimeMillis() - startTime) / 1000
        val questionPos = elapsedSeconds / questions.size

        Log.d("cloudLog", "questionPos = $questionPos")

        if (questionPos < questions.size)
            binding.viewPager.currentItem = questionPos.toInt() - 1
        else {
            binding.timerAnimView.progress = 100F
            finishGame()
        }
    }

    override fun onResume() {
        super.onResume()
        EventBus.getDefault().register(this)
    }

    override fun onPause() {
        super.onPause()
        EventBus.getDefault().unregister(this)
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d("cloudLog", "onDestroy questions fragment")
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onOptionEvent(optionEvent: OptionEvent) {
        if (!optionsEnabled)
            return

        val (qid, opt) = optionEvent

        this.qid = qid
        Log.d("optionLog", "selected opt = ${opt.optId}")
        selectedOptPos = opt.optId[0] - 'A'

        val optionsRV = binding.viewPager.findViewById<RecyclerView>(R.id.optionsRV)
        val selectedViewHolder = optionsRV.findViewHolderForItemId(selectedOptPos.toLong()) as OptionViewHolder?
        selectedViewHolder?.highlightOption()

        optionsEnabled = false
    }

    private fun highlightAns(recyclerView: RecyclerView, pos: Int) {
        val question = questionsAdapter.currentList[pos]

        if (qid == question.qid) {
            Log.d(TAG, "selected option pos = $selectedOptPos")
            val correctPos = question.correctAnswer[0] - 'A'

            val correctViewHolder = recyclerView.findViewHolderForItemId(correctPos.toLong()) as OptionViewHolder?
            correctViewHolder?.highlightAnswer(true)

            if (selectedOptPos == -1)
                return

            if (selectedOptPos == correctPos)
                score++
            else {
                val wrongViewHolder = recyclerView.findViewHolderForItemId(selectedOptPos.toLong()) as OptionViewHolder?
                wrongViewHolder?.highlightAnswer(false)
            }

            selectedOptPos = -1
        }
    }
}