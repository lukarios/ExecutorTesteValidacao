package br.org.fdte.testCase;

import java.io.File;
import javax.xml.parsers.DocumentBuilder ;
import javax.xml.parsers.DocumentBuilderFactory ;
import javax.xml.parsers.ParserConfigurationException ;
import javax.xml.transform.Transformer ;
import javax.xml.transform.TransformerException ;
import javax.xml.transform.TransformerFactory ;
import javax.xml.transform.dom.DOMSource ;
import javax.xml.transform.stream.StreamResult ;

import org.w3c.dom.Attr ;
import org.w3c.dom.Document ;
import org.w3c.dom.Element ;

public class WriteXMLFile {

 public static void main ( String argv [ ] ) {

    try {

	  DocumentBuilderFactory docFactory = DocumentBuilderFactory. newInstance ( ) ;
	  DocumentBuilder docBuilder = docFactory. newDocumentBuilder ( ) ;

	  //root elements / / Elementos de raiz
	  Document doc = docBuilder. newDocument ( ) ;
	  Element rootElement = doc. createElement ( "company" ) ;
	  doc. appendChild ( rootElement ) ;

	  //staff elements / / Elementos do pessoal
	  Element staff = doc. createElement ( "Staff" ) ;
	  rootElement. appendChild ( staff ) ;

	  //set attribute to staff element / Atributo / set para o elemento pessoal
	  Attr attr = doc. createAttribute ( "id" ) ;
	  attr. setValue ( "1" ) ;
	  staff. setAttributeNode ( attr ) ;

	  //shorten way / / Encurtar caminho
	  //staff.setAttribute("id", "1"); / / Staff.setAttribute ("id", "1");

	  //firstname elements / / Elementos firstname
	  Element firstname = doc. createElement ( "firstname" ) ;
	  firstname. appendChild ( doc. createTextNode ( "yong" ) ) ;
	  staff. appendChild ( firstname ) ;

	  //lastname elements / / Elementos apelido
	  Element lastname = doc. createElement ( "lastname" ) ;
	  lastname. appendChild ( doc. createTextNode ( "mook kim" ) ) ;
	  staff. appendChild ( lastname ) ;

	  //nickname elements / / Elementos apelido
	  Element nickname = doc. createElement ( "nickname" ) ;
	  nickname. appendChild ( doc. createTextNode ( "mkyong" ) ) ;
	  staff. appendChild ( nickname ) ;

	  //salary elements / / Elementos de salário
	  Element salary = doc. createElement ( "salary" ) ;
	  salary. appendChild ( doc. createTextNode ( "100000" ) ) ;
	  staff. appendChild ( salary ) ;

	  //write the content into xml file / / Escreve o conteúdo em arquivo xml
	  TransformerFactory transformerFactory = TransformerFactory. newInstance ( ) ;
	  Transformer transformer = transformerFactory. newTransformer ( ) ;
	  DOMSource source = new DOMSource ( doc ) ;
	  StreamResult result = new StreamResult ( new File ( "C:\\Users\\FDTE-Luciana\\Documents\\LucianaRios\\FDTE\\ testing.xml" ) ) ;
	  transformer. transform ( source, result ) ;

	  System . out . println ( "Done" ) ;

     } catch ( ParserConfigurationException pce ) {
	  pce. printStackTrace ( ) ;
     } catch ( TransformerException tfe ) {
	 tfe. printStackTrace ( ) ;
     }
 }
} 