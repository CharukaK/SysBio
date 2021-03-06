/*
 * $Id$
 * $URL$
 * ---------------------------------------------------------------------
 * This file is part of the SysBio API library.
 *
 * Copyright (C) 2009-2016 by the University of Tuebingen, Germany.
 *
 * This library is free software; you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation. A copy of the license
 * agreement is provided in the file named "LICENSE.txt" included with
 * this software distribution and also available online as
 * <http://www.gnu.org/licenses/lgpl-3.0-standalone.html>.
 * ---------------------------------------------------------------------
 */
package de.zbit.kegg.parser.pathway;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.logging.Logger;

import de.zbit.kegg.api.KeggInfos;
import de.zbit.kegg.api.cache.KeggInfoManagement;
import de.zbit.util.DatabaseIdentifiers;
import de.zbit.util.DatabaseIdentifiers.IdentifierDatabases;
import de.zbit.util.Utils;

/**
 * Main Kegg document. Corresponding to the Kegg Pathway class
 * (see {@link http://www.genome.jp/kegg/xml/docs/})
 * 
 * @author Clemens Wrzodek
 * @author Finja B&uuml;chel
 * @version $Rev$
 * @since 1.0
 */
public class Pathway {
  public static final transient Logger log = Logger.getLogger(Pathway.class.getName()); 
  /**
	 * keggid.type the KEGGID of this pathway map
	 */
	private String name = "";
	/**
	 * maporg.type ko/ec/[org prefix]
	 */
	private String org = "";
	/**
	 * mapnumber.type the map number of this pathway map (5 digit integer)
	 */
	private int number = 0;
	/**
	 * string.type the title of this pathway map
	 */
	private String title = "";
	/**
	 * url.type the resource location of the image file of this pathway map
	 */
	private String image = "";
	/**
	 * url.type the resource location of the information about this pathway map
	 */
	private String link = "";
	/**
	 * All entries of the pathway
	 */
	private ArrayList<Entry> entries = new ArrayList<Entry>();
	/**
	 * All reactions of the pathway
	 */
	private ArrayList<Reaction> reactions = new ArrayList<Reaction>();
	/**
	 * All relations of the pathway
	 */
	private ArrayList<Relation> relations = new ArrayList<Relation>();

	/*
	 *  Custom Variables, not in the KGML specification
	 */

	/**
   * additional text
   */
  private String additionalText = "";   

	/**
	 * Comment of the source KGML file.
	 */
	private String comment = null;
	
  /**
   * This map should contain all identifiers for this element.
   * No matter if these are uniprot, entrez gene, etc.
   */
  private Map<DatabaseIdentifiers.IdentifierDatabases, Collection<String>> identifiers = 
    new HashMap<DatabaseIdentifiers.IdentifierDatabases, Collection<String>>();
	
	/**
	 * A description for the origin data format. It all data originates,
	 * e.g., from BioPAX, this can be changed to BioPax.
	 */
	private String originFormatName = "KGML";

  /**
	 * Version number of the source KGML file
	 */
	private double version=0;
	
	/*
	 * Speed improvements
	 */

	/**
	 * Contains ids of entries and the entry itself.
	 */
	private Map<Integer, Entry> idMap = new HashMap<Integer, Entry>();
	
  /**
   * Contains names of entries and the entry itself.
   */
  private Map<String, Collection<Entry>> nameMap = new HashMap<String, Collection<Entry>>();

  /**
   * Contains names of reactions and entries that modify this reactions (usually enzymes).
   * Maps {@link Reaction#getName()} to {@link Entry}s.
   */
  private Map<String, Collection<Entry>> reactionModifiers = new HashMap<String, Collection<Entry>>();
  
  /**
   * Contains names of entries and reactions in which this entry occurs.
   */
  private Map<String, Collection<Reaction>> reactionComponents = new HashMap<String, Collection<Reaction>>();
	
  /**
   * The maximum id number of an contained entry.
   */
  private int maxId=0;
  
	/**
	 * 
	 */
	private Pathway() {
		super();
	}

	/**
	 * 
	 * @param name
	 * @param org
	 * @param number
	 */
	public Pathway(String name, String org, int number) {
		this();
		setName(name);
		setOrg(org);
		setNumber(number);
	}
	
	 /**
   * 
   * @param name
   * @param org
   * @param number
   * @param title
   */
  public Pathway(String name, String org, int number, String title) {
    this(name, org, number);
    setTitle(title);
  }

	/**
	 * 
	 * @param name
	 * @param org
	 * @param number
	 * @param title
	 * @param image
	 * @param link
	 */
	public Pathway(String name, String org, int number, String title,
			String image, String link) {
		this(name, org, number);
		setTitle(title);
		setImage(image);
		setLink(link);
	}
	
	 /**
   * 
   * @param name
   * @param org
   * @param number
   * @param title
   * @param image
   * @param link
   * @param additionalText
   */
  public Pathway(String name, String org, int number, String title,
      String image, String link, String additionalText) {
    this(name, org, number, title, image, link);
    setAdditionalText(additionalText);
  }

	/**
	 * 
	 * @param e
	 */
	public void addEntry(Entry e) {
	  if (entries.contains(e)) return;
	  
	  idMap.put(e.getId(), e);
	  putEntryInNameMap(e);
	  addReactionModifier(e);
	  
	  maxId=Math.max(maxId, e.getId());
	  
		entries.add(e);
	}
	
	/**
	 * Please call this method whenever adding an {@link Entry}.
	 * @param e
	 */
	void putEntryInNameMap(Entry e) {
	  // Put whole name
	  Utils.addToMapOfSets(nameMap, e.getName(), e);
	  
	  // Put splitted name (as whole name may be "hsa:12345 hsa:23456")
	  if (e.getName().contains(" ")) {
	    for (String ko_id:e.getName().split(" ")) {
	      ko_id = ko_id.trim();
	      if (ko_id.length()>0) {
	        Utils.addToMapOfSets(nameMap, ko_id, e);
	      }
	    }
	  }
	}
	
	/**
	 * Adds various synonyms, that should point to one entry.
	 * <p><b>USE THIS WITH CAUTION!</b></p>
	 * @param e
	 * @param synonyms
	 */
	public void putEntrySynonymsInNameMap(Entry e, String synonyms) {
	  // Put splitted name (as whole name may be "hsa:12345 hsa:23456")
	  for (String ko_id:synonyms.split("\\s")) {
	    ko_id = ko_id.trim();
	    if (ko_id.length()>0) {
	      Utils.addToMapOfSets(nameMap, ko_id, e);
	    }
	  }
	}
	
	/**
	 * Please call this method whenever removing an {@link Entry}.
	 * @param e
	 */
	void removeEntryFromNameMap(Entry e) {
	  Set<String> toRemove = new HashSet<String>();
	  // Put whole name
	  toRemove.add(e.getName());
	  
	  // Put splitted name (as whole name may be "hsa:12345 hsa:23456")
	  for (String ko_id:e.getName().split(" ")) {
	    ko_id = ko_id.trim();
	    if (ko_id.length()>0) {
	      toRemove.add(ko_id);
	    }
	  }
	  
	  Utils.removeFromMapOfSets(nameMap, e, toRemove.toArray(new String[0]));
	}
	
	
	/**
	 * If and only if e is a reactionModifier, it is
	 * added to the list.
	 * @param e
	 */
	private void addReactionModifier(Entry e) {
	  addReactionModifier(e, e.getReactionString());
	}
	/**
   * If and only if e is a reactionModifier, it is
   * added to the list.
	 * @param e
	 * @param reactionName may also be "rn:R01793 rn:R01794"!
	 */
	private void addReactionModifier(Entry e, String reactionName) {
	  if (reactionName==null || reactionName.length()<1) {
	    reactionName = e.getReactionString();
	  }
	  
    if (reactionName!=null && reactionName.length()>0) {
      for (String rName: reactionName.trim().split(" ")) {
        if (rName.length()<1) continue;
        Collection<Entry> l = reactionModifiers.get(rName);
        if (l==null) {
          l = new HashSet<Entry>();
          reactionModifiers.put(rName, l);
        }
        l.add(e);
      }
    }
	}
	
  /**
   * Removes the given entry from the {@link #reactionModifiers}
   * list.
   * @param e
   */
  private void removeReactionModifier(Entry entry) {
    if (entry.hasReaction()) {
      for (String reaction : entry.getReactions()) {
        Collection<Entry> l = reactionModifiers.get(reaction);
        if (l!=null) l.remove(entry);
      }
    }
  }

	/**
	 * 
	 * @param r
	 */
	public void addReaction(Reaction r) {
	  if (reactions.contains(r)) return;
		reactions.add(r);
	}

	/**
	 * 
	 * @param r
	 */
	public void addRelation(Relation r) {
	  if (relations.contains(r)) return;
		relations.add(r);
	}

	/**
	 * Returns the complete entry list.
	 * 
	 * You should NOT modify this list, but use the provided methods
	 * (e.g. {@link #addEntry(Entry)}), else the id and name references
	 * are getting corrupt. 
	 * @return
	 */
	public ArrayList<Entry> getEntries() {
		return entries;
	}
	
	/**
	 * Iteratively parsing through all entries and
	 * returning the maximum entry id.
	 * @return
	 */
	public int getMaxEntryId() {
	  return maxId;
	}

	/**
	 * Iterates through all entries all returns the entry
	 * with the given id if it exists, or null.
	 * @param id
	 * @return
	 */
	public Entry getEntryForId(int id) {
		/*for (int i = 0; i < entries.size(); i++)
			if (entries.get(i).getId() == id)
				return entries.get(i); 
		return null;*/
	  return idMap.get(id);
	}

	/**
	 * Returns the entry that has a name that
	 * equalsIgnoreCase the given name.
	 * If no entry with this contidion can be found,
	 * returns null.
	 * @param name WITH prefix ("ko:12345" not "12345").
	 * @return
	 */
	public Collection<Entry> getEntriesForName(String name) {
		/*for (int i = 0; i < entries.size(); i++)
			if (entries.get(i).getName().equalsIgnoreCase(name))
				return entries.get(i);
		return null;*/
	  return nameMap.get(name);
	}
	
	/**
	 * Returns all modifiers (usually enzymes) for the given
	 * reaction, as noted in the KGML document.
	 * @param reactionName - e.g. "rn:R01662"
	 * @return List<Entry> or null
	 */
	public Collection<Entry> getReactionModifiers(String reactionName) {
	  return reactionModifiers.get(reactionName);
	}

	 /**
   * 
   * @return
   */
  public boolean isSetImage() {
    return image!=null && image.length()>0;
  }
  
	/**
	 * 
	 * @return
	 */
	public String getImage() {
		return image;
	}

	/**
	 * 
	 * @return
	 */
	public boolean isSetLink() {
	  return link!=null && link.length()>0;
	}
	
	/**
	 * 
	 * @return
	 */
	public String getLink() {
		return link;
	}

	/**
	 * 
	 * @return
	 */
	public String getName() {
		return name;
	}
	
	/**
	 * Gives the pathway name as compatible with MIRIAM.
	 * In other words: returns the same as {@link #getName()}
	 * but trims the "path:" prefix if one is available.
	 * @return
	 */
	public String getNameForMIRIAM() {
    // We need to trim the "PATH"-Prefix
    String pwID = getName();
    if (pwID!=null && pwID.toLowerCase().startsWith("path:")) {
      pwID = pwID.substring(5);
    }
    return pwID;
	}

	/**
	 * Remember: Pathway number is an 5-digit integer. So if size < 5 you need
	 * to make a prefix of 0's. You'd better use getNumberReal() instead.
	 */
	public int getNumber() {
		return number;
	}

	/**
	 * 
	 * @return
	 */
	public String getNumberReal() {
		String ret = Integer.toString(number);
		while (ret.length() < 5)
			ret = "0" + ret;
		return ret;
	}

	/**
	 * @return organism as KEGG abbreviation (e.g. "hsa")
	 */
	public String getOrg() {
		return org;
	}

	/**
	 * 
	 * @return
	 */
	public ArrayList<Reaction> getReactions() {
		return reactions;
	}

	/**
	 * 
	 * @return
	 */
	public ArrayList<Relation> getRelations() {
		return relations;
	}

	/**
	 * 
	 * @return
	 */
	public String getTitle() {
		return title;
	}

	/**
	 * 
	 * @param image
	 */
	public void setImage(String image) {
		this.image = image;
	}

	/**
	 * 
	 * @param link
	 */
	public void setLink(String link) {
		this.link = link;
	}

	/**
	 * 
	 * @param name
	 */
	public void setName(String name) {
		this.name = name;
	}

	/**
	 * 
	 * @param number
	 */
	public void setNumber(int number) {
		this.number = number;
	}

	/**
	 * 
	 * @param org organism as KEGG abbreviation (e.g. "hsa")
	 */
	public void setOrg(String org) {
		this.org = org;
	}

	/**
	 * 
	 * @param title
	 */
	public void setTitle(String title) {
		this.title = title;
	}

  public String getComment() {
    return comment;
  }

  public void setComment(String comment) {
    this.comment = comment;
  }
  
  /**
   * @return the originFormatName
   */
  public String getOriginFormatName() {
    return originFormatName;
  }

  /**
   * @param originFormatName the originFormatName to set
   */
  public void setOriginFormatName(String originFormatName) {
    this.originFormatName = originFormatName;
  }

  public double getVersion() {
    return version;
  }

  public void setVersion(double version) {
    this.version = version;
  }

  /**
   * This function has to be called by entries, before their
   * ids change. It does NOT perform the change itself.
   * @param entry - Entry object BEFORE THE CHANGE
   * @param id - new id.
   */
  protected void idChange(Entry entry, int id) {
    // Change the maxId
    resetMaxId(entry);
    maxId=Math.max(maxId, id);
    
    // Change the idMap
    idMap.remove(entry.getId());
    idMap.put(id, entry);
  }
  
  /**
   * Reset the maxId by iterating through all elements
   * @param entry - entry to IGNORE if it occurs. Set to
   * null to disable this feature.
   */
  private void resetMaxId(Entry entry) {
    if (entry.getId() == maxId) {
      maxId=0;
      for (Entry e:getEntries()) {
        if (entry!=null && e.equals(entry)) continue;
        maxId = Math.max(maxId, e.getId());
      }
    }
  }
  
  /**
   * This function has to be called by entries, before their
   * reaction changes. It does NOT perform the change itself.
   * @param entry - Entry object BEFORE THE CHANGE
   * @param reaction - the new reaction.
   */
  protected void reactionChange(Entry entry, String reaction) {
    // Remove the old entry
    removeReactionModifier(entry);
    addReactionModifier(entry, reaction);
  }
  
  /**
   * Remove the given entry from this pathway.
   * @param e
   */
  public void removeEntry(Entry e) {
    removeEntry(entries.indexOf(e));
  }
  /**
   * Remove the entry at the given index in the {@link #entries} list.
   * @param index index of the entry to remove.
   */
  public void removeEntry(int index) {
    // Comment [wrzodek 2010-07-01]: Entry may still be in other entries compound (group node) list.
    // Also in other relations, reactions, subtype-values,etc.
    if (index<0) return;
    Entry e = entries.get(index);
    
    // Update internal reference-maps and lists
    idMap.remove(e.getId());
    nameMap.remove(e.getName());
    removeReactionModifier(e);
    if (maxId==e.getId()) {
      resetMaxId(e);
    }
    
    // Really remove this entry from our list
    entries.remove(index);
    
    e.setName(Entry.removedNodeName);
    e=null;
  }

  /**
   * Get a list with ALL reactions in which this {@link Entry}
   * is involved as substrate or product. Does NOT return
   * reactions in which this {@link Entry} is involved
   * as reaction modifier. Use {@link #getReactionModifiers(String)}
   * or {@link Entry#getReactionString()} for this purpose.
   * 
   * @param entry
   * @return a list with reactions in which this entry
   * is involved as substrate or product.
   */
  public Collection<Reaction> getReactionsForEntry(Entry entry) {
    
    Set<Reaction> ret = new HashSet<Reaction>();
    Collection<Reaction> comp = reactionComponents.get(entry.getName());
    if (comp!=null) ret.addAll(comp);
    
    // Put splitted name (as whole name may be "hsa:12345 hsa:23456")
    if (entry.getName().contains(" ")) {
      for (String name:entry.getName().split(" ")) {
        name = name.trim();
        if (name.length()>0) {
          comp = reactionComponents.get(name);
          if (comp!=null) ret.addAll(comp);
        }
      }
    }
    
    return ret;
  }

  /**
   * Registers the {@link ReactionComponent} in internal
   * HashMaps for faster access.
   * @param rc
   * @param reaction
   */
  void registerReactionComponent(ReactionComponent rc, Reaction reaction) {
    // Put whole name
    Utils.addToMapOfSets(reactionComponents, rc.getName(), reaction);
    
    // Put splitted name (as whole name may be "hsa:12345 hsa:23456")
    if (rc.getName().contains(" ")) {
      for (String name:rc.getName().split(" ")) {
        name = name.trim();
        if (name.length()>0) {
          Utils.addToMapOfSets(reactionComponents, name, reaction);
        }
      }
    }
  }
  
  /**
   * Unregisters the {@link ReactionComponent} in internal
   * HashMaps for faster access.
   * @param rc
   * @param reaction
   */
  void unregisterReactionComponent(ReactionComponent rc, Reaction reaction) {
    Set<String> toRemove = new HashSet<String>();
    // Put whole name
    toRemove.add(rc.getName());
    
    // Put splitted name (as whole name may be "hsa:12345 hsa:23456")
    if (rc.getName().contains(" ")) {
      for (String name:rc.getName().split(" ")) {
        name = name.trim();
        if (name.length()>0) {
          toRemove.add(name);
        }
      }
    }
    
    Utils.removeFromMapOfSets(reactionComponents, reaction, toRemove.toArray(new String[0]));
  }

  /**
   * Returns the corresponding {@link Entry} for a
   * {@link ReactionComponent}.
   * @param rc
   * @return {@link Entry} or {@code null} of none found.
   */
  public Entry getEntryForReactionComponent(ReactionComponent rc) {
    return getEntryForReactionComponent(rc, false);
  }
  
  /**
   * <p>Note: This method returns other results than
   * {@link #getEntryForReactionComponent(ReactionComponent, boolean, KeggInfoManagement)}.
   * The other method (NOT THIS ONE) should be preferred.
   * </p> 
   * @param rc
   * @param supressWarning
   * @return
   * @see #getEntryForReactionComponent(ReactionComponent)
   */
  public Entry getEntryForReactionComponent(ReactionComponent rc, boolean supressWarning) {
    return getEntryForReactionComponent(rc,supressWarning,null);
  }
  
  /**
   * <p>Note: This function queries the KEGG API for synonyms of
   * {@link ReactionComponent#getName()}. So better cache all of them
   * before calling this method.</p> 
   * @param rc
   * @param supressWarning
   * @param manag for qerying synonyms of {@code rc}
   * @return
   * @see #getEntryForReactionComponent(ReactionComponent)
   */
  public Entry getEntryForReactionComponent(ReactionComponent rc, boolean supressWarning, KeggInfoManagement manag) {
    Entry rcEntry=null;
    
    if (rc.isSetCorrespondingEntry()) {
      rcEntry = rc.getCorrespondingEntry();
    }
    
    if (rcEntry==null && rc.isSetID()) {
      // Id is a unique identifier and thus preferred!
      rcEntry = getEntryForId(rc.getId());
    } 
    
    if (rcEntry==null && rc.isSetName()) { // no id or invalid id.
      Collection<Entry> c = getEntriesForName(rc.getName());
      int size = c==null?0:c.size();
      
      // Many glycand, compounds, ligands have synonyms! So query them...
      if (size<1 && manag!=null) {
        KeggInfos reaInfo = KeggInfos.get(rc.getName(), manag);
        if (reaInfo.getSameAs()!=null) {
          String[] synonyms = reaInfo.getSameAs().split("\\s");
          int synIndex=-1; c=null;
          while (c==null && ((++synIndex)<synonyms.length) ) {
            c = getEntriesForName(KeggInfos.appendPrefix(synonyms[synIndex]));
            if ( (size = c==null?0:c.size()) > 0 ) break;
          }
        }
      }
        
      if (size>1) {
        /* Actually we don't know which of those entries is really
         * involved in the reaction and I don't know if this ever
         * occurs => report a warning.
         */
        rcEntry = getBestMatchingEntry(rc.getName(), c);
        
        if (!supressWarning) {
          log.warning("Ambiguous reaction component: " + rc.getName());
        }
      }
      if (size>0) rcEntry = c.iterator().next();
      else rcEntry = null;
    }
    
    return rcEntry;
  }

  /**
   * Sometimes, {@link #getEntriesForName(String)} returnes multiple results.
   * This means, that multiple entries in the source pathway are available,
   * in which the given {@code keggName} is contained.
   * 
   * This method checks, if there is a unique entry, describing only the
   * {@code keggName} and returns this. Else, it returns any of the entries.
   * The results are reported at log level fine.
   * @param keggName
   * @param c all entries matching to {@code keggName}.
   * See {@link #getEntriesForName(String)} to generate this collection
   * @return any or best matching entry.
   */
  public static Entry getBestMatchingEntry(String keggName, Collection<Entry> c) {
    // Look for exact match with an contained object
    Set<Entry> exactMatches = new HashSet<Entry>();
    if (c!=null) {
      for (Entry e: c) {
        if (e.getName().equalsIgnoreCase(keggName)) {
          exactMatches.add(e);
        }
      }
    }
    
    if (exactMatches.size()==1) {
      log.fine("Ambiguous reaction component " + keggName + ". Took unique exact id match.");
      return exactMatches.iterator().next();
    } else if (exactMatches.size()>1) {
      for (Entry e: exactMatches) {
        if (e.getCustom()!=null) {
          log.fine("Ambiguous reaction component " + keggName + ". Took first exact match with custom object.");
          return e;
        }
      }
    }
    
    return null;
  }
  
  public boolean isSetName() {
    return (name!=null && name.length()>0);
  }
  

  public boolean isSetOrg() {
    return (org!=null && org.length()>0);
  }
  

  public boolean isSetNumber() {
    return number!=0;
  }

  public boolean isSetTitle() {
    return (title!=null && title.length()>0);
  }
  
  /**
   * Please be careful with this, as it returns
   * internal data structures.
   * @return complemente list of {@link #identifiers}.
   */
  public Map<IdentifierDatabases, Collection<String>> getDatabaseIdentifiers() {
    return identifiers;
  }
  
  /**
   * 
   * @return {@code true} if we have some {@link #identifiers}.
   */
  public boolean isSetDatabaseIdentifiers() {
    return identifiers!=null && identifiers.size()>0;
  }
  
  /**
   * Add an identifier to this {@link Entry}.
   * @param db
   * @param id
   */
  public void addDatabaseIdentifier(IdentifierDatabases db, String id) {
    Utils.addToMapOfSets(identifiers, db, id);
  }
  
  /**
   * Add all identifier of the map to this {@link Entry}.
   * @param db
   * @param id
   */
  public void addDatabaseIdentifiers( Map<IdentifierDatabases, Collection<String>> map) {
    for (java.util.Map.Entry<IdentifierDatabases, Collection<String>> entry : map.entrySet()) {
      for (String value : entry.getValue()) {
        Utils.addToMapOfSets(identifiers, entry.getKey(), value);
      }
    }    
  }
  
  /**
   * 
   * @return all the necessary XML attributes of this class
   */
  public Map<String, String> getKGMLAttributes() {
    Map<String, String> attributes = new TreeMap<String, String>();
    
    if(isSetName()) {
      attributes.put("name", name);
    }
    if(isSetOrg()) {
      attributes.put("org", org);
    }
    if(isSetNumber()) {
      attributes.put("number", String.valueOf(number));
    }
    if(isSetTitle()) {
      attributes.put("title", title);
    }
    if(isSetImage()) {
      attributes.put("image", image);
    }
    if(isSetLink()) {
      attributes.put("link", link);
    }
    
    
    return attributes;
  }
  
  public boolean isSetEntries() {
    return entries!=null && entries.size()>0;
  }
  
  public boolean isSetReactions() {
    return reactions!=null && reactions.size()>0;
  }
  
  public boolean isSetRelations() {
    return relations!=null && relations.size()>0;
  }
  
  @Override
  public int hashCode() {
    int hash = 199;
    if(isSetName())
      hash *= name.hashCode();
    if(isSetOrg())
      hash *= org.hashCode();
    if(isSetNumber())
      hash *= number;
    if(isSetTitle())
      hash *= title.hashCode();
    if(isSetImage())
      hash *= image.hashCode();
    if(isSetLink())
      hash *= link.hashCode();
    if(isSetEntries())
      hash *= entries.hashCode();
    if(isSetReactions())
      hash *= reactions.hashCode();
    if(isSetRelations())
      hash *= relations.hashCode();
  
    
    return hash;
  }
  
  @Override
  public boolean equals(Object obj) {
    boolean equals = Pathway.class.isAssignableFrom(obj.getClass());
    if(equals) {    
      Pathway o = (Pathway)obj;
      equals &= o.isSetName()==this.isSetName();
      if(equals && isSetName()) 
        equals &= (o.getName().equals(this.getName()));
      
      equals &= o.isSetOrg()==this.isSetOrg();
      if(equals && isSetOrg()) 
        equals &= (o.getOrg().equals(this.getOrg()));
      
      equals &= o.isSetNumber()==this.isSetNumber();
      if(equals && isSetNumber()) 
        equals &= (o.getNumber()==this.getNumber());
      
      equals &= o.isSetTitle()==this.isSetTitle();
      if(equals && isSetTitle()) 
        equals &= (o.getTitle().equals(this.getTitle()));
      
      equals &= o.isSetImage()==this.isSetImage();
      if(equals && isSetImage()) 
        equals &= (o.getImage().equals(this.getImage()));

      equals &= o.isSetLink()==this.isSetLink();
      if(equals && isSetLink()) 
        equals &= (o.getLink().equals(this.getLink()));      
      
      equals &= o.isSetEntries()==this.isSetEntries();
      if(equals && isSetEntries()) 
        equals &= (o.getEntries().equals(this.getEntries()));
      
      equals &= o.isSetReactions()==this.isSetReactions();
      if(equals && isSetReactions()) 
        equals &= (o.getReactions().equals(this.getReactions()));
      
      equals &= o.isSetRelations()==this.isSetRelations();
      if(equals && isSetRelations()) 
        equals &= (o.getRelations().equals(this.getRelations()));
      
    }
    return equals;
  }

  /**
   * Generates a HTML-IMG-Tag from the given KEGG Id, that should
   * point to an image for the compound.
   * @param ko_id starting with "cpd:"
   * @return an html image tag, containing a preview picture for the given compound.
   */
  public static String getCompoundPreviewPicture(String ko_id) {
    return String.format("<img src=\"http://www.kegg.jp/Fig/compound/%s.gif\"/><br/>\n", ko_id.trim().substring(4).toUpperCase() );
  }
  
  /**
   * Generates a HTML-IMG-Tag, preferably from ChEBI but with fallback to the 
   * given KEGG Id, that should point to an image for the compound.
   * @param ko_id starting with "cpd:"
   * @param infos API information for this ID
   * @return an html image tag, containing a preview picture for the given compound.
   */
  public static String getCompoundPreviewPicture(String ko_id, KeggInfos infos) {
    return getCompoundPreviewPicture(ko_id, infos, false);
  }
  
  /**
   * Actually just for path2models: returns a compounds preview image from ChEBI
   * instead of using the KEGG compound image, if {@code useChEBI} is {@code true}.
   * @see #getCompoundPreviewPicture(String, KeggInfos)
   * @param ko_id
   * @param infos
   * @param useChEBI
   * @return
   */
  public static String getCompoundPreviewPicture(String ko_id, KeggInfos infos, boolean useChEBI) {
    
    // Get ChEBI from KEGG API
    // XXX: Deactivated, because JSBML cannot write '&' characters.
    // NOTE: If multiple ChEBI ids (e.g., "1234 5678") are contained, this will also
    // be considerd a non-valid ChEBI ID and cause a Fallback to KEGG.
    if (useChEBI && (infos!=null && infos.queryWasSuccessfull())) {
      String ChEBI = infos.getChebi();
      if (ChEBI!=null) {
        // Extract numerical suffix (pattern is CHEBI:\\d+), but
        // API sometimes just returns \\d+
        String numericalID = null;
        if (!Utils.isNumber(ChEBI, true)) {
          ChEBI = ChEBI.toUpperCase().trim();
          if (ChEBI.startsWith("CHEBI:")) ChEBI = ChEBI.substring(6).trim();
          if (Utils.isNumber(ChEBI, true)) {
            numericalID = ChEBI;
          }
        } else {
          numericalID = ChEBI;
        }
        
        // If we could extract a valid ChEBI numer, use this for generating an image.
        if (numericalID!=null && numericalID.length()>0) {
          return String.format("<img src=\"http://www.ebi.ac.uk/chebi/displayImage.do?defaultImage&#61;true&#38;chebiId&#61;%s&#38;dimensions&#61;300\"/><br/>\n", ChEBI);
        }
      }
    }
    
    // Fallback on KEGG ID.
    return getCompoundPreviewPicture(ko_id);
  }
  
  /**
   * Generates a HTML-IMG-Tag from the given KEGG Id, that should
   * point to an image for the pathway. 
   * @param ko_id starting with "path:"
   * @return an html image tag, containing a preview picture for the given referenced pathway.
   */
  public static String getPathwayPreviewPicture(String ko_id) {
    // KEGG provides picture for referenced pathways (e.g., "path:hsa00620" => "map00620.gif").
    String mapNumber = Utils.getNumberFromStringRevAsString(ko_id.length(), ko_id);
    return String.format("<img src=\"http://www.kegg.jp/kegg/misc/thumbnail/map%s.gif\"/><br/>\n", mapNumber );
  }
  
  /**
   * Generates a HTML-IMG-Tag from the given KEGG Id, that should
   * point to an image for the reaction. 
   * @param ko_id starting with "rn:"
   * @return an html image tag, containing a preview picture for the given reaction.
   */
  public static String getReactionPreviewPicture(String ko_id) {
    return getReactionPreviewPicture(ko_id, true);
  }
  
  /**
   * See {@link #getReactionPreviewPicture(String)}.
   * @param ko_id
   * @param withIMGtag if {@code true}, the URL will be embedded into an HTML
   * image tag. If {@code false}, the raw URL will be returned.
   * @return
   */
  public static String getReactionPreviewPicture(String ko_id, boolean withIMGtag) {
    if (withIMGtag) {
      return String.format("<img src=\"http://www.genome.jp/Fig/reaction/%s.gif\"/><br/>\n", KeggInfos.suffix(ko_id.toUpperCase()) );
    } else {
      return "http://www.genome.jp/Fig/reaction/" + KeggInfos.suffix(ko_id.toUpperCase()) + ".gif";
    }
  }

  public String getAdditionalText() {
    return additionalText;
  }

  public void setAdditionalText(String additionalText) {
    this.additionalText = additionalText;
  }
  
  public boolean isSetAdditionalText() {
    if(additionalText!=null && !additionalText.isEmpty())
      return true;
    return false;
  }

  /**
   * @param e
   * @param newName
   */
  public void changeNameOfEntry(Entry e, String newName) {
    String oldName = e.getName();
    e.setName(newName); // takes care of the "nameMap".
    
    // Take care of reactionComponents
    Collection<Reaction> o = reactionComponents.get(oldName);
    if (o!=null) {
      reactionComponents.remove(oldName);
      reactionComponents.put(newName, o);
      
      // Rename in reactions
      for (Reaction r : o) {
        r.getReactant(oldName).setName(newName);
      }
    }
    
  }
}
