package thaw;

import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.Json;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.CorsHandler;
import io.vertx.ext.web.handler.StaticHandler;
import thaw.sql.SQLConsumer;
import thaw.sql.SQLHandler;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.joining;

/**
 * Created by nakaze on 27/12/16.
 */
class REST {
    private final SQLHandler sqlHandler;
    private final AuthenticateHandler authHandler;

    REST() throws SQLException {
        sqlHandler = SQLHandler.createSQLHandler("test");
        authHandler = new AuthenticateHandler(sqlHandler);
    }

    void allowRequest(Router router) {
        CorsHandler corsHandler = CorsHandler.create("*");
        corsHandler.allowedMethod(HttpMethod.GET);
        corsHandler.allowedMethod(HttpMethod.OPTIONS);
        corsHandler.allowedMethod(HttpMethod.POST);
        corsHandler.allowedMethod(HttpMethod.DELETE);
        corsHandler.allowedMethod(HttpMethod.PUT);
        corsHandler.allowedHeader("Content-Type");
        router.route().handler(corsHandler);
    }

    void routeSetup(Router router) {
        router.get("/api/users/").handler(r -> catchExceptions(this::getAllUsers, r));
        router.put("/api/users/:username/:password/:avatar/").handler(r -> catchExceptions(this::addUser, r));

        router.get("/api/logged/users/").handler(this::getLoggedUsers);

        router.get("/api/authenticate/:username/:password/").handler(r -> catchExceptions(authHandler::handle, r));
        router.delete("/api/authenticate/:username/").handler(r -> catchExceptions(authHandler::logout, r));

        router.put("/api/main/channels/:name/").handler(r -> catchExceptions(this::createChannel, r));
        router.get("/api/main/channels/").handler(r -> catchExceptions(this::getAllChannels, r));

        router.put("/api/main/channel/:channel/:username/:content/").handler(r -> catchExceptions(this::postInChannel, r));
        router.get("/api/main/channel/:channel/").handler(r -> catchExceptions(this::getAllInChannel, r));
        // otherwise serve static pages
        router.route().handler(StaticHandler.create());
    }

    private void getLoggedUsers(RoutingContext routingContext) {
        routingContext.response().end(authHandler.getAllSessions().stream().map(s -> Json.encodePrettily(Map.of("username", s.get("username")))).collect(Collectors.joining(", ", "[", "]")));
    }

    private static void catchExceptions(SQLConsumer<RoutingContext> consumer, RoutingContext routingContext) {
        try {
            consumer.accept(routingContext);
        } catch (SQLException e) {
            e.printStackTrace();
            routingContext.response()
                    .setStatusCode(400)
                    .setStatusMessage("Database issue.")
                    .putHeader("content-type", "application/json").end();
        }
    }



    private void createChannel(RoutingContext routingContext) throws SQLException {
        HttpServerResponse response = routingContext.response();

        String[] arguments = getArguments(routingContext.request(), "name");
        if (sendErrorIfEmpty(response, arguments))
            return;

        sqlHandler.addChannel(arguments);
        response.putHeader("content-type", "application/json").end();
    }

    // Keynames need to match with the names of the columns.
    static List<Map<String, String>> resultSetToList(ResultSet resultSet, String... keynames) throws SQLException {
        List<Map<String, String>> list = new ArrayList<>();

        while (resultSet.next()) {
            list.add(Arrays.stream(keynames).collect(Collectors.toMap(k -> k, k -> {
                try {
                    return resultSet.getString(k);
                } catch (SQLException e) {
                    throw new IllegalArgumentException("The column name \"" + k + "\" can't be found.");
                }
            })));
        }

        return list;
    }

    private void getAllChannels(RoutingContext routingContext) throws SQLException {
        routingContext.response().putHeader("content-type", "application/json").end(resultSetToList(sqlHandler.getAllChannels(), "cname").stream().map(Json::encodePrettily).collect(Collectors.joining(", ", "[", "]")));
    }

    private void getAllInChannel(RoutingContext routingContext) throws SQLException {
        HttpServerResponse response = routingContext.response();

        String[] arguments = getArguments(routingContext.request(), "channel");

        if (sendErrorIfEmpty(response, arguments))
            return;

        response.putHeader("content-type", "application/json").end(resultSetToList(sqlHandler.getAllInChannel(arguments), "id", "username", "content", "date").stream().map(Json::encodePrettily).collect(Collectors.joining(",", "[", "]")));
    }

    private void postInChannel(RoutingContext routingContext) throws SQLException {
        HttpServerResponse response = routingContext.response();

        String[] arguments = getArguments(routingContext.request(), "channel", "username", "content");

        if (sendErrorIfEmpty(response, arguments))
            return;

        sqlHandler.postInChannel(arguments);
        response.putHeader("content-type", "application/json").end();
    }

    private void addUser(RoutingContext routingContext) throws SQLException {
        // TODO GET USERS
        HttpServerResponse response = routingContext.response();

        String[] arguments = getArguments(routingContext.request(), "username", "password", "avatar");

        if (sendErrorIfEmpty(response, arguments))
            return;

        sqlHandler.addUser(arguments);
        response.putHeader("content-type", "application/json").end();
    }

    private void getAllUsers(RoutingContext routingContext) throws SQLException {
        routingContext.response()
                .putHeader("content-type", "application/json")
                .end(resultSetToList(sqlHandler.getAllUsers(), "username", "avatar").stream().map(Json::encodePrettily).collect(joining(",", "[", "]")));
    }

    static String[] getArguments(HttpServerRequest request, String... fields) {
        String[] contents = new String[fields.length];

        IntStream.range(0, fields.length).forEach(i -> contents[i] = requireNonNull(request.getParam(fields[i])));

        return contents;
    }

    static boolean sendErrorIfEmpty(HttpServerResponse response, String[] args) {
        boolean emptyArg = Arrays.stream(args).anyMatch(String::isEmpty);
        if (emptyArg)
            response.setStatusCode(401).putHeader("content-type", "application/json").end("Not a valid user");
        return emptyArg;
    }
}
