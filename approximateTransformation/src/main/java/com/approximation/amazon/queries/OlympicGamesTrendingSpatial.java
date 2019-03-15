package com.approximation.amazon.queries;

import static org.apache.tinkerpop.gremlin.structure.Column.keys;
import static org.apache.tinkerpop.gremlin.structure.Column.values;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import org.apache.tinkerpop.gremlin.process.traversal.P;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.__;
import org.apache.tinkerpop.gremlin.structure.Direction;
import org.apache.tinkerpop.gremlin.structure.Edge;
import org.apache.tinkerpop.gremlin.structure.Vertex;
import org.apache.tinkerpop.gremlin.tinkergraph.structure.TinkerGraph;
/**
 * Class OlympicGamesTrendingSpatial*/
public class OlympicGamesTrendingSpatial implements Runnable {
	private TinkerGraph graph;
	private Boolean creation;
	private int hops;

	private final static Logger LOGGER = Logger.getLogger(OlympicGamesTrendingSpatial.class.getName());

	/**
	 * Query OlympicGamesTrending for spatial approximation
	 * @param graph    stores Source Model
	 * @param creation creation flag
	 * @param hops     for spatial approximation
	 */
	public OlympicGamesTrendingSpatial(TinkerGraph graph, Boolean creation, int hops) {
		this.graph = graph;
		this.creation = creation;
		this.hops = hops;
	}

	public void run() {
		int i = 0;

		// initial query

		List<Map<String, Object>> matches = graph.traversal().V().hasLabel("AdCampaign")
				.has("name", P.eq("Olympic Games")).as("campaign").values("endDate").as("end").V()
				.hasLabel("GeographicalArea").where(__.has("postcode", P.eq(24495L))).repeat(__.out("neighbors"))
				.times(hops).emit().as("area").dedup("area").in("isDestinedTo")
				.filter(__.values("date").where(P.lte("end"))).out("contains").as("product")
				.not(__.select("product").outE("isPublicized").inV().where(P.eq("campaign")))
				.select("campaign", "product").groupCount().unfold().where(__.select(values).is(P.gte(100)))
				.select(keys).select("campaign", "product").dedup().toList();
		while (i < 6) {
			// without creation
			if (!creation) {

				Long start = System.currentTimeMillis(); // init time

				List<Map<String, Object>> result = graph.traversal().V().hasLabel("AdCampaign")
						.has("name", P.eq("Olympic Games")).as("campaign").values("endDate").as("end").V()
						.hasLabel("GeographicalArea").where(__.has("postcode", P.eq(24495L)))
						.repeat(__.out("neighbors")).times(hops).emit().as("area").dedup("area").in("isDestinedTo")
						.filter(__.values("date").where(P.lte("end"))).out("contains").as("product")
						.not(__.select("product").outE("isPublicized").inV().where(P.eq("campaign")))
						.select("campaign", "product").groupCount().unfold().where(__.select(values).is(P.gte(100)))
						.select(keys).select("campaign", "product").dedup().toList();

				Long end = System.currentTimeMillis(); // end time

				LOGGER.info(result.size() + " results for OlympicGamesTrending in " + (end - start) + " milliseconds");

			} else {// with creation
				GraphTraversalSource g = graph.traversal();

				Long initResults = graph.traversal().V().outE("isPublicized").count().toList().get(0);
				Long start = System.currentTimeMillis(); // init time

				g.V().hasLabel("AdCampaign").has("name", P.eq("Olympic Games")).as("campaign").values("endDate")
						.as("end").V().hasLabel("GeographicalArea").has("postcode", P.eq(24495L))
						.repeat(__.out("neighbors")).times(hops).emit().as("area").dedup("area").select("area")
						.in("isDestinedTo").filter(__.values("date").where(P.lte("end"))).out("contains").as("product")
						.not(__.select("product").outE("isPublicized").inV().where(P.eq("campaign")))
						.select("campaign", "product").groupCount().unfold().where(__.select(values).is(P.gte(100)))
						.select(keys).addE("isPublicized").from("product").to("campaign").dedup().iterate();

				Long end = System.currentTimeMillis(); // end time

				Long endResults = graph.traversal().V().inE("isPublicized").count().toList().get(0);
				LOGGER.info(endResults - initResults + " results for OlympicGamesTrending in " + (end - start)
						+ " milliseconds");

				// reset graph
				for (Map<String, Object> n : matches) {
					Iterator<Edge> edgesOut = ((Vertex) n.get("product")).edges(Direction.OUT, "isPublicized");
					while (edgesOut.hasNext()) {
						Edge edge = edgesOut.next();
						if (edge.inVertex().equals((Vertex) n.get("campaign"))) {
							edge.remove();
						}

					}

				}

			}

			i++;
		}

	}

}
