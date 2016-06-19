package de.qabel.qabelbox.navigation;

import android.app.Fragment;
import android.content.Intent;
import android.util.Log;

import javax.inject.Inject;

import de.qabel.core.config.Contact;
import de.qabel.core.config.Identity;
import de.qabel.desktop.repository.ContactRepository;
import de.qabel.desktop.repository.IdentityRepository;
import de.qabel.desktop.repository.exception.EntityNotFoundExcepion;
import de.qabel.desktop.repository.exception.PersistenceException;
import de.qabel.qabelbox.R;
import de.qabel.qabelbox.activities.CreateAccountActivity;
import de.qabel.qabelbox.activities.MainActivity;
import de.qabel.qabelbox.contacts.view.ContactsFragment;
import de.qabel.qabelbox.fragments.AboutLicencesFragment;
import de.qabel.qabelbox.fragments.ContactChatFragment;
import de.qabel.qabelbox.fragments.ContactFragment;
import de.qabel.qabelbox.fragments.HelpMainFragment;
import de.qabel.qabelbox.fragments.IdentitiesFragment;
import de.qabel.qabelbox.storage.navigation.AbstractNavigation;
import de.qabel.qabelbox.ui.views.ChatFragment;

public class MainNavigator extends AbstractNavigator implements Navigator {

    public static final String TAG_CONTACT_CHAT_FRAGMENT = "TAG_CONTACT_CHAT_FRAGMENT";
    public static final String TAG_FILES_FRAGMENT = "TAG_FILES_FRAGMENT";
    public static final String TAG_CONTACT_LIST_FRAGMENT = "TAG_CONTACT_LIST_FRAGMENT";
    public static final String TAG_ABOUT_FRAGMENT = "TAG_ABOUT_FRAGMENT";
    public static final String TAG_HELP_FRAGMENT = "TAG_HELP_FRAGMENT";
    public static final String TAG_MANAGE_IDENTITIES_FRAGMENT = "TAG_MANAGE_IDENTITIES_FRAGMENT";
    public static final String TAG_FILES_SHARE_INTO_APP_FRAGMENT = "TAG_FILES_SHARE_INTO_APP_FRAGMENT";

    private static final String TAG = "MainNavigator";

    @Inject
    MainActivity activity;

    @Inject
    IdentityRepository identityRepository;

    @Inject
    ContactRepository contactRepository;

    @Inject
    Identity activeIdentity;

    @Inject
    public MainNavigator(MainActivity activity,
                         IdentityRepository identityRepository,
                         ContactRepository contactRepository,
                         Identity activeIdentity) {
        this.activity = activity;
        this.identityRepository = identityRepository;
        this.contactRepository = contactRepository;
        this.activeIdentity = activeIdentity;
    }

    @Override
    public void selectCreateAccountActivity() {
        Intent intent = new Intent(activity, CreateAccountActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_TASK_ON_HOME);
        activity.startActivity(intent);
        activity.finish();
    }

    /*
        FRAGMENT SELECTION METHODS
    */
    @Override
    public void selectManageIdentitiesFragment() {
        try {
            showMainFragment(IdentitiesFragment.newInstance(identityRepository.findAll()),
                    TAG_MANAGE_IDENTITIES_FRAGMENT);
        } catch (PersistenceException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void selectChatFragment(String activeContact) {
        if (activeContact == null) {
            return;
        }
        try {
            Contact contact = contactRepository.findByKeyId(activeIdentity, activeContact);
            Log.d(TAG, "Selecting chat with  contact " + contact.getAlias());
            showFragment(activity, ChatFragment.Companion.withContact(contact), TAG_CONTACT_CHAT_FRAGMENT, true, true);
        } catch (EntityNotFoundExcepion entityNotFoundExcepion) {
            Log.w(TAG, "Could not find contact " + activeContact);
        }
    }

    @Override
    public void selectContactsFragment() {
        showMainFragment(new ContactsFragment(), TAG_CONTACT_LIST_FRAGMENT);
    }

    @Override
    public void selectHelpFragment() {
        showMainFragment(new HelpMainFragment(), TAG_HELP_FRAGMENT);
    }

    @Override
    public void selectAboutFragment() {
        showMainFragment(AboutLicencesFragment.newInstance(), TAG_ABOUT_FRAGMENT);
    }

    @Override
    public void selectFilesFragment() {
        activity.filesFragment.navigateBackToRoot();
        activity.filesFragment.setIsLoading(false);
        showMainFragment(activity.filesFragment, TAG_FILES_FRAGMENT);
    }

    private void showMainFragment(Fragment fragment, String tag) {
        showFragment(activity, fragment, tag, false, true);
        activity.handleMainFragmentChange();
    }


}
