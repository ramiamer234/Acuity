package com.acuitybotting.website.dashboard.views.resources.proxies;

import com.acuitybotting.db.arangodb.repositories.resources.proxies.domain.Proxy;
import com.acuitybotting.website.dashboard.components.general.list_display.InteractiveList;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.spring.annotation.SpringComponent;
import com.vaadin.flow.spring.annotation.UIScope;
import org.springframework.beans.factory.annotation.Autowired;

@SpringComponent
@UIScope
public class ProxyListComponent extends InteractiveList<Proxy> {

    @Autowired
    private ProxyListComponent() {
/*        withColumn("Host", "10%", document -> new Div(), (document, div) -> div.setText(document.getHost()));
        withColumn("Port", "10%", document -> new Div(), (document, div) -> div.setText(String.valueOf(document.getPort())));
        withColumn("Username", "10%", document -> new Div(), (document, div) -> div.setText(String.valueOf(document.getUsername())));
        withColumn("Password", "10%", document -> new Div(), (document, div) -> div.setText(document.getEncryptedPassword() != null ? "Set" : "None"));
        withColumn("", "5", "5%", proxy -> {
            Button button = new Button(VaadinIcon.EDIT.create());
            button.addClickListener(buttonClickEvent -> getUI().ifPresent(ui -> ui.navigate(ProxyEditView.class, proxy.getParent().getSubKey())));
            return button;
        }, (proxy, button) -> {
        });
        withLoad(proxy -> proxy.getParent().getSubKey(), proxiesService::loadProxies);*/

        Button add = new Button(VaadinIcon.PLUS_CIRCLE.create());
        getControls().add(add);
        add.addClickListener(buttonClickEvent -> getUI().ifPresent(ui -> ui.navigate(ProxyEditView.class)));
    }
}