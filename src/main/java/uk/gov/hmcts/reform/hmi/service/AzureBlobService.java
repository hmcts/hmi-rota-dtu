package uk.gov.hmcts.reform.hmi.service;

import com.azure.core.http.rest.PagedIterable;
import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.models.BlobItem;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AzureBlobService {
    private final BlobContainerClient blobContainerClient;

    @Autowired
    public AzureBlobService(BlobContainerClient blobContainerClient) {
        this.blobContainerClient = blobContainerClient;
    }

    public List<BlobItem> getBlobs() {
        PagedIterable<BlobItem> blobs = blobContainerClient.listBlobs();
        return blobs.stream().toList();
    }

    /**
     * Delete a blob from the blob store by the file name.
     *
     * @param fileName The file name of the blob to delete
     */
    public void deleteBlob(String fileName) {
        BlobClient blobClient = blobContainerClient.getBlobClient(fileName);
        blobClient.delete();
    }
}
