package de.zbit.kegg.parser.pathway;

/**
 * Corresponding to the possible Kegg Relation Types (see {@link http://www.genome.jp/kegg/xml/docs/})
 * @author wrzodek
 */
public enum RelationType {
  /*
   * attribute value    explanation
ECrel   enzyme-enzyme relation, indicating two enzymes catalyzing successive reaction steps
PPrel   protein-protein interaction, such as binding and modification
GErel   gene expression interaction, indicating relation of transcription factor and target gene product
PCrel   protein-compound interaction
maplink   link to another map
   */
  ECrel, PPrel, GErel, PCrel, maplink, other
}
