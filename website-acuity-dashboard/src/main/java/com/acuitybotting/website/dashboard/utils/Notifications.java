package com.acuitybotting.website.dashboard.utils;

import com.vaadin.flow.component.notification.Notification;

public class Notifications {

    public static Notification display(String message){
        return Notification.show(message, 3000, Notification.Position.TOP_END);
    }

    public static void error(String message) {

    }
}
