package it.unipi.booknetapi.controller.notification;


import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import it.unipi.booknetapi.command.notification.*;
import it.unipi.booknetapi.dto.notification.NotificationEmbedResponse;
import it.unipi.booknetapi.dto.notification.NotificationResponse;
import it.unipi.booknetapi.model.user.Role;
import it.unipi.booknetapi.service.auth.AuthService;
import it.unipi.booknetapi.service.notification.NotificationService;
import it.unipi.booknetapi.shared.lib.authentication.UserToken;
import it.unipi.booknetapi.shared.model.PageResult;
import it.unipi.booknetapi.shared.model.PaginationRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/notification")
@Tag(name = "Notification", description = "Notification endpoints")
public class NotificationController {

    private final AuthService authService;
    private final NotificationService notificationService;

    public NotificationController(
            AuthService authService,
            NotificationService notificationService
    ) {
        this.authService = authService;
        this.notificationService = notificationService;
    }


    @GetMapping
    @Operation(summary = "Get all notifications (Admin only)")
    public ResponseEntity<PageResult<NotificationResponse>> getNotifications(
            @RequestHeader("Authorization") String token,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            @RequestParam(required = false) Boolean read
    ) {
        UserToken userToken = this.authService.getUserToken(token);

        if(userToken == null || userToken.getRole() != Role.Admin) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        PaginationRequest paginationRequest = PaginationRequest.builder()
                .page(page != null ? page : 0)
                .size(size != null ? size : 10)
                .build();

        NotificationGetByUserCommand command = NotificationGetByUserCommand.builder()
                .idUser(userToken.getIdUser())
                .read(read)
                .pagination(paginationRequest)
                .userToken(userToken)
                .build();

        PageResult<NotificationResponse> result = this.notificationService.get(command);

        if(result == null) return ResponseEntity.status(HttpStatus.NOT_FOUND).build();

        return ResponseEntity.ok(result);
    }

    @GetMapping("/latest")
    @Operation(summary = "Get latest notifications (Admin only)")
    public ResponseEntity<List<NotificationEmbedResponse>> getLatestNotifications(@RequestHeader("Authorization") String token) {
        UserToken userToken = this.authService.getUserToken(token);

        if(userToken == null || userToken.getRole() != Role.Admin) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        NotificationLastestCommand command = NotificationLastestCommand.builder()
                .idUser(userToken.getIdUser())
                .userToken(userToken)
                .build();

        List<NotificationEmbedResponse> notifications = this.notificationService.get(command);

        if(notifications == null) return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        return ResponseEntity.ok(notifications);
    }

    @GetMapping("/{idNotification}")
    @Operation(summary = "Get Notification information (Admin only)")
    public ResponseEntity<NotificationResponse> getNotification(
            @RequestHeader("Authorization") String token,
            @PathVariable("idNotification") String idNotification
    ) {
        UserToken userToken = this.authService.getUserToken(token);

        if(userToken == null || userToken.getRole() != Role.Admin) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        NotificationGetCommand command = NotificationGetCommand.builder()
                .id(idNotification)
                .userToken(userToken)
                .build();

        NotificationResponse response = this.notificationService.get(command);
        if(response == null) return ResponseEntity.status(HttpStatus.NOT_FOUND).build();

        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{idNotification}")
    @Operation(summary = "Delete Notification (Admin only)")
    public ResponseEntity<String> deleteNotification(
            @RequestHeader("Authorization") String token,
            @PathVariable("idNotification") String idNotification
    ) {
        UserToken userToken = this.authService.getUserToken(token);

        if(userToken == null || userToken.getRole() != Role.Admin) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        NotificationDeleteCommand command = NotificationDeleteCommand.builder()
                .id(idNotification)
                .userToken(userToken)
                .build();

        boolean result = this.notificationService.delete(command);

        return ResponseEntity.ok(result ? "Notification deleted successfully" : "Error deleting notification");
    }

    @PostMapping("/delete")
    @Operation(summary = "Delete multi notifications (Admin only)")
    public ResponseEntity<String> deleteMultiNotifications(@RequestBody List<String> ids, @RequestHeader("Authorization") String token) {
        UserToken userToken = authService.getUserToken(token);

        if(userToken == null || userToken.getRole() != Role.Admin) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        NotificationIdsDeleteCommand command = NotificationIdsDeleteCommand.builder()
                .idUser(userToken.getIdUser())
                .ids(ids)
                .userToken(userToken)
                .build();

        boolean result = this.notificationService.delete(command);

        return ResponseEntity.ok(result ? "Notifications deleted successfully" : "Errors deleting notification");
    }

}
