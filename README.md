# Trading Accuracy for Performance 

This repository provides the implementation and the results of all the experiments for the case study described in our paper entitled "Trading Accuracy for Performance in Data Processing Applications" [1].

# Case Study

Consider a simplified version of Amazon ordering service to identify some situations of interest: 

* Q1. CreateAdCampaign: if a product has been ordered more than 1000 times during an advertising campaign period and a relationship *isPublicized* between the product and the campaign does not exist, then the query creates it.

Gremlin query for Random approximation can be viewed following:

```
graph.traversal().V().hasLabel("AdCampaign").as("campaign").values("initDate").as("initial")
.select("campaign").values("endDate").as("end").V().hasLabel("Order").coin(prob).as("order")
.filter(__.values("date").where(P.lte("end")))
.and(__.values("date").where(P.gte("initial"))).select("order").out("contains")
.as("product").not(__.select("product").outE("isPublicized").inV().where(P.eq("campaign")))
.select("campaign", "product").groupCount().unfold()
.where(__.select(values).is(P.gte(1000))).select(keys).addE("isPublicized").from("product")
.to("campaign").iterate();
```
The results of the experiments run with this query are shown [<em>in this page</em>](docs/query1.md).

* Q2. UnpopularStock: it returns all products that have been ordered by less than 3 customers last month. 


Gremlin query for Random approximation can be viewed following:

```
graph.traversal().V().hasLabel("Product").as("product").group()
.by(__.in("contains").coin(prob).in("orders").as("customer").dedup("product", "customer").count())
.unfold().where(__.select(keys).is(P.lte(3))).select(values).unfold().toList();
```
The results of the experiments run with this query are shown [<em>in this page</em>](docs/query2.md).

* Q3. RelatedProducts: for all products that have been ordered last month, it checks whether there is another product that was included in the same order at least 100 times. The query creates a link *isRelatedTo* between both products if it does not exist.

Gremlin query for Random approximation can be viewed following:

```
graph.traversal().V().hasLabel("Product").as("product1").in("contains")
.where(__.values("date").is(P.inside(initTime, endTime))).coin(prob).as("order1").out("contains").as("product2")
.where(P.neq("product1"))
.not(__.select("product1").outE("isRelatedTo").inV().where(P.eq("product2")))
.select("product1", "product2").groupCount().unfold().where(__.select(values).is(P.gte(100)))
.select(keys).addE("isRelatedTo").from("product1").to("product2").iterate();
```

Gremlin query for Temporal approximation can be viewed following:

```
graph.traversal().V().hasLabel("Product").as("product1").in("contains")
.where(__.values("date").is(P.inside(initTime, endTime))).as("order1").out("contains").as("product2")
.where(P.neq("product1"))
.not(__.select("product1").outE("isRelatedTo").inV().where(P.eq("product2")))
.select("product1", "product2").groupCount().unfold().where(__.select(values).is(P.gte(100)))
.select(keys).addE("isRelatedTo").from("product1").to("product2").iterate();
```

The results of the experiments run with this query are shown [<em>in this page</em>](docs/query3.md).

* Q4. OlympicGamesTrending: considering we have a Rio de Janeiro Oympic Games AdCampaign, the query obtains the products that were ordered at least 100 times in Rio de Janeiro since the beginning of August 2016 until the end of the celebration of the Olympic Games. In this case, the query adds a relationship *isPublicized* between the products and the Olympic Games campaign. 

Gremlin query for Random approximation can be viewed following:

```
graph.traversal().V().hasLabel("AdCampaign").has("name", P.eq("Olympic Games")).as("campaign").values("endDate").as("end")
.V().hasLabel("GeographicalArea").coin(prob)
.has("postcode", P.inside(20000L, 28990L)).as("area").in("isDestinedTo")
.filter(__.values("date").where(P.lte("end")))
.out("contains").as("product")
.not(__.select("product").outE("isPublicized").inV().where(P.eq("campaign")))
.select("campaign", "product").groupCount().unfold().where(__.select(values).is(P.gte(100)))
.select(keys).addE("isPublicized").from("product").to("campaign").iterate();
```

Gremlin query for Spatial approximation can be viewed following:

```
graph.traversal().V().hasLabel("AdCampaign").has("name", P.eq("Olympic Games")).as("campaign").values("endDate")
.as("end").V().hasLabel("GeographicalArea").has("postcode", P.eq(24495L))
.repeat(__.out("neighbors")).times(hops).emit().as("area").dedup("area").select("area")
.in("isDestinedTo").filter(__.values("date").where(P.lte("end"))).out("contains").as("product")
.not(__.select("product").outE("isPublicized").inV().where(P.eq("campaign")))
.select("campaign", "product").groupCount().unfold().where(__.select(values).is(P.gte(100)))
.select(keys).addE("isPublicized").from("product").to("campaign").dedup().iterate();
```
The results of the experiments run with this query are shown [<em>in this page</em>](docs/query4.md).

* Q5. RecommendsPack: if a customer has ordered *Product1* at least 5 times in different orders in the last month and this product is related to *Product2* (*isRelated* connection), then an offer for *Product2* is created for the customer. Such an offer has a priority of 1-highest priority. If *Product1* is related to *Product3* indirectly-i.e., through an intermediate product: *Product1* is related to *ProductX*, which is related to *Product3*-, then an offer for *Product3* with priority 2 is created for the customer. In this case, we say that *Product1* is related with *Product3* in two hops. Similarly, if *Product1* is related to *ProductN* in n hops, the query would create an offer with priority n. In this query, we consider offers from priority 1 to 3.

Gremlin query for Random approximation can be viewed following:

```
graph.traversal().V().hasLabel("Product").as("product1")
.repeat(__.in("isRelatedTo").where(P.neq("product1")).as("x").loops().as("priority").select("x"))
.times(hops).emit().as("product2").in("contains").in("orders").as("customer1")
.not(__.select("product1").outE("offer").inV().where(P.eq("customer1")))
.select("customer1", "product1").groupCount().unfold().where(__.select(values).is(P.gte(5)))
.select(keys).select("customer1", "product1").addE("offer").from("product1").to("customer1")
.property("date", System.currentTimeMillis()).property("priority", hops).iterate();
```

The results of the experiments run with this query are shown [<em>in this page</em>](docs/query5.md).

# Running the case study

This section gives instructions about how to run the source code in this repository.

## Requirements/Dependencies

   * Eclipse IDE (tested with Eclipse version 2018-09 (4.9.0)).
   * Java 8
   
## Configuration and execution

In order to run the case study, the reader has to follow the following steps:

1. Import Java projects into a workspace.

2. Create a folder named 'models' in approximateTransformation/src/main/resources.

3. Download the Source Model files from [here](https://drive.google.com/open?id=1rO2VVCQagcRIitwCWn9aQoNwR4E5T_t4) and copy them into the created folder.

4. Open file 'config.properties' located in approximateTransformation/src/main/resources. This file contains the configuration to run the experiments. It is divided into six parts:

    * Configuration parameters: change the property 'file' to indicate the Source Model to be loaded.
  
    * Q1 - CreateAdCampaign parameters: change 'q1' value to *true* for executing CreateAdCampaign query. Choose 'prob1' value among 0 to 1 to indicate the probability for the random approximation.
  
    * Q2 - UnpopularStock parameters: change 'q2' value to *true* for executing UnpopularStock query. Choose 'prob2' value among 0 to 1 to indicate the probability for the random approximation.
   
    * Q3 - RelatedProducts parameters: Change 'q3Random' value to *true* for executing RelatedProducts query with random approximation and choose 'prob3' value among 0 to 1 to indicate the probability. On the other hand, change 'q3Temporal' value to *true* and 'window3' value among 0 to 30 to indicate the temporal period for temporal approximation.
  
    * Q4 - OlympicGamesTrending parameters: Change 'q4Random' value to *true* for executing OlympicGamesTrending query with random approximation and choose 'prob4' value among 0 to 1 to indicate the probability. On the other hand, change 'q4Spatial' value to *true* and 'hops4' value among 100 to 900 to indicate the number of hops for spatial approximation.
  
    * Q5 - RecommendsPack parameters: change 'q5' value to *true* for executing RecommendsPack query. Choose 'hops5' value among 1 to 3 to indicate the probability for the spatial approximation.
 
 5. Once the configuration is selected, run the file ApproximateTransformationApp.java.
 
 # Experiment Results

The results from running all queries with the different source models and applying different approximation techniques are accessible in the following:

- Results from [Q1](docs/query1.md)
- Results from [Q2](docs/query2.md)
- Results from [Q3](docs/query3.md)
- Results from [Q4](docs/query4.md) 
- Results from [Q5](docs/query5.md)
 
# References

[1] Gala Barquero, Javier Troya, Antonio Vallecillo: Trading Accuracy for Performance in Data Processing Applications.
