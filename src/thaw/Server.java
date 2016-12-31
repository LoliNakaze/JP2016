package thaw;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.core.net.JksOptions;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.CookieHandler;
import io.vertx.ext.web.handler.SessionHandler;
import io.vertx.ext.web.handler.sockjs.BridgeOptions;
import io.vertx.ext.web.handler.sockjs.PermittedOptions;
import io.vertx.ext.web.handler.sockjs.SockJSHandler;
import io.vertx.ext.web.sstore.LocalSessionStore;

import java.sql.SQLException;
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

    private Server() throws SQLException {
        rest = new REST();
    }

    @Override
    public void start() {
        Router router = Router.router(vertx);

        router.route().handler(CookieHandler.create());
        router.route().handler(SessionHandler.create(LocalSessionStore.create(vertx)));

        vertxEventBusRun(router);

        rest.allowRequest(router);

        // route to JSON REST APIs
        rest.routeSetup(router);


        // For HTTPS
        HttpServerOptions httpsOpt = new HttpServerOptions().setUseAlpn(false).setSsl(true).setKeyStoreOptions(new JksOptions().setPath("keystore.jks").setPassword("password"));
        vertx.createHttpServer(httpsOpt).requestHandler(router::accept).listen(8080);
        vertxEventBusRun();
        System.out.println("listen on port 8080");
    }

    private void vertxEventBusRun(Router router) {
        // Allow events for the designated addresses in/out of the event bus bridge
        BridgeOptions opts = new BridgeOptions()
                .addInboundPermitted(new PermittedOptions().setAddress("chat.to.server"))
                .addInboundPermitted(new PermittedOptions().setAddress("log.to.server"))
                .addInboundPermitted(new PermittedOptions().setAddress("unlog.to.server"))
                .addOutboundPermitted(new PermittedOptions().setAddressRegex("log.to.client"))
                .addOutboundPermitted(new PermittedOptions().setAddressRegex("chat.to.client.+"));

        // Create the event bus bridge and add it to the router.
        SockJSHandler ebHandler = SockJSHandler.create(vertx).bridge(opts);
        router.route("/eventbus/*").handler(ebHandler);
    }

    private void vertxEventBusRun() {
        EventBus eb = vertx.eventBus();

        // Register to listen for messages coming IN to the server
        eb.consumer("chat.to.server").handler(message -> {
            // Create a timestamp string
            String timestamp = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss").format(Date.from(Instant.now()));
            // Send the message back out to all clients with the timestamp prepended.
            eb.publish("chat.to.client." + message.body().toString().split("__::__")[0], timestamp + "__::__" + message.body());
        });

        eb.consumer("log.to.server").handler(message -> {
           eb.publish("log.to.client", "log:" + message.body());
        });

        eb.consumer("unlog.to.server").handler(message -> {
            eb.publish("log.to.client", "unlog:" + message.body());
        });
    }

}
