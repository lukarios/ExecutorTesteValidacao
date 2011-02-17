
package executortestevalidacao;

import java.util.Collection;

import br.org.fdte.persistence.*;
import br.org.fdte.testCase.TestCase;

import java.io.OutputStream;
import java.util.Vector;

import executortestevalidacao.ExecutorTesteValidacao.ExecutionMode;
import executortestevalidacao.ExecutorTesteValidacao.ExecutionResult;
import executortestevalidacao.ExecutorTesteValidacao.RetrievalResult;

import executortestevalidacao.AtributesAndValues.Atribute;
import executortestevalidacao.AtributesAndValues.Value;
import executortestevalidacao.AtributesAndValues.AtributeIterator;
import org.cabreva.edt.EDTIterativeManager;

public class PositiveTestExecution extends ExecutorTesteValidacao {

   private TestResults results = null;
   private AtributesAndValues templateDoc = null;
   private AtributesAndValues doc = null;
   private Vector<AtributeIterator> iterators = null;
   private long activationId = 0;
   private CaracterizacaoTesteValidacao teste = null;
   private int repetitions = 1;
   private Atribute[] atributesAsArray = null;
   private int currentVaryingAtribute = 0;

   
   PositiveTestExecution(ExecutionMode mode,
                         int numOfThreads,
                         OutputStream logStream,
                         boolean abort,
                         ExecucaoTesteValidacao currentExecution,
                         long activationId,
                         EDTIterativeManager edtExec,
                         TestCase tstCase) {

       this.mode = mode;
       this.numOfThreads = numOfThreads;
       this.logStream = logStream;
       this.abort = abort;
       this.currentExecution = currentExecution;
       this.activationId = activationId;
       this.edtExec = edtExec;
       this.tstCase = tstCase;
   } // constructor

   protected TestResults executePositiveTests(CaracterizacaoTesteValidacao teste,
                                              Collection<Especificos> especificos
                                              ) throws Exception {
       this.teste = teste;
       this.repetitions = teste.getCasosRepeticoes();
       results = new TestResults();
       int casesToTest = teste.getCasosPositivos();
       if ( casesToTest == 0) {
           fireEvent(ExecutionCallback.ExecutionEventType.NO_POSITIVE_TESTS_TO_EXECUTE, "Test", teste.getId(), "");
           return results;
       }
       templateDoc = new AtributesAndValues(teste.getDocumentoEntrada());
       AtributesAndValues docCopy = new AtributesAndValues(templateDoc, new Boolean(true), false);
       docCopy.dump("--------------------------- Documento SEM repeticoes ");
       docCopy.addRepetitions(repetitions, especificos);
       docCopy.dump("--------------------------- Documento COM repeticoes ");
       doc = new AtributesAndValues(docCopy, null, null); // null to build with no values
       doc.dump("--------------- Doc ");
       buildIterators(docCopy);
       boolean hasDocToGenerate = true;
       boolean generateDocsWithNoOpcionalFields = true;
       generateInitialDoc();
       while ( hasDocToGenerate ) {
           if (doc == null)
               break;
           RetrievalResult retRes = process(doc);
           if (needAbort(retRes.result)) {
              return results;
           }
           // strategy: generate variants only for the first valid doc
           if (generateDocsWithNoOpcionalFields) {
               generateDocsWithNoOpcionalFields(doc);
               generateDocsWithNoOpcionalFields = false;
           }
           generateNewDoc();
       } // while has DocToGenerate
       return results;
   } // executeNegativeTests

   private RetrievalResult process(AtributesAndValues doc) throws Exception {
       long activationTime = System.currentTimeMillis();
       tstCase.setType(positiveToString(true));
       RetrievalResult retRes = executeActivation(teste, doc, activationId);
       updateTestResults(retRes.result);       
       persistActivation(teste, doc, retRes.document, activationId, true, retRes.result, activationTime);
       activationId++;
       return retRes;
   } // process

   private void generateDocsWithNoOpcionalFields(AtributesAndValues doc) throws Exception {
       RetrievalResult retRes = null;
       AtributesAndValues docCopy = new AtributesAndValues();
       int numOpcionals = doc.createWithNoOptionalField(docCopy, null);
       if (numOpcionals > 0) {
           retRes = process(docCopy);
           if (needAbort(retRes.result)) {
               return;
           }
       }
       if (numOpcionals == 1) {
           return;
       }
       Collection<Atribute> opcionals = doc.createListOfOptionalFields();
       for (Atribute t : opcionals) {
           docCopy = new AtributesAndValues();
           if (doc.createWithNoOptionalField(docCopy, t) > 0) {
               retRes = process(docCopy);
               if (needAbort(retRes.result)) {
                   return;
               }

           }
       } // for each atribute
   } // submitDocsWithNoOpcionalFields

   private void generateInitialDoc() {
      Collection<Atribute> atrs = doc.getAtributeCollection();
      for (Atribute t : atrs ) {
         AtributeIterator it = iterators.elementAt(t.id);
         doc.set(t, it.next());
      }
      atributesAsArray = atrs.toArray(new Atribute[0]);
      currentVaryingAtribute = atributesAsArray.length-1;
   } // generateDoc

   private void generateNewDoc() {
      boolean foundNewValue = false;
      while ( ! foundNewValue ) {
          Atribute a = atributesAsArray[currentVaryingAtribute];
          AtributeIterator it = iterators.elementAt(a.id);
          Value v = null;
          if ( (it.hasNext()) && (( v = it.next())!= null)) {
             doc.set(a, v);
             foundNewValue = true;
             currentVaryingAtribute = atributesAsArray.length-1;
          } else {
              it.rewind();
              doc.set(a, it.next());
              currentVaryingAtribute--;
              if (currentVaryingAtribute < 0) {
                  doc = null;
                  break;
              }
          }
      } // while not found
   } // generateDoc

   private void buildIterators(AtributesAndValues in) {
       Collection<Atribute> atrs = in.getAtributeCollection();
       iterators = new Vector<AtributeIterator>();
       for (Atribute t : atrs ) {
           iterators.add(in.getAtributeIterator(t, true));
       }
   } // buildIterators

   private void updateTestResults(ExecutionResult res) {
       if (res.equals(ExecutionResult.FAILURE))
           results.positive_negative++;
       if (res.equals(ExecutionResult.SUCCESS))
           results.positive_positive++;
       if (res.equals(ExecutionResult.TIMEOUT))
           results.timeout++;
   } // updateTestResults

} // PositiveTestExecution
