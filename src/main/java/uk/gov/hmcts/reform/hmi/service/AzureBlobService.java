package uk.gov.hmcts.reform.hmi.service;

import com.azure.core.http.rest.PagedIterable;
import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.models.BlobItem;
import com.azure.storage.blob.specialized.BlobLeaseClient;
import com.azure.storage.blob.specialized.BlobLeaseClientBuilder;
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

    public String copyBlobForProcessing(String fileName, String leaseId) {
        BlobClient currentBlob = blobContainerClient.getBlobClient(fileName);

        // Create the lease client
        BlobLeaseClient leaseClient = new BlobLeaseClientBuilder()
            .blobClient(currentBlob)
            .leaseId(leaseId)
            .buildClient();

        leaseClient.releaseLease();

        BlobClient newBlob = blobContainerClient.getBlobClient("processing_" + fileName);
        newBlob.copyFromUrl(currentBlob.getBlobUrl());

        return "processing_" + fileName;
    }

    public BlobLeaseClient acquireBlobLease(String fileName) {
        // Get the blob client
        BlobClient blob = blobContainerClient.getBlobClient(fileName);

        // Create the lease client
        BlobLeaseClient leaseClient = new BlobLeaseClientBuilder()
            .blobClient(blob)
            .buildClient();

        leaseClient.acquireLease(60);

        return leaseClient;
    }
}
