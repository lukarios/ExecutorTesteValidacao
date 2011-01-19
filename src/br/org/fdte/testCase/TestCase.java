package br.org.fdte.testCase;

import java.util.List;
import java.io.File;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

public class TestCase {

    String type;
    String workflowPath;
    String testCasePath;
    List<DataGroup> dataGroups;

    public TestCase(List<Field> fields) {
        super();
        DataGroup dtGroup = new DataGroup();
        dtGroup.fields = fields;
        dataGroups.add(dtGroup);
    }

    public String getTestCasePath() {
        return testCasePath;
    }

    public void setTestCasePath(String testCasePath) {
        this.testCasePath = testCasePath;
    }

    public String getWorkflowPath() {
        return workflowPath;
    }

    public void setWorkflowPath(String workflowPath) {
        this.workflowPath = workflowPath;
    }

    public void createFileXML() {
        try {

            DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder docBuilder = docFactory.newDocumentBuilder();

            Document doc = docBuilder.newDocument();
            Element rootElement = doc.createElement("testCase");
            doc.appendChild(rootElement);

            Attr attrType = doc.createAttribute("type");
            attrType.setValue("p");
            rootElement.setAttributeNode(attrType);

            Element workflow = doc.createElement("workflow");
            rootElement.appendChild(workflow);

            Attr attrWorkflowName = doc.createAttribute("name");
            attrWorkflowName.setValue("w1");
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
                    attrFieldName.setValue("fullName");
                    fieldElement.setAttributeNode(attrFieldName);

                    Attr attrFieldValue = doc.createAttribute("value");
                    attrFieldValue.setValue("Daniel Assis Alfenas");
                    fieldElement.setAttributeNode(attrFieldValue);
                }
            }

            //write the content into xml file / / Escreve o conteúdo em arquivo xml
            TransformerFactory transformerFactory = TransformerFactory.newInstance();
            Transformer transformer = transformerFactory.newTransformer();
            DOMSource source = new DOMSource(doc);
            StreamResult result = new StreamResult(new File("C:\\Users\\FDTE-Luciana\\Documents\\LucianaRios\\FDTE\\ testing.xml"));
            transformer.transform(source, result);

        } catch (ParserConfigurationException pce) {
            pce.printStackTrace();
        } catch (TransformerException tfe) {
            tfe.printStackTrace();
        }
    }
}
