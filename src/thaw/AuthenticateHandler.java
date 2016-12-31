package thaw;

import io.vertx.core.Handler;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.Session;
import thaw.sql.SQLHandler;
import thaw.utils.Utils;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static thaw.REST.*;

/**
 * Created by nakaze on 30/12/16.
 */
public class AuthenticateHandler implements Handler<RoutingContext> {
    private SQLHandler sqlHandler;
    private ArrayList<Session> connectedUsers = new ArrayList<>();

    public AuthenticateHandler(SQLHandler sqlHandler) {
        this.sqlHandler = sqlHandler;
    }

    public void handle(RoutingContext routingContext) {
        HttpServerResponse response = routingContext.response();
        Session session = routingContext.session();

        String[] arguments = getArguments(routingContext.request(), "username", "password");

        if (sendErrorIfEmpty(response, arguments))
            return;

        List<Map<String, String>> list;
        try {
            list = resultSetToList(sqlHandler.getUser(arguments), "username", "password");
        } catch (SQLException e) {
            routingContext.session().destroy();
            response.putHeader("content-type", "application/json").setStatusCode(405).setStatusMessage("Database error.").end();
            return;
        }

        if (list.size() != 1) {
            routingContext.session().destroy();
            response.putHeader("content-type", "application/json").setStatusCode(402).setStatusMessage("This user doesn't exist.").end();
        } else if (!Utils.checkPassword(list.get(0).get("password"), arguments[1])) {
            routingContext.session().destroy();
            response.putHeader("content-type", "application/json").setStatusCode(403).setStatusMessage("Wrong password").end();
        } else {
            session.put("username", arguments[0]);
            connectedUsers.add(session);
            response.putHeader("content-type", "application/json").end();
        }
    }

    public void logout(RoutingContext routingContext) {
        HttpServerResponse response = routingContext.response();
        HttpServerRequest request = routingContext.request();

        String username = request.getParam("username");

        if (username == null || username.isEmpty()) {
            response.setStatusCode(400).setStatusMessage("No parameters for unlogging").end();
            return;
        }

        if (connectedUsers.isEmpty()) {
            response.setStatusCode(401).setStatusMessage("No user logged in").end();
            throw new IllegalStateException("there is no logged user, you can't logout");
        }
        if (!connectedUsers.removeIf(s -> s.data().get("username").equals(username))) {
            response.setStatusCode(402).setStatusMessage("This user is not logged in: " + username);
            return;
        }
        routingContext.session().destroy();
        response.setStatusMessage("Successfully logged out").end();

    }

    List<Session> getAllSessions() {
        return Collections.unmodifiableList(connectedUsers);
    }
}
