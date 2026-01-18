package it.unipi.booknetapi.controller.notification;


import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/notification")
@Tag(name = "Notification", description = "Notification endpoints")
public class NotificationController {

    public NotificationController() {}


    @GetMapping
    @Operation(summary = "Get all notifications with pagination")
    public ResponseEntity<List<String>> getNotifications() {


        return ResponseEntity.ok(new ArrayList<>());
    }

    @GetMapping("/latest")
    @Operation(summary = "Get latest notifications")
    public ResponseEntity<List<String>> getLatestNotifications() {


        return ResponseEntity.ok(new ArrayList<>());
    }

}
