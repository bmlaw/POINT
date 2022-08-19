package processing;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

import data.Network;
import data.NetworkAdjList;

public class Test {



  private static class MIStruct {
    HashSet<String> descendants = new HashSet<String>();
    HashMap<String, String> names = new HashMap<String, String>();
  }

  /**
   * Helper function to collect all the interaction description terms that descend from physical
   * interactions in the MI ontology.
   *
   * Uses helper method getMIDescendants()
   *
   * Input file: mi.owl.txt
   *
   * @param predicted - indicates whether predicted interactions should be included or not
   * @return - a set of all the MI:XXXX terms that represent physical interactions
   * @throws IOException
   */
  private static MIStruct gatherPhysicalInteractionTerms(boolean predicted) throws IOException {
    HashSet<String> terms = new HashSet<String>();
    terms.add("MI:0218"); // Physical interaction
    terms.add("MI:0915"); // Physical association
    terms.add("MI:0403"); // Co-localzation

    MIStruct result = Test.getMIDescendants(terms);

    if (predicted) {
      result.descendants.add("MI:1110");
    }

    return result;
  }

  /**
   * Helper function to collect all the interaction detection method terms that represent predicted interactions
   * in the MI ontology.
   *
   * Uses helper method getMIDescendants()
   *
   * Input file: mi.owl.txt
   *
   * @param unspecified - whether interactions with unspecified detection methods should also be counted as predicted
   * @return - a set of all the MI:XXXX terms that represent predicted protein-protein interactions
   * @throws IOException
   */
  private static MIStruct gatherPredictedInteractionTerms(boolean unspecified) throws IOException {
    HashSet<String> terms = new HashSet<String>();

    terms.add("MI:0362"); // Inference
    terms.add("MI:0063"); // Interaction Prediction

    MIStruct result = Test.getMIDescendants(terms);

    if (unspecified) {
      result.descendants.add("MI:0686"); // Unspecified method
      result.descendants.add("MI:0001"); // Interaction detection method
      result.descendants.add("-");
    }

    return result;
  }

  /**
   * Helper function to collect all the interaction detection method terms that represent experimentally validated interactions
   * in the MI ontology.
   *
   * Uses helper method getMIDescendants()
   *
   * Input file: mi.owl.txt
   *
   * @param unspecified - whether interactions with unspecified detection methods should also be counted as predicted
   * @return - a set of all the MI:XXXX terms that represent validated protein-protein interactions
   * @throws IOException
   */
  private static MIStruct gatherValidatedInteractionTerms(boolean unspecified) throws IOException {
    HashSet<String> terms = new HashSet<String>();
    terms.add("MI:0045"); // Experimental Interaction Detection

    MIStruct result = Test.getMIDescendants(terms);

    if (unspecified) {
      result.descendants.add("MI:0686"); // Unspecified method
      result.descendants.add("MI:0001"); // Interaction detection method
      result.descendants.add("-");
    }

    return result;
  }

  /**
   * Helper function to collect all the interaction detection method terms in the MI ontology.
   *
   * Uses helper method getMIDescendants()
   *
   * Input file: mi.owl.txt
   *
   * @param unspecified - whether interactions with unspecified detection methods should also be counted as predicted
   * @return - a set of all the MI:XXXX terms that represent physical interactions
   * @throws IOException
   */
  private static MIStruct gatherDetectedInteractionTerms(boolean unspecified) throws IOException {
    MIStruct result = Test.gatherPredictedInteractionTerms(false);
    result.descendants.addAll(Test.gatherValidatedInteractionTerms(false).descendants);

    if (unspecified) {
      result.descendants.add("MI:0686"); // Unspecified method
      result.descendants.add("MI:0001"); // Interaction detection method
      result.descendants.add("-");
    }

    return result;
  }

  /**
   * Parse a molecular interactions ontology file, and get all the descendents of the given terms
   * @param starterTerms - a set of MI terms for which all descendants are to be found
   * @return
   * @throws IOException
   */
  private static MIStruct getMIDescendants(HashSet<String> starterTerms) throws IOException {
    HashMap<String, HashSet<String>> isA = new HashMap<String, HashSet<String>>();

    MIStruct result = new MIStruct();
    result.descendants.addAll(starterTerms);

    // Read in the MI ontology.
    BufferedReader in = new BufferedReader(new FileReader("data/mi.owl.txt"));
    String line = "";
    String newTerm = null;

    while ((line = in.readLine()) != null) {
      if (line.startsWith("id:")) {
        newTerm = line.trim().substring("id:".length()).trim();
      }
      else if (line.startsWith("name")) {
        result.names.put(newTerm, line.trim().substring("name:".length()).trim());
      }
      else if (line.startsWith("is_a: ")) {
        String parent = line.substring("is_a: ".length()).trim();
        parent = parent.substring(0, parent.indexOf("!") - 1);
        isA.putIfAbsent(newTerm, new HashSet<String>());
        isA.get(newTerm).add(parent);
      }
    }

    in.close();

    // Boolean to keep looping through the ontology until the custom result set remains unchanged through an entire loop
    boolean keepGoing = true;
    while (keepGoing) {
      keepGoing = false;
      ArrayList<String> remove = new ArrayList<String>();
      for (String term: isA.keySet()) {
        for (String parent: isA.get(term)) {
          if (result.descendants.contains(parent)) {
            result.descendants.add(term);
            remove.add(term);
            keepGoing = true;
          }
        }
      }

      // Remove all the newly found descendant terms from the remaining terms to be searched
      for (String term: remove) {
        isA.remove(term);
      }
    }

    return result;
  }

  private static String extractMICode(String iRefField) {
    int index = iRefField.lastIndexOf("MI:");
    if (index == -1) {
      return "-";
    }
    return iRefField.substring(index, index + 7);
  }

  public static void main(String[] args) throws Exception {
    BufferedReader in = new BufferedReader(new FileReader("M.musculus-3.4.164.biogrid.txt"));
    String line = "";

    Network newNetwork = new NetworkAdjList("wayne");
    MIStruct interactionTypes = Test.gatherPhysicalInteractionTerms(true);
    int selfLoops = 0;
    int duplicates = 0;
    int wrongTypeLines = 0;

    in.readLine(); // Skip first line
    HashMap<String, Integer> typeCounts = new HashMap<String, Integer>();

    while ((line = in.readLine()) != null) {
      String[] breaks = line.split("\t");

      String protein1 = breaks[2].split("locuslink:")[1];
      String protein2 = breaks[3].split("locuslink:")[1];
      String interactionType = Test.extractMICode(breaks[11]);


      if (interactionType.equals("MI:0794") || interactionType.equals("MI:0796") || interactionType.equals("MI:0799")) {
        wrongTypeLines++;
        continue;
      }

      // If interaction type is not a desired type, skip.
//      if (!interactionTypes.descendants.contains(interactionType)) {
//        wrongTypeLines++;
//        continue;
//      }

      if (protein1.equals(protein2)) {
        selfLoops++;
        continue;
      }

      if (newNetwork.containsNode(protein1) && newNetwork.containsNode(protein2) && newNetwork.areAdjacent(protein1, protein2)) {
        duplicates++;
        continue;
      }

      typeCounts.put(interactionType, typeCounts.getOrDefault(interactionType, 0) + 1);

      if (!newNetwork.containsNode(protein1)) {
        newNetwork.addNode(protein1);
      }
      if (!newNetwork.containsNode(protein2)) {
        newNetwork.addNode(protein2);
      }

      newNetwork.addEdge(protein1, protein2);
    }

    in.close();

    String outputString = "# Nodes = " + newNetwork.getNumVertices() + " #Edges = " + newNetwork.getNumEdges() + " #Self Loops = " + selfLoops + " # Duplicates = " + duplicates + " # Wrong Type = " + wrongTypeLines;

    System.out.println(outputString);

    System.out.println(typeCounts);
  }
}
