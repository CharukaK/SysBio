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
package de.zbit.util.prefs;

import java.io.File;
import java.io.Serializable;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import de.zbit.io.csv.CSVReader;
import de.zbit.io.filefilter.GeneralFileFilter;
import de.zbit.util.Reflect;
import de.zbit.util.ResourceManager;
import de.zbit.util.StringUtil;
import de.zbit.util.Utils;
import de.zbit.util.objectwrapper.ValuePair;

/**
 * A collection of ranges with a few convenient methods to work with them.
 * @author Clemens Wrzodek
 * @author Andreas Dr&auml;ger
 * @version $Rev$
 * @param <Type>
 * @since 1.0
 */
public class Range<Type> implements Serializable, Comparable<Range<Type>> {
  
  /**
   * Generated serial version identifier.
   */
  private static final long serialVersionUID = 5376431888251283263L;
  
  /**
   * The {@link Logger} of this {@link Class}.
   */
  public static final Logger logger = Logger.getLogger(Range.class.getName());
  
  /**
   * 
   * @author Andreas Dr&auml;ger
   * @version $Rev$
   */
  public static enum Relation {
    /**
     * Less than
     */
    LT,
    /**
     * Less or equal than
     */
    LE,
    /**
     * Equal to
     */
    EQ,
    /**
     * Greater than or equal to
     */
    GE,
    /**
     * Greater than
     */
    GT;
    
    /**
     * @return
     */
    public String getRelationSymbol() {
      switch (this) {
        case LT:
          return "<";
        case LE:
          return "<=";
        case EQ:
          return "==";
        case GE:
          return ">=";
        case GT:
          return ">";
        default:
          return null;
      }
    }
  }
  
  /**
   * If {@link #getAllAcceptableValues()} is called, if there are more than
   * this much acceptable values, null is returned. E.g. between {@link Double} 0.0
   * and 1.0 there are infinite many values, but between integer 0 and 10, there
   * only a finite number of values.
   */
  private final static short defaultMaxAcceptableValuesToReturn = 50;
  
  /**
   * A range of any type. Consisting of lower and upper bound and
   * the information, if the lower/upper bound itself is included
   * or not.
   * @author Clemens Wrzodek
   */
  class SubRange implements Serializable {
    /**
     * Generated serial version identifier.
     */
    private static final long serialVersionUID = 6791332324080634829L;
    /**
     * 
     */
    private Type lBound;
    /**
     * 
     */
    private Type uBound;
    /**
     * 
     */
    private boolean excludingLBound=false;
    /**
     * 
     */
    private boolean excludingUBound=false;
    
    /**
     * @return
     */
    @SuppressWarnings("unchecked")
    public Type getMinimum() {
      if (!excludingLBound) {
        return lBound;
      } else if (Utils.isInteger(lBound.getClass())) {
        return (Type) Option.parseOrCast(lBound.getClass(),((Double)lBound)+1);
      } else if (Float.class.isAssignableFrom(lBound.getClass())) {
        return (Type) Option.parseOrCast(lBound.getClass(),((Float)lBound)+Float.MIN_NORMAL);
      } else if (Double.class.isAssignableFrom(lBound.getClass())) {
        return (Type) Option.parseOrCast(lBound.getClass(),((Double)lBound)+Double.MIN_NORMAL);
      } else {
        // Fallback...
        return lBound;
      }
    }
    
    /**
     * @return
     */
    @SuppressWarnings("unchecked")
    public Type getMaximum() {
      if (!excludingUBound) {
        return uBound;
      } else if (Utils.isInteger(uBound.getClass())) {
        return (Type) Option.parseOrCast(uBound.getClass(),((Double)uBound)-1);
      } else if (Float.class.isAssignableFrom(uBound.getClass())) {
        return (Type) Option.parseOrCast(uBound.getClass(),((Float)uBound)-Float.MIN_NORMAL);
      } else if (Double.class.isAssignableFrom(uBound.getClass())) {
        return (Type) Option.parseOrCast(uBound.getClass(),((Double)uBound)-Double.MIN_NORMAL);
      } else {
        // Fallback...
        return uBound;
      }
    }
    
    /**
     * 
     */
    private SubRange() {
      super();
    }
    
    /**
     * 
     * @param value
     */
    public SubRange(Type value) {
      this (value, value);
    }
    
    /**
     * 
     * @param lowerBound
     * @param upperBound
     */
    public SubRange (Type lowerBound, Type upperBound) {
      this (lowerBound, upperBound, false, false);
    }
    
    /**
     * 
     * @param lowerBound
     * @param upperBound
     * @param excludingLBound
     * @param excludingUBound
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public SubRange (Type lowerBound, Type upperBound, boolean excludingLBound, boolean excludingUBound) {
      super();
      lBound = lowerBound;
      uBound = upperBound;
      
      this.excludingLBound = excludingLBound;
      this.excludingUBound = excludingUBound;
      
      // Ensure, that lBound is always smaller than uBound
      if (lBound instanceof Comparable) {
        if (((Comparable) lBound).compareTo((uBound)) > 0) {
          // SWAP
          Type temp = lBound;
          lBound=uBound;
          uBound=temp;
          
          boolean temp2 = this.excludingLBound;
          this.excludingLBound = this.excludingUBound;
          this.excludingUBound = temp2;
        }
      }
    }
    
    
    /**
     * Returns true, if and only if the given value is in the
     * Range defined by this SubRange.
     * @param value
     * @return
     */
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public boolean isInRange(Type value) {
      if (value instanceof Comparable) {
        try {
          // Check lower bound
          int r = ((Comparable) value).compareTo((lBound));
          if ((r < 0) || ((r == 0) && excludingLBound)) {
            return false;
          }
          
          // Check upper bound
          r = ((Comparable) value).compareTo((uBound));
          if ((r > 0) || ((r == 0) && excludingUBound)) {
            return false;
          }
        } catch (Exception e) {
          // E.g., when a float is expected and value is "Hallo", a
          // Float cannot be cast to String Exception is raised when
          // casting to comparable
          return false;
        }
        return true;
      } else if (lBound.equals(uBound)) {
        // Check absolute value
        if (!excludingLBound && !excludingUBound) {
          // Special treatment for classes
          if (value instanceof Class) {
            if (((Class) value).getSimpleName().equals(lBound)) {
              return true;
            }
          }
          //---
          if (value.equals(lBound)) {
            return true;
          } else {
            return false;
          }
        } else {
          return false;
        }
      } else {
        logger.warning(String.format(ResourceManager.getBundle("de.zbit.locales.Warnings")
          .getString("CLASS_NOT_COMPARABLE"), value.getClass().getName()));
        return false;
      }
    }
    
    /**
     * If there are less or equal than 'maximumToReturn'
     * acceptable values, all those are returned. Else: null is returned.
     * @param maximumToReturn
     * @return
     */
    public List<Type> getAllAcceptableValues(int maximumToReturn) {
      List<Type> r = new LinkedList<Type>();
      
      // Check absolute value
      if (lBound.equals(uBound)) {
        if (!excludingLBound && !excludingUBound) {
          r.add(lBound);
        }
        return r;
      }
      
      // Check if ranges have a finite length
      double start = 0, end = 0;
      if (Utils.isInteger(lBound.getClass())) {
        start = ((Number) lBound).doubleValue();
        end = ((Number) uBound).doubleValue();
        
      } else if (lBound instanceof Character) {
        start = (Character) lBound;
        end = (Character) uBound;
        
      } else {
        // E.g. Strings
        return null;
      }
      
      // Exit if too much choices and handle bounds
      if ((end-start)>maximumToReturn) {
        return null;
      }
      if (!excludingLBound) {
        addNumberOrCharacter(lBound.getClass(), r, start);
      }
      // Collect all possible choices
      for (start+=1; start<end; start++) {
        addNumberOrCharacter(lBound.getClass(), r, start);
      }
      if (!excludingUBound) {
        addNumberOrCharacter(lBound.getClass(), r, end);
      }
      
      return r;
    }
    
    /**
     * Add a value to a list.
     * @param Type - the Type of 'val'
     * @param list - the list to add the value 'val'.
     * @param val - value, actually if type Type. It will be converted into type
     * and added to the list.
     * @return
     */
    @SuppressWarnings("unchecked")
    private boolean addNumberOrCharacter(Class<?> Type, List<Type> list, double val) {
      //Integer.decode(nm)
      //Object re = Reflect.invokeIfContains(Type, "decode", String.class, Double.toString(val));
      
      // Make an Integer value of the double
      String strVal = Double.toString(val);
      int pos = strVal.indexOf('.');
      if (pos>0) {
        strVal = strVal.substring(0, pos);
      }
      
      // Parse it into Type and add it to the list.
      Object re;
      try {
        re = Reflect.invokeParser(Type, strVal);
      } catch (Throwable e) {
        re=null;
      }
      if (re == null) {
        // Character
        if (Type.equals(Character.class)) {
          ((List<Character>)list).add(( (char)val) );
          return true;
        } else {
          try {
            list.add( (Type)Type.cast(val) );
          } catch(Throwable t) {return false;}
        }
      } else {
        try {
          list.add((Type) (re));
        } catch(Throwable t) {
          return false;
        }
        return true;
      }
      return false;
    }
  }
  
  /*
   * END OF SUB-CLASSES
   */
  
  /**
   * The list of all subRanges in this Range.
   * (e.g. {1,2,3} has subRanges "1","2" and "3".
   */
  private List<SubRange> ranges = new LinkedList<SubRange>();
  
  /**
   * The original RangeString.
   */
  private String rangeString;
  
  /**
   * The class object of the Type.
   */
  private Class<Type> typee;
  
  /**
   * Additional constraints to restrict an input.
   */
  private Object constraints = null;
  
  /**
   * If this Range has been initialized with
   * {@link #Range(Class, Collection)}, this is the original
   * list of acceptable values.
   */
  private List<Type> listOfAccpetedObjects = null;
  
  /**
   * @return the constraints
   */
  public Object getConstraints() {
    return constraints;
  }
  
  /**
   * 
   * @param requiredType must be an instance of {@link File}!
   * @param filter
   */
  public Range(Class<Type> requiredType, GeneralFileFilter filter) {
    this(requiredType);
    if (!requiredType.isAssignableFrom(File.class)) {
      throw new IllegalArgumentException(
        String.format(ResourceManager.getBundle("de.zbit.locales.Warnings")
          .getString("REQUIRED_TYPE_FOR_X_MUST_BE"), GeneralFileFilter.class
          .getName(), File.class.getName()));
    }
    constraints = filter;
  }
  
  /**
   * This is a convenient constructors that builds a range string from
   * a list of all acceptable objects automatically.
   * 
   * @see {@link #Range(Class, String)} for more information.
   * @param requiredType
   * @param acceptedObjects
   */
  public Range(Class<Type> requiredType, Iterable<Type> acceptedObjects) {
    this(requiredType, Range.toRangeString(acceptedObjects));
    // This requires a list => Convert to list
    if (!(acceptedObjects instanceof List<?>)) {
      List<Type> acceptedObjects2 = new LinkedList<Type>();
      for (Type t: acceptedObjects) {
        acceptedObjects2.add(t);
      }
      acceptedObjects = acceptedObjects2;
    }
    this.setListOfAccpetedObjects((List<Type>) acceptedObjects);
  }
  
  /**
   * This is a convenient constructors that builds a range string from
   * am {@link Enumeration} of all acceptable objects automatically.
   * 
   * @see {@link #Range(Class, String)} for more information.
   * @param requiredType
   * @param acceptedObjects
   */
  public Range(Class<Type> requiredType, Enumeration<Type> acceptedObjects) {
    this(requiredType, Collections.list(acceptedObjects));
  }
  
  /**
   * This is a convenient constructor that builds a range string from
   * a list of all acceptable object automatically.
   * 
   * @param requiredType
   * @param acceptedObjects
   */
  public Range(Class<Type> requiredType, Type... acceptedObjects) {
    this(requiredType, Arrays.asList(acceptedObjects));
  }
  
  /**
   * 
   * @param <T>
   * @param requiredType
   * @param relation
   * @param option
   */
  public <T extends Comparable<Type>> Range(Class<Type> requiredType,
    Relation relation, Option<T> option) {
    this(requiredType);
    this.constraints = new ValuePair<Relation, Option<T>>(relation, option);
  }
  
  /**
   * <p><var>rangeSpec</var> is an optional range specification,
   * placed inside curly braces, consisting of a
   * comma-separated list of range items each specifying
   * permissible values for the option. A range item may be an
   * individual value, or it may itself be a subrange,
   * consisting of two individual values, separated by a comma,
   * and enclosed in square or round brackets. Square and round
   * brackets denote closed and open endpoints of a subrange, indicating
   * that the associated endpoint value is included or excluded
   * from the subrange.
   * The values specified in the range spec need to be
   * consistent with the type of value expected by the option.
   *
   * <p><b>Examples:</b>
   *
   * <p>A range spec of {@code {2,4,8,16}} for an integer
   * value will allow the integers 2, 4, 8, or 16.
   *
   * <p>A range spec of {@code {[-1.0,1.0]}} for a floating
   * point value will allow any floating point number in the
   * range (including) -1.0 to 1.0.
   * 
   * <p>A range spec of {@code {(-88,100],1000}} for an integer
   * value will allow values > -88 and <= 100, as well as 1000.
   *
   * <p>A range spec of {@code {"foo", "bar", ["aaa","zzz")} } for a
   * string value will allow strings equal to {@code "foo"} or
   * {@code "bar"}, plus any string lexically greater than or equal
   * to {@code "aaa"} but less then {@code "zzz"}.
   *
   * @param requiredType - The class object of the Type.
   * @param rangeSpec - as defined above.
   */
  public Range(Class<Type> requiredType, String rangeSpec) {
    this(requiredType);
    this.rangeString = rangeSpec;
    try {
      parseRangeSpec(rangeSpec);
    } catch (ParseException exc) {
      /*
       * We cannot throw this exception because in interfaces it is impossible
       * to catch these.
       */
      throw new IllegalArgumentException(rangeSpec, exc);
    }
  }
  
  /**
   * 
   * @param requiredType
   */
  private Range(Class<Type> requiredType) {
    super();
    this.typee = requiredType;
    this.rangeString = "";
    this.constraints = null;
  }
  
  /**
   * Preserve the original list, if this range has been initialized with
   * {@link #Range(Class, Collection)} !
   * @param acceptedObjects
   */
  @SuppressWarnings({ "unchecked", "rawtypes" })
  private void setListOfAccpetedObjects(List<Type> acceptedObjects) {
    this.listOfAccpetedObjects = acceptedObjects;
    // KEEP LIST SORTED!
    try {
      if (Comparable.class.isAssignableFrom(typee)) {
        Collections.sort((List<? extends Comparable>)listOfAccpetedObjects);
      }
    } catch (Throwable t) {
      t.printStackTrace();
    }
  }
  
  /**
   * Checks whether additional side constraints have been set.
   * @return
   */
  public boolean isSetConstraints() {
    return constraints != null;
  }
  
  /**
   * The source String that has been used to build this class.
   * @return
   */
  public String getRangeSpecString() {
    return rangeString;
  }
  
  /**
   * Add a SubRange to this collection of ranges.
   * @param range
   */
  private void addRange(SubRange range) {
    ranges.add(range);
  }
  
  /**
   * Add a SubRange to this collection of ranges.
   * @param lowerBound
   * @param upperBound
   */
  private void addRange(Type lowerBound, Type upperBound) {
    ranges.add(new SubRange(lowerBound, upperBound));
  }
  
  /**
   * If there are less or equal than {@link #defaultMaxAcceptableValuesToReturn}
   * acceptable values, all those are returned. Else: null is returned.
   * @return
   */
  public List<Type> getAllAcceptableValues() {
    return getAllAcceptableValues(defaultMaxAcceptableValuesToReturn);
  }
  
  /**
   * If there are less or equal than 'maximumToReturn'
   * acceptable values, all those are returned. Else: null is returned.
   * @param maximumToReturn
   * @return
   */
  public List<Type> getAllAcceptableValues(int maximumToReturn) {
    if (listOfAccpetedObjects!=null) {
      return listOfAccpetedObjects;
    }
    List<Type> ret = new LinkedList<Type>();
    for (SubRange r : ranges) {
      List<Type> newItems = r.getAllAcceptableValues(maximumToReturn);
      // Too many elements, or invalid Type
      if (newItems==null) {
        return null;
      }
      for (Type type : newItems) {
        if (!ret.contains(type)) {
          ret.add(type);
        }
      }
      if (ret.size() > maximumToReturn) {
        return null;
      }
    }
    return ret;
  }
  
  /**
   * 
   * @param value
   * @return
   */
  public boolean isInRange(Type value) {
    return isInRange(value, null);
  }
  
  /**
   * Checks, if the given value is in range of all ranges. See also
   * {@link #castAndCheckIsInRange(Object)}.
   * 
   * @param value
   * @param props
   * @return
   */
  //@SuppressWarnings("unchecked")
  public boolean isInRange(Type value, SBProperties props) {
    if (isSetConstraints()) {
      if (constraints instanceof GeneralFileFilter) {
        // in this case, Type must be a String or a File.
        File file;
        if (value instanceof String) {
          file = new File(value.toString());
        } else {
          file = (File) value;
        }
        return ((GeneralFileFilter) constraints).accept(file);
        
        /*} else if (constraints instanceof ValuePair<?, ?>) {
	      // XXX: What is this used for, besides the (FINE!) log-message???
			  Relation relation = (Relation) ((ValuePair<?, ?>) constraints).getA();
        Option<? extends Comparable<Type>> option = (Option<? extends Comparable<Type>>) ((ValuePair<?, ?>) constraints)
            .getB();
        if ((props != null) && (props.containsKey(option))) {
          Comparable<Type> v = ((Comparable<Type>) option.getValue(props));
          String message = "The value %s for option %s ";
          switch (relation) {
            case LT:
              message += (v.compareTo(value) < 0) ? "is" : "is not";
            case LE:
              message += (v.compareTo(value) <= 0) ? "is" : "is not";
            case EQ:
              message += (v.compareTo(value) == 0) ? "is" : "is not";
            case GE:
              message += (v.compareTo(value) >= 0) ? "is" : "is not";
            case GT:
              message += (v.compareTo(value) > 0) ? "is" : "is not";
            default:
              break;
          }
          message += ' ' + relation.getRelationSymbol() + ' ';
          message += value;
          logger.fine(String.format(message, v, option));
        }*/
      }
    }
    // Special treatment for concrete lists (e.g., for classes).
    if (listOfAccpetedObjects != null) {
      if (value instanceof Class) {
        for (Type clazz : listOfAccpetedObjects) {
          if (((Class<?>) clazz).isAssignableFrom((Class<?>) value)) {
            return true;
          }
        }
        return false;
      }
      return listOfAccpetedObjects.contains(value);
    }
    for (SubRange r : ranges) {
      if (r.isInRange(value)) {
        return true;
      }
    }
    return false;
  }
  
  /**
   * A convenient wrapper method, that calls {@link Option#parseOrCast(Class, Object)}
   * before calling {@link #isInRange(Object)}.
   * 
   * @see Option#castAndCheckIsInRange(Object)
   * @param value
   * @return
   * @deprecated use {@link #isInRange(Object, SBProperties)}
   */
  @SuppressWarnings("unused")
  @Deprecated
  private boolean castAndCheckIsInRange(Object value, SBProperties props) {
    Type value2 = Option.parseOrCast(typee, value);
    if (value2 == null) {
      return false;
    }
    return isInRange(value2, props);
  }
  
  
  /**
   * Parse the range specification string into a more convenient
   * data structure. See {@link #getRangeSpecification()} for
   * more information on possible Strings.
   * @param range
   * @return rangeCollection<Type>
   * @throws ParseException
   */
  private void parseRangeSpec(String range) throws ParseException {
    int positionTracker = 0;
    try {
      range = range.substring(range.indexOf('{') + 1, range.lastIndexOf('}'));
      // Be carefull with " and '
      //String[] items = range.split(Pattern.quote(","));
      List<Character> stringSep = new LinkedList<Character>();
      stringSep.add('\''); stringSep.add('"');
      String[] items = CSVReader.getSplits(range, ',', true, true, stringSep);
      
      SubRange r = null;
      for (int i = 0; i < items.length; i++) {
        positionTracker += items[i].length() + 1; // +1 for the ','
        String item = items[i].trim();
        String item2 = item;
        r = new SubRange();
        
        // Check if we have a range
        char c = item.charAt(0);
        if ((c == '(') || (c == '[')) {
          if (c == '(') {
            r.excludingLBound = true;
          }
          i++;
          item = item.substring(1);
          item2 = items[i].trim();
          
          c = item2.charAt(item2.length()-1);
          if (c!=')' && c!=']') {
            throw new Exception();
          } else if (c==')') {
            r.excludingUBound=true;
          }
          item2 = item2.substring(0, item2.length()-1);
        }
        
        // Trim the string indicators
        for (Character sep : stringSep) {
          if ((item.length() > 2) && (item.charAt(0) == sep) && (item.charAt(item.length()-1) == sep)) {
            item = item.substring(1, item.length() - 1);
          }
          if ((item2.length() > 2) && (item2.charAt(0) ==sep) && (item2.charAt(item2.length() - 1) == sep)) {
            item2 = item2.substring(1, item2.length() - 1);
          }
        }
        
        r.lBound = Option.parseOrCast(typee, item);
        r.uBound = Option.parseOrCast(typee, item2);
        
        addRange(r);
      }
      
    } catch (Exception e) {
      // Erase it, so that other methods can see that it is invalid.
      if (range.equals(this.rangeString)) {
        this.rangeString=null;
      }
      logger.log(Level.SEVERE, "Invalid range string.", e);
      
      throw new ParseException(String.format(ResourceManager.getBundle(
          "de.zbit.locales.Warnings").getString("RANGE_IN_WRONG_FORMAT"),
        (range == null ? "null" : range)), positionTracker);
      //e.printStackTrace();
    }
    
  }
  
  /**
   * Returns a List of type String, that is parsable as Range by the
   * Range class.
   * @param <Type>
   * @param acceptedObjects - a simple list of all acceptable objects.
   * @return String
   */
  @SuppressWarnings({ "unchecked" })
  public static <Type> String toRangeString(Iterable<Type> acceptedObjects) {
    Iterable<Type> accObjects = acceptedObjects;
    
    //If the range consists of classes, use the simple class names
    if ((acceptedObjects != null) && (Class.class.isAssignableFrom(acceptedObjects.iterator().next().getClass()))) {
      List<Type> classStrings = new LinkedList<Type>();
      for (Type object : acceptedObjects) {
        /* Simple name for classes doesn't work. It is not precise and
         * causes errors at other positions...
         * Therefore, getName is the only possible option here.
         */
        classStrings.add((Type) ((Class<Type>) object).getName());
      }
      accObjects = classStrings;
    }
    
    String s = '{' + StringUtil.implode(StringUtil.addPrefixAndSuffix(
      accObjects, "\"", "\""), ",") + '}';
    
    logger.finer(String.format("Created a new range-string from a collection: %s", s));
    return s;
  }
  
  /**
   * Returns a List of type String, that is parsable as Range by the
   * Range class.
   * 
   * <p>Tries to gain a nice formatting, e.g., {@link Class}es are
   * represented by their simple name.
   * <p>NOTE: THIS METHOD IS FOR *NICE* FORMATTED RANGES.
   * IT IS *NOT* FOR ARGUMENT PARSING OR OTHER REAL
   * USES (EXCEPT SYSOUTS/ HELP/ ETC.).
   * See {@link #toRangeString(Iterable)} for argument parsing and others!
   * @param <Type>
   * @param acceptedObjects - a simple list of all acceptable objects.
   * @return String
   */
  @SuppressWarnings({ "unchecked" })
  public static <Type> String toNiceRangeString(Iterable<Type> acceptedObjects) {
    Iterable<Type> accObjects = acceptedObjects;
    Iterator<Type> iterator = acceptedObjects.iterator();
    
    /*
     */
    
    //If the range consists of classes, use the simple class names
    if ((acceptedObjects != null) && iterator.hasNext() && (Class.class.isAssignableFrom(iterator.next().getClass()))) {
      List<Type> classStrings = new LinkedList<Type>();
      for (Type object : acceptedObjects) {
        // Note that this is only for a presentation to a user, not for the "real" RangeString.
        classStrings.add((Type) ((Class<Type>) object).getSimpleName());
      }
      accObjects = classStrings;
    }
    
    String s = '{' + StringUtil.implode(StringUtil.addPrefixAndSuffix(accObjects, "\"", "\""), ",") + '}';
    
    logger.finer(String.format("Created a new range-string from a collection: %s", s));
    return s;
  }
  
  /**
   * Builds a compact range, that accepts integers. This method automatically
   * builds {@link SubRange}s with lower and upper bounds, based on the given
   * values and is thus much more efficient for Integers in terms of space and
   * time than using other methods that build ranges based on {@link Iterable}s.
   * (Other methods add each element of the collection as a separate SubRange).
   * 
   * <p>This method works only for integer classes, i.e., Byte, Short, Integer,
   * Long and BigInteger.
   * @param <Type>
   * @param acceptedNumbers list of all acceptable numbers
   * @return instance of {@link Range}, that exclusively accepts all given
   * {@code acceptedNumbers}.
   */
  @SuppressWarnings("unchecked")
  public static <Type extends Number & Comparable<? super Type>> Range<Type> toIntegerRange(Collection<Type> acceptedNumbers) {
    
    // Convert to list and sort ascending
    List<Type> list;
    if (acceptedNumbers instanceof List) {
      list = (List<Type>) acceptedNumbers;
    } else {
      list = new ArrayList<Type>(acceptedNumbers);
    }
    Collections.sort(list);
    
    // Create result range
    Range<Type> result = new Range<Type>((Class<Type>) acceptedNumbers.iterator().next().getClass());
    
    // Build ranges. We can summarize all elements with a distance of 1. Others
    // Must be added as a separate range, each.
    Type lBound = list.get(0);
    Type uBound = list.get(0);
    for (Type cur: list) {
      if (cur.doubleValue()-lBound.doubleValue()>1) {
        result.addRange(lBound, uBound);
        lBound = cur; uBound = cur;
      }
      uBound = cur;
    }
    result.addRange(lBound, uBound);
    
    return result;
  }
  
  /**
   * Builds a range string that accepts exclusively all
   * elements in the given {@link Enum}.
   * @param <T>
   * @param cazz
   * @return {@link String} that can be used, e.g. in {@link Range#Range(Class, String)}.
   */
  public static <T extends Enum<?>> String toRangeString(Class<T> cazz) {
    return toRangeString(cazz.getEnumConstants());
  }
  
  /**
   * Builds a range string that accepts exclusively all
   * given {@code constants}.
   * @param <T>
   * @param constants
   * @return {@link String} that can be used, e.g. in {@link Range#Range(Class, String)}.
   */
  private static <T> String toRangeString(T... constants) {
    StringBuilder sb = new StringBuilder();
    sb.append('{');
    int i = 0;
    for (T element : constants) {
      if (i > 0) {
        sb.append(',');
      }
      sb.append(element.toString());
      i++;
    }
    sb.append('}');
    return sb.toString();
  }
  
  /**
   * Builds a range string that accepts exclusively all
   * elements in the parent Enum of the given {@link Enum} element.
   * @param <T>
   * @param cazz
   * @return {@link String} that can be used, e.g. in {@link Range#Range(Class, String)}.
   */
  public static <T extends Enum<?>> String toRangeString(Enum<?> cazz) {
    return toRangeString(cazz.getDeclaringClass().getEnumConstants());
  }
  
  /**
   * Returns a range specification for boolean values. This can be used to
   * specifiy that a boolean option expects an argument that is either {@code true} or
   * {@code false}.
   * 
   * @return a range specification for boolean values
   */
  public static Range<Boolean> booleanRange() {
    return new Range<Boolean>(Boolean.class, "{\"true\", \"false\"}");
  }
  
  /* (non-Javadoc)
   * @see java.lang.Object#toString()
   */
  @Override
  public String toString() {
    return getRangeSpecString();
  }
  
  /**
   * @return the minimum acceptable value. Only if {@link #typee} is
   * an instance of {@link Comparable}. Currently, this implementation
   * IGNORES the r.excludingLBound option and thus, might return
   * the first value that is actually out of range!
   */
  @SuppressWarnings({ "unchecked", "rawtypes" })
  public Type getMinimum() {
    // listOfAccpetedObjects should be sorted, if it implements comparable.
    Type min = null;
    if ((listOfAccpetedObjects != null) && (listOfAccpetedObjects.size() > 0)) {
      min = listOfAccpetedObjects.get(0);
    }
    
    if (ranges != null) {
      for (SubRange r : ranges) {
        if (min == null) {
          min = r.getMinimum();
        }
        else if ((min instanceof Comparable<?>) && (r.lBound instanceof Comparable<?>)) {
          min = ((Comparable) min).compareTo(r.lBound) < 0 ? min : r.lBound;
        }
        // Too much effort to process this...
        //if (r.excludingLBound) {}
      }
    }
    return min;
  }
  
  /**
   * @return the maximum acceptable value. Only if {@link #typee} is
   * an instance of {@link Comparable}. Currently, this implementation
   * IGNORES the r.excludinguBound option and thus, might return
   * the first value that is actually out of range!
   */
  @SuppressWarnings({ "unchecked", "rawtypes" })
  public Type getMaximum() {
    // listOfAccpetedObjects should be sorted, if it implements comparable.
    Type max = null;
    if ((listOfAccpetedObjects != null) && (listOfAccpetedObjects.size() > 0)) {
      max = listOfAccpetedObjects.get(listOfAccpetedObjects.size()-1);
    }
    
    if (ranges != null) {
      for (SubRange r : ranges) {
        if (max == null) {
          max = r.getMaximum();
        }
        else if ((max instanceof Comparable<?>) && (r.uBound instanceof Comparable<?>)) {
          max = ((Comparable)max).compareTo(r.uBound) > 0 ? max : r.uBound;
        }
        // Too much effort to process this...
        //if (r.excludinguBound) {}
      }
    }
    return max;
  }
  
  /* (non-Javadoc)
   * @see java.lang.Comparable#compareTo(java.lang.Object)
   */
  @Override
  public int compareTo(Range<Type> other) {
    // Is only approximative...
    if (this == other) {
      return 0;
    }
    if (other == null) {
      return -1;
    }
    int ret = 0;
    
    if (!typee.equals(other.typee)) {
      return -2;
    }
    
    if (constraints == null) {
      if (other.constraints != null) {
        return -3;
      }
    } else if (other.constraints == null) {
      if (constraints != null) {
        return -4;
      }
    } else if (constraints.equals(other.constraints)) {
      return 0;
    }
    
    if ((rangeString != null) && (other.rangeString != null)) {
      ret = rangeString.compareTo(other.rangeString);
      if (ret != 0) {
        return ret;
      }
    }
    
    if ((listOfAccpetedObjects != null) && (other.listOfAccpetedObjects != null)) {
      ret = listOfAccpetedObjects.size()-other.listOfAccpetedObjects.size();
      if (ret != 0) {
        return ret;
      }
    }
    
    return ret;
  }
  
}
