package org.example;

import io.github.cdimascio.dotenv.Dotenv;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.client.HttpRequest;
import io.vertx.ext.web.client.WebClient;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

public class Requests {
    private final WebClient client;
    public Requests(WebClient client){
        this.client=client;
    }
    //getting "Daily report"
    public Future<JsonObject> getSingle(String date,String portfolio_id){
        Dotenv dotenv=Dotenv.load();
        Promise<JsonObject> promise= Promise.promise();
        //making request for getting data from api
        client.get(dotenv.get("HOST"),dotenv.get("REQ"))
                .basicAuthentication(dotenv.get("user"),dotenv.get("pass"))
                .putHeader("app-name",dotenv.get("idk"))
                .addQueryParam("portfolio_id",portfolio_id)
                .addQueryParam("date",date)
                .send()
                .onSuccess(response->
                        {
                            if(response.statusCode()==200){
                                JsonObject getSingle=response.bodyAsJsonObject();
                                promise.complete(getSingle);
                            }
                           else{
                               promise.fail("error");
                            }
                        }
                )
                .onFailure( response->
                        promise.fail("error")
                );
        return promise.future();
    }
    //getting currency values of related data for "Daily Report"
    //mainData which returns from getSingle() function
    //legacyMap is the hashMap for mapping codes for legacyCodes
    public Future<JsonObject> getCurrencies(JsonObject mainData,HashMap<String,String> legacyMap,String date,String optionalCurrencies){
        Promise<JsonObject> promise = Promise.promise();
        try {
            Dotenv dotenv = Dotenv.load();
            DateTimeBuilder builder = new DateTimeBuilder();
            List<String> legacyCode = new ArrayList<>();
            JsonArray assetArray = mainData.getJsonArray("assets");
            for (int i = 0; i < assetArray.size(); i++) {
                // * eur/trl something added here
                legacyCode.add(legacyMap.get(assetArray.getJsonObject(i).getString("symbol_id")));
                //if any optionalCurrency is used then check it if it can match base currency or portfolio currency
                if (!(assetArray.getJsonObject(i).getString("ticker").equals(optionalCurrencies)) && !optionalCurrencies.isEmpty()
                        && !(assetArray.getJsonObject(i).getString("currency").equals(optionalCurrencies))) {
                    legacyCode.add(assetArray.getJsonObject(i).getString("ticker") + "/" + optionalCurrencies);
                }
            }
            //making currency request
            String newDate = builder.returnFormattedWithTime(date) + "000000";
            HttpRequest<Buffer> request = client.get(dotenv.get("CHOST"), dotenv.get("CREQ")
                            + newDate +
                            dotenv.get("ADD") +
                            newDate)
                    .putHeader("resource", dotenv.get("RES"))
                    .putHeader("company", dotenv.get("CMPY"))
                    .basicAuthentication(dotenv.get("user1"), dotenv.get("pass1"));
            for (String s : legacyCode) {
                request.addQueryParam("c", s);
            }
            request.send()
                    .onSuccess(ctx -> {
                        JsonObject currencyJson = ctx.bodyAsJsonObject();
                        Set<String> fields = currencyJson.fieldNames();
                        List<String> fieldList = new ArrayList<>(fields);
                        //checking currency request returned empty or not
                        boolean controlEmpytiness = true;
                        for (String s : fieldList) {
                            if (currencyJson.getJsonArray(s).isEmpty()) {
                                controlEmpytiness = false;
                                break;
                            }
                        }
                        if (!controlEmpytiness) {
                            promise.fail("no valid currencies");
                        } else {
                            Reporter report = new Reporter();
                            //create report of the observedData
                            promise.complete(report.createReport(mainData, currencyJson, legacyMap, optionalCurrencies));
                        }

                    });
            return promise.future();
        }
        catch (Exception ex){
            promise.fail("error");
            return promise.future();
        }
    }

}
