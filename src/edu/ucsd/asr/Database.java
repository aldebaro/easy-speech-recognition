package edu.ucsd.asr;

/**Used only to encapsulate inner class Database.Type.
 *
 * @author Aldebaro Klautau
 * @version 2 - September 24, 2000.
 */

public class Database {

  /**Inner class that represents the type of database.
   */
  public static class Type extends SuperType {

    public static final Type TIMIT = new Type("TIMIT");
    public static final Type TIDIGITS = new Type("TIDIGITS");
    public static final Type WSJ0 = new Type("WSJ0");
    public static final Type WSJ1 = new Type("WSJ1");
    public static final Type GENERAL = new Type("GENERAL");
    public static final Type SPOLTECHRAW = new Type("SPOLTECHRAW");
    public static final Type DECTALK = new Type("DECTALK");
    protected static Type[] m_types;

    //In case of adding a new Type (above), don't forget to add it below.
    static {
      m_types = new Type[7];
      m_types[0] = GENERAL;
      m_types[1] = TIMIT;
      m_types[2] = TIDIGITS;
      m_types[3] = WSJ0;
      m_types[4] = WSJ1;
      m_types[5] = SPOLTECHRAW;
      m_types[6] = DECTALK;
    }

    //notice the constructor is 'protected', not public.
    protected Type(String strName) {
      //m_strName is defined in superclass
      m_strName = strName;
    }

    /**Return true if the input String is equal
     * (case sensitive) to the String that represents
     * one of the defined Type's.
     */
    public static boolean isValid(String typeIdentifierString) {
      for (int i=0; i<m_types.length; i++) {

        //notice I am using case sensitive comparation
        //System.out.println("typeIdentifierString = "+typeIdentifierString);
        //System.out.println("m_types[i].toString() = "+m_types[i].toString());
        if (typeIdentifierString.equals(m_types[i].toString())) {
          return true;
        }
      }
      return false;
    }

    /**Return the index of m_types element that matches
     * the input String or -1 if there was no match.
     */
    private static int getTypeIndex(String typeIdentifierString) {
      for (int i=0; i<m_types.length; i++) {
        //notice I am using case sensitive comparation
        if (typeIdentifierString.equals(m_types[i].toString())) {
          return i;
        }
      }
      return -1;
    }

    /**Return the Type correspondent to the given indentifier
    * String or null in case identifier is not valid.
    */
    //it was not declared in SuperType because I wanted to
    //return this specif Type not a SuperType and didn't know how
    //to do the casting from the superclass... Is that possible ?
    public static final Type getType(String typeIdentifierString) {
      if (isValid(typeIdentifierString)) {
        int nindex = getTypeIndex(typeIdentifierString);
        return m_types[nindex];
      } else {
        return null;
      }
    }

    /**Return the Type correspondent to the given indentifier
    * String or exit in case identifier is not valid.
    */
    //it was not declared in SuperType because I wanted to
    //return this specif Type not a SuperType and didn't know how
    //to do the casting from the superclass... Is that possible ?
    public static final Type getTypeAndExitOnError(String typeIdentifierString) {
      if (isValid(typeIdentifierString)) {
        int nindex = getTypeIndex(typeIdentifierString);
        return m_types[nindex];
      } else {
        End.throwError(typeIdentifierString + " is not a valid database type.");
        //make compiler happy:
        return null;
      }
    }
  }
}
