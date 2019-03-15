package com.approximation.amazon.controller;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Properties;
import java.util.logging.Logger;

import org.apache.tinkerpop.gremlin.structure.io.IoCore;
import org.apache.tinkerpop.gremlin.tinkergraph.structure.TinkerGraph;

import com.approximation.amazon.queries.CreateAdCampaign;
import com.approximation.amazon.queries.OlympicGamesTrendingRandom;
import com.approximation.amazon.queries.OlympicGamesTrendingSpatial;
import com.approximation.amazon.queries.RecommendsPack;
import com.approximation.amazon.queries.RelatedProductsRandom;
import com.approximation.amazon.queries.RelatedProductsTemporal;
import com.approximation.amazon.queries.UnpopularStock;

/**
 * Class AmazonController
 */
public class AmazonController {
	
	private final static Logger LOGGER = Logger.getLogger(AmazonController.class.getName());
	
	public static void run() {
		TinkerGraph graph = TinkerGraph.open();
		try {

			Properties p = new Properties();

			p.load(AmazonController.class.getResourceAsStream("/config.properties"));

			boolean individual = Boolean.parseBoolean(p.getProperty("individual"));

			String fileName = p.getProperty("file");

			if (fileName != null && !fileName.isEmpty()) {
				graph.io(IoCore.graphml()).readGraph(AmazonController.class.getResource("/models/" + fileName).getPath());
				LOGGER.info("Database loaded");

				Long maxDate = (Long) graph.traversal().V().hasLabel("Order").values("date").max().toList().get(0);
				Long minDate = (Long) graph.traversal().V().hasLabel("Order").values("date").min().toList().get(0);

				// CreateAdCampaign
				if (Boolean.parseBoolean(p.getProperty("q1"))) {

					CreateAdCampaign createAdCampaign = new CreateAdCampaign(graph, Boolean.parseBoolean(p.getProperty("creation")),
							Double.parseDouble(p.getProperty("prob1")));
					Thread query1 = new Thread(createAdCampaign);
					query1.start();
					if (individual) {
						query1.join();
					}
				}

				// UnpopularStock
				if (Boolean.parseBoolean(p.getProperty("q2"))) {

					UnpopularStock unpopularStock = new UnpopularStock(graph, Double.parseDouble(p.getProperty("prob2")));
					Thread query2 = new Thread(unpopularStock);
					query2.start();
					if (individual) {
						query2.join();

					}
				}

				// RelatedProducts
				if (Boolean.parseBoolean(p.getProperty("q3Random"))) {

					RelatedProductsRandom relatedProductsRandom = new RelatedProductsRandom(graph,
							Boolean.parseBoolean(p.getProperty("creation")), Double.parseDouble(p.getProperty("prob3")),
							minDate, maxDate);
					Thread query3 = new Thread(relatedProductsRandom);
					query3.start();
					if (individual) {
						query3.join();

					}
				}
				if (Boolean.parseBoolean(p.getProperty("q3Temporal"))) {

					int percQ3 = Integer.parseInt(p.getProperty("window3"));
					long windowQ3 = minDate + percQ3 * 86400000L;

					RelatedProductsTemporal relatedProductsTemporal = new RelatedProductsTemporal(graph,
							Boolean.parseBoolean(p.getProperty("creation")), minDate, windowQ3);
					Thread query3 = new Thread(relatedProductsTemporal);
					query3.start();
					if (individual) {
						query3.join();

					}
				}

				// OlympicGamesTrending
				if (Boolean.parseBoolean(p.getProperty("q4Spatial"))) {

					OlympicGamesTrendingSpatial olympicGamesTrendingSpatial = new OlympicGamesTrendingSpatial(graph,
							Boolean.parseBoolean(p.getProperty("creation")), Integer.parseInt(p.getProperty("hops4")));
					Thread query4 = new Thread(olympicGamesTrendingSpatial);
					query4.start();
					if (individual) {
						query4.join();
					}
				}

				if (Boolean.parseBoolean(p.getProperty("q4Random"))) {

					OlympicGamesTrendingRandom olympicGamesTrendingRandom = new OlympicGamesTrendingRandom(graph,
							Boolean.parseBoolean(p.getProperty("creation")),
							Double.parseDouble(p.getProperty("prob4")));
					Thread query4 = new Thread(olympicGamesTrendingRandom);
					query4.start();
					if (individual) {
						query4.join();
					}
				}

				// RecommendsPack
				if (Boolean.parseBoolean(p.getProperty("q5"))) {
					RecommendsPack recommendsPack = new RecommendsPack(graph, Boolean.parseBoolean(p.getProperty("creation")),
							Integer.parseInt(p.getProperty("hops5")));
					Thread query5 = new Thread(recommendsPack);
					query5.start();
					if (individual) {
						query5.join();
					}
				}
			}
		} catch (InterruptedException e) {
			LOGGER.info("Error on data configuration");
		} catch (FileNotFoundException e1) {
			LOGGER.info("File not found");
		} catch (IOException e1) {
			LOGGER.info("File config.properties is not accessible");
		}catch (NumberFormatException e1) {
			LOGGER.info("Error on data configuration");
		}
	}

}
