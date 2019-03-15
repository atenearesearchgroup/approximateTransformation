package com.approximation.amazon.queries;

import static org.apache.tinkerpop.gremlin.structure.Column.keys;
import static org.apache.tinkerpop.gremlin.structure.Column.values;

import java.util.Collection;
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
 * Class RelatedProductsRandom
 */
public class RelatedProductsRandom implements Runnable {

	private TinkerGraph graph;
	private Boolean creation;
	private Double prob;
	private Long windowI;
	private Long windowE;

	private final static Logger LOGGER = Logger.getLogger(RelatedProductsRandom.class.getName());

	/**
	 * Query RelatedProducts for random approximation
	 * 
	 * @param graph stores Source Model
	 * 
	 * @param creation creation flag
	 * 
	 * @param prob probability for random approximation
	 * @param windowI temporal window starting
	 * @param windowE temporal window ending
	 * 
	
	 */

	public RelatedProductsRandom(TinkerGraph graph, Boolean creation, Double prob, Long windowI, Long windowE) {
		this.graph = graph;
		this.creation = creation;
		this.prob = prob;
		this.windowE = windowE;
		this.windowI = windowI;
	}

	public void run() {
		int i = 0;

		// initial query
		List<Map<String, Object>> matches = graph.traversal().V().hasLabel("Product").as("product1").in("contains")
				.where(__.values("date").is(P.inside(windowI,windowE))).as("order1").out("contains").as("product2")
				.where(P.neq("product1")).not(__.select("product1").outE("isRelatedTo").inV().where(P.eq("product2")))
				.select("product1", "product2").groupCount().unfold().where(__.select(values).is(P.gte(100)))
				.select(keys).select("product1", "product2").toList();

		while (i < 6) {

			// without creation
			if (!creation) {
				System.out.println("RelatedProduct");
				Long start = System.currentTimeMillis(); // init time
				List<Collection<Object>> result = graph.traversal().V().hasLabel("Product").as("product1")
						.in("contains").where(__.values("date").is(P.inside(windowI,windowE))).coin(prob).as("order1")
						.out("contains").as("product2").where(P.neq("product1"))
						.not(__.select("product1").outE("isRelatedTo").inV().where(P.eq("product2")))
						.select("product1", "product2").groupCount().unfold().where(__.select(values).is(P.gte(100)))
						.select(keys).toList();
				Long end = System.currentTimeMillis(); // end time

				LOGGER.info(result.size() + " results for RelatedProducts in " + (end - start) + " milliseconds");

			} else { // with creation

				Long initResults = graph.traversal().V().hasLabel("Product").inE("isRelatedTo").count().toList().get(0);
				Long start = System.currentTimeMillis(); // init time

				graph.traversal().V().hasLabel("Product").as("product1").in("contains")
						.where(__.values("date").is(P.inside(windowI,windowE))).coin(prob).as("order1").out("contains")
						.as("product2").where(P.neq("product1"))
						.not(__.select("product1").outE("isRelatedTo").inV().where(P.eq("product2")))
						.select("product1", "product2").groupCount().unfold().where(__.select(values).is(P.gte(100)))
						.select(keys).addE("isRelatedTo").from("product1").to("product2").iterate();

				Long end = System.currentTimeMillis(); // end time

				Long endResults = graph.traversal().V().hasLabel("Product").inE("isRelatedTo").count().toList().get(0);

				LOGGER.info(endResults - initResults + " results for RelatedProducts in " + (end - start)
						+ " milliseconds");

				// reset graph
				for (Map<String, Object> n : matches) {
					Iterator<Edge> edgesOut = ((Vertex) n.get("product1")).edges(Direction.OUT, "isRelatedTo");
					Iterator<Edge> edgesIn = ((Vertex) n.get("product1")).edges(Direction.IN, "isRelatedTo");
					while (edgesOut.hasNext()) {
						Edge edge = edgesOut.next();
						if (edge.inVertex().equals((Vertex) n.get("product2"))) {
							edge.remove();
						}

					}
					while (edgesIn.hasNext()) {
						Edge edge = edgesIn.next();
						if (edge.outVertex().equals((Vertex) n.get("product2"))) {
							edge.remove();
						}

					}

				}

			}
			i++;
		}

	}

}
