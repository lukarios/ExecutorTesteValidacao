package executortestevalidacao;

import java.util.Collection;
import java.util.ArrayList;
import java.util.Iterator;
import java.io.ByteArrayInputStream;
import java.util.Queue;
import java.util.LinkedList;

import javax.xml.parsers.SAXParserFactory;
import javax.xml.parsers.SAXParser;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import org.w3c.dom.NodeList;
import org.w3c.dom.Node;

import static org.w3c.dom.Node.TEXT_NODE;

import br.org.fdte.persistence.TemplateDocumento;
import br.org.fdte.persistence.Atributo;
import br.org.fdte.persistence.ClasseEquivalencia;
import br.org.fdte.persistence.TipoClasseEquivalencia;
import br.org.fdte.persistence.Valor;
import br.org.fdte.persistence.Especificos;

public class AtributesAndValues {
   public class Value {
       Value(String v, boolean p) {
           value = v;
           positive = p;
       } // constructor

       Value(Value v) {
           this.value = v.value;
           this.positive = v.positive;
       }
       String value;
       boolean positive;
   }

   public enum AtributeType { REGULAR, START_OF_GROUP, END_OF_GROUP };

   public class Atribute {
       int id;
       String name;
       boolean opcional = false;
       int maxOccur = 1;
       Collection<Value> values;
       AtributeType type = AtributeType.REGULAR;
       Atribute() {
           values = new ArrayList<Value>();
       }

       Atribute(Atribute a) {
           this.id = a.id;
           this.name = a.name;
           this.type = a.type;
           this.opcional = a.opcional;
           this.maxOccur = a.maxOccur;
           this.values = a.values;
       }

       Atribute(Atributo atr) {
           this.id = 0;
           this.name = atr.getTag();
           this.type = toAtributeType(atr.getIdClasseEquivalencia().getTipo());
           this.maxOccur = atr.getNumeroMaximoOcorrencias();
           this.opcional = toBoolean(atr.getOpcional());
           values = new ArrayList<Value>();
       }
   } // Atribute

   public class AtributeIterator {
       Iterator<Value> it = null;
       Atribute atr = null;
       Boolean positive = null;
       Value currentValue = null;

       AtributeIterator(Atribute atr, Boolean positive) {
           this.atr = atr;
           this.positive = positive;
           it = atr.values.iterator();
       } // constructor

       Value getCurrentValue() {
           return currentValue;
       }

       boolean hasNext() {
           return it.hasNext();
       }

       void rewind() {
           it = atr.values.iterator();
       }
       
       Value next() {
           if ( positive == null ) {
               if ( it.hasNext()) {
                   currentValue = it.next();
               }  else {
                   currentValue = null;
               }
               return currentValue;
           }

           Value v = null;
           currentValue = null;
           while ( it.hasNext() ) {
               v = it.next();
               if ( v.positive == positive) {
                   currentValue = v;
                   break;
               }
           } // while
           return currentValue;
       } // next
   } // AtributeIterator

   private Collection<Atribute> atributes;
   private int maxAtrId = 0;

   AtributesAndValues(TemplateDocumento doc) throws Exception {
       Collection<Atributo> atrs = doc.getAtributoCollection();
       atributes = new ArrayList<Atribute>();
       for ( Atributo a : atrs) {
           Atribute t = new Atribute();
           t.id = maxAtrId++;
           t.name = a.getTag();
           t.opcional = toBoolean(a.getOpcional());
           t.maxOccur = a.getNumeroMaximoOcorrencias();
           t.values = new ArrayList<Value>();
           ClasseEquivalencia ce = a.getIdClasseEquivalencia();
           t.type = toAtributeType(ce.getTipo());
           Collection<Valor> vals = ce.getValorCollection();
           for ( Valor v : vals) {
               Value u = new Value(v.getValor(), toBoolean(v.getPositivoNegativo()));
               t.values.add(u);
           } // for each value
           atributes.add(t);
       } //for each atribute
   } // constructor from DocumentTemplate

   AtributesAndValues() {
       atributes = new ArrayList<Atribute>();
   } // constructor for empty document



   /* DOM version of byte[] constructor
    * It does not work properly for multi-level repetitions
   AtributesAndValues(byte[] raw) throws Exception {
      ByteArrayInputStream is = new ByteArrayInputStream(raw);
      DocumentBuilder docBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
      Document doc = docBuilder.parse(is);
      atributes = new ArrayList<Atribute>();
      NodeList nodes = doc.getChildNodes();
      addAtribute(atributes, nodes);
   } // constructor from XML data stored in database
  */

   /* SAX version of byte[] constructor
    */
   class SaxHandler extends DefaultHandler {

       private Collection<Atribute> atributes = null;
       private int maxAtrId = 0;
       private Queue<String> queue = new LinkedList<String>();
       private String pValue = null;

       SaxHandler(Collection<Atribute> atrs) {
           atributes = atrs;
       } // constructor

        @Override
        public void characters(char[] ch, int start, int length) throws SAXException {
            //super.characters(ch, start, length);
            pValue = new String(ch, start, length);
            //System.out.println("chars " + pValue);
        }

        @Override
        public void endElement(String uri, String localName, String qName) throws SAXException {
            //super.endElement(uri, localName, qName);
            //System.out.println("end of element " + qName);
            if (qName.equals("teste_tool")) {
                return;
            }
            Atribute x = new Atribute();
            atributes.add(x);
            x.id = maxAtrId++;
            x.name = qName;
            String curr = queue.poll();
            if (qName.equals(curr)) {
                x.values.add(new Value(pValue, true));
            } else {
                // just End_Of_Element with no value
                x.type = AtributeType.END_OF_GROUP;
            }
        }

        @Override
        public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
            //super.startElement(uri, localName, qName, attributes);
            //System.out.println("start of element " + qName);
            if (qName.equals("teste_tool")) {
                return;
            }
            while (! queue.isEmpty()) {
                Atribute x = new Atribute();
                atributes.add(x);
                x.id = maxAtrId++;
                x.name = queue.remove();
                x.type = AtributeType.START_OF_GROUP;
            }
            queue.offer(qName);
        } // startElement

        int getMaxAtrId() {
            return maxAtrId;
        } // getMaxAtrId

   } // SaxHandler

   AtributesAndValues(byte[] raw) throws Exception {
       atributes = new ArrayList<Atribute>();
       ByteArrayInputStream is = new ByteArrayInputStream(raw);
       SaxHandler handler = new SaxHandler(atributes);
       SAXParser parser = SAXParserFactory.newInstance().newSAXParser();
       parser.parse(is, handler);
       this.maxAtrId = handler.getMaxAtrId();
   } // SAX constructor for XML data stored in database

   private Collection buildRepetitionGroup(Atribute a) {
     Collection col = new ArrayList<Atribute>();
     int stack = 0;

     // no sense in repeating end_of_group
     if (a.type.equals(AtributeType.END_OF_GROUP)) {
         return null;
     }

     // repetition of a single element
     if (a.type.equals(AtributeType.REGULAR)) {
         col.add(a);
         return col;
     }

     // repetition of a GROUP
     if (a.type.equals(AtributeType.START_OF_GROUP)) {
         boolean addToCol = false;
         for (Atribute t : atributes) {

             if ( (addToCol) &&
                  (t.type.equals(AtributeType.REGULAR))) {
                 col.add(t);
             }

             if ( (addToCol) &&
                  (t.type.equals(AtributeType.START_OF_GROUP))) {
                 col.add(t);
                 stack++;
             }

             if ( (addToCol) &&
                  (t.type.equals(AtributeType.END_OF_GROUP))) {
                 col.add(t);
                 stack--;
                 if (stack == 0) {
                     break;
                 }
             }

             if (a.name.equals(t.name)) {
                 col.add(t);
                 addToCol = true;
                 stack++;
             }

         }
         return col;
     }
     return null;
   } // buildRepetitionGroup

   private void addRepetitionGroup(Collection<Atribute> group, int reps) throws Exception {
       if ( (group == null) || (reps == 0))
           return;

       // find insertion point and insert group,
       // as many times as specified in reps
       // Go on on atributes collection to update atribute_id
       Iterator<Atribute> it = group.iterator();
       Atribute first = it.next();
       ArrayList<Atribute> atrs = (ArrayList<Atribute>)atributes;
       boolean inserted = false;
       boolean firstFound = false;
       int insertAtPos = 0;

       for (int i=0; i < atrs.size(); i++) {
           Atribute a = atrs.get(i);

           if (inserted) {
              a.id += (reps-1) * group.size();
           }

           if (firstFound) {
               if (it.hasNext()) {
                   Atribute fromG = it.next();
                   if (!it.hasNext()) {
                       for (int k = 0; k < reps - 1; k++) {
                           Iterator<Atribute> itrep = group.iterator();
                           int idx = 0;
                           while (itrep.hasNext()) {
                               Atribute atrRep = itrep.next();
                               atrRep.id = (i+1) + k * group.size() + idx;
                               idx++;
                               atrs.add(atrRep.id, atrRep);
                           }
                       }
                       firstFound = false;
                       inserted = true;
                       i += insertAtPos + (reps - 1) * group.size();
                   }
               }
           }

           if (a.name.equals(first.name)) {
               if (group.size() == 1) {
                   int atPos = i;
                   for (int j=0; j<reps-1;j++) {
                       atPos++;
                       Atribute newA = new Atribute(a);
                       newA.id = atPos;
                       atrs.add(atPos, newA);
                   }
                   i += (reps-1);
                   inserted = true;
               } else {
                   firstFound = true;
                   insertAtPos = i;
               }
           }
       }
       this.maxAtrId += (reps-1) * group.size();
   } // addRepetitionGroup

   public void addRepetitions(int repetitions, Collection<Especificos> especificos) throws Exception {

      if ( (especificos == null) &&
           (repetitions == 0) ) {
          return;
      }

      for (Especificos e : especificos ) {
          if ( "R".equals(e.getTipo())) {
              Atribute a = new Atribute(e.getAtributo());
              if ( AtributeType.END_OF_GROUP.equals(a.type)) {
                  continue;
              }
              if (AtributeType.REGULAR.equals(a.type)) {
                  Collection c = new ArrayList<Atribute>();
                  c.add(a);
                  addRepetitionGroup(c, e.getQuantidade());
              }
              if (AtributeType.START_OF_GROUP.equals(a.type)) {
                  Collection c = buildRepetitionGroup(a);
                  addRepetitionGroup(c, e.getQuantidade());
              }
          }
      } // for Especifios
   } // addRepetitions

   private void addAtribute(Collection<Atribute> atributes, NodeList nodelist) {
       for (int i = 0; i < nodelist.getLength(); i++) {
           Node node = nodelist.item(i);
           if (node.getNodeType() == TEXT_NODE) { // skip
               continue;
           }
           if (!node.getNodeName().contains("teste_tool")) {
               Atribute x = new Atribute();
               atributes.add(x);
               x.id = maxAtrId++;
               x.name = node.getNodeName();
               String val = getValue(node);
               x.values.add(new Value(val, true));
           }
           if ( node.hasChildNodes()) {
               addAtribute(atributes, node.getChildNodes());
           }
       } // for each node
   } // addAtribute

   private String getValue(Node node) {
       if ( node.hasChildNodes()) {
           NodeList children = node.getChildNodes();
           for (int i=0; i < children.getLength(); i++) {
               Node child = children.item(i);
               if ( child.getNodeType() == TEXT_NODE) {
                   return child.getNodeValue();
               }
           }
       }
       return null;
   } // getValue
   
   AtributesAndValues (AtributesAndValues in, Boolean positive, Boolean firstValueOnly) {
       atributes = new ArrayList<Atribute>();
       for ( Atribute t : in.atributes ) {
           Atribute x = new Atribute();
           atributes.add(x);
           x.id = maxAtrId++;
           x.name = t.name;
           x.opcional = t.opcional;
           x.maxOccur = t.maxOccur;
           x.type = t.type;
           if ( positive != null ) {
               for ( Value v : t.values) {
                   if ( Boolean.valueOf(v.positive).equals(positive) ) {
                       x.values.add( new Value (v.value, v.positive));
                       if (firstValueOnly) {
                          break;
                       }
                   }
               } // look for desired value
           } // set value
       } // for each atribute
   } // constructor from another document but with no Values


   private void add(Atribute t) {
       atributes.add(t);
   } // add

   Collection<Atribute> createListOfOptionalFields() {
      Collection<Atribute> ret = new ArrayList<Atribute>();
      for (Atribute atr : atributes) {
          if (atr.opcional)
              ret.add(atr);
      } // each atribute
      return ret;
   } // createListOfOptionalFiedls

   int createWithNoOptionalField(AtributesAndValues ret, Atribute t) {
       int hasOpcional = 0;
       int groupStack = 0;
       boolean removeGroup = false;

       if ( (t!= null) && (t.type == AtributeType.END_OF_GROUP) ) {
           return 0;
       }

       for ( Atribute atr : atributes ) {
           if ( atr.opcional ) {
               if (( t == null ) || ( t.id == atr.id ) || (removeGroup)){
                   if ( (removeGroup) && (atr.type.equals(AtributeType.START_OF_GROUP))) {
                       groupStack++;
                   }

                   if ( (removeGroup) && (atr.type.equals(AtributeType.END_OF_GROUP))) {
                       groupStack--;
                       if (groupStack == 0) {
                           removeGroup = false;
                       }
                   }

                   if ( ( t== null) && (atr.type.equals(AtributeType.START_OF_GROUP))) {
                       removeGroup = true;
                       groupStack++;
                   }

                   if ( (t != null) && ( t.id == atr.id) && (t.type.equals(AtributeType.START_OF_GROUP))) {
                       groupStack++;
                       removeGroup = true;
                   }
                   

                   hasOpcional++;
               } else {
                   if (!removeGroup) {
                     ret.add(atr);
                   }
               }
           } else {
               if (!removeGroup) {
                    ret.add(atr);
               }
           }
       } // for each attribute
       return hasOpcional;
   } // createWithNoOptionalField


   AtributeIterator getAtributeIterator(Atribute t, Boolean positive) {
       return new AtributeIterator(t, positive);
   } // getAtributeIterator
   
   Collection<Atribute> getAtributeCollection() {
       return atributes;
   } // getAtributeCollection

   void set(Atribute atr, Value v) {
       for (Atribute t : atributes ) {
           if ( t.id == atr.id ) {
               t.values.clear();
               t.values.add(v);
               break;
           }
       } // look for atr
   } // set

   void set(Atribute atr, String v) {
       for (Atribute t : atributes ) {
           if ( t.id == atr.id ) {
               t.values.clear();
               t.values.add( new Value(v, true));
               break;
           }
       } // look for atr       
   } //set

   Value get(Atribute atr) {
       for (Atribute t : atributes ) {
           if ( t.id == atr.id ) {
               if ( (t.values!=null) && (t.values.iterator().hasNext()) )
                   return t.values.iterator().next();
               else
                   return null;
           }
       } // look for atr
       return null;
   } // get

   static AtributeType toAtributeType (TipoClasseEquivalencia tce) {
       AtributeType ret = AtributeType.REGULAR;
       if (tce.getTipoClasseEquivalencia().equals("startgroup"))
           return AtributeType.START_OF_GROUP;
       else if (tce.getTipoClasseEquivalencia().equals("endgroup"))
           return AtributeType.END_OF_GROUP;
       return ret;
   } // toAtributeType

   /* Positive or Negative; Sim or Nao */
   static boolean toBoolean(String s) {
       if ( "N".equals(s) || "false".equalsIgnoreCase(s)) {
           return false;
       }
       return true;
   } // toBoolean

   void dump(String title) {
       System.out.println("Dumping doc " + title);
       for ( Atribute t : atributes ) {
           System.out.print("* Atribute id=" + t.id + " tag=" + t.name + " Type=" + t.type);
           for ( Value v : t.values ) {
               if ( v != null) {
                  System.out.print( " Value=" + v.value + " Positive=" + v.positive + " Opcional=" + t.opcional);
               } else {
                   System.out.print(" null value");
               }
           }
           System.out.println();
       } // for each atribute
   } // dump

   Atribute newAtribute() {
       Atribute a = new Atribute();
       a.id = maxAtrId++;
       atributes.add(a);
       return a;
   } // newAtribute

   String toXmlString() {
      StringBuilder s = new StringBuilder();
      s.append("<teste_tool>");
      for (Atribute a : atributes ) {
          if ( a.type.equals(AtributeType.START_OF_GROUP)) {
              s.append("<" + a.name + ">");
          } else if (a.type.equals(AtributeType.END_OF_GROUP)) {
              s.append("</" + a.name + ">");
          } else {
              s.append("<" + a.name + ">");
              String val = get(a).value;
              if ( val != null)
                  s.append(get(a).value);
              s.append("</" + a.name + ">");
          }
      } // for each atribute
      s.append("</teste_tool>");
      return s.toString();
   } // toXmlString

   byte[] getBytes() {
       return toXmlString().getBytes();
   } //getBytes
}// AtributesAndValues
