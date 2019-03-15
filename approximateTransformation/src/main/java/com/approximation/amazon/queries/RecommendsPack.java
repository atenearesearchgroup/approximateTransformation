package com.approximation.amazon.queries;

import static org.apache.tinkerpop.gremlin.structure.Column.keys;
import static org.apache.tinkerpop.gremlin.structure.Column.values;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import org.apache.tinkerpop.gremlin.process.traversal.P;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.__;
import org.apache.tinkerpop.gremlin.structure.Direction;
import org.apache.tinkerpop.gremlin.structure.Edge;
import org.apache.tinkerpop.gremlin.structure.Vertex;
import org.apache.tinkerpop.gremlin.tinkergraph.structure.TinkerGraph;

/**
 * Class RecommendsPack
 */
public class RecommendsPack implements Runnable {
	private TinkerGraph graph;
	private Boolean creation;
	private int hops;

	private final static Logger LOGGER = Logger.getLogger(RecommendsPack.class.getName());

	/**
	 * Query RecommendsPack for spatial approximation
	 * @param graph    stores Source Model
	 * @param creation creation flag
	 * @param hops     for spatial approximation
	 */
	public RecommendsPack(TinkerGraph graph, Boolean creation, int hops) {
		this.graph = graph;
		this.creation = creation;
		this.hops = hops;
	}

	public void run() {
		int i = 0;
		// initial query
		List<Map<String, Object>> matches = graph.traversal().V().hasLabel("Product").as("product1")
				.repeat(__.in("isRelatedTo").where(P.neq("product1")).as("x").loops().as("priority").select("x"))
				.times(hops).emit().as("product2").in("contains").in("orders").as("customer1")
				.not(__.select("product1").outE("offer").inV().where(P.eq("customer1"))).select("customer1", "product1")
				.groupCount().unfold().where(__.select(values).is(P.gte(5))).select(keys)
				.select("customer1", "product1").toList();

		while (i < 6) {

			// without creation
			if (!creation) {
				Long start = System.currentTimeMillis(); // init time
				List<Map<String, Object>> result = graph.traversal().V().hasLabel("Product").as("product1")
						.repeat(__.in("isRelatedTo").where(P.neq("product1")).as("x").loops().as("priority")
								.select("x"))
						.times(hops).emit().as("product2").in("contains").in("orders").as("customer1")
						.not(__.select("product1").outE("offer").inV().where(P.eq("customer1")))
						.select("customer1", "product1").groupCount().unfold().where(__.select(values).is(P.gte(5)))
						.select(keys).select("customer1", "product1").toList();
				Long end = System.currentTimeMillis(); // end time
				LOGGER.info(result.size() + " results for RecommendsPack in " + (end - start) + " milliseconds");

			} else {// with creation

				Long initResults = graph.traversal().V().outE("offer").count().toList().get(0);

				Long start = System.currentTimeMillis(); // init time

				graph.traversal().V().hasLabel("Product").as("product1")
						.repeat(__.in("isRelatedTo").where(P.neq("product1")).as("x").loops().as("priority")
								.select("x"))
						.times(hops).emit().as("product2").in("contains").in("orders").as("customer1")
						.not(__.select("product1").outE("offer").inV().where(P.eq("customer1")))
						.select("customer1", "product1").groupCount().unfold().where(__.select(values).is(P.gte(5)))
						.select(keys).select("customer1", "product1").addE("offer").from("product1").to("customer1")
						.property("date", System.currentTimeMillis()).property("priority", hops).iterate();

				Long end = System.currentTimeMillis(); // end time

				Long endResults = graph.traversal().V().outE("offer").count().toList().get(0);
				LOGGER.info(
						endResults - initResults + " results for RecommendsPack in " + (end - start) + " milliseconds");

				// reset graph
				for (Map<String, Object> n : matches) {
					Iterator<Edge> edgesOut = ((Vertex) n.get("product1")).edges(Direction.OUT, "offer");

					while (edgesOut.hasNext()) {
						Edge edge = edgesOut.next();
						if (edge.inVertex().equals((Vertex) n.get("customer1"))) {
							edge.remove();
						}
					}
				}

			}
			i++;
		}

	}
}