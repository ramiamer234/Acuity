package com.acuitybotting.website.dashboard.views.resources.accounts;

import com.acuitybotting.db.arangodb.repositories.resources.accounts.domain.RsAccount;
import com.acuitybotting.website.dashboard.components.general.fields.UserMasterPasswordField;
import com.acuitybotting.website.dashboard.components.general.separator.TitleSeparator;
import com.acuitybotting.website.dashboard.security.view.interfaces.Authed;
import com.acuitybotting.db.arangodb.repositories.resources.accounts.service.AccountsService;
import com.acuitybotting.website.dashboard.utils.Authentication;
import com.acuitybotting.website.dashboard.utils.Components;
import com.acuitybotting.website.dashboard.utils.Notifications;
import com.acuitybotting.website.dashboard.views.RootLayout;
import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.PasswordField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.BeforeEvent;
import com.vaadin.flow.router.HasUrlParameter;
import com.vaadin.flow.router.OptionalParameter;
import com.vaadin.flow.router.Route;

/**
 * Created by Zachary Herridge on 8/14/2018.
 */
@Route(value = "resources/accounts/edit", layout = RootLayout.class)
public class AccountEditView extends VerticalLayout implements Authed, HasUrlParameter<String> {

    private final AccountsService accountsService;
    private final UserMasterPasswordField masterPasswordField;

    private RsAccount accountInfo;
    private String accountEmail;

    public AccountEditView(AccountsService accountsService, UserMasterPasswordField masterPasswordField) {
        this.accountsService = accountsService;
        this.masterPasswordField = masterPasswordField;
    }

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        if (accountEmail != null) {
            accountInfo = accountsService.findAccount(Authentication.getAcuityPrincipalId(), accountEmail).orElse(null);
            if (accountInfo == null) {
                getUI().ifPresent(ui -> ui.navigate(AccountsListView.class));
                Notifications.error("Failed to find account.");
                return;
            }
        } else {
            accountInfo = new RsAccount();
        }

        if (attachEvent.isInitialAttach()) {
            add(new TitleSeparator((accountEmail == null ? "Add" : "Edit") + " Account"));

            TextField emailField = Components.textField("Email", "Email", () -> accountInfo.get_key());
            PasswordField passwordField = new PasswordField("Password");
            add(emailField, passwordField, masterPasswordField);

            add(Components.button(VaadinIcon.PLUS_CIRCLE, event -> {
                boolean save = accountsService.updatePassword(Authentication.getAcuityPrincipalId(), emailField.getValue(), masterPasswordField.encrypt(passwordField.getValue()));

                if (save) getUI().ifPresent(ui -> ui.navigate(AccountsListView.class));
                else Notifications.error("Failed to updatePassword account.");
            }));
        }
    }

    @Override
    public void setParameter(BeforeEvent beforeEvent, @OptionalParameter String accountEmail) {
        this.accountEmail = accountEmail;
    }
}
