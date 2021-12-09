package edu.ucsd.tree;

import edu.ucsd.asr.TableOfLabels;
import edu.ucsd.asr.SuperType;
import edu.ucsd.asr.HeaderProperties;
import edu.ucsd.asr.LogDomainCalculator;
import edu.ucsd.asr.FileWithHeaderReader;
import edu.ucsd.asr.FileWithHeaderWriter;
import edu.ucsd.asr.Print;
import edu.ucsd.asr.End;
import edu.ucsd.util.HeapSort;
import edu.ucsd.tree.tokens.Edge;
import edu.ucsd.tree.tokens.Vertex;
import edu.ucsd.tree.tokens.VertexG;
import edu.ucsd.tree.tokens.VertexEW;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.HashSet;
import java.util.StringTokenizer;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.LinkedList;
import java.io.*;
/**
 * Title:        Speech Recognition Software
 * Description:  Language Modelling Tools
 * Copyright:    Copyright (c) 2001
 * Company:      UCSD
 * @author Nikola Jevtic
 * @version 3.01
 */

public class Network {
  Type m_inputClass, m_outputClass;
  Vertex m_root;
  //VertexG[] m_xallVertices; // all vertices
  int m_nvertices;
  //ArrayList m_allEdges; // all edges
  int m_nedges;
  private boolean m_omaximumPushing = false;
  protected boolean m_oisProbabilityInLogDomain = false;
  protected boolean[] m_overtexFlag;//to replace flag in vertices...

  protected Hashtable m_inputToShort; // maps input strings to Short
  protected Hashtable m_outputToShort; // maps output strings to Short

  protected String[] m_shortToInput;
  protected String[] m_shortToOutput;

  //don't need different file extensions, because the structure is uniform...
  public static final String DICTIONARY_FILE_EXTENSION = "PND";
  public static final String GRAMMAR_FILE_EXTENSION = "WND";
  public static final String HASHTABLE_FILE_EXTENSION = "HSH";
  public static final String MERGED_FILE_EXTENSION = "PND";


  public HeaderProperties m_headerProperties;

  public Network( HeaderProperties hp ) {
    m_headerProperties = hp;
    interpretHeaderProperties();
    m_root = Vertex.getGeneralVertex( 0 );
    //m_allVertices = new ArrayList();
    //m_allVertices.add(0,m_root);
    m_nvertices = 1;
    m_nedges = 0;
  }

  /**
   * Loads the network from a file.
   * @param filename Full path name of the file containg the network description.
   * @param ouseEWVertices If set builds the network with roots as VertexEW.
   *                       VertexEW extends the VertexG structure and includes additional fields needed
   *                       by the EWDecoder. Network isin every other respect identical, but the size
   *                       is larger. Recommended to use only for recognition with EWDecoder.
   */
  public Network( String filename, boolean ouseEWVertices ) {
    //first get the header properties
    FileWithHeaderReader fileWithHeaderReader = new FileWithHeaderReader( filename );
    if (fileWithHeaderReader.wasFileOpenedSuccessfully() &&
        fileWithHeaderReader.wasEndOfHeaderIdentifierFound()) {
      m_headerProperties = fileWithHeaderReader.getHeaderProperties();
      //m_headerProperties.list(System.out);
    }
    File file = new File( filename );
    String dir = file.getPath();
    dir = dir.substring(0, dir.lastIndexOf(file.separator)) + file.separator;
    m_headerProperties.setProperty("Network.Dir", dir);
    this.interpretHeaderProperties();
    //load the rest of the file...
    DataInputStream din = fileWithHeaderReader.getDataWithoutHeader();
    try {
      m_nvertices = din.readInt();
      VertexG[] allVertices;
      if (ouseEWVertices) {
        allVertices = new VertexEW[m_nvertices];
      } else {
        allVertices = new VertexG[m_nvertices];
      }
      m_overtexFlag = new boolean[m_nvertices];
      for (int i = 0; i < m_nvertices; i++) {
        if (ouseEWVertices) {
          allVertices[i] = Vertex.getEWVertex(i);
        } else {
          allVertices[i] = Vertex.getGeneralVertex(i);
        }
        m_overtexFlag[i] = false;
      }
      m_nedges = 0;
      int nrootID = din.readInt();
      m_root = allVertices[nrootID];
      m_overtexFlag[nrootID] = true;
      //2 options for loading: until 1 or 1 until 2
      //loadStructure( new Vertex.Context(m_root), din, allVertices );
      //1
      LinkedList list = new LinkedList();
      list.add(new Vertex.Context(m_root));
      while (!list.isEmpty()) {
        loadStructureNonrecursive( (Vertex.Context) list.removeFirst(),
                                  din, list, allVertices);
      }
      //2
    }
    catch (IOException e) {
      e.printStackTrace();
    }
    //rearrangeEdges();
    for (int i = 0; i < m_overtexFlag.length; i++) {
      if (!m_overtexFlag[i])
        Print.dialog("Fatal error dude; not all have been loaded...");
    }
    m_overtexFlag = null;
    countElements();
    Print.dialog(getDescription());
  }


  /*
  * Needs full path that specifies where the file is located.
  *
  public Network( String filename ) {
    //first get the header properties
    FileWithHeaderReader fileWithHeaderReader = new FileWithHeaderReader(
                                                                      filename);
    if ( fileWithHeaderReader.wasFileOpenedSuccessfully() &&
                        fileWithHeaderReader.wasEndOfHeaderIdentifierFound() ) {
      m_headerProperties = fileWithHeaderReader.getHeaderProperties();
      //m_headerProperties.list(System.out);
    }
    File file = new File(filename);
    String dir = file.getPath();
    dir = dir.substring(0,dir.lastIndexOf(file.separator))+file.separator;
    m_headerProperties.setProperty("Network.Dir",dir);
    this.interpretHeaderProperties();
    //load the rest of the file...
    DataInputStream din = fileWithHeaderReader.getDataWithoutHeader();
    try {
      m_nvertices = din.readInt();
      VertexG[] allVertices = new VertexG[m_nvertices];
      m_overtexFlag = new boolean[m_nvertices];
      for( int i=0; i<m_nvertices; i++ ) {
      //for now create general vertices; later add option for ewaves
        allVertices[i] = Vertex.getGeneralVertex(i);
        m_overtexFlag[i] = false;
      }
      m_nedges = 0;
      int nrootID = din.readInt();
      m_root = allVertices[nrootID];
      m_overtexFlag[nrootID] = true;
      //2 options for loading: until 1 or 1 until 2
      //loadStructure( new Vertex.Context(m_root), din, allVertices );
      //1
      LinkedList list = new LinkedList();
      list.add( new Vertex.Context(m_root) );
      while( !list.isEmpty() ) {
        loadStructureNonrecursive( (Vertex.Context) list.removeFirst(),
                                                    din, list, allVertices );
      }
      //2
    } catch (IOException e) {
      e.printStackTrace();
    }
    //rearrangeEdges();
    for( int i=0; i<m_overtexFlag.length; i++ ) {
      if( !m_overtexFlag[i] )
        Print.dialog("Fatal error dude; not all have been loaded...");
    }
    m_overtexFlag = null;
  }
*/

  public String getDescription() {
    String output = "Input Class is " + this.m_inputClass.toString();
    output += "\nOutput Class is " + this.m_outputClass.toString();
    output += "\nThere are " + m_nvertices + " vertices and " +
              m_nedges + " edges.";
    return output;
  }

  public void clear() {
    m_nedges = 0;
    m_nvertices = 0;
    m_root = null;
  }

  public Vertex getNewVertex() {
    Vertex v = Vertex.getGeneralVertex( m_nvertices++ );
    return v;
  }

  public Edge getNewEdge( short input, short output, float edgeProb,
                          Vertex vert ) {
    Edge e = Edge.getEdge( input, output, edgeProb, vert, m_nedges++ );
    return e;
  }

  public Edge getNewEdge( short input, short[] output, float edgeProb,
                          float[] vertProb, Vertex[] vert ) {
    Edge e = Edge.getEdge( input, output, edgeProb, vertProb, vert,
                           m_nedges++ );
    return e;
  }

/*
  private void rearrangeEdges() {
    int size = m_allEdges.size();
    ArrayList al = new ArrayList( size );
    Edge e;
    for( int i=0; i<size; i++ ) {
      e = (Edge) m_allEdges.get(i);
      al.set( e.m_nid, e );
    }
    m_allEdges = al;
  }
*/

  private void countElements() {
    int nvertices = 0;
    int nedges = 0;
    Vertex v,v2;
    Edge e;
    int ne,nv,nid2;
    m_overtexFlag = new boolean[m_nvertices];
    for(int i=0; i<m_nvertices; i++ ) {
      m_overtexFlag[i] = false;
    }
    int nid = m_root.getID();
    m_overtexFlag[nid] = true;
    nvertices++;
    LinkedList list = new LinkedList();
    list.add(m_root);
    while(!list.isEmpty()) {
      v = (Vertex) list.removeFirst();
      ne = v.getNumberOfEdges();
      nedges += ne;
      for( int i=0; i<ne; i++ ) {
        e = v.getEdge(i);
        nv = e.getNumberOfVertices();
        for( int j=0; j<nv; j++ ) {
          v2 = e.getOutputContext(j).m_vertex;
          nid2 = v2.getID();
          if( nid2<0 ) {
            nvertices++;
            list.add(v2);
          } else if( !m_overtexFlag[nid2] ) {
            m_overtexFlag[nid2] = true;
            nvertices++;
            list.add(v2);
          }
        }
      }
    }
    Print.dialog("Number of ID-ed vertices is "+m_nvertices);
    Print.dialog("Total number of vertices is "+nvertices+" and edges is " + nedges+".");
  }

  public void saveNetwork( String filename ) {
    File file = new File( filename );
    String path = file.getPath();
    path = path.substring( 0, path.lastIndexOf(file.separator) ) +
           file.separator;
    //make sure to save hashtables if they exist...
    if( m_inputClass==Type.WORDS &&
        m_headerProperties.containsKey("Network.InputHashtableFileName") ) {
      saveWordHashtable( path +
                         m_headerProperties.getPropertyAndExitIfKeyNotFound(
                                    "Network.InputHashtableFileName" ), true );
    }
    if( m_outputClass==Type.WORDS &&
        m_headerProperties.containsKey( "Network.OutputHashtableFileName" ) ) {
      saveWordHashtable( path +
                         m_headerProperties.getPropertyAndExitIfKeyNotFound(
                                  "Network.OutputHashtableFileName" ), false );
    }
    String property = (m_oisProbabilityInLogDomain)?"true":"false";
    m_headerProperties.setProperty("Network.IsProbabilityInLogDomain",property);
    //get the proper header
    FileWithHeaderWriter fwhw = new FileWithHeaderWriter();
    String header = fwhw.formatHeader( m_headerProperties );

    try {
      FileOutputStream fileOutputStream = new FileOutputStream(file);
      DataOutputStream dout = new DataOutputStream(
                                    new BufferedOutputStream(fileOutputStream));
      dout.writeBytes( header );
      //int nvertices = m_allVertices.size();
      m_overtexFlag = new boolean[m_nvertices];
      for( int i=0; i<m_nvertices; i++ ) {
        m_overtexFlag[i] = false;//not visited
      }
      dout.writeInt( m_nvertices );
      int id = m_root.getID();
      //root must be vertex with id!
      m_overtexFlag[id] = true;
      dout.writeInt( id );
      //switch: until 1 or from 1 to 2
      //saveStructure( m_root, dout );
      //1
      LinkedList list = new LinkedList();
      list.add(m_root);
      while( !list.isEmpty() ) {
        saveStructureNonrecursive( (Vertex) list.removeFirst(), dout, list );
      }
      //2
      dout.close();
    } catch (IOException e) {
      e.printStackTrace();
    }
    //testing if all vertices have been saved:
    for( int i=0; i<m_nvertices; i++ ) {
      if( !m_overtexFlag[i] ) {
        Print.error("Dude, not all have been saved!!!");
        End.exit(0);
      }
    }
    m_overtexFlag = null;
  }

  /**
   * This function returns default token for the class that label belongs to.
   * io specifies whether label belongs to input or output tokens.
   */
  public String convertLabel2EquivalentInTOL( String label, boolean io ) {
    if( io ) {
      return m_shortToInput[((Short)m_inputToShort.get(label)).shortValue()];
    } else {
      return m_shortToOutput[((Short)m_outputToShort.get(label)).shortValue()];
    }
  }

  private void interpretHeaderProperties() {
    //generate input classes
    String property = m_headerProperties.getPropertyAndExitIfKeyNotFound(
                                                        "Network.InputClass");
    if( Type.TIMIT39.toString().equals( property ) ) {//case sensitive
      m_inputClass = Type.TIMIT39;
      TableOfLabels tol = new TableOfLabels( TableOfLabels.Type.TIMIT39 );
      convertTableOfLabels( tol, true );
    } else if( Type.TIMIT48.toString().equals( property ) ) {
      m_inputClass = Type.TIMIT48;
      TableOfLabels tol = new TableOfLabels( TableOfLabels.Type.TIMIT48 );
      convertTableOfLabels( tol, true );
    } else if ( Type.ISIP41.toString().equals( property ) ) {
      m_inputClass = Type.ISIP41;
      TableOfLabels tol = new TableOfLabels( TableOfLabels.Type.ISIP41 );
      convertTableOfLabels( tol, true );
    } else if ( Type.TIDIGITS.toString().equals(property) ) {
      m_inputClass = Type.TIDIGITS;
      TableOfLabels tol = new TableOfLabels( TableOfLabels.Type.TIDIGITS );
      convertTableOfLabels( tol, true );
    } else if( Type.WORDS.toString().equals( property ) ) {
      m_inputClass = Type.WORDS;
      if( m_headerProperties.containsKey("Network.InputHashtableFileName") ) {
        String filename = m_headerProperties.getProperty("Network.Dir","");
        filename += m_headerProperties.getPropertyAndExitIfKeyNotFound(
                                              "Network.InputHashtableFileName");
        Print.dialog("Loading input hashtable from: " + filename );
        loadWordHashtable( filename, true );
      } else {
        Print.dialog("No input hashtable with words specified.");
      }
    } else {
      Print.error("Input class not specified... "+property);
      //End.exit();
    }

    //generate output classes
    property = m_headerProperties.getPropertyAndExitIfKeyNotFound("Network.OutputClass");
    if( Type.TIMIT39.toString().equals( property ) ) {//case sensitive
      m_outputClass = Type.TIMIT39;
      TableOfLabels tol = new TableOfLabels( TableOfLabels.Type.TIMIT39 );
      convertTableOfLabels( tol, false );
    } else if( Type.TIMIT48.toString().equals( property ) ) {
      m_outputClass = Type.TIMIT48;
      TableOfLabels tol = new TableOfLabels( TableOfLabels.Type.TIMIT48 );
      convertTableOfLabels( tol, false );
    } else if( Type.WORDS.toString().equals( property ) ) {
      m_outputClass = Type.WORDS;
      boolean same = (m_headerProperties.getProperty(
                                "Network.InputWordsAndOutputWordsAreSame",
                                "false").equals("true"))?true:false;
      if( m_headerProperties.containsKey(
                                "Network.OutputHashtableFileName") ) {
        //default "" is left so that one does not need to specify separately
        //the name and folder. full path will work... however, must make sure
        //that after loading I have just the filename in this property.
        //this is done in loadWordHashtable
        String filename = m_headerProperties.getProperty("Network.Dir","");
        filename += m_headerProperties.getPropertyAndExitIfKeyNotFound(
                                            "Network.OutputHashtableFileName");
        loadWordHashtable( filename, false );
      } else {
        //initialize hashtable
        m_outputToShort = new Hashtable();
        m_outputToShort.put("<EPS>",new Short((short) 0));
        m_outputToShort.put("<EOS>",new Short((short) 1));
        Print.dialog("output hashtable with words only initialized.");
      }
      //here i set the translators to point to the same thing...
      if( m_inputClass==Type.WORDS && same ) {
        Print.dialog("Setting input to equal output.");
        setInputEqualToOutput();
      }
    } else {
      Print.error("Output class not specified... "+property);
      //End.exit();
    }

    property = m_headerProperties.getProperty("Network.TypeOfProbabilityPushing","SUM");
    if( property.equals("SUM") ) {
      this.m_omaximumPushing = false;
    } else if( property.equals("MAXIMUM") ) {
      this.m_omaximumPushing = true;
    } else {
      this.m_omaximumPushing = true;
      Print.dialog("Not clear what kind of pushing is required... applying MAXIMUM");
    }

    property = m_headerProperties.getProperty("Network.IsProbabilityInLogDomain","false");
    m_oisProbabilityInLogDomain = (property.equals("true"))?true:false;

  }

  public void setInputEqualToOutput() {
    m_inputToShort = m_outputToShort;
    m_shortToInput = m_shortToOutput;
  }

  public void createInputOutputFromWordClass() {
    int n = Word.getNumberOfWords();
    m_outputToShort = new Hashtable( n );
    m_shortToOutput = new String[n];
    Word w;
    for( short i=0; i<n; i++ ) {
      w = Word.getWord(i);
      m_outputToShort.put( w.getName(), new Short(i) );
      m_shortToOutput[i] = w.getName();
    }
    setInputEqualToOutput();
    m_headerProperties.setProperty("Network.InputWordsAndOutputWordsAreSame","true");
  }

  /**
   * This function takes into account the format that network uses for probabilities.
   * If they are in logarithms, values would be multiplied, otherwise they would be
   * exponentiated.
   */
  protected void changeLMBias( float bias ) {
    //mark vertices with unique IDs to make sure we dont loop through network.
    //int n = m_allVertices.size();
    m_overtexFlag = new boolean[m_nvertices];
    for( int i=0; i<m_nvertices; i++ ) {
      m_overtexFlag[i] = false;
    }
    m_overtexFlag[m_root.getID()] = true;
    LinkedList tasks = new LinkedList();
    tasks.add( m_root );
    while( tasks.size() > 0 ) {
      changeLMBias( tasks, bias );
    }
    //freeing some memory...
    m_overtexFlag = null;
  }

  private void changeLMBias( LinkedList list, float bias ) {
    Vertex v = (Vertex) list.removeFirst();
    Edge e;
    int nvert, vID;
    Edge.OutputContext oc;
    int nedges = v.getNumberOfEdges();
    for( int i=0; i<nedges; i++ ) {
      e = v.getEdge( i );
      //first scale the curent edge
      e.scaleProbabilities( bias, m_oisProbabilityInLogDomain );
      //now must check if the children vertices need to be processed...
      nvert = e.getNumberOfVertices();
      for( int j=0; j<nvert; j++ ) {
        oc = e.getOutputContext( j );
        vID = oc.m_vertex.getID();
        if( vID < 0 ) {//this means internal node
          list.add( oc.m_vertex );
        } else {//must check if it was visited before...
          if( !m_overtexFlag[vID] ) {
            m_overtexFlag[vID] = true;
            list.add( oc.m_vertex );
          }
        }
      }
    }
  }

  /**
   * Assumes the dictionary contains counts or absolute probabilities; this
   * should become the sub-dictionary in compressed format. only root should
   * have id.
   */
  public void truncateDictionary( Network bigDictionary, HashSet targetWords ) {
    String word;
    Edge dummy = Edge.getEdge( (short) 0, (short) 0, 0.0f, null, -1 );
    Hashtable words = new Hashtable( targetWords.size() );
    //first make sure we have all the words in output dictionary
    Iterator it = targetWords.iterator();
    while( it.hasNext() ) {
      word = (String) it.next();
      //dummy edge is just to offer 0.0f as a log probability modifier in dfs
      words.put( word, new Vertex.DestinationInfo( dummy ));
      //make sure we have all the words in the dictionary
      if( !m_outputToShort.containsKey( word ) ) {
        short index = (short) m_outputToShort.size();
        m_outputToShort.put( word, new Short(index) );
      }
    }
    updateTableFromIOHashtable( false );
    Vertex.Mark mark = new Vertex.Mark( bigDictionary.m_root );
    dfs( words, bigDictionary.m_root, mark, bigDictionary );
    //here, for no particular reason, I would like to keep the counts.
    formSubD( words, new Vertex.Context( m_root ), bigDictionary.m_root, mark,
        bigDictionary );
  }

  private void formSubD( Hashtable words, Vertex.Context currentV, Vertex oldV,
                         Vertex.Mark mark, Network bigD ) {
    Edge[] edges = new Edge[mark.count0];
    int nvert,n;
    int nedge=0;
    short in;
    short[] out;
    float fin;
    float[] fout;
    Vertex[] nodes;
    Edge oldE;
    Edge.OutputContext oc;
    String output;
    int nold = oldV.getNumberOfEdges();
    for( int i=0; i<nold; i++ ) if( mark.count[i] > 0 ) {
      oldE = oldV.getEdge(i);
      n = mark.count[i];
      in = oldE.m_input;//they have the same input table of labels
      out = new short[n];
      fin = 0.0f;
      fout = new float[n];
      nodes = new Vertex[n];
      nvert = 0;
      int noldOut = oldE.getNumberOfVertices();
      for( int j=0; j<noldOut; j++ ) if( mark.m_oflag[i][j] ) {
        oc = oldE.getOutputContext( j );
        output = bigD.getOutput( oc.m_output );
        if( output==" " ) {//internal node
          out[nvert] = (short) -1;
          formSubD( words, new Vertex.Context( nodes, nvert ), oc.m_vertex,
                    mark.m_extension[i][j], bigD );
          //previous function will set potential on this extension
          fout[nvert] = mark.m_extension[i][j].potential;
          fin += fout[nvert];
        } else {//must be something from words...
          out[nvert] = getOutput( output );
          fout[nvert] = oc.m_fscore;
          if( noldOut == 1 ) {
            fin = oldE.m_fedgeProbability;
          } else if( n == 1 ) {
            fin = oc.m_fscore;
          }
          nodes[nvert] = m_root;//this is RecurrentNetwork
        }
        nvert++;//next vertex
      }
      //edge is sortof ready...
      edges[nedge] = getNewEdge( in, out, fin, fout, nodes );
    }
    if( currentV.isVertex() ) {
      ((VertexG) currentV.getVertex()).setEdges( edges );
    } else {
      currentV.createVertex( edges );
    }
  }

  public static Network merge( RecurrentNetwork inputNet, Network outputNet,
                               String LMlookAhead ) {

    Print.dialog("OutputNet size="+outputNet.m_nvertices);
    if( !inputNet.m_oisProbabilityInLogDomain )  {
      Print.error("Input net should contain log probs.");
      End.exit(0);
    }
    if( !outputNet.m_oisProbabilityInLogDomain ) {
      Print.dialog("Output net should contain log probabilities. Switching now...");
      outputNet.changeToLogDomain();
    }
    HeaderProperties hp = new HeaderProperties();
    //this is not good. I am just saying this because need something...
    hp.setProperty("Network.InputClass","WORDS");
    hp.setProperty("Network.OutputClass","WORDS");
    hp.setProperty("Network.TypeOfProbabilityPushing",LMlookAhead);

    Network merged = new Network( hp );

    //check for compatibility
    if( inputNet.m_outputClass != outputNet.m_inputClass ) {
      Print.error("Output class of the first transducer different from the "+
                        "input class of the second transducer");
      End.exit();
    }
    //even if they claim compatibility some problems are possible; e.g. words
    //if collected from different source sets may be different. no problem if
    //vocabulary is larger; trouble if something is missing. will report prblms.

    merged.m_inputToShort = inputNet.m_inputToShort;
    merged.m_shortToInput = inputNet.m_shortToInput;
    merged.m_inputClass = inputNet.m_inputClass;
    if( merged.m_inputClass==Type.WORDS ) {
      String fn = inputNet.m_headerProperties.getPropertyAndExitIfKeyNotFound(
                                             "Network.InputHashtableFileName" );
      merged.m_headerProperties.setProperty( "Network.InputHashtableFileName",
                                             fn );
    }
    merged.m_headerProperties.setProperty( "Network.InputClass",
                                           merged.m_inputClass.toString() );
    merged.m_outputToShort = outputNet.m_outputToShort;
    merged.m_shortToOutput = outputNet.m_shortToOutput;
    merged.m_outputClass = outputNet.m_outputClass;
    if( merged.m_outputClass==Type.WORDS ) {
      String fn = outputNet.m_headerProperties.getPropertyAndExitIfKeyNotFound(
                                            "Network.OutputHashtableFileName" );
      merged.m_headerProperties.setProperty( "Network.OutputHashtableFileName",
                                             fn );
    }
    merged.m_headerProperties.setProperty( "Network.OutputClass",
                                           merged.m_outputClass.toString() );
    merged.m_oisProbabilityInLogDomain = outputNet.m_oisProbabilityInLogDomain;

    //easy stuff done. now to real work:
    int size = outputNet.m_nvertices;
    //equivalence table;
    //shows which contexts in outputNet and newNet are equivalent
    Vertex[] equiv = new Vertex[size];
    merged.m_overtexFlag = new boolean[size];
    for( int i=0; i<size; i++ ) {
      merged.m_overtexFlag[i] = false;
      equiv[i] = null;
    }

    //this "todo" list contains vertices from outNet yet to be processed
    LinkedList list = new LinkedList();
    //this list will include internal nodes that need be expanded too.
    LinkedList intList;
    //as we build newNet we will input info on which of these are equivalent
    equiv[outputNet.m_root.getID()] = merged.m_root;
    //means it has been inserted in (todo) list
    merged.m_overtexFlag[outputNet.m_root.getID()] = true;
    list.add( outputNet.m_root );

    Vertex oldvert, newvert;
    //hashtable should map inputs into: 1.total probability,
    //                                  2.edge showing everything else
    Hashtable words;
    //we build network as long as there are nodes yet to be expanded in list
    while( !list.isEmpty() ) {
      oldvert = (Vertex) list.removeFirst();
      newvert = equiv[oldvert.getID()];
      if( newvert == null ) {
        Print.error("Equivalent vertex should have been initialized...");
        End.exit(0);
      }
      merged.m_nedges = 0;//for every tree edges may start from 0.
      /**@todo: Must rewrite this procedure to run in a single parse, without generation
       * of the auxiliary Vertex.Mark[][] structures. It was needed while dictionary could
       * have had any numbers, like frequencies, counts and such, but if it has probs,
       * 1 pass depth first search os doable.*/
      //first check for <BACKOFF>; it must be first edge if there...
      Edge backoff;
      words = new Hashtable();
      Edge e0 = oldvert.getEdge(0);
      String key = outputNet.getInput( e0.m_input );
      Vertex.DestinationInfo destInfo;
      if( key.equalsIgnoreCase( "<BACKOFF>" ) ) {
        Vertex olddest = e0.getOutputContext(0).m_vertex;
        int olddestID = olddest.getID();
        Vertex newdest;
        if( merged.m_overtexFlag[olddestID] ) {
          if( equiv[olddestID]==null ) {
            Print.error("This is not possible...");
            End.exit();
          }
          newdest = equiv[olddestID];
        } else {
          newdest = merged.getNewVertex();
          equiv[olddestID] = newdest;
          merged.m_overtexFlag[olddestID] = true;
          list.add( olddest );
        }
        //probability in right format as merged is same as output
        float pr = e0.m_fedgeProbability;
        backoff = merged.getNewEdge( (short) -1, e0.m_input, pr, newdest );
      } else {
        destInfo = new Vertex.DestinationInfo( e0 );
        words.put( key, destInfo );
        backoff = null;
      }
      //we add all possible destinations to hashtable
      int nedges = oldvert.getNumberOfEdges();
      for( int i=1; i<nedges; i++ ) {
        e0 = oldvert.getEdge( i );
        key = outputNet.getInput(e0.m_input);
        destInfo = new Vertex.DestinationInfo( e0 );
        words.put( key, destInfo );
      }

      Vertex.Mark mark = new Vertex.Mark( inputNet.m_root );
      merged.dfs( words, inputNet.m_root, mark, inputNet );
      intList = new LinkedList();
      //Context contains the information about the new vertex
      //(where it is or where it should be if can not be created yet).
      Vertex.Context vc = new Vertex.Context( newvert );//where it is case
      Vertex.ContextMark vcm = new Vertex.ContextMark( vc, mark,
                                                       inputNet.m_root );
      //Vertex.ContextMark vcm
      intList.add( vcm );
      //now we create a new network and connect to the boundaries
      while( !intList.isEmpty() ) {
        vcm = (Vertex.ContextMark) intList.removeFirst();
        merged.connect( words, vcm.m_newVertexContext, vcm.m_oldVertex,
                        vcm.m_mark, list, intList, inputNet, equiv, backoff );
        //only the first vertex may have the backoff previously determined
        //every following vertex is internal node.
        backoff = null;
      }
      //newvert, mark... ccc can not pust its potential further so:
      for( int i=0; i<newvert.getNumberOfEdges(); i++ ) {
        e0 = newvert.getEdge(i);
        e0.m_fedgeProbability += mark.potential;
      }
    }
    Print.dialog("Merge complete!");

    //here we can test the merged network probabilities
    Print.dialog("Testing probabilities...");
    for( int i=0; i<size; i++ ) {
      merged.m_overtexFlag[i] = false;
    }
    list.clear();
    list.add(outputNet.m_root);
    merged.m_overtexFlag[outputNet.m_root.getID()] = true;
    while( !list.isEmpty() ) {
      oldvert = (Vertex) list.removeFirst();
      newvert = equiv[oldvert.getID()];
      //must build hashtable
      words = new Hashtable();
      Edge e0 = oldvert.getEdge(0);
      String key = outputNet.getInput( e0.m_input );
      Edge.OutputContext oc;
      Vertex olddest;
      int olddestID;
      //check first edge for backoff...
      if( key.equalsIgnoreCase( "<BACKOFF>" ) ) {
        //single output context for outputNet(words->words)
        olddest = e0.getOutputContext(0).m_vertex;
        olddestID = olddest.getID();
        if( !merged.m_overtexFlag[olddestID] ) {
          merged.m_overtexFlag[olddestID] = true;
          list.add( olddest );
        }
      } else {
        //single output context for outputNet(words->words)
        oc = e0.getOutputContext(0);
        olddest = oc.m_vertex;
        olddestID = olddest.getID();
        if( !merged.m_overtexFlag[olddestID] ) {
          merged.m_overtexFlag[olddestID] = true;
          list.addLast( olddest );
        }
        float[] scores = new float[2];
        scores[0] = LogDomainCalculator.m_fLOG_DOMAIN_ZERO;
        scores[1] = e0.m_fedgeProbability;//oc contains nothing
        key = merged.m_shortToOutput[oc.m_output];
        words.put( key, scores );
      }
      int nedges = oldvert.getNumberOfEdges();
      for( int k=1; k<nedges; k++ ) {
        e0 = oldvert.getEdge( k );
        //single output context for outputNet(words->words)
        oc = e0.getOutputContext(0);
        olddest = oc.m_vertex;
        olddestID = olddest.getID();
        if( !merged.m_overtexFlag[olddestID] ) {
          merged.m_overtexFlag[olddestID] = true;
          list.addLast( olddest );
        }
        float[] scores = new float[2];
        scores[0] = LogDomainCalculator.m_fLOG_DOMAIN_ZERO;
        scores[1] = e0.m_fedgeProbability;//oc contains nothing
        key = merged.m_shortToOutput[oc.m_output];
        if( words.containsKey(key) ) {
          Print.error("Should not have repetitions of the same word within context");
          End.exit(0);
        }
        words.put( key, scores );
      }
      //hashtable built. now we traverse the subtree inside merged
      merged.testMergedContext( newvert, words, 0.0f );
      //now must check if all probabilities are close to the original values:
      Enumeration e = words.keys();
      while( e.hasMoreElements() ) {
        key = (String) e.nextElement();
        float[] scores = (float[]) words.get(key);
        if( Math.abs(scores[0]-scores[1])>1e-5 ) {
          Print.error("Wrong scores for word: "+key+"\t"+scores[0]+"\t"+scores[1]);
          End.exit(0);
        }
      }
    }
    //checking my recursion...
    for( int i=0; i<size; i++ ) {
      if( !merged.m_overtexFlag[i] ) {
        Print.error("Did not check all vertices!!!");
        End.exit(0);
      }
    }
    Print.dialog("Checked all probabilities, and all the potentials match the LM");

    merged.m_overtexFlag = null;
    return merged;
  }

  private void testMergedContext( Vertex current, Hashtable words,
                                  float fscore ) {
    Edge e;
    int nvert;
    Edge.OutputContext oc;
    float ftemp;
    int nedges = current.getNumberOfEdges();
    for( int i=0; i<nedges; i++ ) {
      e = current.getEdge(i);
      ftemp = fscore + e.m_fedgeProbability;
      nvert = e.getNumberOfVertices();
      for( int j=0; j<nvert; j++ ) {
        oc = e.getOutputContext(j);
        if( oc.m_output<0 ) {//internal context
          testMergedContext( oc.m_vertex, words, ftemp+oc.m_fscore );
        } else if( oc.m_output>0 ) {
          String key = m_shortToOutput[oc.m_output];
          if( !words.containsKey(key) ) {
            End.throwError("Hashtable words does not contain key: "+key);
          }
          float[] scores = (float[]) words.get(key);
          scores[0] = LogDomainCalculator.add( scores[0], ftemp+oc.m_fscore );
        }
      }
    }
  }

  /**
   * This function should proceed in the breadth first fashion to create edges
   * in a fashionable order.
   */
  private void connect( Hashtable words, Vertex.Context newVert, Vertex oldVert,
                        Vertex.Mark m, LinkedList todo, LinkedList intList,
                        Network inputNet, Vertex[] equiv, Edge backoff ) {
    String output;
    Edge oldEdge;
    Edge[] edges;
    Edge.OutputContext oc,ocLM;
    Vertex.Context vc;
    Vertex.ContextMark vcm;
    int nedge;
    int nvert;
    int noldVert;
    int n;
    Vertex.DestinationInfo destIn;
    if( backoff==null ) {
      edges = new Edge[m.count0];
      nedge = 0;
    } else {
      edges = new Edge[m.count0+1];
      edges[0] = backoff;
      nedge = 1;
    }
    //we go through all the edges and check all outputs
    for( int i=0; i<m.m_extension.length; i++ ) if( m.count[i]>0 ) {
      oldEdge = oldVert.getEdge(i);
      //now we create this edge
      if( oldEdge.getNumberOfVertices() == 1 ) {
        //must check if it is final edge
        oc = oldEdge.getOutputContext( 0 );
        if( oc.m_output < 0 ) {//internal vertex
          edges[nedge] = getNewEdge( oldEdge.m_input, (short) -1,
                                     oldEdge.m_fedgeProbability, null );
          //now must insert a new task in intList
          vc = new Vertex.Context( edges[nedge], 0 );
          vcm = new Vertex.ContextMark( vc, m.m_extension[i][0], oc.m_vertex );
          intList.addLast( vcm );
        } else {//final vertex, probabilities not fixed...
          output = inputNet.getOutput( oc.m_output );
          destIn = (Vertex.DestinationInfo) words.get(output);
          //assume there is only one destination
          ocLM = destIn.outNetEdge.getOutputContext(0);
          //now need to check if the appropriate vertex exists...
          //if it has been visited, the equiv was created...
          int ocLMID = ocLM.m_vertex.getID();
          if( m_overtexFlag[ocLMID] ) {
            if( equiv[ocLMID]==null ) {
              Print.error("This is not possible...");
              End.exit();
            }
          } else { //needed new node
            equiv[ocLMID] = getNewVertex();
            //and another thing is to add a little something to list
            m_overtexFlag[ocLMID] = true;
            todo.addLast( ocLM.m_vertex );
          }
          //setting the potential
          float fprob = m.m_extension[i][0].potential - m.potential;
          edges[nedge] = getNewEdge( oldEdge.m_input, ocLM.m_output, fprob,
                                     equiv[ocLMID] );
        }
      } else {//more than one output vertex in original net
        n = m.count[i];
        short in = oldEdge.m_input;
        short[] out = new short[n];
        //setting the probability
        float fin = oldEdge.m_fedgeProbability;
        float[] fout = new float[n];
        Vertex[] nodes = new Vertex[n];
        nvert = 0;
        noldVert = oldEdge.getNumberOfVertices();
        for( int j=0; j<noldVert; j++ ) if( m.m_oflag[i][j] ) {
          oc = oldEdge.getOutputContext( j );
          if( oc.m_output < 0 ) {
            //internal node
            out[nvert] = (short) -1;
            fout[nvert] = oc.m_fscore;
            //nodes[nvert] = null;
            //still dont have an edge to append to intList
          } else {
            //output node
            output = inputNet.getOutput( oc.m_output );
            destIn = (Vertex.DestinationInfo) words.get(output);
            //assume there is only one destination
            ocLM = destIn.outNetEdge.getOutputContext(0);
            out[nvert] = ocLM.m_output;
            //the probability stored in oldEdge is not the right value
            fout[nvert] = m.m_extension[i][j].potential - m.potential - fin;
            //now need to check if the appropriate vertex exists...
            //if it has been visited, the equiv was created...
            int ocLMID = ocLM.m_vertex.getID();
            if( m_overtexFlag[ocLMID] ) {
              if( equiv[ocLMID]==null ) {
                Print.error("This is not possible...");
                End.exit();
              }
              nodes[nvert] = equiv[ocLMID];
            } else { //needed new node
              nodes[nvert] = getNewVertex();
              //and another thing is to add a little something to list
              m_overtexFlag[ocLMID] = true;
              equiv[ocLMID] = nodes[nvert];
              todo.addLast( ocLM.m_vertex );
            }
          }
          nvert++;
        }
        //now create new edge:
        edges[nedge] = getNewEdge( in, out, fin, fout, nodes );
        nvert = 0;
        for( int j=0; j<noldVert; j++ ) if( m.m_oflag[i][j] ) {
          oc = oldEdge.getOutputContext( j );
          if( oc.m_output < 0 ) {
            //now we can append internal nodes to intList
            vc = new Vertex.Context( edges[nedge], nvert );
            vcm = new Vertex.ContextMark( vc, m.m_extension[i][j], oc.m_vertex );
            intList.addLast( vcm );
          }
          nvert++;
        }
      }
      nedge++;
    }
    if( newVert.isVertex() ) {
      ((VertexG) newVert.getVertex()).setEdges( edges );
    } else {
      newVert.createVertex( edges );
    }
  }

  /**
   * traverses entire tree looking for proper subtree; on top of that computes
   * probabilities for the subtree. Conveniently uses the fields of the
   * dictionary as they are not used, except for the final field. It will be
   * a little awkward... Must have log-probabilities in both input and output
   * networks...
   */
  private void dfs( Hashtable words, Vertex v, Vertex.Mark m,
                    Network inputNet ) {
    String output;
    Edge e;
    Edge.OutputContext oc;
    Vertex.DestinationInfo di;
    float fscore;
    int nout;
    int nedges = v.getNumberOfEdges();
    float[] fpartial = new float[nedges];
    for(int i=0; i<nedges; i++) {
      fpartial[i] = LogDomainCalculator.m_fLOG_DOMAIN_ZERO;
      e = v.getEdge(i);
      nout = e.getNumberOfVertices();
      for( int j=0; j<nout; j++ ) {
        oc = e.getOutputContext( j );
        if( oc.m_output < 0 ) {//internal node...
          Vertex.Mark r  = new Vertex.Mark( oc.m_vertex );
          m.m_extension[i][j] = r;
          dfs( words, oc.m_vertex, r, inputNet );
          //dfs has also established the potential at the mark...
          if( r.count0 > 0 ) {
            m.m_oflag[i][j] = true;
          } else {
            m.m_oflag[i][j] = false;
          }
        } else {
          output = inputNet.getOutput( oc.m_output );
          if( words.containsKey(output) ) {
            //added after changing the edges...
            di = (Vertex.DestinationInfo) words.get( output );
            //single output assumed in the output net:
            fscore = di.outNetEdge.m_fedgeProbability;
            if( nout==1 ) {
              //for single outputs, oc returns 0.0f by default; log score assumed
              fscore += e.m_fedgeProbability;
            } else {
              fscore += oc.m_fscore;
            }
            //no need to initialize with oc.m_vertex! just potential is enough
            m.m_extension[i][j] = new Vertex.Mark( fscore );
            m.m_oflag[i][j] = true;
          } else {
            m.m_oflag[i][j] = false;
          }
        }
        if( m.m_oflag[i][j] ) {
          m.count[i]++;
          //add here partial potentials for this edge...
          if( this.m_omaximumPushing ) {
            if( m.m_extension[i][j].potential > fpartial[i] ) {
              fpartial[i] = m.m_extension[i][j].potential;
            }
          } else {
            fpartial[i] = LogDomainCalculator.add(
                                  fpartial[i], m.m_extension[i][j].potential );
          }
        }
      }
      if( m.count[i] > 0 ) {
        m.count0++;
        //adding potential to this vertex...
        if( this.m_omaximumPushing ) {
          if( fpartial[i] > m.potential ) {
            m.potential = fpartial[i];
          }
        } else {
          m.potential = LogDomainCalculator.add( m.potential, fpartial[i] );
        }
      }
    }
    //now we can set the probabilities in the old network:
    for( int i=0; i<nedges; i++) if( m.count[i] > 0 ) {
      e = v.getEdge(i);
      nout = e.getNumberOfVertices();
      if( nout>1 ) {
        e.m_fedgeProbability = fpartial[i] - m.potential;
        for( int j=0; j<nout; j++ ) if( m.m_oflag[i][j] ) {
          oc = e.getOutputContext( j );
          if( oc.m_output<0 ) {
            e.setVertexProbability(j,m.m_extension[i][j].potential-fpartial[i]);
          }//else it holds output score
        }
      } else {//edgeProb may hold output score
        oc = e.getOutputContext( 0 );
        if( oc.m_output < 0 ) {
          e.m_fedgeProbability = fpartial[i] - m.potential;
        }
      }
    }
  }

  public void insertWord( String input, String word ) {
    StringTokenizer strtok = new StringTokenizer( input );
    short[] in = new short[strtok.countTokens()];
    short[] out = new short[in.length];
    String tok;
    for( int i=0; i<in.length; i++ ) {
      out[i] = (short) -1;//-1 is like no output
      tok = strtok.nextToken();
      if( m_inputToShort.containsKey(tok) ) {
        in[i] = ((Short)m_inputToShort.get(tok)).shortValue();
      } else {
        Print.error("Unknown phone: "+tok);
        End.exit(0);
      }
    }
    //setting the output word
    if( m_outputToShort.containsKey( word ) ) {
      out[in.length-1] = ((Short)m_outputToShort.get(word)).shortValue();
    } else {
      short index = (short) m_outputToShort.size();
      out[in.length-1] = index;
      m_outputToShort.put( word, new Short(index) );
    }
    insertSequence( in, out, m_root, 0 );
  }

  public void insertWord( String[] input, String word ) {
    //for( int i=0; i<input.length; i++ )
    //  Print.dialog(input[i]);
    short[] in = new short[input.length];
    short[] out = new short[input.length];
    for( int i=0; i<in.length; i++ ) {
      out[i] = (short) -1;//-1 is like no output
      if( m_inputToShort.containsKey(input[i]) ) {
        in[i] = ( (Short) m_inputToShort.get(input[i])).shortValue();
      } else {
        Print.error("Unknown phone: "+input[i]);
        End.exit(0);
      }
    }
    //setting the output word
    if( m_outputToShort.containsKey( word ) ) {
      out[in.length-1] = ((Short)m_outputToShort.get(word)).shortValue();
    } else {
      short index = (short) m_outputToShort.size();
      out[in.length-1] = index;
      m_outputToShort.put( word, new Short(index) );
    }
    insertSequence( in, out, m_root, 0 );
  }

  public void insertSentence( String input ) {
    StringTokenizer strtok = new StringTokenizer( input );
    short[] in = new short[strtok.countTokens()];
    String tok;
    for( int i=0; i<in.length; i++ ) {
      tok = strtok.nextToken();
      if( m_inputToShort.containsKey( tok ) ) {
        in[i] = ((Short) m_inputToShort.get( tok )).shortValue();
      } else {
        short index = (short) m_inputToShort.size();
        in[i] = index;
        m_inputToShort.put( tok, new Short(index) );
      }
    }
    //now the sequence is 'translated'
    insertSequence( in, in, m_root, 0 );
  }

  /**
   * Converts counts to absolute representation of probabilities.
   * Before running any kind of recognition needs to be converted to logarithms.
   */
  protected void uniformDistribution() {
    m_overtexFlag = new boolean[m_nvertices];
    for( int i=0; i<m_nvertices; i++ ) {
      m_overtexFlag[i] = false;
    }
    m_overtexFlag[m_root.getID()] = true;
    applyUniform( m_root );
    m_overtexFlag = null;
  }

  private void applyUniform( Vertex v ) {
    float total = 0.0f;
    float score;
    Edge e;
    Edge.OutputContext oc;
    int nvert;
    Vertex v2;
    int nedges = v.getNumberOfEdges();
    for( int i=0; i<nedges; i++ ) {
      e = v.getEdge(i);
      total = add( total, e.m_fedgeProbability );
      nvert = e.getNumberOfVertices();
      for( int j=0; j<nvert; j++ ) {
        oc = e.getOutputContext( j );
        score = div( oc.m_fscore, e.m_fedgeProbability );
        e.setVertexProbability( j, score );
        v2 = oc.m_vertex;
        if( !m_overtexFlag[v2.getID()] && (v2.getNumberOfEdges() != 0) ) {
          m_overtexFlag[v2.getID()] = true;
          applyUniform( v2 );
        }
      }
    }
    for( int i=0; i<nedges; i++ ) {
      e = v.getEdge(i);
      e.m_fedgeProbability = div( e.m_fedgeProbability, total );
    }
  }

  /**
   * here for now assume Networks have the same I/O classes. Just for debugging
   * purposes.
   */
  public boolean isEquivalent( Network net ) {
    m_overtexFlag = new boolean[m_nvertices];
    for( int i=0; i<m_nvertices; i++ ) {
      m_overtexFlag[i] = false;
    }
    m_overtexFlag[m_root.getID()] = true;
    boolean result = checkEquivalence( m_root, net.m_root );
    m_overtexFlag = null;
    return result;
  }

  private boolean checkEquivalence( Vertex v, Vertex w ) {
    int nedges = v.getNumberOfEdges();
    //check if the networks are sorted
    if( !HeapSort.isSorted( v.getEdges() ) ) {
      Print.error("Unsorted context in primary network!");
    }
    if( !HeapSort.isSorted( w.getEdges() ) ) {
      Print.error("Unsorted context in secondary network!");
    }
    //first pass to see if edges agree:
    if( nedges!=w.getNumberOfEdges() ) {
      Print.error("Number of outgoing edges different!!!");
      return false;
    }
    for( int i=0; i<nedges; i++ ) {
      Edge ev = v.getEdge(i);
      Edge ew = w.getEdge(i);
      //check input
      if( ev.m_input!=ew.m_input ) {
        Print.error("Edge input not matching...");
        return false;
      }
      //check input prob
      if( ev.m_fedgeProbability != ew.m_fedgeProbability ) {
        float diff = ev.m_fedgeProbability - ew.m_fedgeProbability;
        Print.error("Edge probability does not match... " +
                    ev.m_fedgeProbability + "\t" + ew.m_fedgeProbability );
        return false;
      }
      int nvert = ev.getNumberOfVertices();
      if( nvert!=ew.getNumberOfVertices() ) {
        Print.error("Number of outgoing vertices does not match...");
        return false;
      }
      for( int j=0; j<nvert; j++ ) {
        Edge.OutputContext ocv = ev.getOutputContext( j );
        Edge.OutputContext ocw;
        boolean found = false;
        for( int k=0; k<nvert; k++ ) {
          ocw = ew.getOutputContext( k );
          if( ocw.m_output == ocv.m_output ) {
            if( !found ) {
              found = true;
            } else {
              Print.error("Repeated output in secondary network!");
            }
            //check probability
            if( ocw.m_fscore != ocv.m_fscore ) {
              Print.error("Non matching output probabilities...");
              return false;
            }
            //check the next context:
            if( !m_overtexFlag[ocv.m_vertex.getID()] ) {
              m_overtexFlag[ocv.m_vertex.getID()] = true;
              if( !checkEquivalence( ocv.m_vertex, ocw.m_vertex ) ) {
                return false;
              }
            }
          }
        }
        if( !found ) {
          Print.error("Output not found!");
          return false;
        }
      }
    }
    //otherwise return true
    return true;
  }


  /**
   * Converts counts in the class into the log-representation of no-weihts
   */
  protected void noDistribution() {
    m_overtexFlag = new boolean[m_nvertices];
    for( int i=0; i<m_nvertices; i++ ) {
      m_overtexFlag[i] = false;
    }
    m_overtexFlag[m_root.getID()] = true;
    applyNoDistribution( m_root );
    m_overtexFlag = null;
  }

  private void applyNoDistribution( Vertex v ) {
    Edge e;
    Vertex v2;
    int nedges = v.getNumberOfEdges();
    for( int i=0; i<nedges; i++ ) {
      e = v.getEdge(i);
      e.m_fedgeProbability = 0.0f;
      int nvert = e.getNumberOfVertices();
      for( int j=0; j<nvert; j++ ) {
        e.setVertexProbability( j, 0.0f );
        v2 = e.getOutputContext(j).m_vertex;
        if( !m_overtexFlag[v2.getID()] && (v2.getNumberOfEdges() != 0) ) {
          m_overtexFlag[v2.getID()] = true;
          applyNoDistribution( v2 );
        }
      }
    }
  }

  public static Network createDigitNetwork( String[] words, boolean[] term ) {
    //initializing network
    HeaderProperties hp = new HeaderProperties();
    hp.setProperty("Network.InputClass","WORDS");
    hp.setProperty("Network.OutputClass","WORDS");
    Network net = new Network( hp );
    Print.dialog("Network initialized");
    //updating vocabulary
    short ID;
    for( int i=0; i<words.length; i++ ) {
      if( !net.m_outputToShort.containsKey(words[i]) ) {
        ID = (short) net.m_outputToShort.size();
        net.m_outputToShort.put( words[i], new Short( ID ));
      } else {
        Print.error("repeated word: "+words[i]);
        End.exit();
      }
    }
    net.updateTableFromIOHashtable(false);
    net.m_headerProperties.setProperty(
                "Network.InputWordsAndOutputWordsAreSame", "true" );
    net.setInputEqualToOutput();
    //now creating network; if term[0], then LM is unlimited sequence of
    //words in any order
    short w;
    if( term[0] ) {
      int nedges = words.length;
      Edge[] edges = new Edge[nedges];
      Edge e;
      for( int i=0; i<words.length; ) {
        w = net.getOutput(words[i]);
        e = net.getNewEdge( w, w, 1.0f, net.m_root );
        edges[i++] = e;
      }
      ((VertexG) net.m_root).setEdges( edges );
      w = net.getOutput("<EOS>");
      e = net.getNewEdge( w, w, 1.0f, net.m_root );
      //this inserts it in the sorted order; this was easy modification
      net.m_root.addEdge( e );
    } else {
      Vertex[] nodes = new Vertex[term.length];
      nodes[0] = net.m_root;
      Edge e;
      Edge[] edges;
      int vocabSize = words.length;
      int nedges;
      int edgeSize=0;
      for( int j=1; j<term.length; j++ ) {
        nedges = vocabSize;
        //if( term[j-1] ) nedges++;
        edges = new Edge[nedges];
        nodes[j] = net.getNewVertex();
        for( int i=0; i<words.length; i++ ) {
          w = net.getOutput(words[i]);
          e = net.getNewEdge( w, w, 1.0f, nodes[j] );
          edges[i] = e;
        }
        ((VertexG) nodes[j-1]).setEdges( edges );
        if( term[j-1] ) {
          w = net.getOutput("<EOS>");
          e = net.getNewEdge( w, w, 1.0f, nodes[j] );
          //again add edge in the sorted order
          nodes[j-1].addEdge( e );
        }
      }
      edges = new Edge[1];
      w = net.getOutput("<EOS>");
      e = net.getNewEdge( w, w, 1.0f, net.m_root );
      edges[0] = e;
      ((VertexG) nodes[nodes.length-1]).setEdges( edges );
    }
    return net;
  }

  //must insert a path corresponding to a sequence of inputs/outputs
  private void insertSequence( short[] in, short[] out, Vertex current,
                               int step ) {
    boolean flag = false;
    int edgeIndex = 0;
    int nvert;
    short input;
    Edge.OutputContext oc;
    Vertex old;
    //this either finds existing edge (flag=true, edgeIndex) or returns the
    //appropriate insert position (flag=false, edgeIndex);
    //implementation not very efficient; done only once in the net generation
    int nedges = current.getNumberOfEdges();
    if( nedges != 0 ) { //else flag=false
      for( edgeIndex=0; edgeIndex<nedges; edgeIndex++ ) {
        input = current.getEdge(edgeIndex).m_input;
        if( in[step] == input )  {
          flag = true;
          break;
        } else if( in[step] < input ) {
          flag = false;
          break;
        }
      }
    }
    //linear is faster for small number of phones! log only for n-grams
    //Vertex.EdgeIndex ei = current.containsInput(in[step]);
    //int test = ((ei.oexists)?1:0) + ((flag)?1:0);
    //if( test==1 || edgeIndex!=ei.nindex ) {
    //  Print.dialog( "linear: " + flag + ", " + edgeIndex +
    //                "\tlog: " + ei.oexists + ", " + ei.nindex );
    //}
    if(flag) {//phone found; need to check if there is our output...
      flag = false;
      Edge e = current.getEdge(edgeIndex);
      nvert = e.getNumberOfVertices();
      for( int i=0; i<nvert; i++ ) {
        oc = e.getOutputContext( i );
        if( oc.m_output == out[step] ) {//output is found!
          current = oc.m_vertex;
          e.setVertexProbability( i, oc.m_fscore+1.0f );//increase the count
          e.m_fedgeProbability++;
          flag=true;
          break;
        }
      }
      if( !flag ) {//output was not found
        old = current;
        //need to check if this is a last step
        if( step==(in.length-1) ) {
          current = m_root;
        } else {
          current = getNewVertex();
        }
        //addDestination increments the input count as well.
        ((VertexG) old).setEdge( edgeIndex,
                                 e.addDestination(out[step],current) );
      }
    } else {//phone not found, need to add the entire phone
      Vertex v;
      if( step == (in.length-1) ) {//if it's last step, we return it to root
        v = m_root;
      } else {
        v = getNewVertex();
      }
      Edge e = getNewEdge( in[step], out[step], 1.0f, v );
      current.addEdge( e );
      current = v;
    }
    if( ++step<in.length ) {
      insertSequence( in, out, current, step );
    }
  }

  public String getInput( short n ) {
    //Print.dialog("Requested input is: " + n );
    if( n>=0 )
      return m_shortToInput[n];
    else
      return " ";
  }

  public short getInput( String word ) {
    if( m_inputToShort.containsKey( word ) )
      return ((Short) m_inputToShort.get(word)).shortValue();
    else
      return (short) -1;
  }

  public String getOutput( short n ) {
    if( n>=0 )
      return m_shortToOutput[n];
    else
      return " ";
  }

  public short getOutput( String word ) {
    if( m_outputToShort.containsKey( word ) )
      return ((Short) m_outputToShort.get(word)).shortValue();
    else
      return (short) -1;
  }

  public void updateTableFromIOHashtable( boolean io ) {
    String[] table;
    Hashtable hash;
    if( io ) {
      hash = m_inputToShort;
    } else {
      hash = m_outputToShort;
    }

    table = new String[hash.size()];
    Enumeration enume = hash.keys();
    while ( enume.hasMoreElements() ) {
      String s = (String) enume.nextElement();
      short ind = ((Short) hash.get( s )).shortValue();
      if( table[ind]==null ) {
        table[ind] = s;
      } else {
        Print.warning("Already set value for this token; old value:  "+
                      table[ind]+", new value: "+s);
        Print.warning("keeping old value");
      }
    }
    for( int i=0; i<table.length; i++ ) {
      if( table[i]==null ) {
        Print.error("Unexpected condition: not specified value in table; aborting");
        End.exit();
      }
    }

    if( io ) {
      m_shortToInput = table;
    } else {
      m_shortToOutput = table;
    }
  }

  //again assuming input if io=true
  private void loadWordHashtable( String filename, boolean io ) {
    File file = new File( filename );
    byte[] data = new byte[0];
    if( !file.exists() || !file.canRead() ) {
      Print.error( "Illegal filename: " + filename );
      End.exit();
    } else {
      try {
        int length = ((int)file.length());
        data = new byte[length];
        FileInputStream fileInputStream = new FileInputStream(file);
        fileInputStream.read( data );
      } catch (IOException e) {
        e.printStackTrace();
      }
      String strData = new String(data);
      StringTokenizer st = new StringTokenizer( strData );//tokenizes around ' '
      Hashtable hash = new Hashtable();
      int n = Integer.parseInt( st.nextToken() );
      //Print.dialog("vocabulary has "+n+" words.");
      String[] table = new String[n];
      short j = 0;
      while( st.hasMoreTokens() ) {
        String tok = st.nextToken();
        table[j] = tok;
        //Print.dialog("table["+j+"]="+table[j]);
        hash.put( tok, new Short(j++) );
      }
      //making sure that even if dictionary was setup manually we keep just
      //the name and not the path in m_headerProperties. must be in same
      //folder with file containing network
      filename = file.getPath();
      filename = filename.substring(filename.lastIndexOf(file.separator)+1);
      //Print.dialog(filename);
      if( io ) {
        m_inputToShort = hash;
        m_shortToInput = table;
        m_headerProperties.setProperty("Network.InputHashtableFileName",filename);
      } else {
        m_outputToShort = hash;
        m_shortToOutput =table;
        m_headerProperties.setProperty("Network.OutputHashtableFileName",filename);
      }
    }
  }

  private void saveWordHashtable( String filename, boolean io ) {
    Print.dialog("saving hashtable to: "+filename);
    String[] table;
    if( io ) table = m_shortToInput;
    else table = m_shortToOutput;
    int size = table.length;
    try {
      PrintWriter pw = new PrintWriter(new BufferedWriter( new FileWriter(filename) ));
      pw.print( size );
      for( int i=0; i<size; i++ ) pw.print(" "+table[i]);
      pw.close();
    } catch ( IOException e ) {
      e.printStackTrace();
    }
  }

  //recursive save function
  private void saveStructure( Vertex v, DataOutputStream dout )
                                                  throws IOException {
    //dout.writeInt( v.m_nid );
    int nedges = v.getNumberOfEdges();
    dout.writeShort( nedges );
    Edge e;
    Edge.OutputContext oc;
    int nvert, id;
    boolean[][] osave = new boolean[nedges][];
    for( int i=0; i<nedges; i++ ) {
      e = v.getEdge(i);
      dout.writeInt(e.m_nid);
      dout.writeFloat(e.m_fedgeProbability);
      dout.writeShort(e.m_input);
      nvert = e.getNumberOfVertices();
      dout.writeShort(nvert);
      //slightly better recursive save...
      osave[i] = new boolean[nvert];
      for( int j=0; j<nvert; j++ ) {
        oc = e.getOutputContext( j );
        dout.writeFloat( oc.m_fscore );
        dout.writeShort( oc.m_output );
        id = oc.m_vertex.getID();
        dout.writeInt( id );
        if( id<0 ) {//internal node
          osave[i][j] = true;
        } else if( m_overtexFlag[id] ) {
          osave[i][j] = false;
        } else {
          osave[i][j] = true;
          m_overtexFlag[id] = true;
        }
      }
    }
    for( int i=0; i<nedges; i++ ) {
      e = v.getEdge(i);
      nvert = e.getNumberOfVertices();
      for( int j=0; j<nvert; j++ ) {
        if( osave[i][j] ) {
          oc = e.getOutputContext( j );
          saveStructure( oc.m_vertex, dout );
        }
      }
    }
  }

  /**
   * This function implements the same order of data writing to a file as
   * saveStructure but avoids stack overflow for too many recursive calls!!!
   */
  private void saveStructureNonrecursive( Vertex v, DataOutputStream dout,
                                          LinkedList list ) throws IOException {
    //basically the same behaviour as saveStructure()
    int nedges = v.getNumberOfEdges();
    dout.writeShort( nedges );
    Edge e;
    Edge.OutputContext oc;
    int nvert, id;
    boolean[][] osave = new boolean[nedges][];
    for( int i=0; i<nedges; i++ ) {
      e = v.getEdge(i);
      dout.writeInt(e.m_nid);
      dout.writeFloat(e.m_fedgeProbability);
      dout.writeShort(e.m_input);
      nvert = e.getNumberOfVertices();
      dout.writeShort(nvert);
      //non-recursive save...
      osave[i] = new boolean[nvert];
      for( int j=0; j<nvert; j++ ) {
        oc = e.getOutputContext( j );
        dout.writeFloat( oc.m_fscore );
        dout.writeShort( oc.m_output );
        id = oc.m_vertex.getID();
        dout.writeInt( id );
        if( id<0 ) {
          osave[i][j] = true;
        } else if( m_overtexFlag[id] ) {
          osave[i][j] = false;
        } else {
          osave[i][j] = true;
          m_overtexFlag[id] = true;
        }
      }
    }
    for( int i=nedges-1; i>=0; i-- ) {
      e = v.getEdge(i);
      nvert = e.getNumberOfVertices();
      for( int j=nvert-1; j>=0; j-- ) {
        if( osave[i][j] ) {
          oc = e.getOutputContext( j );
          list.addFirst( oc.m_vertex );
        }
      }
    }
  }

  private void loadStructure( Vertex.Context vc, DataInputStream din,
                              VertexG[] allVertices ) throws IOException {
    int nedges = din.readShort();
    Edge[] edges = new Edge[nedges];
    Edge e;
    Edge.OutputContext oc;
    int nvert, id;
    boolean[][] oload = new boolean[nedges][];
    for( int i=0; i<nedges; i++ ) {
      int edgeindex = din.readInt();
      float edprob = din.readFloat();
      short input = din.readShort();
      nvert = din.readShort();
      oload[i] = new boolean[nvert];
      float[] veprob = new float[nvert];
      short[] output = new short[nvert];
      Vertex[] desc = new Vertex[nvert];
      for( int j=0; j<nvert; j++ ) {
        veprob[j] = din.readFloat();
        output[j] = din.readShort();
        id = din.readInt();
        if( id<0 ) {
          oload[i][j] = true;
        } else {
          desc[j] = allVertices[id];
          if( !m_overtexFlag[id] ) {
            m_overtexFlag[id] = true;
            oload[i][j] = true;
          } else {
            oload[i][j] = false;
          }
        }
      }
      e = getNewEdge( input, output, edprob, veprob, desc );
      e.m_nid = edgeindex;
      edges[i] = e;
    }
    for( int i=0; i<nedges; i++ ) {
      nvert = edges[i].getNumberOfVertices();
      for( int j=0; j<nvert; j++ ) {
        if( oload[i][j] ) {
          oc = edges[i].getOutputContext( j );
          if( oc.m_vertex==null ) {
            loadStructure( new Vertex.Context(edges[i],j), din, allVertices );
          } else {
            loadStructure( new Vertex.Context(oc.m_vertex), din, allVertices );
          }
        }
      }
    }
    //if we passed vertex, it's a stable vertex with array implementation
    if( vc.isVertex() ) {
      ((VertexG) vc.getVertex()).setEdges( edges );
    } else { //ow we create it here and now (must have just a single parent)
      vc.createVertex( edges );
    }
  }

  private void loadStructureNonrecursive( Vertex.Context vc,
                        DataInputStream din, LinkedList list,
                        VertexG[] allVertices ) throws IOException {
    int nedges = din.readShort();
    Edge[] edges = new Edge[nedges];
    Edge e;
    Edge.OutputContext oc;
    int nvert, id;
    boolean[][] oload = new boolean[nedges][];
    for( int i=0; i<nedges; i++ ) {
      int edgeindex = din.readInt();
      float edprob = din.readFloat();
      short input = din.readShort();
      nvert = din.readShort();
      oload[i] = new boolean[nvert];
      float[] veprob = new float[nvert];
      short[] output = new short[nvert];
      Vertex[] desc = new Vertex[nvert];
      for( int j=0; j<nvert; j++ ) {
        veprob[j] = din.readFloat();
        output[j] = din.readShort();
        id = din.readInt();
        if( id<0 ) {
          oload[i][j] = true;
        } else {
          desc[j] = allVertices[id];
          if( !m_overtexFlag[id] ) {
            m_overtexFlag[id] = true;
            oload[i][j] = true;
          } else {
            oload[i][j] = false;
          }
        }
      }
      e = getNewEdge( input, output, edprob, veprob, desc );
      e.m_nid = edgeindex;
      edges[i] = e;
    }
    for( int i=nedges-1; i>=0; i-- ) {
      nvert = edges[i].getNumberOfVertices();
      for( int j=nvert-1; j>=0; j-- ) {
        if( oload[i][j] ) {
          oc = edges[i].getOutputContext( j );
          if( oc.m_vertex==null ) {
            list.addFirst( new Vertex.Context(edges[i],j) );
          } else {
            list.addFirst( new Vertex.Context(oc.m_vertex) );
          }
        }
      }
    }
    //if we passed vertex, it's a stable vertex with array implementation
    if( vc.isVertex() ) {
      ((VertexG) vc.getVertex()).setEdges( edges );
    } else { //ow we create it here and now (must have just a single parent)
      vc.createVertex( edges );
    }
  }

  /**
   * Assumes potential backoffs have input -1 and are the first edge coming out
   * of the vertex. All vertices connected through such edges are considered
   * equivalent, and if there is a loop, the function may never return. (However
   * no routine so far can create such a loop, and it would be meaningless...)
   * If a sequence of transitions exists that takesspecified input tokens, with
   * (possibly) some backoffs in between, the function returns "true".
   */
  public boolean accept( String[] tokens ) {
    return accept( tokens, 0, m_root );
  }

  private boolean accept( String[] tokens, int curToken, Vertex v ) {
    Edge.OutputContext oc;
    Edge e = v.getEdge(0);
    //checking for the backoff...
    if( e.m_input==-1 ) {
      oc = e.getOutputContext(0);
      if( accept( tokens, curToken, oc.m_vertex ) ) {
        return true;
      }
    }
    int nvert, nedges;
    short tok = ((Short) m_inputToShort.get(tokens[curToken])).shortValue();
    nedges = v.getNumberOfEdges();
    for( int i=0; i<nedges; i++ ) {
      e = v.getEdge( i );
      if( e.m_input == tok ) {
        nvert = e.getNumberOfVertices();
        for( int j=0; j<nvert; j++ ) {
          oc = e.getOutputContext( j );
          if( accept( tokens, curToken+1, oc.m_vertex ) ) {
            return true;
          }
        }
        break;
      }
    }
    return false;
  }

  public TableOfLabels getTableOfLabels( boolean io ) {
    Type type;
    if( io ) type = this.m_inputClass;
    else type = this.m_outputClass;

    if( type==Type.TIMIT39 )
      return new TableOfLabels( TableOfLabels.Type.TIMIT39 );
    else if( type==Type.TIMIT48 )
      return new TableOfLabels( TableOfLabels.Type.TIMIT48 );
    else if( type==Type.ISIP41 )
      return new TableOfLabels( TableOfLabels.Type.ISIP41 );
    else if( type==Type.TIDIGITS )
      return new TableOfLabels( TableOfLabels.Type.TIDIGITS );
    else {
      End.throwError("Not valid operation.");
      return null;
    }
  }

  /**
   * For now modifies the vertex id-s so that when the dictionary is loaded
   * it is in compact format. Also modifies the probabilities so
   * that each pronunciation has fraction  of probability depending only
   * on it's associated word. It expects the network to have absolute
   * probabilities or counts. The only fields that matter are those at the
   * output (end-of-word) points. The rest are reset to 0.
   */
  protected void finalizeDictionary() {
    //prepare hashtable with all the words
    if( this.m_oisProbabilityInLogDomain ) {
      Print.error("Probability should not be in log domain. Check usage...");
      End.exit(0);
    }
    float[] words = new float[m_shortToOutput.length];
    for( int i=0; i<m_shortToOutput.length; i++ ) {
      words[i] = 0.0f;
    }
    m_overtexFlag = new boolean[m_nvertices];
    for( int i=0; i<m_nvertices; i++ ) {
      m_overtexFlag[i] = false;
    }
    m_overtexFlag[m_root.getID()] = true;
    removeID( m_root, words );
    //sanity check
    for( int i=0; i<words.length; i++ ) {
      if( words[i] <= 0.0f ) {
        Print.warning( "Some words have still got zero count: " +
                       m_shortToOutput[i] +"("+i+")"+ " score = " + words[i] );
        Print.warning("Normal missing counts are for special symbols like <EPS>" );
        //End.exit(0);
      }
    }
    m_overtexFlag = null;
    m_nvertices = 1;
    if( m_root.getID()!=0 ) {
      Print.error("root should be zero... check this");
      End.exit(0);
    }
    //now fixing the probabilities
    m_oisProbabilityInLogDomain = true;
    fixDictionaryProbabilities( m_root, words );
  }

  /**
   * Removes id from internal vertices and gathers the total counts for all
   * the words separately...
   */
  private void removeID( Vertex v, float[] words ) {
    Edge e;
    Edge.OutputContext oc;
    Vertex v2;
    int n = v.getNumberOfEdges();
    int k, id;
    for( int i=0; i<n; i++ ) {
      e = v.getEdge( i );
      k = e.getNumberOfVertices();
      for( int j=0; j<k; j++ ) {
        oc = e.getOutputContext(j);
        if( oc.m_output<0 ) {//this means internal node
          if( m_overtexFlag[oc.m_vertex.getID()] ) {
            Print.error("We were not supposed to reach this vertex before!!!");
            End.exit(0);
          } else {
            m_overtexFlag[oc.m_vertex.getID()] = true;
            ((VertexG)oc.m_vertex).removeID();
            removeID( oc.m_vertex, words );
          }
        } else {//the destination is root.
          //assuming absolute probabilities...
          if( k==1 ) {
            words[oc.m_output] += e.m_fedgeProbability;
          } else {
            words[oc.m_output] += oc.m_fscore;
          }
          if( oc.m_output==1 ) {
            Print.dialog("The contents of edgeProb:outProb:k:words[1] "+
                         e.m_fedgeProbability+":"+oc.m_fscore+":"+k+":"+words[1]);
          }
        }
      }
    }
  }

  private void fixDictionaryProbabilities( Vertex v, float[] words ) {
    Edge e;
    Edge.OutputContext oc;
    Vertex v2;
    int n = v.getNumberOfEdges();
    int k, id;
    for( int i=0; i<n; i++ ) {
      e = v.getEdge( i );
      k = e.getNumberOfVertices();
      for( int j=0; j<k; j++ ) {
        oc = e.getOutputContext(j);
        if( oc.m_output<0 ) {//this means internal node
          e.setVertexProbability( j, 0.0f );
          e.m_fedgeProbability = 0.0f;
          fixDictionaryProbabilities( oc.m_vertex, words );
        } else {//the destination is root.
          //assuming absolute probabilities...
          if( k==1 ) {
            e.m_fedgeProbability =
              (float) Math.log((double) e.m_fedgeProbability /
                                        words[oc.m_output] );
          } else {
            float score = (float) Math.log((double) oc.m_fscore /
                                                    words[oc.m_output] );
            e.setVertexProbability( j, score );
            e.m_fedgeProbability = 0.0f;
          }
        }
      }
    }
  }

  protected void changeToLogDomain() {
    if( !m_oisProbabilityInLogDomain ) {
      m_oisProbabilityInLogDomain = true;
      Vertex v;
      m_overtexFlag = new boolean[m_nvertices];
      for( int i=0; i<m_nvertices; i++ ) {
        m_overtexFlag[i] = false;
      }
      m_overtexFlag[m_root.getID()] = true;
      change2Log( m_root );
      //freeing some memory...
      m_overtexFlag = null;
    } else {
      Print.dialog("Probabilities were already in log domain...");
    }
  }

  private void change2Log( Vertex v ) {
    Edge e;
    Edge.OutputContext oc;
    Vertex v2;
    int n = v.getNumberOfEdges();
    int k, id;
    for( int i=0; i<n; i++ ) {
      e = v.getEdge(i);
      e.m_fedgeProbability = LogDomainCalculator.calculateLog(
                                              (double) e.m_fedgeProbability );
      k = e.getNumberOfVertices();
      for( int j=0; j<k; j++ ) {
        oc = e.getOutputContext( j );
        if( oc.m_fscore != 0.0f ) {
          float score = LogDomainCalculator.calculateLog((double) oc.m_fscore );
          e.setVertexProbability( j, score );
        }
        v2 = oc.m_vertex;
        id = v2.getID();
        if( id<0 ) {
          change2Log( v2 );
        } else {
          if( !m_overtexFlag[id] ) {
            m_overtexFlag[id] = true;
            change2Log( v2 );
          }
        }
      }
    }
  }

  //assumes input if io=true
  private void convertTableOfLabels ( TableOfLabels tol, boolean io ) {
    String[] table;
    Hashtable hash = new Hashtable();
    table = new String[tol.getNumberOfEntries()];
    String[] temp;
    for( short i=0; i<tol.getNumberOfEntries(); i++ ) {
      temp = tol.getLabels(i);
      Short value = new Short( i );
      table[i] = temp[0];
      hash.put( temp[0], value );
      for( short j=1; j<temp.length; j++ )
        hash.put( temp[j], value );
    }
    if( io ) {
      m_inputToShort = hash;
      m_shortToInput = table;
    } else {
      m_outputToShort = hash;
      m_shortToOutput = table;
    }
  }

  private float add( float x, float y ) {
    if( m_oisProbabilityInLogDomain ) {
      float a,b;
      //make sure a>b
      if( x>y ) {
        a = x;
        b = y;
      } else {
        a = y;
        b = x;
      }
      return a + (float) Math.log(1.0+Math.exp((double)b-a));
    } else {
      return x+y;
    }
  }

  private float div( float x, float y ) {
    if( m_oisProbabilityInLogDomain ) {
      return x - y;
    } else {
      return x / y;
    }
  }

  private float mul( float x, float y ) {
    if( m_oisProbabilityInLogDomain ) {
      return x + y;
    } else {
      return x * y;
    }
  }

  public static class Type extends SuperType {
    public static final Type TIMIT39 = new Type("TIMIT39");
    public static final Type TIMIT48 = new Type("TIMIT48");
    public static final Type ISIP41 = new Type("ISIP41");
    public static final Type TIDIGITS = new Type("TIDIGITS");
    public static final Type WORDS = new Type("WORDS");
    public static final Type EMPTY = new Type("EMPTY");
    protected static Type[] m_types;

    //In case of adding a new Type (above), don't forget to add it below.
    static {
      m_types = new Type[6];
      m_types[0] = TIMIT39;
      m_types[1] = TIMIT48;
      m_types[2] = ISIP41;
      m_types[3] = TIDIGITS;
      m_types[4] = WORDS;
      m_types[5] = EMPTY;
    }

    //constructor is protected
    protected Type(String strName) {
      m_strName = strName;
    }

    public static boolean isValid(String typeIdentifierString) {
      for (int i=0; i<m_types.length; i++) {
        //notice case sensitive comparison
        if( typeIdentifierString.equals( m_types[i].toString() ) ) {
          return true;
        }
      }
      return false;
    }

    private static int getTypeIndex(String typeIdentifierString) {
      for (int i=0; i<m_types.length; i++) {
        //notice case sensitive comparison
        if( typeIdentifierString.equals( m_types[i].toString() ) ) {
          return i;
        }
      }
      return -1;
    }

    /**Return the Type correspondent to the given indentifier
     * String or null in case identifier is not valid.
     */
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
    public static final Type getTypeAndExitOnError(String typeIdentifierString) {
      if (isValid(typeIdentifierString)) {
        int nindex = getTypeIndex(typeIdentifierString);
        return m_types[nindex];
      } else {
        Print.error(typeIdentifierString + " is not a valid Type.");
        End.exit();
        //make compiler happy:
        return null;
      }
    }
  }
}
