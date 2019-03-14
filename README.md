# Overview

The increase in the amount of information produced by existing data sources requires the efficient processing of data flows in real time. Due to the large volume of data, applications that need to process this information to make informed decisions and to detect situations of interest usually impose strong requirements on resources.  

This repository provides the implementation and the results of all the experiments for the case study described in our paper entitled "Trading Accuracy for Performance in Data Processing Applications" [1], which proposes approximate solutions to process large amounts of information flows in order to reduce the amount of data to be processed and estimate the accuracy of this solutions based on the type of queries and the distribution of data. The accuracy is defined in terms of the precision and recall of the results obtained. 

# Case Study

Consider a simplified version of Amazon ordering service to identify some situations of interest: 

* Q1. CreateAdCampaign: if a product has been ordered more than 1000 times during an advertising campaign period and a relationship *isPublicized* between the product and the campaign does not exist, then the query creates it.

The results of the experiments run with this query are shown [here](docs/query1.md).


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

* Q2. UnpopularStock: it returns all products that have been ordered by less than 3 customers last month. 

The results of the experiments run with this query are shown [here](docs/query2.md).

```
graph.traversal().V().hasLabel("Product").as("product").group()
.by(__.in("contains").coin(prob).in("orders").as("customer").dedup("product", "customer").count())
.unfold().where(__.select(keys).is(P.lte(3))).select(values).unfold().toList();
```

* Q3. RelatedProducts: for all products that have been ordered last month, it checks whether there is another product that was included in the same order at least 100 times. The query creates a link *isRelatedTo* between both products if it does not exist.

The results of the experiments run with this query are shown [here](docs/query3.md).

Random:

```
graph.traversal().V().hasLabel("Product").as("product1").in("contains")
.where(__.values("date").is(P.inside(initTime, endTime))).coin(prob).as("order1").out("contains").as("product2")
.where(P.neq("product1"))
.not(__.select("product1").outE("isRelatedTo").inV().where(P.eq("product2")))
.select("product1", "product2").groupCount().unfold().where(__.select(values).is(P.gte(100)))
.select(keys).addE("isRelatedTo").from("product1").to("product2").iterate();
```

Temporal:

```
graph.traversal().V().hasLabel("Product").as("product1").in("contains")
.where(__.values("date").is(P.inside(initTime, endTime))).as("order1").out("contains").as("product2")
.where(P.neq("product1"))
.not(__.select("product1").outE("isRelatedTo").inV().where(P.eq("product2")))
.select("product1", "product2").groupCount().unfold().where(__.select(values).is(P.gte(100)))
.select(keys).addE("isRelatedTo").from("product1").to("product2").iterate();
```

* Q4. OlympicGamesTrending: considering we have a Rio de Janeiro Oympic Games AdCampaign, the query obtains the products that were ordered at least 100 times in Rio de Janeiro since the beginning of August 2016 until the end of the celebration of the Olympic Games. In this case, the query adds a relationship *isPublicized* between the products and the Olympic Games campaign. 

Random:

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

Spatial:

```
graph.traversal().V().hasLabel("AdCampaign").has("name", P.eq("Olympic Games")).as("campaign").values("endDate")
.as("end").V().hasLabel("GeographicalArea").has("postcode", P.eq(24495L))
.repeat(__.out("neighbors")).times(hops).emit().as("area").dedup("area").select("area")
.in("isDestinedTo").filter(__.values("date").where(P.lte("end"))).out("contains").as("product")
.not(__.select("product").outE("isPublicized").inV().where(P.eq("campaign")))
.select("campaign", "product").groupCount().unfold().where(__.select(values).is(P.gte(100)))
.select(keys).addE("isPublicized").from("product").to("campaign").dedup().iterate();
```

* Q5. RecommendsPack: if a customer has ordered *Product1* at least 5 times in different orders in the last month and this product is related to *Product2* (*isRelated* connection), then an offer for *Product2* is created for the customer. Such an offer has a priority of 1-highest priority. If *Product1* is related to *Product3* indirectly-i.e., through an intermediate product: *Product1* is related to *ProductX*, which is related to *Product3*-, then an offer for *Product3* with priority 2 is created for the customer. In this case, we say that *Product1* is related with *Product3* in two hops. Similarly, if *Product1* is related to *ProductN* in n hops, the query would create an offer with priority n. In this query, we consider offers from priority 1 to 3.

```
graph.traversal().V().hasLabel("Product").as("product1")
.repeat(__.in("isRelatedTo").where(P.neq("product1")).as("x").loops().as("priority").select("x"))
.times(hops).emit().as("product2").in("contains").in("orders").as("customer1")
.not(__.select("product1").outE("offer").inV().where(P.eq("customer1")))
.select("customer1", "product1").groupCount().unfold().where(__.select(values).is(P.gte(5)))
.select(keys).select("customer1", "product1").addE("offer").from("product1").to("customer1")
.property("date", System.currentTimeMillis()).property("priority", hops).iterate();
```

# References

[1] Gala Barquero, Javier Troya, Antonio Vallecillo: Trading Accuracy for Performance in Data Processing Applications.
