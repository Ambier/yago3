package fromOtherSources;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import basics.Fact;
import basics.FactComponent;
import basics.YAGO;
import extractors.DataExtractor;
import extractors.Extractor;
import javatools.datatypes.FinalSet;
import javatools.datatypes.Pair;
import javatools.parsers.Char17;
import utils.Theme;

public class WikidataImageLicenseExtractor extends Extractor {

  public static final Theme WIKIDATAIMAGELICENSE = new Theme("wikidataImageLicenses2", 
      "Licences extracted for wikidata Images");
  
  private static final String wikipediaUsersUrl = "wikipedia.org/wiki/";
  private static final String flickerUsersUrl = "https://www.flickr.com/people/";
  private static Pattern authorFieldPattern = Pattern.compile("\\|(?:[Aa]uthor|[Aa]rtist)=(.*?)(\\n\\||\\n?$)");
  private static Map<String, Pattern> authorPatterns;
  static {
    Map<String, Pattern> tempMap = new HashMap<String, Pattern>();
    tempMap.put("Unknown", Pattern.compile("\\{\\{[Uu]nknown\\|[Aa]uthor\\}\\}"));
    tempMap.put("wikiUser1", Pattern.compile("\\[\\[(?::?(.{2,3}):)?((?:[Uu]ser:)?.+?)(?:\\|(.+?))?\\]\\]"));
    tempMap.put("wikiUser2", Pattern.compile("\\{\\{((?:(?:([Uu]ser)|(?:[Cc]reator)|(?:U)|(?:Ud)):.+?)\\}\\}"));
    tempMap.put("flickrUser1", Pattern.compile("\\[\\[flickruser:(.+?)(?:\\|(.+))?\\]\\]"));
    tempMap.put("flickrUser2", Pattern.compile("\\[\\[flickruser:(.+?)(?:\\|(.+))?\\]\\]"));
    tempMap.put("wikiUserAtProject", Pattern.compile("\\{\\{(?:(?:user at project)|(?:original uploader))\\|(.*?)\\|(?:wikipedia\\|)?(.{2,3})\\}\\}"));
    authorPatterns = Collections.unmodifiableMap(tempMap);
  }
  
  private static class authorUser {
    String name;
    String url;
    
    public authorUser() {
      this.name = null;
      this.url = null;
    }
  }
  
  public static int cnt;
  
  @Override
  public Set<Theme> input() {
    return (new FinalSet<>(WikidataImageExtractor.WIKIDATAIMAGES));
  }

  @Override
  public Set<Theme> output() {
    //return (new FinalSet<>());
    return (new FinalSet<>(WIKIDATAIMAGELICENSE));
  }

  @Override
  public void extract() throws Exception {
    
   cnt = 0;
    Set<Fact> wikidataEntityImage = WikidataImageExtractor.WIKIDATAIMAGES.factCollection().getFactsWithRelation(YAGO.hasWikiDataImageUrl);
    for(Fact fact : wikidataEntityImage){
      String entityName = fact.getSubject();
      String url = FactComponent.stripBrackets(fact.getObject());
      int fileNameIndex = url.indexOf("/wiki/") + "/wiki/".length();
      String fileName = url.substring(fileNameIndex);
      String editUrl = "https://commons.wikimedia.org/w/index.php?title=" + URLEncoder.encode(fileName, "UTF-8")  + "&action=edit";
      extractImageLicense(editUrl, FactComponent.forUri(url), entityName);
    }
    System.out.println("cnt = " + cnt);
  }
  
  private static void extractImageLicense(String imageUrl, String printUrl, String entity){
    try {
      Connection connect = Jsoup.connect(imageUrl);
      Element body = connect.get().body();
      Element textBox = body.getElementById("wpTextbox1");
      if (textBox != null) {
        String information = getInformation(textBox.val());
        authorUser author = new authorUser();
        if (information != null) {
          // Find the author or artist of the image:
          author = findAuthor(information);
        }
        else
          System.out.println("textBox is empty for: " + imageUrl + " " + textBox.val());
        
        
        
        
        if (author.name != null || author.url != null) {
          Fact f = new Fact(new Fact(printUrl, YAGO.hasAuthor, YAGO.author));
          f.makeId();
          WIKIDATAIMAGELICENSE.write(f);
          if (author.name != null)
            WIKIDATAIMAGELICENSE.write(new Fact(f.getId(), YAGO.hasName, author.name));
          if (author.url != null)
            WIKIDATAIMAGELICENSE.write(new Fact(f.getId(), YAGO.hasUrl, author.url));
        }
      }
      else {
        System.out.println("No wpTextbox1 for entity: " + entity + " with image url: " + printUrl + " and image edit url: " + imageUrl);
        cnt++;
      }
      
    } catch (IOException e) {
      e.printStackTrace();
      System.out.println("Error: Connection problem for: " + imageUrl);
    }
    
  }

  private static String getInformation(String textBox) {
   int start = -1;
   int offset = 0;
   if (textBox.contains("{{Information")) {
     start = textBox.indexOf("{{Information");
     offset = "{{Information".length();
   }
   else if (textBox.contains("{{COAInformation")) {// Coat Of Arm
     start = textBox.indexOf("{{COAInformation");
     offset = "{{COAInformation".length();
   }
   if (start != -1) {
     int brackets = 2;
     int i;
     for(i = start + offset; i < textBox.length(); i++)
     {
       char current = textBox.charAt(i);
       if(current == '{')
         brackets++;
       else if (current == '}')
         brackets--;
       if(brackets == 0) 
         break;
     }
     return textBox.substring(start + offset, i - 1).replaceAll("\n+", "\n");
   }
   return textBox;
 }
  //check:
//https://commons.wikimedia.org/w/index.php?title=File%3AWappen_Geringswalde.png&action=edit
  
 private static authorUser findAuthor(String text) {
   authorUser author = new authorUser();
   Matcher matcher = authorFieldPattern.matcher(text);
   if (matcher.find()) {
     String match = matcher.group(1);
     for (String key:authorPatterns.keySet()) {
       Matcher matcherAuthorType = authorPatterns.get(key).matcher(match);
       if (matcherAuthorType.find()) {
         if(key.equals("Unknown")) {
           author.name = "Unknown";
         }
         if (key.equals("wikiUser1")) {
           if (matcherAuthorType.group(1) != null) 
             author.url = "http://" + matcherAuthorType.group(1) + "." + wikipediaUsersUrl + matcherAuthorType.group(2).replaceAll(" ", "_");
           else 
             author.url = "http://commons." + wikipediaUsersUrl + matcherAuthorType.group(2).replaceAll(" ", "_");
           if (matcherAuthorType.group(3) != null)
             author.name = matcherAuthorType.group(3);
         }
         if (key.equals("wikiUser2"))
           author.url = "http://commons." + wikipediaUsersUrl + matcherAuthorType.group(1).replaceAll(" ", "_");
         if (key.equals("flickrUser1"))
           author.url = flickerUsersUrl + matcher.group(1);
         if (key.equals("wikiUserAtProject"))
           author.url = "http://" + matcherAuthorType.group(2) + "." + wikipediaUsersUrl + "User:" + matcherAuthorType.group(1).replaceAll(" ", "_");;
       }
     }
     if (author.name == null && author.url == null)
       author.name = match; // for now TODO: should change this later. now I want to inspect
   }
  return author;
 }

}
