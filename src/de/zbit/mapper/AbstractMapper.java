/*
 * $Id$
 * $URL$
 * ---------------------------------------------------------------------
 * This file is part of the SysBio API library.
 *
 * Copyright (C) 2011 by the University of Tuebingen, Germany.
 *
 * This library is free software; you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation. A copy of the license
 * agreement is provided in the file named "LICENSE.txt" included with
 * this software distribution and also available online as
 * <http://www.gnu.org/licenses/lgpl-3.0-standalone.html>.
 * ---------------------------------------------------------------------
 */
package de.zbit.mapper;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import de.zbit.io.CSVReader;
import de.zbit.util.AbstractProgressBar;
import de.zbit.util.ArrayUtils;
import de.zbit.util.FileDownload;
import de.zbit.util.FileTools;
import de.zbit.util.ProgressBar;
import de.zbit.util.Timer;
import de.zbit.util.prefs.Option;

/**
 * An abstract mapper that can download a csv file or read an
 * supplied / already downloaded file and build an internal map
 * from one column to another.
 * Afterwards, one sourceIdentifier can be mapped to the
 * corresponding targetIdentifier.
 * @author Clemens Wrzodek
 * @version $Rev$
 */
public abstract class AbstractMapper<SourceType, TargetType> implements Serializable {
  private static final long serialVersionUID = -1940567043534334136L;
  
  public static final Logger log = Logger.getLogger(AbstractMapper.class.getName());
  
  private Class<TargetType> targetType;
  private Class<SourceType> sourceType;
  
  /**
   * A progress Bar that is used while downloading a File.
   * If null, no progress will be displayed.
   */
  private AbstractProgressBar progress=null;
  
  /**
   * A boolean flag, wether {@link #readMappingData()} has been
   * called or not.
   */
  private boolean isInizialized=false;
  
  /**
   * Contains a mapping from RefSeq to GeneID.
   * XXX: Hier eventuell eine initial Capacity oder load factor angeben, falls BottleNeck.
   */
  protected Map<SourceType, TargetType> mapping = new HashMap<SourceType, TargetType>();

  /**
   * Inintializes the mapper. Downloads and reads the mapping
   * file automatically as required.
   * @throws IOException
   */
  public AbstractMapper(Class<SourceType> sourceType, Class<TargetType> targetType) throws IOException {
    this(sourceType,targetType,null);
  }
  
  /**
   * Inintializes the mapper. Downloads and reads the mapping
   * file automatically as required.
   * @see AbstractMapper#AbstractMapper(Class, Class)
   */
  public AbstractMapper(Class<SourceType> sourceType, Class<TargetType> targetType, AbstractProgressBar progress) throws IOException {
    super();
    this.progress = progress;
    this.sourceType=sourceType;
    this.targetType=targetType;
  }
  
  
  /**
   * Returns the HTTP URL of the latest mapping file.
   * @return
   */
  public abstract String getRemoteURL();
  
  /**
   * Returns the local file name where the downloaded file should be saved to.
   * @return
   */
  public abstract String getLocalFile();
  /**
   * This may be overwritten instead of {@link #getLocalFile()}.
   * This method is preferred if it does not return null.
   * 
   * For eventual downloads, the return value of {@link #getLocalFile()}
   * is still used!
   * @return
   */
  public String[] getLocalFiles() {
    return null;
  }

  /**
   * Return a simple name what is mapped to what
   * (e.g. "RefSeq2GeneID").
   * @return
   */
  public abstract String getMappingName();
  
  /**
   * @param r - the CSVReader can OPTIONALLY be used to infere the column number.
   * @return the target column (e.g. 2)
   */
  public abstract int getTargetColumn(CSVReader r);
  /**
   * @param r - the CSVReader can OPTIONALLY be used to infere the column number.
   * @return the source column (e.g. 1)
   */
  public abstract int getSourceColumn(CSVReader r);
  
  /**
   * This may be overwritten instead of {@link #getSourceColumn(CSVReader)}.
   * This method is preferred if it does not return null.
   * @param r
   * @return
   */
  public int[] getMultiSourceColumn(CSVReader r) {
    return null;
  }
  

  
  /**
   * Returns true if the {@link #localFile} has already been downloaded from
   * {@link #downloadURL}.
   * @return
   */
  public boolean isCachedDataAvailable() {
    // Check multi files
    if (getLocalFiles()!=null) {
      for (String localFile: getLocalFiles()) {
        if (localFile!=null && localFile.length()>0) {
          if (FileTools.checkInputResource(localFile, this.getClass())) return true;
        }
      }
    }
    
    // Check single file
    return (FileTools.checkInputResource(getLocalFile(), this.getClass()));
  }
  
  /**
   * Downloads the {@link #downloadURL} and stores the path of the downloaded
   * file in {@link #localFile}.
   * If the download was successfull can be checked with {@link #isCachedDataAvailable()}.
   */
  private void downloadData() {
    String localFile = getLocalFile();
    if (localFile==null) {
      if (getLocalFiles()!=null && getLocalFiles().length>0 && getLocalFiles()[0]!=null) {
        localFile = getLocalFiles()[0];
      }
    }
    FileDownload.ProgressBar = progress;
    try {
      // Create parent directories
      new File(new File(localFile).getParent()).mkdirs();
    } catch (Throwable t){};
    String localf = FileDownload.download(getRemoteURL(), localFile);
    if (localFile!=null && localFile.length()>0) localFile = localf;
    if (FileDownload.ProgressBar!=null) {
      ((ProgressBar)FileDownload.ProgressBar).finished();
    }
  }

  
  /**
   * Reads the mapping from {@link #localFile} into the {@link #mapping} set.
   * Downloads data automatically from {@link #downloadURL} as required.
   * @return true if and only if everything was without critical errors.
   * @throws IOException
   */
  public boolean readMappingData() throws IOException {
    isInizialized=true;
    String[] localFiles = ArrayUtils.merge(getLocalFiles(), getLocalFile());
    
    if (!isCachedDataAvailable()) {
      if (getRemoteURL()!=null) {
        downloadData();
      } else {
        log.severe("Mapping file for " + getMappingName() + " not available and no download URL is known.");
      }
      if (!isCachedDataAvailable()) {
        return false;
      }
    }
    
    // Parse all files.
    Timer t = new Timer();
    for (String localFile: localFiles) {
      log.config("Reading " + getMappingName() + " mapping file " + localFile);
      CSVReader r = new CSVReader(localFile);
      r.setUseParentPackageForOpeningFiles(this.getClass());
      int[] multiSourceColumn = getMultiSourceColumn(r);
      if (multiSourceColumn==null || multiSourceColumn.length<1)
        multiSourceColumn = new int[]{getSourceColumn(r)};
      int targetColumn = getTargetColumn(r);
      
      // Get maximumal col number
      int maxColumn = targetColumn;
      for (int sourceColumn: multiSourceColumn)
        maxColumn = Math.max(maxColumn, sourceColumn);
      
      if (targetColumn<0 || ArrayUtils.indexOf(multiSourceColumn, -1)>=0) {
        log.severe("Could not get columns for '" + localFile + "' mapping file.");
        return false;
      }
      
      // Read RefSeq <=> Gene ID mapping.
      String[] line;
      r.open();
      // XXX: When using a progressBar here with a compressed File, the bar Fails!
      while ((line = r.getNextLine())!=null) {
        if (line.length<=maxColumn) {
          log.severe("Incomplete entry in mapping file '" + localFile + "'. Please try to delete this file and execute this application again.");
          continue;
        }
        
        
        // Get target ID
        if (line[targetColumn].length()==0) {
          log.finest("Empty target in " + getMappingName() + " mapping file.");
          continue;
        }
        TargetType target = Option.parseOrCast(targetType, line[targetColumn]);
        if (target==null) {
          log.warning("Invalid target content in " + getMappingName() + " mapping file: " + ((line.length>1)?line[targetColumn]:"line too short."));
          continue;
        }
        
        // Optional method that allow customization.
        target = postProcessTargetID(target);
        
        // Add mapping for all source columns
        for (int sourceColumn: multiSourceColumn) {
          // Get source ID
          SourceType source = Option.parseOrCast(sourceType, line[sourceColumn]);
          if (source==null) {
            log.warning("Invalid source content in " + getMappingName() + " mapping file: " + ((line.length>1)?line[sourceColumn]:"line too short."));
            continue;
          }
          source = postProcessSourceID(source);
          mapping.put(source, target);
        }
      }
    }
    
    log.config("Parsed " + getMappingName() + " mapping file in " + t.getNiceAndReset()+". Read " + ((mapping!=null)?mapping.size():"0") + " mappings.");
    return (mapping!=null && mapping.size()>0);
  }

  /**
   * Optional method that allow customizations.
   * @param target
   * @return
   */
  protected TargetType postProcessTargetID(TargetType target) {
    return target;
  }
  
  /**
   * Optional method that allow customizations.
   * @param source
   * @return
   */
  protected SourceType postProcessSourceID(SourceType source) {
    return source;
  }
  

  /**
   * Returns the TargetID for the given SourceID.
   * @param sourceID
   * @return TargetType targetID
   * @throws Exception - if mapping data could not be read (in general).
   */
  public TargetType map(SourceType sourceID) throws Exception {
    if (!isInizialized) init();
    if (!isReady()) throw new Exception(getMappingName()+" mapping data has not been read successfully.");
    SourceType trimmedInput = postProcessSourceID(sourceID);
    TargetType ret = mapping.get(trimmedInput);
    log.finest("map: " + trimmedInput + ", to: " + ret);
    return ret;
  }

  protected void init() throws IOException {
    if (!readMappingData()) mapping=null;
  }

  /**
   * @return true if and only if the data has been read and
   * mapping data is available.
   */
  public boolean isReady() {
    return mapping!=null && mapping.size()>0;
  }
  
}