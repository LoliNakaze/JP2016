package thaw;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.EventBus;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.sockjs.BridgeOptions;
import io.vertx.ext.web.handler.sockjs.PermittedOptions;
import io.vertx.ext.web.handler.sockjs.SockJSHandler;

import java.sql.SQLException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.Date;

// java
// --add-exports java.base/sun.nio.ch=ALL-UNNAMED
// --add-exports java.base/sun.net.dns=ALL-UNNAMED
// Server
public class Server extends AbstractVerticle {
    private final REST rest;

    public static void main(String[] args) throws ClassNotFoundException, SQLException {
        // development option, avoid caching to see changes of
        // static files without having to reload the application,
        // obviously, this line should be commented in production
        //System.setProperty("vertx.disableFileCaching", "true");
        Class.forName("org.sqlite.JDBC");

        Vertx vertx = Vertx.vertx();
        vertx.deployVerticle(new Server());
    }

    Server() throws SQLException {
        rest = new REST();
    }

    @Override
    public void start() {
        Router router = Router.router(vertx);

        // Allow events for the designated addresses in/out of the event bus bridge
        BridgeOptions opts = new BridgeOptions()
                .addInboundPermitted(new PermittedOptions().setAddress("chat.to.server"))
                .addOutboundPermitted(new PermittedOptions().setAddressRegex("chat.to.client.+"));

        // Create the event bus bridge and add it to the router.
        SockJSHandler ebHandler = SockJSHandler.create(vertx).bridge(opts);
        router.route("/eventbus/*").handler(ebHandler);

        rest.allowRequest(router);

        // route to JSON REST APIs
        // Je vais voir
        rest.routeSetup(router);

        vertx.createHttpServer().requestHandler(router::accept).listen(8080);
        EventBus eb = vertx.eventBus();

        // Register to listen for messages coming IN to the server
        eb.consumer("chat.to.server").handler(message -> {
            // Create a timestamp string
            String timestamp = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss").format(Date.from(Instant.now()));
            // Send the message back out to all clients with the timestamp prepended.
            eb.publish("chat.to.client." + message.body().toString().split("__::__")[0], timestamp + "__::__" + message.body());
        });

        System.out.println("listen on port 8080");
    }

}
