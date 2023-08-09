package org.example;

import io.vertx.core.AbstractVerticle;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.client.WebClientOptions;
import io.vertx.ext.web.handler.BodyHandler;

public class VerticleClass extends AbstractVerticle {
    @Override
    public void start() throws Exception {
        //Dotenv dotenv= Dotenv.load();
        //create a web client to make request to report base
        WebClientOptions options = new WebClientOptions()
                .setDefaultPort(443)
                .setSsl(true);
        WebClient client = WebClient.create(vertx, options);
        //creating handler for requests
        Handlers handlers= new Handlers(client);
        //creating router for fetch
        Router router=Router.router(vertx);
        router.route().handler(BodyHandler.create());
        router.get("/detailedReport").handler(handlers::detailedReport);

        vertx.createHttpServer()
                .requestHandler(router)
                .listen(8888)
                .onSuccess(server ->
                        System.out.println(
                                "HTTP server started on port " + server.actualPort()
                        )
                );
    }
}
