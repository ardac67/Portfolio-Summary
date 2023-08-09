package org.example;
import io.github.cdimascio.dotenv.Dotenv;
import io.vertx.core.CompositeFuture;
import io.vertx.core.Future;
import io.vertx.core.MultiMap;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.client.WebClient;
import java.util.HashMap;


public class Handlers {
    private WebClient client;
    private  HashMap<String,String> legacyMap;

    private Requests request;
    private JsonObject error;
    public  Handlers(WebClient client){
        request = new Requests(client);
        this.client=client;
        //code -> another code mapping
        legacyMap= new HashMap<>();
        //getting legacy codes when Handlers initialized
        getLegacyCode();
        error= new JsonObject();
        error.put("detail","not known");
    }
    public void detailedReport(RoutingContext routingContext) {
        try {
            //query parameters related stuff
            MultiMap queryParams=routingContext.queryParams();
            boolean getPortfolio_id=queryParams.contains("portfolio_id");
            boolean isFilled= ( queryParams.contains("startDate") && queryParams.contains("endDate") );
            boolean isEqual= queryParams.get("startDate").equals(queryParams.get("endDate"));
            boolean isCurrFilled= queryParams.contains("curr");
            //querying single request if the startDate and endDate are equal
            if(isEqual && (isFilled && getPortfolio_id)){
                    String portfolio_id=queryParams.get("portfolio_id");
                    //passing start date as currency date
                    Future<JsonObject> future = request.getSingle(queryParams.get("startDate"),portfolio_id);
                    future.onComplete(ar -> {
                        if (ar.failed()) {
                            routingContext.response().setStatusCode(500).end(error.encodePrettily());
                        } else {
                            //"Get daily report" data is main data
                            JsonObject mainData = future.result();
                            //optional currency added or not
                            String optionalCurrencies= "";
                            if(isCurrFilled){
                               optionalCurrencies= queryParams.get("curr");
                            }
                            //making the calculations according to the currencies of those days
                            Future<JsonObject> legacyCodes = request.getCurrencies(mainData, legacyMap, queryParams.get("startDate"),optionalCurrencies);
                            legacyCodes.onComplete(ar1 -> {
                                if (ar1.failed()) {
                                    routingContext.response().setStatusCode(400).end(new JsonObject().put("detail","no currency for that date").encodePrettily());
                                } else {
                                    routingContext.response().end(legacyCodes.result().encodePrettily());
                                }
                            });
                        }
                    });
            }
            //querying multiple report which is "Get Daily Report"
            else if(!isEqual && (isFilled && getPortfolio_id)){
                String portfolio_id=queryParams.get("portfolio_id");
                //making separated futures which will be composed together in further part of the function
                Future<JsonObject> futureStart = request.getSingle(queryParams.get("startDate"),portfolio_id);
                Future<JsonObject> futureEnd = request.getSingle(queryParams.get("endDate"),portfolio_id);
                //creating reporter object to making calculations
                Reporter tempReport= new Reporter();
                //composing all futures
                //"future start" is the start date of the report "future end" is the end date of the report
                CompositeFuture.all(futureStart,futureEnd).onComplete(ar->{
                    if(ar.succeeded()){
                        //getting all futures in jsonObject
                        JsonObject startData= futureStart.result();
                        JsonObject endData=futureEnd.result();
                        String optionalCurrencies= "";
                        //checking if there is any optional currency parameter provided
                        if(isCurrFilled){
                            optionalCurrencies= queryParams.get("curr");
                        }
                        //another future composition for the calculations
                        Future<JsonObject> startDateCalc = request.getCurrencies(startData, legacyMap, queryParams.get("startDate"),optionalCurrencies);
                        Future<JsonObject> endDateCalc = request.getCurrencies(endData, legacyMap, queryParams.get("endDate"),optionalCurrencies);
                        CompositeFuture.all(startDateCalc,endDateCalc).onComplete(res->{
                            if(res.succeeded()){
                                //returning computed result as provided "Daily Report Start" and "Daily Report End"
                                JsonObject result=tempReport.getResultForMultiple(startDateCalc.result(),endDateCalc.result());

                                routingContext.response().end(result.encodePrettily());
                            }
                            else{
                                routingContext.response().setStatusCode(400).end("no currency for that date");
                            }
                        });
                    }
                    else{
                        routingContext.response().setStatusCode(500).end(error.encodePrettily());
                    }
                });
            }
            else{
                routingContext.response().setStatusCode(400).end("bad request");
            }

        }
        catch (Exception ex){
            routingContext.response().setStatusCode(500).end(error.encodePrettily());
        }
    }
    public void getLegacyCode(){
        //trying to get legacy code for one time to check currency codes are valid or not
        try {
            Dotenv dotenv = Dotenv.load();
            client.get(dotenv.get("LEGACYURL"), dotenv.get("LEGACYREQ"))
                    .send()
                    .onSuccess(response -> {
                                JsonArray jsonLegacyArray = response.bodyAsJsonArray();
                                for (int j = 0; j < jsonLegacyArray.size(); j++) {
                                    String legacyCode = jsonLegacyArray.getJsonObject(j).getString("legacyCode");
                                    String id = jsonLegacyArray.getJsonObject(j).getString("_id");
                                    legacyMap.put(id, legacyCode);
                                }
                            }
                    )
                    .onFailure(response ->
                            System.out.println("Error happened when making request")
                    );
        }
        catch(Exception ex){
            System.out.println(ex.getMessage());
        }
    }
}
