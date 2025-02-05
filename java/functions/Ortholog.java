package functions;

// import DomainAlignment.Ortholog;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;

import data.Domain;
import data.Mapping;
import data.Orthologies;
import functions.Static;

public class Ortholog {

  /**
   * Reads in the text-file of proteins and their domains generated by the PERL script into a data
   * structure.
   *
   * @param speciesName - the species name (and directory) for the domain file
   * @param algorithm - the domain-finding algorithm. "all" for all of them.
   * @param fileType - the sub-descriptor of the file, as the tokoen before the .txt - e.g.
   *        processed or trimmed
   * @return - a HashMap that maps an Ensembl protein id to a string containing the domain info,
   *         where each domain is of the form "<name>,<start position>,<end position>" and are
   *         separated by two spaces
   * @throws Exception
   */
  public static HashMap<String, String> readDomainsFile(String speciesName, String algorithm, String fileType)
      throws Exception {
    BufferedReader in =
        new BufferedReader(new FileReader(speciesName + "/" + speciesName + ".protein_domains." + algorithm + "."
            + fileType + ".txt"));
    String line = "";

    HashMap<String, String> humanProteins = new HashMap<String, String>();

    while ((line = Static.skipCommentLines(in)) != null) {
      String[] split = line.split("\t");
      String proteinName = split[0];
      String proteinId = split[1];

      ArrayList<Domain> domains = new ArrayList<Domain>();
      // If there are no domains, then there will only be 1 tab character, and we leave the domain
      // info string blank.
      if (split.length > 2) {
        // If there are domains, there will be 2 tab characters, and everything after the second is
        // the domain information
        for (int i = 2; i < split.length; i++) {
          String[] split2 = split[i].split(",");
          domains.add(new Domain(split2[0], Integer.parseInt(split2[1]), Integer.parseInt(split2[2])));
        }
      }
      Collections.sort(domains);

      String domainString = "";
      for (Domain domain: domains) {
        domainString += domain.name + "," + domain.start + "," + domain.end + "  ";
      }
      domainString = domainString.trim();


      humanProteins.put(proteinId, domainString);
    }

    in.close();

    return humanProteins;
  }


  /**
   * Takes two species, loads up all their orthology data, maps it with all available mapping
   * information, and returns a combined data structure for all the orthology information between
   * those two species.
   *
   * @return - a data structure mapping protein id, to ortholog id, to a set of sources for that
   *         orthology pairing
   * @throws Exception
   */
  public static HashMap<String, HashMap<String, HashSet<String>>> getOrthologies(String mainSpecies, String otherSpecies)
      throws Exception {
    Static.debugOutput("Ortholog.getOrthologies() reading in orthology data for " + mainSpecies + " & " + otherSpecies + ". (" + new Date().toString() + ")");
    // Normal state
    String species1 = mainSpecies;
    String species2 = otherSpecies;
    boolean flip = false;

    if (mainSpecies.compareTo(species2) > 0) {
      // If species1 is alphabetically after species2, flipping is required to accommodate filename
      species1 = otherSpecies;
      species2 = mainSpecies;
      flip = true;
    }

    HashSet<String> orthologs = new HashSet<String>();

    Mapping mapper1 = Mapping.masterEnsemblpMapping(species1);
    Mapping mapper2 = Mapping.masterEnsemblpMapping(species2);

    try {
      orthologs.addAll(Ortholog.inParanoid("data/orthology/Output." + species1 + "-" + species2,
          mapper1, 'b', mapper2, 'b', flip));
    }
    catch (FileNotFoundException e) {
      System.out.println("Error attempting to process " + "data/orthology/Output." + species1 + "-" + species2
          + ". Skipping file.");
    }

    // try {
    // orthologs.addAll(domainNetworkAlignment.Ortholog.homologene("orthology/homologene.data",
    // Static.speciesLongName(mainSpecies), Static.speciesLongName(otherSpecies), mapper1, 'b',
    // mapper2, 'b', flip));
    // }
    // catch (FileNotFoundException e) {
    // System.out.println("Error attempting to process " +
    // "orthology/homologene.data. Skipping file.");
    // }

    try {
      orthologs.addAll(Ortholog.orthoMCL("data/orthology/OrthoMCL." + species1 + "-" + species2,
          species1, species2, mapper1, 'b', mapper2, 'b', flip));
    }
    catch (FileNotFoundException e) {
      System.out.println("Error attempting to process " + "odata/rthology/OrthoMCL." + species1 + "-" + species2
          + ". Skipping file.");
    }

    try {
      orthologs.addAll(Ortholog.ensembl("data/orthology/Ensembl." + species1 + "-" + species2, null,
          'f', null, 'f', flip));
    }
    catch (FileNotFoundException e) {
      System.out.println("Error attempting to process " + "data/orthology/Ensembl." + species1 + "-" + species2
          + ". Skipping file.");
    }

    Static.debugOutput("Ortholog.getOrthologies() done reading in orthology data. (" + new Date().toString() + ")");

    return Ortholog.mergeOrthologies(orthologs);
  }

  /**
   * Takes an inParanoid download file, builds a list of orthologies from it, and a bunch of other files.
   * @param inparanoidFile
   * @param species1Remap - Mapping object used to remap protein1 entries for output (e.g. from uniprot ID to protein name)
   * @param dir1 - direction that the species1Remap Mapping should be utilized
   * @param species2Remap - Mapping object used to remap protein2 entries for output
   * @param dir2 - direction that the species1Remap Mapping should be utilized
   * @param swapOrder - true if output should have species2 listed first, false if species1 listed first
   * @return - returns an HashSet of tab-delimited strings; each string represents a pair of orthologous proteins,
   *           via their Uniprot IDs (unless remapped) and then "Inparanoid" to indicate source
   * @throws Exception
   */
  public static HashSet<String> inParanoid(String inparanoidFile, Mapping species1Remap, char dir1, Mapping species2Remap, char dir2, boolean swapOrder) throws Exception {
      BufferedReader in = new BufferedReader(new FileReader(inparanoidFile));
      String line = "";

      HashSet<String> proteins1 = new HashSet<String>();
      HashSet<String> proteins2 = new HashSet<String>();
      HashSet<String> results = new HashSet<String>();

      // Skip down to first ___ line
      while ((line = in.readLine()) != null && line.charAt(0) != '_') {
      }
      in.readLine();

      while ((line = in.readLine()) != null) {
          if (line.startsWith("Score difference")) {
              line = in.readLine();
              do {
                  String[] split = line.split("\t");
                  if (split[0].trim().length() > 0) {
                      proteins1.add(split[0].trim());
                  }
                  if (split[3].trim().length() > 0) {
                      proteins2.add(split[3].trim());
                  }
                  line = in.readLine();
              } while (!line.startsWith("Bootstrap"));

              // Process the orthogroup now
              HashSet<String> temp1 = new HashSet<String>();
              HashSet<String> temp2 = new HashSet<String>();

              // Remap names if necessary
              if (species1Remap != null) {
                  species1Remap.remapCheckOriginal(proteins1, temp1, dir1);
              }
              else {
                  temp1 = proteins1;
              }
              if (species2Remap != null) {
                  species2Remap.remapCheckOriginal(proteins2, temp2, dir2);
              }
              else {
                  temp2 = proteins2;
              }

              for (String protein1: temp1) {
                  for (String protein2: temp2) {
                      if (!swapOrder) {
                          results.add(protein1 + "\t" + protein2 + "\tInparanoid");
                      }
                      else {
                          results.add(protein2 + "\t" + protein1 + "\tInparanoid");
                      }
                  }
              }

              proteins1 = new HashSet<String>();
              proteins2 = new HashSet<String>();

          }



//Old code. Used one-to-one mappings.
//        Iterator<String> it1 = species1.iterator();
//
//
//        while (it1.hasNext()) {
//            String p1 = it1.next();
//            // Rename the protein according to the remapping
//            if (species1Remap != null && species1Remap.containsForward(p1)) {
//                p1 = species2Remap.get(p1);
//            }
//
//            Iterator<String> it2 = species2.iterator();
//
//            while (it2.hasNext()) {
//                String p2 = it2.next();
//                // Rename the protein according to the remapping hashtable
//                if (species2Remap != null && species2Remap.containsKey(p2)) {
//                    p2 = species2Remap.get(p2);
//                }
//
//                if (!swapOrder) {
//                    orthologList.add(p1 + "\t" + p2 + "\tInparanoid");
//                }
//                else {
//                    orthologList.add(p2 + "\t" + p1 + "\tInparanoid");
//                }
//            }
//        }
//

      }

      in.close();

      return results;
  }

  /**
   * Reads in a downloaded file from OrthoMCL, and plucks out the orthologous protein pairs between two species.
   * @param orthomclFile - filename for the downloaded Ortholog file
   * @param species1Code - the shorthand code for the first species - OrthoMCL is order-insensitive, so this is by preference
   * @param species2Code - the shorthand code for the second species
   * @param species1Remap - a Hashtable that replaces Ensembl IDs from species 1, if provided - OrthoMCL is order-insensitive, so this is by preference
   * @param species2Remap - a Hashtable that replaces Ensembl IDs from species 2, if provided
   * @return
   * @throws Exception
   */
  public static HashSet<String> orthoMCL(String orthomclFile, String species1, String species2, Mapping species1Remap, char dir1, Mapping species2Remap, char dir2, boolean swapOrder) throws Exception {
      BufferedReader in = new BufferedReader(new FileReader(orthomclFile.trim()));
      String line = "";

      int speciesInt = -1;
      // Orthogroups maps an OrthoGroup (string name) to a list of two lists of proteins - the first sublist containing
      // member proteins from species 1, and the second sublist member proteins from species 2
      HashMap<String, ArrayList<ArrayList<String>>> orthoGroups = new HashMap<String, ArrayList<ArrayList<String>>>();

      in.readLine(); // Skip first header line

      while ((line = Static.skipCommentLines(in)) != null) {
          String protein = "";
          HashSet<String> proteins = new HashSet<String>();

          line = line.replace("scer_s288c__", ""); // Some bullshit to deal with annoying yeast nomenclature

          if (line.contains(Static.speciesLongName(species1))) {
              speciesInt = 1;
              protein = line.split("\t")[1];
              proteins.add(protein);
              HashSet<String> temp = new HashSet<String>();
              if (species1Remap != null) {
                  species1Remap.remapCheckOriginal(proteins, temp, dir1);
              }
              proteins = temp;
          }
          else if (line.contains(Static.speciesLongName(species2))) {
              speciesInt = 2;
              protein = line.split("\t")[1];
              proteins.add(protein);
              HashSet<String> temp = new HashSet<String>();
              if (species2Remap != null) {
                  species2Remap.remapCheckOriginal(proteins, temp, dir2);
              }
              else {
                  temp = proteins;
              }
              proteins = temp;
          }

          // Read in the OrthoGroup assignment
          String group = line.split("\t")[7];

          if (!orthoGroups.containsKey(group)) {
              ArrayList<ArrayList<String>> temp = new ArrayList<ArrayList<String>>();
              temp.add(new ArrayList<String>());
              temp.add(new ArrayList<String>());
              orthoGroups.put(group, temp);
          }

          // Add the protein to the correct slot in the OrthoGroup data structure
          for (String proteinString: proteins) {
              orthoGroups.get(group).get(speciesInt-1).add(proteinString);

          }
      }

      in.close();



      HashSet<String> results = new HashSet<String>();

      for (ArrayList<ArrayList<String>> orthoGroup: orthoGroups.values()) {
          for (String protein1: orthoGroup.get(0)) {
              for (String protein2: orthoGroup.get(1)) {
                  if (!swapOrder) {
                      results.add(protein1 + "\t" + protein2 + "\tOrthoMCL");
                  }
                  else {
                      results.add(protein2 + "\t" + protein1 + "\tOrthoMCL");
                  }

              }
          }
      }

      return results;
  }

  /**
   * Scans a list of orthologous proteins in two species, produced by Ensembl, and returns a list of orthologous protein pairs, by name
   * @param ensemblFile - a list of orthologous proteins, by Ensembl IDs
   * @param species1Remap - a Hashtable that replaces Ensembl IDs from species 1, if provided
   * @param species2Remap - a Hashtable that replaces Ensembl IDs from species 2, if provided
   * @param swapOrder - whether to swap the order of the species for output
   * @return - an arraylist of strings; the first is the source of the orthologies; every other is a pair of orthologous proteins,
   *           tab-delimited
   * @throws Exception
   */
  public static HashSet<String> ensembl(String ensemblFile, Mapping species1Remap, char dir1, Mapping species2Remap, char dir2, boolean swapOrder) throws Exception {
      // Create a mapping from Ensembl IDs to protein names for human proteins
      String line = "";

      // Scan orthology file for matches
      HashSet<String> results = new HashSet<String>();

      BufferedReader in = new BufferedReader(new FileReader(ensemblFile));

      while ((line = Static.skipCommentLines(in)) != null) {

          String[] split = line.split("\t");
          // Both proteins must be present.
          if (split.length < 2 || split[0].trim().length() == 0 || split[1].trim().length() == 0) {
            continue;
          }

          HashSet<String> proteins1 = new HashSet<String>();
          HashSet<String> proteins2 = new HashSet<String>();
          proteins1.add(split[0]);
          proteins2.add(split[1]);

          HashSet<String> temp1 = new HashSet<String>();
          HashSet<String> temp2 = new HashSet<String>();

          if (species1Remap != null) {
              species1Remap.remapCheckOriginal(proteins1, temp1, dir1);
          }
          else {
              temp1 = proteins1;
          }
          if (species2Remap != null) {
              species2Remap.remapCheckOriginal(proteins2, temp2, dir2);
          }
          else {
              temp2 = proteins2;
          }

          for (String protein1: temp1) {
              for (String protein2: temp2) {
                  if (!swapOrder) {
                      results.add(protein1 + "\t" + protein2 + "\tEnsembl");
                  }
                  else {
                      results.add(protein2 + "\t" + protein1 + "\tEnsembl");
                  }
              }
          }


      }

      in.close();

      return results;
  }


  /**
   * Takes a set of orthologies from different sources in string form, and merges them into a single data structure
   * @param orthologies - a set of orthology strings, each of the form "<protein 1>\t<protein2>\t<source>"
   * @return - a data structure mapping protein name, to ortholog name, to a set of sources for that orthology pairing
   */
  public static HashMap<String, HashMap<String, HashSet<String>>> mergeOrthologies(HashSet<String> orthologies) {
      HashMap<String, HashMap<String, HashSet<String>>> results = new HashMap<String, HashMap<String, HashSet<String>>>();

      for (String orthology: orthologies) {
          String[] split = orthology.split("\t");

          // First time seeing protein 1
          results.putIfAbsent(split[0], new HashMap<String, HashSet<String>>());

          // First time seeing protein 1 + protein 2 combination
          results.get(split[0]).putIfAbsent(split[1], new HashSet<String>());

          // Add the source the orthology was from
          results.get(split[0]).get(split[1]).add(split[2]);
      }

      return results;
  }

}
