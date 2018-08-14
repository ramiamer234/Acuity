package com.acuitybotting.website.dashboard.views.connections.clients;

import com.acuitybotting.data.flow.messaging.services.client.exceptions.MessagingException;
import com.acuitybotting.db.arango.acuity.rabbit_db.domain.gson.GsonRabbitDocument;
import com.acuitybotting.db.arango.acuity.rabbit_db.service.RabbitDbService;
import com.acuitybotting.website.dashboard.DashboardRabbitService;
import com.acuitybotting.website.dashboard.components.general.list_display.InteractiveList;
import com.acuitybotting.website.dashboard.security.view.interfaces.Authed;
import com.acuitybotting.website.dashboard.utils.Authentication;
import com.acuitybotting.website.dashboard.utils.Components;
import com.acuitybotting.website.dashboard.views.RootLayout;
import com.acuitybotting.website.dashboard.views.connections.ConnectionsTabNavComponent;
import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.spring.annotation.SpringComponent;
import com.vaadin.flow.spring.annotation.UIScope;

import java.util.Set;
import java.util.stream.Collectors;

/**
 * Created by Zachary Herridge on 8/8/2018.
 */
@Route(value = "connections/clients", layout = RootLayout.class)
public class ClientsListView extends VerticalLayout implements Authed {

    private ClientListComponent clientListComponent;

    public ClientsListView(ClientListComponent clientListComponent, ConnectionsTabNavComponent connectionsTabNavComponent) {
        this.clientListComponent = clientListComponent;
        setPadding(false);
        add(connectionsTabNavComponent, clientListComponent);
    }

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        clientListComponent.load();
    }

    @SpringComponent
    @UIScope
    private static class ClientListComponent extends InteractiveList<GsonRabbitDocument> {

        private final RabbitDbService rabbitDbService;
        private final DashboardRabbitService rabbitService;

        public ClientListComponent(RabbitDbService rabbitDbService, DashboardRabbitService rabbitService) {
            this.rabbitDbService = rabbitDbService;
            this.rabbitService = rabbitService;

            withColumn("ID", "33%", document -> new Span(), (document, span) -> span.setText(document.getSubKey()));
            withColumn("", "33%", document -> Components.button(VaadinIcon.CLOSE, event -> kill(document.getSubKey())), (document, button) -> {});

            withLoad(GsonRabbitDocument::getSubKey, this::loadClients);
        }

        private void kill(String id){
            String queue = "user." + Authentication.getAcuityPrincipalId() + ".queue." + id;
            try {
                rabbitService.getRabbitChannel().buildMessage(
                        "",
                        queue,
                        "{}"
                ).setAttribute("killConnection", "").send();
            } catch (MessagingException e) {
                e.printStackTrace();
            }
        }

        private Set<GsonRabbitDocument> loadClients() {
            return rabbitDbService
                    .loadByGroup(RabbitDbService.buildQueryMap(Authentication.getAcuityPrincipalId(), "services.registered-connections", "connections"), GsonRabbitDocument.class)
                    .stream()
                    .filter(connection -> connection.getSubKey().startsWith("RPC_") && (boolean) connection.getHeaders().getOrDefault("connected", false))
                    .collect(Collectors.toSet());
        }
    }
}
