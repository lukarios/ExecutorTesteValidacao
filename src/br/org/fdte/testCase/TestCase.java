package br.org.fdte.testCase;

import br.org.fdte.commons.exceptions.ExcFillData;
import java.util.List;
import java.io.File;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import java.io.IOException;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.xml.sax.SAXException;


import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import br.org.fdte.persistence.ExecucaoTesteValidacao;
import java.util.ArrayList;

public class TestCase {

    String type;
    String workflowPath;
    String testCasePath;
    String resultPath;
    List<DataGroup> dataGroups = new ArrayList<DataGroup>();
    ExecucaoTesteValidacao execucao;
    Long idActivation;
    String fileNameXML;
    private String fileNameResultXML;
    TestExecutionResult tstResult;

    public enum TestCaseResult {

        NOK, OK
    };

    public enum SystemStatus {

        FAIL, SUCCESS
    };

    public class TestExecutionResult {

        TestCaseResult tstCaseResult;
        String message;
        SystemStatus systemStatus;

        public TestCaseResult getTstCaseResult() {
            return tstCaseResult;
        }
    };

    public String getFileNameXML() {
        return fileNameXML;
    }

    public void setFields(List<Field> fields) {
        DataGroup dtGroup = new DataGroup();
        dtGroup.fields = fields;
        dataGroups.clear();
        dataGroups.add(dtGroup);
    }

    public List<Field> getFields() {
        return dataGroups.iterator().next().fields;
    }

    public void setIdActivation(Long idActivation) {
        this.idActivation = idActivation;
    }

    public void setExecution(ExecucaoTesteValidacao execucao) {
        this.execucao = execucao;
    }

    public void setType(String type) {
        this.type = type;
    }

    public void createFileXML() throws ExcFillData {
        try {

            DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder docBuilder = docFactory.newDocumentBuilder();

            Document doc = docBuilder.newDocument();
            Element rootElement = doc.createElement("testCase");
            doc.appendChild(rootElement);

            Attr attrType = doc.createAttribute("type");
            /*if (dataGroups.get(0).fields.get(0).isPositive)
            attrType.setValue("p");
            else
            attrType.setValue("n");*/
            attrType.setValue(type);
            rootElement.setAttributeNode(attrType);

            Element workflow = doc.createElement("workflow");
            rootElement.appendChild(workflow);

            Attr attrWorkflowName = doc.createAttribute("name");
            attrWorkflowName.setValue(workflowPath);
            workflow.setAttributeNode(attrWorkflowName);

            Element parameters = doc.createElement("parameters");
            rootElement.appendChild(parameters);

            for (DataGroup dtg : dataGroups) {

                Element dataGroup = doc.createElement("dataGroup");
                parameters.appendChild(dataGroup);

                Attr attrDtGroupName = doc.createAttribute("name");
                attrDtGroupName.setValue(dtg.name);
                dataGroup.setAttributeNode(attrDtGroupName);

                //para cada Field dentro de um datagroup
                for (Field field : dtg.fields) {

                    Element fieldElement = doc.createElement("field");
                    dataGroup.appendChild(fieldElement);

                    Attr attrFieldName = doc.createAttribute("name");
                    attrFieldName.setValue(field.name);
                    fieldElement.setAttributeNode(attrFieldName);

                    Attr attrFieldValue = doc.createAttribute("value");
                    attrFieldValue.setValue(field.value);
                    fieldElement.setAttributeNode(attrFieldValue);
                }
            }

            if (testCasePath == null) {
                throw new ExcFillData("Não existe um caminho para criar os casos de testes de entrada: ");
            }

            //write the content into xml file / / Escreve o conteúdo em arquivo xml
            TransformerFactory transformerFactory = TransformerFactory.newInstance();
            Transformer transformer = transformerFactory.newTransformer();
            DOMSource source = new DOMSource(doc);
            if (execucao.getId() == null) {
                fileNameXML = testCasePath + "\\Exec" + "_SystemExercise_" + idActivation.toString() + ".xml";
                fileNameResultXML = resultPath + "\\Exec" + "_SystemExercise_" + idActivation.toString() + ".xml";
            } else {
                fileNameXML = testCasePath + "\\Exec" + execucao.getId().toString() + "_" + idActivation.toString() + ".xml";
                fileNameResultXML = resultPath + "\\Exec" + execucao.getId().toString() + "_" + idActivation.toString() + ".xml";
            }
            StreamResult result = new StreamResult(new File(fileNameXML));
            transformer.transform(source, result);

        } catch (ParserConfigurationException pce) {
            pce.printStackTrace();
        } catch (TransformerException tfeTe) {
            tfeTe.printStackTrace();
        }

    }

    public void readResultFileXML() {

        String strTestCaseResult, strSystemStatus, strMessage;

        try {

            DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder docBuilder = docFactory.newDocumentBuilder();
            Document doc = docBuilder.parse(this.fileNameResultXML);

            strTestCaseResult = doc.getDocumentElement().getAttributes().item(0).getNodeValue();
            strSystemStatus = doc.getDocumentElement().getChildNodes().item(0).getAttributes().item(0).getNodeValue();
            strMessage = doc.getDocumentElement().getChildNodes().item(1).getTextContent();

            System.out.println("TestCaseResult : " + strTestCaseResult
                    + " SystemStatus : " + strSystemStatus
                    + " Message : " + strMessage);

            tstResult = new TestExecutionResult();

            tstResult.message = strMessage;

            //obter do xml de saida os parametros passados no contrutor do TestExecutionResult
            if (strSystemStatus.equalsIgnoreCase("fail")) {
                tstResult.systemStatus = SystemStatus.FAIL;
            } else {
                tstResult.systemStatus = SystemStatus.SUCCESS;
            }

            if (strTestCaseResult.equalsIgnoreCase("NOK")) {
                tstResult.tstCaseResult = TestCaseResult.NOK;
            } else {
                tstResult.tstCaseResult = TestCaseResult.OK;
            }


        } catch (ParserConfigurationException ex) {
        } catch (SAXException ex) {
        } catch (IOException ex) {
        }

    }

    public void setTestCasePath(String testCasePath) {
        this.testCasePath = testCasePath;
    }

    public void setWorkflowPath(String workflowPath) {
        this.workflowPath = workflowPath;
    }

    public void setResultPath(String resultPath) {
        this.resultPath = resultPath;
    }

    public TestExecutionResult getTstResult() {
        return tstResult;
    }

    public String getFileNameResultXML() {
        return fileNameResultXML;
    }
}
