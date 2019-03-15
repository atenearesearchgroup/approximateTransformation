package com.approximation.amazon.queries;

import static org.apache.tinkerpop.gremlin.structure.Column.keys;
import static org.apache.tinkerpop.gremlin.structure.Column.values;

import java.util.List;
import java.util.logging.Logger;

import org.apache.tinkerpop.gremlin.process.traversal.P;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.__;
import org.apache.tinkerpop.gremlin.tinkergraph.structure.TinkerGraph;

/**
 * Class UnpopularStock
 */

public class UnpopularStock implements Runnable {
	private TinkerGraph graph;
	private Double prob;

	private final static Logger LOGGER = Logger.getLogger(UnpopularStock.class.getName());
	/**
	 * Query UnpopularStock
	 * @param graph stores Source Model
	 * @param prob probability for random approximation
	 */
	public UnpopularStock(TinkerGraph graph, Double prob) {
		this.graph = graph;
		this.prob = prob;
	}

	public void run() {
		int i = 0;
		GraphTraversalSource g = graph.traversal();

		while (i < 6) {
			Long start = System.currentTimeMillis(); // init time
			List<Object> result = g.V().hasLabel("Product").as("product").group()
					.by(__.in("contains").coin(prob).in("orders").as("customer").dedup("product", "customer").count())
					.unfold().where(__.select(keys).is(P.lte(3))).select(values).unfold().toList();
			Long end = System.currentTimeMillis(); // end time

			LOGGER.info(result.size() + " results for UnpopularStock in " + (end - start) + " milliseconds");
			i++;
		}

	}

}
