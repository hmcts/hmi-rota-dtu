package uk.gov.hmcts.reform.hmi.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.xml.sax.SAXException;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import javax.xml.XMLConstants;
import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;

@Slf4j
@Service
public class ValidationService {

    private Validator initValidator(String xsdPath) throws SAXException {
        SchemaFactory factory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        factory.setProperty(XMLConstants.ACCESS_EXTERNAL_DTD, "");
        factory.setProperty(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
        Source schemaFile = new StreamSource(getSchemaFile(xsdPath));
        Schema schema = factory.newSchema(schemaFile);
        return schema.newValidator();
    }

    public boolean isValid(String xsdPath, byte[] rotaXml) throws IOException, SAXException {
        Validator validator = initValidator(xsdPath);
        try {
            validator.validate(new StreamSource(new ByteArrayInputStream(rotaXml)));
            return true;
        } catch (SAXException ex) {
            log.error(String.format("Failed to validate the schema with error message: %s", ex.getMessage()));
            return false;
        }
    }

    private File getSchemaFile(String location) {
        return new File(Thread.currentThread().getContextClassLoader()
                            .getResource(location)
                            .getFile());
    }
}
