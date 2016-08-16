package de.qabel.qabelbox.chat.view.views

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.support.v7.widget.LinearLayoutManager
import android.view.*
import de.qabel.core.config.Contact
import de.qabel.core.config.Identity
import de.qabel.qabelbox.QblBroadcastConstants
import de.qabel.qabelbox.R
import de.qabel.qabelbox.chat.dagger.ChatModule
import de.qabel.qabelbox.chat.dto.ChatMessage
import de.qabel.qabelbox.chat.view.adapters.ChatMessageAdapter
import de.qabel.qabelbox.chat.view.presenters.ChatPresenter
import de.qabel.qabelbox.dagger.components.MainActivityComponent
import de.qabel.qabelbox.fragments.BaseFragment
import de.qabel.qabelbox.helper.Helper
import kotlinx.android.synthetic.main.fragment_contact_chat.*
import kotlinx.android.synthetic.main.fragment_contact_chat.view.*
import org.jetbrains.anko.*
import javax.inject.Inject

class ChatFragment : ChatView, BaseFragment(), AnkoLogger {

    override var messageText: String
        get() = view?.etText?.text.toString()
        set(value) {
            view?.etText?.setText(value)
        }


    override val isFabNeeded: Boolean = false

    var injectCompleted = false

    companion object {
        val ARG_CONTACT = "CONTACT"

        fun withContact(contact: Contact): ChatFragment {
            val fragment = ChatFragment()
            fragment.arguments = with(Bundle()) {
                putString(ARG_CONTACT, contact.keyIdentifier)
                this
            }
            return fragment
        }
    }

    val adapter = ChatMessageAdapter(listOf())

    lateinit override var contactKeyId: String
    @Inject
    lateinit var presenter: ChatPresenter
    @Inject
    lateinit var identity: Identity

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        contactKeyId = arguments.getString(ARG_CONTACT) ?: throw IllegalArgumentException(
                "Starting ChatFragment without contactKeyId")
        val component = getComponent(MainActivityComponent::class.java).plus(ChatModule(this))
        component.inject(this)
        injectCompleted = true
        bt_send.setOnClickListener { presenter.sendMessage() }

        configureAsSubFragment()

        val layoutManager = LinearLayoutManager(view.context)
        layoutManager.stackFromEnd = true
        contact_chat_list.layoutManager = layoutManager
        contact_chat_list.adapter = adapter

    }

    /**
     * Block notifications in which only the currently active contact
     * and identity are involved.
     */
    private val notificationBlockReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (isOrderedBroadcast) {
                val ids = intent.getStringArrayListExtra(Helper.AFFECTED_IDENTITIES_AND_CONTACTS)
                        ?.filterNotNull() ?: return

                val currentKeys = listOf(contactKeyId, identity.keyIdentifier)
                if (ids.containsAll(currentKeys)) {
                    if (injectCompleted) {
                        presenter.refreshMessages()
                    }
                    if (ids.all { currentKeys.contains(it) }) {
                        abortBroadcast()
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        presenter.refreshMessages()
        ctx.registerReceiver(notificationBlockReceiver, IntentFilter(QblBroadcastConstants.Chat.NOTIFY_NEW_MESSAGES).apply {
            priority = 1
        })
        refreshContactOverlay()
        mActivity?.toolbar?.setOnClickListener { presenter.handleHeaderClick() }
    }

    override fun refreshContactOverlay() {
        chat_contact_toolbar?.visibility = if (presenter.showContactMenu) View.VISIBLE else View.GONE
    }

    override fun onPause() {
        super.onPause()
        ctx.unregisterReceiver(notificationBlockReceiver)

        mActivity?.toolbar?.isClickable = false
    }

    override fun onCreateView(inflater: LayoutInflater?, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        return inflater?.inflate(R.layout.fragment_contact_chat, container, false)
                ?: throw IllegalStateException("Could not create view")
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        action_add_contact.setOnClickListener { presenter.handleContactAddClick() }
        action_ignore_contact.setOnClickListener { presenter.handleContactIgnoreClick() }
    }

    override fun showEmpty() {
        busy()
        onUiThread {
            adapter.messages = listOf()
            adapter.notifyDataSetChanged()
            idle()
        }
    }

    override fun showMessages(messages: List<ChatMessage>) {
        debug("Showing ${messages.size} messages")
        busy()
        onUiThread {
            fillAdapter(messages)
            idle()
        }
    }

    override fun appendMessage(message: ChatMessage) {
        busy()
        onUiThread {
            adapter.messages = adapter.messages + message
            adapter.notifyDataSetChanged()
            scrollToBottom()
            idle()
        }
    }

    private fun scrollToBottom() {
        contact_chat_list.scrollToPosition(adapter.itemCount - 1)
    }

    private fun fillAdapter(messages: List<ChatMessage>) {
        debug("Filling adapter with ${messages.size} messages")
        adapter.messages = messages
        adapter.notifyDataSetChanged()
        scrollToBottom()
    }

    override val title: String by lazy {
        if (injectCompleted) presenter.title else ""
    }

    override val subtitle: String? by lazy {
        if (injectCompleted && !presenter.subtitle.isEmpty()) presenter.subtitle else null
    }

    override fun supportBackButton(): Boolean = true

    override fun sendMessageStateChange() {
        ctx.sendBroadcast(Intent(QblBroadcastConstants.Chat.MESSAGE_STATE_CHANGED))
    }

    override fun showError(error: Throwable) {
        onUiThread {
            longToast(getString(R.string.error_saving_changed))
            error("Error in ChatFragment", error)
            refreshContactOverlay()
        }
    }

}
