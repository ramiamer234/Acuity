package com.acuitybotting.website.dashboard.views.connections.launchers;

import com.acuitybotting.db.arango.acuity.rabbit_db.domain.GsonRabbitDocument;
import com.acuitybotting.db.arango.acuity.rabbit_db.service.RabbitDbService;
import com.acuitybotting.website.dashboard.DashboardRabbitService;
import com.acuitybotting.website.dashboard.components.general.list_display.InteractiveList;
import com.acuitybotting.website.dashboard.security.view.interfaces.UsersOnly;
import com.acuitybotting.website.dashboard.views.RootLayout;
import com.acuitybotting.website.dashboard.views.connections.ConnectionsTabNavComponent;
import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.spring.annotation.SpringComponent;
import com.vaadin.flow.spring.annotation.UIScope;

import java.util.Set;
import java.util.stream.Collectors;

/**
 * Created by Zachary Herridge on 8/8/2018.
 */
@Route(value = "connections/launchers", layout = RootLayout.class)
public class LaunchersListView extends VerticalLayout implements UsersOnly {

    private LauncherListComponent launcherListComponent;

    public LaunchersListView(LauncherListComponent launcherListComponent, ConnectionsTabNavComponent connectionsTabNavComponent) {
        this.launcherListComponent = launcherListComponent;
        setPadding(false);
        add(connectionsTabNavComponent, launcherListComponent);
    }

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        launcherListComponent.load();
    }

    @SpringComponent
    @UIScope
    private static class LauncherListComponent extends InteractiveList<GsonRabbitDocument> {

        private final RabbitDbService rabbitDbService;
        private final DashboardRabbitService rabbitService;

        public LauncherListComponent(RabbitDbService rabbitDbService, DashboardRabbitService rabbitService) {
            this.rabbitDbService = rabbitDbService;
            this.rabbitService = rabbitService;
            getControls().add(new Button("Launch Client"));
            withColumn("ID", "33%", document -> new Span(), (document, span) -> span.setText(document.getSubKey()));
            withColumn("Host", "33%", document -> new Span(), (document, span) -> span.setText(String.valueOf(document.getHeaders().getOrDefault("peerHost", ""))));
            withColumn("Last Update", "33%", document -> new Span(), (document, span) -> span.setText(String.valueOf(document.getHeaders().getOrDefault("connectionConfirmationTime", ""))));
            withLoad(GsonRabbitDocument::getSubKey, this::loadLaunchers);
        }

        private Set<GsonRabbitDocument> loadLaunchers() {
            return rabbitDbService
                    .loadByGroup(RabbitDbService.buildQueryMap(UsersOnly.getCurrentPrincipalUid(), "services.registered-connections", "connections"), GsonRabbitDocument.class)
                    .stream()
                    .filter(connection -> connection.getSubKey().startsWith("ABL_") && (boolean) connection.getHeaders().getOrDefault("connected", false))
                    .collect(Collectors.toSet());
        }
    }
}