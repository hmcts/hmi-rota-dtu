package uk.gov.hmcts.reform.hmi.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;

@Slf4j
@Service
public class ConversionService {

    public JsonNode convertXmlToJson(byte[] rotaXml) {
        try {
            XmlMapper xmlMapper = new XmlMapper();
            return xmlMapper.readTree(rotaXml);
        } catch (IOException e) {
            log.error(String.format("Failed to convert the blob to Json with error message: %s", e.getMessage()));
        }
        return null;
    }
}
