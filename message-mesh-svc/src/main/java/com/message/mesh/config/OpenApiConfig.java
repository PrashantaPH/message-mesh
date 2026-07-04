package com.message.mesh.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import io.swagger.v3.oas.models.tags.Tag;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * Central OpenAPI (Swagger) definition for the Message Mesh chat service.
 *
 * <p>Exposes interactive documentation at {@code /swagger-ui.html} and the raw
 * specification at {@code /v3/api-docs}. A global JWT bearer security scheme is
 * declared so that protected endpoints can be exercised directly from Swagger UI
 * via the <em>Authorize</em> dialog.</p>
 */
@Configuration
public class OpenApiConfig {

    /** Name of the reusable security scheme referenced by protected operations. */
    public static final String BEARER_SCHEME = "bearerAuth";

    @Value("${spring.application.name:message-mesh-svc}")
    private String applicationName;

    @Value("${server.port:8080}")
    private String serverPort;

    @Bean
    public OpenAPI messageMeshOpenAPI() {
        return new OpenAPI()
                .info(apiInfo())
                .servers(List.of(
                        new Server()
                                .url("http://localhost:" + serverPort)
                                .description("Local development server"),
                        new Server()
                                .url("https://message-mesh-dev.onrender.com")
                                .description("Development server")
                ))
                .tags(List.of(
                        new Tag()
                                .name("Admin")
                                .description("""
                                        Administrative endpoints (require the ADMIN role): user \
                                        management (create, role/status changes, password reset, session \
                                        revocation, delete), conversation and message moderation \
                                        (soft-delete/restore), platform statistics, and the audit trail."""),
                        new Tag()
                                .name("Authentication")
                                .description("""
                                        Public endpoints for account registration and login. \
                                        Successful calls return a signed JWT used to authorize every \
                                        other request and the STOMP WebSocket handshake."""),
                        new Tag()
                                .name("Users")
                                .description("""
                                        Look up and update the authenticated profile (display name, \
                                        password, account deletion), discover other users, and query \
                                        real-time presence (online roster)."""),
                        new Tag()
                                .name("Conversations")
                                .description("""
                                        Create and list conversations, fetch message history with \
                                        sequence-based pagination, list conversation members (roster), and \
                                        acknowledge read receipts."""),
                        new Tag()
                                .name("Messages")
                                .description("""
                                        Per-message actions: edit or soft-delete a message, and add \
                                        or remove emoji reactions. Updated messages are re-broadcast to \
                                        the conversation over STOMP.""")
                ))
                .addSecurityItem(new SecurityRequirement().addList(BEARER_SCHEME))
                .components(new Components().addSecuritySchemes(BEARER_SCHEME, bearerScheme()));
    }

    private Info apiInfo() {
        return new Info()
                .title("Message Mesh API")
                .version("1.0.0")
                .description("""
                        Scalable real-time chat backend.

                        ## Overview
                        Message Mesh combines a stateless REST API for resource management with a
                        STOMP-over-WebSocket channel for live messaging, typing indicators, delivery
                        acknowledgements, and presence updates.

                        ## Authentication
                        1. Register via `POST /api/auth/register` or sign in via `POST /api/auth/login`.
                        2. Both return a JWT in the `token` field.
                        3. Send it as `Authorization: Bearer <token>` on every protected REST request,
                           and as a connect header on the `/ws` WebSocket handshake.
                        4. In Swagger UI, click **Authorize** and paste the raw token (no `Bearer ` prefix).

                        ## Roles
                        Every user has a platform role (`USER` or `ADMIN`). The `/api/admin/**` endpoints
                        require the `ADMIN` role; other endpoints only require a valid JWT. Sensitive
                        endpoints (auth, admin) are rate limited.

                        ## Real-time channel (not covered by this spec)
                        STOMP destinations are handled over WebSocket and therefore are not part of the
                        OpenAPI document. Connect at `/ws` (SockJS) with the JWT as a connect header.

                        Client → server (`/app` prefix):
                        - `SEND /app/chat.send` — publish a message (supports `parentId` for threaded replies)
                        - `SEND /app/chat.ack` — acknowledge delivery/read of a message
                        - `SEND /app/chat.typing` — broadcast typing state

                        Server → client (broadcast topics):
                        - `SUBSCRIBE /topic/conv.{id}` — receive messages for a conversation (new, edited, deleted, reactions)
                        - `SUBSCRIBE /topic/conv.{id}.typing` — receive typing indicators for a conversation
                        - `SUBSCRIBE /topic/conv.{id}.meta` — receive conversation meta events (rename, member add/remove, role change, delete)
                        - `SUBSCRIBE /topic/presence` — receive presence (online/offline) updates

                        Server → client (user-scoped queues, `/user` prefix):
                        - `SUBSCRIBE /user/queue/ack` — receive personal delivery/read acknowledgements
                        - `SUBSCRIBE /user/queue/conversations` — receive newly created conversations
                        """)
                .contact(new Contact()
                        .name("Message Mesh Platform Team")
                        .email("platform@message-mesh.dev.com")
                        .url("https://message-mesh.dev.com"))
                .license(new License()
                        .name("Apache License 2.0")
                        .url("https://www.apache.org/licenses/LICENSE-2.0"));
    }

    private SecurityScheme bearerScheme() {
        return new SecurityScheme()
                .name(BEARER_SCHEME)
                .type(SecurityScheme.Type.HTTP)
                .scheme("bearer")
                .bearerFormat("JWT")
                .description("JWT bearer token obtained from the authentication endpoints. "
                        + "Paste only the raw token value; Swagger UI adds the 'Bearer ' prefix.");
    }
}
