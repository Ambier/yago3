package fromThemes;

import java.io.File;
import java.util.Set;

import fromOtherSources.HardExtractor;
import fromWikipedia.CategoryExtractor;
import fromWikipedia.FlightExtractor;
import fromWikipedia.GenderExtractor;
import fromWikipedia.InfoboxExtractor;
import fromWikipedia.TemporalCategoryExtractor;
import fromWikipedia.TemporalInfoboxExtractor;



import javatools.administrative.Announce;
import javatools.datatypes.FinalSet;
import basics.Fact;
import basics.FactComponent;
import basics.RDFS;
import basics.Theme;
import basics.Theme.ThemeGroup;

/**
 * YAGO2s - FactExtractor
 * 
 * Deduplicates all instance-instance facts and puts them into the right themes
 * 
 * @author Fabian M. Suchanek
 * 
 */
public class FactExtractor extends SimpleDeduplicator {

  @Override
  public Set<Theme> input() {
    return new FinalSet<>(CategoryExtractor.CATEGORYFACTS,
        GenderExtractor.PERSONS_GENDER,        
        HardExtractor.HARDWIREDFACTS, 
        InfoboxExtractor.INFOBOXFACTS,        
        RuleExtractor.RULERESULTS,  
        FlightExtractor.FLIGHTS,
        //GeoNamesDataImporter.GEONAMESDATA,
        TemporalCategoryExtractor.TEMPORALCATEGORYFACTS,
        TemporalInfoboxExtractor.TEMPORALINFOBOXFACTS);
  }

  /** All facts of YAGO */
  public static final Theme YAGOFACTS = new Theme("yagoFacts", "All facts of YAGO that hold between instances", ThemeGroup.CORE);

  /** relations that we exclude, because they are treated elsewhere */
  public static final Set<String> relationsExcluded = new FinalSet<>(RDFS.type, RDFS.subclassOf, RDFS.domain, RDFS.range, RDFS.subpropertyOf,
      RDFS.label, "skos:prefLabel", "<isPreferredMeaningOf>", "<hasGivenName>", "<hasFamilyName>", "<hasGloss>");

  @Override
  public Theme myOutput() {
    return YAGOFACTS;
  }

  @Override
  public boolean isMyRelation(Fact fact) {
    if(fact.getRelation().startsWith("<_")) return(false);
    if(relationsExcluded.contains(fact.getRelation())) return(false);
    if(FactComponent.isFactId(fact.getArg(1))) return(false);
    if(FactComponent.isLiteral(fact.getArg(2))) return(false);
    return(true);
  }
  
  public static void main(String[] args) throws Exception {
    Announce.setLevel(Announce.Level.DEBUG);
    new FactExtractor().extract(new File("C:/fabian/data/yago2s"), "test");
  }

}