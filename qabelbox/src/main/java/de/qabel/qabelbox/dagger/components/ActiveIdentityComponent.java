package de.qabel.qabelbox.dagger.components;

import org.jetbrains.annotations.NotNull;

import dagger.Subcomponent;
import de.qabel.qabelbox.base.MainActivity;
import de.qabel.qabelbox.box.views.FolderChooserActivity;
import de.qabel.qabelbox.chat.dagger.ChatComponent;
import de.qabel.qabelbox.chat.dagger.ChatOverviewComponent;
import de.qabel.qabelbox.chat.dagger.ChatOverviewModule;
import de.qabel.qabelbox.contacts.dagger.ContactDetailsComponent;
import de.qabel.qabelbox.contacts.dagger.ContactDetailsModule;
import de.qabel.qabelbox.contacts.dagger.ContactEditComponent;
import de.qabel.qabelbox.contacts.dagger.ContactEditModule;
import de.qabel.qabelbox.contacts.dagger.ContactsComponent;
import de.qabel.qabelbox.contacts.dagger.ContactsModule;
import de.qabel.qabelbox.chat.dagger.ChatModule;
import de.qabel.qabelbox.dagger.modules.ActiveIdentityModule;
import de.qabel.qabelbox.dagger.modules.FileBrowserModule;
import de.qabel.qabelbox.dagger.modules.FileBrowserViewModule;
import de.qabel.qabelbox.dagger.modules.FolderChooserModule;
import de.qabel.qabelbox.dagger.scopes.ActivityScope;
import de.qabel.qabelbox.identity.view.IdentitiesFragment;
import de.qabel.qabelbox.identity.dagger.IdentityDetailsComponent;
import de.qabel.qabelbox.identity.dagger.IdentityDetailsModule;
import de.qabel.qabelbox.index.dagger.IndexSearchComponent;
import de.qabel.qabelbox.index.dagger.IndexSearchModule;

@ActivityScope
@Subcomponent(
        modules = ActiveIdentityModule.class
)
public interface ActiveIdentityComponent {

    void inject(MainActivity activity);

    ChatComponent plus(ChatModule chatModule);
    ChatOverviewComponent plus(ChatOverviewModule chatOverviewModule);
    FileBrowserComponent plus(FileBrowserViewModule fileBrowserViewModule);
    FolderChooserComponent plus(FolderChooserModule folderChooserModule);

    ContactsComponent plus(ContactsModule contactsModule);
    ContactDetailsComponent plus(ContactDetailsModule contactDetailsModule);
    ContactEditComponent plus(ContactEditModule contactEditModule);

    IdentityDetailsComponent plus(IdentityDetailsModule identityDetailsModule);

    IndexSearchComponent plus(IndexSearchModule indexSearchModule);

    void inject(IdentitiesFragment identitiesFragment);
}

