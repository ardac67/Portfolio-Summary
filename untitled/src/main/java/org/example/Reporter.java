package org.example;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import java.util.HashMap;

public class Reporter{
    //creating report for single object
    //mainData is the data which taken from "Get Daily Report"
    //currencies is the currency values of the portfolio ticker
    public JsonObject createReport(JsonObject mainData, JsonObject currencies, HashMap<String,String> legacyMap,String optionalCurrency){
        JsonObject header = new JsonObject()
                .put("portfolio_id", mainData.getString("portfolio_id"))
                .put("portfolilo_name", mainData.getString("portfolilo_name"));

        JsonArray bodyArray = new JsonArray(); // Create a JsonArray to store "body" objects

        //iterating through Assets array to get necessary parts
        for (int i = 0; i < mainData.getJsonArray("assets").size(); i++) {
            JsonObject body = new JsonObject();
            body.put("currency", mainData.getJsonArray("assets").getJsonObject(i).getString("currency"))
                    .put("ticker", mainData.getJsonArray("assets").getJsonObject(i).getString("ticker"))
                .put("asset_id",mainData.getJsonArray("assets").getJsonObject(i).getString("asset_id"));
            //calculation for profits for one document based
            double calculation = calculateProfit(
                    mainData.getJsonArray("assets").getJsonObject(i).getDouble("soldAmount"),
                    mainData.getJsonArray("assets").getJsonObject(i).getDouble("boughtAmount"),
                    mainData.getJsonArray("assets").getJsonObject(i).getDouble("heldQuantity"),
                    currencies.getJsonArray(legacyMap.get(mainData.getJsonArray("assets").getJsonObject(i)
                                    .getString("symbol_id"))).getJsonObject(0)
                            .getDouble("c")
            );
            //checking anotherCurrency added into parameters if not skip it's calculations
            if(!optionalCurrency.isEmpty()){
                if(!(mainData.getJsonArray("assets").getJsonObject(i).getString("ticker").equals(optionalCurrency))
                    && !(mainData.getJsonArray("assets").getJsonObject(i).getString("currency").equals(optionalCurrency))
                )
                {
                    String tempCurr=mainData.getJsonArray("assets").getJsonObject(i).getString("ticker")+"/"+optionalCurrency;
                    double optCurrency=currencies.getJsonArray(tempCurr).getJsonObject(0).getDouble("c");
                    double otherCalculation=calculation / optCurrency;
                    body.put("otherCurrency",otherCalculation);
                }
            }
            body.put("profit", calculation);
            bodyArray.add(body);
        }

        header.put("Body", bodyArray);

        return header;
    }
    //calculating profit based on a formula
    private double calculateProfit(double soldAmount,double boughtAmount,double heldQuantity,double closeValue){
            return soldAmount-boughtAmount+( heldQuantity*closeValue );
    }
    //it is used for the two document based report
    //startCalculation and endCalculation is return value from createReport one by one
    public JsonObject getResultForMultiple(JsonObject startCalculation, JsonObject endCalculation){
        JsonObject result= new JsonObject();
        //asset_id it's, value mapping
        HashMap<String,Double> asset_profit= new HashMap<>();
        //asset_id and it's value for another currency if it is added
        HashMap<String,Double> asset_profit_for_otherCurr= new HashMap<>();
        JsonArray bodyArray= startCalculation.getJsonArray("Body");
        //getting profit from asset_id
        for(int i=0;i<bodyArray.size();i++){
            asset_profit.put(bodyArray.getJsonObject(i).getString("asset_id"),bodyArray.getJsonObject(i).getDouble("profit"));
            //if anotherCurrency added get it's profit from asset_id
            if(bodyArray.getJsonObject(i).containsKey("otherCurrency")) {
                asset_profit_for_otherCurr.put(bodyArray.getJsonObject(i).getString("asset_id"), bodyArray.getJsonObject(i).getDouble("otherCurrency"));
            }
        }
        bodyArray=endCalculation.getJsonArray("Body");
        JsonArray tempArray =new JsonArray();
        for(int i=0;i<bodyArray.size();i++){
            JsonObject temp = new JsonObject();
            //making necessary calculations
            //subtracting profit values
            double startProfit=bodyArray.getJsonObject(i).getDouble("profit");
            double endProfit=asset_profit.get(bodyArray.getJsonObject(i).getString("asset_id"));
            //subtracting profit values and getting new profits and percentages
            //if any additional currency added make calculation for it also
            if(bodyArray.getJsonObject(i).containsKey("otherCurrency")){
                double profitForStart=bodyArray.getJsonObject(i).getDouble("otherCurrency");
                double profitForEnd=asset_profit_for_otherCurr.get(bodyArray.getJsonObject(i).getString("asset_id"));
                double result_otherCurr=profitForStart-profitForEnd;
                double percentage_for_other_Curr= (( profitForStart-profitForEnd )/profitForStart)*100;
                temp.put("profit_with_other_curr",result_otherCurr);
                temp.put("percentage_for_other_Curr",percentage_for_other_Curr);
            }
            //getting new profits and percentages
            double calculate= startProfit-endProfit;
            double percentage= (( startProfit-endProfit )/startProfit)*100;
            temp.put("asset_id",bodyArray.getJsonObject(i).getString("asset_id"));
            temp.put("ticker",bodyArray.getJsonObject(i).getString("ticker"));
            temp.put("currency",bodyArray.getJsonObject(i).getString("currency"));
            temp.put("profit",displayDoubleWithPrecision(calculate));
            temp.put("percentage",displayDoubleWithPrecision(percentage));
            tempArray.add(temp);
        }
        result.put("result",tempArray);
        return result;
    }
    public double displayDoubleWithPrecision(double value) {
        int precision=4;
        double factor = Math.pow(10, precision);
        return Math.round(value * factor) / factor;
    }
}
