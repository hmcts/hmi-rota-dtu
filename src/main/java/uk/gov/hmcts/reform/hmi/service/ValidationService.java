package uk.gov.hmcts.reform.hmi.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.xml.sax.SAXException;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import javax.xml.XMLConstants;
import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;

@Slf4j
@Service
@SuppressWarnings({"PMD.UseProperClassLoader"})
public class ValidationService {

    private Validator initValidator(String xsdPath) throws SAXException {
        try (InputStream masterFile = this.getClass().getClassLoader()
            .getResourceAsStream(xsdPath)) {
            SchemaFactory factory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            factory.setProperty(XMLConstants.ACCESS_EXTERNAL_DTD, "");
            factory.setProperty(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
            Source schemaFile = new StreamSource(masterFile);
            Schema schema = factory.newSchema(schemaFile);
            return schema.newValidator();
        } catch (IOException e) {
            log.error(String.format("Failed to get rota xsd file: %s", e.getMessage()));
            return null;
        }
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
}
