package uk.gov.hmcts.reform.hmi.service;

import com.azure.core.http.rest.PagedIterable;
import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.models.BlobItem;
import com.azure.storage.blob.specialized.BlobLeaseClient;
import com.azure.storage.blob.specialized.BlobLeaseClientBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AzureBlobService {
    private final BlobContainerClient rotaBlobContainerClient;
    private final BlobContainerClient processingBlobContainerClient;

    @Autowired
    public AzureBlobService(@Qualifier("rota") BlobContainerClient rotaBlobContainerClient,
                            @Qualifier("processing") BlobContainerClient processingBlobContainerClient) {
        this.rotaBlobContainerClient = rotaBlobContainerClient;
        this.processingBlobContainerClient = processingBlobContainerClient;
    }

    public List<BlobItem> getBlobs() {
        PagedIterable<BlobItem> blobs = rotaBlobContainerClient.listBlobs();
        return blobs.stream().toList();
    }

    /**
     * Delete a blob from the blob store by the file name.
     *
     * @param fileName The file name of the blob to delete
     */
    public void deleteOriginalBlob(String fileName) {
        BlobClient blobClient = rotaBlobContainerClient.getBlobClient(fileName);
        blobClient.delete();
    }

    /**
     * Delete a blob from the blob store by the file name.
     *
     * @param fileName The file name of the blob to delete
     */
    public void deleteProcessingBlob(String fileName) {
        BlobClient blobClient = processingBlobContainerClient.getBlobClient(fileName);
        blobClient.delete();
    }

    public void copyBlobToProcessingContainer(String fileName, String leaseId) {
        BlobClient currentBlob = rotaBlobContainerClient.getBlobClient(fileName);
        BlobClient processingBlob = processingBlobContainerClient.getBlobClient(fileName);

        // Create the lease client
        BlobLeaseClient leaseClient = new BlobLeaseClientBuilder()
            .blobClient(currentBlob)
            .leaseId(leaseId)
            .buildClient();

        leaseClient.releaseLease();

        processingBlob.copyFromUrl(currentBlob.getBlobUrl());
    }

    public String copyBlobForProcessing(String fileName, String leaseId) {
        BlobClient currentBlob = rotaBlobContainerClient.getBlobClient(fileName);

        // Create the lease client
        BlobLeaseClient leaseClient = new BlobLeaseClientBuilder()
            .blobClient(currentBlob)
            .leaseId(leaseId)
            .buildClient();

        leaseClient.releaseLease();

        BlobClient newBlob = rotaBlobContainerClient.getBlobClient("processing_" + fileName);


        return "processing_" + fileName;
    }

    public BlobLeaseClient acquireBlobLease(String fileName) {
        // Get the blob client
        BlobClient blob = rotaBlobContainerClient.getBlobClient(fileName);

        // Create the lease client
        BlobLeaseClient leaseClient = new BlobLeaseClientBuilder()
            .blobClient(blob)
            .buildClient();

        leaseClient.acquireLease(60);

        return leaseClient;
    }
}
